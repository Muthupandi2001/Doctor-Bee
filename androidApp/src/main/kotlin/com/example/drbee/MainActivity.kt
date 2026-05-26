package com.example.drbee

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

class MainActivity : ComponentActivity() {

    var deepLinkParams by mutableStateOf<DeepLinkParams?>(null)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        processIntent(intent)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.BLACK

        enableEdgeToEdge(
            statusBarStyle     = SystemBarStyle.dark("#000000".toColorInt()),
            navigationBarStyle = SystemBarStyle.dark("#000000".toColorInt())
        )

        setContent {
            var showDialog by remember { mutableStateOf(false) }
            var onImageProcessedCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }
            var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

            // Compress + encode to Base64 — Android-only, lives here in androidMain
            val processAndCompressImage: (Uri?) -> Unit = { uri ->
                if (uri != null) {
                    try {
                        val inputStream: InputStream? = contentResolver.openInputStream(uri)
                        val originalBitmap = BitmapFactory.decodeStream(inputStream)
                        originalBitmap?.let { bitmap ->
                            val outputStream = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 65, outputStream)
                            val bytes = outputStream.toByteArray()
                            val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)
                            onImageProcessedCallback?.invoke(base64String)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val galleryLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri -> processAndCompressImage(uri) }

            val cameraLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.TakePicture()
            ) { success -> if (success) processAndCompressImage(cameraImageUri) }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted && cameraImageUri != null) cameraLauncher.launch(cameraImageUri!!)
            }

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title   = { Text("Select Profile Photo") },
                    text    = { Text("Choose a source to upload your profile image.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showDialog = false
                            galleryLauncher.launch("image/*")
                        }) { Text("Gallery") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showDialog = false
                            val tempFile = File.createTempFile("avatar_capture", ".jpg", cacheDir)
                            val uri = FileProvider.getUriForFile(
                                this@MainActivity, "$packageName.fileprovider", tempFile
                            )
                            cameraImageUri = uri
                            if (ContextCompat.checkSelfPermission(
                                    this@MainActivity, Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                cameraLauncher.launch(uri)
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }) { Text("Camera") }
                    }
                )
            }

            App(
                deepLinkParams = deepLinkParams,
                onShareRequested = { userId -> shareReferralLink(this, userId) },
                onPickImageRequested = { callback ->
                    onImageProcessedCallback = callback
                    showDialog = true
                },
                // ✅ Base64 → ImageBitmap decoding lives here — android.* is fine in androidMain
                onDecodeImageRequested = { base64String, onResult ->
                    try {
                        val bytes  = Base64.decode(base64String, Base64.NO_WRAP)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        onResult(bitmap?.asImageBitmap())
                    } catch (e: Exception) {
                        e.printStackTrace()
                        onResult(null)
                    }
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        processIntent(intent)
    }

    private fun processIntent(intent: Intent?) {
        val data: Uri? = intent?.data
        if (data != null) {
            when {
                data.scheme == "drbee" && data.host == "open" -> {
                    val screen     = data.getQueryParameter("screen")
                    val referrerId = data.getQueryParameter("referrerId")
                    Log.d("DrBeeDeepLink", "screen=$screen, referrerId=$referrerId")
                    deepLinkParams = DeepLinkParams(screen, referrerId)
                    if (screen == "referral" && referrerId != null) {
                        Toast.makeText(this, "Welcome! Referred by: $referrerId", Toast.LENGTH_LONG).show()
                    }
                }
                else -> deepLinkParams = null
            }
        } else {
            deepLinkParams = null
        }
    }

    private fun shareReferralLink(context: Context, userId: String) {
        val fullUrl = "https://merry-parfait-8fe34a.netlify.app/?screen=referral&referrerId=$userId"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Join DrBee App!")
            putExtra(Intent.EXTRA_TEXT, "Hey! Join DrBee: $fullUrl")
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Link Via"))
    }

}