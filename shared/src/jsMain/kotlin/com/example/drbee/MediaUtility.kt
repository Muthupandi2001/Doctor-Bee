// jsMain/kotlin/com/example/drbee/ImagePicker.js.kt
package com.example.drbee

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.skia.Image
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
actual fun decodeBase64ToImageBitmap(base64: String): ImageBitmap? = try {
    val bytes = Base64.Default.decode(base64)
    Image.makeFromEncoded(bytes).toComposeImageBitmap()
} catch (e: Exception) { null }

actual class ImagePickerLauncher(private val doLaunch: () -> Unit) {
    actual fun launch() = doLaunch()
}

@Composable
actual fun rememberImagePickerLauncher(onResult: (base64: String) -> Unit): ImagePickerLauncher {
    return remember {
        ImagePickerLauncher {
            val input = document.createElement("input")
            input.setAttribute("type", "file")
            input.setAttribute("accept", "image/*")
            input.addEventListener("change") { event ->
                val file = event.target.asDynamic().files[0] ?: return@addEventListener
                val reader = js("new FileReader()").unsafeCast<dynamic>()
                reader.onload = { e: dynamic ->
                    val dataUrl = e.target.result.unsafeCast<String>()
                    val base64  = dataUrl.substringAfter(",")
                    onResult(base64)
                }
                reader.readAsDataURL(file)
            }
            input.asDynamic().click()
        }
    }
}

actual fun shareReferralLink(userId: String) {
    val url  = "https://merry-parfait-8fe34a.netlify.app/?screen=referral&referrerId=$userId"
    val text = "Hey! Join DrBee: $url"

    // ✅ Build the share data object explicitly — no js() literal with captured vars
    val shareData = js("({})").unsafeCast<dynamic>()
    shareData.title = "Join DrBee App!"
    shareData.text  = text
    shareData.url   = url

    if (js("typeof navigator.share !== 'undefined'").unsafeCast<Boolean>()) {
        window.navigator.asDynamic().share(shareData)
    } else {
        window.navigator.clipboard.writeText(text)
        window.alert("Invite link copied to clipboard!")
    }
}