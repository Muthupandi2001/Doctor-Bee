package com.example.drbee  // ✅ same package

actual class NotificationService actual constructor() {

    actual fun initialize() {
        // Web/JS — no-op
    }

    actual fun getFcmToken(onToken: (String) -> Unit) {
        // Web/JS — no-op
    }

    actual suspend fun sendPushNotification(
        recipientUserId : String,
        senderName      : String,
        messageText     : String,
        senderId        : String,
        roomId          : String
    ) {
        // Web/JS — no-op
    }
}