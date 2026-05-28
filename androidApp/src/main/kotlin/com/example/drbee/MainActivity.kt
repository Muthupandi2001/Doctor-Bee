package com.example.drbee

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat
import com.example.drbee.NavHost.DeepLinkParams
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    var deepLinkParams by mutableStateOf<DeepLinkParams?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("FCM", "Notification permission granted=$isGranted")
        if (isGranted) fetchAndStoreFcmToken()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityProvider.set(this)
        processIntent(intent)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge(
            statusBarStyle     = SystemBarStyle.dark("#000000".toColorInt()),
            navigationBarStyle = SystemBarStyle.dark("#000000".toColorInt())
        )

        requestNotificationPermission()
        handleNotificationIntent(intent)

        setContent {
            App(
                deepLinkParams = deepLinkParams,
                onUserLoggedIn = { uid ->
                    // ✅ This is the KEY fix — called after every login
                    Log.d("FCM", ">>> onUserLoggedIn callback fired — uid=$uid")

                    // ✅ Step 1: Attach the notification_queue listener now
                    // that we have a valid uid. This is what makes web→app work.
                    NotificationService().initialize()

                    // ✅ Step 2: Save/refresh FCM token for this user
                    FcmTokenHelper.saveTokenForUser(uid)

                    // ✅ Step 3: Flush any token that arrived before login
                    MyFCMService.flushPendingToken(uid)

                    Log.d("FCM", ">>> onUserLoggedIn setup complete for uid=$uid")
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        processIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun processIntent(intent: Intent?) {
        val data: Uri? = intent?.data
        deepLinkParams = if (data?.scheme == "drbee" && data.host == "open") {
            val screen     = data.getQueryParameter("screen")
            val referrerId = data.getQueryParameter("referrerId")
            Log.d("DrBeeDeepLink", "screen=$screen referrerId=$referrerId")
            DeepLinkParams(screen, referrerId)
        } else null
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            fetchAndStoreFcmToken()
        }
    }

    private fun fetchAndStoreFcmToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            Log.d("FCM", "fetchAndStoreFcmToken — token fetched")
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (!uid.isNullOrBlank()) {
                MyFCMService.saveTokenToDatabase(uid, token)
            } else {
                Log.d("FCM", "No user yet — storing as pendingToken")
                MyFCMService.pendingToken = token
            }
        }
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val senderId = intent?.getStringExtra("OPEN_SENDER_ID") ?: return
        val roomId   = intent.getStringExtra("OPEN_ROOM_ID")   ?: return
        Log.d("FCM", "Notification tapped — senderId=$senderId roomId=$roomId")
    }
}