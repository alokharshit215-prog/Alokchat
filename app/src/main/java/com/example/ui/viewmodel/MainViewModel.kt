package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppSettingsState
import com.example.data.model.CallHistoryEntry
import com.example.data.model.CallSession
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSummary
import com.example.data.model.FontSizePreference
import com.example.data.model.MessageStatus
import com.example.data.model.PrivacyOption
import com.example.data.model.StatusItem
import com.example.data.model.StatusPrivacyMode
import com.example.data.model.StatusType
import com.example.data.model.StatusViewer
import com.example.data.model.ThemePreference
import com.example.data.model.UserProfile
import com.example.data.prefs.UserPreferences
import com.example.data.repository.AuthRepository
import com.example.data.repository.CallRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.GroupRepository
import com.example.data.repository.MediaRepository
import com.example.data.repository.StatusRepository
import com.example.data.repository.UserRepository
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val currentUid: String,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository,
    private val groupRepository: GroupRepository,
    private val statusRepository: StatusRepository,
    private val callRepository: CallRepository,
    private val mediaRepository: MediaRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private var presenceListener: ValueEventListener? = null
    private val deliveredSyncTimestamps = mutableMapOf<String, Long>()

    val settings: StateFlow<AppSettingsState> = userPreferences.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppSettingsState())

    val currentUser: StateFlow<UserProfile?> = userRepository.observeUser(currentUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val chats: StateFlow<List<ChatSummary>> = chatRepository.observeUserChats(currentUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val blockedUids: StateFlow<Set<String>> = userRepository.observeBlockedUsers(currentUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val savedContactUids: StateFlow<Set<String>> = userRepository.observeSavedContactUids(currentUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val myContactAndChatUids: StateFlow<Set<String>> = combine(chats, savedContactUids) { chatList, saved ->
        val chatPeers = chatList.filter { !it.group && it.peerUid.isNotBlank() }.map { it.peerUid }
        (chatPeers + saved).toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val activeStatuses: StateFlow<List<StatusItem>> = combine(blockedUids, myContactAndChatUids) { blocked, contacts ->
        blocked to contacts
    }.flatMapLatest { (blocked, contacts) ->
        statusRepository.observeActiveStatuses(currentUid, blocked, contacts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val callHistory: StateFlow<List<CallHistoryEntry>> = callRepository.observeCallHistory(currentUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val incomingCall: StateFlow<CallSession?> = callRepository.observeIncomingCalls(currentUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val _searchResults = MutableStateFlow<List<UserProfile>>(emptyList())
    val searchResults: StateFlow<List<UserProfile>> = _searchResults.asStateFlow()

    private val _allUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    val allUsers: StateFlow<List<UserProfile>> = _allUsers.asStateFlow()

    private val _contactMatches = MutableStateFlow<List<UserProfile>>(emptyList())
    val contactMatches: StateFlow<List<UserProfile>> = _contactMatches.asStateFlow()

    private val _starredMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val starredMessages: StateFlow<List<ChatMessage>> = _starredMessages.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    val totalUnreadCount: StateFlow<Int> = combine(chats, blockedUids) { chatList, blocked ->
        chatList.filter { it.peerUid !in blocked }.sumOf { it.unreadCount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    init {
        viewModelScope.launch {
            val authUser = authRepository.currentUser
            userRepository.ensureUserRecord(
                uid = currentUid,
                email = authUser?.email.orEmpty(),
                phone = authUser?.phoneNumber.orEmpty()
            )
        }
        presenceListener = userRepository.startPresenceTracking(currentUid)
        userRepository.syncFcmToken(currentUid)
        loadDirectoryUsers()
        viewModelScope.launch {
            chats.collect { chatList ->
                chatList.forEach { summary ->
                    if (summary.lastMessageSenderId.isNotBlank() &&
                        summary.lastMessageSenderId != currentUid &&
                        (summary.unreadCount > 0 || summary.lastMessageStatus == MessageStatus.SENT.name)
                    ) {
                        val lastSyncedAt = deliveredSyncTimestamps[summary.chatId] ?: 0L
                        if (summary.lastMessageAt > lastSyncedAt) {
                            deliveredSyncTimestamps[summary.chatId] = summary.lastMessageAt
                            launch {
                                chatRepository.markChatMessagesDelivered(summary.chatId, currentUid)
                            }
                        }
                    }
                }
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun loadDirectoryUsers() {
        viewModelScope.launch {
            _allUsers.value = userRepository.listAllOtherUsers(currentUid)
        }
    }

    fun searchUsers(query: String) {
        viewModelScope.launch {
            _searchResults.value = if (query.isBlank()) {
                _allUsers.value
            } else {
                userRepository.searchRegisteredUsers(query, currentUid)
            }
        }
    }

    fun discoverDeviceContacts(context: Context) {
        viewModelScope.launch {
            _isBusy.value = true
            val matches = userRepository.findRegisteredContacts(context, currentUid)
            _contactMatches.value = matches
            _isBusy.value = false
            _toastMessage.value = if (matches.isEmpty()) {
                "No contacts found on Alok Chat yet. Share an invite!"
            } else {
                "Found ${matches.size} contact(s) on Alok Chat"
            }
        }
    }

    fun openDirectChat(peer: UserProfile, onOpened: (String) -> Unit) {
        val me = currentUser.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            chatRepository.openOrCreateDirectChat(me, peer)
                .onSuccess { chatId ->
                    _isBusy.value = false
                    onOpened(chatId)
                }
                .onFailure { err ->
                    _isBusy.value = false
                    _toastMessage.value = err.message ?: "Could not open chat"
                }
        }
    }

    fun sendDirectMessageText(peer: UserProfile, text: String, onOpened: (String) -> Unit) {
        val me = currentUser.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            chatRepository.openOrCreateDirectChat(me, peer)
                .onSuccess { chatId ->
                    chatRepository.sendMessage(
                        chatId = chatId,
                        sender = me,
                        type = com.example.data.model.MessageType.TEXT,
                        text = text
                    )
                    _isBusy.value = false
                    onOpened(chatId)
                }
                .onFailure { err ->
                    _isBusy.value = false
                    _toastMessage.value = err.message ?: "Could not send reply"
                }
        }
    }

    fun createGroup(
        name: String,
        description: String,
        photoBitmap: Bitmap?,
        memberUids: List<String>,
        onCreated: (String) -> Unit
    ) {
        val me = currentUser.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            var photoUrl = ""
            if (photoBitmap != null) {
                val bytes = mediaRepository.compressBitmapToJpegBytes(photoBitmap)
                val path = "group_photos/temp_${System.currentTimeMillis()}.jpg"
                mediaRepository.uploadBytes("grp_${System.currentTimeMillis()}", path, bytes, "image/jpeg")
                    .onSuccess { photoUrl = it }
            }
            groupRepository.createGroup(me, name, description, photoUrl, memberUids)
                .onSuccess { groupId ->
                    _isBusy.value = false
                    onCreated(groupId)
                }
                .onFailure { err ->
                    _isBusy.value = false
                    _toastMessage.value = err.message ?: "Failed to create group"
                }
        }
    }

    fun updateProfile(
        displayName: String,
        username: String,
        about: String,
        phone: String? = null,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            userRepository.updateProfile(currentUid, displayName, username, about, phone = phone)
                .onSuccess {
                    _isBusy.value = false
                    _toastMessage.value = "Profile updated"
                    onDone()
                }
                .onFailure { err ->
                    _isBusy.value = false
                    _toastMessage.value = err.message ?: "Could not update profile"
                }
        }
    }

    fun uploadProfilePhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            _isBusy.value = true
            val bytes = mediaRepository.compressBitmapToJpegBytes(bitmap, 85)
            val path = "profile_photos/$currentUid/avatar_${System.currentTimeMillis()}.jpg"
            mediaRepository.uploadBytes("avatar_$currentUid", path, bytes, "image/jpeg")
                .onSuccess { url ->
                    userRepository.updatePhotoUrl(currentUid, url)
                    _isBusy.value = false
                    _toastMessage.value = "Profile photo updated"
                }
                .onFailure { err ->
                    _isBusy.value = false
                    _toastMessage.value = err.message ?: "Failed to upload photo"
                }
        }
    }

    fun removeProfilePhoto() {
        viewModelScope.launch {
            userRepository.updatePhotoUrl(currentUid, "")
            _toastMessage.value = "Profile photo removed"
        }
    }

    fun postTextStatus(
        text: String,
        bgColor: Long,
        privacyMode: StatusPrivacyMode,
        excludedUids: Set<String> = emptySet(),
        allowedUids: Set<String> = emptySet(),
        onDone: () -> Unit
    ) {
        val me = currentUser.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            statusRepository.postStatus(
                user = me,
                type = StatusType.TEXT,
                text = text,
                backgroundColorHex = bgColor,
                privacyMode = privacyMode,
                excludedUids = excludedUids,
                allowedUids = allowedUids,
                contactUids = myContactAndChatUids.value
            ).onSuccess {
                _isBusy.value = false
                _toastMessage.value = "Status published"
                onDone()
            }.onFailure { err ->
                _isBusy.value = false
                _toastMessage.value = err.message ?: "Failed to post status"
            }
        }
    }

    fun postMediaStatus(
        context: Context,
        uri: Uri,
        isVideo: Boolean,
        caption: String,
        privacyMode: StatusPrivacyMode,
        excludedUids: Set<String> = emptySet(),
        allowedUids: Set<String> = emptySet(),
        onDone: () -> Unit
    ) {
        val me = currentUser.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val path = "status_media/$currentUid/${System.currentTimeMillis()}"
            val uploadResult = if (isVideo) {
                mediaRepository.uploadUri("status_${System.currentTimeMillis()}", "$path.mp4", uri, "video/mp4")
            } else {
                val bytes = mediaRepository.compressImageUri(context, uri)
                if (bytes != null) {
                    mediaRepository.uploadBytes("status_${System.currentTimeMillis()}", "$path.jpg", bytes, "image/jpeg")
                } else {
                    mediaRepository.uploadUri("status_${System.currentTimeMillis()}", "$path.jpg", uri, "image/jpeg")
                }
            }
            uploadResult.onSuccess { url ->
                statusRepository.postStatus(
                    user = me,
                    type = if (isVideo) StatusType.VIDEO else StatusType.IMAGE,
                    mediaUrl = url,
                    caption = caption,
                    privacyMode = privacyMode,
                    excludedUids = excludedUids,
                    allowedUids = allowedUids,
                    contactUids = myContactAndChatUids.value
                ).onSuccess {
                    _isBusy.value = false
                    _toastMessage.value = "Status published"
                    onDone()
                }.onFailure { err ->
                    _isBusy.value = false
                    _toastMessage.value = err.message
                }
            }.onFailure { err ->
                _isBusy.value = false
                _toastMessage.value = err.message ?: "Status upload failed"
            }
        }
    }

    fun markStatusViewed(status: StatusItem) {
        val me = currentUser.value ?: return
        viewModelScope.launch {
            statusRepository.markStatusViewed(status.uid, status.id, me)
        }
    }

    fun observeStatusViewers(ownerUid: String, statusId: String): Flow<List<StatusViewer>> {
        return statusRepository.observeStatusViewers(ownerUid, statusId)
    }

    fun deleteStatus(statusId: String) {
        viewModelScope.launch {
            statusRepository.deleteStatus(currentUid, statusId)
            _toastMessage.value = "Status deleted"
        }
    }

    fun toggleBlockUser(targetUid: String, block: Boolean) {
        viewModelScope.launch {
            userRepository.setUserBlocked(currentUid, targetUid, block)
            _toastMessage.value = if (block) "User blocked" else "User unblocked"
        }
    }

    fun reportUser(targetUid: String, reason: String) {
        viewModelScope.launch {
            userRepository.reportUser(currentUid, targetUid, reason)
            _toastMessage.value = "Report submitted. Thank you."
        }
    }

    fun updatePrivacy(
        photo: PrivacyOption,
        lastSeen: PrivacyOption,
        online: PrivacyOption,
        status: PrivacyOption,
        readReceipts: Boolean
    ) {
        viewModelScope.launch {
            userRepository.updatePrivacySettings(currentUid, photo, lastSeen, online, status, readReceipts)
            _toastMessage.value = "Privacy settings saved"
        }
    }

    fun updateStatusPrivacy(
        mode: StatusPrivacyMode,
        excludedUids: Set<String>,
        allowedUids: Set<String>
    ) {
        viewModelScope.launch {
            userRepository.updateStatusPrivacySettings(currentUid, mode, excludedUids, allowedUids)
                .onSuccess {
                    _toastMessage.value = "Status privacy updated"
                }
                .onFailure { err ->
                    _toastMessage.value = err.message ?: "Could not update status privacy"
                }
        }
    }

    fun togglePinChat(chatId: String, pinned: Boolean) {
        viewModelScope.launch { chatRepository.togglePinChat(currentUid, chatId, pinned) }
    }

    fun toggleMuteChat(chatId: String, muted: Boolean) {
        viewModelScope.launch { chatRepository.toggleMuteChat(currentUid, chatId, muted) }
    }

    fun loadStarredMessages() {
        viewModelScope.launch {
            _starredMessages.value = chatRepository.loadStarredMessages(currentUid)
        }
    }

    fun setTheme(theme: ThemePreference) = viewModelScope.launch { userPreferences.setTheme(theme) }
    fun setFontSize(size: FontSizePreference) = viewModelScope.launch { userPreferences.setFontSize(size) }
    fun setEnterIsSend(enabled: Boolean) = viewModelScope.launch { userPreferences.setEnterIsSend(enabled) }
    fun setNotificationSound(enabled: Boolean) = viewModelScope.launch { userPreferences.setNotificationSound(enabled) }
    fun setNotificationVibrate(enabled: Boolean) = viewModelScope.launch { userPreferences.setNotificationVibrate(enabled) }
    fun setNotificationPreview(enabled: Boolean) = viewModelScope.launch { userPreferences.setNotificationShowPreview(enabled) }
    fun setAutoDownloadWifi(enabled: Boolean) = viewModelScope.launch { userPreferences.setAutoDownloadWifi(enabled) }
    fun setAutoDownloadCellular(enabled: Boolean) = viewModelScope.launch { userPreferences.setAutoDownloadCellular(enabled) }
    fun setChatWallpaper(idx: Int) = viewModelScope.launch { userPreferences.setChatWallpaper(idx) }

    fun getCacheSizeBytes(context: Context): Long = mediaRepository.getCacheSizeBytes(context)

    fun clearMediaCache(context: Context) {
        mediaRepository.clearAppCache(context)
        _toastMessage.value = "Local media cache cleared"
    }

    fun signOut() {
        viewModelScope.launch {
            userRepository.stopPresenceTracking(currentUid, presenceListener)
            presenceListener = null
            userRepository.clearFcmToken(currentUid)
            authRepository.signOut()
        }
    }

    override fun onCleared() {
        super.onCleared()
        userRepository.stopPresenceTracking(currentUid, presenceListener)
    }
}
