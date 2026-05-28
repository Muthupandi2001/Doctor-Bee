package com.example.drbee.CommunityScreen

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.example.drbee.CommunityScreen.Comment.CommentBottomSheet
import com.example.drbee.CommunityScreen.ModelClass.CommunityPost
import com.example.drbee.CommunityScreen.Post.deletePost
import com.example.drbee.CommunityScreen.Post.editPost
import com.example.drbee.CommunityScreen.Post.sharePost
import com.example.drbee.CommunityScreen.Post.toggleLike
import com.example.drbee.Helper.DB_URL
import com.example.drbee.Helper.PAGE_SIZE
import com.example.drbee.Helper.SessionManager
import com.example.drbee.Helper.formatTimestamp
import com.example.drbee.Helper.toSafeInt
import com.example.drbee.Helper.toSafeLong
import com.example.drbee.NotificationService
import com.example.drbee.ProfileScreen.WonderBeeTheme
import com.example.drbee.currentTimeMillis
import com.example.drbee.decodeBase64ToImageBitmap
import com.example.drbee.rememberCameraLauncher
import com.example.drbee.rememberImagePickerLauncher
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.database.database
import io.github.aakira.napier.Napier
import io.github.alexzhirkevich.compottie.assets.ImageRepresentable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen() {

    val allPosts = remember { mutableStateListOf<CommunityPost>() }
    val displayedPosts = remember { mutableStateListOf<CommunityPost>() }
    var isLoadingMore by remember { mutableStateOf(false) }

    var showUploadDialog by remember { mutableStateOf(false) }
    var uploadWithCamera by remember { mutableStateOf(false) }
    var launchCamera by remember { mutableStateOf(false) }
    var launchgallery by remember { mutableStateOf(false) }
    var selectedPostForComment by remember { mutableStateOf<CommunityPost?>(null) }

    val scope = rememberCoroutineScope()

    var currentUserId by remember { mutableStateOf("") }
    var currentUserName by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= displayedPosts.size - 3 && displayedPosts.size < allPosts.size
        }
    }

    val gradient = WonderBeeTheme.extendedDesign.primaryGradientBrush


    fun loadMore() {
        if (isLoadingMore || displayedPosts.size >= allPosts.size) return
        isLoadingMore = true
        displayedPosts.addAll(allPosts.drop(displayedPosts.size).take(PAGE_SIZE))
        isLoadingMore = false
    }


    LaunchedEffect(Unit) {
        currentUserId = when {
            Firebase.auth.currentUser?.uid?.isNotBlank() == true ->
                Firebase.auth.currentUser?.uid ?: ""

            SessionManager.savedUserId.isNotBlank() ->
                SessionManager.savedUserId

            else -> ""
        }
        if (currentUserId.isNotBlank()) {
            runCatching {
                val snap = Firebase.database(DB_URL)
                    .reference("users")
                    .child(currentUserId)
                    .child("name")
                    .valueEvents.first()
                currentUserName = snap.value as? String ?: "Anonymous"
            }
        }
    }



    LaunchedEffect(Unit) {
        try {
            Firebase.database(DB_URL)
                .reference("community_posts")
                .valueEvents
                .collect { snapshot ->
                    val fetched = snapshot.children.mapNotNull { child ->
                        try {
                            val key = child.key ?: return@mapNotNull null
                            CommunityPost(
                                id = key,
                                authorId = child.child("authorId").value as? String ?: "",
                                authorName = child.child("authorName").value as? String ?: "",
                                authorAvatar = child.child("authorAvatar").value as? String,
                                description = child.child("description").value as? String ?: "",
                                imageBase64 = child.child("imageBase64").value as? String,
                                likeCount = child.child("likeCount").value.toSafeInt(),
                                commentCount = child.child("commentCount").value.toSafeInt(),
                                timestamp = child.child("timestamp").value.toSafeLong()
                            )
                        } catch (e: Exception) {
                            Napier.e("Community parse error: ${e.message}")
                            null
                        }
                    }.sortedByDescending { it.timestamp }

                    Napier.d("Fetched ${fetched.size} posts")
                    allPosts.clear()
                    allPosts.addAll(fetched)

                    val pageSize = maxOf(PAGE_SIZE, displayedPosts.size)
                    displayedPosts.clear()
                    displayedPosts.addAll(allPosts.take(pageSize))
                }
        } catch (e: Exception) {
            Napier.e("Community stream error: ${e.message}")
        }
    }


    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) loadMore() }


    var imageBase64 by remember { mutableStateOf("") }
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    val onImagePicked: (String) -> Unit = { base64 ->
        if (base64.isNotEmpty()) {
            imageBase64 = base64
            imageBitmap = decodeBase64ToImageBitmap(base64)
            showUploadDialog = true
        }
    }

    val galleryPicker = rememberImagePickerLauncher(onImagePicked)
    val cameraPicker = rememberCameraLauncher(onImagePicked)  // ← uses rememberCameraLauncher


    Box(modifier = Modifier.fillMaxSize()) {

//        Scaffold(
//            containerColor = WonderBeeTheme.extendedDesign.surfaceBackground,
//        )
//        { padding ->
//        }

        Column {
            CommunityTopBar(gradient = gradient)

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(bottom = 20.dp, top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            )
            {
                if (displayedPosts.isEmpty()) {
                    item { CommunityEmptyState() }
                }

                items(items = displayedPosts, key = { it.id }) { post ->
                    CommunityPostCard(
                        post = post,
                        currentUserId = currentUserId,
                        onLike = { isLiked -> toggleLike(post, currentUserId, isLiked, scope) },
                        onComment = { selectedPostForComment = post },
                        onShare = { sharePost(post) }
                    )
                }

                if (isLoadingMore || displayedPosts.size < allPosts.size) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = WonderBeeTheme.materialScheme.primary,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.5.dp
                            )
                        }
                    }
                }
            }
        }

        CommunityFab(
            gradient = gradient,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp),
            onPickCamera = { cameraPicker.launch(); showUploadDialog = false },
            onPickGallery = { galleryPicker.launch(); showUploadDialog = false }
        )
    }


//    LaunchedEffect(launchCamera,launchgallery) {
//        if (launchCamera) cameraPicker.launch()
//        else if(launchgallery)galleryPicker.launch()
//    }

    if (showUploadDialog) {
        imageBitmap?.let {
            UploadPostDialog(
                imageBitmap = it,
                imageBase64 = imageBase64,
                currentUserId = currentUserId,
                currentUserName = currentUserName,
                onDismiss = { showUploadDialog = false },
                onPosted = { showUploadDialog = false }
            )
        }
    }

    selectedPostForComment?.let { post ->
        CommentBottomSheet(
            post = post,
            currentUserId = currentUserId,
            currentUserName = currentUserName,
            onDismiss = { selectedPostForComment = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommunityTopBar(gradient: Brush) {

    Row(
        modifier = Modifier.fillMaxWidth()
            .background(gradient)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                "Community",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                "Stay updated",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 11.sp
            )
        }
    }

//    TopAppBar(
//        title = {
//        },
//        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
//        modifier = Modifier.background(gradient)
//    )
}

@Composable
fun CommunityPostCard(
    post: CommunityPost,
    currentUserId: String,
    onLike: (Boolean) -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit
) {
    var isLiked by remember(post.id) { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val avatarBitmap = remember(post.authorAvatar) {
        post.authorAvatar?.takeIf { it.isNotEmpty() }?.let { decodeBase64ToImageBitmap(it) }
    }
    val postBitmap = remember(post.imageBase64) {
        post.imageBase64?.takeIf { it.isNotEmpty() }?.let { decodeBase64ToImageBitmap(it) }
    }
    val timeStr = remember(post.timestamp) { formatTimestamp(post.timestamp) }
    val gradient = WonderBeeTheme.extendedDesign.primaryGradientBrush

    LaunchedEffect(post.id, currentUserId) {
        if (currentUserId.isBlank() || post.id.isBlank()) return@LaunchedEffect
        try {
            Firebase.database(DB_URL)
                .reference("community_likes")
                .child(post.id)
                .child(currentUserId)
                .valueEvents
                .collect { snap ->
                    isLiked = snap.value == true || snap.value == 1L || snap.value == 1.0
                }
        } catch (e: Exception) {
            Napier.e("Like state read: ${e.message}")
        }
    }


    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = WonderBeeTheme.materialScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    )
    {
        Column(modifier = Modifier.fillMaxWidth()) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .border(2.dp, gradient, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarBitmap != null) {
                        Image(
                            bitmap = avatarBitmap,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(brush = gradient, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = post.authorName.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.authorName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = WonderBeeTheme.materialScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = WonderBeeTheme.materialScheme.onSurface.copy(alpha = 0.45f)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = timeStr,
                            fontSize = 12.sp,
                            color = WonderBeeTheme.materialScheme.onSurface.copy(alpha = 0.45f)
                        )
                    }
                }

                if (post.authorId == currentUserId) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = WonderBeeTheme.materialScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(WonderBeeTheme.materialScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = WonderBeeTheme.materialScheme.onSurface,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Edit",
                                            color = WonderBeeTheme.materialScheme.onSurface,
                                            fontSize = 14.sp
                                        )
                                    }
                                },
                                onClick = { showMenu = false; showEditDialog = true }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = Color(0xFFE53935),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Delete",
                                            color = Color(0xFFE53935),
                                            fontSize = 14.sp
                                        )
                                    }
                                },
                                onClick = { showMenu = false; showDeleteDialog = true }
                            )
                        }
                    }
                }
            }

            // Description
            if (post.description.isNotBlank()) {
                Text(
                    text = post.description,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontSize = 14.sp,
                    color = WonderBeeTheme.materialScheme.onSurface.copy(alpha = 0.85f),
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(10.dp))
            }

            // Post image
            if (post.imageBase64?.isNotEmpty() == true) {
                if (postBitmap != null) {
                    Image(
                        bitmap = postBitmap,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = 320.dp)
                            .clip(RoundedCornerShape(0.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(WonderBeeTheme.materialScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = WonderBeeTheme.materialScheme.primary,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            Divider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                color = WonderBeeTheme.materialScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.8.dp
            )

            // Action row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PostActionButton(
                    icon = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    label = "${post.likeCount}",
                    tint = if (isLiked) Color(0xFFE53935)
                    else WonderBeeTheme.materialScheme.onSurface.copy(alpha = 0.5f),
                    onClick = { onLike(isLiked) }
                )
                PostActionButton(
                    icon = Icons.Outlined.Comment,
                    label = "${post.commentCount}",
                    tint = WonderBeeTheme.materialScheme.onSurface.copy(alpha = 0.5f),
                    onClick = onComment
                )
                PostActionButton(
                    icon = Icons.Outlined.Share,
                    label = "Share",
                    tint = WonderBeeTheme.materialScheme.onSurface.copy(alpha = 0.5f),
                    onClick = onShare
                )
            }

            Spacer(Modifier.height(6.dp))
        }
    }


    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = WonderBeeTheme.materialScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Delete Post",
                    fontWeight = FontWeight.Bold,
                    color = WonderBeeTheme.materialScheme.onSurface
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete this post? This cannot be undone.",
                    color = WonderBeeTheme.materialScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(Color(0xFFE53935), RoundedCornerShape(12.dp))
                        .clickable {
                            showDeleteDialog = false
                            scope.launch { deletePost(post.id) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Delete",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(
                        "Cancel",
                        color = WonderBeeTheme.materialScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        )
    }

    if (showEditDialog) {
        EditPostDialog(
            post = post,
            onDismiss = { showEditDialog = false },
            onSaved = { showEditDialog = false }
        )
    }


}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPostDialog(
    post: CommunityPost,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var description by remember { mutableStateOf(post.description) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val gradient = WonderBeeTheme.extendedDesign.primaryGradientBrush

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WonderBeeTheme.materialScheme.surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(brush = gradient, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "Edit Post",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = WonderBeeTheme.materialScheme.onSurface
                )
            }
        },
        text = {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Edit your post…",
                        color = WonderBeeTheme.materialScheme.onSurface.copy(alpha = 0.4f)
                    )
                },
                minLines = 3,
                maxLines = 8,
                shape = RoundedCornerShape(16.dp),
                textStyle = LocalTextStyle.current.copy(
                    color = WonderBeeTheme.materialScheme.onSurface
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = WonderBeeTheme.extendedDesign.surfaceBackground,
                    unfocusedContainerColor = WonderBeeTheme.extendedDesign.surfaceBackground,
                    focusedBorderColor = WonderBeeTheme.extendedDesign.inputFocusedBorderColor,
                    unfocusedBorderColor = WonderBeeTheme.extendedDesign.inputUnfocusedBorderColor,
                    cursorColor = WonderBeeTheme.materialScheme.primary
                )
            )
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(brush = gradient, shape = RoundedCornerShape(14.dp))
                    .clickable(enabled = !isSaving && description.isNotBlank()) {
                        if (isSaving) return@clickable
                        isSaving = true
                        scope.launch {
                            editPost(postId = post.id, newDescription = description.trim())
                            isSaving = false
                            onSaved()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Save Changes",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = WonderBeeTheme.materialScheme.onSurface.copy(alpha = 0.5f))
            }
        }
    )
}


@Composable
private fun RowScope.PostActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick, modifier = Modifier.weight(1f)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = tint, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CommunityFab(
    gradient: Brush,
    modifier: Modifier = Modifier,
    onPickCamera: () -> Unit,
    onPickGallery: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.End,
        modifier = modifier
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            Column(horizontalAlignment = Alignment.End) {
                FabMiniOption(
                    gradient = gradient,
                    icon = Icons.Default.PhotoCamera,
                    label = "Camera",
                    onClick = { expanded = false; onPickCamera() }
                )
                Spacer(Modifier.height(10.dp))
                FabMiniOption(
                    gradient = gradient,
                    icon = Icons.Default.PhotoLibrary,
                    label = "Gallery",
                    onClick = { expanded = false; onPickGallery() }
                )
                Spacer(Modifier.height(14.dp))
            }
        }

        Box(
            modifier = Modifier
                .size(58.dp)
                .background(brush = gradient, shape = CircleShape)
                .clickable { expanded = !expanded },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.Close else Icons.Default.Add,
                contentDescription = "Create post",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun FabMiniOption(
    gradient: Brush,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onClick() }
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = WonderBeeTheme.materialScheme.surface),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = WonderBeeTheme.materialScheme.onSurface
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(brush = gradient, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UploadPostDialog(
    imageBase64: String,
    imageBitmap: ImageBitmap,
    currentUserId: String,
    currentUserName: String,
    onDismiss: () -> Unit,
    onPosted: () -> Unit
) {


    var description by remember { mutableStateOf("") }

    var isPosting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val gradient = WonderBeeTheme.extendedDesign.primaryGradientBrush
    val notifService = remember { NotificationService() }


    var authorAvatar by remember { mutableStateOf("") }
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotBlank()) {
            runCatching {
                val snap = Firebase.database(DB_URL)
                    .reference("users")
                    .child(currentUserId)
                    .child("profileImage")
                    .valueEvents.first()
                authorAvatar = snap.value as? String ?: ""
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WonderBeeTheme.materialScheme.surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(brush = gradient, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "New Post", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    color = WonderBeeTheme.materialScheme.onSurface
                )
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "What's on your mind?",
                            color = WonderBeeTheme.materialScheme.onSurface.copy(alpha = 0.4f)
                        )
                    },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(16.dp),
                    textStyle = LocalTextStyle.current.copy(color = WonderBeeTheme.materialScheme.onSurface),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = WonderBeeTheme.extendedDesign.surfaceBackground,
                        unfocusedContainerColor = WonderBeeTheme.extendedDesign.surfaceBackground,
                        focusedBorderColor = WonderBeeTheme.extendedDesign.inputFocusedBorderColor,
                        unfocusedBorderColor = WonderBeeTheme.extendedDesign.inputUnfocusedBorderColor,
                        cursorColor = WonderBeeTheme.materialScheme.primary
                    )
                )

                Spacer(Modifier.height(12.dp))

                if (imageBitmap != null) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            bitmap = imageBitmap!!,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { /*imageBase64 = ""; imageBitmap = null*/ },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(28.dp)
                                .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Close, null, tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                } else {
                    // ✅ Show both options inline — no system chooser
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Camera button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .border(1.5.dp, gradient, RoundedCornerShape(16.dp))
                                .clickable { /*cameraPicker.launch() */ },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    modifier = Modifier.size(26.dp),
                                    tint = WonderBeeTheme.materialScheme.primary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Camera", fontSize = 12.sp,
                                    color = WonderBeeTheme.materialScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        // Gallery button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .border(1.5.dp, gradient, RoundedCornerShape(16.dp))
                                .clickable { /*galleryPicker.launch()*/ },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(26.dp),
                                    tint = WonderBeeTheme.materialScheme.primary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Gallery", fontSize = 12.sp,
                                    color = WonderBeeTheme.materialScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(brush = gradient, shape = RoundedCornerShape(14.dp))
                    .clickable(enabled = !isPosting && description.isNotBlank()) {
                        if (isPosting) return@clickable
                        isPosting = true
                        scope.launch {
                            publishPost(
                                currentUserId = currentUserId,
                                authorName = currentUserName,
                                authorAvatar = authorAvatar,
                                description = description,
                                imageBase64 = imageBase64,
                                notifService = notifService
                            )
                            isPosting = false
                            onPosted()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isPosting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Send,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Post to Community", color = Color.White,
                            fontWeight = FontWeight.SemiBold, fontSize = 15.sp
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = WonderBeeTheme.materialScheme.onSurface.copy(alpha = 0.5f))
            }
        }
    )
}


@Composable
private fun CommunityEmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val gradient = WonderBeeTheme.extendedDesign.primaryGradientBrush
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(brush = gradient, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Groups,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "No posts yet",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = WonderBeeTheme.materialScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Be the first to share something!",
            fontSize = 14.sp,
            color = WonderBeeTheme.materialScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

private suspend fun publishPost(
    currentUserId: String,
    authorName: String,
    authorAvatar: String,
    description: String,
    imageBase64: String,
    notifService: NotificationService
) {
    if (currentUserId.isBlank()) return
    try {
        val db = Firebase.database(DB_URL)
        val postsRef = db.reference("community_posts")
        val postRef = postsRef.push()
        val postId = postRef.key ?: "post_${currentUserId}_${currentTimeMillis()}"

        // ✅ Write each field individually with correct types
        // This avoids JS map serialization issues entirely
        postRef.child("id").setValue(postId)
        postRef.child("authorId").setValue(currentUserId)
        postRef.child("authorName").setValue(authorName)
        postRef.child("description").setValue(description)
        postRef.child("timestamp").setValue(currentTimeMillis().toDouble()) // ← Double, not Long
        postRef.child("likeCount").setValue(0.0)                            // ← Double, not Int
        postRef.child("commentCount").setValue(0.0)                         // ← Double, not Int
        if (authorAvatar.isNotEmpty()) postRef.child("authorAvatar").setValue(authorAvatar)
        if (imageBase64.isNotEmpty()) postRef.child("imageBase64").setValue(imageBase64)

        broadcastNewPostNotification(
            senderName = authorName,
            senderId = currentUserId,
            description = description,
            notifService = notifService
        )
    } catch (e: Exception) {
        Napier.e("Publish post error: ${e.message}")
    }
}

private suspend fun broadcastNewPostNotification(
    senderName: String,
    senderId: String,
    description: String,
    notifService: NotificationService
) {
    try {
        val usersSnap = Firebase.database(DB_URL).reference("users").valueEvents.first()
        usersSnap.children.forEach { child ->
            val uid = child.key ?: return@forEach
            if (uid == senderId) return@forEach
            try {
                notifService.sendPushNotification(
                    recipientUserId = uid,
                    senderName = senderName,
                    messageText = description.take(100).ifEmpty { "📷 New post" },
                    senderId = senderId,
                    roomId = "community"
                )
            } catch (e: Exception) {
                Napier.e("Notify $uid failed: ${e.message}")
            }
        }
    } catch (e: Exception) {
        Napier.e("broadcastNewPostNotification: ${e.message}")
    }
}
