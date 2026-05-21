package com.example.drbee

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize

class DrBeeApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 1. ✅ Core initialization using the GitLive context wrapper
        Firebase.initialize(this)

        // 2. ✅ Explicitly link your native Android layers to your Realtime Database URL
        val databaseUrl = "https://doctor-bee-2d622-default-rtdb.firebaseio.com"

        // Enable local offline persistence (optional, but recommended for chat apps)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        // Explicitly set the database reference instance URL globally
        FirebaseDatabase.getInstance(databaseUrl)
    }
}
