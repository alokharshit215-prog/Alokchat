package com.example.media

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class VoiceRecordingState(
    val isRecording: Boolean = false,
    val elapsedMs: Long = 0L,
    val amplitude: Float = 0f,
    val outputFile: File? = null
)

class VoiceRecorderManager(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var timerJob: Job? = null
    private var startTimeMs: Long = 0L

    private val _state = MutableStateFlow(VoiceRecordingState())
    val state: StateFlow<VoiceRecordingState> = _state.asStateFlow()

    fun startRecording(scope: CoroutineScope): Result<Unit> {
        return try {
            stopAndDiscard()
            val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            currentFile = file
            startTimeMs = System.currentTimeMillis()
            _state.value = VoiceRecordingState(
                isRecording = true,
                elapsedMs = 0L,
                amplitude = 0f,
                outputFile = file
            )
            timerJob = scope.launch(Dispatchers.Default) {
                while (isActive && _state.value.isRecording) {
                    val amp = try {
                        ((recorder?.maxAmplitude ?: 0) / 32767f).coerceIn(0f, 1f)
                    } catch (_: Exception) {
                        0f
                    }
                    _state.value = _state.value.copy(
                        elapsedMs = System.currentTimeMillis() - startTimeMs,
                        amplitude = amp
                    )
                    delay(100)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start voice recording", e)
            stopAndDiscard()
            Result.failure(e)
        }
    }

    fun stopAndGetRecording(): Pair<Uri, Long>? {
        val file = currentFile ?: return null
        val duration = (System.currentTimeMillis() - startTimeMs).coerceAtLeast(500L)
        releaseRecorder()
        _state.value = VoiceRecordingState()
        return if (file.exists() && file.length() > 0L) {
            Uri.fromFile(file) to duration
        } else {
            null
        }
    }

    fun stopAndDiscard() {
        releaseRecorder()
        currentFile?.delete()
        currentFile = null
        _state.value = VoiceRecordingState()
    }

    private fun releaseRecorder() {
        timerJob?.cancel()
        timerJob = null
        try {
            recorder?.stop()
        } catch (_: Exception) {
        }
        try {
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
    }

    private companion object {
        const val TAG = "VoiceRecorder"
    }
}
