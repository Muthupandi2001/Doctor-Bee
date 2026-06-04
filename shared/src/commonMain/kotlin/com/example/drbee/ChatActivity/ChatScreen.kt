package com.example.drbee.ChatActivity

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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

@Composable
fun ChatActivity(
    targetOtherUserId        : String?  = null,
    notificationVersion      : Int      = 0,
    onChatDetailStateChanged : (Boolean) -> Unit = {}
) {
    val databaseUrl = "https://doctor-bee-2d622-default-rtdb.firebaseio.com/"
    val currentUid  = Firebase.auth.currentUser?.uid
        ?: SessionManager.savedUserId.takeIf { it.isNotBlank() }
        ?: ""

    var chatList     by remember { mutableStateOf<List<FirebaseChatModel>>(emptyList()) }
    var isLoading    by remember { mutableStateOf(true) }
    var selectedChat by remember { mutableStateOf<FirebaseChatModel?>(null) }

    // ── Load user list (real-time) ────────────────────────────────────────────
    LaunchedEffect(currentUid) {
        if (currentUid.isBlank()) { isLoading = false; return@LaunchedEffect }
        try {
            Firebase.database(databaseUrl)
                .reference("users")
                .valueEvents
                .mapNotNull { snapshot ->
                    snapshot.children.mapNotNull { child ->
                        if (child.key == currentUid) return@mapNotNull null
                        try { child.value<FirebaseChatModel>().copy(id = child.key ?: "") }
                        catch (e: Exception) { Napier.e("Serialization: ${e.message}"); null }
                    }
                }
                .collect { list ->
                    chatList  = list
                    isLoading = false
                }
        } catch (e: Exception) {
            Napier.e("DB error: ${e.message}")
            isLoading = false
        }
    }

    // ── Auto-open from notification ───────────────────────────────────────────
    // Three keys so every case is covered:
    //
    // chatList.size  → re-fires once users load (killed-state: list arrives after
    //                  the event, so we need a second chance to match)
    //
    // notificationVersion → re-fires when the same user is tapped again while
    //                        the app is live (version bumps even if UID unchanged)
    //
    // targetOtherUserId   → re-fires when a different user's notification arrives
    LaunchedEffect(targetOtherUserId, notificationVersion, chatList.size) {
        if (targetOtherUserId.isNullOrBlank()) return@LaunchedEffect
        if (chatList.isEmpty()) return@LaunchedEffect
        val match = chatList.firstOrNull { it.id == targetOtherUserId }
        if (match != null) {
            selectedChat = match
        } else {
            Napier.w("ChatActivity: user not found in list — id=$targetOtherUserId")
        }
    }

    // ── Report open/closed state to MainScreen ────────────────────────────────
    DisposableEffect(selectedChat) {
        onChatDetailStateChanged(selectedChat != null)
        onDispose { onChatDetailStateChanged(false) }
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    when {
        isLoading            -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator(color = WonderBeeTheme.materialScheme.primary)
        }
        selectedChat != null -> ChatDetailScreen(
            chat   = selectedChat!!,
            onBack = { selectedChat = null }
        )
        else                 -> ChatListScreen(
            chatList    = chatList,
            onChatClick = { selectedChat = it }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ChatListScreen(
    chatList    : List<FirebaseChatModel>,
    onChatClick : (FirebaseChatModel) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WonderBeeTheme.extendedDesign.surfaceBackground)
    ) {
        Text(
            text       = "Chats",
            fontSize   = 34.sp,
            fontWeight = FontWeight.Bold,
            color      = WonderBeeTheme.materialScheme.onBackground,
            modifier   = Modifier.padding(start = 20.dp, top = 50.dp, bottom = 16.dp)
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(chatList, key = { it.id }) { chat ->
                ChatListRow(chat = chat, onChatClick = { onChatClick(chat) })
                HorizontalDivider(
                    modifier  = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color     = WonderBeeTheme.materialScheme.onBackground.copy(alpha = 0.08f)
                )
            }
        }
    }
}

@Composable
private fun ChatListRow(chat: FirebaseChatModel, onChatClick: () -> Unit) {
    var rowBitmap by remember(chat.id) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(chat.profileImage) {
        rowBitmap = chat.profileImage?.takeIf { it.isNotEmpty() }
            ?.let { withContext(Dispatchers.Default) { decodeBase64ToImageBitmap(it) } }
    }

    val parsedColor = remember(chat.colorHex) {
        runCatching {
            Color(chat.colorHex.removePrefix("#").toLong(16) or 0xFF000000L)
        }.getOrDefault(Color(0xFF6C63FF))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onChatClick() },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier         = Modifier.size(60.dp).clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (rowBitmap != null) {
                    Image(
                        bitmap             = rowBitmap!!,
                        contentDescription = null,
                        modifier           = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale       = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier         = Modifier
                            .fillMaxSize()
                            .background(parsedColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = chat.name.take(1).uppercase(),
                            color      = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 22.sp
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    chat.name,
                    style = TextStyle(
                        brush      = WonderBeeTheme.extendedDesign.primaryGradientBrush,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    chat.message.ifEmpty { "Say hi 👋" },
                    style    = TextStyle(
                        brush    = WonderBeeTheme.extendedDesign.primaryGradientBrush,
                        fontSize = 13.sp
                    ),
                    maxLines = 1
                )
            }
            if (chat.time.isNotEmpty()) {
                Text(
                    chat.time,
                    fontSize = 11.sp,
                    color    = WonderBeeTheme.materialScheme.onBackground.copy(alpha = 0.4f)
                )
            }
        }
    }
}

fun clock_timestamp(): Long =
    TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds