package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.AppSettingsState
import com.example.data.model.CallSession
import com.example.data.model.UserProfile
import com.example.di.AppContainer
import com.example.ui.components.AlokBrandLogo
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.auth.AuthFlowScreen
import com.example.ui.screens.call.CallScreen
import com.example.ui.screens.camera.CameraCaptureScreen
import com.example.ui.screens.chat.ChatScreen
import com.example.ui.screens.group.CreateGroupScreen
import com.example.ui.screens.group.GroupInfoScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.search.UserSearchScreen
import com.example.ui.screens.settings.AboutScreen
import com.example.ui.screens.settings.ProfileScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.StarredMessagesScreen
import com.example.ui.screens.status.CreateMediaStatusScreen
import com.example.ui.screens.status.CreateTextStatusScreen
import com.example.ui.screens.status.StatusViewerScreen
import com.example.ui.theme.AlokChatTheme
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.CallViewModel
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.MainViewModel

private sealed interface CallLaunchMode {
    data class Outgoing(
        val id: String,
        val caller: UserProfile,
        val callee: UserProfile,
        val isVideo: Boolean
    ) : CallLaunchMode

    data class Incoming(val session: CallSession) : CallLaunchMode
}

class MainActivity : ComponentActivity() {

    private var deepLinkChatIdState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLinkChatIdState.value = intent?.getStringExtra("chatId")

        val container = (application as AlokChatApp).container

        setContent {
            val settings by container.userPreferences.settingsFlow
                .collectAsStateWithLifecycle(initialValue = AppSettingsState())

            AlokChatTheme(
                themePreference = settings.themePreference,
                fontScaleMultiplier = settings.fontSizePreference.scale
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AlokChatRoot(
                        container = container,
                        deepLinkChatId = deepLinkChatIdState.value,
                        onClearDeepLink = { deepLinkChatIdState.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val chatId = intent.getStringExtra("chatId")
        if (!chatId.isNullOrBlank()) {
            deepLinkChatIdState.value = chatId
        }
    }
}

@Composable
private fun AlokChatRoot(
    container: AppContainer,
    deepLinkChatId: String?,
    onClearDeepLink: () -> Unit
) {
    var showSplash by rememberSaveable { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen(onFinished = { showSplash = false })
        return
    }

    val firebaseUser by container.authRepository.observeAuthState()
        .collectAsStateWithLifecycle(initialValue = container.authRepository.currentUser)

    val authViewModel: AuthViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(container.authRepository, container.userRepository) as T
            }
        }
    )

    val user = firebaseUser
    if (user == null) {
        LaunchedEffect(Unit) {
            authViewModel.resetState()
        }
        AuthFlowScreen(
            viewModel = authViewModel,
            onAuthenticated = {}
        )
        return
    }

    val mainViewModel: MainViewModel = viewModel(
        key = "main_${user.uid}",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(
                    currentUid = user.uid,
                    authRepository = container.authRepository,
                    userRepository = container.userRepository,
                    chatRepository = container.chatRepository,
                    groupRepository = container.groupRepository,
                    statusRepository = container.statusRepository,
                    callRepository = container.callRepository,
                    mediaRepository = container.mediaRepository,
                    userPreferences = container.userPreferences
                ) as T
            }
        }
    )

    val currentUserProfile by mainViewModel.currentUser.collectAsStateWithLifecycle()

    val profile = currentUserProfile
    if (profile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AlokBrandLogo(size = 72.dp)
                CircularProgressIndicator()
                Text(
                    text = "Loading ALOK CHAT...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    if (profile.displayName.isBlank()) {
        LaunchedEffect(profile.uid) {
            authViewModel.enterProfileSetup(profile.username)
        }
        AuthFlowScreen(
            viewModel = authViewModel,
            onAuthenticated = {}
        )
        return
    }

    AuthenticatedAppScaffold(
        container = container,
        mainViewModel = mainViewModel,
        currentProfile = profile,
        deepLinkChatId = deepLinkChatId,
        onClearDeepLink = onClearDeepLink
    )
}

@Composable
private fun AuthenticatedAppScaffold(
    container: AppContainer,
    mainViewModel: MainViewModel,
    currentProfile: UserProfile,
    deepLinkChatId: String?,
    onClearDeepLink: () -> Unit
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val settings by mainViewModel.settings.collectAsStateWithLifecycle()
    val allChats by mainViewModel.chats.collectAsStateWithLifecycle()
    val toastMessage by mainViewModel.toastMessage.collectAsStateWithLifecycle()
    val incomingCall by mainViewModel.incomingCall.collectAsStateWithLifecycle()

    var capturedPhoto by remember { mutableStateOf<Pair<String, Uri>?>(null) }
    var pendingCameraTarget by remember { mutableStateOf<String?>(null) }
    var activeCallMode by remember { mutableStateOf<CallLaunchMode?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val target = pendingCameraTarget
        pendingCameraTarget = null
        if (granted && target != null) {
            navController.navigate("camera/$target")
        } else if (!granted) {
            Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchCamera(target: String) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            navController.navigate("camera/$target")
        } else {
            pendingCameraTarget = target
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun buildChatRoute(
        chatId: String,
        peerUid: String,
        isGroup: Boolean,
        title: String,
        photoUrl: String
    ): String {
        val encChat = Uri.encode(chatId)
        val encPeer = Uri.encode(peerUid)
        val encTitle = Uri.encode(title.ifBlank { "Alok Chat" })
        val encPhoto = Uri.encode(photoUrl)
        return "chat/$encChat?peerUid=$encPeer&isGroup=$isGroup&title=$encTitle&photo=$encPhoto"
    }

    fun startOutgoingCall(peer: UserProfile, isVideo: Boolean) {
        if (activeCallMode != null) return
        activeCallMode = CallLaunchMode.Outgoing(
            id = "out_${peer.uid}_${System.currentTimeMillis()}",
            caller = currentProfile,
            callee = peer,
            isVideo = isVideo
        )
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            mainViewModel.clearToast()
        }
    }

    LaunchedEffect(incomingCall) {
        val incoming = incomingCall
        if (incoming != null && activeCallMode == null) {
            activeCallMode = CallLaunchMode.Incoming(incoming)
        }
    }

    LaunchedEffect(deepLinkChatId, allChats) {
        val targetId = deepLinkChatId
        if (!targetId.isNullOrBlank()) {
            val summary = allChats.firstOrNull { it.chatId == targetId }
            if (summary != null) {
                navController.navigate(
                    buildChatRoute(
                        chatId = summary.chatId,
                        peerUid = summary.peerUid,
                        isGroup = summary.group,
                        title = summary.title,
                        photoUrl = summary.photoUrl
                    )
                )
                onClearDeepLink()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "home"
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = mainViewModel,
                    onOpenChat = { summary ->
                        navController.navigate(
                            buildChatRoute(
                                chatId = summary.chatId,
                                peerUid = summary.peerUid,
                                isGroup = summary.group,
                                title = summary.title,
                                photoUrl = summary.photoUrl
                            )
                        )
                    },
                    onOpenUserSearch = { navController.navigate("search") },
                    onOpenCreateGroup = { navController.navigate("create_group") },
                    onOpenCamera = { launchCamera("home") },
                    onOpenCreateTextStatus = { navController.navigate("status_text") },
                    onOpenCreateMediaStatus = { navController.navigate("status_media") },
                    onViewUserStatuses = { uid -> navController.navigate("status_view/${Uri.encode(uid)}") },
                    onOpenSettings = { navController.navigate("settings") },
                    onOpenProfile = { navController.navigate("profile") },
                    onOpenStarred = { navController.navigate("starred") },
                    onOpenAbout = { navController.navigate("about") },
                    onStartVoiceCall = { peer -> startOutgoingCall(peer, isVideo = false) },
                    onStartVideoCall = { peer -> startOutgoingCall(peer, isVideo = true) }
                )
            }

            composable("search") {
                UserSearchScreen(
                    viewModel = mainViewModel,
                    onBack = { navController.popBackStack() },
                    onCreateGroup = { navController.navigate("create_group") },
                    onOpenChat = { chatId, peer ->
                        navController.navigate(
                            buildChatRoute(
                                chatId = chatId,
                                peerUid = peer.uid,
                                isGroup = false,
                                title = peer.bestDisplayName(),
                                photoUrl = peer.photoUrl
                            )
                        ) {
                            popUpTo("home")
                        }
                    }
                )
            }

            composable("create_group") {
                CreateGroupScreen(
                    viewModel = mainViewModel,
                    onBack = { navController.popBackStack() },
                    onGroupCreated = { groupId, groupName ->
                        navController.navigate(
                            buildChatRoute(
                                chatId = groupId,
                                peerUid = "",
                                isGroup = true,
                                title = groupName,
                                photoUrl = ""
                            )
                        ) {
                            popUpTo("home")
                        }
                    }
                )
            }

            composable(
                route = "group_info/{groupId}",
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { entry ->
                val groupId = Uri.decode(entry.arguments?.getString("groupId").orEmpty())
                GroupInfoScreen(
                    groupId = groupId,
                    currentUid = currentProfile.uid,
                    mainViewModel = mainViewModel,
                    groupRepository = container.groupRepository,
                    mediaRepository = container.mediaRepository,
                    onBack = { navController.popBackStack() },
                    onGroupLeftOrDeleted = {
                        navController.popBackStack("home", inclusive = false)
                    }
                )
            }

            composable(
                route = "chat/{chatId}?peerUid={peerUid}&isGroup={isGroup}&title={title}&photo={photo}",
                arguments = listOf(
                    navArgument("chatId") { type = NavType.StringType },
                    navArgument("peerUid") { type = NavType.StringType; defaultValue = "" },
                    navArgument("isGroup") { type = NavType.BoolType; defaultValue = false },
                    navArgument("title") { type = NavType.StringType; defaultValue = "Alok Chat" },
                    navArgument("photo") { type = NavType.StringType; defaultValue = "" }
                )
            ) { entry ->
                val chatId = Uri.decode(entry.arguments?.getString("chatId").orEmpty())
                val peerUid = Uri.decode(entry.arguments?.getString("peerUid").orEmpty())
                val isGroup = entry.arguments?.getBoolean("isGroup") ?: false
                val initialTitle = Uri.decode(entry.arguments?.getString("title").orEmpty())
                val initialPhoto = Uri.decode(entry.arguments?.getString("photo").orEmpty())

                val chatVm: ChatViewModel = viewModel(
                    key = "chat_$chatId",
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return ChatViewModel(
                                chatId = chatId,
                                currentUid = currentProfile.uid,
                                peerUid = peerUid,
                                isGroup = isGroup,
                                chatRepository = container.chatRepository,
                                userRepository = container.userRepository,
                                groupRepository = container.groupRepository,
                                mediaRepository = container.mediaRepository,
                                voiceRecorderManager = container.voiceRecorderManager,
                                audioPlayerManager = container.audioPlayerManager
                            ) as T
                        }
                    }
                )

                val chatCapturedUri = if (capturedPhoto?.first == "chat") capturedPhoto?.second else null

                ChatScreen(
                    viewModel = chatVm,
                    mainViewModel = mainViewModel,
                    userRepository = container.userRepository,
                    allChats = allChats,
                    settings = settings,
                    initialTitle = initialTitle,
                    initialPhoto = initialPhoto,
                    capturedPhotoUri = chatCapturedUri,
                    onClearCapturedPhoto = { capturedPhoto = null },
                    onBack = { navController.popBackStack() },
                    onOpenCamera = { launchCamera("chat") },
                    onOpenGroupInfo = { gId ->
                        navController.navigate("group_info/${Uri.encode(gId)}")
                    },
                    onStartVoiceCall = { peer -> startOutgoingCall(peer, isVideo = false) },
                    onStartVideoCall = { peer -> startOutgoingCall(peer, isVideo = true) }
                )
            }

            composable(
                route = "camera/{target}",
                arguments = listOf(navArgument("target") { type = NavType.StringType })
            ) { entry ->
                val target = entry.arguments?.getString("target") ?: "home"
                CameraCaptureScreen(
                    onPhotoCaptured = { uri ->
                        if (target == "home") {
                            capturedPhoto = "status" to uri
                            navController.navigate("status_media") {
                                popUpTo("home")
                            }
                        } else {
                            capturedPhoto = target to uri
                            navController.popBackStack()
                        }
                    },
                    onClose = { navController.popBackStack() }
                )
            }

            composable("status_text") {
                CreateTextStatusScreen(
                    viewModel = mainViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("status_media") {
                val statusCapturedUri = if (capturedPhoto?.first == "status") capturedPhoto?.second else null
                CreateMediaStatusScreen(
                    viewModel = mainViewModel,
                    initialCapturedUri = statusCapturedUri,
                    onClearCaptured = { capturedPhoto = null },
                    onOpenCamera = { launchCamera("status") },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "status_view/{ownerUid}",
                arguments = listOf(navArgument("ownerUid") { type = NavType.StringType })
            ) { entry ->
                val ownerUid = Uri.decode(entry.arguments?.getString("ownerUid").orEmpty())
                StatusViewerScreen(
                    ownerUid = ownerUid,
                    currentUid = currentProfile.uid,
                    viewModel = mainViewModel,
                    onReplyToUser = { peer, replyMessage ->
                        mainViewModel.sendDirectMessageText(peer, replyMessage) { chatId ->
                            navController.navigate(
                                buildChatRoute(
                                    chatId = chatId,
                                    peerUid = peer.uid,
                                    isGroup = false,
                                    title = peer.bestDisplayName(),
                                    photoUrl = peer.photoUrl
                                )
                            ) {
                                popUpTo("home")
                            }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("profile") {
                val profileCapturedUri = if (capturedPhoto?.first == "profile") capturedPhoto?.second else null
                ProfileScreen(
                    viewModel = mainViewModel,
                    authRepository = container.authRepository,
                    userRepository = container.userRepository,
                    capturedPhotoUri = profileCapturedUri,
                    onClearCaptured = { capturedPhoto = null },
                    onOpenCamera = { launchCamera("profile") },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("settings") {
                SettingsScreen(
                    viewModel = mainViewModel,
                    onOpenProfile = { navController.navigate("profile") },
                    onOpenStarred = { navController.navigate("starred") },
                    onOpenAbout = { navController.navigate("about") },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("starred") {
                StarredMessagesScreen(
                    viewModel = mainViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("about") {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }

        val callMode = activeCallMode
        if (callMode != null) {
            val callKey = when (callMode) {
                is CallLaunchMode.Outgoing -> callMode.id
                is CallLaunchMode.Incoming -> "in_${callMode.session.callId}"
            }
            val callVm: CallViewModel = viewModel(
                key = callKey,
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return CallViewModel(
                            context = context.applicationContext,
                            currentUid = currentProfile.uid,
                            callRepository = container.callRepository
                        ) as T
                    }
                }
            )

            LaunchedEffect(callKey) {
                when (callMode) {
                    is CallLaunchMode.Outgoing -> {
                        callVm.startOutgoingCall(callMode.caller, callMode.callee, callMode.isVideo)
                    }
                    is CallLaunchMode.Incoming -> {
                        callVm.attachIncomingCall(callMode.session)
                    }
                }
            }

            BackHandler {
                callVm.endCall()
            }

            CallScreen(
                viewModel = callVm,
                currentUid = currentProfile.uid,
                onCallEnded = { activeCallMode = null }
            )
        }
    }
}
