//package com.example.drbee.Service
//import com.google.firebase.messaging.FirebaseMessagingService
//import com.google.firebase.messaging.RemoteMessage
//class DrBeeFCMService : FirebaseMessagingService() {
//    companion object {
//        const val CHANNEL_ID        = "drbee_chat_channel"
//        const val CHANNEL_NAME      = "Chat Notifications"
//        const val EXTRA_SENDER_ID   = "senderId"
//        const val EXTRA_SENDER_NAME = "senderName"
//    }
//
//    // ── Called when app is FOREGROUND or BACKGROUND ───────────────────────
//    override fun onMessageReceived(message: RemoteMessage) {
//        super.onMessageReceived(message)
//
//        val senderId   = message.data["senderId"]   ?: return
//        val senderName = message.data["senderName"] ?: "Someone"
//        val body       = message.data["body"]       ?: "Sent you a message"
//
//        showChatNotification(
//            senderId   = senderId,
//            senderName = senderName,
//            body       = body
//        )
//    }
//
//    // ── FCM token refresh ─────────────────────────────────────────────────
//    override fun onNewToken(token: String) {
//        super.onNewToken(token)
//        // TODO: Save this token to your Firebase database under the current user
//        // Firebase.database.reference("users/{uid}/fcmToken").setValue(token)
//    }
//
//    // ── Build & show notification ─────────────────────────────────────────
//    private fun showChatNotification(
//        senderId: String,
//        senderName: String,
//        body: String
//    ) {
//        val notificationManager = getSystemService(
//            Context.NOTIFICATION_SERVICE
//        ) as NotificationManager
//
//        // Create channel (required Android 8+)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                CHANNEL_ID,
//                CHANNEL_NAME,
//                NotificationManager.IMPORTANCE_HIGH
//            ).apply {
//                description = "DrBee chat message notifications"
//                enableVibration(true)
//            }
//            notificationManager.createNotificationChannel(channel)
//        }
//
//        // ✅ Intent carries senderId so MainActivity can navigate to that chat
//        val clickIntent = Intent(this, MainActivity::class.java).apply {
//            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
//            putExtra(EXTRA_SENDER_ID,   senderId)
//            putExtra(EXTRA_SENDER_NAME, senderName)
//        }
//
//        val pendingIntent = PendingIntent.getActivity(
//            this,
//            senderId.hashCode(),    // unique per sender so taps don't collide
//            clickIntent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
//            .setSmallIcon(android.R.drawable.ic_dialog_email)
//            .setContentTitle(senderName)
//            .setContentText(body)
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//            .setAutoCancel(true)
//            .setContentIntent(pendingIntent)
//            .build()
//
//        notificationManager.notify(senderId.hashCode(), notification)
//    }
//}