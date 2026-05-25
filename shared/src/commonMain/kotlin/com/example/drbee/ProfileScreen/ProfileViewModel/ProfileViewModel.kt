package com.example.drbee.ProfileScreen.ProfileViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.database
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class UserProfile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val message: String = "", // Used for Website/Bio in UI
    val time: String = "",
    val colorHex: String = "#FFB300",
    val phone: String = "",
    val profileImage: String = "" // ADDED: To store Base64 image string
)

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val user: UserProfile) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState

    fun loadProfile(userId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = ProfileUiState.Loading

                Firebase.database
                    .reference("users")
                    .child(userId)
                    .valueEvents
                    .collect { snap ->
                        val map = snap.value as? Map<*, *>
                        if (map != null) {
                            val user = UserProfile(
                                id      = map["id"]?.toString()       ?: userId,
                                name    = map["name"]?.toString()     ?: "Unknown",
                                email   = map["email"]?.toString()    ?: "",
                                message = map["message"]?.toString()  ?: "",
                                time    = map["time"]?.toString()     ?: "",
                                colorHex= map["colorHex"]?.toString() ?: "#FFB300",
                                // Handle both 'phone' and 'phoneNumber' keys
                                phone   = map["phone"]?.toString() ?: map["phoneNumber"]?.toString() ?: "",
                                // ADDED: Load the profile image from Firebase
                                profileImage = map["profileImage"]?.toString() ?: ""
                            )
                            _uiState.value = ProfileUiState.Success(user)
                        } else {
                            _uiState.value = ProfileUiState.Error("User not found")
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Failed to load profile")
            }
        }
    }

    // ── Function to save updates to Firebase ─────────────────────────────
    fun updateProfile(user: UserProfile) {
        viewModelScope.launch {
            try {
                // Create a map to ensure we update the correct fields in Firebase
                val userMap = mapOf(
                    "id" to user.id,
                    "name" to user.name,
                    "email" to user.email,
                    "message" to user.message,
                    "time" to user.time,
                    "colorHex" to user.colorHex,
                    "phone" to user.phone,
                    "phoneNumber" to user.phone,
                    // ADDED: Save the profile image to Firebase
                    "profileImage" to user.profileImage
                )

                Firebase.database
                    .reference("users")
                    .child(user.id)
                    .setValue(userMap)

                // Optimistically update the local state so UI reflects changes immediately
                _uiState.value = ProfileUiState.Success(user)

            } catch (e: Exception) {
                println("Error updating profile: ${e.message}")
            }
        }
    }
}