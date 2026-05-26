package com.example.drbee

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize

class DrBeeApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Firebase.initialize(this)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        // ✅ Set context FIRST before anything else
        NotificationService.appContext = applicationContext

        NotificationService().initialize()
        createNotificationChannel()

        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (!uid.isNullOrBlank()) {
                MyFCMService.saveTokenToDatabase(uid, token)
            } else {
                MyFCMService.pendingToken = token
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "drbee_chat_channel",
                "Chat Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "DrBee chat message notifications"
                enableLights(true)
                enableVibration(true)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}