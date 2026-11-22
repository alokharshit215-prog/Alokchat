package com.example.ui.screens.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddIcCall
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.CallDirection
import com.example.data.model.CallHistoryEntry
import com.example.data.model.ChatSummary
import com.example.data.model.MessageStatus
import com.example.data.model.StatusItem
import com.example.data.model.StatusPrivacyMode
import com.example.data.model.UserProfile
import com.example.ui.components.AlokBrandLogo
import com.example.ui.components.MessageStatusTicks
import com.example.ui.components.UserAvatar
import com.example.ui.screens.status.StatusPrivacySettingsDialog
import com.example.ui.screens.status.formatStatusPrivacySummary
import com.example.ui.theme.AlokDangerRed
import com.example.ui.theme.AlokMint
import com.example.ui.viewmodel.MainViewModel
import com.example.util.FormatUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenChat: (ChatSummary) -> Unit,
    onOpenUserSearch: () -> Unit,
    onOpenCreateGroup: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenCreateTextStatus: () -> Unit,
    onOpenCreateMediaStatus: () -> Unit,
    onViewUserStatuses: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenStarred: () -> Unit,
    onOpenAbout: () -> Unit,
    onStartVoiceCall: (UserProfile) -> Unit,
    onStartVideoCall: (UserProfile) -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val chats by viewModel.chats.collectAsStateWithLifecycle()
    val blockedUids by viewModel.blockedUids.collectAsStateWithLifecycle()
    val statuses by viewModel.activeStatuses.collectAsStateWithLifecycle()
    val callHistory by viewModel.callHistory.collectAsStateWithLifecycle()
    val totalUnread by viewModel.totalUnreadCount.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val contactUids by viewModel.myContactAndChatUids.collectAsStateWithLifecycle()

    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    var chatContextTarget by remember { mutableStateOf<ChatSummary?>(null) }
    var deleteChatConfirmTarget by remember { mutableStateOf<ChatSummary?>(null) }
    var showStatusPrivacyDialog by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    val scope = rememberCoroutineScope()

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val filteredChats = remember(chats, blockedUids, searchQuery) {
        chats.filter { it.peerUid !in blockedUids }
            .filter {
                searchQuery.isBlank() ||
                    it.title.contains(searchQuery, ignoreCase = true) ||
                    it.lastMessage.contains(searchQuery, ignoreCase = true)
            }
    }

    val filteredCalls = remember(callHistory, searchQuery) {
        callHistory.filter {
            searchQuery.isBlank() || it.peerName.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AlokBrandLogo(size = 32.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "ALOK CHAT",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onOpenCamera,
                            modifier = Modifier.testTag("home_camera_button")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Camera")
                        }
                        IconButton(
                            onClick = {
                                searchActive = !searchActive
                                if (!searchActive) searchQuery = ""
                            },
                            modifier = Modifier.testTag("home_search_button")
                        ) {
                            Icon(
                                if (searchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onOpenProfile)
                                .testTag("home_profile_avatar")
                        ) {
                            UserAvatar(
                                photoUrl = currentUser?.photoUrl.orEmpty(),
                                name = currentUser?.displayName.orEmpty(),
                                size = 32.dp
                            )
                        }
                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.testTag("home_menu_button")
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("New group") },
                                    onClick = {
                                        menuExpanded = false
                                        onOpenCreateGroup()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Find people") },
                                    onClick = {
                                        menuExpanded = false
                                        onOpenUserSearch()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Starred messages") },
                                    onClick = {
                                        menuExpanded = false
                                        onOpenStarred()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Status privacy") },
                                    onClick = {
                                        menuExpanded = false
                                        showStatusPrivacyDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Settings") },
                                    onClick = {
                                        menuExpanded = false
                                        onOpenSettings()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("About Alok Chat") },
                                    onClick = {
                                        menuExpanded = false
                                        onOpenAbout()
                                    }
                                )
                            }
                        }
                    }
                )
                if (searchActive) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search chats, updates, or calls...") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("home_search_input")
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (totalUnread > 0) {
                                    Badge { Text(if (totalUnread > 99) "99+" else "$totalUnread") }
                                }
                            }
                        ) {
                            Icon(
                                if (pagerState.currentPage == 0) Icons.Filled.Chat else Icons.Outlined.Chat,
                                contentDescription = "Chats"
                            )
                        }
                    },
                    label = { Text("Chats") },
                    modifier = Modifier.testTag("nav_chats_tab")
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    icon = {
                        Icon(
                            if (pagerState.currentPage == 1) Icons.Filled.Update else Icons.Outlined.Update,
                            contentDescription = "Updates"
                        )
                    },
                    label = { Text("Updates") },
                    modifier = Modifier.testTag("nav_updates_tab")
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 2,
                    onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                    icon = {
                        Icon(
                            if (pagerState.currentPage == 2) Icons.Filled.Phone else Icons.Outlined.Call,
                            contentDescription = "Calls"
                        )
                    },
                    label = { Text("Calls") },
                    modifier = Modifier.testTag("nav_calls_tab")
                )
            }
        },
        floatingActionButton = {
            when (pagerState.currentPage) {
                0 -> FloatingActionButton(
                    onClick = onOpenUserSearch,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("new_chat_fab")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "New Chat")
                }
                1 -> Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = onOpenCreateTextStatus,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.testTag("text_status_fab")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Text status")
                    }
                    FloatingActionButton(
                        onClick = onOpenCreateMediaStatus,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("media_status_fab")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Media status")
                    }
                }
                2 -> FloatingActionButton(
                    onClick = onOpenUserSearch,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("new_call_fab")
                ) {
                    Icon(Icons.Default.AddIcCall, contentDescription = "New Call")
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            when (page) {
                0 -> ChatsListPage(
                    chats = filteredChats,
                    currentUid = currentUser?.uid.orEmpty(),
                    onChatClick = onOpenChat,
                    onChatLongPress = { chatContextTarget = it }
                )
                1 -> UpdatesStatusPage(
                    currentUser = currentUser,
                    statuses = statuses,
                    searchQuery = searchQuery,
                    onAddStatus = onOpenCreateMediaStatus,
                    onOpenStatusPrivacy = { showStatusPrivacyDialog = true },
                    onViewStatuses = onViewUserStatuses
                )
                2 -> CallsHistoryPage(
                    calls = filteredCalls,
                    usersByUid = allUsers.associateBy { it.uid },
                    onVoiceCall = onStartVoiceCall,
                    onVideoCall = onStartVideoCall
                )
            }
        }
    }

    chatContextTarget?.let { target ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { chatContextTarget = null },
            title = { Text(target.title.ifBlank { "Chat options" }) },
            text = {
                Column {
                    DropdownMenuItem(
                        text = { Text(if (target.pinned) "Unpin chat" else "Pin chat") },
                        leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) },
                        onClick = {
                            viewModel.togglePinChat(target.chatId, !target.pinned)
                            chatContextTarget = null
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (target.muted) "Unmute notifications" else "Mute notifications") },
                        leadingIcon = { Icon(Icons.Default.NotificationsOff, contentDescription = null) },
                        onClick = {
                            viewModel.toggleMuteChat(target.chatId, !target.muted)
                            chatContextTarget = null
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete chat", color = AlokDangerRed) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = AlokDangerRed) },
                        onClick = {
                            deleteChatConfirmTarget = target
                            chatContextTarget = null
                        }
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { chatContextTarget = null }) {
                    Text("Close")
                }
            }
        )
    }

    deleteChatConfirmTarget?.let { target ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteChatConfirmTarget = null },
            title = { Text("Delete chat?") },
            text = {
                Text("Delete chat with ${target.title.ifBlank { "this user" }}? All messages in this chat will be removed from your chat list.")
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        viewModel.deleteChat(target.chatId)
                        deleteChatConfirmTarget = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = AlokDangerRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_delete_chat_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { deleteChatConfirmTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showStatusPrivacyDialog) {
        val u = currentUser
        StatusPrivacySettingsDialog(
            initialMode = u?.resolvedStatusPrivacyMode() ?: StatusPrivacyMode.EVERYONE,
            initialExcludedUids = u?.statusExcludedUids?.filterValues { it }?.keys ?: emptySet(),
            initialAllowedUids = u?.statusAllowedUids?.filterValues { it }?.keys ?: emptySet(),
            allUsers = allUsers,
            contactUids = contactUids,
            onSave = { mode, excl, allow ->
                viewModel.updateStatusPrivacy(mode, excl, allow)
                showStatusPrivacyDialog = false
            },
            onDismiss = { showStatusPrivacyDialog = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatsListPage(
    chats: List<ChatSummary>,
    currentUid: String,
    onChatClick: (ChatSummary) -> Unit,
    onChatLongPress: (ChatSummary) -> Unit
) {
    if (chats.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_chats_1795369056971),
                contentDescription = "No chats yet",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(24.dp))
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Welcome to ALOK CHAT",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Tap the New Chat button below to search by @username, email, or name and start messaging.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(chats, key = { it.chatId }) { chat ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onChatClick(chat) },
                        onLongClick = { onChatLongPress(chat) }
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("chat_row_${chat.chatId}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    photoUrl = chat.photoUrl,
                    name = chat.title,
                    size = 52.dp,
                    isGroup = chat.group
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = chat.title.ifBlank { "Alok Chat User" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = FormatUtils.formatChatListTime(chat.lastMessageAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (chat.unreadCount > 0) AlokMint else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (chat.lastMessageSenderId == currentUid && chat.lastMessage.isNotBlank()) {
                            MessageStatusTicks(
                                isDelivered = chat.lastMessageStatus == MessageStatus.DELIVERED.name ||
                                    chat.lastMessageStatus == MessageStatus.READ.name,
                                isRead = chat.lastMessageStatus == MessageStatus.READ.name
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = chat.lastMessage.ifBlank { "Tap to start chatting" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (chat.pinned) {
                            Icon(
                                Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp).padding(end = 2.dp)
                            )
                        }
                        if (chat.muted) {
                            Icon(
                                Icons.Default.NotificationsOff,
                                contentDescription = "Muted",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp).padding(end = 4.dp)
                            )
                        }
                        if (chat.unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(AlokMint)
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = chat.unreadCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(start = 82.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun UpdatesStatusPage(
    currentUser: UserProfile?,
    statuses: List<StatusItem>,
    searchQuery: String,
    onAddStatus: () -> Unit,
    onOpenStatusPrivacy: () -> Unit,
    onViewStatuses: (String) -> Unit
) {
    val myUid = currentUser?.uid.orEmpty()
    val myStatuses = statuses.filter { it.uid == myUid }
    val othersGrouped = remember(statuses, myUid, searchQuery) {
        statuses.filter { it.uid != myUid }
            .filter { searchQuery.isBlank() || it.userName.contains(searchQuery, ignoreCase = true) }
            .groupBy { it.uid }
    }
    val statusMode = currentUser?.resolvedStatusPrivacyMode() ?: StatusPrivacyMode.EVERYONE
    val excludedCount = currentUser?.statusExcludedUids?.count { it.value } ?: 0
    val allowedCount = currentUser?.statusAllowedUids?.count { it.value } ?: 0
    val privacyLabel = formatStatusPrivacySummary(statusMode, excludedCount, allowedCount)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onOpenStatusPrivacy)
                        .testTag("updates_status_privacy_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Status privacy",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = privacyLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        if (myStatuses.isNotEmpty()) onViewStatuses(myUid) else onAddStatus()
                    }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    UserAvatar(
                        photoUrl = currentUser?.photoUrl.orEmpty(),
                        name = currentUser?.displayName.orEmpty(),
                        size = 54.dp,
                        modifier = if (myStatuses.isNotEmpty()) {
                            Modifier.border(2.5.dp, AlokMint, CircleShape).padding(3.dp)
                        } else Modifier
                    )
                    if (myStatuses.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add status",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("My status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (myStatuses.isEmpty()) {
                            "Tap to add a 24-hour status update"
                        } else {
                            "${myStatuses.size} active update(s) • Tap to view"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (othersGrouped.isNotEmpty()) {
            item {
                Text(
                    "Recent updates",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(othersGrouped.entries.toList(), key = { it.key }) { (uid, userStatuses) ->
                val latest = userStatuses.first()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onViewStatuses(uid) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(
                        photoUrl = latest.userPhotoUrl,
                        name = latest.userName,
                        size = 54.dp,
                        modifier = Modifier
                            .border(2.5.dp, AlokMint, CircleShape)
                            .padding(3.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = latest.userName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${userStatuses.size} update(s) • ${FormatUtils.formatChatListTime(latest.createdAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else if (myStatuses.isEmpty()) {
            item {
                Text(
                    text = "No friend status updates yet. Updates disappear automatically after 24 hours.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun CallsHistoryPage(
    calls: List<CallHistoryEntry>,
    usersByUid: Map<String, UserProfile>,
    onVoiceCall: (UserProfile) -> Unit,
    onVideoCall: (UserProfile) -> Unit
) {
    if (calls.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Phone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text("No call history yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Start a voice or video call from any chat or tap the Call button below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(calls, key = { it.id }) { entry ->
            val peer = usersByUid[entry.peerId] ?: UserProfile(
                uid = entry.peerId,
                displayName = entry.peerName,
                photoUrl = entry.peerPhoto
            )
            val isMissed = entry.direction == CallDirection.MISSED.name
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(photoUrl = entry.peerPhoto, name = entry.peerName, size = 48.dp)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.peerName.ifBlank { "Alok Chat User" },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isMissed) AlokDangerRed else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (entry.direction) {
                                CallDirection.OUTGOING.name -> Icons.AutoMirrored.Filled.CallMade
                                CallDirection.MISSED.name -> Icons.AutoMirrored.Filled.CallMissed
                                else -> Icons.AutoMirrored.Filled.CallReceived
                            },
                            contentDescription = entry.direction,
                            tint = if (isMissed) AlokDangerRed else AlokMint,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        val durationSuffix = if (entry.durationSeconds > 0) {
                            " • ${FormatUtils.formatDurationSeconds(entry.durationSeconds)}"
                        } else ""
                        Text(
                            text = "${FormatUtils.formatChatListTime(entry.timestamp)}$durationSuffix",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = {
                        if (entry.video) onVideoCall(peer) else onVoiceCall(peer)
                    }
                ) {
                    Icon(
                        imageVector = if (entry.video) Icons.Default.Videocam else Icons.Default.Phone,
                        contentDescription = "Call back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
