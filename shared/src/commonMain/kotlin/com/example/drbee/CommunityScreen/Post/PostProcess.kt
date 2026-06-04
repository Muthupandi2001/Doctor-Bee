package com.example.drbee.CommunityScreen.Post
import com.example.drbee.CommunityScreen.ModelClass.CommunityPost
import com.example.drbee.Helper.AppConfig
import com.example.drbee.Helper.toSafeInt
import com.example.drbee.currentTimeMillis
import com.example.drbee.shareReferralLink
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.database
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


 suspend fun postComment(
    postId     : String,
    authorId   : String,
    authorName : String,
    text       : String
) {
    if (postId.isBlank() || authorId.isBlank()) return
    try {
        val commentsRef = Firebase.database(AppConfig.DB_URL)
            .reference("community_comments")
            .child(postId)
        val ref       = commentsRef.push()
        val commentId = ref.key ?: return

        ref.child("id").setValue(commentId)
        ref.child("authorId").setValue(authorId)
        ref.child("authorName").setValue(authorName)
        ref.child("text").setValue(text)
        ref.child("timestamp").setValue(currentTimeMillis().toDouble()) // ← Double

        val countRef = Firebase.database(AppConfig.DB_URL)
            .reference("community_posts")
            .child(postId)
            .child("commentCount")
        val current = countRef.valueEvents.first().value.toSafeInt()
        countRef.setValue((current + 1).toDouble()) // ← Double

    } catch (e: Exception) {
        Napier.e("postComment: ${e.message}")
    }
}

 fun toggleLike(
     post             : CommunityPost,
     currentUserId    : String,
     isCurrentlyLiked : Boolean,
     scope            : CoroutineScope
) {
    if (currentUserId.isBlank() || post.id.isBlank()) return
    scope.launch {
        try {
            val db      = Firebase.database(AppConfig.DB_URL)
            val likeRef = db.reference("community_likes")
                .child(post.id)
                .child(currentUserId)

            if (isCurrentlyLiked) likeRef.removeValue()
            else                  likeRef.setValue(true)

            val newCount = if (isCurrentlyLiked) maxOf(0, post.likeCount - 1)
            else post.likeCount + 1

            db.reference("community_posts")
                .child(post.id)
                .child("likeCount")
                .setValue(newCount.toDouble()) // ← Double

        } catch (e: Exception) {
            Napier.e("toggleLike: ${e.message}")
        }
    }
}

 fun sharePost(post: CommunityPost) {
    shareReferralLink(post.id)
}

 suspend fun editPost(postId: String, newDescription: String) {
    if (postId.isBlank()) return
    try {
        Firebase.database(AppConfig.DB_URL)
            .reference("community_posts")
            .child(postId)
            .child("description")
            .setValue(newDescription)
        Napier.d("Post $postId edited successfully")
    } catch (e: Exception) {
        Napier.e("editPost error: ${e.message}")
    }
}


 suspend fun deletePost(postId: String) {
    if (postId.isBlank()) return
    try {
        val db = Firebase.database(AppConfig.DB_URL)
        // Delete the post
        db.reference("community_posts").child(postId).removeValue()
        // Delete associated likes
        db.reference("community_likes").child(postId).removeValue()
        // Delete associated comments
        db.reference("community_comments").child(postId).removeValue()
        Napier.d("Post $postId deleted successfully")
    } catch (e: Exception) {
        Napier.e("deletePost error: ${e.message}")
    }
}
