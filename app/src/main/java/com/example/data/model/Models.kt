package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.database.IgnoreExtraProperties

enum class ThemePreference { SYSTEM, LIGHT, DARK }
enum class FontSizePreference(val scale: Float, val label: String) {
    SMALL(0.9f, "Small"),
    MEDIUM(1.0f, "Medium"),
    LARGE(1.15f, "Large")
}

enum class PrivacyOption(val label: String) {
    EVERYONE("Everyone"),
    CONTACTS("My contacts"),
    NOBODY("Nobody")
}

enum class StatusPrivacyMode(val label: String, val description: String) {
    EVERYONE("Everyone", "All registered Alok Chat users"),
    MY_CONTACTS("My contacts", "People in your contacts and chats"),
    MY_CONTACTS_EXCEPT("My contacts except...", "Share with contacts, except selected people"),
    ONLY_SHARE_WITH("Only share with...", "Only selected people can view")
}

enum class MessageType {
    TEXT, IMAGE, VIDEO, DOCUMENT, PDF, AUDIO, VOICE, CONTACT, LOCATION, SYSTEM
}

enum class MessageStatus {
    SENDING, SENT, DELIVERED, READ, FAILED
}

enum class StatusType {
    TEXT, IMAGE, VIDEO
}

enum class CallStatus {
    RINGING, ACCEPTED, REJECTED, ENDED, MISSED
}

enum class CallDirection {
    OUTGOING, INCOMING, MISSED
}

data class CountryCode(
    val name: String,
    val dialCode: String,
    val iso: String,
    val flag: String
)

val DEFAULT_COUNTRY_CODES = listOf(
    CountryCode("India", "+91", "IN", "\uD83C\uDDEE\uD83C\uDDF3"),
    CountryCode("United States", "+1", "US", "\uD83C\uDDFA\uD83C\uDDF8"),
    CountryCode("United Kingdom", "+44", "GB", "\uD83C\uDDEC\uD83C\uDDE7"),
    CountryCode("United Arab Emirates", "+971", "AE", "\uD83C\uDDE6\uD83C\uDDEA"),
    CountryCode("Singapore", "+65", "SG", "\uD83C\uDDF8\uD83C\uDDEC"),
    CountryCode("Australia", "+61", "AU", "\uD83C\uDDE6\uD83C\uDDFA"),
    CountryCode("Canada", "+1", "CA", "\uD83C\uDDE8\uD83C\uDDE6"),
    CountryCode("Germany", "+49", "DE", "\uD83C\uDDE9\uD83C\uDDEA"),
    CountryCode("Bangladesh", "+880", "BD", "\uD83C\uDDE7\uD83C\uDDE9"),
    CountryCode("Nepal", "+977", "NP", "\uD83C\uDDF3\uD83C\uDDF5"),
    CountryCode("Sri Lanka", "+94", "LK", "\uD83C\uDDF1\uD83C\uDDF0"),
    CountryCode("Saudi Arabia", "+966", "SA", "\uD83C\uDDF8\uD83C\uDDE6"),
    CountryCode("Japan", "+81", "JP", "\uD83C\uDDEF\uD83C\uDDF5")
)

@Keep
@IgnoreExtraProperties
data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val phone: String = "",
    val username: String = "",
    val displayName: String = "",
    val about: String = "Hey there! I am using Alok Chat.",
    val photoUrl: String = "",
    val online: Boolean = false,
    val lastSeen: Long = 0L,
    val fcmToken: String = "",
    val createdAt: Long = 0L,
    val privacyPhoto: String = PrivacyOption.EVERYONE.name,
    val privacyLastSeen: String = PrivacyOption.EVERYONE.name,
    val privacyOnline: String = PrivacyOption.EVERYONE.name,
    val privacyStatus: String = PrivacyOption.EVERYONE.name,
    val statusPrivacyMode: String = StatusPrivacyMode.EVERYONE.name,
    val statusExcludedUids: Map<String, Boolean> = emptyMap(),
    val statusAllowedUids: Map<String, Boolean> = emptyMap(),
    val readReceiptsEnabled: Boolean = true
) {
    fun bestDisplayName(): String {
        return displayName.ifBlank {
            email.substringBefore("@").ifBlank {
                phone.ifBlank { username.ifBlank { "Alok Chat User" } }
            }
        }
    }

    fun resolvedStatusPrivacyMode(): StatusPrivacyMode {
        StatusPrivacyMode.entries.firstOrNull { it.name == statusPrivacyMode }?.let { return it }
        return when (privacyStatus) {
            PrivacyOption.CONTACTS.name -> StatusPrivacyMode.MY_CONTACTS
            PrivacyOption.NOBODY.name -> StatusPrivacyMode.ONLY_SHARE_WITH
            else -> StatusPrivacyMode.EVERYONE
        }
    }
}

@Keep
@IgnoreExtraProperties
data class ChatSummary(
    val chatId: String = "",
    val group: Boolean = false,
    val peerUid: String = "",
    val title: String = "",
    val photoUrl: String = "",
    val lastMessage: String = "",
    val lastMessageType: String = MessageType.TEXT.name,
    val lastMessageSenderId: String = "",
    val lastMessageAt: Long = 0L,
    val unreadCount: Int = 0,
    val pinned: Boolean = false,
    val muted: Boolean = false,
    val lastMessageStatus: String = MessageStatus.SENT.name
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val type: String = MessageType.TEXT.name,
    val text: String = "",
    val mediaUrl: String = "",
    val mediaName: String = "",
    val mediaMimeType: String = "",
    val mediaSizeBytes: Long = 0L,
    val mediaDurationMs: Long = 0L,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationLabel: String = "",
    val contactName: String = "",
    val contactPhone: String = "",
    val replyToId: String = "",
    val replyToSender: String = "",
    val replyToPreview: String = "",
    val replyToType: String = "",
    val forwarded: Boolean = false,
    val edited: Boolean = false,
    val deletedForAll: Boolean = false,
    val deletedFor: Map<String, Boolean> = emptyMap(),
    val pinned: Boolean = false,
    val starredBy: Map<String, Boolean> = emptyMap(),
    val reactions: Map<String, String> = emptyMap(),
    val deliveredTo: Map<String, Boolean> = emptyMap(),
    val readBy: Map<String, Boolean> = emptyMap(),
    val timestamp: Long = 0L
)

data class PendingUpload(
    val tempId: String,
    val chatId: String,
    val type: MessageType,
    val caption: String,
    val fileName: String,
    val localUri: String,
    val mimeType: String,
    val durationMs: Long = 0L,
    val progress: Float = 0f,
    val failed: Boolean = false,
    val errorMessage: String? = null
)

@Keep
@IgnoreExtraProperties
data class GroupInfo(
    val groupId: String = "",
    val name: String = "",
    val description: String = "",
    val photoUrl: String = "",
    val createdBy: String = "",
    val createdAt: Long = 0L,
    val members: Map<String, Boolean> = emptyMap(),
    val admins: Map<String, Boolean> = emptyMap()
)

@Keep
@IgnoreExtraProperties
data class StatusItem(
    val id: String = "",
    val uid: String = "",
    val userName: String = "",
    val userPhotoUrl: String = "",
    val type: String = StatusType.TEXT.name,
    val text: String = "",
    val backgroundColorHex: Long = 0xFF008069,
    val mediaUrl: String = "",
    val caption: String = "",
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,
    val privacy: String = StatusPrivacyMode.EVERYONE.name,
    val excludedUids: Map<String, Boolean> = emptyMap(),
    val allowedUids: Map<String, Boolean> = emptyMap(),
    val contactUids: Map<String, Boolean> = emptyMap()
)

@Keep
@IgnoreExtraProperties
data class StatusViewer(
    val uid: String = "",
    val userName: String = "",
    val userPhotoUrl: String = "",
    val viewedAt: Long = 0L
)

@Keep
@IgnoreExtraProperties
data class CallSession(
    val callId: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerPhoto: String = "",
    val calleeId: String = "",
    val calleeName: String = "",
    val calleePhoto: String = "",
    val video: Boolean = false,
    val status: String = CallStatus.RINGING.name,
    val offerSdp: String = "",
    val answerSdp: String = "",
    val createdAt: Long = 0L,
    val startedAt: Long = 0L,
    val endedAt: Long = 0L
)

@Keep
@IgnoreExtraProperties
data class IceCandidateData(
    val sdpMid: String = "",
    val sdpMLineIndex: Int = 0,
    val sdp: String = ""
)

@Keep
@IgnoreExtraProperties
data class CallHistoryEntry(
    val id: String = "",
    val peerId: String = "",
    val peerName: String = "",
    val peerPhoto: String = "",
    val video: Boolean = false,
    val direction: String = CallDirection.OUTGOING.name,
    val durationSeconds: Long = 0L,
    val timestamp: Long = 0L
)

data class DeviceContact(
    val name: String,
    val phone: String
)

data class AppSettingsState(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val fontSizePreference: FontSizePreference = FontSizePreference.MEDIUM,
    val enterIsSend: Boolean = false,
    val notificationSound: Boolean = true,
    val notificationVibrate: Boolean = true,
    val notificationShowPreview: Boolean = true,
    val autoDownloadWifi: Boolean = true,
    val autoDownloadCellular: Boolean = false,
    val chatWallpaperIndex: Int = 0
)
