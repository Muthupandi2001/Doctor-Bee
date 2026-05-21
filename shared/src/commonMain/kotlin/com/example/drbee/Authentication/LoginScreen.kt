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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drbee.Authentication.AuthRepository.AuthRepository
import com.example.drbee.Helper.BeeAmberGold
import com.example.drbee.Helper.BeeBrightYellow
import com.example.drbee.Helper.BeeDarkNavy
import com.example.drbee.Helper.BeeMutedGold
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onNavigateToSignup: () -> Unit, onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authRepository = remember {
        AuthRepository()
    }


    val scope = rememberCoroutineScope()

    var message by remember {
        mutableStateOf("")
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
            Icon(imageVector = Icons.Default.CircleNotifications, contentDescription = "Bee Logo", modifier = Modifier.size(60.dp), tint = BeeDarkNavy)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "WonderBee", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = BeeAmberGold)

        Spacer(modifier = Modifier.height(40.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Honeycomb Email", color = BeeAmberGold) },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = BeeAmberGold) },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BeeAmberGold, unfocusedBorderColor = BeeMutedGold)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Hive Password", color = BeeAmberGold) },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BeeAmberGold) },
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BeeAmberGold, unfocusedBorderColor = BeeMutedGold)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {

                scope.launch {

                    val result =
                        authRepository.login(
                            email,
                            password
                        )

                    result.onSuccess {

                        message = "Login Success"

                        onLoginSuccess()
                    }

                    result.onFailure {

                        message =
                            it.message ?: "Login Failed"
                    }
                }
            },

            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),

            shape = RoundedCornerShape(25.dp),

            colors = ButtonDefaults.buttonColors(
                containerColor = BeeAmberGold
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