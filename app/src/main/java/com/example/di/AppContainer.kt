package com.example.di

import android.content.Context
import com.example.data.prefs.UserPreferences
import com.example.data.repository.AuthRepository
import com.example.data.repository.CallRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.GroupRepository
import com.example.data.repository.MediaRepository
import com.example.data.repository.StatusRepository
import com.example.data.repository.UserRepository
import com.example.media.AudioPlayerManager
import com.example.media.VoiceRecorderManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firebaseDatabase: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance("https://alok-chat-5d65c-default-rtdb.firebaseio.com")
    }
    val firebaseStorage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance("gs://alok-chat-5d65c.firebasestorage.app")
    }

    val userPreferences: UserPreferences by lazy { UserPreferences(appContext) }
    val authRepository: AuthRepository by lazy { AuthRepository(firebaseAuth) }
    val userRepository: UserRepository by lazy { UserRepository(firebaseDatabase) }
    val mediaRepository: MediaRepository by lazy { MediaRepository(firebaseStorage) }
    val chatRepository: ChatRepository by lazy { ChatRepository(firebaseDatabase) }
    val groupRepository: GroupRepository by lazy { GroupRepository(firebaseDatabase) }
    val statusRepository: StatusRepository by lazy { StatusRepository(firebaseDatabase) }
    val callRepository: CallRepository by lazy { CallRepository(firebaseDatabase) }
    val voiceRecorderManager: VoiceRecorderManager by lazy { VoiceRecorderManager(appContext) }
    val audioPlayerManager: AudioPlayerManager by lazy { AudioPlayerManager() }
}
