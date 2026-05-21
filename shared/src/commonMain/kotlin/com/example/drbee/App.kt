package com.example.drbee

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.drbee.Authentication.DashboardScreen
import com.example.drbee.Authentication.GetStartedScreen
import com.example.drbee.Authentication.LoginScreen
import com.example.drbee.Authentication.SignupScreen
import com.example.drbee.MainScreen.MainScreen
import com.example.drbee.OnBoarding.OnBoardingScreen
import com.example.drbee.ProfileScreen.ThemePreferencesManager
import com.example.drbee.ProfileScreen.ThemePreferencesManager.currentAppThemeSelection

// ✅ FIX 1: Import your custom Theme wrapper and global State controller flag
import com.example.drbee.ProfileScreen.WonderBeeTheme
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth

// Stub class to make your snippet compile immediately
class AuthRepository {
    suspend fun login(e: String, p: String): Result<Unit> = Result.success(Unit)
}


object Routes {
    const val GET_STARTED = "get_started"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val DASHBOARD = "dashboard"
    const val MAINSCREEN = "mainscreen"
    const val ONBOARDING = "onBoarding"
}

@Composable
fun App() {
    val navController = rememberNavController()

    val auth = remember { Firebase.auth }

    val initialDestination = remember {
        if (auth.currentUser != null) Routes.MAINSCREEN else Routes.GET_STARTED
    }

    LaunchedEffect(Unit) {
        ThemePreferencesManager.loadThemeSettings { _, _, _, _ ->
            // Cursors are not present at this root layer level, ignoring values cleanly
        }
    }


    WonderBeeTheme(themeType = currentAppThemeSelection) {
        NavHost(
            navController = navController,
            startDestination = initialDestination // ✅ Automatically routes based on active token state
        ) {
            composable(Routes.GET_STARTED) {
                GetStartedScreen(
                    onGetStarted = {
                        // Keep the history stack so hitting back on Login doesn't close the app
                        navController.navigate(Routes.LOGIN)
                    }
                )
            }

            composable(Routes.LOGIN) {
                LoginScreen(
                    navController = navController, // Pass navController inside to execute back action
                    onLoginSuccess = {
                        navController.navigate(Routes.ONBOARDING)
                    },
                    onNavigateToSignup = {
                        navController.navigate(Routes.SIGNUP)
                    }
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
                MainScreen(navController)
            }

            composable(Routes.ONBOARDING) {
                OnBoardingScreen(
                    onBoardingFinished = {
                        navController.navigate(Routes.MAINSCREEN)
                    }
                )
            }
        }
    }
}
