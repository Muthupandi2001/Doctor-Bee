package com.example.drbee.NavHost

/**
 * Single source of truth for all navigation routes in the app.
 * Every screen route is defined here — no magic strings scattered across the codebase.
 */
object NavRoutes {

    // ── Auth / Onboarding ────────────────────────────────────────────────────
    const val GET_STARTED = "get_started"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val DASHBOARD = "dashboard"
    const val ONBOARDING = "onboarding"

    // ── Main shell ───────────────────────────────────────────────────────────
    const val MAINSCREEN = "main_screen"

    // ── Profile ──────────────────────────────────────────────────────────────
    const val EDIT_PROFILE = "editprofile?userId={userId}"
    const val PROFILE_SETTING = "profile_setting"

    fun editprofile(userId: String) = "editprofile?userId=$userId"

    // ── Chat ─────────────────────────────────────────────────────────────────
    // Full-screen chat detail reachable from ANY tab (notification tap, deep link, etc.)
    // Route: chat_detail/{otherUserId}
    const val CHAT_DETAIL = "chat_detail/{otherUserId}"

    fun chatDetail(otherUserId: String) = "chat_detail/$otherUserId"
}

/** Carries data from a deep link (custom scheme: drbee://open?...) */
data class DeepLinkParams(
    val screen: String?,
    val referrerId: String?
)