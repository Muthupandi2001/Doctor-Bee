package com.example.drbee.ChatActivity

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drbee.Helper.SessionManager
import com.example.drbee.MainScreen.BottomTab
import com.example.drbee.MainScreen.applyGradientTint
import com.example.drbee.ProfileScreen.ThemePreferencesManager
import com.example.drbee.ProfileScreen.WonderBeeTheme
import com.example.drbee.decodeBase64ToImageBitmap
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.database.database
import drbee.shared.generated.resources.Res
import drbee.shared.generated.resources.apj
import drbee.shared.generated.resources.ic_next_button_rounded
import drbee.shared.generated.resources.unicorn
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.painterResource
import theme.AppStrings.TRACK_GOAL_TITLE
import kotlin.text.contains
import kotlin.time.TimeSource

@Composable
fun ChatActivity() {
    var liveChatList by remember { mutableStateOf<List<FirebaseChatModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentUid_local by remember { mutableStateOf("") }
    var selectedChat by remember { mutableStateOf<FirebaseChatModel?>(null) }

    val activeGradient = WonderBeeTheme.extendedDesign.primaryGradientBrush
    val unselectedColor = WonderBeeTheme.materialScheme.onBackground.copy(alpha = 0.4f)
    val auth = remember { Firebase.auth }
    val currentUid = auth.currentUser?.uid ?: ""

    val databaseUrl = "https://doctor-bee-2d622-default-rtdb.firebaseio.com/"


    Napier.d(currentUid + "currentUid:::")

    LaunchedEffect(currentUid) {

        val currentUid_local = when {
            currentUid.isNotBlank() -> currentUid
            SessionManager.isLoggedIn -> SessionManager.savedUserId
            else -> ""
        }

        try {
            Firebase.database(databaseUrl)
                .reference("users")
                .valueEvents
                .mapNotNull { dataSnapshot ->
                    dataSnapshot.children.mapNotNull { childSnapshot ->
                        try {
                            // Skip loading your own account profile in the chat contact directory list
                            if (childSnapshot.key == currentUid_local) return@mapNotNull null

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


    val masterScrollState = rememberScrollState()

    var currentImageBase64 by remember { mutableStateOf("") }
    var profileBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

// 1. Safe extraction: Check if chat list has data and extract string safely
//    val primaryChatUser = chatList.isNotEmpty()

    val currentId = SessionManager.savedUserId





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
        val auth = remember { Firebase.auth }
        val currentUid = auth.currentUser?.uid ?: ""

        val currentUid_local = remember(currentUid) {
            when {
                currentUid.isNotBlank() -> currentUid
                SessionManager.isLoggedIn -> SessionManager.savedUserId
                else -> ""
            }
        }

        LazyColumn {
            items(chatList) { firebaseChat ->

                // 1. Check if this specific row belongs to the current user
                val isCurrentUser = firebaseChat.id == currentUid_local

                // 2. Decode the Base64 image ONLY for this specific user row independently
                var rowBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

                LaunchedEffect(firebaseChat.profileImage) {
                    val base64String = firebaseChat.profileImage.orEmpty()
                    if (base64String.isNotEmpty()) {
                        // Decodes in background thread to prevent UI lag
                        rowBitmap = withContext(Dispatchers.Default) {
                            decodeBase64ToImageBitmap(base64String)
                        }
                    } else {
                        rowBitmap = null
                    }
                }

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

                        // 3. Render the dynamic local row image
              Box(modifier = Modifier.clip(RoundedCornerShape(40.dp))){
                  if (rowBitmap != null) {
                      Image(
                          bitmap = rowBitmap!!,
                          contentDescription = TRACK_GOAL_TITLE,
                          modifier = Modifier
                              .size(60.dp), // Reduced from 300.dp to look proper in a list item row
                          contentScale = ContentScale.Crop
                      )
                  } else {
                      // Fallback placeholder when no profile image exists
                      Box(
                          modifier = Modifier
                              .size(60.dp)
                              .background(Color.LightGray, shape = CircleShape),
                          contentAlignment = Alignment.Center
                      ) {
                          Text(
                              text = uiChatModel.name.take(1).uppercase(),
                              color = Color.White,
                              fontWeight = FontWeight.Bold
                          )
                      }
                  }
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
                        }
                    }
                }
            }
        }

    }
}


fun clock_timestamp(): Long = TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds