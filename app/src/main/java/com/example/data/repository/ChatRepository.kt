package com.example.data.repository

import android.util.Log
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSummary
import com.example.data.model.MessageStatus
import com.example.data.model.MessageType
import com.example.data.model.UserProfile
import com.example.util.FormatUtils
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository(private val db: FirebaseDatabase) {

    private val chatsRef = db.reference.child("chats")
    private val userChatsRef = db.reference.child("userChats")
    private val messagesRef = db.reference.child("messages")
    private val typingRef = db.reference.child("typing")
    private val groupsRef = db.reference.child("groups")
    private val notificationsRef = db.reference.child("notifications")

    fun observeUserChats(uid: String): Flow<List<ChatSummary>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val ref = userChatsRef.child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(ChatSummary::class.java) }
                    .sortedWith(
                        compareByDescending<ChatSummary> { it.pinned }
                            .thenByDescending { it.lastMessageAt }
                    )
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "observeUserChats cancelled: ${error.message}")
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeMessages(chatId: String, limit: Int = 50): Flow<List<ChatMessage>> = callbackFlow {
        if (chatId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val query = messagesRef.child(chatId)
            .orderByChild("timestamp")
            .limitToLast(limit)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    child.getValue(ChatMessage::class.java)?.copy(id = child.key ?: "")
                }.sortedBy { it.timestamp }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "observeMessages cancelled: ${error.message}")
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    fun observeTypingUsers(chatId: String, currentUid: String): Flow<List<String>> = callbackFlow {
        val ref = typingRef.child(chatId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val typingIds = snapshot.children.mapNotNull { child ->
                    val isTyping = child.getValue(Boolean::class.java) ?: false
                    val uid = child.key
                    if (isTyping && uid != null && uid != currentUid) uid else null
                }
                trySend(typingIds)
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun setTyping(chatId: String, uid: String, isTyping: Boolean) {
        if (chatId.isBlank() || uid.isBlank()) return
        if (isTyping) {
            typingRef.child(chatId).child(uid).onDisconnect().removeValue()
            typingRef.child(chatId).child(uid).setValue(true)
        } else {
            typingRef.child(chatId).child(uid).removeValue()
        }
    }

    suspend fun openOrCreateDirectChat(currentUser: UserProfile, peerUser: UserProfile): Result<String> {
        return try {
            val chatId = FormatUtils.deterministicChatId(currentUser.uid, peerUser.uid)
            val chatSnap = chatsRef.child(chatId).get().await()
            if (!chatSnap.exists()) {
                chatsRef.child(chatId).setValue(
                    mapOf(
                        "chatId" to chatId,
                        "group" to false,
                        "createdAt" to System.currentTimeMillis(),
                        "members" to mapOf(
                            currentUser.uid to true,
                            peerUser.uid to true
                        )
                    )
                ).await()
            }
            val now = System.currentTimeMillis()
            val myChatSnap = userChatsRef.child(currentUser.uid).child(chatId).get().await()
            if (!myChatSnap.exists()) {
                val mySummary = ChatSummary(
                    chatId = chatId,
                    group = false,
                    peerUid = peerUser.uid,
                    title = peerUser.bestDisplayName(),
                    photoUrl = peerUser.photoUrl,
                    lastMessage = "",
                    lastMessageAt = now,
                    unreadCount = 0
                )
                userChatsRef.child(currentUser.uid).child(chatId).setValue(mySummary).await()
            }
            val peerChatSnap = userChatsRef.child(peerUser.uid).child(chatId).get().await()
            if (!peerChatSnap.exists()) {
                val peerSummary = ChatSummary(
                    chatId = chatId,
                    group = false,
                    peerUid = currentUser.uid,
                    title = currentUser.bestDisplayName(),
                    photoUrl = currentUser.photoUrl,
                    lastMessage = "",
                    lastMessageAt = now,
                    unreadCount = 0
                )
                userChatsRef.child(peerUser.uid).child(chatId).setValue(peerSummary).await()
            }
            Result.success(chatId)
        } catch (e: Exception) {
            Log.e(TAG, "openOrCreateDirectChat failed", e)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        chatId: String,
        sender: UserProfile,
        type: MessageType,
        text: String = "",
        mediaUrl: String = "",
        mediaName: String = "",
        mediaMimeType: String = "",
        mediaSizeBytes: Long = 0L,
        mediaDurationMs: Long = 0L,
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        locationLabel: String = "",
        contactName: String = "",
        contactPhone: String = "",
        replyTo: ChatMessage? = null,
        forwarded: Boolean = false
    ): Result<ChatMessage> {
        return try {
            val msgId = messagesRef.child(chatId).push().key
                ?: return Result.failure(Exception("Could not generate message ID."))
            val now = System.currentTimeMillis()
            val senderName = sender.bestDisplayName()

            val message = ChatMessage(
                id = msgId,
                chatId = chatId,
                senderId = sender.uid,
                senderName = senderName,
                type = type.name,
                text = text,
                mediaUrl = mediaUrl,
                mediaName = mediaName,
                mediaMimeType = mediaMimeType,
                mediaSizeBytes = mediaSizeBytes,
                mediaDurationMs = mediaDurationMs,
                latitude = latitude,
                longitude = longitude,
                locationLabel = locationLabel,
                contactName = contactName,
                contactPhone = contactPhone,
                replyToId = replyTo?.id ?: "",
                replyToSender = replyTo?.senderName ?: "",
                replyToPreview = replyTo?.let { previewTextFor(it) } ?: "",
                replyToType = replyTo?.type ?: "",
                forwarded = forwarded,
                timestamp = now,
                deliveredTo = mapOf(sender.uid to true),
                readBy = mapOf(sender.uid to true)
            )
            messagesRef.child(chatId).child(msgId).setValue(message).await()

            val preview = previewTextFor(message)
            val memberUids = resolveChatMembers(chatId)
            val isGroup = groupsRef.child(chatId).get().await().exists()
            val groupSnap = if (isGroup) groupsRef.child(chatId).get().await() else null
            val groupTitle = groupSnap?.child("name")?.getValue(String::class.java) ?: "Group"
            val groupPhoto = groupSnap?.child("photoUrl")?.getValue(String::class.java) ?: ""

            memberUids.forEach { memberUid ->
                val summaryRef = userChatsRef.child(memberUid).child(chatId)
                val existing = summaryRef.get().await().getValue(ChatSummary::class.java)
                val unread = if (memberUid == sender.uid) 0 else (existing?.unreadCount ?: 0) + 1
                val updatedSummary = (existing ?: ChatSummary(
                    chatId = chatId,
                    group = isGroup,
                    peerUid = if (isGroup) "" else memberUids.firstOrNull { it != memberUid }.orEmpty(),
                    title = if (isGroup) groupTitle else senderName,
                    photoUrl = if (isGroup) groupPhoto else sender.photoUrl
                )).copy(
                    lastMessage = preview,
                    lastMessageType = type.name,
                    lastMessageSenderId = sender.uid,
                    lastMessageAt = now,
                    unreadCount = unread,
                    lastMessageStatus = MessageStatus.SENT.name
                )
                summaryRef.setValue(updatedSummary).await()

                if (memberUid != sender.uid) {
                    notificationsRef.child(memberUid).child(msgId).setValue(
                        mapOf(
                            "type" to if (isGroup) "group_message" else "message",
                            "chatId" to chatId,
                            "title" to if (isGroup) "$senderName • $groupTitle" else senderName,
                            "body" to preview,
                            "isGroup" to isGroup,
                            "timestamp" to now
                        )
                    )
                }
            }
            setTyping(chatId, sender.uid, false)
            Result.success(message)
        } catch (e: Exception) {
            Log.e(TAG, "sendMessage failed", e)
            Result.failure(e)
        }
    }

    suspend fun forwardMessages(
        messages: List<ChatMessage>,
        targetChatIds: List<String>,
        sender: UserProfile
    ): Result<Unit> {
        return try {
            for (targetChatId in targetChatIds) {
                for (msg in messages) {
                    val type = MessageType.entries.firstOrNull { it.name == msg.type } ?: MessageType.TEXT
                    sendMessage(
                        chatId = targetChatId,
                        sender = sender,
                        type = type,
                        text = msg.text,
                        mediaUrl = msg.mediaUrl,
                        mediaName = msg.mediaName,
                        mediaMimeType = msg.mediaMimeType,
                        mediaSizeBytes = msg.mediaSizeBytes,
                        mediaDurationMs = msg.mediaDurationMs,
                        latitude = msg.latitude,
                        longitude = msg.longitude,
                        locationLabel = msg.locationLabel,
                        contactName = msg.contactName,
                        contactPhone = msg.contactPhone,
                        forwarded = true
                    )
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun editMessage(chatId: String, messageId: String, newText: String): Result<Unit> {
        return try {
            messagesRef.child(chatId).child(messageId).updateChildren(
                mapOf(
                    "text" to newText.trim(),
                    "edited" to true
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMessagesForMe(chatId: String, uid: String, messageIds: List<String>): Result<Unit> {
        return try {
            messageIds.forEach { msgId ->
                messagesRef.child(chatId).child(msgId).child("deletedFor").child(uid).setValue(true).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMessagesForEveryone(chatId: String, messageIds: List<String>): Result<Unit> {
        return try {
            messageIds.forEach { msgId ->
                messagesRef.child(chatId).child(msgId).updateChildren(
                    mapOf(
                        "deletedForAll" to true,
                        "text" to "This message was deleted",
                        "mediaUrl" to "",
                        "pinned" to false
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleStarMessage(chatId: String, messageId: String, uid: String, star: Boolean): Result<Unit> {
        return try {
            val ref = messagesRef.child(chatId).child(messageId).child("starredBy").child(uid)
            if (star) ref.setValue(true).await() else ref.removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun togglePinMessage(chatId: String, messageId: String, pin: Boolean): Result<Unit> {
        return try {
            messagesRef.child(chatId).child(messageId).child("pinned").setValue(pin).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setReaction(chatId: String, messageId: String, uid: String, emoji: String?): Result<Unit> {
        return try {
            val ref = messagesRef.child(chatId).child(messageId).child("reactions").child(uid)
            if (emoji.isNullOrBlank()) ref.removeValue().await() else ref.setValue(emoji).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markMessagesDeliveredAndRead(
        chatId: String,
        uid: String,
        messages: List<ChatMessage>,
        sendReadReceipts: Boolean
    ) {
        try {
            userChatsRef.child(uid).child(chatId).child("unreadCount").setValue(0)
            messages.filter { it.senderId != uid && !it.deletedForAll }.forEach { msg ->
                if (msg.deliveredTo[uid] != true) {
                    messagesRef.child(chatId).child(msg.id).child("deliveredTo").child(uid).setValue(true)
                }
                if (sendReadReceipts && msg.readBy[uid] != true) {
                    messagesRef.child(chatId).child(msg.id).child("readBy").child(uid).setValue(true)
                }
            }
        } catch (_: Exception) {
        }
    }

    suspend fun togglePinChat(uid: String, chatId: String, pinned: Boolean) {
        userChatsRef.child(uid).child(chatId).child("pinned").setValue(pinned).await()
    }

    suspend fun toggleMuteChat(uid: String, chatId: String, muted: Boolean) {
        userChatsRef.child(uid).child(chatId).child("muted").setValue(muted).await()
    }

    suspend fun loadStarredMessages(uid: String): List<ChatMessage> {
        return try {
            val userChatSnap = userChatsRef.child(uid).get().await()
            val chatIds = userChatSnap.children.mapNotNull { it.key }
            val result = mutableListOf<ChatMessage>()
            for (chatId in chatIds) {
                val msgSnap = messagesRef.child(chatId).get().await()
                msgSnap.children.mapNotNull { it.getValue(ChatMessage::class.java)?.copy(id = it.key ?: "") }
                    .filter { it.starredBy[uid] == true && it.deletedFor[uid] != true && !it.deletedForAll }
                    .forEach { result.add(it) }
            }
            result.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun resolveChatMembers(chatId: String): List<String> {
        val chatMembersSnap = chatsRef.child(chatId).child("members").get().await()
        if (chatMembersSnap.exists()) {
            return chatMembersSnap.children.mapNotNull { child ->
                if (child.getValue(Boolean::class.java) == true) child.key else null
            }
        }
        val groupMembersSnap = groupsRef.child(chatId).child("members").get().await()
        return groupMembersSnap.children.mapNotNull { child ->
            if (child.getValue(Boolean::class.java) == true) child.key else null
        }
    }

    private fun previewTextFor(msg: ChatMessage): String {
        return when (msg.type) {
            MessageType.IMAGE.name -> if (msg.text.isNotBlank()) "\uD83D\uDCF7 ${msg.text}" else "\uD83D\uDCF7 Photo"
            MessageType.VIDEO.name -> if (msg.text.isNotBlank()) "\uD83C\uDFA5 ${msg.text}" else "\uD83C\uDFA5 Video"
            MessageType.VOICE.name -> "\uD83C\uDFA4 Voice message"
            MessageType.AUDIO.name -> "\uD83C\uDFB5 ${msg.mediaName.ifBlank { "Audio" }}"
            MessageType.PDF.name -> "\uD83D\uDCC4 ${msg.mediaName.ifBlank { "PDF Document" }}"
            MessageType.DOCUMENT.name -> "\uD83D\uDCCE ${msg.mediaName.ifBlank { "Document" }}"
            MessageType.CONTACT.name -> "\uD83D\uDC64 ${msg.contactName.ifBlank { "Contact" }}"
            MessageType.LOCATION.name -> "\uD83D\uDCCD Location"
            else -> msg.text
        }
    }

    private companion object {
        const val TAG = "ChatRepository"
    }
}
