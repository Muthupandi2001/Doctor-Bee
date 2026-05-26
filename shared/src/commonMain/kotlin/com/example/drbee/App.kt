package com.example.drbee

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.drbee.Authentication.DashboardScreen
import com.example.drbee.Authentication.GetStartedScreen
import com.example.drbee.Authentication.LoginScreen
import com.example.drbee.Authentication.SignupScreen
import com.example.drbee.Helper.SessionManager
import com.example.drbee.MainScreen.MainScreen
import com.example.drbee.OnBoarding.OnBoardingScreen
import com.example.drbee.ProfileScreen.OtherProfile
import com.example.drbee.ProfileScreen.ThemePreferencesManager
import com.example.drbee.ProfileScreen.ThemePreferencesManager.currentAppThemeSelection
import com.example.drbee.ProfileScreen.WonderBeeTheme
import com.example.drbee.ProfileScreen.profileScreenKMP
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import io.github.aakira.napier.Napier

object Routes {
    const val GET_STARTED      = "get_started"
    const val PROFILE_SETTING  = "profile_setting"
    const val LOGIN            = "login"
    const val SIGNUP           = "signup"
    const val DASHBOARD        = "dashboard"
    const val MAINSCREEN       = "mainscreen"
    const val ONBOARDING       = "onBoarding"
    const val PROFILE          = "profile/{userId}"

    fun profile(userId: String) = "profile/$userId"
}

data class DeepLinkParams(
    val screen: String? = null,
    val referrerId: String? = null
)

@Composable
fun App(
    deepLinkParams: DeepLinkParams?,
    onShareRequested: (String) -> Unit,
    onPickImageRequested: ((String) -> Unit) -> Unit,
    // Android side passes a lambda that decodes Base64 → ImageBitmap (no android.* in commonMain)
    onDecodeImageRequested: (String, (ImageBitmap?) -> Unit) -> Unit,
    onUserLoggedIn: (uid: String) -> Unit   // ✅ NEW

) {
    val navController = rememberNavController()
    val auth = remember { Firebase.auth }

    var initialDestination by remember { mutableStateOf<String?>(null) }
    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ThemePreferencesManager.loadThemeSettings { _, _, _, _ -> }

        auth.authStateChanged.collect { firebaseUser ->
            val sessionLoggedIn = SessionManager.isLoggedIn
            Napier.d("GitLive Auth State → uid=${firebaseUser?.uid}, sessionLoggedIn=$sessionLoggedIn")

            initialDestination = when {
                firebaseUser != null && sessionLoggedIn -> {
                    SessionManager.saveLoginState(true)
                    SessionManager.saveUserData(
                        userId = firebaseUser.uid,
                        email  = firebaseUser.email ?: "",
                        name   = SessionManager.savedUserName
                    )
                    if (SessionManager.isFreshLogin) Routes.ONBOARDING else Routes.MAINSCREEN
                }
                firebaseUser == null && sessionLoggedIn && !isInitialized -> Routes.MAINSCREEN
                else -> {
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
            navController.navigate(Routes.profile(referrerId)) { launchSingleTop = true }
        }
    }

    if (initialDestination == null) return

    WonderBeeTheme(themeType = currentAppThemeSelection) {
        NavHost(navController = navController, startDestination = initialDestination!!) {

            composable(Routes.GET_STARTED) {
                GetStartedScreen(onGetStarted = {
                    navController.navigate(Routes.LOGIN) { launchSingleTop = true }
                })
            }

//            composable(Routes.LOGIN) {
//                LoginScreen(
//                    navController = navController,
//                    onLoginSuccess = {
//                        val firebaseUser = Firebase.auth.currentUser
//                        SessionManager.saveUserID(userId = firebaseUser?.uid ?: "")
//                        navController.navigate(Routes.ONBOARDING) {
//                            popUpTo(Routes.GET_STARTED) { inclusive = true }
//                            launchSingleTop = true
//                        }
//                    },
//                    onNavigateToSignup = {
//                        navController.navigate(Routes.SIGNUP) { launchSingleTop = true }
//                    }
//                )
//            }
//
//            composable(Routes.SIGNUP) {
//                SignupScreen(onNavigateToLogin = { navController.navigate(Routes.LOGIN) })
//            }


            composable(Routes.LOGIN) {
                LoginScreen(
                    navController = navController,
                    onLoginSuccess = {
                        val uid = Firebase.auth.currentUser?.uid ?: ""
                        SessionManager.saveUserID(userId = uid)
                        SessionManager.isFreshLogin = true
                        SessionManager.saveLoginState(true)

                        // ✅ Save FCM token — crosses to Android side
                        if (uid.isNotBlank()) onUserLoggedIn(uid)

                        navController.navigate(Routes.ONBOARDING) {
                            popUpTo(Routes.GET_STARTED) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToSignup = {
                        navController.navigate(Routes.SIGNUP) { launchSingleTop = true }
                    }
                )
            }

            composable(Routes.SIGNUP) {
                SignupScreen(
                    onNavigateToLogin = {
                        navController.navigate(Routes.LOGIN)
                    },
                    // ✅ Called right after user is created in Firebase
                    onSignupSuccess = { uid ->
                        onUserLoggedIn(uid)
                    }
                )
            }

            composable(Routes.DASHBOARD) { DashboardScreen() }

            composable(Routes.ONBOARDING) {
                OnBoardingScreen(onBoardingFinished = {
                    SessionManager.isFreshLogin = false
                    navController.navigate(Routes.MAINSCREEN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                        launchSingleTop = true
                    }
                })
            }

            composable(Routes.MAINSCREEN) {
                MainScreen(
                    navController = navController,
                    onShareRequested = { userId -> onShareRequested(userId) },
                    onLogoutSuccess = {
                        navController.navigate(Routes.GET_STARTED) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // ✅ navArgument declared so userId is read from the path, not savedStateHandle
            composable(
                route = Routes.PROFILE,
                arguments = listOf(navArgument("userId") {
                    type = NavType.StringType; defaultValue = ""
                })
            ) { backStackEntry ->
                // ✅ savedStateHandle is the KMP-safe way to read typed nav args in commonMain.
                // backStackEntry.arguments is android.os.Bundle which has no getString in commonMain.
                val userId = backStackEntry.savedStateHandle.get<String>("userId") ?: ""
                OtherProfile(
                    userId = userId,
                    onBack = { navController.popBackStack() },
                    onShareRequested = { id -> onShareRequested(id) },
                    onPickImageRequested = onPickImageRequested,
                    onDecodeImageRequested = onDecodeImageRequested  // threaded through
                )
            }

            composable(Routes.PROFILE_SETTING) { profileScreenKMP() }
        }
    }
}