package com.example.data.repository

import com.example.data.model.PrivacyOption
import com.example.data.model.StatusItem
import com.example.data.model.StatusType
import com.example.data.model.StatusViewer
import com.example.data.model.UserProfile
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class StatusRepository(private val db: FirebaseDatabase) {

    private val statusesRef = db.reference.child("statuses")
    private val statusViewsRef = db.reference.child("statusViews")

    fun observeActiveStatuses(currentUid: String, blockedUids: Set<String>): Flow<List<StatusItem>> =
        callbackFlow {
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val now = System.currentTimeMillis()
                    val all = mutableListOf<StatusItem>()
                    snapshot.children.forEach { userNode ->
                        val ownerUid = userNode.key ?: return@forEach
                        if (ownerUid in blockedUids) return@forEach
                        userNode.children.forEach { statusNode ->
                            val item = statusNode.getValue(StatusItem::class.java) ?: return@forEach
                            val validExpiry = item.expiresAt > now
                            val allowedPrivacy = when {
                                item.uid == currentUid -> true
                                item.privacy == PrivacyOption.NOBODY.name -> false
                                item.excludedUids[currentUid] == true -> false
                                else -> true
                            }
                            if (validExpiry && allowedPrivacy) {
                                all.add(item.copy(id = statusNode.key ?: item.id))
                            }
                        }
                    }
                    trySend(all.sortedByDescending { it.createdAt })
                }

                override fun onCancelled(error: DatabaseError) {}
            }
            statusesRef.addValueEventListener(listener)
            awaitClose { statusesRef.removeEventListener(listener) }
        }

    suspend fun postStatus(
        user: UserProfile,
        type: StatusType,
        text: String = "",
        backgroundColorHex: Long = 0xFF008069,
        mediaUrl: String = "",
        caption: String = "",
        privacy: PrivacyOption = PrivacyOption.EVERYONE
    ): Result<Unit> {
        return try {
            val statusId = statusesRef.child(user.uid).push().key
                ?: return Result.failure(Exception("Failed to allocate status ID"))
            val now = System.currentTimeMillis()
            val expiresAt = now + 24L * 60L * 60L * 1000L
            val item = StatusItem(
                id = statusId,
                uid = user.uid,
                userName = user.bestDisplayName(),
                userPhotoUrl = user.photoUrl,
                type = type.name,
                text = text,
                backgroundColorHex = backgroundColorHex,
                mediaUrl = mediaUrl,
                caption = caption,
                createdAt = now,
                expiresAt = expiresAt,
                privacy = privacy.name
            )
            statusesRef.child(user.uid).child(statusId).setValue(item).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markStatusViewed(ownerUid: String, statusId: String, viewer: UserProfile) {
        if (ownerUid == viewer.uid) return
        try {
            statusViewsRef.child(ownerUid).child(statusId).child(viewer.uid).setValue(
                StatusViewer(
                    uid = viewer.uid,
                    userName = viewer.bestDisplayName(),
                    userPhotoUrl = viewer.photoUrl,
                    viewedAt = System.currentTimeMillis()
                )
            ).await()
        } catch (_: Exception) {
        }
    }

    fun observeStatusViewers(ownerUid: String, statusId: String): Flow<List<StatusViewer>> = callbackFlow {
        val ref = statusViewsRef.child(ownerUid).child(statusId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val viewers = snapshot.children.mapNotNull { it.getValue(StatusViewer::class.java) }
                    .sortedByDescending { it.viewedAt }
                trySend(viewers)
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun deleteStatus(uid: String, statusId: String): Result<Unit> {
        return try {
            statusesRef.child(uid).child(statusId).removeValue().await()
            statusViewsRef.child(uid).child(statusId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
