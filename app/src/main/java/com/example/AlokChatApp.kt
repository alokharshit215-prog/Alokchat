package com.example

import android.app.Application
import android.util.Log
import com.example.di.AppContainer
import com.example.service.NotificationHelper
import com.google.firebase.database.FirebaseDatabase

class AlokChatApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseDatabase.getInstance("https://alok-chat-5d65c-default-rtdb.firebaseio.com")
                .setPersistenceEnabled(true)
        } catch (e: Exception) {
            Log.w("AlokChatApp", "Realtime DB persistence setup skipped: ${e.message}")
        }
        NotificationHelper.createNotificationChannels(this)
        container = AppContainer(this)
    }
}
