package com.example.drbee.Authentication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drbee.Authentication.AuthRepository.AuthRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import dev.gitlive.firebase.database.database

@Composable
fun SignupScreen(
    onNavigateToLogin: () -> Unit
) {
    val authRepository = remember { AuthRepository() }
    val scope = rememberCoroutineScope()

    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") } // Added Phone State
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    // Theme Colors
    val BeeBackground = Color(0xFFFFFBF0) // Light Cream
    val BeeAmberGold = Color(0xFFFFB300)
    val BeeBrown = Color(0xFF3E2723)     // Dark text
    val BeeCard = Color(0xFFFFFFFF)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BeeBackground)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Main Card Container
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = BeeCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "WonderBee",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = BeeAmberGold
                )
                Text(
                    text = "Join the hive today",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Full Name Field
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name", color = BeeBrown) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BeeAmberGold) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = BeeAmberGold,
                        unfocusedIndicatorColor = Color.Gray.copy(alpha = 0.5f),
                        cursorColor = BeeAmberGold
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Phone Number Field (NEW)
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number", color = BeeBrown) },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = BeeAmberGold) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = BeeAmberGold,
                        unfocusedIndicatorColor = Color.Gray.copy(alpha = 0.5f),
                        cursorColor = BeeAmberGold
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address", color = BeeBrown) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = BeeAmberGold) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = BeeAmberGold,
                        unfocusedIndicatorColor = Color.Gray.copy(alpha = 0.5f),
                        cursorColor = BeeAmberGold
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    visualTransformation = PasswordVisualTransformation(),
                    label = { Text("Password", color = BeeBrown) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BeeAmberGold) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = BeeAmberGold,
                        unfocusedIndicatorColor = Color.Gray.copy(alpha = 0.5f),
                        cursorColor = BeeAmberGold
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Confirm Password Field
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    visualTransformation = PasswordVisualTransformation(),
                    label = { Text("Confirm Password", color = BeeBrown) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BeeAmberGold) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = BeeAmberGold,
                        unfocusedIndicatorColor = Color.Gray.copy(alpha = 0.5f),
                        cursorColor = BeeAmberGold
                    )
                )
                Spacer(modifier = Modifier.height(32.dp))

                // Create Account Button
                Button(
                    onClick = {
                        if (password != confirmPassword) {
                            message = "Password mismatch"
                            return@Button
                        }

                        scope.launch {
                            val result = authRepository.signup(email, password)

                            result.onSuccess {
                                try {
                                    val currentUser = Firebase.auth.currentUser
                                    val userId = currentUser?.uid

                                    if (userId != null) {
                                        // Clean data structure for instant serialization
                                        val newUserMap = mapOf(
                                            "id" to userId,
                                            "name" to fullName,
                                            "email" to email,
                                            "phoneNumber" to phoneNumber, // Added Phone to Database
                                            "message" to "Hey! I just joined WonderBee.",
                                            "time" to "Just now",
                                            "colorHex" to "#6C63FF"
                                        )

                                        Firebase.database
                                            .reference("users")
                                            .child(userId)
                                            .setValue(newUserMap)

                                        message = "Signup Success"
                                        onNavigateToLogin()
                                    } else {
                                        message = "User verification index missing"
                                    }
                                } catch (e: Exception) {
                                    message = "Database creation failed: ${e.message}"
                                    Napier.e("Database Error: ${e.message}", throwable = e)
                                }
                            }
                            result.onFailure { error ->
                                message = error.message ?: "Signup Failed"
                                Napier.e("Auth Signup Failed: ${error.message}", throwable = error)
                            }

                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BeeAmberGold)
                ) {
                    Text("Create Account", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status Message
                if (message.isNotEmpty()) {
                    Text(
                        text = message,
                        color = if (message == "Signup Success") Color(0xFF4CAF50) else Color.Red,
                        fontSize = 13.sp
                    )
                }

                Napier.d("message:::: $message")
            }
        }
    }
}
