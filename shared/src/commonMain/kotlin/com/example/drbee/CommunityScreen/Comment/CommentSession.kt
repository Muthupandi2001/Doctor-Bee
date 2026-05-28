package com.example.drbee.CommunityScreen.Comment

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drbee.CommunityScreen.ModelClass.CommunityComment
import com.example.drbee.CommunityScreen.ModelClass.CommunityPost

import com.example.drbee.CommunityScreen.Post.postComment
import com.example.drbee.Helper.DB_URL
import com.example.drbee.Helper.formatTimestamp
import com.example.drbee.Helper.toSafeLong
import com.example.drbee.ProfileScreen.WonderBeeTheme
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.database
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
 fun CommentBottomSheet(
    post            : CommunityPost,
    currentUserId   : String,
    currentUserName : String,
    onDismiss       : () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val comments   = remember { mutableStateListOf<CommunityComment>() }
    var inputText  by remember { mutableStateOf("") }
    val scope      = rememberCoroutineScope()
    val gradient   = WonderBeeTheme.extendedDesign.primaryGradientBrush

    LaunchedEffect(post.id) {
        try {
            Firebase.database(DB_URL)
                .reference("community_comments")
                .child(post.id)
                .valueEvents
                .collect { snapshot ->
                    val fetched = snapshot.children.mapNotNull { child ->
                        try {
                            val key = child.key ?: return@mapNotNull null
                            CommunityComment(
                                id = key,
                                authorId = child.child("authorId").value as? String ?: "",
                                authorName = child.child("authorName").value as? String ?: "",
                                text = child.child("text").value as? String ?: "",
                                timestamp = child.child("timestamp").value.toSafeLong()
                            )
                        } catch (e: Exception) { null }
                    }.sortedBy { it.timestamp }
                    comments.clear()
                    comments.addAll(fetched)
                }
        } catch (e: Exception) {
            Napier.e("Comments stream: ${e.message}")
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = WonderBeeTheme.materialScheme.surface,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(
                        WonderBeeTheme.materialScheme.outlineVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(2.dp)
                    )
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(12.dp))

            Text(
                "Comments (${comments.size})",
                fontWeight = FontWeight.Bold,
                fontSize   = 16.sp,
                color      = WonderBeeTheme.materialScheme.onSurface
            )
            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier            = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (comments.isEmpty()) {
                    item {
                        Box(
                            modifier         = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Be the first to comment!",
                                color    = WonderBeeTheme.materialScheme.onSurface.copy(alpha = 0.4f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                items(items = comments, key = { it.id }) { comment ->
                    CommentItem(comment = comment)
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value         = inputText,
                    onValueChange = { inputText = it },
                    modifier      = Modifier.weight(1f),
                    placeholder   = {
                        Text(
                            "Write a comment…",
                            color = WonderBeeTheme.materialScheme.onSurface.copy(alpha = 0.4f)
                        )
                    },
                    shape      = RoundedCornerShape(24.dp),
                    textStyle  = LocalTextStyle.current.copy(
                        color = WonderBeeTheme.materialScheme.onSurface
                    ),
                    colors     = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor   = WonderBeeTheme.extendedDesign.surfaceBackground,
                        unfocusedContainerColor = WonderBeeTheme.extendedDesign.surfaceBackground,
                        focusedBorderColor      = WonderBeeTheme.extendedDesign.inputFocusedBorderColor,
                        unfocusedBorderColor    = WonderBeeTheme.extendedDesign.inputUnfocusedBorderColor,
                        cursorColor             = WonderBeeTheme.materialScheme.primary
                    ),
                    maxLines   = 3,
                    singleLine = false
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(brush = gradient, shape = CircleShape)
                        .clickable(enabled = inputText.isNotBlank()) {
                            val text = inputText.trim()
                            if (text.isBlank()) return@clickable
                            inputText = ""
                            scope.launch {
                                postComment(
                                    postId     = post.id,
                                    authorId   = currentUserId,
                                    authorName = currentUserName,
                                    text       = text
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint               = Color.White,
                        modifier           = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentItem(comment: CommunityComment) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier         = Modifier
                .size(32.dp)
                .background(
                    brush = WonderBeeTheme.extendedDesign.primaryGradientBrush,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = comment.authorName.take(1).uppercase(),
                color      = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize   = 13.sp
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = comment.authorName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 13.sp,
                    color      = WonderBeeTheme.materialScheme.onSurface
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text     = formatTimestamp(comment.timestamp),
                    fontSize = 11.sp,
                    color    = WonderBeeTheme.materialScheme.onSurface.copy(alpha = 0.4f)
                )
            }
            Text(
                text       = comment.text,
                fontSize   = 13.sp,
                color      = WonderBeeTheme.materialScheme.onSurface.copy(alpha = 0.8f),
                lineHeight = 18.sp
            )
        }
    }
}