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

            // ✅ kotlin.js.Date works in jsMain — no Clock.System needed
            val uniqueKey = Date().getTime().toLong().toString()

            Firebase.database(databaseUrl)
                .reference("notification_queue")
                .child(recipientUserId)
                .child(uniqueKey)
                .setValue(
                    mapOf(
                        "senderName"  to senderName,
                        "messageText" to messageText,
                        "senderId"    to senderId,
                        "roomId"      to roomId,
                        "timestamp"   to uniqueKey
                    )
                )

        } catch (e: Exception) {
            println("❌ JS push queue error: ${e.message}")
        }
    }
}