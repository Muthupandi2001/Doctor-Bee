package com.example.drbee.Authentication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CircleNotifications
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.drbee.Authentication.AuthRepository.AuthRepository
import com.example.drbee.Helper.BeeAmberGold
import com.example.drbee.Helper.BeeBrightYellow
import com.example.drbee.Helper.BeeDarkNavy
import com.example.drbee.Helper.BeeMutedGold
import com.example.drbee.Helper.SessionManager
import kotlinx.coroutines.launch
import theme.AppColors.GradientEnd
import theme.AppColors.GradientStart

@Composable
fun LoginScreen(
    navController: NavController,
    onNavigateToSignup: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authRepository = remember { AuthRepository() }
    val scope = rememberCoroutineScope()

    // 🚨 NEW VALIDATION STATES
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(navController) {
        onDispose { }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(BeeBrightYellow, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CircleNotifications,
                contentDescription = "Bee Logo",
                modifier = Modifier.size(60.dp),
                tint = BeeDarkNavy
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "WonderBee",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            style = TextStyle(
                brush = Brush.linearGradient(
                    colors = listOf(GradientStart, GradientEnd)
                )
            )
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 🐝 HONEYCOMB EMAIL INPUT FIELD
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                emailError = null // Clear red border when user types
            },
            label = { Text("Honeycomb Email", color = if (emailError != null) Color.Red else BeeAmberGold) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = if (emailError != null) Color.Red else BeeAmberGold
                )
            },
            isError = emailError != null, // ✅ Highlights field red natively
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BeeAmberGold,
                unfocusedBorderColor = Color.Gray,
                errorBorderColor = Color.Red
            )
        )
        // ✅ Dynamic Email Error Text Message
        emailError?.let {
            Text(
                text = it,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 🐝 HIVE PASSWORD INPUT FIELD
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                passwordError = null // Clear red border when user types
            },
            label = { Text("Hive Password", color = if (passwordError != null) Color.Red else BeeAmberGold) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (passwordError != null) Color.Red else BeeAmberGold
                )
            },
            visualTransformation = PasswordVisualTransformation(),
            isError = passwordError != null, // ✅ Highlights field red natively
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BeeAmberGold,
                unfocusedBorderColor = Color.Gray,
                errorBorderColor = Color.Red
            )
        )
        // ✅ Dynamic Password Error Text Message
        passwordError?.let {
            Text(
                text = it,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                // Clear previous states before starting execution
                emailError = null
                passwordError = null

                // 1. Client-side blank field checking strings
                if (email.isBlank()) {
                    emailError = "Email field cannot be empty."
                    return@Button
                }
                if (password.isBlank()) {
                    passwordError = "Password field cannot be empty."
                    return@Button
                }

                scope.launch {
                    val result = authRepository.login(email, password)
                    result.onSuccess {
                        SessionManager.isFreshLogin = true
                        SessionManager.saveLoginState(true)
                        onLoginSuccess()
                    }
                    result.onFailure { exception ->
                        val errorMessage = exception.message ?: "Authentication failed"

                        // 2. Server-side error parsing logic
                        if (errorMessage.contains("user", ignoreCase = true) || errorMessage.contains("email", ignoreCase = true)) {
                            emailError = "No account found with this email."
                        } else if (errorMessage.contains("password", ignoreCase = true)) {
                            passwordError = "Incorrect password. Please try again."
                        } else {
                            // Fallback catch-all error directly attached to the forms
                            emailError = "Invalid credentials."
                            passwordError = "Invalid credentials."
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(GradientStart, GradientEnd)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            )
        ) {
            Text(
                text = "Welcome Back!",
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "← Go Back to Start",
            color = Color.Gray,
            modifier = Modifier
                .padding(8.dp)
                .clickable { navController.popBackStack() }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Forgot Password?", color = BeeDarkNavy, modifier = Modifier.clickable { })

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "🐝 Sign Up Now 🐝",
            fontWeight = FontWeight.Bold,
            color = BeeAmberGold,
            modifier = Modifier.clickable { onNavigateToSignup() }
        )
    }
}
