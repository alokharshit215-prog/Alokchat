package com.example.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {

    const val CHANNEL_MESSAGES = "alok_chat_messages"
    const val CHANNEL_GROUPS = "alok_chat_groups"
    const val CHANNEL_CALLS = "alok_chat_calls"
    const val CHANNEL_MISSED_CALLS = "alok_chat_missed_calls"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val messageChannel = NotificationChannel(
            CHANNEL_MESSAGES,
            "Messages",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Direct chat message notifications"
            enableVibration(true)
        }

        val groupChannel = NotificationChannel(
            CHANNEL_GROUPS,
            "Group Messages",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Group chat notifications"
            enableVibration(true)
        }

        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val callChannel = NotificationChannel(
            CHANNEL_CALLS,
            "Incoming Calls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Incoming voice and video calls"
            enableVibration(true)
            setSound(
                ringtoneUri,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }

        val missedCallChannel = NotificationChannel(
            CHANNEL_MISSED_CALLS,
            "Missed Calls",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Missed voice and video call alerts"
        }

        manager.createNotificationChannels(
            listOf(messageChannel, groupChannel, callChannel, missedCallChannel)
        )
    }

    fun showChatNotification(
        context: Context,
        chatId: String,
        title: String,
        body: String,
        isGroup: Boolean,
        showPreview: Boolean,
        playSound: Boolean
    ) {
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("chatId", chatId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            chatId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (isGroup) CHANNEL_GROUPS else CHANNEL_MESSAGES
        val contentText = if (showPreview) body else "New message on Alok Chat"

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(contentText)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)

        if (!playSound) {
            builder.setSilent(true)
        }

        NotificationManagerCompat.from(context).notify(chatId.hashCode(), builder.build())
    }

    fun showIncomingCallNotification(
        context: Context,
        callId: String,
        callerName: String,
        isVideo: Boolean
    ) {
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("callId", callId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            callId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callType = if (isVideo) "Incoming video call" else "Incoming voice call"
        val notification = NotificationCompat.Builder(context, CHANNEL_CALLS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(callerName)
            .setContentText("$callType • ALOK CHAT")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(callId.hashCode(), notification)
    }

    fun showMissedCallNotification(
        context: Context,
        callId: String,
        callerName: String,
        isVideo: Boolean
    ) {
        if (!hasNotificationPermission(context)) return
        NotificationManagerCompat.from(context).cancel(callId.hashCode())

        val callType = if (isVideo) "Missed video call" else "Missed voice call"
        val notification = NotificationCompat.Builder(context, CHANNEL_MISSED_CALLS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(callType)
            .setContentText("From $callerName")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(("missed_$callId").hashCode(), notification)
    }

    fun cancelNotification(context: Context, id: String) {
        NotificationManagerCompat.from(context).cancel(id.hashCode())
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
