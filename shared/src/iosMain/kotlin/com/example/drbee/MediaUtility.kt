// iosMain/kotlin/com/example/drbee/ImagePicker.ios.kt
package com.example.drbee

import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import org.jetbrains.skia.Image
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.base64EncodedStringWithOptions
import platform.UIKit.*
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock


actual fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

private var activeCameraDelegate: NSObject? = null

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



@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberCameraLauncher(onResult: (base64: String) -> Unit): ImagePickerLauncher {
    return remember {
        ImagePickerLauncher {
            val authStatus = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)

            when (authStatus) {
                AVAuthorizationStatusAuthorized -> {
                    launchIosCamera(onResult)
                }
                AVAuthorizationStatusNotDetermined -> {
                    AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                        if (granted) {
                            dispatch_async(dispatch_get_main_queue()) {
                                launchIosCamera(onResult)
                            }
                        }
                    }
                }
                else -> { }
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun launchIosCamera(onResult: (String) -> Unit) {
    if (!UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)) {
        return
    }

    val imagePicker = UIImagePickerController().apply {
        sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
    }

    val delegate = object : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
        override fun imagePickerController(
            picker: UIImagePickerController,
            didFinishPickingMediaWithInfo: Map<Any?, *> // FIXED: Changed Map<*, *> to Map<Any?, *>
        ) {
            val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
            if (image != null) {
                val resizedImage = resizeIosImage(image, 800.0)
                val data: NSData? = UIImageJPEGRepresentation(resizedImage, 0.7)

                if (data != null) {
                    val base64 = data.base64EncodedStringWithOptions(0u)
                    onResult(base64)
                }
            }

            picker.dismissViewControllerAnimated(true, null)
            activeCameraDelegate = null
        }

        override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
            picker.dismissViewControllerAnimated(true, null)
            activeCameraDelegate = null
        }
    }

    activeCameraDelegate = delegate
    imagePicker.delegate = delegate

    val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
    rootViewController?.presentViewController(imagePicker, animated = true, completion = null)
}

@OptIn(ExperimentalForeignApi::class)
private fun resizeIosImage(image: UIImage, maxSize: Double): UIImage {
    val width = image.size.useContents { width }
    val height = image.size.useContents { height }

    if (width <= maxSize && height <= maxSize) return image

    val ratio = minOf(maxSize / width, maxSize / height)
    val newWidth = width * ratio
    val newHeight = height * ratio

    UIGraphicsBeginImageContextWithOptions(CGSizeMake(newWidth, newHeight), false, 1.0)
    image.drawInRect(platform.CoreGraphics.CGRectMake(0.0, 0.0, newWidth, newHeight))
    val scaledImage = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()

    return scaledImage ?: image
}


object IosCrashBridge {
    var onLogMessage: ((String) -> Unit)? = null
    var onRecordException: ((String) -> Unit)? = null
}

actual fun logCrashMessage(message: String) {
    IosCrashBridge.onLogMessage?.invoke(message)
}

actual fun logCrashException(throwable: Throwable) {
    IosCrashBridge.onRecordException?.invoke(throwable.message ?: throwable.toString())
}

