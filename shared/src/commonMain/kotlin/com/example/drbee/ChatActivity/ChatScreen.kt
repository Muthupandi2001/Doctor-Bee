package com.example.drbee.ChatActivity

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drbee.MainScreen.BottomTab
import com.example.drbee.MainScreen.applyGradientTint
import com.example.drbee.ProfileScreen.ThemePreferencesManager
import com.example.drbee.ProfileScreen.WonderBeeTheme
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.database.database
import drbee.shared.generated.resources.Res
import drbee.shared.generated.resources.apj
import drbee.shared.generated.resources.ic_next_button_rounded
import drbee.shared.generated.resources.unicorn
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.painterResource
import kotlin.text.contains
import kotlin.time.TimeSource

@Composable
fun ChatActivity() {
    var liveChatList by remember { mutableStateOf<List<FirebaseChatModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedChat by remember { mutableStateOf<FirebaseChatModel?>(null) }

    val activeGradient = WonderBeeTheme.extendedDesign.primaryGradientBrush
    val unselectedColor = WonderBeeTheme.materialScheme.onBackground.copy(alpha = 0.4f)

    // ✅ Get the current logged-in user's ID
    val currentUserId = remember { Firebase.auth.currentUser?.uid ?: "" }
    val databaseUrl = "https://doctor-bee-2d622-default-rtdb.firebaseio.com/"

    LaunchedEffect(currentUserId) {
        try {
            Firebase.database(databaseUrl)
                .reference("users")
                .valueEvents
                .mapNotNull { dataSnapshot ->
                    dataSnapshot.children.mapNotNull { childSnapshot ->
                        try {
                            // Skip loading your own account profile in the chat contact directory list
                            if (childSnapshot.key == currentUserId) return@mapNotNull null

                            val baseChat = childSnapshot.value<FirebaseChatModel>()
                            baseChat.copy(id = childSnapshot.key ?: "")
                        } catch (e: Exception) {
                            Napier.e("Serialization breakdown on chat node ${childSnapshot.key}: ${e.message}")
                            null
                        }
                    }
                }
                .collect { updatedList ->
                    liveChatList = updatedList
                    isLoading = false
                    Napier.d("Successfully loaded live chats count: ${updatedList.size}")
                }
        } catch (e: Exception) {
            Napier.e("Realtime database root socket broken: ${e.message}")
            isLoading = false
        }
    }

    Napier.d("Current state - selectedChat: ${selectedChat?.name}, list size: ${liveChatList.size}")

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF075E54))
        }
    } else if (selectedChat == null) {
        ChatListScreen(
            chatList = liveChatList,
            onChatClick = { selectedChat = it }
        )
    } else {
        ChatDetailScreen(
            chat = selectedChat!!,
            onBack = { selectedChat = null }
        )
    }
}

// ==========================================
// 3. CHAT LIST SCREEN
// ==========================================

@Composable
fun ChatListScreen(
    chatList: List<FirebaseChatModel>,
    onChatClick: (FirebaseChatModel) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WonderBeeTheme.extendedDesign.surfaceBackground.copy(0.5f))
    ) {
        Text(
            text = "Chats",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, top = 50.dp, bottom = 16.dp)
        )

        LazyColumn {
            items(chatList) { firebaseChat ->

                val parsedColor = remember(firebaseChat.colorHex) {
                    try {
                        val hex = firebaseChat.colorHex.removePrefix("#")
                        Color(hex.toLong(16) or 0xFF000000)
                    } catch (e: Exception) {
                        Color(0xFF6C63FF)
                    }
                }

                val uiChatModel = remember(firebaseChat, parsedColor) {
                    ChatModel(
                        name = firebaseChat.name,
                        message = firebaseChat.message,
                        time = firebaseChat.time,
                        color = parsedColor
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { onChatClick(firebaseChat) },
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
//                        Box(
//                            modifier = Modifier
//                                .size(40.dp)
//                                .clip(CircleShape)
//                                .background(parsedColor),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Text(
//                                text = uiChatModel.name.firstOrNull()?.toString() ?: "?",
//                                color = Color.White,
//                                fontWeight = FontWeight.Bold
//                            )
//                        }


                     if (uiChatModel.name.lowercase().contains("abdul")){
                         Image(
                             painter = painterResource(Res.drawable.apj),
                             contentDescription = "Next button",
                             modifier = Modifier .size(60.dp).clickable(
                                 interactionSource = remember { MutableInteractionSource() },
                                 indication = null,
                                 onClick = {

                                 }) ,  contentScale = ContentScale.Crop
                         )
                     }else if (uiChatModel.name.lowercase().contains("unicorn")){
                         Image(
                             painter = painterResource(Res.drawable.unicorn),
                             contentDescription = "Next button",
                             modifier = Modifier .size(60.dp).clickable(
                                 interactionSource = remember { MutableInteractionSource() },
                                 indication = null,
                                 onClick = {

                                 }), contentScale = ContentScale.Crop
                         )
                     }


                        Spacer(modifier = Modifier.width(12.dp))
                        Column {

                            Text(
                                text = uiChatModel.name,
                                style = TextStyle(
                                    brush = WonderBeeTheme.extendedDesign.primaryGradientBrush,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )


                            Text(
                                text = uiChatModel.message,
                                style = TextStyle(
                                    brush = WonderBeeTheme.extendedDesign.primaryGradientBrush,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
//                            Text(text = uiChatModel.name, fontWeight = FontWeight.Bold)
//                            Text(text = uiChatModel.message, color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}



fun clock_timestamp(): Long = TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds