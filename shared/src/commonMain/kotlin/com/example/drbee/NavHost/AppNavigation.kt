package com.example.drbee.NavHost

import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.drbee.Authentication.DashboardScreen
import com.example.drbee.Authentication.GetStartedScreen
import com.example.drbee.Authentication.LoginScreen
import com.example.drbee.Authentication.SignupScreen
import com.example.drbee.Helper.SessionManager
import com.example.drbee.MainScreen.MainScreen
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
    navController             : NavHostController,
    startDestination          : String,
    pendingReferrerId         : String?,           // ← replaces pendingProfileNavigation
    onPendingReferrerConsumed : () -> Unit,        // ← replaces onPendingProfileConsumed
    onUserLoggedIn            : (uid: String) -> Unit
) {
    WonderBeeTheme(themeType = currentAppThemeSelection) {
        NavHost(
            navController    = navController,
            startDestination = startDestination
        ) {

            composable(NavRoutes.GET_STARTED) {
                GetStartedScreen(
                    onGetStarted = {
                        navController.navigate(NavRoutes.LOGIN) { launchSingleTop = true }
                    }
                )
            }

            composable(NavRoutes.LOGIN) {
                LoginScreen(
                    navController      = navController,
                    onLoginSuccess     = {
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
                        navController.navigate(NavRoutes.SIGNUP) { launchSingleTop = true }
                    }
                )
            }

            composable(NavRoutes.SIGNUP) {
                SignupScreen(
                    onNavigateToLogin = { navController.navigate(NavRoutes.LOGIN) },
                    onSignupSuccess   = { uid -> onUserLoggedIn(uid) }
                )
            }

            composable(NavRoutes.DASHBOARD) { DashboardScreen() }

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

            // ── MainScreen now receives pendingReferrerId to show the popup ──
            composable(NavRoutes.MAINSCREEN) {
                MainScreen(
                    navController             = navController,
                    pendingReferrerId         = pendingReferrerId,
                    onPendingReferrerConsumed = onPendingReferrerConsumed,
                    onLogoutSuccess           = {
                        navController.navigate(NavRoutes.GET_STARTED) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route     = NavRoutes.EDIT_PROFILE,
                arguments = listOf(navArgument("userId") {
                    type         = NavType.StringType
                    defaultValue = ""
                })
            ) { backStackEntry ->
                val userId by backStackEntry.savedStateHandle
                    .getStateFlow("userId", "")
                    .collectAsState()
                EditProfileProfile(
                    userId = userId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.PROFILE_SETTING) { profileScreenKMP() }
        }
    }
}