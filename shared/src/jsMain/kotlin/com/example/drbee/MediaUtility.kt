// jsMain/kotlin/com/example/drbee/ImagePicker.js.kt
package com.example.drbee

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.skia.Image
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.files.Blob
import org.w3c.files.FileReader
import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.skia.Bitmap
import kotlin.coroutines.resume

actual fun decodeBase64ToImageBitmap(base64: String): ImageBitmap? = try {
    val cleanBase64 = base64.substringAfter(",")

    // Decode base64 to byte array using browser-native Window API
    val binaryString = window.atob(cleanBase64)
    val bytes = ByteArray(binaryString.length) { i -> binaryString[i].code.toByte() }

    // 1. Create the Skia Image from bytes
    val skiaImage = Image.makeFromEncoded(bytes)

    // 2. Wrap it inside a Skia Bitmap matching the source image dimensions
    val skiaBitmap = Bitmap.makeFromImage(skiaImage)

    // 3. This extension function will now correctly resolve without a receiver mismatch
    skiaBitmap.asComposeImageBitmap()
} catch (e: Exception) {
    null
}

suspend fun blobToBase64WithCompression(blob: Blob): String = suspendCancellableCoroutine<String> { continuation ->
    try {
        val reader = FileReader()
        reader.onloadend = { _ ->
            val dataUrl = reader.result as String

            // Set up an image element to compress via canvas
            val img = window.document.createElement("img") as HTMLImageElement
            img.src = dataUrl
            img.onload = {
                val canvas = window.document.createElement("canvas") as HTMLCanvasElement
                canvas.width = img.width
                canvas.height = img.height

                val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
                ctx.drawImage(img, 0.0, 0.0)

                // Compress to JPEG with 0.65 (65%) quality matching Android
                val compressedDataUrl = canvas.toDataURL("image/jpeg", 0.65)

                // Strip the prefix to match Android raw output
                val base64Raw = compressedDataUrl.substringAfter(",")
                continuation.resume(base64Raw)
            }
            img.onerror = { _, _, _, _, _ ->
                continuation.resume("")
            }
        }
        reader.onerror = { continuation.resume("") }
        reader.readAsDataURL(blob)
    } catch (e: Exception) {
        continuation.resume("")
    }
}

actual class ImagePickerLauncher(private val doLaunch: () -> Unit) {
    actual fun launch() = doLaunch()
}

@Composable
actual fun rememberImagePickerLauncher(onResult: (base64: String) -> Unit): ImagePickerLauncher {
    // Keep a stable reference to the callback wrapper
    val currentOnResult by rememberUpdatedState(onResult)

    return remember {
        ImagePickerLauncher {
            val input = document.createElement("input") as org.w3c.dom.HTMLInputElement
            input.type = "file"
            input.accept = "image/*"

            // Append input to the document body to protect it from early Garbage Collection
            document.body?.appendChild(input)

            input.addEventListener("change") { event ->
                val files = event.target.asDynamic().files
                val file = if (files != null && files.length > 0) files[0] else null

                if (file != null) {
                    val reader = FileReader()
                    reader.onload = { _ ->
                        // Safely extract the raw string directly from the reader object
                        val dataUrl = reader.result as? String ?: ""
                        val base64 = dataUrl.substringAfter(",")
                        currentOnResult(base64)

                        // Clean up the DOM element from the body once execution finishes
                        document.body?.removeChild(input)
                    }
                    reader.onerror = {
                        document.body?.removeChild(input)
                    }
                    reader.readAsDataURL(file.unsafeCast<Blob>())
                } else {
                    document.body?.removeChild(input)
                }
            }

            // Fire the click event and ensure target visibility
            input.asDynamic().click()
        }
    }
}


actual fun shareReferralLink(userId: String) {
    val url  = "https://netlify.app"
    val text = "Hey! Join DrBee: $url"

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
