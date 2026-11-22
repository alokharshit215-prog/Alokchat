package com.example.data.repository

import com.example.data.model.PrivacyOption
import com.example.data.model.StatusItem
import com.example.data.model.StatusPrivacyMode
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

    fun observeActiveStatuses(
        currentUid: String,
        blockedUids: Set<String>,
        myContactAndChatUids: Set<String> = emptySet()
    ): Flow<List<StatusItem>> =
        callbackFlow {
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val now = System.currentTimeMillis()
                    val all = mutableListOf<StatusItem>()
                    snapshot.children.forEach { userNode ->
                        val ownerUid = userNode.key ?: return@forEach
                        if (ownerUid in blockedUids) return@forEach
                        userNode.children.forEach { statusNode ->
                            val raw = statusNode.getValue(StatusItem::class.java) ?: return@forEach
                            val item = raw.copy(
                                id = statusNode.key ?: raw.id,
                                uid = raw.uid.ifBlank { ownerUid }
                            )
                            val validExpiry = item.expiresAt > now
                            if (validExpiry && isStatusVisibleToViewer(item, currentUid, blockedUids, myContactAndChatUids)) {
                                all.add(item)
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
        privacyMode: StatusPrivacyMode = StatusPrivacyMode.EVERYONE,
        excludedUids: Set<String> = emptySet(),
        allowedUids: Set<String> = emptySet(),
        contactUids: Set<String> = emptySet()
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
                privacy = privacyMode.name,
                excludedUids = excludedUids.associateWith { true },
                allowedUids = allowedUids.associateWith { true },
                contactUids = contactUids.associateWith { true }
            )
            statusesRef.child(user.uid).child(statusId).setValue(item).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        fun isStatusVisibleToViewer(
            item: StatusItem,
            viewerUid: String,
            blockedUids: Set<String>,
            viewerContactAndChatUids: Set<String>
        ): Boolean {
            if (item.uid == viewerUid) return true
            if (item.uid in blockedUids) return false

            return when (item.privacy) {
                PrivacyOption.NOBODY.name -> false
                StatusPrivacyMode.ONLY_SHARE_WITH.name -> item.allowedUids[viewerUid] == true
                StatusPrivacyMode.MY_CONTACTS_EXCEPT.name -> {
                    item.excludedUids[viewerUid] != true &&
                        (item.contactUids[viewerUid] == true || item.uid in viewerContactAndChatUids)
                }
                StatusPrivacyMode.MY_CONTACTS.name, PrivacyOption.CONTACTS.name -> {
                    item.contactUids[viewerUid] == true || item.uid in viewerContactAndChatUids
                }
                else -> item.excludedUids[viewerUid] != true
            }
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
