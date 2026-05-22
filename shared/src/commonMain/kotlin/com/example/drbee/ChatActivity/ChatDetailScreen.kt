package com.example.drbee.ChatActivity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drbee.ProfileScreen.ThemePreferencesManager
import com.example.drbee.ProfileScreen.WonderBeeTheme
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.database.database
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

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

    val currentUserId = remember { Firebase.auth.currentUser?.uid ?: "" }

    // ✅ FIX 1: Explicit trailing forward slash ensures regional sharding routing matches across targets
    val databaseUrl = "https://doctor-bee-2d622-default-rtdb.firebaseio.com/"

    // ✅ FIX 2: Normalized lowercase sorting handles web engine variations
    val roomId = remember(currentUserId, chat.id) {
        val id1 = currentUserId.lowercase()
        val id2 = chat.id.lowercase()
        if (id1 < id2) "${id1}_${id2}" else "${id2}_${id1}"
    }

    val avatarColor = remember(chat.colorHex) {
        try {
            Color(chat.colorHex.removePrefix("#").toLong(16) or 0xFF000000)
        } catch (e: Exception) {
            Color(0xFF075E54)
        }
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
                            val isMessageSentByMe = msg.senderId == currentUserId

                            msg.copy(
                                id = childSnapshot.key ?: "",
                                isMe = isMessageSentByMe
                            )
                        } catch (e: Exception) {
                            Napier.e("Failed to deserialize message object structure: ${e.message}")
                            null
                        }
                    }
                    // ✅ Note: Messages stream in Firebase order since sorting is removed
                }
                .collect { fetchedMessages ->
                    dbMessages = fetchedMessages
                }
        } catch (e: Exception) {
            Napier.e("Message websocket stream connection lost: ${e.message}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WonderBeeTheme.extendedDesign.surfaceBackground)
    ) {
        // TOP HEADER BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(WonderBeeTheme.extendedDesign.primaryGradientBrush)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        )
        {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null,
                tint = WonderBeeTheme.materialScheme.onPrimary,
                modifier = Modifier.clickable { onBack() }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(45.dp)
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chat.name.firstOrNull()?.toString() ?: "?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = chat.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        // REALTIME CHAT LIST
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(10.dp),
            verticalArrangement = Arrangement.Top
        ) {
            items(dbMessages) { msg ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = if (msg.isMe) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    // 1. Assign backgrounds from the active theme matrix preferences
                    val bubbleBgColor = if (msg.isMe) {
                        // Sender gets Color 2 if custom overrides are active, otherwise falls back to system primary
                        if (ThemePreferencesManager.isCustomColorEnabled) ThemePreferencesManager.customColorEnd
                        else WonderBeeTheme.materialScheme.primary
                    } else {
                        // Receiver gets Color 1 if custom overrides are active, otherwise falls back to system secondary
                        if (ThemePreferencesManager.isCustomColorEnabled) ThemePreferencesManager.customColorStart
                        else WonderBeeTheme.materialScheme.secondary
                    }

                    // 2. KMP-safe luminance calculation to keep reading text high-contrast and legible
                    val luminance = (0.299f * bubbleBgColor.red) + (0.587f * bubbleBgColor.green) + (0.114f * bubbleBgColor.blue)
                    val bubbleTextColor = if (luminance > 0.5f) Color(0xFF0F1A34) else Color.White

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = bubbleBgColor
                        ),
                        shape = if (msg.isMe) {
                            // Sleek asymmetrical shape tailored for sender placement orientation
                            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
                        } else {
                            // Sleek asymmetrical shape tailored for receiver placement orientation
                            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
                        },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = msg.text,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            color = bubbleTextColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // PERSISTENT TEXT INPUT AREA
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "Type message...",
                        // Blends text smoothly against your theme layout color
                        color = WonderBeeTheme.materialScheme.onBackground.copy(alpha = 0.5f)
                    )
                },
                shape = RoundedCornerShape(30.dp),
                textStyle = LocalTextStyle.current.copy(
                    color = WonderBeeTheme.materialScheme.onBackground
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    // ✅ Sets your solid layout token directly inside the container color properties
                    focusedContainerColor = WonderBeeTheme.extendedDesign.surfaceBackground,
                    unfocusedContainerColor = WonderBeeTheme.extendedDesign.surfaceBackground,

                    // Match borders to your defined focused/unfocused system theme lines
                    focusedBorderColor = WonderBeeTheme.extendedDesign.inputFocusedBorderColor,
                    unfocusedBorderColor = WonderBeeTheme.extendedDesign.inputUnfocusedBorderColor,

                    // Sync cursor indicator to your primary accent selection point
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
                                val messageRef = Firebase.database(databaseUrl)
                                    .reference("chats_messages")
                                    .child(roomId)
                                    .push()

//                                val newMessage = FirebaseMessageModel(
//                                    id = messageRef.key ?: "",
//                                    text = outgoingText,
//                                    senderId = currentUserId,
//                                    isMe = true,
//                                    // ✅ FIX 3: Replaced local TimeSource with platform-agnostic millisecond clock
//                                    timestamp = TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds
//                                )

                                val newMessage = FirebaseMessageModel(
                                    id = messageRef.key ?: "",
                                    text = outgoingText,
                                    senderId = currentUserId
                                )

                                messageRef.setValue(newMessage)
                            } catch (e: Exception) {
                                Napier.e("Transmission failed writing message record: ${e.message}")
                            }
                        }
                    }
                },

                modifier = Modifier
                    .background(
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
