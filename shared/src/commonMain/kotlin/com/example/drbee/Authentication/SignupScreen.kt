package com.example.drbee.Authentication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drbee.Authentication.AuthRepository.AuthRepository
import com.example.drbee.ProfileScreen.WonderBeeTheme
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.database.database
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch

// ── Liquid Glass field shape ──────────────────────────────────────────────────
private val GlassShape = RoundedCornerShape(20.dp)

// ── Glass tint colors ─────────────────────────────────────────────────────────
private val GlassFill        = Color(0x1AFFFFFF)   // white 10%
private val GlassBorder      = Color(0x33FFFFFF)   // white 20%
private val GlassInnerShine  = Color(0x26FFFFFF)   // white 15% — top highlight line
@Composable
fun GlassField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    error: String?             = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean        = false
) {

    // ── Theme tokens ──────────────────────────────────────────────────────
    val gradientBrush = WonderBeeTheme.extendedDesign.primaryGradientBrush
    val accentColor   = WonderBeeTheme.extendedDesign.inputFocusedBorderColor
    val errorColor    = WonderBeeTheme.materialScheme.error
    val onBg          = WonderBeeTheme.materialScheme.onBackground

    val borderColor = when {
        error != null -> errorColor
        else          -> GlassBorder
    }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(GlassShape)
                // ── frosted glass fill ────────────────────────────────
                .background(GlassFill)
                // ── outer glass border ────────────────────────────────
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = GlassShape
                )
                // ── inner top-edge shine line (iOS liquid glass effect)
                .drawBehind {
                    drawLine(
                        color       = GlassInnerShine,
                        start       = Offset(24f, 2f),
                        end         = Offset(size.width - 24f, 2f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
        ) {
            TextField(
                value                = value,
                onValueChange        = onValueChange,
                placeholder          = {
                    Text(
                        text     = label,
                        color    = Color.White.copy(alpha = 0.4f),
                        fontSize = 15.sp
                    )
                },
                leadingIcon          = {
                    Icon(
                        imageVector        = icon,
                        contentDescription = null,
                        tint               = if (error != null)
                            errorColor
                        else
                            accentColor.copy(alpha = 0.85f),
                        modifier           = Modifier.size(22.dp)
                    )
                },
                singleLine           = true,
                visualTransformation = if (isPassword)
                    PasswordVisualTransformation()
                else
                    VisualTransformation.None,
                keyboardOptions      = KeyboardOptions(keyboardType = keyboardType),
                modifier             = Modifier.fillMaxWidth(),
                colors               = TextFieldDefaults.colors(
                    focusedTextColor          = Color.White,
                    unfocusedTextColor        = Color.White,
                    errorTextColor            = errorColor,
                    focusedContainerColor     = Color.Transparent,
                    unfocusedContainerColor   = Color.Transparent,
                    errorContainerColor       = Color.Transparent,
                    focusedIndicatorColor     = Color.Transparent,
                    unfocusedIndicatorColor   = Color.Transparent,
                    errorIndicatorColor       = Color.Transparent,
                    cursorColor               = accentColor
                )
            )
        }
        // inline error
        error?.let {
            Text(
                text     = it,
                color    = errorColor,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}
@Composable
fun SignupScreen(
    onNavigateToLogin: () -> Unit,
    onSignupSuccess: (uid: String) -> Unit
) {


    // ── State ─────────────────────────────────────────────────────────────
    val authRepository = remember { AuthRepository() }
    val scope          = rememberCoroutineScope()

    var fullName        by remember { mutableStateOf("") }
    var phoneNumber     by remember { mutableStateOf("") }
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var message         by remember { mutableStateOf("") }
    var termsAccepted   by remember { mutableStateOf(false) }

    var fullNameError        by remember { mutableStateOf<String?>(null) }
    var phoneError           by remember { mutableStateOf<String?>(null) }
    var emailError           by remember { mutableStateOf<String?>(null) }
    var passwordError        by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    // ── Theme tokens ──────────────────────────────────────────────────────
    val gradientBrush = WonderBeeTheme.extendedDesign.primaryGradientBrush
    val accentColor   = WonderBeeTheme.extendedDesign.inputFocusedBorderColor
    val errorColor    = WonderBeeTheme.materialScheme.error
    val onBg          = WonderBeeTheme.materialScheme.onBackground

    // ── Liquid glass field composable ─────────────────────────────────────


    // ── Animated liquid background ────────────────────────────────────────
    // Three soft radial orbs — simulates iOS depth-blur background
    val bgBrush = Brush.radialGradient(
        colors      = listOf(
            Color(0xFF1A0533),   // deep purple
            Color(0xFF0D1F4A),   // dark navy
            Color(0xFF0A2A1A)    // dark teal
        ),
        center      = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
        radius      = 1200f,
        tileMode    = TileMode.Clamp
    )

    // ── Root ──────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                // deep mesh background
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1A0533),
                        Color(0xFF0D1F4A),
                        Color(0xFF0A2A1A)
                    ),
                    start = Offset(0f, 0f),
                    end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
    ) {
        // ── Orb 1 — amber top-right ───────────────────────────────────────
        Box(
            modifier = Modifier
                .size(260.dp)
                .offset(x = 160.dp, y = (-80).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x59FFA000),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(50)
                )
        )
        // ── Orb 2 — purple bottom-left ────────────────────────────────────
        Box(
            modifier = Modifier
                .size(240.dp)
                .offset(x = (-80).dp, y = 480.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x40B400FF),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(50)
                )
        )
        // ── Orb 3 — teal mid-right ────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = 220.dp, y = 280.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x3300C8B4),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(50)
                )
        )

        // ── Scrollable content ────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(72.dp))

            // ── Header ────────────────────────────────────────────────────
            Text(
                text  = "Hey there,",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
            Text(
                text       = "Create an Account",
                fontSize   = 26.sp,
                color      = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(32.dp))

            // ── Fields ────────────────────────────────────────────────────
            GlassField(
                value         = fullName,
                onValueChange = { fullName = it; fullNameError = null },
                label         = "Full Name",
                icon          = Icons.Default.Person,
                error         = fullNameError
            )
            Spacer(Modifier.height(12.dp))

            GlassField(
                value         = phoneNumber,
                onValueChange = { phoneNumber = it; phoneError = null },
                label         = "Phone Number",
                icon          = Icons.Default.Phone,
                error         = phoneError,
                keyboardType  = KeyboardType.Phone
            )
            Spacer(Modifier.height(12.dp))

            GlassField(
                value         = email,
                onValueChange = { email = it; emailError = null },
                label         = "Email Address",
                icon          = Icons.Default.Email,
                error         = emailError,
                keyboardType  = KeyboardType.Email
            )
            Spacer(Modifier.height(12.dp))

            GlassField(
                value         = password,
                onValueChange = { password = it; passwordError = null },
                label         = "Password",
                icon          = Icons.Default.Lock,
                error         = passwordError,
                isPassword    = true
            )
            Spacer(Modifier.height(12.dp))

            GlassField(
                value         = confirmPassword,
                onValueChange = { confirmPassword = it; confirmPasswordError = null },
                label         = "Confirm Password",
                icon          = Icons.Default.Lock,
                error         = confirmPasswordError,
                isPassword    = true
            )

            Spacer(Modifier.height(16.dp))

            // ── Terms checkbox ────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.fillMaxWidth()
            ) {
                // glass checkbox
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(GlassFill)
                        .border(1.dp, GlassBorder, RoundedCornerShape(7.dp))
                        .clickable { termsAccepted = !termsAccepted },
                    contentAlignment = Alignment.Center
                ) {
                    if (termsAccepted) {
                        Text("✓", color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(
                            color    = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )) { append("By continuing you accept our ") }
                        withStyle(SpanStyle(
                            color          = Color.White.copy(alpha = 0.7f),
                            fontSize       = 12.sp,
                            textDecoration = TextDecoration.Underline
                        )) { append("Privacy Policy") }
                        withStyle(SpanStyle(
                            color    = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )) { append(" and ") }
                        withStyle(SpanStyle(
                            color          = Color.White.copy(alpha = 0.7f),
                            fontSize       = 12.sp,
                            textDecoration = TextDecoration.Underline
                        )) { append("Terms of Use") }
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Message ───────────────────────────────────────────────────
            if (message.isNotEmpty()) {
                Text(
                    text     = message,
                    color    = if (message == "Signup Success") Color(0xFF4CAF50) else errorColor,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Gradient Register button ──────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(gradientBrush)             // ← primaryGradientBrush
                    .clickable {
                        var valid = true
                        if (fullName.isBlank())      { fullNameError = "Name is required";          valid = false }
                        if (phoneNumber.isBlank())   { phoneError    = "Phone is required";         valid = false }
                        if (email.isBlank())         { emailError    = "Email is required";         valid = false }
                        if (password.isBlank())      { passwordError = "Password is required";      valid = false }
                        if (password != confirmPassword) { confirmPasswordError = "Passwords do not match"; valid = false }
                        if (!valid) return@clickable

                        scope.launch {
                            val result = authRepository.signup(email, password)
                            result.onSuccess {
                                try {
                                    val currentUser = Firebase.auth.currentUser
                                    val userId      = currentUser?.uid
                                    if (userId != null) {
                                        val newUserMap = mapOf(
                                            "id"          to userId,
                                            "name"        to fullName,
                                            "email"       to email,
                                            "phoneNumber" to phoneNumber,
                                            "message"     to "Hey! I just joined WonderBee.",
                                            "time"        to "Just now",
                                            "colorHex"    to "#6C63FF"
                                        )
                                        Firebase.database
                                            .reference("users")
                                            .child(userId)
                                            .setValue(newUserMap)
                                        onSignupSuccess(userId)
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
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = "Register",
                    color      = Color.White,
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(28.dp))

            // ── Or divider ────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(
                    modifier  = Modifier.weight(1f),
                    color     = Color.White.copy(alpha = 0.15f),
                    thickness = 1.dp
                )
                Text(
                    "  Or  ",
                    color    = Color.White.copy(alpha = 0.35f),
                    fontSize = 13.sp
                )
                HorizontalDivider(
                    modifier  = Modifier.weight(1f),
                    color     = Color.White.copy(alpha = 0.15f),
                    thickness = 1.dp
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Social glass buttons ──────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier              = Modifier.fillMaxWidth()
            ) {
                // Google
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(GlassFill)
                        .border(1.dp, GlassBorder, RoundedCornerShape(17.dp))
                        .drawBehind {
                            drawLine(
                                color       = GlassInnerShine,
                                start       = Offset(8f, 2f),
                                end         = Offset(size.width - 8f, 2f),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "G",
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color(0xFF4285F4)
                    )
                }

                Spacer(Modifier.width(20.dp))

                // Facebook
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(GlassFill)
                        .border(1.dp, GlassBorder, RoundedCornerShape(17.dp))
                        .drawBehind {
                            drawLine(
                                color       = GlassInnerShine,
                                start       = Offset(8f, 2f),
                                end         = Offset(size.width - 8f, 2f),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "f",
                        fontSize   = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color(0xFF1877F2)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Already have account ──────────────────────────────────────
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(
                        color    = Color.White.copy(alpha = 0.55f),
                        fontSize = 14.sp
                    )) { append("Already have an account? ") }
                    withStyle(SpanStyle(
                        color      = accentColor,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )) { append("Login") }
                },
                modifier = Modifier.clickable { onNavigateToLogin() }
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}