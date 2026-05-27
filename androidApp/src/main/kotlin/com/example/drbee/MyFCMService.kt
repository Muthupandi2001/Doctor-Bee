package com.example.drbee

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFCMService : FirebaseMessagingService() {

    companion object {
        private const val TAG        = "FCM"
        private const val DB_URL     = "https://doctor-bee-2d622-default-rtdb.firebaseio.com/"
        private const val CHANNEL_ID = "drbee_chat_channel"

        // Holds a token that arrived before the user was logged in
        var pendingToken: String? = null

        fun saveTokenToDatabase(uid: String, token: String) =
            FcmTokenHelper.saveToDatabase(uid, token)

        fun flushPendingToken(uid: String) {
            val token = pendingToken ?: return
            Log.d(TAG, "♻️ Flushing pending token for uid=$uid")
            saveTokenToDatabase(uid, token)
            pendingToken = null
        }
    }

    // ── Token refresh ─────────────────────────────────────────────────────────
    // Fires on: new install, data clear, FCM rotation, and when
    // SERVICE_NOT_AVAILABLE resolves itself on new devices
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "🔄 onNewToken fired")
        FcmTokenHelper.onNewTokenReceived(token)
    }

    // ── Incoming message ──────────────────────────────────────────────────────
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "📩 Message from: ${message.from}")

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "DrBee"

        val body = message.notification?.body
            ?: message.data["body"]
            ?: return   // nothing to show

        val senderId = message.data["senderId"].orEmpty()
        val roomId   = message.data["roomId"].orEmpty()

        showNotification(title, body, senderId, roomId)
    }

    // ── Show local notification ───────────────────────────────────────────────
    private fun showNotification(
        title    : String,
        body     : String,
        senderId : String,
        roomId   : String
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("OPEN_SENDER_ID", senderId)
            putExtra("OPEN_ROOM_ID",   roomId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}