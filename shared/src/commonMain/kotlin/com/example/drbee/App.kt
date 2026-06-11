package com.example.drbee

import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.example.drbee.Helper.SessionManager
import com.example.drbee.NavHost.AppNavigation
import com.example.drbee.NavHost.DeepLinkParams
import com.example.drbee.NavHost.NavRoutes
import com.example.drbee.ProfileScreen.ThemePreferencesManager
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import io.github.aakira.napier.Napier

@Composable
fun App(
    deepLinkParams : DeepLinkParams?,
    onUserLoggedIn : (uid: String) -> Unit
) {
    val navController = rememberNavController()
    val auth          = remember { Firebase.auth }

    var startDestination    by remember { mutableStateOf<String?>(null) }
    var isAuthResolved      by remember { mutableStateOf(false) }

    // ── Deep-link referral: store the referrer UID to show as a popup ─────
    // (replaces the old pendingProfileNavigation that navigated to EditProfile)
    var pendingReferrerId   by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        ThemePreferencesManager.loadThemeSettings { _, _, _, _ -> }
        auth.authStateChanged.collect { firebaseUser ->
            val sessionLoggedIn = SessionManager.isLoggedIn
            Napier.d("Auth → uid=${firebaseUser?.uid} session=$sessionLoggedIn")
            startDestination = when {
                firebaseUser != null && sessionLoggedIn -> {
                    SessionManager.saveLoginState(true)
                    SessionManager.saveUserData(
                        userId = firebaseUser.uid,
                        email  = firebaseUser.email ?: "",
                        name   = SessionManager.savedUserName
                    )
                    if (SessionManager.isFreshLogin) NavRoutes.ONBOARDING
                    else NavRoutes.MAINSCREEN
                }
                firebaseUser == null && sessionLoggedIn && !isAuthResolved -> NavRoutes.MAINSCREEN
                else -> { SessionManager.clearSession(); NavRoutes.GET_STARTED }
            }
            isAuthResolved = true
        }
    }

    // ── Parse deep link — store referrerId; do NOT navigate to EditProfile ─
    LaunchedEffect(deepLinkParams) {
        val referrerId = deepLinkParams?.referrerId
        if (deepLinkParams?.screen == "referral"
            && !referrerId.isNullOrBlank()
            && referrerId != "null"
        ) {
            pendingReferrerId = referrerId   // ← popup, not navigation
        }
    }

    val destination = startDestination ?: return

    AppNavigation(
        navController            = navController,
        startDestination         = destination,
        pendingReferrerId        = pendingReferrerId,          // ← new param
        onPendingReferrerConsumed = { pendingReferrerId = null }, // ← new param
        onUserLoggedIn           = onUserLoggedIn
    )
}