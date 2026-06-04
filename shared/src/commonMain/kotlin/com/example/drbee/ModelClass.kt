package com.example.drbee


import com.example.drbee.NavHost.NavRoutes

/**
 * Carries the payload from a tapped FCM notification.
 *
 * @param senderId  UID of the user who sent the message
 * @param roomId    The OTHER user's UID (used to resolve the chat partner)
 * @param type      "chat_message" | "community"
 */
data class NotificationParams(
    val senderId : String,
    val roomId   : String,
    val type     : String
) {
    /**
     * Converts this notification payload into the correct nav route string.
     * Returns null when the type is unrecognised (caller should ignore).
     */
    fun toNavRoute(): String? = when (type) {
        "chat_message" -> NavRoutes.chatDetail(otherUserId = roomId)
        "community"    -> "community"   // handled by MainScreen tab switch
        else           -> null
    }

    val isChatMessage: Boolean get() = type == "chat_message"
    val isCommunity  : Boolean get() = type == "community"
}