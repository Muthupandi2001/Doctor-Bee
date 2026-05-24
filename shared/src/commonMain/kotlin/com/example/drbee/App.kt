package com.example.drbee

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.drbee.Authentication.DashboardScreen
import com.example.drbee.Authentication.GetStartedScreen
import com.example.drbee.Authentication.LoginScreen
import com.example.drbee.Authentication.SignupScreen
import com.example.drbee.MainScreen.MainScreen
import com.example.drbee.OnBoarding.OnBoardingScreen
import com.example.drbee.ProfileScreen.ProfileScreen
import com.example.drbee.ProfileScreen.ThemePreferencesManager
import com.example.drbee.ProfileScreen.ThemePreferencesManager.currentAppThemeSelection
import com.example.drbee.ProfileScreen.WonderBeeTheme
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import io.github.aakira.napier.Napier
import androidx.navigation.NavBackStackEntry

object Routes {
    const val GET_STARTED = "get_started"
    const val LOGIN       = "login"
    const val SIGNUP      = "signup"
    const val DASHBOARD   = "dashboard"
    const val MAINSCREEN  = "mainscreen"
    const val ONBOARDING  = "onBoarding"
    const val PROFILE     = "profile/{userId}"           // ✅ new route

    fun profile(userId: String) = "profile/$userId"      // ✅ helper so no typos
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

    val initialDestination = remember {
        if (auth.currentUser != null) Routes.MAINSCREEN else Routes.GET_STARTED
    }

    // ✅ Deep link handler — navigates to ProfileScreen when referrerId arrives
    LaunchedEffect(deepLinkParams) {
        val referrerId = deepLinkParams?.referrerId
        if (deepLinkParams?.screen == "referral" && !referrerId.isNullOrBlank()) {
            Napier.d("Deep link referrerId received: $referrerId")
            navController.navigate(Routes.profile(referrerId)) {
                // Don't stack duplicate profile screens on rotation / re-trigger
                launchSingleTop = true
            }
        }
    }

//    LaunchedEffect(notificationChatTarget) {
//        val target = notificationChatTarget
//        if (target != null) {
//            navController.navigate(Routes.chat(target.userId, target.name)) {
//                launchSingleTop = true
//            }
//        }
//    }

    LaunchedEffect(Unit) {
        ThemePreferencesManager.loadThemeSettings { _, _, _, _ -> }
    }



//    val systemUiController = rememberSystemUiController()
//
//    SideEffect {
//        systemUiController.setStatusBarColor(
//            color = Color.Black,
//            darkIcons = false
//        )
//    }



    WonderBeeTheme(themeType = currentAppThemeSelection) {
        NavHost(
            navController    = navController,
            startDestination = initialDestination
        ) {
            composable(Routes.GET_STARTED) {
                GetStartedScreen(
                    onGetStarted = { navController.navigate(Routes.LOGIN) }
                )
            }

            composable(Routes.LOGIN) {
                LoginScreen(
                    navController     = navController,
                    onLoginSuccess    = { navController.navigate(Routes.ONBOARDING) },
                    onNavigateToSignup= { navController.navigate(Routes.SIGNUP) }
                )
            }

            composable(Routes.SIGNUP) {
                SignupScreen(
                    onNavigateToLogin = {
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.SIGNUP) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.DASHBOARD) {
                DashboardScreen()
            }

            composable(Routes.MAINSCREEN) {
                MainScreen(
                    navController    = navController,
                    onShareRequested = { userId -> onShareRequested(userId) }
                )
            }

            composable(Routes.ONBOARDING) {
                OnBoardingScreen(
                    onBoardingFinished = { navController.navigate(Routes.MAINSCREEN) }
                )
            }

            // ✅ Profile route — receives userId from path segment
            composable(Routes.PROFILE) { backStackEntry ->
                // ✅ KMP-safe — reads directly from savedStateHandle
                val userId: String = backStackEntry
                    .savedStateHandle
                    .get<String>("userId")
                    ?: ""

                ProfileScreen(
                    userId           = userId,
                    onBack           = { navController.popBackStack() },
                    onShareRequested = { id -> onShareRequested(id) }
                )
            }
        }
    }
}