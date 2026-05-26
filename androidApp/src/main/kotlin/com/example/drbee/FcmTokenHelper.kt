package com.example.drbee

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging

object FcmTokenHelper {

    private const val DB_URL =
        "https://doctor-bee-2d622-default-rtdb.firebaseio.com/"

    fun saveTokenForUser(uid: String) {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                FirebaseDatabase
                    .getInstance(DB_URL)
                    .getReference("fcm_tokens")
                    .child(uid)
                    .setValue(token)
                    .addOnSuccessListener {
                        Log.d("FCM", "✅ Token saved for uid=$uid")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FCM", "❌ Token save failed: ${e.message}")
                    }
                MyFCMService.pendingToken = null
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "❌ Token fetch failed: ${e.message}")
            }
    }
}