package com.example.drbee.ChatActivity

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drbee.Helper.SessionManager
import com.example.drbee.NotificationService
import com.example.drbee.ProfileScreen.ThemePreferencesManager
import com.example.drbee.ProfileScreen.WonderBeeTheme
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.database.database
import drbee.shared.generated.resources.Res
import drbee.shared.generated.resources.apj
import drbee.shared.generated.resources.unicorn
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chat: FirebaseChatModel,
    onBack: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    var dbMessages by remember { mutableStateOf<List<FirebaseMessageModel>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // ✅ Uses expect/actual — no Android import needed
    val notificationService = remember { NotificationService() }

    var currentUserId by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        currentUserId = when {
            Firebase.auth.currentUser?.uid?.isNotBlank() == true ->
                Firebase.auth.currentUser?.uid ?: ""
            SessionManager.savedUserId.isNotBlank() ->
                SessionManager.savedUserId
            else -> ""
        }
    }

    val databaseUrl = "https://doctor-bee-2d622-default-rtdb.firebaseio.com/"

    val roomId = remember(currentUserId, chat.id) {
        val id1 = currentUserId.lowercase()
        val id2 = chat.id.lowercase()
        if (id1 < id2) "${id1}_${id2}" else "${id2}_${id1}"
    }

    LaunchedEffect(dbMessages.size) {
        if (dbMessages.isNotEmpty()) {
            listState.animateScrollToItem(dbMessages.size - 1)
        }
    }

    LaunchedEffect(roomId) {
        try {
            Firebase.database(databaseUrl)
                .reference("chats_messages")
                .child(roomId)
                .valueEvents
                .mapNotNull { dataSnapshot ->
                    dataSnapshot.children.mapNotNull { childSnapshot ->
                        try {
                            val msg = childSnapshot.value<FirebaseMessageModel>()
                            msg.copy(
                                id = childSnapshot.key ?: "",
                                isMe = msg.senderId == currentUserId
                            )
                        } catch (e: Exception) {
                            Napier.e("Deserialize error: ${e.message}")
                            null
                        }
                    }
                }
                .collect { fetchedMessages -> dbMessages = fetchedMessages }
        } catch (e: Exception) {
            Napier.e("Stream error: ${e.message}")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WonderBeeTheme.extendedDesign.surfaceBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── TOP HEADER ──────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WonderBeeTheme.extendedDesign.primaryGradientBrush)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    tint = WonderBeeTheme.materialScheme.onPrimary,
                    modifier = Modifier.clickable { onBack() }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier.size(45.dp).clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        chat.name.lowercase().contains("abdul") ->
                            Image(
                                painter = painterResource(Res.drawable.apj),
                                contentDescription = null,
                                modifier = Modifier.size(60.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        chat.name.lowercase().contains("unicorn") ->
                            Image(
                                painter = painterResource(Res.drawable.unicorn),
                                contentDescription = null,
                                modifier = Modifier.size(60.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = chat.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            // ── CHAT LIST ────────────────────────────
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                items(dbMessages) { msg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment =
                            if (msg.isMe) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        val bubbleBgColor = if (msg.isMe) {
                            if (ThemePreferencesManager.isCustomColorEnabled)
                                ThemePreferencesManager.customColorEnd
                            else WonderBeeTheme.materialScheme.primary
                        } else {
                            if (ThemePreferencesManager.isCustomColorEnabled)
                                ThemePreferencesManager.customColorStart
                            else WonderBeeTheme.materialScheme.secondary
                        }

                        val luminance = (0.299f * bubbleBgColor.red) +
                                (0.587f * bubbleBgColor.green) +
                                (0.114f * bubbleBgColor.blue)

                        val bubbleTextColor =
                            if (luminance > 0.5f) Color(0xFF0F1A34) else Color.White

                        Card(
                            colors = CardDefaults.cardColors(containerColor = bubbleBgColor),
                            shape = if (msg.isMe)
                                RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp)
                            else
                                RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Text(
                                text = msg.text,
                                modifier = Modifier.padding(
                                    horizontal = 14.dp,
                                    vertical = 10.dp
                                ),
                                color = bubbleTextColor,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // ── BOTTOM MESSAGE BAR ───────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WonderBeeTheme.extendedDesign.surfaceBackground)
                    .padding(12.dp)
                    .imePadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "Type message...",
                            color = WonderBeeTheme.materialScheme.onBackground.copy(alpha = 0.5f)
                        )
                    },
                    shape = RoundedCornerShape(30.dp),
                    textStyle = LocalTextStyle.current.copy(
                        color = WonderBeeTheme.materialScheme.onBackground
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = WonderBeeTheme.extendedDesign.surfaceBackground,
                        unfocusedContainerColor = WonderBeeTheme.extendedDesign.surfaceBackground,
                        focusedBorderColor = WonderBeeTheme.extendedDesign.inputFocusedBorderColor,
                        unfocusedBorderColor = WonderBeeTheme.extendedDesign.inputUnfocusedBorderColor,
                        cursorColor = WonderBeeTheme.materialScheme.primary
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))

                FloatingActionButton(
                    onClick = {
                        if (messageText.isNotBlank() && currentUserId.isNotEmpty()) {
                            val outgoingText = messageText
                            messageText = ""

                            scope.launch {
                                try {
                                    // ✅ 1: Save message to Firebase
                                    val messageRef = Firebase.database(databaseUrl)
                                        .reference("chats_messages")
                                        .child(roomId)
                                        .push()

                                    val newMessage = FirebaseMessageModel(
                                        id = messageRef.key ?: "",
                                        text = outgoingText,
                                        senderId = currentUserId
                                    )
                                    messageRef.setValue(newMessage)

                                    // ✅ 2: Get sender name from DB
                                    val senderSnapshot = Firebase.database(databaseUrl)
                                        .reference("users")
                                        .child(currentUserId)
                                        .child("name")
                                        .valueEvents
                                        .first()

                                    val senderName =
                                        (senderSnapshot.value as? String) ?: chat.name

                                    // ✅ 3: Send push notification via expect/actual
                                    notificationService.sendPushNotification(
                                        recipientUserId = chat.id,
                                        senderName = senderName,
                                        messageText = outgoingText,
                                        senderId = currentUserId,
                                        roomId = roomId
                                    )

                                } catch (e: Exception) {
                                    Napier.e("Send failed: ${e.message}")
                                }
                            }
                        }
                    },
                    modifier = Modifier.background(
                        brush = WonderBeeTheme.extendedDesign.primaryGradientBrush,
                        shape = FloatingActionButtonDefaults.shape
                    ),
                    containerColor = Color.Transparent,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    }
}