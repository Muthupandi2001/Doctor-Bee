package com.example.drbee.Helper


import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

object SessionManager {

    private val settings: Settings = Settings()

    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    var isLoggedIn: Boolean
        get() = settings.getBoolean(KEY_IS_LOGGED_IN, defaultValue = false)
        set(value) = settings.set(KEY_IS_LOGGED_IN, value)

    fun saveLoginState(loggedIn: Boolean) {
        isLoggedIn = loggedIn
    }

    fun clearSession() {
        isLoggedIn = false
    }
}