//package com.example.drbee.ChatActivity
//import androidx.compose.ui.graphics.Color
//import kotlinx.serialization.Serializable
//import kotlin.jvm.Transient
//
//@Serializable
//data class FirebaseChatModel(
//    val id: String = "",
//    val name: String = "",
//    val email: String = "",
//    val message: String = "",
//    val time: String = "",
//    val colorHex: String = "#6C63FF"
//)
//
//@Serializable
//data class FirebaseMessageModel(
//    val id: String = "",
//    val text: String = "",
//    val senderId: String = "",
//    val timestamp: Long = 0L,
//
//    // @Transient tells the serializer to ignore this local UI flag
//    // when reading/writing data keys to your Firebase JSON database node
//    @Transient
//    val isMe: Boolean = false
//)
//
//data class ChatModel(
//    val name: String,
//    val message: String,
//    val time: String,
//    val color: Color
//)
//
//data class MessageModel(
//    val text: String,
//    val isMe: Boolean
//)


package com.example.drbee.ChatActivity

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class FirebaseChatModel(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val message: String = "",
    val time: String = "",
    val colorHex: String = "#6C63FF"
)

@Serializable
data class FirebaseMessageModel(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",

    // ✅ Timestamp completely removed to fix the KMP Web crash bug

    @Transient
    val isMe: Boolean = false
)

data class ChatModel(
    val name: String,
    val message: String,
    val time: String,
    val color: Color
)

data class MessageModel(
    val text: String,
    val isMe: Boolean
)

