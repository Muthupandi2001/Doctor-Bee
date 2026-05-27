package com.example.drbee.ProfileScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.drbee.Helper.BeeAmber
import com.example.drbee.Helper.BeeAmberLight
import com.example.drbee.Helper.BeeBackground
import com.example.drbee.Helper.BeeBrown
import com.example.drbee.Helper.BeeCard
import com.example.drbee.ProfileScreen.ProfileViewModel.ProfileUiState
import com.example.drbee.ProfileScreen.ProfileViewModel.ProfileViewModel
import com.example.drbee.ProfileScreen.ProfileViewModel.UserProfile
import com.example.drbee.decodeBase64ToImageBitmap      // ✅ expect fun
import com.example.drbee.rememberImagePickerLauncher   // ✅ expect fun

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileProfile(
    userId           : String,
    onBack           : () -> Unit,
//    onShareRequested : (String) -> Unit,
    // ✅ onPickImageRequested and onDecodeImageRequested REMOVED
    viewModel        : ProfileViewModel = viewModel()
) {
    val uiState          by viewModel.uiState.collectAsState()
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
                title = {
                    Text("Edit Profile", color = BeeBrown, fontWeight = FontWeight.SemiBold)
                },
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
                    user             = state.user,
                    modifier         = Modifier.padding(padding),
                    isSaveSuccessful = isSaveSuccessful,
                    onSaveClick      = { updatedUser ->
                        viewModel.updateProfile(updatedUser)
                        isSaveSuccessful = true
                    }
                    // ✅ No more lambda params passed down
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BeeEditProfileContent(
    user             : UserProfile,
    modifier         : Modifier = Modifier,
    isSaveSuccessful : Boolean,
    onSaveClick      : (UserProfile) -> Unit
    // ✅ onPickImageRequested and onDecodeImageRequested REMOVED
) {
    var nameState          by remember { mutableStateOf(user.name) }
    var emailState         by remember { mutableStateOf(user.email) }
    var phoneState         by remember { mutableStateOf(user.phone) }
    var websiteState       by remember { mutableStateOf(user.message) }
    var currentImageBase64 by remember { mutableStateOf(user.profileImage) }
    var profileBitmap      by remember { mutableStateOf<ImageBitmap?>(null) }

    // ✅ Decode directly via expect fun — no lambda from MainActivity
    LaunchedEffect(currentImageBase64) {
        profileBitmap = currentImageBase64
            .takeIf { it.isNotEmpty() }
            ?.let { decodeBase64ToImageBitmap(it) }
    }

    // ✅ Pick image via expect/actual — Camera + Gallery chooser on Android
    val imagePicker = rememberImagePickerLauncher { base64 ->
        if (base64.isNotEmpty()) currentImageBase64 = base64
    }

    Column(
        modifier            = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))

        // ── Avatar picker ────────────────────────────────────────────────────
        Box(
            modifier         = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .clickable { imagePicker.launch() },   // ✅ direct launch
            contentAlignment = Alignment.Center
        ) {
            if (profileBitmap != null) {
                Image(
                    bitmap             = profileBitmap!!,
                    contentDescription = "Profile Photo",
                    modifier           = Modifier.fillMaxSize(),
                    contentScale       = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BeeAmberLight)
                        .border(4.dp, BeeCard, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = nameState.take(2).uppercase(),
                        fontSize   = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color      = BeeAmber
                    )
                }
            }

            // Camera badge overlay
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(BeeAmber)
                    .border(2.dp, BeeBackground, CircleShape)
                    .align(Alignment.BottomEnd),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.CameraAlt,
                    contentDescription = "Change photo",
                    tint               = BeeCard,
                    modifier           = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        ProfileInputField(Icons.Default.Person,   "Full Name",   nameState)    { nameState    = it }
        Spacer(Modifier.height(16.dp))
        ProfileInputField(Icons.Default.Phone,    "Phone",       phoneState)   { phoneState   = it }
        Spacer(Modifier.height(16.dp))
        ProfileInputField(Icons.Default.Email,    "Email",       emailState)   { emailState   = it }
        Spacer(Modifier.height(16.dp))
        ProfileInputField(Icons.Default.Language, "Website/Bio", websiteState) { websiteState = it }
        Spacer(Modifier.height(32.dp))

        BeeSaveButton(isSaveSuccessful) {
            onSaveClick(
                user.copy(
                    name         = nameState,
                    email        = emailState,
                    phone        = phoneState,
                    message      = websiteState,
                    profileImage = currentImageBase64
                )
            )
        }

        Spacer(Modifier.height(20.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileInputField(
    icon          : ImageVector,
    label         : String,
    value         : String,
    onValueChange : (String) -> Unit
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        modifier      = Modifier.fillMaxWidth(),
        label         = { Text(label, color = BeeBrown.copy(alpha = 0.7f)) },
        leadingIcon   = { Icon(icon, contentDescription = null, tint = BeeAmber) },
        singleLine    = true,
        shape         = RoundedCornerShape(12.dp),
        colors        = TextFieldDefaults.colors(
            focusedContainerColor   = BeeCard,
            unfocusedContainerColor = BeeCard,
            disabledContainerColor  = BeeCard,
            focusedIndicatorColor   = BeeAmber,
            unfocusedIndicatorColor = BeeBrown.copy(alpha = 0.2f),
            focusedTextColor        = BeeBrown,
            unfocusedTextColor      = BeeBrown,
            cursorColor             = BeeAmber
        )
    )
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BeeSaveButton(isSaveSuccessful: Boolean, onClick: () -> Unit) {
    val buttonText   = if (isSaveSuccessful) "Information Updated" else "Update Changes"
    val buttonColor  = if (isSaveSuccessful) Color(0xFF4CAF50) else BeeAmber
    val contentColor = if (isSaveSuccessful) Color.White else BeeBrown

    Button(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape    = RoundedCornerShape(16.dp),
        colors   = ButtonDefaults.buttonColors(containerColor = buttonColor),
        enabled  = !isSaveSuccessful
    ) {
        if (isSaveSuccessful) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = contentColor)
            Spacer(Modifier.width(8.dp))
        }
        Text(buttonText, color = contentColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}