package com.example.drbee

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "🔄 Token refreshed")

        // Use native FirebaseAuth — safe inside a Service
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (!uid.isNullOrBlank()) {
            saveTokenToDatabase(uid, token)
        } else {
            pendingToken = token
            Log.w("FCM", "⚠️ Token refresh queued — user not logged in")
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("FCM", "📩 Foreground message: ${message.notification?.body}")
    }

    companion object {
        private const val DB_URL =
            "https://doctor-bee-2d622-default-rtdb.firebaseio.com/"

        var pendingToken: String? = null

        fun saveTokenToDatabase(uid: String, token: String) {
            FirebaseDatabase
                .getInstance(DB_URL)
                .getReference("fcm_tokens")
                .child(uid)
                .setValue(token)
                .addOnSuccessListener {
                    android.util.Log.d("FCM", "✅ Token saved for uid=$uid")
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("FCM", "❌ Token save failed: ${e.message}")
                }
        }

        fun flushPendingToken(uid: String) {
            val token = pendingToken ?: return
            saveTokenToDatabase(uid, token)
            pendingToken = null
        }
    }
}