package com.example.drbee

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.*

object FcmTokenHelper {

    private const val TAG       = "FCM"
    private const val DB_URL    = "https://doctor-bee-2d622-default-rtdb.firebaseio.com/"
    private const val MAX_RETRY = 5

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── Called after login / signup ───────────────────────────────────────────
    fun saveTokenForUser(uid: String) {
        if (uid.isBlank()) return
        fetchWithRetry(uid)
    }

    // ── Called on app start for already-logged-in users ───────────────────────
    fun initForCurrentUser(uid: String) {
        if (uid.isBlank()) return
        fetchWithRetry(uid)
    }

    // ── Core retry loop ───────────────────────────────────────────────────────
    private fun fetchWithRetry(uid: String, attempt: Int = 1) {
        scope.launch {
            // Exponential backoff: 0s, 2s, 4s, 8s, 16s
            val delayMs = if (attempt == 1) 0L else minOf((1L shl attempt) * 1000L, 32_000L)
            if (delayMs > 0) {
                Log.d(TAG, "⏳ Retry attempt $attempt in ${delayMs}ms — uid=$uid")
                delay(delayMs)
            }

            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    Log.d(TAG, "✅ Token fetched (attempt $attempt) — uid=$uid")
                    saveToDatabase(uid, token)
                    PendingTokenStore.clear()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Attempt $attempt failed: ${e.message}")
                    if (attempt < MAX_RETRY) {
                        fetchWithRetry(uid, attempt + 1)
                    } else {
                        Log.e(TAG, "🚫 All retries exhausted — uid=$uid. Will save via onNewToken.")
                        // onNewToken() fires automatically when FCM recovers
                        PendingTokenStore.set(uid)
                    }
                }
        }
    }

    // ── Called from MyFCMService.onNewToken ───────────────────────────────────
    fun onNewTokenReceived(token: String) {
        // Priority 1: pending uid from failed retries
        val pendingUid = PendingTokenStore.get()
        if (!pendingUid.isNullOrBlank()) {
            Log.d(TAG, "♻️ onNewToken — saving for pending uid=$pendingUid")
            saveToDatabase(pendingUid, token)
            PendingTokenStore.clear()
            return
        }

        // Priority 2: currently logged-in user
        val currentUid = com.google.firebase.auth.FirebaseAuth
            .getInstance().currentUser?.uid
        if (!currentUid.isNullOrBlank()) {
            Log.d(TAG, "♻️ onNewToken — saving for current uid=$currentUid")
            saveToDatabase(currentUid, token)
        } else {
            // No user yet — hold token until flushPendingToken() is called
            Log.w(TAG, "⚠️ onNewToken — no user yet, queuing token")
            MyFCMService.pendingToken = token
        }
    }

    // ── Shared DB write ───────────────────────────────────────────────────────
    fun saveToDatabase(uid: String, token: String) {
        if (uid.isBlank() || token.isBlank()) return
        FirebaseDatabase.getInstance(DB_URL)
            .getReference("fcm_tokens")
            .child(uid)
            .setValue(token)
            .addOnSuccessListener { Log.d(TAG, "✅ Token saved — uid=$uid") }
            .addOnFailureListener { Log.e(TAG, "❌ Token save failed: ${it.message}") }
    }
}

// ── Stores uid when retries fail so onNewToken can pick it up ─────────────────
private object PendingTokenStore {
    private var uid: String? = null
    fun set(value: String) { uid = value }
    fun get(): String?      = uid
    fun clear()             { uid = null }
}