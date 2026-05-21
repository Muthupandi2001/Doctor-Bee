package com.example.drbee

import androidx.compose.material3.MaterialTheme
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

object Routes {
    const val GET_STARTED = "get_started"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val DASHBOARD = "dashboard"
    const val MAINSCREEN = "mainscreen"

    const val ONBOARDING = "onBoarding"
}

@Composable
@Preview
fun App() {
    val navController = rememberNavController()

    MaterialTheme {

        NavHost(
            navController = navController,
            startDestination = Routes.GET_STARTED
        ) {

            composable(Routes.GET_STARTED) {
                GetStartedScreen(
                    onGetStarted = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.GET_STARTED) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Routes.ONBOARDING) {
//                            popUpTo(Routes.MAINSCREEN) {
//                                inclusive = true }
                        }
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
                    },

                    )
            }

            composable(Routes.DASHBOARD) {
                DashboardScreen(
//                    onLogout = {
//                        navController.navigate(Routes.LOGIN) {
//                            popUpTo(0) // clear stack
//                        }
//                    }
                )
            }


            composable(Routes.MAINSCREEN) {
                MainScreen(
//                    onLogout = {
//                        navController.navigate(Routes.LOGIN) {
//                            popUpTo(0) // clear stack
//                        }
//                    }
                )
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