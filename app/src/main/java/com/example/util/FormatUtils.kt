package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object FormatUtils {

    fun deterministicChatId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }

    fun sanitizePhoneKey(phone: String): String {
        return phone.replace(Regex("[^0-9+]"), "")
    }

    fun normalizePhoneE164(raw: String, defaultCountryDial: String = "+91"): String {
        val cleaned = raw.trim().replace(Regex("[\\s\\-().]"), "")
        return when {
            cleaned.startsWith("+") -> cleaned
            cleaned.startsWith("00") -> "+" + cleaned.drop(2)
            cleaned.startsWith("0") && cleaned.length == 11 -> defaultCountryDial + cleaned.drop(1)
            cleaned.length == 10 -> defaultCountryDial + cleaned
            else -> if (cleaned.isNotEmpty()) "$defaultCountryDial$cleaned" else ""
        }
    }

    fun formatMessageTime(timestampMs: Long): String {
        if (timestampMs <= 0L) return ""
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestampMs))
    }

    fun formatChatListTime(timestampMs: Long): String {
        if (timestampMs <= 0L) return ""
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = timestampMs }
        return when {
            now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR) ->
                SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestampMs))

            now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) - target.get(Calendar.DAY_OF_YEAR) == 1 ->
                "Yesterday"

            else -> SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(timestampMs))
        }
    }

    fun formatLastSeen(lastSeenMs: Long): String {
        if (lastSeenMs <= 0L) return "Offline"
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = lastSeenMs }
        val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(lastSeenMs))
        return when {
            now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR) ->
                "last seen today at $timeStr"

            now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) - target.get(Calendar.DAY_OF_YEAR) == 1 ->
                "last seen yesterday at $timeStr"

            else -> "last seen ${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(lastSeenMs))} at $timeStr"
        }
    }

    fun formatDurationSeconds(totalSeconds: Long): String {
        val mins = totalSeconds / 60
        val secs = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }

    fun formatDurationMillis(millis: Long): String {
        return formatDurationSeconds((millis / 1000L).coerceAtLeast(0L))
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return ""
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
