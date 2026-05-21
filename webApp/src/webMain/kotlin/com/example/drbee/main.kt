package com.example.drbee

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize
import dev.gitlive.firebase.FirebaseOptions

@OptIn(ExperimentalComposeUiApi::class)
fun main() {

    val webOptions = FirebaseOptions(
        applicationId = "1:444402775117:web:82608e86b7a6afde1b1804",
        apiKey = "AIzaSyBzHwoHb9KhxPDvdPfkOtI8BbjFprgYgXk",
        projectId = "doctor-bee-2d622",
        storageBucket = "doctor-bee-2d622.firebasestorage.app",
        // 🚨 FIXED: Added trailing slash here to ensure proper regional data routing
        databaseUrl = "https://doctor-bee-2d622-default-rtdb.firebaseio.com"
    )

    // Core KMP Multiplatform Initialization for Browser context
    Firebase.initialize(options = webOptions)

    ComposeViewport {
        App()
    }
}
