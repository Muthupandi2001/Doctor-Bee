package com.example.drbee.ChatActivity

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
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.database.database
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock
import kotlin.time.TimeSource

@Composable
fun ChatActivity() {
    var liveChatList by remember { mutableStateOf<List<FirebaseChatModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedChat by remember { mutableStateOf<FirebaseChatModel?>(null) }

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
            .background(Brush.verticalGradient(listOf(Color(0xFFF5F7FA), Color(0xFFE4ECF7))))
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
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(parsedColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiChatModel.name.firstOrNull()?.toString() ?: "?",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = uiChatModel.name, fontWeight = FontWeight.Bold)
                            Text(text = uiChatModel.message, color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}


//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ChatDetailScreen(
//    chat: FirebaseChatModel, // This represents the 'other' user you clicked on
//    onBack: () -> Unit
//) {
//    var messageText by remember { mutableStateOf("") }
//    var dbMessages by remember { mutableStateOf<List<FirebaseMessageModel>>(emptyList()) }
//    val scope = rememberCoroutineScope()
//    val listState = rememberLazyListState()
//
//    val currentUserId = remember { Firebase.auth.currentUser?.uid ?: "" }
//    val databaseUrl = "https://doctor-bee-2d622-default-rtdb.firebaseio.com/"
//
//    // ✅ FIX 1: Generate a deterministic, bidirectional conversation room ID
//    // Alphabetically sorting the two IDs guarantees both sender and receiver access the identical database node!
////    val roomId = remember(currentUserId, chat.id) {
////        if (currentUserId < chat.id) "${currentUserId}_${chat.id}" else "${chat.id}_${currentUserId}"
////    }
//
//    val roomId = remember(currentUserId, chat.id) {
//        val id1 = currentUserId.lowercase()
//        val id2 = chat.id.lowercase()
//        if (id1 < id2) "${id1}_${id2}" else "${id2}_${id1}"
//    }
//
//    val avatarColor = remember(chat.colorHex) {
//        try {
//            Color(chat.colorHex.removePrefix("#").toLong(16) or 0xFF000000)
//        } catch (e: Exception) {
//            Color(0xFF075E54)
//        }
//    }
//
//    // ✅ FIX 2: Dynamic list auto-scroller whenever a new incoming/outgoing message arrives
//    LaunchedEffect(dbMessages.size) {
//        if (dbMessages.isNotEmpty()) {
//            listState.animateScrollToItem(dbMessages.size - 1)
//        }
//    }
//
//    // 1. ✅ REAL-TIME BIDIRECTIONAL STREAM LISTENER
//    LaunchedEffect(roomId) {
//        try {
//            Firebase.database(databaseUrl)
//                .reference("chats_messages")
//                .child(roomId) // 👈 Swapped out chat.id for our mutual symmetric roomId node
//                .valueEvents
//                .mapNotNull { dataSnapshot ->
//                    dataSnapshot.children.mapNotNull { childSnapshot ->
//                        try {
//                            val msg = childSnapshot.value<FirebaseMessageModel>()
//
//                            // Re-evaluate bubble side orientation flag contextually
//                            val isMessageSentByMe = msg.senderId == currentUserId
//
//                            msg.copy(
//                                id = childSnapshot.key ?: "",
//                                isMe = isMessageSentByMe
//                            )
//                        } catch (e: Exception) {
//                            Napier.e("Failed to deserialize message object structure: ${e.message}")
//                            null
//                        }
//                    }.sortedBy { it.timestamp }
//                }
//                .collect { fetchedMessages ->
//                    dbMessages = fetchedMessages
//                }
//        } catch (e: Exception) {
//            Napier.e("Message websocket stream connection lost: ${e.message}")
//        }
//    }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(Color(0xFFECE5DD))
//    ) {
//        // TOP HEADER BAR
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .background(Color(0xFF075E54))
//                .padding(horizontal = 16.dp, vertical = 14.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Icon(
//                imageVector = Icons.Default.ArrowBack,
//                contentDescription = null,
//                tint = Color.White,
//                modifier = Modifier.clickable { onBack() }
//            )
//
//            Spacer(modifier = Modifier.width(12.dp))
//
//            Box(
//                modifier = Modifier
//                    .size(45.dp)
//                    .clip(CircleShape)
//                    .background(avatarColor),
//                contentAlignment = Alignment.Center
//            ) {
//                Text(
//                    text = chat.name.firstOrNull()?.toString() ?: "?",
//                    color = Color.White,
//                    fontWeight = FontWeight.Bold
//                )
//            }
//
//            Spacer(modifier = Modifier.width(12.dp))
//
//            Text(
//                text = chat.name,
//                color = Color.White,
//                fontWeight = FontWeight.Bold,
//                fontSize = 18.sp
//            )
//        }
//
//        // REALTIME CHAT CONVERSATION BUBBLES LIST
//        LazyColumn(
//            state = listState, // 👈 Hooks list view control state engine
//            modifier = Modifier
//                .weight(1f)
//                .padding(10.dp),
//            verticalArrangement = Arrangement.Top
//        ) {
//            items(dbMessages) { msg ->
//                Box(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(vertical = 4.dp),
//                    contentAlignment = if (msg.isMe) Alignment.CenterEnd else Alignment.CenterStart
//                ) {
//                    Card(
//                        colors = CardDefaults.cardColors(
//                            containerColor = if (msg.isMe) Color(0xFFDCF8C6) else Color.White
//                        ),
//                        shape = RoundedCornerShape(14.dp)
//                    ) {
//                        Text(
//                            text = msg.text,
//                            modifier = Modifier.padding(12.dp),
//                            color = Color.Black
//                        )
//                    }
//                }
//            }
//        }
//
//        // PERSISTENT TEXT INPUT AREA
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(10.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            OutlinedTextField(
//                value = messageText,
//                onValueChange = { messageText = it },
//                modifier = Modifier.weight(1f),
//                placeholder = { Text("Type message...") },
//                shape = RoundedCornerShape(30.dp),
//                colors = OutlinedTextFieldDefaults.colors(
//                    focusedContainerColor = Color.White,
//                    unfocusedContainerColor = Color.White
//                )
//            )
//
//            Spacer(modifier = Modifier.width(8.dp))
//
//            FloatingActionButton(
//                onClick = {
//                    if (messageText.isNotBlank() && currentUserId.isNotEmpty()) {
//                        val outgoingText = messageText
//                        messageText = ""
//
//                        scope.launch {
//                            try {
//                                val messageRef = Firebase.database(databaseUrl)
//                                    .reference("chats_messages")
//                                    .child(roomId)
//                                    .push()
//
//                                val newMessage = FirebaseMessageModel(
//                                    id = messageRef.key ?: "",
//                                    text = outgoingText,
//                                    senderId = currentUserId,
//                                    isMe = true,
//                                    // ✅ FIXED: Standard global timestamp epoch tracking
//                                    timestamp = TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds
//                                )
//
//                                messageRef.setValue(newMessage)
//                            } catch (e: Exception) {
//                                Napier.e("Transmission failed writing message record: ${e.message}")
//                            }
//                        }
//                    }
//                },
//                containerColor = Color(0xFF075E54)
//            ) {
//                Icon(
//                    imageVector = Icons.Default.Send,
//                    contentDescription = null,
//                    tint = Color.White
//                )
//            }
//        }
//    }
//}

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

//    LaunchedEffect(roomId) {
//        try {
//            Firebase.database(databaseUrl)
//                .reference("chats_messages")
//                .child(roomId)
//                .valueEvents
//                .mapNotNull { dataSnapshot ->
//                    dataSnapshot.children.mapNotNull { childSnapshot ->
//                        try {
//                            val msg = childSnapshot.value<FirebaseMessageModel>()
//                            val isMessageSentByMe = msg.senderId == currentUserId
//
//                            msg.copy(
//                                id = childSnapshot.key ?: "",
//                                isMe = isMessageSentByMe
//                            )
//                        } catch (e: Exception) {
//                            Napier.e("Failed to deserialize message object structure: ${e.message}")
//                            null
//                        }
//                    }.sortedBy { it.timestamp }
//                }
//                .collect { fetchedMessages ->
//                    dbMessages = fetchedMessages
//                }
//        } catch (e: Exception) {
//            Napier.e("Message websocket stream connection lost: ${e.message}")
//        }
//    }


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
            .background(Color(0xFFECE5DD))
    ) {
        // TOP HEADER BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF075E54))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null,
                tint = Color.White,
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
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (msg.isMe) Color(0xFFDCF8C6) else Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = msg.text,
                            modifier = Modifier.padding(12.dp),
                            color = Color.Black
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
                placeholder = { Text("Type message...") },
                shape = RoundedCornerShape(30.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
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
                containerColor = Color(0xFF075E54)
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


fun clock_timestamp(): Long = TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds