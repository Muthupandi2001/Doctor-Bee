package com.example.drbee

import android.content.Context
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

actual class NotificationService actual constructor() {

    companion object {
        private const val PROJECT_ID = "doctor-bee-2d622"
        private const val DB_URL = "https://doctor-bee-2d622-default-rtdb.firebaseio.com/"
        private const val FCM_URL = "https://fcm.googleapis.com/v1/projects/$PROJECT_ID/messages:send"
        private const val SCOPE = "https://www.googleapis.com/auth/firebase.messaging"

        var appContext: Context? = null
            get() {
                if (field != null) return field
                return try {
                    val activityThread = Class.forName("android.app.ActivityThread")
                    val method = activityThread.getMethod("currentApplication")
                    method.invoke(null) as? Context
                } catch (e: Exception) {
                    android.util.Log.e("FCM", "❌ Cannot get context: ${e.message}")
                    null
                }
            }
    }

    // ✅ Called after login — starts listening to notification_queue
    actual fun initialize() {
        FirebaseMessaging.getInstance().isAutoInitEnabled = true
        listenForQueuedNotifications()   // ← THIS was missing
    }

    // ✅ Listens to notification_queue/{currentUserUid}/
    private fun listenForQueuedNotifications() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            android.util.Log.w("FCM", "⚠️ initialize() called before login — queue listener skipped")
            return
        }

        android.util.Log.d("FCM", "👂 Listening to notification_queue/$uid")

        FirebaseDatabase
            .getInstance(DB_URL)
            .getReference("notification_queue")
            .child(uid)
            .addChildEventListener(object : ChildEventListener {

                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val senderName  = snapshot.child("senderName").getValue(String::class.java)  ?: return
                    val messageText = snapshot.child("messageText").getValue(String::class.java) ?: return
                    val senderId    = snapshot.child("senderId").getValue(String::class.java)    ?: return
                    val roomId      = snapshot.child("roomId").getValue(String::class.java)      ?: return

                    android.util.Log.d("FCM", "📬 Queue entry received from $senderName")

                    // ✅ Delete immediately to avoid re-triggering on app restart
                    snapshot.ref.removeValue()

                    // ✅ Send the actual FCM push
                    CoroutineScope(Dispatchers.IO).launch {
                        sendPushNotification(
                            recipientUserId = uid,
                            senderName      = senderName,
                            messageText     = messageText,
                            senderId        = senderId,
                            roomId          = roomId
                        )
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {
                    android.util.Log.e("FCM", "❌ Queue listener cancelled: ${error.message}")
                }
            })
    }

    actual fun getFcmToken(onToken: (String) -> Unit) {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token: String -> onToken(token) }
            .addOnFailureListener { e ->
                android.util.Log.e("FCM", "Token fetch failed: ${e.message}")
            }
    }

    actual suspend fun sendPushNotification(
        recipientUserId: String,
        senderName: String,
        messageText: String,
        senderId: String,
        roomId: String
    ) = withContext(Dispatchers.IO) {
        try {
            val ctx = appContext ?: run {
                android.util.Log.e("FCM", "❌ appContext is null")
                return@withContext
            }

            // Step 1: Fetch recipient FCM token from DB
            val tokenSnapshot = Firebase.database(DB_URL)
                .reference("fcm_tokens")
                .child(recipientUserId)
                .valueEvents
                .first()

            val recipientToken = tokenSnapshot.value as? String
            if (recipientToken.isNullOrBlank()) {
                android.util.Log.e("FCM", "❌ No FCM token for $recipientUserId")
                return@withContext
            }

            // Step 2: Get OAuth2 access token from service account
            val credentials = GoogleCredentials
                .fromStream(ctx.assets.open("service_account.json"))
                .createScoped(listOf(SCOPE))
            credentials.refreshIfExpired()
            val accessToken = credentials.accessToken.tokenValue

            // Step 3: Build FCM V1 payload
            val payload = JSONObject().apply {
                put("message", JSONObject().apply {
                    put("token", recipientToken)
                    put("notification", JSONObject().apply {
                        put("title", senderName)
                        put("body",
                            if (messageText.length > 100)
                                messageText.substring(0, 100) + "…"
                            else messageText
                        )
                    })
                    put("data", JSONObject().apply {
                        put("senderId", senderId)
                        put("roomId", roomId)
                        put("type", "chat_message")
                    })
                    put("android", JSONObject().apply {
                        put("priority", "HIGH")
                        put("notification", JSONObject().apply {
                            put("channel_id", "drbee_chat_channel")
                            put("sound", "default")
                        })
                    })
                })
            }

            // Step 4: POST to FCM V1 endpoint
            val connection = (URL(FCM_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Content-Type", "application/json; UTF-8")
                doOutput = true
                connectTimeout = 10_000
                readTimeout = 10_000
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use {
                it.write(payload.toString())
                it.flush()
            }

            val code = connection.responseCode
            val responseBody = if (code == 200)
                connection.inputStream.bufferedReader().readText()
            else
                connection.errorStream?.bufferedReader()?.readText() ?: "no error body"

            if (code == 200)
                android.util.Log.d("FCM", "✅ Push sent to $recipientUserId")
            else
                android.util.Log.e("FCM", "❌ FCM error $code: $responseBody")

            connection.disconnect()

        } catch (e: Exception) {
            android.util.Log.e("FCM", "❌ Exception: ${e.message}", e)
        }
    }
}