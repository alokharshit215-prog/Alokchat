package com.example.ui.screens.group

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UserProfile
import com.example.data.repository.GroupRepository
import com.example.data.repository.MediaRepository
import com.example.ui.components.ImageCropDialog
import com.example.ui.components.UserAvatar
import com.example.ui.theme.AlokDangerRed
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onGroupCreated: (String, String) -> Unit
) {
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    var groupName by remember { mutableStateOf("") }
    var groupDesc by remember { mutableStateOf("") }
    var selectedUids by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }
    var croppedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) pendingCropUri = uri }
    )

    LaunchedEffect(Unit) { viewModel.loadDirectoryUsers() }

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
                title = { Text("New Group") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = croppedBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Group icon",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Pick group photo",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("group_name_input")
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = groupDesc,
                onValueChange = { groupDesc = it },
                label = { Text("Group description (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Participants (${selectedUids.size} selected)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(allUsers, key = { it.uid }) { user ->
                    val checked = user.uid in selectedUids
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedUids = if (checked) selectedUids - user.uid else selectedUids + user.uid
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(photoUrl = user.photoUrl, name = user.displayName, size = 44.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.bestDisplayName(), style = MaterialTheme.typography.bodyLarge)
                            Text("@${user.username}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                selectedUids = if (checked) selectedUids - user.uid else selectedUids + user.uid
                            }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    viewModel.createGroup(groupName, groupDesc, croppedBitmap, selectedUids.toList()) { groupId ->
                        onGroupCreated(groupId, groupName.trim())
                    }
                },
                enabled = groupName.trim().isNotEmpty() && selectedUids.isNotEmpty() && !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("create_group_submit_button")
            ) {
                Text(if (isBusy) "Creating group..." else "Create Group")
            }
        }
    }

    pendingCropUri?.let { uri ->
        ImageCropDialog(
            imageUri = uri,
            onCropped = {
                croppedBitmap = it
                pendingCropUri = null
            },
            onDismiss = { pendingCropUri = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(
    groupId: String,
    currentUid: String,
    mainViewModel: MainViewModel,
    groupRepository: GroupRepository,
    mediaRepository: MediaRepository,
    onBack: () -> Unit,
    onGroupLeftOrDeleted: () -> Unit
) {
    val group by groupRepository.observeGroup(groupId).collectAsStateWithLifecycle(initialValue = null)
    val allUsers by mainViewModel.allUsers.collectAsStateWithLifecycle()
    val me by mainViewModel.currentUser.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var editingName by remember(group) { mutableStateOf(group?.name.orEmpty()) }
    var editingDesc by remember(group) { mutableStateOf(group?.description.orEmpty()) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAddParticipants by remember { mutableStateOf(false) }
    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) pendingCropUri = uri }
    )

    val isAdmin = group?.admins?.get(currentUid) == true
    val userMap = remember(allUsers, me) {
        (allUsers + listOfNotNull(me)).associateBy { it.uid }
    }
    val memberProfiles = remember(group, userMap) {
        group?.members?.keys?.map { uid ->
            userMap[uid] ?: UserProfile(uid = uid, displayName = "User ${uid.take(6)}")
        } ?: emptyList()
    }

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
                title = { Text("Group Info") }
            )
        }
    ) { padding ->
        val currentGroup = group
        if (currentGroup == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Loading group...")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.clickable(enabled = isAdmin) {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    ) {
                        UserAvatar(
                            photoUrl = currentGroup.photoUrl,
                            name = currentGroup.name,
                            size = 96.dp,
                            isGroup = true
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(currentGroup.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    if (currentGroup.description.isNotBlank()) {
                        Text(
                            currentGroup.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "${currentGroup.members.size} participant(s)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (isAdmin) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { showEditDialog = true }) {
                                Text("Edit Group")
                            }
                            OutlinedButton(onClick = { showAddParticipants = true }) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Add")
                            }
                        }
                    }
                }
            }

            item { HorizontalDivider() }

            item {
                Text("Participants", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(memberProfiles, key = { it.uid }) { member ->
                val memberIsAdmin = currentGroup.admins[member.uid] == true
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(photoUrl = member.photoUrl, name = member.displayName, size = 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (member.uid == currentUid) "You" else member.bestDisplayName(),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (memberIsAdmin) {
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        "Admin",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(member.about, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (isAdmin && member.uid != currentUid) {
                        IconButton(onClick = {
                            scope.launch {
                                groupRepository.setAdminStatus(groupId, member.uid, !memberIsAdmin)
                            }
                        }) {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = if (memberIsAdmin) "Remove admin" else "Make admin",
                                tint = if (memberIsAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            scope.launch {
                                groupRepository.removeParticipant(groupId, member.uid)
                            }
                        }) {
                            Icon(Icons.Default.PersonRemove, contentDescription = "Remove participant", tint = AlokDangerRed)
                        }
                    }
                }
            }

            item { HorizontalDivider() }

            item {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            groupRepository.leaveGroup(groupId, currentUid)
                            onGroupLeftOrDeleted()
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AlokDangerRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Leave Group")
                }

                if (isAdmin || currentGroup.createdBy == currentUid) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                groupRepository.deleteGroup(groupId)
                                onGroupLeftOrDeleted()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AlokDangerRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Delete Group")
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Group") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editingName,
                        onValueChange = { editingName = it },
                        label = { Text("Group name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editingDesc,
                        onValueChange = { editingDesc = it },
                        label = { Text("Description") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        groupRepository.updateGroupDetails(groupId, editingName, editingDesc, null)
                        showEditDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddParticipants) {
        val currentMembers = group?.members?.keys ?: emptySet()
        val candidates = allUsers.filter { it.uid !in currentMembers }
        var toAdd by remember { mutableStateOf<Set<String>>(emptySet()) }
        AlertDialog(
            onDismissRequest = { showAddParticipants = false },
            title = { Text("Add Participants") },
            text = {
                if (candidates.isEmpty()) {
                    Text("All registered users are already in this group.")
                } else {
                    LazyColumn(modifier = Modifier.height(260.dp)) {
                        items(candidates, key = { it.uid }) { user ->
                            val checked = user.uid in toAdd
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        toAdd = if (checked) toAdd - user.uid else toAdd + user.uid
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(photoUrl = user.photoUrl, name = user.displayName, size = 38.dp)
                                Spacer(Modifier.width(10.dp))
                                Text(user.bestDisplayName(), modifier = Modifier.weight(1f))
                                Checkbox(checked = checked, onCheckedChange = {
                                    toAdd = if (checked) toAdd - user.uid else toAdd + user.uid
                                })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = toAdd.isNotEmpty(),
                    onClick = {
                        scope.launch {
                            groupRepository.addParticipants(groupId, toAdd.toList())
                            showAddParticipants = false
                        }
                    }
                ) { Text("Add (${toAdd.size})") }
            },
            dismissButton = {
                TextButton(onClick = { showAddParticipants = false }) { Text("Close") }
            }
        )
    }

    pendingCropUri?.let { uri ->
        ImageCropDialog(
            imageUri = uri,
            onCropped = { bmp ->
                pendingCropUri = null
                scope.launch {
                    val bytes = mediaRepository.compressBitmapToJpegBytes(bmp)
                    mediaRepository.uploadBytes(
                        "grp_icon_$groupId",
                        "group_photos/$groupId/${System.currentTimeMillis()}.jpg",
                        bytes,
                        "image/jpeg"
                    ).onSuccess { url ->
                        groupRepository.updateGroupDetails(
                            groupId,
                            group?.name.orEmpty(),
                            group?.description.orEmpty(),
                            url
                        )
                    }
                }
            },
            onDismiss = { pendingCropUri = null }
        )
    }
}
