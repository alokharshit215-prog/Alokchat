package com.example.data.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.UploadTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class FileMeta(
    val name: String,
    val mimeType: String,
    val sizeBytes: Long
)

class MediaRepository(private val storage: FirebaseStorage) {

    private val activeUploads = ConcurrentHashMap<String, UploadTask>()

    fun cancelUpload(uploadId: String) {
        activeUploads.remove(uploadId)?.cancel()
    }

    fun getFileMeta(context: Context, uri: Uri): FileMeta {
        var name = "file_${System.currentTimeMillis()}"
        var size = 0L
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: name
                    if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
                }
            }
        } catch (_: Exception) {
        }
        if (size == 0L && uri.scheme == "file") {
            val f = File(uri.path ?: "")
            if (f.exists()) {
                name = f.name
                size = f.length()
            }
        }
        return FileMeta(name = name, mimeType = mime, sizeBytes = size)
    }

    suspend fun compressBitmapToJpegBytes(bitmap: Bitmap, quality: Int = 85): ByteArray =
        withContext(Dispatchers.Default) {
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray()
        }

    suspend fun compressImageUri(context: Context, uri: Uri, maxDim: Int = 1440, quality: Int = 82): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                val input = context.contentResolver.openInputStream(uri) ?: return@withContext null
                val rawBitmap = input.use { BitmapFactory.decodeStream(it) } ?: return@withContext null

                val rotatedBitmap = try {
                    context.contentResolver.openInputStream(uri)?.use { exifStream ->
                        val exif = ExifInterface(exifStream)
                        val orientation = exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                        val angle = when (orientation) {
                            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                            else -> 0f
                        }
                        if (angle != 0f) {
                            val matrix = Matrix().apply { postRotate(angle) }
                            Bitmap.createBitmap(
                                rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true
                            )
                        } else {
                            rawBitmap
                        }
                    } ?: rawBitmap
                } catch (_: Exception) {
                    rawBitmap
                }

                val scale = maxOf(rotatedBitmap.width, rotatedBitmap.height).let { largest ->
                    if (largest > maxDim) maxDim.toFloat() / largest else 1f
                }
                val finalBitmap = if (scale < 1f) {
                    Bitmap.createScaledBitmap(
                        rotatedBitmap,
                        (rotatedBitmap.width * scale).toInt().coerceAtLeast(1),
                        (rotatedBitmap.height * scale).toInt().coerceAtLeast(1),
                        true
                    )
                } else {
                    rotatedBitmap
                }

                val out = ByteArrayOutputStream()
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                out.toByteArray()
            } catch (e: Exception) {
                Log.e(TAG, "compressImageUri failed", e)
                null
            }
        }

    suspend fun uploadBytes(
        uploadId: String,
        storagePath: String,
        bytes: ByteArray,
        contentType: String,
        onProgress: (Float) -> Unit = {}
    ): Result<String> {
        return try {
            val ref = storage.reference.child(storagePath)
            val metadata = StorageMetadata.Builder().setContentType(contentType).build()
            val task = ref.putBytes(bytes, metadata)
            activeUploads[uploadId] = task

            suspendCancellableCoroutine { cont ->
                task.addOnProgressListener { snap ->
                    if (snap.totalByteCount > 0) {
                        onProgress(snap.bytesTransferred.toFloat() / snap.totalByteCount.toFloat())
                    }
                }.addOnSuccessListener {
                    activeUploads.remove(uploadId)
                    cont.resume(Unit)
                }.addOnFailureListener { e ->
                    activeUploads.remove(uploadId)
                    cont.resumeWithException(e)
                }
                cont.invokeOnCancellation {
                    activeUploads.remove(uploadId)?.cancel()
                }
            }
            val url = ref.downloadUrl.await().toString()
            Result.success(url)
        } catch (e: Exception) {
            activeUploads.remove(uploadId)
            Log.e(TAG, "uploadBytes failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun uploadUri(
        uploadId: String,
        storagePath: String,
        uri: Uri,
        contentType: String,
        onProgress: (Float) -> Unit = {}
    ): Result<String> {
        return try {
            val ref = storage.reference.child(storagePath)
            val metadata = StorageMetadata.Builder().setContentType(contentType).build()
            val task = ref.putFile(uri, metadata)
            activeUploads[uploadId] = task

            suspendCancellableCoroutine { cont ->
                task.addOnProgressListener { snap ->
                    if (snap.totalByteCount > 0) {
                        onProgress(snap.bytesTransferred.toFloat() / snap.totalByteCount.toFloat())
                    }
                }.addOnSuccessListener {
                    activeUploads.remove(uploadId)
                    cont.resume(Unit)
                }.addOnFailureListener { e ->
                    activeUploads.remove(uploadId)
                    cont.resumeWithException(e)
                }
                cont.invokeOnCancellation {
                    activeUploads.remove(uploadId)?.cancel()
                }
            }
            val url = ref.downloadUrl.await().toString()
            Result.success(url)
        } catch (e: Exception) {
            activeUploads.remove(uploadId)
            Log.e(TAG, "uploadUri failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteStoragePath(storagePath: String) {
        try {
            storage.reference.child(storagePath).delete().await()
        } catch (_: Exception) {
        }
    }

    suspend fun downloadMediaToDevice(
        context: Context,
        mediaUrl: String,
        fileName: String,
        mimeType: String,
        onProgress: (Float) -> Unit = {}
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val connection = URL(mediaUrl).openConnection().apply { connect() }
            val total = connection.contentLength.toLong().coerceAtLeast(1L)
            val tempFile = File(context.cacheDir, "dl_${System.currentTimeMillis()}_$fileName")
            connection.getInputStream().use { input ->
                tempFile.outputStream().use { output ->
                    val buf = ByteArray(8192)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buf).also { read = it } != -1) {
                        output.write(buf, 0, read)
                        downloaded += read
                        onProgress((downloaded.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val collection = when {
                    mimeType.startsWith("image/") -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    mimeType.startsWith("video/") -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    else -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
                }
                val relativePath = when {
                    mimeType.startsWith("image/") -> "${Environment.DIRECTORY_PICTURES}/Alok Chat"
                    mimeType.startsWith("video/") -> "${Environment.DIRECTORY_MOVIES}/Alok Chat"
                    else -> "${Environment.DIRECTORY_DOWNLOADS}/Alok Chat"
                }
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                }
                val outUri = context.contentResolver.insert(collection, values)
                    ?: return@withContext Result.failure(Exception("Could not create MediaStore entry."))
                context.contentResolver.openOutputStream(outUri)?.use { out ->
                    tempFile.inputStream().use { it.copyTo(out) }
                }
                tempFile.delete()
                Result.success(outUri)
            } else {
                val outFile = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    fileName
                )
                tempFile.copyTo(outFile, overwrite = true)
                tempFile.delete()
                Result.success(Uri.fromFile(outFile))
            }
        } catch (e: Exception) {
            Log.e(TAG, "downloadMediaToDevice failed", e)
            Result.failure(e)
        }
    }

    fun getCacheSizeBytes(context: Context): Long {
        fun folderSize(dir: File): Long {
            var size = 0L
            dir.listFiles()?.forEach { file ->
                size += if (file.isDirectory) folderSize(file) else file.length()
            }
            return size
        }
        return folderSize(context.cacheDir)
    }

    fun clearAppCache(context: Context) {
        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
    }

    private companion object {
        const val TAG = "MediaRepository"
    }
}
