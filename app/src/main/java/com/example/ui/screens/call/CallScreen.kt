package com.example.ui.screens.call

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CallStatus
import com.example.ui.components.UserAvatar
import com.example.ui.theme.AlokDangerRed
import com.example.ui.theme.AlokEmerald
import com.example.ui.theme.AlokEmeraldDark
import com.example.ui.theme.AlokMint
import com.example.ui.viewmodel.CallViewModel
import com.example.util.FormatUtils
import org.webrtc.SurfaceViewRenderer

@Composable
fun CallScreen(
    viewModel: CallViewModel,
    currentUid: String,
    onCallEnded: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val session = state.session

    val isVideo = session?.video == true
    val isIncomingRinging = !state.isOutgoing && session?.status == CallStatus.RINGING.name

    var permissionsGranted by remember { mutableStateOf(false) }
    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { results ->
            permissionsGranted = results.values.all { it }
        }
    )

    LaunchedEffect(isVideo) {
        val needed = if (isVideo) {
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
        } else {
            arrayOf(Manifest.permission.RECORD_AUDIO)
        }
        val allGranted = needed.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) permissionsGranted = true
        else permLauncher.launch(needed)
    }

    LaunchedEffect(state.isEnded) {
        if (state.isEnded) onCallEnded()
    }

    val peerName = if (session == null) "Alok Chat Call"
    else if (session.callerId == currentUid) session.calleeName else session.callerName
    val peerPhoto = if (session == null) ""
    else if (session.callerId == currentUid) session.calleePhoto else session.callerPhoto

    var localRenderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }
    var remoteRenderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }

    LaunchedEffect(localRenderer, remoteRenderer, permissionsGranted) {
        if (isVideo && (localRenderer != null || remoteRenderer != null)) {
            viewModel.attachVideoRenderers(localRenderer, remoteRenderer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(AlokEmeraldDark, Color(0xFF071612)))
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (isVideo && !isIncomingRinging && permissionsGranted) {
            AndroidView(
                factory = { ctx ->
                    SurfaceViewRenderer(ctx).also { remoteRenderer = it }
                },
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(width = 112.dp, height = 160.dp)
            ) {
                AndroidView(
                    factory = { ctx ->
                        SurfaceViewRenderer(ctx).also { localRenderer = it }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Top caller info
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isVideo || isIncomingRinging) {
                UserAvatar(photoUrl = peerPhoto, name = peerName, size = 108.dp)
                Spacer(Modifier.height(18.dp))
            }
            Text(
                text = peerName,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            val statusLabel = if (state.durationSeconds > 0L) {
                FormatUtils.formatDurationSeconds(state.durationSeconds)
            } else {
                state.connectionLabel
            }
            Text(
                text = "$statusLabel • ALOK CHAT",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
        }

        // Bottom controls
        if (isIncomingRinging) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp, start = 40.dp, end = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CallControlButton(
                    icon = Icons.Default.CallEnd,
                    label = "Decline",
                    containerColor = AlokDangerRed,
                    onClick = viewModel::rejectIncomingCall,
                    testTag = "reject_call_button"
                )
                CallControlButton(
                    icon = Icons.Default.Call,
                    label = "Accept",
                    containerColor = AlokMint,
                    onClick = viewModel::acceptIncomingCall,
                    testTag = "accept_call_button"
                )
            }
        } else {
            Surface(
                color = Color.Black.copy(alpha = 0.45f),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 32.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CallControlButton(
                        icon = if (state.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        label = if (state.isMuted) "Unmute" else "Mute",
                        containerColor = if (state.isMuted) Color.White else Color.White.copy(alpha = 0.2f),
                        iconTint = if (state.isMuted) AlokEmeraldDark else Color.White,
                        onClick = viewModel::toggleMute
                    )
                    CallControlButton(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        label = "Speaker",
                        containerColor = if (state.isSpeakerOn) Color.White else Color.White.copy(alpha = 0.2f),
                        iconTint = if (state.isSpeakerOn) AlokEmeraldDark else Color.White,
                        onClick = viewModel::toggleSpeaker
                    )
                    if (isVideo) {
                        CallControlButton(
                            icon = if (state.isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            label = "Video",
                            containerColor = if (!state.isCameraOn) Color.White else Color.White.copy(alpha = 0.2f),
                            iconTint = if (!state.isCameraOn) AlokEmeraldDark else Color.White,
                            onClick = viewModel::toggleCamera
                        )
                        CallControlButton(
                            icon = Icons.Default.Cameraswitch,
                            label = "Flip",
                            containerColor = Color.White.copy(alpha = 0.2f),
                            onClick = viewModel::switchCamera
                        )
                    }
                    CallControlButton(
                        icon = Icons.Default.CallEnd,
                        label = "End",
                        containerColor = AlokDangerRed,
                        onClick = viewModel::endCall,
                        testTag = "end_call_button"
                    )
                }
            }
        }
    }
}

@Composable
private fun CallControlButton(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    iconTint: Color = Color.White,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(containerColor)
                .let { if (testTag.isNotBlank()) it.testTag(testTag) else it }
        ) {
            Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}
