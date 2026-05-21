package com.example.drbee.Authentication.AuthRepository

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth

class AuthRepository {

    suspend fun signup(
        email: String,
        password: String
    ): Result<String> {
        return try {
            val user = Firebase.auth
                .createUserWithEmailAndPassword(email, password)
                .user

            Result.success(user?.uid ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(
        email: String,
        password: String
    ): Result<String> {
        return try {
            val user = Firebase.auth
                .signInWithEmailAndPassword(email, password)
                .user

            Result.success(user?.uid ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
