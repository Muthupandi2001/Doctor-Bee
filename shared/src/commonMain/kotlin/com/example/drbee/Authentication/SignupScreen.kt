package com.example.drbee.Authentication

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drbee.Authentication.AuthRepository.AuthRepository
import com.example.drbee.ChatActivity.FirebaseChatModel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import dev.gitlive.firebase.database.database // Make sure to import this at the top

@Composable
fun SignupScreen(
    onNavigateToLogin: () -> Unit
) {
    val authRepository = remember { AuthRepository() }
    val scope = rememberCoroutineScope()

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    val BeeAmberGold = Color(0xFFFFB300) // Fallback local color token declaration

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "WonderBee", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = BeeAmberGold)
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name") },
            leadingIcon = { Icon(Icons.Default.Person, null) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, null) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            visualTransformation = PasswordVisualTransformation(),
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            visualTransformation = PasswordVisualTransformation(),
            label = { Text("Confirm Password") },
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(20.dp))


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
                                // This will now print "The email address is already in use by another account." directly onto your UI screen
                                message = error.message ?: "Signup Failed"
                                Napier.e("Auth Signup Failed: ${error.message}", throwable = error)
                            }

                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BeeAmberGold)
                ) {
                    Text("Create Account", color = Color.White)
                }


        Spacer(modifier = Modifier.height(20.dp))
        Text(message)
        Napier.d("message:::: $message")
    }
}
