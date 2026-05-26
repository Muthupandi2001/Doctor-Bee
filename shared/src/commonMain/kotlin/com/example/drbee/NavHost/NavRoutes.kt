package com.example.drbee.NavHost

data class DeepLinkParams(
    val screen: String? = null,
    val referrerId: String? = null
)

object NavRoutes {
    const val GET_STARTED     = "get_started"
    const val PROFILE_SETTING = "profile_setting"
    const val LOGIN           = "login"
    const val SIGNUP          = "signup"
    const val DASHBOARD       = "dashboard"
    const val MAINSCREEN      = "mainscreen"
    const val ONBOARDING      = "onBoarding"
    const val PROFILE         = "profile/{userId}"

    fun profile(userId: String) = "profile/$userId"
}