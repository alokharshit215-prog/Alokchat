package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChatMessage
import com.example.data.model.GroupInfo
import com.example.data.model.MessageType
import com.example.data.model.PendingUpload
import com.example.data.model.UserProfile
import com.example.data.repository.ChatRepository
import com.example.data.repository.GroupRepository
import com.example.data.repository.MediaRepository
import com.example.data.repository.UserRepository
import com.example.media.AudioPlaybackState
import com.example.media.AudioPlayerManager
import com.example.media.VoiceRecorderManager
import com.example.media.VoiceRecordingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ChatViewModel(
    val chatId: String,
    val currentUid: String,
    val peerUid: String,
    val isGroup: Boolean,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val mediaRepository: MediaRepository,
    private val voiceRecorderManager: VoiceRecorderManager,
    private val audioPlayerManager: AudioPlayerManager
) : ViewModel() {

    private val pageLimit = MutableStateFlow(50)

    val messages: StateFlow<List<ChatMessage>> = pageLimit
        .flatMapLatest { limit -> chatRepository.observeMessages(chatId, limit) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val currentUser: StateFlow<UserProfile?> = userRepository.observeUser(currentUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val peerUser: StateFlow<UserProfile?> = if (!isGroup && peerUid.isNotBlank()) {
        userRepository.observeUser(peerUid)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)
    } else {
        MutableStateFlow(null)
    }

    val groupInfo: StateFlow<GroupInfo?> = if (isGroup) {
        groupRepository.observeGroup(chatId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)
    } else {
        MutableStateFlow(null)
    }

    val typingUserIds: StateFlow<List<String>> = chatRepository.observeTypingUsers(chatId, currentUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val recordingState: StateFlow<VoiceRecordingState> = voiceRecorderManager.state
    val audioPlaybackState: StateFlow<AudioPlaybackState> = audioPlayerManager.state

    private val _pendingUploads = MutableStateFlow<List<PendingUpload>>(emptyList())
    val pendingUploads: StateFlow<List<PendingUpload>> = _pendingUploads.asStateFlow()

    private val _replyTo = MutableStateFlow<ChatMessage?>(null)
    val replyTo: StateFlow<ChatMessage?> = _replyTo.asStateFlow()

    private val _editingMessage = MutableStateFlow<ChatMessage?>(null)
    val editingMessage: StateFlow<ChatMessage?> = _editingMessage.asStateFlow()

    private val _selectedMessageIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedMessageIds: StateFlow<Set<String>> = _selectedMessageIds.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    private val _statusToast = MutableStateFlow<String?>(null)
    val statusToast: StateFlow<String?> = _statusToast.asStateFlow()

    fun clearToast() {
        _statusToast.value = null
    }

    fun loadMoreMessages() {
        pageLimit.value += 50
    }

    fun onTypingChanged(isTyping: Boolean) {
        chatRepository.setTyping(chatId, currentUid, isTyping)
    }

    fun setReplyTo(msg: ChatMessage?) {
        _editingMessage.value = null
        _replyTo.value = msg
    }

    fun startEditing(msg: ChatMessage) {
        _replyTo.value = null
        _editingMessage.value = msg
        _selectedMessageIds.value = emptySet()
    }

    fun cancelEditing() {
        _editingMessage.value = null
    }

    fun toggleSelectMessage(messageId: String) {
        _selectedMessageIds.update { set ->
            if (messageId in set) set - messageId else set + messageId
        }
    }

    fun clearSelection() {
        _selectedMessageIds.value = emptySet()
    }

    fun markVisibleMessagesRead(msgs: List<ChatMessage>) {
        val me = currentUser.value ?: return
        viewModelScope.launch {
            chatRepository.markMessagesDeliveredAndRead(
                chatId = chatId,
                uid = currentUid,
                messages = msgs,
                sendReadReceipts = me.readReceiptsEnabled
            )
        }
    }

    fun sendTextOrSaveEdit(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val editing = _editingMessage.value
        if (editing != null) {
            viewModelScope.launch {
                chatRepository.editMessage(chatId, editing.id, trimmed)
                _editingMessage.value = null
            }
            return
        }
        val sender = currentUser.value ?: return
        val reply = _replyTo.value
        _replyTo.value = null
        viewModelScope.launch {
            chatRepository.sendMessage(
                chatId = chatId,
                sender = sender,
                type = MessageType.TEXT,
                text = trimmed,
                replyTo = reply
            )
        }
    }

    fun sendContact(name: String, phone: String) {
        val sender = currentUser.value ?: return
        viewModelScope.launch {
            chatRepository.sendMessage(
                chatId = chatId,
                sender = sender,
                type = MessageType.CONTACT,
                contactName = name,
                contactPhone = phone
            )
        }
    }

    fun sendLocation(lat: Double, lng: Double, label: String) {
        val sender = currentUser.value ?: return
        viewModelScope.launch {
            chatRepository.sendMessage(
                chatId = chatId,
                sender = sender,
                type = MessageType.LOCATION,
                latitude = lat,
                longitude = lng,
                locationLabel = label
            )
        }
    }

    fun sendMediaUris(
        context: Context,
        uris: List<Uri>,
        type: MessageType,
        caption: String = "",
        durationMs: Long = 0L
    ) {
        uris.forEach { uri ->
            val meta = mediaRepository.getFileMeta(context, uri)
            val resolvedType = when {
                type == MessageType.DOCUMENT && meta.mimeType == "application/pdf" -> MessageType.PDF
                else -> type
            }
            val upload = PendingUpload(
                tempId = "up_${System.currentTimeMillis()}_${uri.hashCode()}",
                chatId = chatId,
                type = resolvedType,
                caption = caption,
                fileName = meta.name,
                localUri = uri.toString(),
                mimeType = meta.mimeType,
                durationMs = durationMs
            )
            _pendingUploads.update { it + upload }
            executeUpload(context, upload, meta.sizeBytes)
        }
    }

    fun retryUpload(context: Context, tempId: String) {
        val existing = _pendingUploads.value.firstOrNull { it.tempId == tempId } ?: return
        val reset = existing.copy(failed = false, progress = 0f, errorMessage = null)
        _pendingUploads.update { list -> list.map { if (it.tempId == tempId) reset else it } }
        val meta = mediaRepository.getFileMeta(context, Uri.parse(reset.localUri))
        executeUpload(context, reset, meta.sizeBytes)
    }

    fun cancelUpload(tempId: String) {
        mediaRepository.cancelUpload(tempId)
        _pendingUploads.update { list -> list.filterNot { it.tempId == tempId } }
    }

    private fun executeUpload(context: Context, upload: PendingUpload, sizeBytes: Long) {
        val sender = currentUser.value ?: return
        val uri = Uri.parse(upload.localUri)
        val reply = _replyTo.value
        _replyTo.value = null

        viewModelScope.launch {
            val storagePath = "chat_media/$chatId/${System.currentTimeMillis()}_${upload.fileName}"
            val result = if (upload.type == MessageType.IMAGE) {
                val compressed = mediaRepository.compressImageUri(context, uri)
                if (compressed != null) {
                    mediaRepository.uploadBytes(upload.tempId, storagePath, compressed, "image/jpeg") { p ->
                        _pendingUploads.update { list ->
                            list.map { if (it.tempId == upload.tempId) it.copy(progress = p) else it }
                        }
                    }
                } else {
                    mediaRepository.uploadUri(upload.tempId, storagePath, uri, upload.mimeType) { p ->
                        _pendingUploads.update { list ->
                            list.map { if (it.tempId == upload.tempId) it.copy(progress = p) else it }
                        }
                    }
                }
            } else {
                mediaRepository.uploadUri(upload.tempId, storagePath, uri, upload.mimeType) { p ->
                    _pendingUploads.update { list ->
                        list.map { if (it.tempId == upload.tempId) it.copy(progress = p) else it }
                    }
                }
            }

            result.onSuccess { url ->
                _pendingUploads.update { list -> list.filterNot { it.tempId == upload.tempId } }
                chatRepository.sendMessage(
                    chatId = chatId,
                    sender = sender,
                    type = upload.type,
                    text = upload.caption,
                    mediaUrl = url,
                    mediaName = upload.fileName,
                    mediaMimeType = upload.mimeType,
                    mediaSizeBytes = sizeBytes,
                    mediaDurationMs = upload.durationMs,
                    replyTo = reply
                )
            }.onFailure { err ->
                _pendingUploads.update { list ->
                    list.map {
                        if (it.tempId == upload.tempId) {
                            it.copy(failed = true, errorMessage = err.message ?: "Upload failed")
                        } else it
                    }
                }
            }
        }
    }

    fun startVoiceRecording() {
        voiceRecorderManager.startRecording(viewModelScope).onFailure {
            _statusToast.value = "Could not start microphone recording"
        }
    }

    fun cancelVoiceRecording() {
        voiceRecorderManager.stopAndDiscard()
    }

    fun sendVoiceRecording(context: Context) {
        val result = voiceRecorderManager.stopAndGetRecording() ?: return
        val (uri, durationMs) = result
        sendMediaUris(context, listOf(uri), MessageType.VOICE, durationMs = durationMs)
    }

    fun togglePlayAudio(messageId: String, url: String) {
        audioPlayerManager.togglePlayPause(messageId, url, viewModelScope)
    }

    fun seekAudio(messageId: String, positionMs: Long) {
        audioPlayerManager.seekTo(messageId, positionMs)
    }

    fun downloadMedia(context: Context, message: ChatMessage) {
        if (message.mediaUrl.isBlank()) return
        viewModelScope.launch {
            _downloadProgress.update { it + (message.id to 0.05f) }
            val name = message.mediaName.ifBlank { "alokchat_${message.id}" }
            val mime = message.mediaMimeType.ifBlank {
                when (message.type) {
                    MessageType.IMAGE.name -> "image/jpeg"
                    MessageType.VIDEO.name -> "video/mp4"
                    MessageType.PDF.name -> "application/pdf"
                    else -> "application/octet-stream"
                }
            }
            mediaRepository.downloadMediaToDevice(context, message.mediaUrl, name, mime) { progress ->
                _downloadProgress.update { it + (message.id to progress) }
            }.onSuccess {
                _downloadProgress.update { it - message.id }
                _statusToast.value = "Saved to device ($name)"
            }.onFailure { err ->
                _downloadProgress.update { it - message.id }
                _statusToast.value = err.message ?: "Download failed"
            }
        }
    }

    fun setReaction(messageId: String, emoji: String?) {
        viewModelScope.launch {
            chatRepository.setReaction(chatId, messageId, currentUid, emoji)
        }
    }

    fun toggleStarSelected() {
        val selected = messages.value.filter { it.id in _selectedMessageIds.value }
        viewModelScope.launch {
            selected.forEach { msg ->
                val currentlyStarred = msg.starredBy[currentUid] == true
                chatRepository.toggleStarMessage(chatId, msg.id, currentUid, !currentlyStarred)
            }
            clearSelection()
        }
    }

    fun togglePinMessage(msg: ChatMessage) {
        viewModelScope.launch {
            chatRepository.togglePinMessage(chatId, msg.id, !msg.pinned)
            clearSelection()
        }
    }

    fun deleteSelectedForMe() {
        val ids = _selectedMessageIds.value.toList()
        viewModelScope.launch {
            chatRepository.deleteMessagesForMe(chatId, currentUid, ids)
            clearSelection()
        }
    }

    fun deleteSelectedForEveryone() {
        val ids = _selectedMessageIds.value.toList()
        viewModelScope.launch {
            chatRepository.deleteMessagesForEveryone(chatId, ids)
            clearSelection()
        }
    }

    fun forwardSelectedTo(targetChatIds: List<String>) {
        val sender = currentUser.value ?: return
        val selected = messages.value.filter { it.id in _selectedMessageIds.value }
        if (selected.isEmpty() || targetChatIds.isEmpty()) return
        viewModelScope.launch {
            chatRepository.forwardMessages(selected, targetChatIds, sender)
            clearSelection()
            _statusToast.value = "Forwarded to ${targetChatIds.size} chat(s)"
        }
    }

    override fun onCleared() {
        super.onCleared()
        onTypingChanged(false)
        voiceRecorderManager.stopAndDiscard()
        audioPlayerManager.stop()
    }
}
