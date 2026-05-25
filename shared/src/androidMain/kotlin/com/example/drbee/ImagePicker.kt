//package com.example.drbee.ProfileScreen
//
//import android.Manifest
//import android.content.Context
//import android.content.pm.PackageManager
//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import android.net.Uri
//import android.util.Base64
//import androidx.activity.compose.rememberLauncherForActivityResult
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.material3.AlertDialog
//import androidx.compose.material3.Text
//import androidx.compose.material3.TextButton
//import androidx.compose.runtime.*
//import androidx.compose.ui.platform.LocalContext
//import androidx.core.content.ContextCompat
//import androidx.core.content.FileProvider
//import java.io.ByteArrayOutputStream
//import java.io.File
//import java.io.InputStream
//
//@Composable
//fun AndroidProfileScreenWrapper(
//    userId: String,
//    onBack: () -> Unit,
//    onShareRequested: (String) -> Unit
//) {
//    val context = LocalContext.current
//    var showDialog by remember { mutableStateOf(false) }
//    var onImageProcessedCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }
//
//    // Temporary file URI holder for Camera capture
//    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
//
//    // Helper to compress and convert file streams into clean base64 data
//    fun processAndCompressImage(uri: Uri?) {
//        if (uri == null) return
//        try {
//            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
//            val originalBitmap = BitmapFactory.decodeStream(inputStream)
//
//            originalBitmap?.let { bitmap ->
//                val outputStream = ByteArrayOutputStream()
//                // Compress to 65% JPEG quality to optimize the payload size for Firebase Database string rules
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 65, outputStream)
//                val bytes = outputStream.toByteArray()
//                val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)
//
//                // Return result to UI state
//                onImageProcessedCallback?.invoke(base64String)
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//
//    // 1. Launcher: Photo Gallery Pick
//    val galleryLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.GetContent()
//    ) { uri ->
//        processAndCompressImage(uri)
//    }
//
//    // 2. Launcher: Capture Camera Photo
//    val cameraLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.TakePicture()
//    ) { success ->
//        if (success) {
//            processAndCompressImage(cameraImageUri)
//        }
//    }
//
//    // 3. Launcher: Handle Runtime Permissions
//    val permissionLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.RequestPermission()
//    ) { isGranted ->
//        if (isGranted) {
//            cameraLauncher.launch(cameraImageUri!!)
//        }
//    }
//
//    // Trigger dialog and cache callback lambda
//    val handlePickRequest: ((String) -> Unit) -> Unit = { callback ->
//        onImageProcessedCallback = callback
//        showDialog = true
//    }
//
//    // Dialog Prompt UI
//    if (showDialog) {
//        AlertDialog(
//            onDismissRequest = { showDialog = false },
//            title = { Text("Select Profile Photo") },
//            text = { Text("Choose a source to upload your profile image.") },
//            confirmButton = {
//                TextButton(onClick = {
//                    showDialog = false
//                    galleryLauncher.launch("image/*")
//                }) { Text("Gallery") }
//            },
//            dismissButton = {
//                TextButton(onClick = {
//                    showDialog = false
//
//                    // Create secure file authority path for camera
//                    val tempFile = File.createTempFile("avatar_capture", ".jpg", context.cacheDir)
//                    val uri = FileProvider.getUriForFile(
//                        context,
//                        "${context.packageName}.fileprovider",
//                        tempFile
//                    )
//                    cameraImageUri = uri
//
//                    // Verify if system has permission granted
//                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
//                        cameraLauncher.launch(uri)
//                    } else {
//                        permissionLauncher.launch(Manifest.permission.CAMERA)
//                    }
//                }) { Text("Camera") }
//            }
//        )
//    }
//
//    // Calls the updated, architecture-clean profile layout
//    OtherProfile(
//        userId = userId,
//        onBack = onBack,
//        onShareRequested = onShareRequested,
//        onPickImageRequested = handlePickRequest
//    )
//}
