package com.example.drbee.ChatActivity

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drbee.Helper.SessionManager
import com.example.drbee.NotificationService
import com.example.drbee.ProfileScreen.ThemePreferencesManager
import com.example.drbee.ProfileScreen.WonderBeeTheme
import com.example.drbee.decodeBase64ToImageBitmap
import com.example.drbee.logCrashException
import com.example.drbee.logCrashMessage
import com.example.drbee.rememberImagePickerLauncher
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.database.database
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DB_URL = "https://doctor-bee-2d622-default-rtdb.firebaseio.com/"

/**
 * Chat detail screen.
 * Takes the already-resolved [FirebaseChatModel] — no extra Firebase fetch needed.
 * ChatActivity already has the model from its user list, so we reuse it directly.
 */
@Composable
fun ChatDetailScreen(
    chat   : FirebaseChatModel,   // the other user — already loaded by ChatActivity
    onBack : () -> Unit
) {
    val currentUserId = remember {
        Firebase.auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: SessionManager.savedUserId.takeIf { it.isNotBlank() }
            ?: ""
    }

    var messageText   by remember { mutableStateOf("") }
    var dbMessages    by remember { mutableStateOf<List<FirebaseMessageModel>>(emptyList()) }
    var profileBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    val scope               = rememberCoroutineScope()
    val listState           = rememberLazyListState()
    val notificationService = remember { NotificationService() }

    // Deterministic, order-independent room ID
    val roomId = remember(currentUserId, chat.id) {
        val a = currentUserId.lowercase()
        val b = chat.id.lowercase()
        if (a < b) "${a}_${b}" else "${b}_${a}"
    }

    // ── Profile image ─────────────────────────────────────────────────────────
    LaunchedEffect(chat.profileImage) {
        profileBitmap = chat.profileImage
            ?.takeIf { it.isNotEmpty() }
            ?.let { withContext(Dispatchers.Default) { decodeBase64ToImageBitmap(it) } }
    }

    // ── Auto-scroll on new message ────────────────────────────────────────────
    LaunchedEffect(dbMessages.size) {
        if (dbMessages.isNotEmpty()) listState.animateScrollToItem(dbMessages.size - 1)
    }

    // ── Real-time message stream ──────────────────────────────────────────────
    LaunchedEffect(roomId) {
        try {
            Firebase.database(DB_URL)
                .reference("chats_messages")
                .child(roomId)
                .valueEvents
                .mapNotNull { snapshot ->
                    snapshot.children.mapNotNull { child ->
                        try {
                            child.value<FirebaseMessageModel>().copy(
                                id   = child.key ?: "",
                                isMe = child.value<FirebaseMessageModel>().senderId == currentUserId
                            )
                        } catch (e: Exception) {
                            Napier.e("Deserialize error: ${e.message}")
                            null
                        }
                    }
                }
                .collect { dbMessages = it }
        } catch (e: Exception) {
            Napier.e("Stream error: ${e.message}")
        }
    }

    // ── Send message ──────────────────────────────────────────────────────────
    fun sendMessage(text: String = "", imageBase64: String = "") {
        if (currentUserId.isEmpty()) return
        scope.launch {
            try {
                val ref = Firebase.database(DB_URL)
                    .reference("chats_messages")
                    .child(roomId)
                    .push()

                ref.setValue(
                    FirebaseMessageModel(
                        id          = ref.key ?: "",
                        text        = text,
                        imageBase64 = imageBase64.ifEmpty { null },
                        senderId    = currentUserId
                    )
                )

                val senderName = runCatching {
                    Firebase.database(DB_URL)
                        .reference("users")
                        .child(currentUserId)
                        .child("name")
                        .valueEvents
                        .first()
                        .value as? String
                }.getOrNull() ?: chat.name

                notificationService.sendPushNotification(
                    recipientUserId = chat.id,
                    senderName      = senderName,
                    messageText     = text.ifEmpty { "📷 Image" },
                    senderId        = currentUserId,
                    roomId          = roomId,
                    isChat          = true
                )
            } catch (e: Exception) {
                Napier.e("Send failed: ${e.message}")
            }
        }
    }

    // ── Image picker ──────────────────────────────────────────────────────────
    val imagePicker = rememberImagePickerLauncher { base64 ->
        if (base64.isNotEmpty()) sendMessage(imageBase64 = base64)
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WonderBeeTheme.extendedDesign.surfaceBackground)
            .systemBarsPadding()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            ChatHeader(
                chat          = chat,
                profileBitmap = profileBitmap,
                onBack        = onBack
            )

            LazyColumn(
                state               = listState,
                modifier            = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(items = dbMessages, key = { it.id }) { msg ->
                    ChatMessageBubble(msg = msg)
                }
            }

            ChatInputBar(
                messageText  = messageText,
                onTextChange = { messageText = it },
                onAttach     = { imagePicker.launch() },
                onSend       = {
                    val text = messageText.trim()
                    if (text.isNotBlank()) {
                        logCrashMessage("User initiated message delivery on ChatDetailScreen")
                        try {
                            messageText = ""
                            sendMessage(text = text)
                        } catch (t: Throwable) {
                            logCrashException(t)
                        }
                    }
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatHeader(
    chat          : FirebaseChatModel,
    profileBitmap : ImageBitmap?,
    onBack        : () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WonderBeeTheme.extendedDesign.primaryGradientBrush)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector        = Icons.Default.ArrowBack,
            contentDescription = "Back",
            tint               = WonderBeeTheme.materialScheme.onPrimary,
            modifier           = Modifier.clickable { onBack() }
        )
        Spacer(Modifier.width(12.dp))

        Box(
            modifier         = Modifier.size(45.dp).clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (profileBitmap != null) {
                Image(
                    bitmap             = profileBitmap,
                    contentDescription = null,
                    modifier           = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale       = ContentScale.Crop
                )
            } else {
                Box(
                    modifier         = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = chat.name.take(1).uppercase(),
                        color      = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))
        Text(
            text       = chat.name,
            color      = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize   = 18.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Input bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatInputBar(
    messageText  : String,
    onTextChange : (String) -> Unit,
    onAttach     : () -> Unit,
    onSend       : () -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .background(WonderBeeTheme.extendedDesign.surfaceBackground)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onAttach) {
            Icon(
                imageVector        = Icons.Default.AttachFile,
                contentDescription = "Attach image",
                tint               = WonderBeeTheme.materialScheme.primary
            )
        }

        Spacer(Modifier.width(4.dp))

        OutlinedTextField(
            value         = messageText,
            onValueChange = onTextChange,
            modifier      = Modifier.weight(1f),
            placeholder   = {
                Text(
                    text  = "Type message...",
                    color = WonderBeeTheme.materialScheme.onBackground.copy(alpha = 0.5f)
                )
            },
            shape     = RoundedCornerShape(30.dp),
            textStyle = LocalTextStyle.current.copy(
                color = WonderBeeTheme.materialScheme.onBackground
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor   = WonderBeeTheme.extendedDesign.surfaceBackground,
                unfocusedContainerColor = WonderBeeTheme.extendedDesign.surfaceBackground,
                focusedBorderColor      = WonderBeeTheme.extendedDesign.inputFocusedBorderColor,
                unfocusedBorderColor    = WonderBeeTheme.extendedDesign.inputUnfocusedBorderColor,
                cursorColor             = WonderBeeTheme.materialScheme.primary
            )
        )

        Spacer(Modifier.width(8.dp))

        FloatingActionButton(
            onClick        = onSend,
            modifier       = Modifier.background(
                brush = WonderBeeTheme.extendedDesign.primaryGradientBrush,
                shape = FloatingActionButtonDefaults.shape
            ),
            containerColor = Color.Transparent,
            elevation      = FloatingActionButtonDefaults.elevation(0.dp)
        ) {
            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Message bubble
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ChatMessageBubble(msg: FirebaseMessageModel) {
    var imageBitmap by remember(msg.id) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(msg.imageBase64) {
        imageBitmap = msg.imageBase64
            ?.takeIf { it.isNotEmpty() }
            ?.let { withContext(Dispatchers.Default) { decodeBase64ToImageBitmap(it) } }
    }

    val bubbleBg = if (msg.isMe) {
        if (ThemePreferencesManager.isCustomColorEnabled) ThemePreferencesManager.customColorEnd
        else WonderBeeTheme.materialScheme.primary
    } else {
        if (ThemePreferencesManager.isCustomColorEnabled) ThemePreferencesManager.customColorStart
        else WonderBeeTheme.materialScheme.secondary
    }

    val luminance   = 0.299f * bubbleBg.red + 0.587f * bubbleBg.green + 0.114f * bubbleBg.blue
    val textColor   = if (luminance > 0.5f) Color(0xFF0F1A34) else Color.White
    val bubbleShape = if (msg.isMe)
        RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp)
    else
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp)

    Box(
        modifier         = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        contentAlignment = if (msg.isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Card(
            colors    = CardDefaults.cardColors(containerColor = bubbleBg),
            shape     = bubbleShape,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {

                if (!msg.imageBase64.isNullOrEmpty()) {
                    if (imageBitmap != null) {
                        Image(
                            bitmap             = imageBitmap!!,
                            contentDescription = "Image",
                            modifier           = Modifier
                                .widthIn(max = 220.dp)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Gray.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(28.dp),
                                color       = textColor,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }

                if (msg.text.isNotEmpty()) {
                    if (!msg.imageBase64.isNullOrEmpty()) Spacer(Modifier.height(6.dp))
                    Text(
                        text  = msg.text,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}