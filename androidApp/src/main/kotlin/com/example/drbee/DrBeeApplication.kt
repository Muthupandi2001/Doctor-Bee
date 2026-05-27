package com.example.drbee

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize

class DrBeeApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // ── 1. Core init ──────────────────────────────────────────────────────
        Firebase.initialize(this)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        NotificationService.appContext = applicationContext

        // ── 2. Notification channel (must exist before any notification) ───────
        createNotificationChannel()

        // ── 3. NotificationService setup ──────────────────────────────────────
        NotificationService().initialize()

        // ── 4. Token fetch for already-logged-in user on app start ────────────
        //    Uses retry — handles SERVICE_NOT_AVAILABLE on new/cold devices
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (!uid.isNullOrBlank()) {
            Log.d("FCM", "App start — fetching token for uid=$uid")
            FcmTokenHelper.initForCurrentUser(uid)
        } else {
            // Not logged in yet — onNewToken or flushPendingToken will handle it
            Log.d("FCM", "App start — no user, token will be saved after login")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "drbee_chat_channel",
                "Chat Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description   = "DrBee chat message notifications"
                enableLights(true)
                enableVibration(true)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}