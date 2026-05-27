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
    // ✅ onShareRequested, onPickImageRequested, onDecodeImageRequested all removed
) {
    val navController = rememberNavController()
    val auth          = remember { Firebase.auth }

    var initialDestination       by remember { mutableStateOf<String?>(null) }
    var isInitialized            by remember { mutableStateOf(false) }
    var pendingProfileNavigation by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        ThemePreferencesManager.loadThemeSettings { _, _, _, _ -> }
        auth.authStateChanged.collect { firebaseUser ->
            val sessionLoggedIn = SessionManager.isLoggedIn
            Napier.d("Auth state → uid=${firebaseUser?.uid}, session=$sessionLoggedIn")
            initialDestination = when {
                firebaseUser != null && sessionLoggedIn -> {
                    SessionManager.saveLoginState(true)
                    SessionManager.saveUserData(
                        userId = firebaseUser.uid,
                        email  = firebaseUser.email ?: "",
                        name   = SessionManager.savedUserName
                    )
                    if (SessionManager.isFreshLogin) NavRoutes.ONBOARDING else NavRoutes.MAINSCREEN
                }
                firebaseUser == null && sessionLoggedIn && !isInitialized -> NavRoutes.MAINSCREEN
                else -> { SessionManager.clearSession(); NavRoutes.GET_STARTED }
            }
            isInitialized = true
        }
    }

    LaunchedEffect(deepLinkParams) {
        val referrerId = deepLinkParams?.referrerId
        if (deepLinkParams?.screen == "referral"
            && !referrerId.isNullOrBlank()
            && referrerId != "null") {
            pendingProfileNavigation = referrerId
        }
    }

    val destination = initialDestination ?: return

    AppNavigation(
        navController            = navController,
        startDestination         = destination,
        pendingProfileNavigation = pendingProfileNavigation,
        onPendingProfileConsumed = { pendingProfileNavigation = null },
        onUserLoggedIn           = onUserLoggedIn
        // ✅ No share/image lambdas
    )
}