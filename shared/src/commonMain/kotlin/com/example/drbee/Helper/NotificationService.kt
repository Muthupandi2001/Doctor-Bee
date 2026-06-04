package com.example.drbee

expect class NotificationService() {
    fun initialize()
    fun getFcmToken(onToken: (String) -> Unit)
    suspend fun sendPushNotification(
        recipientUserId: String,
        senderName: String,
        messageText: String,
        senderId: String,
        roomId: String,
        isChat: Boolean
    )
}