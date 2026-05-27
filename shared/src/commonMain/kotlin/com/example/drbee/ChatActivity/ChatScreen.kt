package com.example.drbee.ChatActivity

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drbee.Helper.SessionManager
import com.example.drbee.ProfileScreen.WonderBeeTheme
import com.example.drbee.decodeBase64ToImageBitmap
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.database.database
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import kotlin.time.TimeSource

// ─────────────────────────────────────────────────────────────────────────────
// ENTRY POINT
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ChatActivity(
    onChatDetailStateChanged : (Boolean) -> Unit = {},
) {
    var liveChatList by remember { mutableStateOf<List<FirebaseChatModel>>(emptyList()) }
    var isLoading    by remember { mutableStateOf(true) }
    var selectedChat by remember { mutableStateOf<FirebaseChatModel?>(null) }

    val auth        = remember { Firebase.auth }
    val currentUid  = auth.currentUser?.uid ?: ""
    val databaseUrl = "https://doctor-bee-2d622-default-rtdb.firebaseio.com/"

    LaunchedEffect(selectedChat) {
        onChatDetailStateChanged(selectedChat != null)
    }

    LaunchedEffect(currentUid) {
        val resolvedUid = when {
            currentUid.isNotBlank()   -> currentUid
            SessionManager.isLoggedIn -> SessionManager.savedUserId
            else                      -> ""
        }
        try {
            Firebase.database(databaseUrl)
                .reference("users")
                .valueEvents
                .mapNotNull { snapshot ->
                    snapshot.children.mapNotNull { child ->
                        try {
                            if (child.key == resolvedUid) return@mapNotNull null
                            child.value<FirebaseChatModel>().copy(id = child.key ?: "")
                        } catch (e: Exception) {
                            Napier.e("Serialization error: ${e.message}")
                            null
                        }
                    }
                }
                .collect { list ->
                    liveChatList = list
                    isLoading    = false
                }
        } catch (e: Exception) {
            Napier.e("Database error: ${e.message}")
            isLoading = false
        }
    }

    when {
        isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WonderBeeTheme.materialScheme.primary)
            }
        }
        selectedChat == null -> {
            ChatListScreen(
                chatList               = liveChatList,
                onChatClick            = { selectedChat = it }
            )
        }
        else -> {
            ChatDetailScreen(
                chat                   = selectedChat!!,
                onBack                 = { selectedChat = null }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CHAT LIST SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ChatListScreen(
    chatList             : List<FirebaseChatModel>,
    onChatClick          : (FirebaseChatModel) -> Unit
) {
    val auth          = remember { Firebase.auth }
    val currentUid    = auth.currentUser?.uid ?: ""

    val resolvedUid = remember(currentUid) {
        when {
            currentUid.isNotBlank()   -> currentUid
            SessionManager.isLoggedIn -> SessionManager.savedUserId
            else                      -> ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WonderBeeTheme.extendedDesign.surfaceBackground)
    ) {
        Text(
            text     = "Chats",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color    = WonderBeeTheme.materialScheme.onBackground,
            modifier = Modifier.padding(start = 20.dp, top = 50.dp, bottom = 16.dp)
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(chatList) { firebaseChat ->

                // ✅ Decode profile image per row independently
                var rowBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

                LaunchedEffect(firebaseChat.profileImage) {
                    val base64 = firebaseChat.profileImage.orEmpty()
                    if (base64.isNotEmpty()) {
                        rowBitmap = withContext(Dispatchers.Default) {
                            decodeBase64ToImageBitmap(base64)
                        }
                    } else {
                        rowBitmap = null
                    }
                }

                val parsedColor = remember(firebaseChat.colorHex) {
                    try {
                        val hex = firebaseChat.colorHex.removePrefix("#")
                        Color(hex.toLong(16) or 0xFF000000L)
                    } catch (e: Exception) {
                        Color(0xFF6C63FF)
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { onChatClick(firebaseChat) },
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Row(
                        modifier          = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        Box(modifier = Modifier.clip(CircleShape)) {
                            if (rowBitmap != null) {
                                Image(
                                    bitmap             = rowBitmap!!,
                                    contentDescription = null,
                                    modifier           = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .background(parsedColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text       = firebaseChat.name.take(1).uppercase(),
                                        color      = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize   = 22.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text  = firebaseChat.name,
                                style = TextStyle(
                                    brush      = WonderBeeTheme.extendedDesign.primaryGradientBrush,
                                    fontSize   = 16.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text  = firebaseChat.message.ifEmpty { "Say hi 👋" },
                                style = TextStyle(
                                    brush      = WonderBeeTheme.extendedDesign.primaryGradientBrush,
                                    fontSize   = 13.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                maxLines = 1
                            )
                        }

                        if (firebaseChat.time.isNotEmpty()) {
                            Text(
                                text     = firebaseChat.time,
                                fontSize = 11.sp,
                                color    = WonderBeeTheme.materialScheme.onBackground.copy(alpha = 0.4f)
                            )
                        }
                    }
                }

                Divider(
                    modifier  = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color     = WonderBeeTheme.materialScheme.onBackground.copy(alpha = 0.08f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UTILITY
// ─────────────────────────────────────────────────────────────────────────────

fun clock_timestamp(): Long = TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds