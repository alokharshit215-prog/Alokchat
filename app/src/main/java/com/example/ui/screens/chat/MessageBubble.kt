package com.example.ui.screens.chat

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ChatMessage
import com.example.data.model.MessageType
import com.example.data.model.ThemePreference
import com.example.media.AudioPlaybackState
import com.example.ui.components.MessageStatusTicks
import com.example.ui.theme.AlokIncomingBubbleDark
import com.example.ui.theme.AlokIncomingBubbleLight
import com.example.ui.theme.AlokOutgoingBubbleDark
import com.example.ui.theme.AlokOutgoingBubbleLight
import com.example.util.FormatUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleItem(
    message: ChatMessage,
    currentUid: String,
    isGroup: Boolean,
    isSelected: Boolean,
    themePreference: ThemePreference,
    audioPlaybackState: AudioPlaybackState,
    downloadProgress: Float?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onImageClick: (ChatMessage) -> Unit,
    onVideoClick: (ChatMessage) -> Unit,
    onDownloadClick: (ChatMessage) -> Unit,
    onToggleAudio: (ChatMessage) -> Unit,
    onSeekAudio: (String, Long) -> Unit,
    onReactionClick: (String) -> Unit
) {
    val context = LocalContext.current
    val isDark = when (themePreference) {
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }

    if (message.type == MessageType.SYSTEM.name) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        return
    }

    val isMine = message.senderId == currentUid
    val bubbleColor = when {
        isMine && isDark -> AlokOutgoingBubbleDark
        isMine -> AlokOutgoingBubbleLight
        isDark -> AlokIncomingBubbleDark
        else -> AlokIncomingBubbleLight
    }

    val highlightBg = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    } else {
        Color.Transparent
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(highlightBg)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp, vertical = 3.dp),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMine) 16.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 16.dp
            ),
            tonalElevation = 1.dp,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(min = 84.dp, max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                if (isGroup && !isMine && !message.deletedForAll) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (message.forwarded && !message.deletedForAll) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Filled.Forward,
                            contentDescription = "Forwarded",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Forwarded",
                            style = MaterialTheme.typography.labelSmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                }

                if (message.replyToId.isNotBlank() && !message.deletedForAll) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(modifier = Modifier.padding(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(34.dp)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = message.replyToSender.ifBlank { "Reply" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = message.replyToPreview,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (message.deletedForAll) {
                    Text(
                        text = "This message was deleted",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    when (message.type) {
                        MessageType.IMAGE.name -> {
                            AsyncImage(
                                model = message.mediaUrl,
                                contentDescription = "Photo message",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onImageClick(message) }
                            )
                            if (message.text.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(message.text, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        MessageType.VIDEO.name -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.78f))
                                    .clickable { onVideoClick(message) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Play video",
                                            tint = Color.White,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = message.mediaName.ifBlank { "Video" },
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )
                                }
                            }
                            if (message.text.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(message.text, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        MessageType.VOICE.name, MessageType.AUDIO.name -> {
                            val isActive = audioPlaybackState.activeMessageId == message.id
                            val isPlaying = isActive && audioPlaybackState.isPlaying
                            val totalMs = if (isActive && audioPlaybackState.durationMs > 0) {
                                audioPlaybackState.durationMs
                            } else {
                                message.mediaDurationMs.coerceAtLeast(1000L)
                            }
                            val currentPos = if (isActive) audioPlaybackState.positionMs else 0L

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(
                                    onClick = { onToggleAudio(message) },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(
                                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause audio" else "Play audio",
                                        tint = Color.White
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    if (message.type == MessageType.AUDIO.name && message.mediaName.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.AudioFile,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                message.mediaName,
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Slider(
                                        value = (currentPos.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f),
                                        onValueChange = { frac ->
                                            onSeekAudio(message.id, (frac * totalMs).toLong())
                                        },
                                        modifier = Modifier.height(26.dp)
                                    )
                                    Text(
                                        text = "${FormatUtils.formatDurationMillis(currentPos)} / ${FormatUtils.formatDurationMillis(totalMs)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDownloadClick(message) }) {
                                    if (downloadProgress != null) {
                                        CircularProgressIndicator(
                                            progress = { downloadProgress },
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Download,
                                            contentDescription = "Save audio",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        MessageType.PDF.name, MessageType.DOCUMENT.name -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
                                    .clickable { onDownloadClick(message) }
                                    .padding(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (message.type == MessageType.PDF.name) {
                                        Icons.Default.PictureAsPdf
                                    } else {
                                        Icons.Default.Description
                                    },
                                    contentDescription = null,
                                    tint = if (message.type == MessageType.PDF.name) Color(0xFFD93025) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = message.mediaName.ifBlank { "Document" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = FormatUtils.formatFileSize(message.mediaSizeBytes).ifBlank { "Tap to save" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (downloadProgress != null) {
                                    CircularProgressIndicator(
                                        progress = { downloadProgress },
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.Download, contentDescription = "Download document")
                                }
                            }
                        }

                        MessageType.CONTACT.name -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                    .clickable {
                                        val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${message.contactPhone}"))
                                        context.startActivity(dial)
                                    }
                                    .padding(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.Contacts,
                                    contentDescription = "Contact",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(message.contactName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text(message.contactPhone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        MessageType.LOCATION.name -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                    .clickable {
                                        val geoUri = Uri.parse(
                                            "geo:${message.latitude},${message.longitude}?q=${message.latitude},${message.longitude}(${Uri.encode(message.locationLabel.ifBlank { "Shared Location" })})"
                                        )
                                        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                                        runCatching { context.startActivity(mapIntent) }
                                    }
                                    .padding(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = "Location",
                                    tint = Color(0xFFD93025),
                                    modifier = Modifier.size(30.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = message.locationLabel.ifBlank { "Shared Location" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${String.format("%.4f", message.latitude)}, ${String.format("%.4f", message.longitude)} • Open map",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        else -> {
                            Text(
                                text = message.text,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                Spacer(Modifier.height(3.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (message.starredBy[currentUid] == true) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Starred",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    if (message.edited && !message.deletedForAll) {
                        Text(
                            text = "Edited",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = FormatUtils.formatMessageTime(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isMine && !message.deletedForAll) {
                        val isDelivered = message.deliveredTo.keys.any { it != currentUid }
                        val isRead = message.readBy.keys.any { it != currentUid }
                        MessageStatusTicks(isDelivered = isDelivered, isRead = isRead)
                    }
                }
            }
        }

        if (message.reactions.isNotEmpty() && !message.deletedForAll) {
            val grouped = message.reactions.values.groupingBy { it }.eachCount()
            Surface(
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 2.dp,
                shadowElevation = 1.dp,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clickable {
                        val myReaction = message.reactions[currentUid]
                        if (myReaction != null) onReactionClick("")
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    grouped.forEach { (emoji, count) ->
                        Text(
                            text = if (count > 1) "$emoji $count" else emoji,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
