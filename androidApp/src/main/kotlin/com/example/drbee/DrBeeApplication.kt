package com.example.drbee

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.example.drbee.ProfileScreen.Napier
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.database.FirebaseDatabase
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize

class DrBeeApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // ── 1. Core Firebase init ─────────────────────────────────────────────
        Firebase.initialize(this)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        NotificationService.appContext = applicationContext

        // ── 2. Notification channel ───────────────────────────────────────────
        createNotificationChannel()

        com.google.firebase.FirebaseApp.initializeApp(this)



        // ── 3. Only initialize listener if user is ALREADY logged in
        //    (returning user opening app again).
        //    Fresh logins are handled in MainActivity.onUserLoggedIn.
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (!uid.isNullOrBlank()) {

//            FirebaseCrashlytics.getInstance().setCustomKey(uid.toString(),email.toString())

            Log.d("FCM", "App start — user already logged in uid=$uid")
            NotificationService().initialize()       // attach queue listener
            FcmTokenHelper.initForCurrentUser(uid)   // refresh FCM token
        } else {
            Log.d("FCM", "App start — no user logged in yet")
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