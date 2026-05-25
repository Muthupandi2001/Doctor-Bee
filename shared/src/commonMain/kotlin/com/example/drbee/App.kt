package com.example.drbee

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.drbee.Authentication.DashboardScreen
import com.example.drbee.Authentication.GetStartedScreen
import com.example.drbee.Authentication.LoginScreen
import com.example.drbee.Authentication.SignupScreen
import com.example.drbee.Helper.SessionManager
import com.example.drbee.Helper.SessionManager.savedUserId
import com.example.drbee.MainScreen.MainScreen
import com.example.drbee.OnBoarding.OnBoardingScreen
import com.example.drbee.ProfileScreen.ProfileScreen
import com.example.drbee.ProfileScreen.ThemePreferencesManager
import com.example.drbee.ProfileScreen.ThemePreferencesManager.currentAppThemeSelection
import com.example.drbee.ProfileScreen.WonderBeeTheme
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import io.github.aakira.napier.Napier

object Routes {
    const val GET_STARTED = "get_started"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val DASHBOARD = "dashboard"
    const val MAINSCREEN = "mainscreen"
    const val ONBOARDING = "onBoarding"
    const val PROFILE = "profile/{userId}"

    fun profile(userId: String) = "profile/$userId"
}

data class DeepLinkParams(
    val screen: String? = null,
    val referrerId: String? = null
)

@Composable
fun App(
    deepLinkParams: DeepLinkParams?,
    onShareRequested: (String) -> Unit
) {
    val navController = rememberNavController()
    val auth = remember { Firebase.auth }

    // ✅ App starts with a safe null destination to avoid initialization races
    var initialDestination by remember { mutableStateOf<String?>(null) }

    // ✅ Tracks if we have processed our first definitive auth sweep
    var isInitialized by remember { mutableStateOf(false) }
    val currentUid = auth.currentUser?.uid ?: ""

    LaunchedEffect(Unit) {
        ThemePreferencesManager.loadThemeSettings { _, _, _, _ -> }

        // ✅ GitLive uses .authStateChanged instead of .authStateFlow
        auth.authStateChanged.collect { firebaseUser ->
            val sessionLoggedIn = SessionManager.isLoggedIn

            Napier.d("GitLive Auth State → uid=${firebaseUser?.uid}, sessionLoggedIn=$sessionLoggedIn, fresh=${SessionManager.isFreshLogin}")

            initialDestination = when {
                // ✅ FIX: If both are valid, check if it's a brand new login session
                firebaseUser != null && sessionLoggedIn -> {
                    Napier.d("App → MAINSCREEN (Restoring Cache from Firebase)")
                    SessionManager.saveLoginState(true)
                    SessionManager.saveUserData(
                        userId = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        name = SessionManager.savedUserName
                    )
                    if (SessionManager.isFreshLogin) {
                        Napier.d("App → ONBOARDING (Fresh login detected)")
                        Routes.ONBOARDING
                    } else {
                        Napier.d("App → MAINSCREEN (Returning user)")
                        Routes.MAINSCREEN
                    }
                }

                firebaseUser == null && sessionLoggedIn && !isInitialized -> {
                    Napier.d("App → Waiting for GitLive Firebase to finish initialization...")
                    Routes.MAINSCREEN
                }
                else -> {
                    Napier.d("App → GET_STARTED (User is completely logged out)")
                    SessionManager.clearSession()
                    Routes.GET_STARTED
                }
            }

            isInitialized = true
        }

    }

    LaunchedEffect(deepLinkParams) {
        val referrerId = deepLinkParams?.referrerId
        if (deepLinkParams?.screen == "referral" && !referrerId.isNullOrBlank()) {
            Napier.d("Deep link referrerId received: $referrerId")
            navController.navigate(Routes.profile(referrerId)) {
                launchSingleTop = true
            }
        }
    }

    if (initialDestination == null) return

    WonderBeeTheme(themeType = currentAppThemeSelection) {
        NavHost(
            navController = navController,
            startDestination = initialDestination!!
        ) {

            composable(Routes.GET_STARTED) {
                GetStartedScreen(
                    onGetStarted = {
                        navController.navigate(Routes.LOGIN) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Routes.LOGIN) {
                LoginScreen(
                    navController = navController,
                    onLoginSuccess = {
                        val firebaseUser = Firebase.auth.currentUser
                        SessionManager.saveUserID(
                            userId = firebaseUser?.uid ?: "",
                        )
                        navController.navigate(Routes.ONBOARDING) {
                            popUpTo(Routes.GET_STARTED) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToSignup = {
                        navController.navigate(Routes.SIGNUP) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            // ─────────────────────────────────────────
            // SIGNUP
            // ─────────────────────────────────────────
            composable(Routes.SIGNUP) {
                SignupScreen(
                    onNavigateToLogin = {
                        navController.navigate(Routes.LOGIN) {
//                            popUpTo(Routes.SIGNUP) { inclusive = true }
//                            launchSingleTop = true
                        }
                    }
                )
            }

            // ─────────────────────────────────────────
            // DASHBOARD
            // ─────────────────────────────────────────
            composable(Routes.DASHBOARD) {
                DashboardScreen()
            }

            // ─────────────────────────────────────────
            // ONBOARDING
            // ─────────────────────────────────────────
            composable(Routes.ONBOARDING) {
                OnBoardingScreen(
                    onBoardingFinished = {
                        SessionManager.isFreshLogin = false
                        navController.navigate(Routes.MAINSCREEN) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // ─────────────────────────────────────────
            // MAIN SCREEN
            // ─────────────────────────────────────────
            composable(Routes.MAINSCREEN) {
                MainScreen(
                    navController = navController,
                    onShareRequested = { userId -> onShareRequested(userId) },
                    // ✅ Pass logout handler into MainScreen
                    onLogoutSuccess = {
                        navController.navigate(Routes.GET_STARTED) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // ─────────────────────────────────────────
            // PROFILE (deep link target)
            // ─────────────────────────────────────────
//            composable(Routes.PROFILE) { backStackEntry ->
//                val userId: String = backStackEntry
//                    .arguments
//                    ?.getString("userId")   // ✅ Correct way to read path params (not savedStateHandle)
//                    ?: ""
//
//                ProfileScreen(
//                    userId           = userId,
//                    onBack           = { navController.popBackStack() },
//                    onShareRequested = { id -> onShareRequested(id) },
//                    // ✅ Logout from profile also clears everything
//                    onLogoutSuccess  = {
//                        navController.navigate(Routes.GET_STARTED) {
//                            popUpTo(0) { inclusive = true }
//                            launchSingleTop = true
//                        }
//                    }
//                )
//            }


            // ✅ Profile route — receives userId from path segment
            composable(Routes.PROFILE) { backStackEntry ->
                // ✅ KMP-safe — reads directly from savedStateHandle
                val userId: String = backStackEntry
                    .savedStateHandle
                    .get<String>("userId")
                    ?: ""

                ProfileScreen(
                    userId = userId,
                    onBack = { navController.popBackStack() },
                    onShareRequested = { id -> onShareRequested(id) }
                )
            }
        }
    }
}