package com.example.drbee.ProfileScreen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.drbee.Helper.BeeAmber
import com.example.drbee.Helper.BeeBackground
import com.example.drbee.Helper.BeeBrown
import com.example.drbee.ProfileScreen.ProfileViewModel.ProfileUiState
import com.example.drbee.ProfileScreen.ProfileViewModel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherProfile(
    userId: String,
    onBack: () -> Unit,
    onShareRequested: (String) -> Unit,
    onPickImageRequested: ((String) -> Unit) -> Unit,
    // Android side supplies this lambda — decodes Base64 → ImageBitmap off the main thread
    onDecodeImageRequested: (String, (ImageBitmap?) -> Unit) -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isSaveSuccessful by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) viewModel.loadProfile(userId)
    }

    LaunchedEffect(uiState) {
        if (uiState is ProfileUiState.Loading) isSaveSuccessful = false
    }

    Scaffold(
        containerColor = BeeBackground,
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", color = BeeBrown, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = BeeAmber)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BeeBackground)
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BeeAmber)
                }
            }
            is ProfileUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${state.message}", color = Color.Red)
                }
            }
            is ProfileUiState.Success -> {
                BeeEditProfileContent(
                    user = state.user,
                    modifier = Modifier.padding(padding),
                    onSaveClick = { updatedUser ->
                        viewModel.updateProfile(updatedUser)
                        isSaveSuccessful = true
                    },
                    isSaveSuccessful = isSaveSuccessful,
                    onPickImageRequested = onPickImageRequested,
                    onDecodeImageRequested = onDecodeImageRequested  // passed straight through
                )
            }
        }
    }
}