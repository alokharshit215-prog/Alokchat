package com.example.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.FontSizePreference
import com.example.data.model.PrivacyOption
import com.example.data.model.StatusPrivacyMode
import com.example.data.model.ThemePreference
import com.example.data.repository.AuthRepository
import com.example.data.repository.UserRepository
import com.example.ui.components.AlokBrandLogo
import com.example.ui.components.AlokCreditFooter
import com.example.ui.components.ImageCropDialog
import com.example.ui.components.UserAvatar
import com.example.ui.screens.status.StatusPrivacySettingsDialog
import com.example.ui.screens.status.formatStatusPrivacySummary
import com.example.ui.theme.AlokDangerRed
import com.example.ui.viewmodel.MainViewModel
import com.example.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    authRepository: AuthRepository,
    userRepository: UserRepository,
    capturedPhotoUri: Uri?,
    onClearCaptured: () -> Unit,
    onOpenCamera: () -> Unit,
    onBack: () -> Unit
) {
    val user by viewModel.currentUser.collectAsStateWithLifecycle()

    var displayName by remember(user) { mutableStateOf(user?.displayName.orEmpty()) }
    var username by remember(user) { mutableStateOf(user?.username.orEmpty()) }
    var about by remember(user) { mutableStateOf(user?.about.orEmpty()) }
    var phone by remember(user) { mutableStateOf(user?.phone.orEmpty()) }
    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }

    LaunchedEffect(capturedPhotoUri) {
        if (capturedPhotoUri != null) {
            pendingCropUri = capturedPhotoUri
            onClearCaptured()
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) pendingCropUri = uri }
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
                title = { Text("Profile") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(modifier = Modifier.clickable { showPhotoSourceDialog = true }) {
                UserAvatar(
                    photoUrl = user?.photoUrl.orEmpty(),
                    name = displayName,
                    size = 104.dp
                )
            }
            TextButton(onClick = { showPhotoSourceDialog = true }) {
                Text("Change Profile Photo")
            }

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' || c == '.' } },
                label = { Text("Username") },
                prefix = { Text("@") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("profile_username_input")
            )
            OutlinedTextField(
                value = about,
                onValueChange = { about = it },
                label = { Text("About") },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone number (optional)") },
                placeholder = { Text("+91 9876543210") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Account Email", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    user?.email?.ifBlank { authRepository.currentUser?.email.orEmpty() }.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = { viewModel.updateProfile(displayName, username, about, phone = phone) },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_profile_button")
            ) {
                Text("Save Profile")
            }
        }
    }

    if (showPhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoSourceDialog = false },
            title = { Text("Profile Photo") },
            text = {
                Column {
                    ListActionRow(Icons.Default.CameraAlt, "Take photo with Camera") {
                        showPhotoSourceDialog = false
                        onOpenCamera()
                    }
                    ListActionRow(Icons.Default.PhotoLibrary, "Choose from Gallery") {
                        showPhotoSourceDialog = false
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    if (!user?.photoUrl.isNullOrBlank()) {
                        ListActionRow(Icons.Default.Delete, "Remove profile photo", tint = AlokDangerRed) {
                            showPhotoSourceDialog = false
                            viewModel.removeProfilePhoto()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPhotoSourceDialog = false }) { Text("Close") }
            }
        )
    }

    pendingCropUri?.let { uri ->
        ImageCropDialog(
            imageUri = uri,
            onCropped = { bmp ->
                pendingCropUri = null
                viewModel.uploadProfilePhoto(bmp)
            },
            onDismiss = { pendingCropUri = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onOpenProfile: () -> Unit,
    onOpenStarred: () -> Unit,
    onOpenAbout: () -> Unit,
    onBack: () -> Unit
) {
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val blockedUids by viewModel.blockedUids.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val contactUids by viewModel.myContactAndChatUids.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var activeSubDialog by remember { mutableStateOf<String?>(null) }
    var showStatusPrivacyDialog by remember { mutableStateOf(false) }

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
                title = { Text("Settings") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenProfile)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(
                        photoUrl = user?.photoUrl.orEmpty(),
                        name = user?.displayName.orEmpty(),
                        size = 62.dp
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user?.bestDisplayName() ?: "Your Profile",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "@${user?.username.orEmpty()} • ${user?.about.orEmpty()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider()
            }

            item {
                SettingsRow(Icons.Default.Person, "Profile & Account", "Edit name, photo, @username, contact info") {
                    onOpenProfile()
                }
                SettingsRow(Icons.Default.Lock, "Privacy", "Photo, last seen, online, status, read receipts") {
                    activeSubDialog = "privacy"
                }
                SettingsRow(Icons.Default.Block, "Blocked Users", "${blockedUids.size} blocked contact(s)") {
                    activeSubDialog = "blocked"
                }
                SettingsRow(Icons.Default.Notifications, "Notifications", "Sound, vibration, lock-screen preview privacy") {
                    activeSubDialog = "notifications"
                }
                SettingsRow(Icons.Default.Chat, "Chat Settings & Theme", "${settings.themePreference.name} theme • ${settings.fontSizePreference.label} text") {
                    activeSubDialog = "chats"
                }
                SettingsRow(Icons.Default.Storage, "Storage and Data", "Auto-download & local cache (${FormatUtils.formatFileSize(viewModel.getCacheSizeBytes(context)).ifBlank { "0 B" }})") {
                    activeSubDialog = "storage"
                }
                SettingsRow(Icons.Default.Star, "Starred Messages", "View saved important messages") {
                    onOpenStarred()
                }
                SettingsRow(Icons.Default.Info, "About ALOK CHAT", "Created by Alok • Supported by Harshit") {
                    onOpenAbout()
                }
                HorizontalDivider()
                SettingsRow(Icons.AutoMirrored.Filled.Logout, "Logout", "Sign out of ${user?.email?.ifBlank { user?.phone.orEmpty() }.orEmpty()}", tint = AlokDangerRed) {
                    viewModel.signOut()
                }
                Spacer(Modifier.height(24.dp))
                AlokCreditFooter()
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    when (activeSubDialog) {
        "privacy" -> {
            val u = user
            var photo by remember { mutableStateOf(PrivacyOption.entries.firstOrNull { it.name == u?.privacyPhoto } ?: PrivacyOption.EVERYONE) }
            var lastSeen by remember { mutableStateOf(PrivacyOption.entries.firstOrNull { it.name == u?.privacyLastSeen } ?: PrivacyOption.EVERYONE) }
            var online by remember { mutableStateOf(PrivacyOption.entries.firstOrNull { it.name == u?.privacyOnline } ?: PrivacyOption.EVERYONE) }
            val statusMode = u?.resolvedStatusPrivacyMode() ?: StatusPrivacyMode.EVERYONE
            val statusLabel = formatStatusPrivacySummary(
                mode = statusMode,
                excludedCount = u?.statusExcludedUids?.count { it.value } ?: 0,
                allowedCount = u?.statusAllowedUids?.count { it.value } ?: 0
            )
            var receipts by remember { mutableStateOf(u?.readReceiptsEnabled ?: true) }
            AlertDialog(
                onDismissRequest = { activeSubDialog = null },
                title = { Text("Privacy") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrivacyCycleRow("Profile photo", photo) {
                            photo = PrivacyOption.entries[(photo.ordinal + 1) % PrivacyOption.entries.size]
                        }
                        PrivacyCycleRow("Last seen", lastSeen) {
                            lastSeen = PrivacyOption.entries[(lastSeen.ordinal + 1) % PrivacyOption.entries.size]
                        }
                        PrivacyCycleRow("Online status", online) {
                            online = PrivacyOption.entries[(online.ordinal + 1) % PrivacyOption.entries.size]
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showStatusPrivacyDialog = true }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Status updates", style = MaterialTheme.typography.bodyLarge)
                            Text(statusLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Read receipts", style = MaterialTheme.typography.bodyLarge)
                            Switch(checked = receipts, onCheckedChange = { receipts = it })
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val legacyStatus = when (statusMode) {
                            StatusPrivacyMode.EVERYONE -> PrivacyOption.EVERYONE
                            StatusPrivacyMode.MY_CONTACTS, StatusPrivacyMode.MY_CONTACTS_EXCEPT -> PrivacyOption.CONTACTS
                            StatusPrivacyMode.ONLY_SHARE_WITH -> PrivacyOption.NOBODY
                        }
                        viewModel.updatePrivacy(photo, lastSeen, online, legacyStatus, receipts)
                        activeSubDialog = null
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { activeSubDialog = null }) { Text("Cancel") }
                }
            )
        }

        "blocked" -> {
            val blockedProfiles = allUsers.filter { it.uid in blockedUids }
            AlertDialog(
                onDismissRequest = { activeSubDialog = null },
                title = { Text("Blocked Users") },
                text = {
                    if (blockedUids.isEmpty()) {
                        Text("You have not blocked any users.")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            blockedUids.forEach { uid ->
                                val profile = blockedProfiles.firstOrNull { it.uid == uid }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(profile?.displayName?.ifBlank { profile.phone } ?: uid.take(8))
                                    TextButton(onClick = { viewModel.toggleBlockUser(uid, false) }) {
                                        Text("Unblock")
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { activeSubDialog = null }) { Text("Close") }
                }
            )
        }

        "notifications" -> {
            AlertDialog(
                onDismissRequest = { activeSubDialog = null },
                title = { Text("Notifications") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ToggleRow("Notification sound", settings.notificationSound, viewModel::setNotificationSound)
                        ToggleRow("Vibrate", settings.notificationVibrate, viewModel::setNotificationVibrate)
                        ToggleRow("Show message preview", settings.notificationShowPreview, viewModel::setNotificationPreview)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { activeSubDialog = null }) { Text("Done") }
                }
            )
        }

        "chats" -> {
            AlertDialog(
                onDismissRequest = { activeSubDialog = null },
                title = { Text("Chat Settings & Theme") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Theme", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        ThemePreference.entries.forEach { theme ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { viewModel.setTheme(theme) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = settings.themePreference == theme, onClick = { viewModel.setTheme(theme) })
                                Text(theme.name.lowercase().replaceFirstChar { it.uppercase() })
                            }
                        }
                        Text("Font Size", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FontSizePreference.entries.forEach { fs ->
                                OutlinedButton(
                                    onClick = { viewModel.setFontSize(fs) },
                                    colors = if (settings.fontSizePreference == fs) {
                                        ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                    } else ButtonDefaults.outlinedButtonColors()
                                ) {
                                    Text(fs.label)
                                }
                            }
                        }
                        ToggleRow("Enter key sends message", settings.enterIsSend, viewModel::setEnterIsSend)
                        ToggleRow("Pattern chat wallpaper", settings.chatWallpaperIndex == 0) { enabled ->
                            viewModel.setChatWallpaper(if (enabled) 0 else 1)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { activeSubDialog = null }) { Text("Done") }
                }
            )
        }

        "storage" -> {
            AlertDialog(
                onDismissRequest = { activeSubDialog = null },
                title = { Text("Storage & Data") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ToggleRow("Auto-download on Wi-Fi", settings.autoDownloadWifi, viewModel::setAutoDownloadWifi)
                        ToggleRow("Auto-download on Mobile Data", settings.autoDownloadCellular, viewModel::setAutoDownloadCellular)
                        Button(
                            onClick = {
                                viewModel.clearMediaCache(context)
                                activeSubDialog = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Clear Local Cache")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { activeSubDialog = null }) { Text("Done") }
                }
            )
        }
    }

    if (showStatusPrivacyDialog) {
        val u = user
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarredMessagesScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val starred by viewModel.starredMessages.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadStarredMessages() }

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
                title = { Text("Starred Messages") }
            )
        }
    ) { padding ->
        if (starred.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No starred messages yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(starred, key = { it.id }) { msg ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(msg.senderName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    FormatUtils.formatChatListTime(msg.timestamp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(msg.text.ifBlank { msg.mediaName.ifBlank { msg.type } })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
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
                title = { Text("About ALOK CHAT") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            AlokBrandLogo(size = 90.dp)
            Text(
                text = "ALOK CHAT",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )
            Text("Version 1.0.0", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Credits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Created by: Alok", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text("Supported by: Harshit", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Security & Transport", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = stringResource(R.string.about_security_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.weight(1f))
            AlokCreditFooter()
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ListActionRow(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PrivacyCycleRow(
    label: String,
    option: PrivacyOption,
    onCycle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCycle)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(option.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChanged)
    }
}
