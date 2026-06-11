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
import com.example.drbee.Helper.NotificationEvent
import com.example.drbee.Helper.NotificationRouter
import com.example.drbee.NavHost.DeepLinkParams
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    var deepLinkParams by mutableStateOf<DeepLinkParams?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d(TAG, "Notification permission granted=$isGranted")
        if (isGranted) fetchAndStoreFcmToken()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityProvider.set(this)
        configureSystemBars()

        Log.d(TAG, "onCreate — action=${intent?.action} extras=${intent?.extras?.keySet()}")
        intent?.extras?.keySet()?.forEach { key ->
            Log.d(TAG, "  extra[$key] = ${intent.extras?.get(key)}")
        }

        parseDeepLink(intent?.data)?.let { deepLinkParams = it }


        routeIntent(intent)
        requestNotificationPermission()

        setContent {
            App(
                deepLinkParams = deepLinkParams,
                onUserLoggedIn = ::onUserLoggedIn
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        Log.d(TAG, "onNewIntent — action=${intent.action} extras=${intent.extras?.keySet()}")
        intent.extras?.keySet()?.forEach { key ->
            Log.d(TAG, "  extra[$key] = ${intent.extras?.get(key)}")
        }

        parseDeepLink(intent.data)?.let { deepLinkParams = it }
        routeIntent(intent)
    }

    private fun routeIntent(intent: Intent?) {
        if (intent == null) {
            Log.d(TAG, "routeIntent: intent is null — skipping")
            return
        }

        val senderId = intent.getStringExtra(EXTRA_SENDER_ID)
            ?: intent.getStringExtra("senderId")
        val roomId   = intent.getStringExtra(EXTRA_ROOM_ID)
            ?: intent.getStringExtra("roomId")
        val type     = intent.getStringExtra(EXTRA_TYPE)
            ?: intent.getStringExtra("type")

        Log.d(TAG, "routeIntent → senderId=$senderId roomId=$roomId type=$type")

        if (senderId.isNullOrBlank() || roomId.isNullOrBlank()) {
            Log.d(TAG, "routeIntent: no notification extras — nothing to route")
            return
        }

        when (type) {
            "chat_message" -> {
                Log.d(TAG, "routeIntent: posting OpenChat for senderId=$senderId")
                NotificationRouter.post(
                    NotificationEvent.OpenChat(otherUserId = senderId, roomId = roomId)
                )
            }
            "community" -> {
                Log.d(TAG, "routeIntent: posting OpenCommunity")
                NotificationRouter.post(NotificationEvent.OpenCommunity)
            }
            else -> Log.w(TAG, "routeIntent: unknown type=$type — no event posted")
        }
    }

    private fun parseDeepLink(uri: Uri?): DeepLinkParams? {
        if (uri == null) return null

        // Handle custom scheme: drbee://open?screen=referral&referrerId=xxx
        if (uri.scheme == "drbee" && uri.host == "open") {
            return DeepLinkParams(
                screen     = uri.getQueryParameter("screen"),
                referrerId = uri.getQueryParameter("referrerId")
            )
        }

        // Handle HTTPS Netlify deep link:
        // https://creative-bunny-e05f99.netlify.app?screen=referral&referrerId=xxx
        if (uri.scheme == "https" && uri.host == "gleaming-kringle-ce84e1.netlify.app") {
            return DeepLinkParams(
                screen     = uri.getQueryParameter("screen"),
                referrerId = uri.getQueryParameter("referrerId")
            )
        }

        return null
    }

    private fun onUserLoggedIn(uid: String) {
        Log.d(TAG, "onUserLoggedIn uid=$uid")
        NotificationService().initialize()
        FcmTokenHelper.saveTokenForUser(uid)
        MyFCMService.flushPendingToken(uid)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            fetchAndStoreFcmToken()
        }
    }

    private fun fetchAndStoreFcmToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (!uid.isNullOrBlank()) MyFCMService.saveTokenToDatabase(uid, token)
            else MyFCMService.pendingToken = token
        }
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge(
            statusBarStyle     = SystemBarStyle.dark("#000000".toColorInt()),
            navigationBarStyle = SystemBarStyle.dark("#000000".toColorInt())
        )
    }

    companion object {
        private const val TAG     = "MainActivity"
        const val EXTRA_SENDER_ID = "OPEN_SENDER_ID"
        const val EXTRA_ROOM_ID   = "OPEN_ROOM_ID"
        const val EXTRA_TYPE      = "OPEN_TYPE"
    }
}