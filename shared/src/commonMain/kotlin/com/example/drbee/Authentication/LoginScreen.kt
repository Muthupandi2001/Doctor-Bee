package com.example.drbee.Authentication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.drbee.Authentication.AuthRepository.AuthRepository
import com.example.drbee.Helper.SessionManager
import com.example.drbee.ProfileScreen.WonderBeeTheme
import kotlinx.coroutines.launch

// ── Glass constants ───────────────────────────────────────────────────────────
private val GlassShape       = RoundedCornerShape(20.dp)
private val GlassFill        = Color(0x1AFFFFFF)
private val GlassBorder      = Color(0x33FFFFFF)
private val GlassInnerShine  = Color(0x26FFFFFF)

@Composable
fun LoginScreen(
    navController: NavController,
    onNavigateToSignup: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    // ── Theme tokens ──────────────────────────────────────────────────────
    val gradientBrush = WonderBeeTheme.extendedDesign.primaryGradientBrush
    val accentColor   = WonderBeeTheme.extendedDesign.inputFocusedBorderColor
    val errorColor    = WonderBeeTheme.materialScheme.error

    // ── State — identical to original ─────────────────────────────────────
    var email             by remember { mutableStateOf("") }
    var password          by remember { mutableStateOf("") }
    val authRepository    = remember { AuthRepository() }
    val scope             = rememberCoroutineScope()
    var emailError        by remember { mutableStateOf<String?>(null) }
    var passwordError     by remember { mutableStateOf<String?>(null) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    DisposableEffect(navController) { onDispose { } }

    // ── Glass field helper ────────────────────────────────────────────────
    @Composable
    fun GlassField(
        value: String,
        onValueChange: (String) -> Unit,
        label: String,
        leadingIcon: @Composable () -> Unit,
        error: String?                  = null,
        trailingIcon: @Composable (() -> Unit)? = null,
        visualTransformation: VisualTransformation = VisualTransformation.None
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(GlassShape)
                    .background(if (error != null) Color(0x26FF0000) else GlassFill)
                    .border(
                        width = 1.dp,
                        color = if (error != null) errorColor.copy(alpha = 0.6f) else GlassBorder,
                        shape = GlassShape
                    )
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
                    leadingIcon          = leadingIcon,
                    trailingIcon         = trailingIcon,
                    singleLine           = true,
                    visualTransformation = visualTransformation,
                    modifier             = Modifier.fillMaxWidth(),
                    colors               = TextFieldDefaults.colors(
                        focusedTextColor        = Color.White,
                        unfocusedTextColor      = Color.White,
                        errorTextColor          = errorColor,
                        focusedContainerColor   = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        errorContainerColor     = Color.Transparent,
                        focusedIndicatorColor   = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        errorIndicatorColor     = Color.Transparent,
                        cursorColor             = accentColor
                    )
                )
            }
            error?.let {
                Text(
                    text     = it,
                    color    = errorColor,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 4.dp)
                )
            }
        }
    }

    // ── Root ──────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
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
                        colors = listOf(Color(0x59FFA000), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(50)
                )
        )
        // ── Orb 2 — purple bottom-left ────────────────────────────────────
        Box(
            modifier = Modifier
                .size(240.dp)
                .offset(x = (-80).dp, y = 440.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x40B400FF), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(50)
                )
        )
        // ── Orb 3 — teal mid-right ────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(180.dp)
                .offset(x = 220.dp, y = 280.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x3300C8B4), Color.Transparent)
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Spacer(Modifier.height(80.dp))

            // ── Glass logo orb ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(50))
                    .background(GlassFill)
                    .border(1.dp, GlassBorder, RoundedCornerShape(50))
                    .drawBehind {
                        drawCircle(
                            color  = GlassInnerShine,
                            radius = size.minDimension / 2f - 2f,
                            style  = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 1.dp.toPx()
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("🐝", fontSize = 42.sp)
            }

            Spacer(Modifier.height(20.dp))

            // ── Title ─────────────────────────────────────────────────────
            Text(
                text     = "Welcome Back",
                fontSize = 14.sp,
                color    = Color.White.copy(alpha = 0.55f)
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(
                        brush      = gradientBrush,
                        fontSize   = 30.sp,
                        fontWeight = FontWeight.Bold
                    )) { append("Bee Login") }
                }
            )

            Spacer(Modifier.height(40.dp))

            // ── Email field ───────────────────────────────────────────────
            GlassField(
                value         = email,
                onValueChange = { email = it; emailError = null },
                label         = "Honeycomb Email",
                error         = emailError,
                leadingIcon   = {
                    Icon(
                        imageVector        = Icons.Default.Email,
                        contentDescription = null,
                        tint               = if (emailError != null) errorColor
                        else accentColor.copy(alpha = 0.85f),
                        modifier           = Modifier.size(22.dp)
                    )
                }
            )

            Spacer(Modifier.height(14.dp))

            // ── Password field ────────────────────────────────────────────
            GlassField(
                value                = password,
                onValueChange        = { password = it; passwordError = null },
                label                = "Hive Password",
                error                = passwordError,
                visualTransformation = if (isPasswordVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                leadingIcon          = {
                    Icon(
                        imageVector        = Icons.Default.Lock,
                        contentDescription = null,
                        tint               = if (passwordError != null) errorColor
                        else accentColor.copy(alpha = 0.85f),
                        modifier           = Modifier.size(22.dp)
                    )
                },
                trailingIcon         = {
                    IconButton(
                        onClick = { isPasswordVisible = !isPasswordVisible }
                    ) {
                        Icon(
                            imageVector        = if (isPasswordVisible)
                                Icons.Default.Visibility
                            else
                                Icons.Default.VisibilityOff,
                            contentDescription = if (isPasswordVisible)
                                "Hide password"
                            else
                                "Show password",
                            tint               = if (passwordError != null) errorColor
                            else Color.White.copy(alpha = 0.45f),
                            modifier           = Modifier.size(20.dp)
                        )
                    }
                }
            )

            Spacer(Modifier.height(12.dp))

            // ── Forgot password ───────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Text(
                    text     = "Forgot Password?",
                    color    = accentColor.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { }   // ← original empty clickable
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── Gradient login button ─────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(gradientBrush)
                    .clickable {
                        // ── exact original logic ──────────────────────────
                        emailError    = null
                        passwordError = null

                        if (email.isBlank()) {
                            emailError = "Email field cannot be empty."
                            return@clickable
                        }
                        if (password.isBlank()) {
                            passwordError = "Password field cannot be empty."
                            return@clickable
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
                                when {
                                    errorMessage.contains("user", ignoreCase = true) ||
                                            errorMessage.contains("email", ignoreCase = true) -> {
                                        emailError = "No account found with this email."
                                    }
                                    errorMessage.contains("password", ignoreCase = true) -> {
                                        passwordError = "Incorrect password. Please try again."
                                    }
                                    else -> {
                                        emailError    = "Invalid credentials."
                                        passwordError = "Invalid credentials."
                                    }
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = "Welcome Back!",
                    fontSize   = 18.sp,
                    color      = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Go back ───────────────────────────────────────────────────
            Text(
                text     = "← Go Back to Start",
                color    = Color.White.copy(alpha = 0.4f),
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(8.dp)
                    .clickable { navController.popBackStack() }  // ← original
            )

            Spacer(Modifier.height(8.dp))

            // ── Or divider ────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
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

            Spacer(Modifier.height(16.dp))

            // ── Sign up CTA — original text kept ─────────────────────────
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(
                        color    = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )) { append("Don't have an account? ") }
                    withStyle(SpanStyle(
                        color      = accentColor,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold
                    )) { append("🐝 Sign Up Now 🐝") }  // ← original text
                },
                modifier = Modifier.clickable { onNavigateToSignup() }
            )

            Spacer(Modifier.height(48.dp))
        }
    }
}