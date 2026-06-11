package com.example.drbee.ProfileScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drbee.Helper.AppConfig
import com.example.drbee.decodeBase64ToImageBitmap
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.database
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

// ── Data model ────────────────────────────────────────────────────────────────
data class ReferrerProfile(
    val uid        : String            = "",
    val name       : String            = "",
    val username   : String            = "",
    val bio        : String            = "",
    val avatarBase64 : String          = "",   // base64, same as community posts
    val postsCount : Int               = 0,
    val followers  : Int               = 0,
    val following  : Int               = 0,
    // Each entry: Pair(postId, imageBase64) — only posts that have an image
    val posts      : List<Pair<String, String>> = emptyList()
)

// ── Fetch: user node + community_posts filtered by authorId ──────────────────
suspend fun fetchReferrerProfile(uid: String): ReferrerProfile? {
    return try {
        // 1. User profile node
        val userSnap = Firebase.database(AppConfig.DB_URL)
            .reference("users")
            .child(uid)
            .valueEvents
            .first()

        if (!userSnap.exists) return null

        val name     = userSnap.child("name").value as? String
            ?: userSnap.child("displayName").value as? String ?: "Unknown"
        val username = userSnap.child("username").value as? String
            ?: (userSnap.child("email").value as? String)?.substringBefore("@") ?: "user"
        val bio      = userSnap.child("bio").value as? String ?: ""
        val avatar   = userSnap.child("profileImage").value as? String
            ?: userSnap.child("photoUrl").value as? String ?: ""
        val followers = (userSnap.child("followers").value as? Long)?.toInt() ?: 0
        val following = (userSnap.child("following").value as? Long)?.toInt() ?: 0

        // 2. All community posts — filter client-side by authorId
        val postsSnap = Firebase.database(AppConfig.DB_URL)
            .reference("community_posts")
            .valueEvents
            .first()

        val userPosts = postsSnap.children
            .filter { child ->
                (child.child("authorId").value as? String) == uid
            }
            .mapNotNull { child ->
                val postId   = child.key ?: return@mapNotNull null
                val imageB64 = child.child("imageBase64").value as? String
                if (!imageB64.isNullOrBlank()) Pair(postId, imageB64) else null
            }
            .sortedByDescending {
                // reuse timestamp from the snapshot for ordering
                postsSnap.child(it.first).child("timestamp").value as? Long ?: 0L
            }

        ReferrerProfile(
            uid          = uid,
            name         = name,
            username     = username,
            bio          = bio,
            avatarBase64 = avatar,
            postsCount   = postsSnap.children.count { child ->
                (child.child("authorId").value as? String) == uid
            },
            followers    = followers,
            following    = following,
            posts        = userPosts
        )
    } catch (e: Exception) { null }
}

// ── Count formatter ───────────────────────────────────────────────────────────
private fun Int.formatCount(): String = when {
    this >= 1_000_000 -> {
        val i = ((this / 1_000_000.0) * 10).roundToInt()
        "${i / 10}.${i % 10}M"
    }
    this >= 1_000 -> {
        val i = ((this / 1_000.0) * 10).roundToInt()
        "${i / 10}.${i % 10}K"
    }
    else -> this.toString()
}

// ── Main sheet ────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferralProfileSheet(
    referrerId : String,
    onDismiss  : () -> Unit
) {
    val gradientBrush = WonderBeeTheme.extendedDesign.primaryGradientBrush
    val surface       = WonderBeeTheme.extendedDesign.surfaceBackground
    val onSurface     = WonderBeeTheme.materialScheme.onBackground

    var profile by remember { mutableStateOf<ReferrerProfile?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(referrerId) {
        profile = fetchReferrerProfile(referrerId)
        loading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = surface,
        dragHandle       = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(onSurface.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
            )
        },
        modifier = Modifier.fillMaxHeight(0.88f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text       = if (loading) "Profile" else (profile?.username ?: "Profile"),
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = onSurface,
                    modifier   = Modifier.align(Alignment.Center)
                )
                IconButton(
                    onClick  = onDismiss,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector        = Icons.Default.Close,
                        contentDescription = "Close",
                        tint               = onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            HorizontalDivider(color = onSurface.copy(alpha = 0.08f), thickness = 0.5.dp)

            when {
                loading -> {
                    Box(
                        modifier         = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(36.dp),
                            strokeWidth = 2.5.dp,
                            color       = WonderBeeTheme.extendedDesign.inputFocusedBorderColor
                        )
                    }
                }
                profile == null -> {
                    Box(
                        modifier         = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🐝", fontSize = 40.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text      = "Profile not found",
                                color     = onSurface.copy(alpha = 0.5f),
                                fontSize  = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                else -> {
                    ProfileContent(
                        profile       = profile!!,
                        gradientBrush = gradientBrush,
                        onSurface     = onSurface
                    )
                }
            }
        }
    }
}

// ── Profile content ───────────────────────────────────────────────────────────
@Composable
private fun ProfileContent(
    profile       : ReferrerProfile,
    gradientBrush : Brush,
    onSurface     : Color
) {
    // Decode avatar once
    val avatarBitmap: ImageBitmap? = remember(profile.avatarBase64) {
        profile.avatarBase64.takeIf { it.isNotBlank() }
            ?.let { decodeBase64ToImageBitmap(it) }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Avatar + stats ────────────────────────────────────────────────
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Gradient-ring avatar
            Box(
                modifier         = Modifier
                    .size(82.dp)
                    .background(gradientBrush, CircleShape)
                    .padding(2.5.dp)
                    .background(WonderBeeTheme.extendedDesign.surfaceBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (avatarBitmap != null) {
                    Image(
                        bitmap           = avatarBitmap,
                        contentDescription = null,
                        modifier         = Modifier
                            .size(76.dp)
                            .clip(CircleShape),
                        contentScale     = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier         = Modifier
                            .size(76.dp)
                            .background(onSurface.copy(alpha = 0.08f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Person,
                            contentDescription = null,
                            tint               = onSurface.copy(alpha = 0.4f),
                            modifier           = Modifier.size(40.dp)
                        )
                    }
                }
            }

            // Stats
            Row(
                modifier              = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                StatColumn("Posts",     profile.postsCount.formatCount(), onSurface)
                StatColumn("Followers", profile.followers.formatCount(),  onSurface)
                StatColumn("Following", profile.following.formatCount(),  onSurface)
            }
        }

        // ── Name + bio ────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text       = profile.name,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                color      = onSurface
            )
            if (profile.bio.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text       = profile.bio,
                    fontSize   = 13.sp,
                    color      = onSurface.copy(alpha = 0.75f),
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Follow button ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(gradientBrush),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = "Follow",
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color      = Color.White
            )
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = onSurface.copy(alpha = 0.08f), thickness = 0.5.dp)

        // ── Grid tab indicator ────────────────────────────────────────────
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .height(44.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = Icons.Default.GridOn,
                contentDescription = "Posts",
                tint               = onSurface,
                modifier           = Modifier.size(22.dp)
            )
        }

        HorizontalDivider(color = onSurface.copy(alpha = 0.08f), thickness = 0.5.dp)

        // ── 3-column post grid ────────────────────────────────────────────
        if (profile.posts.isEmpty()) {
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🐝", fontSize = 32.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text  = "No posts yet",
                        color = onSurface.copy(alpha = 0.4f),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns           = GridCells.Fixed(3),
                modifier          = Modifier.fillMaxSize(),
                userScrollEnabled = true
            ) {
                items(
                    items = profile.posts,
                    key   = { it.first }           // postId as stable key
                ) { (_, imageBase64) ->
                    PostThumbnail(
                        imageBase64 = imageBase64,
                        onSurface   = onSurface
                    )
                }
            }
        }
    }
}

// ── Stat column ───────────────────────────────────────────────────────────────
@Composable
private fun StatColumn(label: String, value: String, onSurface: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = onSurface)
        Text(text = label, fontSize = 12.sp, color = onSurface.copy(alpha = 0.55f))
    }
}

// ── Post thumbnail (base64) ───────────────────────────────────────────────────
@Composable
private fun PostThumbnail(imageBase64: String, onSurface: Color) {
    // Decode inside remember so it only runs once per unique base64 string
    val bitmap: ImageBitmap? = remember(imageBase64) {
        imageBase64.takeIf { it.isNotBlank() }
            ?.let { decodeBase64ToImageBitmap(it) }
    }

    Box(
        modifier         = Modifier
            .aspectRatio(1f)
            .padding(0.5.dp)
            .background(onSurface.copy(alpha = 0.06f)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap             = bitmap,
                contentDescription = null,
                modifier           = Modifier.fillMaxSize(),
                contentScale       = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector        = Icons.Default.Person,
                contentDescription = null,
                tint               = onSurface.copy(alpha = 0.15f),
                modifier           = Modifier.size(24.dp)
            )
        }
    }
}