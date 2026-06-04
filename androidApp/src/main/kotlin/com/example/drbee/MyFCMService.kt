package com.example.drbee

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson

class MyFCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "onNewToken fired")
        FcmTokenHelper.onNewTokenReceived(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received: ${Gson().toJson(message)}")

        val title    = message.notification?.title ?: message.data["title"] ?: APP_NAME
        val body     = message.notification?.body  ?: message.data["body"]  ?: return
        val senderId = message.data["senderId"].orEmpty()
        val roomId   = message.data["roomId"].orEmpty()
        val type     = message.data["type"].orEmpty()

        showNotification(
            title    = title,
            body     = body,
            senderId = senderId,
            roomId   = roomId,
            type     = type
        )
    }

    private fun showNotification(
        title    : String,
        body     : String,
        senderId : String,
        roomId   : String,
        type     : String
    ) {
        // KEY FIX: Use a completely explicit intent with all required flags.
        //
        // For KILLED state: Android needs FLAG_ACTIVITY_NEW_TASK to start the
        // activity fresh. Without it, extras may be dropped on task rebuild.
        //
        // For LIVE/BACKGROUND state: FLAG_ACTIVITY_SINGLE_TOP ensures onNewIntent
        // is called on the existing instance instead of creating a new one.
        //
        // We set BOTH so it works in all states.
        val tapIntent = Intent(applicationContext, MainActivity::class.java).apply {
            action = "OPEN_CHAT_${System.currentTimeMillis()}"  // unique action prevents intent reuse
            flags  = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_SENDER_ID, senderId)
            putExtra(MainActivity.EXTRA_ROOM_ID,   roomId)
            putExtra(MainActivity.EXTRA_TYPE,      type)
        }

        Log.d(TAG, "Building notification tapIntent → senderId=$senderId type=$type")

        // KEY FIX: Use a unique requestCode per notification.
        // Using System.currentTimeMillis().toInt() ensures each notification
        // gets its own PendingIntent slot — they don't overwrite each other,
        // and Android doesn't reuse a stale cached PendingIntent from before the
        // app was killed (which would have no extras).
        //
        // FLAG_UPDATE_CURRENT alone is NOT enough when the app is killed because
        // Android may serve a cached PendingIntent. FLAG_IMMUTABLE + unique
        // requestCode forces a fresh PendingIntent every time.
        val requestCode   = System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            requestCode,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            // KEY FIX: Add the data as notification extras as a fallback.
            // If the PendingIntent extras are ever dropped by the system,
            // MainActivity can read them from getIntent().extras directly.
            .addExtras(android.os.Bundle().apply {
                putString(MainActivity.EXTRA_SENDER_ID, senderId)
                putString(MainActivity.EXTRA_ROOM_ID,   roomId)
                putString(MainActivity.EXTRA_TYPE,      type)
            })
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(requestCode, notification)
    }

    companion object {
        private const val TAG      = "MyFCMService"
        private const val APP_NAME = "DrBee"
        const val CHANNEL_ID       = "drbee_chat_channel"

        var pendingToken: String? = null

        fun saveTokenToDatabase(uid: String, token: String) =
            FcmTokenHelper.saveToDatabase(uid, token)

        fun flushPendingToken(uid: String) {
            val token = pendingToken ?: return
            Log.d(TAG, "Flushing pending token for uid=$uid")
            saveTokenToDatabase(uid, token)
            pendingToken = null
        }
    }
}