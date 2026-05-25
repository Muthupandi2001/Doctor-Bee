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

    // ✅ Reactive auth state — waits for Firebase to confirm before rendering NavHost
//    var initialDestination by remember { mutableStateOf<String?>(null) }

    var initialDestination = remember {
        if (SessionManager.isLoggedIn) Routes.MAINSCREEN else Routes.GET_STARTED
    }

    LaunchedEffect(Unit) {
        // ✅ Load theme settings on startup
        ThemePreferencesManager.loadThemeSettings { _, _, _, _ -> }
    }

//    LaunchedEffect(Unit) {
//        try {
//            auth.authStateChanged.collect { user ->
//                // ✅ Only set once on first launch — don't re-navigate on every auth event
//                if (initialDestination == null) {
//                    initialDestination = if (user != null) Routes.MAINSCREEN else Routes.GET_STARTED
//                }
//            }
//        } catch (e: Exception) {
//            // ✅ Fallback if authStateChanged is not available in your KMP Firebase version
//            initialDestination = if (auth.currentUser != null) Routes.MAINSCREEN else Routes.GET_STARTED
//            Napier.e("Auth state stream error: ${e.message}")
//        }
//    }

    // ✅ Deep link handler
    LaunchedEffect(deepLinkParams) {
        val referrerId = deepLinkParams?.referrerId
        if (deepLinkParams?.screen == "referral" && !referrerId.isNullOrBlank()) {
            Napier.d("Deep link referrerId received: $referrerId")
            navController.navigate(Routes.profile(referrerId)) {
                launchSingleTop = true
            }
        }
    }

    // ✅ Wait until auth state is confirmed before showing any screen
    if (initialDestination == null) return

    WonderBeeTheme(themeType = currentAppThemeSelection) {
        NavHost(
            navController = navController,
            startDestination = initialDestination
        ) {

            // ─────────────────────────────────────────
            // GET STARTED
            // ─────────────────────────────────────────
            composable(Routes.GET_STARTED) {
                GetStartedScreen(
                    onGetStarted = {
                        navController.navigate(Routes.LOGIN) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            // ─────────────────────────────────────────
            // LOGIN
            // ─────────────────────────────────────────
            composable(Routes.LOGIN) {
                LoginScreen(
                    navController = navController,
                    onLoginSuccess = {
                        navController.navigate(Routes.ONBOARDING) {
                            // ✅ Clear GET_STARTED and LOGIN from back stack
                            // so pressing back from onboarding doesn't return to login
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
                        navController.navigate(Routes.MAINSCREEN) {
                            // ✅ Clear onboarding from back stack
                            // so back button from MainScreen exits the app, not re-shows onboarding
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
                    // ✅ Logout clears the entire back stack and returns to GET_STARTED
//                    onLogoutSuccess  = {
//                        navController.navigate(Routes.GET_STARTED) {
//                            popUpTo(0) { inclusive = true }
//                            launchSingleTop = true
//                        }
//                    }
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