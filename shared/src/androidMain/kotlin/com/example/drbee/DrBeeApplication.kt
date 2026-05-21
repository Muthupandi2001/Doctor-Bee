package com.example.drbee

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.initialize

class DrBeeApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // ✅ Initialize KMP Firebase using the GitLive context wrapper
        Firebase.initialize(this)
    }
}