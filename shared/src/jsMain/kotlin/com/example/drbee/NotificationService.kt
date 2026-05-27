// jsMain/kotlin/com/example/drbee/NotificationService.js.kt
package com.example.drbee

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.database
import kotlin.js.Date

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
        try {
            val databaseUrl = "https://doctor-bee-2d622-default-rtdb.firebaseio.com/"

            // Generate an explicit timestamp reference string
            val uniqueKey = Date().getTime().toLong().toString()

            // ✅ CRITICAL FIX: Construct an absolute Javascript Plain Object
            // This prevents Kotlin Map wrapper compilation layers from writing corrupt data properties
            val jsPayload = js("({})")
            jsPayload.senderName = senderName.toString()
            jsPayload.messageText = messageText.toString()
            jsPayload.senderId = senderId.toString()
            jsPayload.roomId = roomId.toString()
            jsPayload.timestamp = uniqueKey.toString()

            // Write straight to the network using the clean underlying structure
            Firebase.database(databaseUrl)
                .reference("notification_queue")
                .child(recipientUserId)
                .child(uniqueKey)
                .setValue(jsPayload.unsafeCast<Any>())

            println("🚀 JS Notification successfully queued under: notification_queue/$recipientUserId/$uniqueKey")

        } catch (e: Exception) {
            println("❌ JS push queue error: ${e.message}")
        }
    }
}
