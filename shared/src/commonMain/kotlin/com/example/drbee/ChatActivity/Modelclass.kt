package com.example.drbee.ChatActivity

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class FirebaseChatModel(
    val id           : String = "",
    val name         : String = "",
    val email        : String = "",
    val message      : String = "",
    val time         : String = "",
    val colorHex     : String = "#6C63FF",
    val profileImage : String = ""   // ✅ base64 image string from Firebase
)

@Serializable
data class FirebaseMessageModel(
    val id       : String = "",
    val text     : String = "",
    val senderId : String = "",
    @Transient
    val isMe     : Boolean = false
)

data class ChatModel(
    val name    : String,
    val message : String,
    val time    : String,
    val color   : Color
)

data class MessageModel(
    val text : String,
    val isMe : Boolean
)