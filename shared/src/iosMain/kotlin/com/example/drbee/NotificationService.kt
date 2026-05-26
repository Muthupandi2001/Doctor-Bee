package com.example.drbee

actual class NotificationService actual constructor() {

    actual fun initialize() {}

    actual fun getFcmToken(onToken: (String) -> Unit) {}

    actual suspend fun sendPushNotification(
        recipientUserId: String,
        senderName: String,
        messageText: String,
        senderId: String,
        roomId: String
    ) {}
}