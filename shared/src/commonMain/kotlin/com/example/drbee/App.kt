package com.example.drbee

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
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
    deepLinkParams: DeepLinkParams?,
    onShareRequested: (String) -> Unit,
    onPickImageRequested: ((String) -> Unit) -> Unit,
    onDecodeImageRequested: (String, (ImageBitmap?) -> Unit) -> Unit,
    onUserLoggedIn: (uid: String) -> Unit
) {
    val navController = rememberNavController()
    val auth          = remember { Firebase.auth }

    var initialDestination      by remember { mutableStateOf<String?>(null) }
    var isInitialized           by remember { mutableStateOf(false) }
    var pendingProfileNavigation by remember { mutableStateOf<String?>(null) }

    // ── Auth state: resolves the start destination once on launch ──────────
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
                    if (SessionManager.isFreshLogin) NavRoutes.ONBOARDING
                    else NavRoutes.MAINSCREEN
                }
                firebaseUser == null && sessionLoggedIn && !isInitialized -> NavRoutes.MAINSCREEN
                else -> {
                    SessionManager.clearSession()
                    NavRoutes.GET_STARTED
                }
            }
            isInitialized = true
        }
    }

    // ── Deep link: store referrer ID, navigate once graph is ready ─────────
    LaunchedEffect(deepLinkParams) {
        val referrerId = deepLinkParams?.referrerId
        val isValid    = !referrerId.isNullOrBlank() && referrerId != "null"
        if (deepLinkParams?.screen == "referral" && isValid) {
            pendingProfileNavigation = referrerId
        }
    }

    // ── Wait for start destination before composing the graph ─────────────
    val destination = initialDestination ?: return

    AppNavigation(
        navController              = navController,
        startDestination           = destination,
        pendingProfileNavigation   = pendingProfileNavigation,
        onPendingProfileConsumed   = { pendingProfileNavigation = null },
        onShareRequested           = onShareRequested,
        onPickImageRequested       = onPickImageRequested,
        onDecodeImageRequested     = onDecodeImageRequested,
        onUserLoggedIn             = onUserLoggedIn
    )
}