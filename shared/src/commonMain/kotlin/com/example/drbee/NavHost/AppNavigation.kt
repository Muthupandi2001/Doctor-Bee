package com.example.drbee.NavHost  // ✅ fix 1: lowercase package name

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.navigation.NavHostController             // ✅ fix 2: correct type
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable           // ✅ fix 3: missing import
import androidx.navigation.navArgument
import com.example.drbee.Authentication.DashboardScreen
import com.example.drbee.Authentication.GetStartedScreen
import com.example.drbee.Authentication.LoginScreen
import com.example.drbee.Authentication.SignupScreen
import com.example.drbee.Helper.SessionManager
import com.example.drbee.MainScreen.MainScreen
import com.example.drbee.NavHost.NavRoutes
import com.example.drbee.OnBoarding.OnBoardingScreen
import com.example.drbee.ProfileScreen.EditProfileProfile
import com.example.drbee.ProfileScreen.ThemePreferencesManager.currentAppThemeSelection
import com.example.drbee.ProfileScreen.WonderBeeTheme
import com.example.drbee.ProfileScreen.profileScreenKMP
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.delay

@Composable
fun AppNavigation(
    navController: NavHostController,                  // ✅ fix 2: was NavController
    startDestination: String,
    pendingProfileNavigation: String?,
    onPendingProfileConsumed: () -> Unit,
    onShareRequested: (String) -> Unit,
    onPickImageRequested: ((String) -> Unit) -> Unit,
    onDecodeImageRequested: (String, (ImageBitmap?) -> Unit) -> Unit,
    onUserLoggedIn: (uid: String) -> Unit
) {
    LaunchedEffect(startDestination, pendingProfileNavigation) {
        val target = pendingProfileNavigation ?: return@LaunchedEffect
        delay(300)
        navController.navigate(NavRoutes.profile(target)) {
            launchSingleTop = true
        }
        onPendingProfileConsumed()
    }

    WonderBeeTheme(themeType = currentAppThemeSelection) {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {

            composable(NavRoutes.GET_STARTED) {
                GetStartedScreen(
                    onGetStarted = {
                        navController.navigate(NavRoutes.LOGIN) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(NavRoutes.LOGIN) {
                LoginScreen(
                    navController = navController,
                    onLoginSuccess = {
                        val uid = Firebase.auth.currentUser?.uid ?: ""
                        SessionManager.saveUserID(userId = uid)
                        SessionManager.isFreshLogin = true
                        SessionManager.saveLoginState(true)
                        if (uid.isNotBlank()) onUserLoggedIn(uid)
                        navController.navigate(NavRoutes.ONBOARDING) {
                            popUpTo(NavRoutes.GET_STARTED) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToSignup = {
                        navController.navigate(NavRoutes.SIGNUP) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(NavRoutes.SIGNUP) {
                SignupScreen(
                    onNavigateToLogin = { navController.navigate(NavRoutes.LOGIN) },
                    onSignupSuccess = { uid -> onUserLoggedIn(uid) }
                )
            }

            composable(NavRoutes.DASHBOARD) {
                DashboardScreen()
            }

            composable(NavRoutes.ONBOARDING) {
                OnBoardingScreen(
                    onBoardingFinished = {
                        SessionManager.isFreshLogin = false
                        navController.navigate(NavRoutes.MAINSCREEN) {
                            popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(NavRoutes.MAINSCREEN) {
                MainScreen(
                    navController = navController,
                    onShareRequested = { userId -> onShareRequested(userId) },
                    onLogoutSuccess = {
                        navController.navigate(NavRoutes.GET_STARTED) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = NavRoutes.PROFILE,
                arguments = listOf(
                    navArgument("userId") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val userId by backStackEntry.savedStateHandle.getStateFlow("userId", "")
                    .collectAsState()
                EditProfileProfile(
                    userId = userId,
                    onBack = { navController.popBackStack() },
                    onShareRequested = { id -> onShareRequested(id) },
                    onPickImageRequested = onPickImageRequested,
                    onDecodeImageRequested = onDecodeImageRequested
                )
            }

            composable(NavRoutes.PROFILE_SETTING) {
                profileScreenKMP()
            }
        }
    }
}