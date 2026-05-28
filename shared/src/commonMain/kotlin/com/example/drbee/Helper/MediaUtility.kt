// commonMain/kotlin/com/example/drbee/ImagePicker.kt
package com.example.drbee

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

expect fun decodeBase64ToImageBitmap(base64: String): ImageBitmap?

expect class ImagePickerLauncher {
    fun launch()
}

@Composable
expect fun rememberImagePickerLauncher(onResult: (base64: String) -> Unit): ImagePickerLauncher

@Composable
expect fun rememberCameraLauncher(onResult: (base64: String) -> Unit): ImagePickerLauncher

expect fun shareReferralLink(userId: String)

expect fun currentTimeMillis(): Long