package com.example.drbee

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // ✅ CLEAN: Firebase is initialized in DrBeeApplication on startup.
        // Loading the shared multiplatform UI entry element here.
        setContent {
            App()
        }
    }
}
