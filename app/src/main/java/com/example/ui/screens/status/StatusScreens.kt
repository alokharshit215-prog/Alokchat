package com.example.ui.screens.status

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Surface
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.model.StatusPrivacyMode
import com.example.data.model.StatusType
import com.example.data.model.UserProfile
import com.example.ui.components.UserAvatar
import com.example.ui.viewmodel.MainViewModel
import com.example.util.FormatUtils
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTextStatusScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val contactUids by viewModel.myContactAndChatUids.collectAsStateWithLifecycle()

    val palette = remember {
        listOf(0xFF008069L, 0xFF128C7EL, 0xFF075E54L, 0xFF512DA8L, 0xFFC2185BL, 0xFFE65100L)
    }
    var colorIndex by remember { mutableIntStateOf(0) }
    var text by remember { mutableStateOf("") }
    var privacyMode by remember(currentUser?.uid) {
        mutableStateOf(currentUser?.resolvedStatusPrivacyMode() ?: StatusPrivacyMode.EVERYONE)
    }
    var excludedUids by remember(currentUser?.uid) {
        mutableStateOf(currentUser?.statusExcludedUids?.filterValues { it }?.keys ?: emptySet())
    }
    var allowedUids by remember(currentUser?.uid) {
        mutableStateOf(currentUser?.statusAllowedUids?.filterValues { it }?.keys ?: emptySet())
    }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(palette[colorIndex]))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { showPrivacyDialog = true },
                    modifier = Modifier.testTag("status_privacy_button")
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        formatStatusPrivacySummary(privacyMode, excludedUids.size, allowedUids.size),
                        color = Color.White
                    )
                }
                IconButton(onClick = { colorIndex = (colorIndex + 1) % palette.size }) {
                    Icon(Icons.Default.Palette, contentDescription = "Change color", tint = Color.White)
                }
            }
        }

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = {
                Text(
                    "Type a status...",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                color = Color.White,
                textAlign = TextAlign.Center
            ),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .testTag("status_text_input")
        )

        Button(
            onClick = {
                viewModel.postTextStatus(
                    text = text.trim(),
                    bgColor = palette[colorIndex],
                    privacyMode = privacyMode,
                    excludedUids = excludedUids,
                    allowedUids = allowedUids,
                    onDone = onBack
                )
            },
            enabled = text.isNotBlank() && !isBusy,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .testTag("publish_status_button")
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(if (isBusy) "Posting..." else "Post")
        }
    }

    if (showPrivacyDialog) {
        StatusPrivacySettingsDialog(
            initialMode = privacyMode,
            initialExcludedUids = excludedUids,
            initialAllowedUids = allowedUids,
            allUsers = allUsers,
            contactUids = contactUids,
            onSave = { mode, excl, allow ->
                privacyMode = mode
                excludedUids = excl
                allowedUids = allow
                viewModel.updateStatusPrivacy(mode, excl, allow)
                showPrivacyDialog = false
            },
            onDismiss = { showPrivacyDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMediaStatusScreen(
    viewModel: MainViewModel,
    initialCapturedUri: Uri?,
    onClearCaptured: () -> Unit,
    onOpenCamera: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val contactUids by viewModel.myContactAndChatUids.collectAsStateWithLifecycle()

    var selectedUri by remember { mutableStateOf(initialCapturedUri) }
    var isVideo by remember { mutableStateOf(false) }
    var caption by remember { mutableStateOf("") }
    var privacyMode by remember(currentUser?.uid) {
        mutableStateOf(currentUser?.resolvedStatusPrivacyMode() ?: StatusPrivacyMode.EVERYONE)
    }
    var excludedUids by remember(currentUser?.uid) {
        mutableStateOf(currentUser?.statusExcludedUids?.filterValues { it }?.keys ?: emptySet())
    }
    var allowedUids by remember(currentUser?.uid) {
        mutableStateOf(currentUser?.statusAllowedUids?.filterValues { it }?.keys ?: emptySet())
    }
    var showPrivacy by remember { mutableStateOf(false) }

    LaunchedEffect(initialCapturedUri) {
        if (initialCapturedUri != null) {
            selectedUri = initialCapturedUri
            isVideo = false
            onClearCaptured()
        }
    }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedUri = uri
                val mime = context.contentResolver.getType(uri).orEmpty()
                isVideo = mime.startsWith("video/")
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("New Status Update") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Gallery")
                }
                OutlinedButton(
                    onClick = onOpenCamera,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Camera")
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val uri = selectedUri
                if (uri == null) {
                    Text(
                        "Pick a photo or video from Gallery, or snap one with Camera.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                } else if (!isVideo) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Selected status photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("Video selected: ${uri.lastPathSegment}", style = MaterialTheme.typography.titleMedium)
                }
            }

            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                label = { Text("Caption (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { showPrivacy = true },
                    modifier = Modifier.testTag("media_status_privacy_button")
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(formatStatusPrivacySummary(privacyMode, excludedUids.size, allowedUids.size))
                }
                Button(
                    onClick = {
                        selectedUri?.let { uri ->
                            viewModel.postMediaStatus(
                                context = context,
                                uri = uri,
                                isVideo = isVideo,
                                caption = caption.trim(),
                                privacyMode = privacyMode,
                                excludedUids = excludedUids,
                                allowedUids = allowedUids,
                                onDone = onBack
                            )
                        }
                    },
                    enabled = selectedUri != null && !isBusy
                ) {
                    Text(if (isBusy) "Uploading..." else "Post Status")
                }
            }
        }
    }

    if (showPrivacy) {
        StatusPrivacySettingsDialog(
            initialMode = privacyMode,
            initialExcludedUids = excludedUids,
            initialAllowedUids = allowedUids,
            allUsers = allUsers,
            contactUids = contactUids,
            onSave = { mode, excl, allow ->
                privacyMode = mode
                excludedUids = excl
                allowedUids = allow
                viewModel.updateStatusPrivacy(mode, excl, allow)
                showPrivacy = false
            },
            onDismiss = { showPrivacy = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusViewerScreen(
    ownerUid: String,
    currentUid: String,
    viewModel: MainViewModel,
    onReplyToUser: (UserProfile, String) -> Unit,
    onBack: () -> Unit
) {
    val allStatuses by viewModel.activeStatuses.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val ownerStatuses = remember(allStatuses, ownerUid) {
        allStatuses.filter { it.uid == ownerUid }.sortedBy { it.createdAt }
    }
    var currentIndex by remember { mutableIntStateOf(0) }
    var replyText by remember { mutableStateOf("") }
    var showViewersSheet by remember { mutableStateOf(false) }

    if (ownerStatuses.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val safeIndex = currentIndex.coerceIn(0, ownerStatuses.lastIndex)
    val item = ownerStatuses[safeIndex]
    val viewers by viewModel.observeStatusViewers(ownerUid, item.id)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    LaunchedEffect(item.id) {
        viewModel.markStatusViewed(item)
        if (item.type != StatusType.VIDEO.name) {
            delay(5000L)
            if (safeIndex < ownerStatuses.lastIndex) {
                currentIndex = safeIndex + 1
            } else {
                onBack()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (item.type == StatusType.TEXT.name) Color(item.backgroundColorHex) else Color.Black
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        when (item.type) {
            StatusType.TEXT.name -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
            StatusType.IMAGE.name -> {
                AsyncImage(
                    model = item.mediaUrl,
                    contentDescription = item.caption,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            StatusType.VIDEO.name -> {
                val context = LocalContext.current
                val player = remember(item.mediaUrl) {
                    ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(item.mediaUrl))
                        prepare()
                        playWhenReady = true
                    }
                }
                DisposableEffect(player) {
                    onDispose { player.release() }
                }
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Top progress bars & header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(12.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ownerStatuses.forEachIndexed { idx, _ ->
                    LinearProgressIndicator(
                        progress = { if (idx <= safeIndex) 1f else 0f },
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.weight(1f).height(3.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                UserAvatar(photoUrl = item.userPhotoUrl, name = item.userName, size = 38.dp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.userName, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        FormatUtils.formatChatListTime(item.createdAt),
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (ownerUid == currentUid) {
                    IconButton(onClick = {
                        viewModel.deleteStatus(item.id)
                        if (ownerStatuses.size <= 1) onBack()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete status", tint = Color.White)
                    }
                }
            }
        }

        // Left / right tap navigation regions
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 100.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable {
                        if (safeIndex > 0) currentIndex = safeIndex - 1
                    }
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable {
                        if (safeIndex < ownerStatuses.lastIndex) currentIndex = safeIndex + 1 else onBack()
                    }
            )
        }

        // Bottom caption & reply / viewers bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (item.caption.isNotBlank()) {
                Text(
                    text = item.caption,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            if (ownerUid == currentUid) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { showViewersSheet = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("${viewers.size} view(s)", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("Reply to status...", color = Color.White.copy(alpha = 0.7f)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                val ownerProfile = allUsers.firstOrNull { it.uid == ownerUid }
                                    ?: UserProfile(uid = ownerUid, displayName = item.userName, photoUrl = item.userPhotoUrl)
                                onReplyToUser(ownerProfile, "Replied to status: ${replyText.trim()}")
                                replyText = ""
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Reply", tint = Color.White)
                    }
                }
            }
        }
    }

    if (showViewersSheet) {
        ModalBottomSheet(onDismissRequest = { showViewersSheet = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Viewed by (${viewers.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                if (viewers.isEmpty()) {
                    Text("No views yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn {
                        items(viewers, key = { it.uid }) { viewer ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(photoUrl = viewer.userPhotoUrl, name = viewer.userName, size = 40.dp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(viewer.userName, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        FormatUtils.formatChatListTime(viewer.viewedAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

fun formatStatusPrivacySummary(
    mode: StatusPrivacyMode,
    excludedCount: Int,
    allowedCount: Int
): String {
    return when (mode) {
        StatusPrivacyMode.EVERYONE -> "Everyone"
        StatusPrivacyMode.MY_CONTACTS -> "My contacts"
        StatusPrivacyMode.MY_CONTACTS_EXCEPT -> if (excludedCount > 0) "Contacts except ($excludedCount)" else "My contacts except..."
        StatusPrivacyMode.ONLY_SHARE_WITH -> if (allowedCount > 0) "Only share ($allowedCount)" else "Only share with..."
    }
}

@Composable
fun StatusPrivacySettingsDialog(
    initialMode: StatusPrivacyMode,
    initialExcludedUids: Set<String>,
    initialAllowedUids: Set<String>,
    allUsers: List<UserProfile>,
    contactUids: Set<String>,
    onSave: (StatusPrivacyMode, Set<String>, Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(initialMode) }
    var excludedUids by remember { mutableStateOf(initialExcludedUids) }
    var allowedUids by remember { mutableStateOf(initialAllowedUids) }
    var pickerTargetMode by remember { mutableStateOf<StatusPrivacyMode?>(null) }

    val orderedModes = remember {
        listOf(
            StatusPrivacyMode.MY_CONTACTS,
            StatusPrivacyMode.MY_CONTACTS_EXCEPT,
            StatusPrivacyMode.ONLY_SHARE_WITH,
            StatusPrivacyMode.EVERYONE
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Status privacy") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "Who can see your status updates",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                orderedModes.forEach { mode ->
                    val isSelected = selectedMode == mode
                    val sub = when (mode) {
                        StatusPrivacyMode.MY_CONTACTS_EXCEPT -> if (excludedUids.isEmpty()) {
                            mode.description
                        } else {
                            "${excludedUids.size} contact(s) excluded"
                        }
                        StatusPrivacyMode.ONLY_SHARE_WITH -> if (allowedUids.isEmpty()) {
                            mode.description
                        } else {
                            "Shared with ${allowedUids.size} selected contact(s)"
                        }
                        else -> mode.description
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                selectedMode = mode
                                if (mode == StatusPrivacyMode.MY_CONTACTS_EXCEPT || mode == StatusPrivacyMode.ONLY_SHARE_WITH) {
                                    pickerTargetMode = mode
                                }
                            }
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                            .testTag("status_privacy_option_${mode.name}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                selectedMode = mode
                                if (mode == StatusPrivacyMode.MY_CONTACTS_EXCEPT || mode == StatusPrivacyMode.ONLY_SHARE_WITH) {
                                    pickerTargetMode = mode
                                }
                            }
                        )
                        Spacer(Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mode.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                sub,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (mode == StatusPrivacyMode.MY_CONTACTS_EXCEPT || mode == StatusPrivacyMode.ONLY_SHARE_WITH) {
                            TextButton(onClick = {
                                selectedMode = mode
                                pickerTargetMode = mode
                            }) {
                                Text("Select")
                            }
                        }
                    }
                }
                if (selectedMode == StatusPrivacyMode.ONLY_SHARE_WITH && allowedUids.isEmpty()) {
                    Text(
                        "Select at least one person to share your status with.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedMode, excludedUids, allowedUids) },
                enabled = selectedMode != StatusPrivacyMode.ONLY_SHARE_WITH || allowedUids.isNotEmpty(),
                modifier = Modifier.testTag("save_status_privacy_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    pickerTargetMode?.let { target ->
        val initialSet = if (target == StatusPrivacyMode.MY_CONTACTS_EXCEPT) excludedUids else allowedUids
        val dialogTitle = if (target == StatusPrivacyMode.MY_CONTACTS_EXCEPT) {
            "Hide status from..."
        } else {
            "Only share status with..."
        }
        StatusAudiencePickerDialog(
            title = dialogTitle,
            allUsers = allUsers,
            contactUids = contactUids,
            initialSelected = initialSet,
            onConfirm = { picked ->
                if (target == StatusPrivacyMode.MY_CONTACTS_EXCEPT) {
                    excludedUids = picked
                } else {
                    allowedUids = picked
                }
                pickerTargetMode = null
            },
            onDismiss = { pickerTargetMode = null }
        )
    }
}

@Composable
private fun StatusAudiencePickerDialog(
    title: String,
    allUsers: List<UserProfile>,
    contactUids: Set<String>,
    initialSelected: Set<String>,
    onConfirm: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(initialSelected) }
    var query by remember { mutableStateOf("") }

    val sortedUsers = remember(allUsers, contactUids, query) {
        allUsers
            .filter { u ->
                query.isBlank() ||
                    u.bestDisplayName().contains(query, ignoreCase = true) ||
                    u.username.contains(query, ignoreCase = true) ||
                    u.email.contains(query, ignoreCase = true)
            }
            .sortedWith(
                compareByDescending<UserProfile> { it.uid in contactUids }
                    .thenBy { it.bestDisplayName().lowercase() }
            )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search contacts or users...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (sortedUsers.isEmpty()) {
                    Text(
                        "No users available yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(sortedUsers, key = { it.uid }) { user ->
                            val isChecked = user.uid in selected
                            val isContact = user.uid in contactUids
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selected = if (isChecked) selected - user.uid else selected + user.uid
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(
                                    photoUrl = user.photoUrl,
                                    name = user.bestDisplayName(),
                                    size = 38.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.bestDisplayName(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = buildString {
                                            if (user.username.isNotBlank()) append("@${user.username}")
                                            if (isContact) {
                                                if (isNotEmpty()) append(" • ")
                                                append("Contact")
                                            }
                                        }.ifBlank { user.email },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        selected = if (isChecked) selected - user.uid else selected + user.uid
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selected) }) {
                Text("Done (${selected.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
