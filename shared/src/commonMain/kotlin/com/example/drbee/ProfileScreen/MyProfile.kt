package com.example.drbee.ProfileScreen

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.drbee.ProfileScreen.ProfileViewModel.ProfileUiState
import com.example.drbee.ProfileScreen.ProfileViewModel.ProfileViewModel
import com.example.drbee.ProfileScreen.ProfileViewModel.UserProfile

// ── Bee palette ──────────────────────────────────────────────────────────────
private val BeeAmber       = Color(0xFFFFB300)
private val BeeAmberDark   = Color(0xFF7B5700)
private val BeeAmberLight  = Color(0xFFFAEEDA)
private val BeeBrown       = Color(0xFF5C3A00)
private val BeeBackground  = Color(0xFF1A1206)
private val BeeSurface     = Color(0xFF241A08)
private val BeeCard        = Color(0xFFFFFDF7)
private val BeeCardBorder  = Color(0xFFEF9F27)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onShareRequested: (String) -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Load profile when userId arrives
    LaunchedEffect(userId) {
        if (userId.isNotBlank()) viewModel.loadProfile(userId)
    }

    Scaffold(
        containerColor = BeeBackground,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = BeeAmber
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is ProfileUiState.Loading -> BeeLoadingScreen()
            is ProfileUiState.Error   -> BeeErrorScreen(state.message)
            is ProfileUiState.Success -> BeeProfileContent(
                user            = state.user,
                onShareRequested= onShareRequested,
                modifier        = Modifier.padding(padding)
            )
        }
    }
}

// ── Loading ───────────────────────────────────────────────────────────────────
@Composable
private fun BeeLoadingScreen() {
    val infiniteAnim = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteAnim.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(800, easing = EaseInOut), RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("🐝", fontSize = 48.sp, modifier = Modifier.graphicsLayer { this.alpha = alpha })
            Text("Loading hive...", color = BeeAmber, fontSize = 14.sp)
        }
    }
}

// ── Error ─────────────────────────────────────────────────────────────────────
@Composable
private fun BeeErrorScreen(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🍯", fontSize = 40.sp)
            Text("Oops, no honey here", color = BeeAmber, fontWeight = FontWeight.Medium)
            Text(message, color = Color(0xFF888780), fontSize = 13.sp)
        }
    }
}

// ── Main content ──────────────────────────────────────────────────────────────
@Composable
private fun BeeProfileContent(
    user: UserProfile,
    onShareRequested: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header honeycomb banner ─────────────────────────────────────────
        BeeProfileHeader(user)

        // ── Body ────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .offset(y = (-24).dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Referral ID card
            ReferralIdCard(userId = user.id)

            // Stats row
            BeeStatsRow()

            // Profile info
            SectionLabel("Profile info")
            ProfileInfoCard(user = user)

            // Share button
            BeeShareButton(onClick = { onShareRequested(user.id) })

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────
@Composable
private fun BeeProfileHeader(user: UserProfile) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(BeeAmber)
            .padding(top = 8.dp, bottom = 52.dp),
        contentAlignment = Alignment.Center
    ) {
        // Honeycomb hex decoration top-right
        HexDecoration(
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Avatar
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(BeeAmberDark)
                        .border(3.dp, BeeCard, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.name.take(2).uppercase(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Medium,
                        color = BeeCard
                    )
                }
                // Online bee dot
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(BeeBackground)
                        .border(2.dp, BeeAmber, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🐝", fontSize = 9.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = user.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = BeeBackground
            )
            Text(
                text = user.email,
                fontSize = 13.sp,
                color = BeeBrown
            )
        }
    }
}

// ── Hex decoration ────────────────────────────────────────────────────────────
@Composable
private fun HexDecoration(modifier: Modifier = Modifier) {
    // Simple stacked hex shapes for visual flair
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(2) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(HexagonShape)
                    .background(BeeBrown.copy(alpha = 0.35f))
            )
        }
    }
}

private val HexagonShape = object : Shape {
    override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: LayoutDirection, density: Density): androidx.compose.ui.graphics.Outline {
        val path = Path().apply {
            val w = size.width; val h = size.height
            moveTo(w * 0.5f, 0f)
            lineTo(w, h * 0.25f)
            lineTo(w, h * 0.75f)
            lineTo(w * 0.5f, h)
            lineTo(0f, h * 0.75f)
            lineTo(0f, h * 0.25f)
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

// ── Referral ID card ──────────────────────────────────────────────────────────
@Composable
private fun ReferralIdCard(userId: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BeeCard),
        border = BorderStroke(0.5.dp, BeeCardBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(BeeAmberLight),
                contentAlignment = Alignment.Center
            ) {
                Text("🔑", fontSize = 16.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Referral ID",
                    fontSize = 10.sp,
                    color = Color(0xFF854F0B),
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = userId,
                    fontSize = 12.sp,
                    color = Color(0xFF412402),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Copy icon
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Copy ID",
                tint = BeeAmber,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ── Stats row ─────────────────────────────────────────────────────────────────
@Composable
private fun BeeStatsRow() {
    // Static placeholders — replace with real Firebase data as needed
    val stats = listOf("24" to "Referrals", "142" to "Honey pts", "6" to "Days active")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        stats.forEach { (value, label) ->
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BeeCard),
                border = BorderStroke(0.5.dp, BeeCardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(value, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = BeeAmber)
                    Spacer(Modifier.height(2.dp))
                    Text(label, fontSize = 10.sp, color = Color(0xFF854F0B))
                }
            }
        }
    }
}

// ── Section label ─────────────────────────────────────────────────────────────
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        color = Color(0xFFEF9F27),
        letterSpacing = 0.8.sp
    )
}

// ── Profile info card ─────────────────────────────────────────────────────────
@Composable
private fun ProfileInfoCard(user: UserProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BeeCard),
        border = BorderStroke(0.5.dp, BeeCardBorder)
    ) {
        Column {
            InfoRow(emoji = "✉️", label = "Email",   value = user.email)
            Divider(color = BeeAmberLight, thickness = 0.5.dp)
            InfoRow(emoji = "💬", label = "Status",  value = user.message)
            Divider(color = BeeAmberLight, thickness = 0.5.dp)
            InfoRow(emoji = "🕐", label = "Joined",  value = user.time)
        }
    }
}

@Composable
private fun InfoRow(emoji: String, label: String, value: String) {
    Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BeeAmberLight),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 14.sp)
        }
        Column {
            Text(label, fontSize = 10.sp, color = Color(0xFF854F0B))
            Spacer(Modifier.height(1.dp))
            Text(value.ifBlank { "—" }, fontSize = 13.sp, color = Color(0xFF1A1206))
        }
    }
}

// ── Share button ──────────────────────────────────────────────────────────────
@Composable
private fun BeeShareButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BeeAmber)
    ) {
        Icon(Icons.Default.Share, contentDescription = null, tint = BeeBrown)
        Spacer(Modifier.width(8.dp))
        Text("Share my referral link", color = BeeBrown, fontWeight = FontWeight.Medium)
    }
}