package com.example.drbee

import android.content.Context
import android.util.Log
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
        private const val TAG        = "FCM"
        private const val PROJECT_ID = "doctor-bee-2d622"
        private const val DB_URL     = "https://doctor-bee-2d622-default-rtdb.firebaseio.com/"
        private const val FCM_URL    = "https://fcm.googleapis.com/v1/projects/$PROJECT_ID/messages:send"
        private const val SCOPE      = "https://www.googleapis.com/auth/firebase.messaging"

        @Volatile private var listeningUid: String? = null

        var appContext: Context? = null
            get() {
                if (field != null) return field
                return try {
                    val activityThread = Class.forName("android.app.ActivityThread")
                    val method = activityThread.getMethod("currentApplication")
                    method.invoke(null) as? Context
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Cannot get context: ${e.message}")
                    null
                }
            }
    }

    actual fun initialize() {
        Log.d(TAG, ">>> initialize() called")
        FirebaseMessaging.getInstance().isAutoInitEnabled = true
        listenForQueuedNotifications()
        // ✅ Publish fresh OAuth token to DB so web can use it
        publishOAuthTokenToDb()
    }

    // ✅ NEW: Writes a fresh short-lived OAuth token into Firebase DB
    // Web reads this token to call FCM V1 API directly
    private fun publishOAuthTokenToDb() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val ctx = appContext ?: return@launch
                val credentials = GoogleCredentials
                    .fromStream(ctx.assets.open("service_account.json"))
                    .createScoped(listOf(SCOPE))
                credentials.refreshIfExpired()
                val token     = credentials.accessToken.tokenValue
                val expiresAt = credentials.accessToken.expirationTime?.time
                    ?: (System.currentTimeMillis() + 3600_000)

                val tokenObj = JSONObject().apply {
                    put("token",     token)
                    put("expiresAt", expiresAt)
                }

                FirebaseDatabase.getInstance(DB_URL)
                    .getReference("fcm_oauth_token")
                    .setValue(tokenObj.toString())
                    .addOnSuccessListener {
                        Log.d(TAG, "✅ OAuth token published to DB")
                    }
                    .addOnFailureListener {
                        Log.e(TAG, "❌ Failed to publish OAuth token: ${it.message}")
                    }

            } catch (e: Exception) {
                Log.e(TAG, "❌ publishOAuthTokenToDb error: ${e.message}")
            }
        }
    }

    private fun listenForQueuedNotifications() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        Log.d(TAG, ">>> listenForQueuedNotifications() uid=$uid")

        if (uid.isNullOrBlank()) {
            Log.w(TAG, "⚠ No user — skipping")
            return
        }
        if (listeningUid == uid) {
            Log.d(TAG, "ℹ Already listening uid=$uid")
            return
        }
        listeningUid = uid
        Log.d(TAG, "✅ Attaching listener notification_queue/$uid")

        FirebaseDatabase
            .getInstance(DB_URL)
            .getReference("notification_queue")
            .child(uid)
            .addChildEventListener(object : ChildEventListener {

                override fun onChildAdded(snapshot: DataSnapshot, prev: String?) {
                    Log.d(TAG, ">>> onChildAdded key=${snapshot.key}")
                    Log.d(TAG, ">>> raw value=${snapshot.value}")

                    val senderName  = snapshot.child("senderName").value?.toString()
                    val messageText = snapshot.child("messageText").value?.toString()
                    val senderId    = snapshot.child("senderId").value?.toString()
                    val roomId      = snapshot.child("roomId").value?.toString()
                    val isChat      = snapshot.child("isChat").value?.toString()
                        ?.toBooleanStrictOrNull() ?: true

                    Log.d(TAG, ">>> senderName=$senderName messageText=$messageText")

                    if (senderName == null || messageText == null ||
                        senderId == null || roomId == null) {
                        Log.e(TAG, "❌ Incomplete entry — deleting")
                        snapshot.ref.removeValue()
                        return
                    }

                    val nodeRef = snapshot.ref
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            sendPushNotification(
                                recipientUserId = uid,
                                senderName      = senderName,
                                messageText     = messageText,
                                senderId        = senderId,
                                roomId          = roomId,
                                isChat          = isChat
                            )
                            withContext(Dispatchers.Main) {
                                nodeRef.removeValue()
                                Log.d(TAG, "✅ Queue entry removed")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Failed: ${e.message}")
                        }
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, prev: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, prev: String?) {}
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "❌ Cancelled: ${error.message}")
                    listeningUid = null
                }
            })
    }

    actual fun getFcmToken(onToken: (String) -> Unit) {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { onToken(it) }
            .addOnFailureListener { Log.e(TAG, "❌ Token failed: ${it.message}") }
    }

    actual suspend fun sendPushNotification(
        recipientUserId : String,
        senderName      : String,
        messageText     : String,
        senderId        : String,
        roomId          : String,
        isChat          : Boolean
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, ">>> sendPushNotification recipient=$recipientUserId")
        try {
            val ctx = appContext ?: run {
                Log.e(TAG, "❌ appContext null")
                return@withContext
            }

            // Fetch recipient token
            val tokenSnap = Firebase.database(DB_URL)
                .reference("fcm_tokens")
                .child(recipientUserId)
                .valueEvents.first()

            val recipientToken = tokenSnap.value as? String
            if (recipientToken.isNullOrBlank()) {
                Log.e(TAG, "❌ No FCM token for $recipientUserId")
                return@withContext
            }

            // Get OAuth token
            val credentials = GoogleCredentials
                .fromStream(ctx.assets.open("service_account.json"))
                .createScoped(listOf(SCOPE))
            credentials.refreshIfExpired()
            val accessToken = credentials.accessToken.tokenValue

            val truncated = if (messageText.length > 100)
                messageText.substring(0, 100) + "…" else messageText

            val payload = JSONObject().apply {
                put("message", JSONObject().apply {
                    put("token", recipientToken)
                    put("notification", JSONObject().apply {
                        put("title", senderName)
                        put("body", truncated)
                    })
                    put("data", JSONObject().apply {
                        put("senderId", senderId)
                        put("roomId", roomId)
                      if (isChat){
                          put("type", "chat_message")
                      }else{
                          put("type", "community")
                      }
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

            val connection = (URL(FCM_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Content-Type", "application/json; UTF-8")
                doOutput      = true
                connectTimeout = 10_000
                readTimeout    = 10_000
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use {
                it.write(payload.toString())
                it.flush()
            }

            val code = connection.responseCode
            val body = if (code == 200)
                connection.inputStream.bufferedReader().readText()
            else
                connection.errorStream?.bufferedReader()?.readText() ?: "no body"

            if (code == 200) Log.d(TAG, "✅ Push sent to $recipientUserId")
            else Log.e(TAG, "❌ FCM error $code: $body")

            connection.disconnect()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception: ${e.message}", e)
        }
    }
}