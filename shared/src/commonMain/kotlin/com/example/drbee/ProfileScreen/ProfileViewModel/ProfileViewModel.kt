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
    val message: String = "",
    val time: String = "",
    val colorHex: String = "#FFB300"
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

                val snapshot = Firebase.database
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
                                colorHex= map["colorHex"]?.toString() ?: "#FFB300"
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
}