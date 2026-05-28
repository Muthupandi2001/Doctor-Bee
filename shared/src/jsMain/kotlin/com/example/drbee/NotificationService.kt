package com.example.drbee

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.database
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.first
import kotlin.js.Date

actual class NotificationService actual constructor() {

    companion object {
        private const val DB_URL     = "https://doctor-bee-2d622-default-rtdb.firebaseio.com/"
        private const val PROJECT_ID = "doctor-bee-2d622"
        private const val FCM_URL    = "https://fcm.googleapis.com/v1/projects/$PROJECT_ID/messages:send"
    }

    actual fun initialize() {}
    actual fun getFcmToken(onToken: (String) -> Unit) {}

    actual suspend fun sendPushNotification(
        recipientUserId : String,
        senderName      : String,
        messageText     : String,
        senderId        : String,
        roomId          : String
    ) {
        try {
            // ── Step 1: Read OAuth token that Android published to DB ──────────
            val oauthSnap = Firebase.database(DB_URL)
                .reference("fcm_oauth_token")
                .valueEvents
                .first()

            val oauthJson  = oauthSnap.value as? String
            if (oauthJson.isNullOrBlank()) {
                println("❌ [FCM-JS] No OAuth token in DB — using queue fallback")
                writeToQueue(recipientUserId, senderName, messageText, senderId, roomId)
                return
            }

            // Parse token and check expiry
            val parsed    = JSON.parse<dynamic>(oauthJson)
            val token     = parsed.token  as? String
            val expiresAt = parsed.expiresAt as? Double

            if (token.isNullOrBlank()) {
                println("❌ [FCM-JS] OAuth token missing — using queue fallback")
                writeToQueue(recipientUserId, senderName, messageText, senderId, roomId)
                return
            }

            val now = Date().getTime()
            if (expiresAt != null && now >= expiresAt - 60_000) {
                println("⚠ [FCM-JS] OAuth token expired — using queue fallback")
                writeToQueue(recipientUserId, senderName, messageText, senderId, roomId)
                return
            }

            // ── Step 2: Fetch recipient FCM token ─────────────────────────────
            val tokenSnap = Firebase.database(DB_URL)
                .reference("fcm_tokens")
                .child(recipientUserId)
                .valueEvents
                .first()

            val recipientToken = tokenSnap.value as? String
            if (recipientToken.isNullOrBlank()) {
                println("❌ [FCM-JS] No FCM token for uid=$recipientUserId — queue fallback")
                writeToQueue(recipientUserId, senderName, messageText, senderId, roomId)
                return
            }

            // ── Step 3: Build FCM V1 payload ──────────────────────────────────
            val truncated = if (messageText.length > 100)
                messageText.substring(0, 100) + "…" else messageText

            val notification = js("({})")
            notification.title = senderName.toString()
            notification.body  = truncated.toString()

            val dataObj = js("({})")
            dataObj.senderId = senderId.toString()
            dataObj.roomId   = roomId.toString()
            dataObj.type     = "chat_message"

            val androidNotif = js("({})")
            androidNotif.channel_id = "drbee_chat_channel"
            androidNotif.sound      = "default"

            val androidObj = js("({})")
            androidObj.priority     = "HIGH"
            androidObj.notification = androidNotif

            val msgObj = js("({})")
            msgObj.token        = recipientToken.toString()
            msgObj.notification = notification
            msgObj.data         = dataObj
            msgObj.android      = androidObj

            val payload = js("({})")
            payload.message = msgObj

            // ── Step 4: POST to FCM V1 API ────────────────────────────────────
            val headers = js("({})")
            headers["Authorization"] = "Bearer $token"
            headers["Content-Type"]  = "application/json"

            val fetchOptions = js("({})")
            fetchOptions.method  = "POST"
            fetchOptions.headers = headers
            fetchOptions.body    = JSON.stringify(payload)

            val response = kotlinx.browser.window
                .fetch(FCM_URL, fetchOptions)
                .await()

            val responseText = response.text().await()

            if (response.ok) {
                println("✅ [FCM-JS] Push sent directly to uid=$recipientUserId")
                println("✅ [FCM-JS] Response: $responseText")
            } else {
                println("❌ [FCM-JS] FCM error ${response.status}: $responseText")
                writeToQueue(recipientUserId, senderName, messageText, senderId, roomId)
            }

        } catch (e: Exception) {
            println("❌ [FCM-JS] Exception: ${e.message}")
            writeToQueue(recipientUserId, senderName, messageText, senderId, roomId)
        }
    }

    private suspend fun writeToQueue(
        recipientUserId : String,
        senderName      : String,
        messageText     : String,
        senderId        : String,
        roomId          : String
    ) {
        try {
            val uniqueKey = Date().getTime().toLong().toString()

            val jsPayload = js("""{
                senderName:  '',
                messageText: '',
                senderId:    '',
                roomId:      '',
                timestamp:   ''
            }""")
            jsPayload.senderName  = senderName.toString()
            jsPayload.messageText = messageText.toString()
            jsPayload.senderId    = senderId.toString()
            jsPayload.roomId      = roomId.toString()
            jsPayload.timestamp   = uniqueKey.toString()

            Firebase.database(DB_URL)
                .reference("notification_queue")
                .child(recipientUserId)
                .child(uniqueKey)
                .setValue(jsPayload.unsafeCast<Any>())

            println("✅ [FCM-JS] Queue fallback written")
        } catch (e: Exception) {
            println("❌ [FCM-JS] Queue error: ${e.message}")
        }
    }
}