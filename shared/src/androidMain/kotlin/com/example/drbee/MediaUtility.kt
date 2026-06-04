// androidMain/kotlin/com/example/drbee/ImagePicker.android.kt
package com.example.drbee

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream
import android.app.Activity
import android.content.ContentValues
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import java.lang.ref.WeakReference
import com.google.firebase.crashlytics.FirebaseCrashlytics

actual fun decodeBase64ToImageBitmap(base64: String): ImageBitmap? = try {
    val bytes = Base64.decode(base64, Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
} catch (e: Exception) { null }

actual class ImagePickerLauncher(private val doLaunch: () -> Unit) {
    actual fun launch() = doLaunch()
}

@Composable
actual fun rememberImagePickerLauncher(onResult: (base64: String) -> Unit): ImagePickerLauncher {
    val context = LocalContext.current

    // Chooser: shows both Gallery and Camera
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        val base64 = context.uriToBase64(uri)
        if (base64.isNotEmpty()) onResult(base64)
    }

    return remember {
        ImagePickerLauncher {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            val cameraIntent  = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val chooser = Intent.createChooser(galleryIntent, "Select Image").apply {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
            }
            launcher.launch(chooser)
        }
    }
}

private fun Context.uriToBase64(uri: Uri): String = try {
    val inputStream = contentResolver.openInputStream(uri) ?: return ""
    val original    = BitmapFactory.decodeStream(inputStream)
    val out         = ByteArrayOutputStream()
    original.compress(Bitmap.CompressFormat.JPEG, 65, out)
    Base64.encodeToString(out.toByteArray(), Base64.DEFAULT)
} catch (e: Exception) { "" }

actual fun shareReferralLink(userId: String) {
    val activity = ActivityProvider.get()
    val url      = "https://merry-parfait-8fe34a.netlify.app/?screen=referral&referrerId=$userId"
    val intent   = Intent(Intent.ACTION_SEND).apply {
        type     = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Join DrBee App!")
        putExtra(Intent.EXTRA_TEXT, "Hey! Join DrBee: $url")
    }
    activity.startActivity(Intent.createChooser(intent, "Share Link Via"))
}

object ActivityProvider {
    private var activityRef: WeakReference<Activity>? = null

    fun set(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    fun clear() {
        activityRef?.clear()
        activityRef = null
    }

    fun get(): Activity = activityRef?.get()
        ?: error("ActivityProvider not initialized — call set() in MainActivity.onCreate")
}

@Composable
actual fun rememberCameraLauncher(onResult: (base64: String) -> Unit): ImagePickerLauncher {
    val context   = LocalContext.current
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handled below */ }

    // Camera capture launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (!success) return@rememberLauncherForActivityResult
        val uri = cameraUri ?: return@rememberLauncherForActivityResult
        try {
            val stream = context.contentResolver.openInputStream(uri) ?: return@rememberLauncherForActivityResult
            val bitmap = BitmapFactory.decodeStream(stream)
            stream.close()
            onResult(bitmapToBase64(bitmap))
        } catch (e: Exception) { /* ignore */ }
    }

    return remember {
        ImagePickerLauncher {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                val uri = createCameraUri(context)
                cameraUri = uri
                cameraLauncher.launch(uri)
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
}

private fun bitmapToBase64(bitmap: Bitmap): String {
    val stream = ByteArrayOutputStream()
    // Resize if too large to avoid memory issues
    val maxSize = 800
    val scaled = if (bitmap.width > maxSize || bitmap.height > maxSize) {
        val ratio = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height)
        Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt(),
            (bitmap.height * ratio).toInt(),
            true
        )
    } else bitmap
    scaled.compress(Bitmap.CompressFormat.JPEG, 70, stream)
    return Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT)
}
private fun createCameraUri(context: Context): Uri {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "community_photo_${System.currentTimeMillis()}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
    }
    return context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()


// androidMain/src/androidMain/kotlin/KmpLogger.android.kt


actual fun logCrashMessage(message: String) {
    try {
        FirebaseCrashlytics.getInstance().log(message)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

actual fun logCrashException(throwable: Throwable) {
    try {
        FirebaseCrashlytics.getInstance().recordException(throwable)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

