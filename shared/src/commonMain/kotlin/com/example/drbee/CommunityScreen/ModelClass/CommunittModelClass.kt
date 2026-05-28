package com.example.drbee.CommunityScreen.ModelClass

import kotlinx.serialization.Serializable


@Serializable
data class CommunityPost(
    val id           : String  = "",
    val authorId     : String  = "",
    val authorName   : String  = "",
    val authorAvatar : String? = null,
    val description  : String  = "",
    val imageBase64  : String? = null,
    val likeCount    : Int     = 0,
    val commentCount : Int     = 0,
    val timestamp    : Long    = 0L,
)

@Serializable
data class CommunityComment(
    val id         : String = "",
    val authorId   : String = "",
    val authorName : String = "",
    val text       : String = "",
    val timestamp  : Long   = 0L
)
