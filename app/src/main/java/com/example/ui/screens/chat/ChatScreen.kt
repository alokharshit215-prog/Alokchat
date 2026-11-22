package com.example.ui.screens.chat

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AppSettingsState
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSummary
import com.example.data.model.DeviceContact
import com.example.data.model.MessageType
import com.example.data.model.PendingUpload
import com.example.data.model.PrivacyOption
import com.example.data.model.UserProfile
import com.example.data.repository.UserRepository
import com.example.ui.components.ContactPickerDialog
import com.example.ui.components.EmojiPickerSheet
import com.example.ui.components.ForwardChatsDialog
import com.example.ui.components.FullscreenImageViewer
import com.example.ui.components.UserAvatar
import com.example.ui.components.VideoPlayerDialog
import com.example.ui.theme.AlokDangerRed
import com.example.ui.theme.AlokMint
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.MainViewModel
import com.example.util.FormatUtils
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    mainViewModel: MainViewModel,
    userRepository: UserRepository,
    allChats: List<ChatSummary>,
    settings: AppSettingsState,
    initialTitle: String,
    initialPhoto: String,
    capturedPhotoUri: Uri?,
    onClearCapturedPhoto: () -> Unit,
    onBack: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenGroupInfo: (String) -> Unit,
    onStartVoiceCall: (UserProfile) -> Unit,
    onStartVideoCall: (UserProfile) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val peer by viewModel.peerUser.collectAsStateWithLifecycle()
    val group by viewModel.groupInfo.collectAsStateWithLifecycle()
    val typingIds by viewModel.typingUserIds.collectAsStateWithLifecycle()
    val recordingState by viewModel.recordingState.collectAsStateWithLifecycle()
    val audioPlaybackState by viewModel.audioPlaybackState.collectAsStateWithLifecycle()
    val pendingUploads by viewModel.pendingUploads.collectAsStateWithLifecycle()
    val replyTo by viewModel.replyTo.collectAsStateWithLifecycle()
    val editingMsg by viewModel.editingMessage.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedMessageIds.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val statusToast by viewModel.statusToast.collectAsStateWithLifecycle()
    val blockedUids by mainViewModel.blockedUids.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showAttachSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var searchInChat by remember { mutableStateOf(false) }
    var chatSearchQuery by remember { mutableStateOf("") }
    var showForwardDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showClearChatDialog by remember { mutableStateOf(false) }
    var showDeleteChatDialog by remember { mutableStateOf(false) }
    var showContactPicker by remember { mutableStateOf(false) }
    var deviceContacts by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }
    var previewMediaUris by remember { mutableStateOf<Pair<List<Uri>, MessageType>?>(null) }
    var previewCaption by remember { mutableStateOf("") }
    var viewingImageMsg by remember { mutableStateOf<ChatMessage?>(null) }
    var viewingVideoMsg by remember { mutableStateOf<ChatMessage?>(null) }
    var reactionTargetMsg by remember { mutableStateOf<ChatMessage?>(null) }

    val isBlocked = !viewModel.isGroup && viewModel.peerUid in blockedUids

    BackHandler(enabled = selectedIds.isNotEmpty() || searchInChat || showEmojiPicker) {
        when {
            selectedIds.isNotEmpty() -> viewModel.clearSelection()
            searchInChat -> {
                searchInChat = false
                chatSearchQuery = ""
            }
            showEmojiPicker -> showEmojiPicker = false
        }
    }

    LaunchedEffect(capturedPhotoUri) {
        if (capturedPhotoUri != null) {
            previewMediaUris = listOf(capturedPhotoUri) to MessageType.IMAGE
            onClearCapturedPhoto()
        }
    }

    LaunchedEffect(editingMsg) {
        editingMsg?.let { inputText = it.text }
    }

    LaunchedEffect(statusToast) {
        statusToast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    val visibleMessages = remember(messages, viewModel.currentUid, chatSearchQuery) {
        messages.filter { it.deletedFor[viewModel.currentUid] != true }
            .filter {
                chatSearchQuery.isBlank() || it.text.contains(chatSearchQuery, ignoreCase = true) ||
                    it.mediaName.contains(chatSearchQuery, ignoreCase = true)
            }
    }

    LaunchedEffect(visibleMessages.lastOrNull()?.id, visibleMessages.size) {
        if (visibleMessages.isNotEmpty()) {
            viewModel.markVisibleMessagesRead(visibleMessages)
            listState.animateScrollToItem(visibleMessages.lastIndex)
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.onTypingChanged(false) }
    }

    // Launchers (zero-permission Photo Picker & SAF Document Picker)
    val multiPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10),
        onResult = { uris ->
            if (uris.isNotEmpty()) previewMediaUris = uris to MessageType.IMAGE
        }
    )
    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) previewMediaUris = listOf(uri) to MessageType.VIDEO
        }
    )
    val docPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) viewModel.sendMediaUris(context, listOf(uri), MessageType.DOCUMENT)
        }
    )
    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) viewModel.sendMediaUris(context, listOf(uri), MessageType.PDF)
        }
    )
    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) viewModel.sendMediaUris(context, listOf(uri), MessageType.AUDIO)
        }
    )
    val micPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) viewModel.startVoiceRecording()
            else Toast.makeText(context, "Microphone permission required for voice messages", Toast.LENGTH_SHORT).show()
        }
    )
    val contactPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                scope.launch {
                    deviceContacts = userRepository.readDeviceContactsForSharing(context)
                    showContactPicker = true
                }
            } else {
                Toast.makeText(context, "Contacts permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    )
    @SuppressLint("MissingPermission")
    fun shareCurrentLocation() {
        val client = LocationServices.getFusedLocationProviderClient(context)
        client.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                viewModel.sendLocation(loc.latitude, loc.longitude, "My Current Location")
            } else {
                Toast.makeText(context, "Location unavailable right now. Try again outdoors.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Could not fetch location", Toast.LENGTH_SHORT).show()
        }
    }
    val locationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) shareCurrentLocation()
            else Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    )

    val title = if (viewModel.isGroup) {
        group?.name?.ifBlank { initialTitle } ?: initialTitle
    } else {
        peer?.bestDisplayName()?.ifBlank { initialTitle } ?: initialTitle
    }
    val avatarUrl = if (viewModel.isGroup) {
        group?.photoUrl ?: initialPhoto
    } else {
        val p = peer
        if (p != null && p.privacyPhoto == PrivacyOption.NOBODY.name) "" else (p?.photoUrl ?: initialPhoto)
    }

    val subtitle = when {
        typingIds.isNotEmpty() -> "typing..."
        viewModel.isGroup -> "${group?.members?.size ?: 0} participants"
        peer?.online == true && peer?.privacyOnline != PrivacyOption.NOBODY.name -> "online"
        peer != null && peer?.lastSeen ?: 0L > 0L && peer?.privacyLastSeen != PrivacyOption.NOBODY.name ->
            FormatUtils.formatLastSeen(peer?.lastSeen ?: 0L)
        else -> ""
    }

    val pinnedMessage = remember(visibleMessages) {
        visibleMessages.lastOrNull { it.pinned && !it.deletedForAll }
    }

    Scaffold(
        topBar = {
            if (selectedIds.isNotEmpty()) {
                val selectedMessages = visibleMessages.filter { it.id in selectedIds }
                val single = selectedMessages.singleOrNull()
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    navigationIcon = {
                        IconButton(onClick = viewModel::clearSelection) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Clear selection")
                        }
                    },
                    title = { Text("${selectedIds.size}") },
                    actions = {
                        if (single != null) {
                            IconButton(onClick = {
                                viewModel.setReplyTo(single)
                                viewModel.clearSelection()
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = "Reply")
                            }
                            IconButton(onClick = { reactionTargetMsg = single }) {
                                Icon(Icons.Default.EmojiEmotions, contentDescription = "React")
                            }
                            IconButton(onClick = { viewModel.togglePinMessage(single) }) {
                                Icon(Icons.Default.PushPin, contentDescription = "Pin message")
                            }
                            if (single.senderId == viewModel.currentUid && single.type == MessageType.TEXT.name && !single.deletedForAll) {
                                IconButton(onClick = { viewModel.startEditing(single) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                            }
                        }
                        IconButton(onClick = viewModel::toggleStarSelected) {
                            Icon(Icons.Default.Star, contentDescription = "Star")
                        }
                        IconButton(onClick = {
                            val combined = selectedMessages.filter { it.text.isNotBlank() }.joinToString("\n") { it.text }
                            if (combined.isNotBlank()) {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Alok Chat", combined))
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                            viewModel.clearSelection()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        }
                        IconButton(
                            onClick = { showForwardDialog = true },
                            modifier = Modifier.testTag("forward_selected_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = "Forward")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                )
            } else {
                Column {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White,
                            actionIconContentColor = Color.White
                        ),
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable(enabled = viewModel.isGroup) {
                                    onOpenGroupInfo(viewModel.chatId)
                                }
                            ) {
                                UserAvatar(
                                    photoUrl = avatarUrl,
                                    name = title,
                                    size = 38.dp,
                                    isGroup = viewModel.isGroup,
                                    isOnline = !viewModel.isGroup && peer?.online == true &&
                                        peer?.privacyOnline != PrivacyOption.NOBODY.name
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (subtitle.isNotBlank()) {
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            if (!viewModel.isGroup && peer != null) {
                                IconButton(
                                    onClick = { peer?.let(onStartVideoCall) },
                                    modifier = Modifier.testTag("chat_video_call_button")
                                ) {
                                    Icon(Icons.Default.Videocam, contentDescription = "Video call")
                                }
                                IconButton(
                                    onClick = { peer?.let(onStartVoiceCall) },
                                    modifier = Modifier.testTag("chat_voice_call_button")
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = "Voice call")
                                }
                            }
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Chat menu")
                                }
                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                    if (viewModel.isGroup) {
                                        DropdownMenuItem(
                                            text = { Text("Group info") },
                                            onClick = {
                                                showMenu = false
                                                onOpenGroupInfo(viewModel.chatId)
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Search messages") },
                                        onClick = {
                                            showMenu = false
                                            searchInChat = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Clear chat") },
                                        onClick = {
                                            showMenu = false
                                            showClearChatDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete chat", color = AlokDangerRed) },
                                        onClick = {
                                            showMenu = false
                                            showDeleteChatDialog = true
                                        }
                                    )
                                    if (!viewModel.isGroup && viewModel.peerUid.isNotBlank()) {
                                        DropdownMenuItem(
                                            text = { Text(if (isBlocked) "Unblock user" else "Block user") },
                                            onClick = {
                                                showMenu = false
                                                mainViewModel.toggleBlockUser(viewModel.peerUid, !isBlocked)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Report user") },
                                            onClick = {
                                                showMenu = false
                                                showReportDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )
                    if (searchInChat) {
                        Surface(color = MaterialTheme.colorScheme.surface) {
                            OutlinedTextField(
                                value = chatSearchQuery,
                                onValueChange = { chatSearchQuery = it },
                                placeholder = { Text("Search in conversation...") },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        searchInChat = false
                                        chatSearchQuery = ""
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close search")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    if (pinnedMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val idx = visibleMessages.indexOfFirst { it.id == pinnedMessage.id }
                                    if (idx >= 0) {
                                        scope.launch { listState.animateScrollToItem(idx) }
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.PushPin,
                                    contentDescription = "Pinned",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = pinnedMessage.text.ifBlank { "Pinned media message" },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .navigationBarsPadding()
        ) {
            if (settings.chatWallpaperIndex == 0) {
                Image(
                    painter = painterResource(id = R.drawable.img_splash_pattern_1795369076698),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.32f)
                )
            }

            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    if (visibleMessages.size >= 50) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                TextButton(onClick = viewModel::loadMoreMessages) {
                                    Text("Load earlier messages")
                                }
                            }
                        }
                    }

                    items(visibleMessages, key = { it.id }) { msg ->
                        MessageBubbleItem(
                            message = msg,
                            currentUid = viewModel.currentUid,
                            isGroup = viewModel.isGroup,
                            isSelected = msg.id in selectedIds,
                            themePreference = settings.themePreference,
                            audioPlaybackState = audioPlaybackState,
                            downloadProgress = downloadProgress[msg.id],
                            onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    viewModel.toggleSelectMessage(msg.id)
                                }
                            },
                            onLongClick = { viewModel.toggleSelectMessage(msg.id) },
                            onImageClick = { viewingImageMsg = it },
                            onVideoClick = { viewingVideoMsg = it },
                            onDownloadClick = { viewModel.downloadMedia(context, it) },
                            onToggleAudio = {
                                if (it.mediaUrl.isNotBlank()) {
                                    viewModel.togglePlayAudio(it.id, it.mediaUrl)
                                }
                            },
                            onSeekAudio = viewModel::seekAudio,
                            onReactionClick = { emoji ->
                                viewModel.setReaction(msg.id, emoji.ifBlank { null })
                            }
                        )
                    }

                    items(pendingUploads, key = { it.tempId }) { upload ->
                        PendingUploadBubble(
                            upload = upload,
                            onCancel = { viewModel.cancelUpload(upload.tempId) },
                            onRetry = { viewModel.retryUpload(context, upload.tempId) }
                        )
                    }
                }

                if (isBlocked) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("You blocked this user.", style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { mainViewModel.toggleBlockUser(viewModel.peerUid, false) }) {
                                Text("Unblock")
                            }
                        }
                    }
                } else {
                    replyTo?.let { reply ->
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Replying to ${reply.senderName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        reply.text.ifBlank { reply.type },
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { viewModel.setReplyTo(null) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel reply")
                                }
                            }
                        }
                    }

                    editingMsg?.let {
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Editing message", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    viewModel.cancelEditing()
                                    inputText = ""
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel edit")
                                }
                            }
                        }
                    }

                    if (recordingState.isRecording) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = viewModel::cancelVoiceRecording) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete recording", tint = AlokDangerRed)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(AlokDangerRed)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Recording ${FormatUtils.formatDurationMillis(recordingState.elapsedMs)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.sendVoiceRecording(context) },
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send voice message", tint = Color.White)
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 2.dp,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    IconButton(onClick = { showEmojiPicker = !showEmojiPicker }) {
                                        Icon(
                                            Icons.Default.EmojiEmotions,
                                            contentDescription = "Emoji",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    OutlinedTextField(
                                        value = inputText,
                                        onValueChange = {
                                            inputText = it
                                            viewModel.onTypingChanged(it.isNotBlank())
                                        },
                                        placeholder = { Text("Message") },
                                        maxLines = 4,
                                        keyboardOptions = KeyboardOptions(
                                            imeAction = if (settings.enterIsSend) ImeAction.Send else ImeAction.Default
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onSend = {
                                                if (settings.enterIsSend && inputText.isNotBlank()) {
                                                    viewModel.sendTextOrSaveEdit(inputText)
                                                    inputText = ""
                                                }
                                            }
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("chat_message_input"),
                                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent
                                        )
                                    )
                                    IconButton(
                                        onClick = { showAttachSheet = true },
                                        modifier = Modifier.testTag("chat_attach_button")
                                    ) {
                                        Icon(
                                            Icons.Default.AttachFile,
                                            contentDescription = "Attach",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (inputText.isBlank()) {
                                        IconButton(onClick = onOpenCamera) {
                                            Icon(
                                                Icons.Default.CameraAlt,
                                                contentDescription = "Camera",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    if (inputText.isNotBlank()) {
                                        viewModel.sendTextOrSaveEdit(inputText)
                                        inputText = ""
                                        viewModel.onTypingChanged(false)
                                    } else {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context, Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (granted) viewModel.startVoiceRecording()
                                        else micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .testTag("chat_send_or_mic_button")
                            ) {
                                Icon(
                                    imageVector = if (inputText.isNotBlank()) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                                    contentDescription = if (inputText.isNotBlank()) "Send message" else "Record voice message",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    if (showEmojiPicker) {
                        EmojiPickerSheet(
                            onEmojiSelected = { inputText += it },
                            onDismiss = { showEmojiPicker = false }
                        )
                    }
                }
            }
        }
    }

    if (showAttachSheet) {
        ModalBottomSheet(onDismissRequest = { showAttachSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Share in chat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AttachOption("Camera", Icons.Default.CameraAlt, Color(0xFFD93025)) {
                        showAttachSheet = false
                        onOpenCamera()
                    }
                    AttachOption("Photos", Icons.Default.Collections, Color(0xFF7F5AF0)) {
                        showAttachSheet = false
                        multiPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    AttachOption("Video", Icons.Default.Videocam, Color(0xFFE67E22)) {
                        showAttachSheet = false
                        videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    }
                    AttachOption("PDF", Icons.Default.PictureAsPdf, Color(0xFFC0392B)) {
                        showAttachSheet = false
                        pdfPicker.launch(arrayOf("application/pdf"))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AttachOption("Document", Icons.Default.Description, Color(0xFF2980B9)) {
                        showAttachSheet = false
                        docPicker.launch(arrayOf("*/*"))
                    }
                    AttachOption("Audio", Icons.Default.AudioFile, Color(0xFFF39C12)) {
                        showAttachSheet = false
                        audioPicker.launch(arrayOf("audio/*"))
                    }
                    AttachOption("Contact", Icons.Default.Contacts, Color(0xFF16A085)) {
                        showAttachSheet = false
                        val granted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.READ_CONTACTS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            scope.launch {
                                deviceContacts = userRepository.readDeviceContactsForSharing(context)
                                showContactPicker = true
                            }
                        } else {
                            contactPermLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                    }
                    AttachOption("Location", Icons.Default.LocationOn, AlokMint) {
                        showAttachSheet = false
                        val granted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) shareCurrentLocation()
                        else locationPermLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    previewMediaUris?.let { (uris, type) ->
        AlertDialog(
            onDismissRequest = { previewMediaUris = null; previewCaption = "" },
            title = { Text("Send ${uris.size} ${if (type == MessageType.VIDEO) "video" else "photo(s)"}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (type == MessageType.IMAGE) {
                        AsyncImage(
                            model = uris.first(),
                            contentDescription = "Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Text("Video ready to upload (${uris.size})", style = MaterialTheme.typography.bodyMedium)
                    }
                    OutlinedTextField(
                        value = previewCaption,
                        onValueChange = { previewCaption = it },
                        label = { Text("Add a caption...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.sendMediaUris(context, uris, type, caption = previewCaption)
                    previewMediaUris = null
                    previewCaption = ""
                }) { Text("Send") }
            },
            dismissButton = {
                TextButton(onClick = { previewMediaUris = null; previewCaption = "" }) { Text("Cancel") }
            }
        )
    }

    if (showForwardDialog) {
        ForwardChatsDialog(
            chats = allChats,
            onConfirm = { targetIds ->
                viewModel.forwardSelectedTo(targetIds)
                showForwardDialog = false
            },
            onDismiss = { showForwardDialog = false }
        )
    }

    if (showDeleteDialog) {
        val allMine = visibleMessages.filter { it.id in selectedIds }.all { it.senderId == viewModel.currentUid }
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete ${selectedIds.size} message(s)?") },
            confirmButton = {
                Column {
                    if (allMine) {
                        TextButton(onClick = {
                            viewModel.deleteSelectedForEveryone()
                            showDeleteDialog = false
                        }) { Text("Delete for everyone", color = AlokDangerRed) }
                    }
                    TextButton(onClick = {
                        viewModel.deleteSelectedForMe()
                        showDeleteDialog = false
                    }) { Text("Delete for me") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showReportDialog) {
        var reason by remember { mutableStateOf("Spam or unwanted messages") }
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report user") },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    mainViewModel.reportUser(viewModel.peerUid, reason)
                    showReportDialog = false
                }) { Text("Submit Report") }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showClearChatDialog) {
        AlertDialog(
            onDismissRequest = { showClearChatDialog = false },
            title = { Text("Clear this chat?") },
            text = { Text("Messages in this chat will be cleared for you.") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearChatDialog = false
                        viewModel.clearChat()
                    }
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteChatDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteChatDialog = false },
            title = { Text("Delete chat?") },
            text = { Text("Delete this chat and remove it from your chat list?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteChatDialog = false
                        viewModel.deleteChat {
                            mainViewModel.showToast("Chat deleted")
                            onBack()
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = AlokDangerRed,
                        contentColor = Color.White
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteChatDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showContactPicker) {
        ContactPickerDialog(
            contacts = deviceContacts,
            onSelect = { contact ->
                viewModel.sendContact(contact.name, contact.phone)
                showContactPicker = false
            },
            onDismiss = { showContactPicker = false }
        )
    }

    reactionTargetMsg?.let { msg ->
        AlertDialog(
            onDismissRequest = { reactionTargetMsg = null },
            title = { Text("React to message") },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("❤️", "👍", "😂", "😮", "😢", "🙏").forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 28.sp,
                            modifier = Modifier
                                .clickable {
                                    viewModel.setReaction(msg.id, emoji)
                                    viewModel.clearSelection()
                                    reactionTargetMsg = null
                                }
                                .padding(6.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { reactionTargetMsg = null }) { Text("Close") }
            }
        )
    }

    viewingImageMsg?.let { msg ->
        FullscreenImageViewer(
            imageUrl = msg.mediaUrl,
            title = msg.senderName,
            onDownload = { viewModel.downloadMedia(context, msg) },
            onDismiss = { viewingImageMsg = null }
        )
    }

    viewingVideoMsg?.let { msg ->
        VideoPlayerDialog(
            videoUrl = msg.mediaUrl,
            title = msg.mediaName,
            onDownload = { viewModel.downloadMedia(context, msg) },
            onDismiss = { viewingVideoMsg = null }
        )
    }
}

@Composable
private fun AttachOption(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(tint),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun PendingUploadBubble(
    upload: PendingUpload,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.width(240.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (upload.failed) "Upload failed: ${upload.fileName}" else "Uploading ${upload.fileName}",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (upload.failed) {
                        IconButton(onClick = onRetry, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry upload", modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel upload", modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { upload.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
