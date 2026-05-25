package com.example.drbee.Helper

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import io.github.aakira.napier.Napier

object SessionManager {

    private val settings: Settings by lazy { Settings() }

    private const val KEY_IS_LOGGED_IN  = "is_logged_in"
    private const val KEY_USER_ID       = "saved_user_id"
    private const val KEY_USER_EMAIL    = "saved_user_email"
    private const val KEY_USER_NAME     = "saved_user_name"

    var isFreshLogin: Boolean = false

    // ─────────────────────────────────────────
    // LOGIN STATE
    // ─────────────────────────────────────────
    var isLoggedIn: Boolean
        get() {
            val value = settings.getBoolean(KEY_IS_LOGGED_IN, defaultValue = false)
            Napier.d("SessionManager.isLoggedIn READ → $value")
            return value
        }
        set(value) {
            Napier.d("SessionManager.isLoggedIn WRITE → $value")
            settings.set(KEY_IS_LOGGED_IN, value)
        }

    // ─────────────────────────────────────────
    // USER DATA
    // ─────────────────────────────────────────
    var savedUserId: String
        get() = settings.getString(KEY_USER_ID, defaultValue = "")
        set(value) = settings.set(KEY_USER_ID, value)

    var savedUserEmail: String
        get() = settings.getString(KEY_USER_EMAIL, defaultValue = "")
        set(value) = settings.set(KEY_USER_EMAIL, value)

    var savedUserName: String
        get() = settings.getString(KEY_USER_NAME, defaultValue = "")
        set(value) = settings.set(KEY_USER_NAME, value)

    // ─────────────────────────────────────────
    // SAVE on login
    // ─────────────────────────────────────────
    fun saveLoginState(loggedIn: Boolean) {
        Napier.d("SessionManager.saveLoginState($loggedIn) called")
        isLoggedIn = loggedIn
        // Verify it was actually written
        Napier.d("SessionManager verify after write → ${settings.getBoolean(KEY_IS_LOGGED_IN, false)}")
    }

    fun saveUserData(userId: String, email: String, name: String) {
        savedUserId    = userId
        savedUserEmail = email
        savedUserName  = name
        Napier.d("SessionManager.saveUserData → userId=$userId email=$email name=$name")
    }

    fun saveUserID(userId: String) {
        savedUserId = userId
    }

    // ─────────────────────────────────────────
    // CLEAR on logout
    // ─────────────────────────────────────────
    fun clearSession() {
        Napier.d("SessionManager.clearSession() called — wiping all keys")
        isLoggedIn     = false
        savedUserId    = ""
        savedUserEmail = ""
        savedUserName  = ""
    }
}