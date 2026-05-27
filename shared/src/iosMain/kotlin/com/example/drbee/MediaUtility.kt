// iosMain/kotlin/com/example/drbee/ImagePicker.ios.kt
package com.example.drbee

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import platform.Foundation.base64EncodedStringWithOptions
import platform.UIKit.*
import platform.darwin.NSObject
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
    val delegateRef  = remember { mutableStateOf<ImagePickerDelegate?>(null) }
    val pickerRef    = remember { mutableStateOf<UIImagePickerController?>(null) }

    DisposableEffect(Unit) {
        val delegate = ImagePickerDelegate(onResult)
        val picker   = UIImagePickerController().apply {
            sourceType    = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
            allowsEditing = false
            this.delegate = delegate
        }
        delegateRef.value = delegate
        pickerRef.value   = picker
        onDispose { delegateRef.value?.invalidate() }
    }

    return remember {
        ImagePickerLauncher {
            val picker = pickerRef.value ?: return@ImagePickerLauncher
            UIApplication.sharedApplication.keyWindow
                ?.rootViewController
                ?.presentViewController(picker, animated = true, completion = null)
        }
    }
}

private class ImagePickerDelegate(
    private var onResult: ((String) -> Unit)?
) : NSObject(),
    UIImagePickerControllerDelegateProtocol,
    UINavigationControllerDelegateProtocol {

    fun invalidate() { onResult = null }

    @OptIn(ExperimentalEncodingApi::class)
    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val image  = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage ?: return
        val data   = UIImageJPEGRepresentation(image, 0.65) ?: return
        val base64 = data.base64EncodedStringWithOptions(0u)
        onResult?.invoke(base64)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
    }
}

actual fun shareReferralLink(userId: String) {
    val url  = "https://merry-parfait-8fe34a.netlify.app/?screen=referral&referrerId=$userId"
    val text = "Hey! Join DrBee: $url"

    val controller = UIActivityViewController(
        activityItems      = listOf(text),
        applicationActivities = null
    )

    UIApplication.sharedApplication
        .keyWindow
        ?.rootViewController
        ?.presentViewController(controller, animated = true, completion = null)
}