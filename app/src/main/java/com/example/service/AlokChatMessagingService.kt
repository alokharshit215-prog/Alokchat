package com.example.service

import android.util.Log
import com.example.data.prefs.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlokChatMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance().reference
            .child("users")
            .child(uid)
            .child("fcmToken")
            .setValue(token)
            .addOnFailureListener {
                Log.w(TAG, "Failed to update FCM token: ${it.message}")
            }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        NotificationHelper.createNotificationChannels(applicationContext)

        val data = message.data
        val type = data["type"] ?: "message"
        val title = data["title"] ?: message.notification?.title ?: "Alok Chat"
        val body = data["body"] ?: message.notification?.body ?: "New notification"

        scope.launch {
            val prefs = UserPreferences(applicationContext).settingsFlow.first()
            when (type) {
                "incoming_call" -> {
                    val callId = data["callId"] ?: return@launch
                    val isVideo = data["isVideo"] == "true"
                    NotificationHelper.showIncomingCallNotification(
                        applicationContext,
                        callId,
                        title,
                        isVideo
                    )
                }
                "missed_call" -> {
                    val callId = data["callId"] ?: "missed"
                    val isVideo = data["isVideo"] == "true"
                    NotificationHelper.showMissedCallNotification(
                        applicationContext,
                        callId,
                        title,
                        isVideo
                    )
                }
                else -> {
                    val chatId = data["chatId"] ?: "general"
                    val isGroup = data["isGroup"] == "true"
                    NotificationHelper.showChatNotification(
                        context = applicationContext,
                        chatId = chatId,
                        title = title,
                        body = body,
                        isGroup = isGroup,
                        showPreview = prefs.notificationShowPreview,
                        playSound = prefs.notificationSound
                    )
                }
            }
        }
    }

    private companion object {
        const val TAG = "AlokChatFCM"
    }
}
