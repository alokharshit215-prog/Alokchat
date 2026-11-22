package com.example.data.repository

import android.util.Log
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSummary
import com.example.data.model.GroupInfo
import com.example.data.model.MessageType
import com.example.data.model.UserProfile
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class GroupRepository(private val db: FirebaseDatabase) {

    private val groupsRef = db.reference.child("groups")
    private val chatsRef = db.reference.child("chats")
    private val userChatsRef = db.reference.child("userChats")
    private val messagesRef = db.reference.child("messages")

    fun observeGroup(groupId: String): Flow<GroupInfo?> = callbackFlow {
        val ref = groupsRef.child(groupId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(runCatching { snapshot.getValue(GroupInfo::class.java) }.getOrNull())
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun createGroup(
        creator: UserProfile,
        name: String,
        description: String,
        photoUrl: String,
        participantUids: List<String>
    ): Result<String> {
        return try {
            val groupId = groupsRef.push().key
                ?: return Result.failure(Exception("Could not create group ID."))
            val now = System.currentTimeMillis()
            val allMembers = (participantUids + creator.uid).distinct().associateWith { true }
            val admins = mapOf(creator.uid to true)

            val group = GroupInfo(
                groupId = groupId,
                name = name.trim(),
                description = description.trim(),
                photoUrl = photoUrl,
                createdBy = creator.uid,
                createdAt = now,
                members = allMembers,
                admins = admins
            )

            groupsRef.child(groupId).setValue(group).await()
            chatsRef.child(groupId).setValue(
                mapOf(
                    "chatId" to groupId,
                    "group" to true,
                    "createdAt" to now,
                    "members" to allMembers
                )
            ).await()

            val creatorName = creator.bestDisplayName()
            val systemText = "$creatorName created group \"${name.trim()}\""
            val msgId = messagesRef.child(groupId).push().key ?: now.toString()
            val sysMsg = ChatMessage(
                id = msgId,
                chatId = groupId,
                senderId = creator.uid,
                senderName = creatorName,
                type = MessageType.SYSTEM.name,
                text = systemText,
                timestamp = now
            )
            messagesRef.child(groupId).child(msgId).setValue(sysMsg).await()

            allMembers.keys.forEach { uid ->
                userChatsRef.child(uid).child(groupId).setValue(
                    ChatSummary(
                        chatId = groupId,
                        group = true,
                        peerUid = "",
                        title = name.trim(),
                        photoUrl = photoUrl,
                        lastMessage = systemText,
                        lastMessageType = MessageType.SYSTEM.name,
                        lastMessageSenderId = creator.uid,
                        lastMessageAt = now,
                        unreadCount = if (uid == creator.uid) 0 else 1
                    )
                ).await()
            }
            Result.success(groupId)
        } catch (e: Exception) {
            Log.e(TAG, "createGroup failed", e)
            Result.failure(e)
        }
    }

    suspend fun updateGroupDetails(
        groupId: String,
        name: String,
        description: String,
        photoUrl: String?
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "name" to name.trim(),
                "description" to description.trim()
            )
            if (photoUrl != null) {
                updates["photoUrl"] = photoUrl
            }
            groupsRef.child(groupId).updateChildren(updates).await()
            val group = groupsRef.child(groupId).get().await().getValue(GroupInfo::class.java)
            group?.members?.keys?.forEach { uid ->
                val summaryUpdates = mutableMapOf<String, Any>("title" to name.trim())
                if (photoUrl != null) summaryUpdates["photoUrl"] = photoUrl
                userChatsRef.child(uid).child(groupId).updateChildren(summaryUpdates)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addParticipants(groupId: String, newUids: List<String>): Result<Unit> {
        return try {
            val group = groupsRef.child(groupId).get().await().getValue(GroupInfo::class.java)
                ?: return Result.failure(Exception("Group not found"))
            val now = System.currentTimeMillis()
            newUids.forEach { uid ->
                groupsRef.child(groupId).child("members").child(uid).setValue(true).await()
                chatsRef.child(groupId).child("members").child(uid).setValue(true).await()
                userChatsRef.child(uid).child(groupId).setValue(
                    ChatSummary(
                        chatId = groupId,
                        group = true,
                        title = group.name,
                        photoUrl = group.photoUrl,
                        lastMessage = "You were added to ${group.name}",
                        lastMessageType = MessageType.SYSTEM.name,
                        lastMessageAt = now,
                        unreadCount = 1
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeParticipant(groupId: String, targetUid: String): Result<Unit> {
        return try {
            groupsRef.child(groupId).child("admins").child(targetUid).removeValue().await()
            groupsRef.child(groupId).child("members").child(targetUid).removeValue().await()
            chatsRef.child(groupId).child("members").child(targetUid).removeValue().await()
            userChatsRef.child(targetUid).child(groupId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setAdminStatus(groupId: String, targetUid: String, makeAdmin: Boolean): Result<Unit> {
        return try {
            val ref = groupsRef.child(groupId).child("admins").child(targetUid)
            if (makeAdmin) ref.setValue(true).await() else ref.removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun leaveGroup(groupId: String, uid: String): Result<Unit> = removeParticipant(groupId, uid)

    suspend fun deleteGroup(groupId: String): Result<Unit> {
        return try {
            val group = groupsRef.child(groupId).get().await().getValue(GroupInfo::class.java)
            group?.members?.keys?.forEach { uid ->
                userChatsRef.child(uid).child(groupId).removeValue().await()
            }
            messagesRef.child(groupId).removeValue().await()
            chatsRef.child(groupId).removeValue().await()
            groupsRef.child(groupId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "GroupRepository"
    }
}
