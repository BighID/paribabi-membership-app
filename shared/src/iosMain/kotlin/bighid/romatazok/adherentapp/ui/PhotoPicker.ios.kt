@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package bighid.romatazok.adherentapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreFoundation.CFDataCreateMutable
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.create
import platform.ImageIO.CGImageDestinationAddImageFromSource
import platform.ImageIO.CGImageDestinationCreateWithData
import platform.ImageIO.CGImageDestinationFinalize
import platform.ImageIO.CGImageSourceCreateWithData
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

/**
 * The photo picker of iOS: no permission to ask, the person chooses one photo. A HEIC photo becomes a JPEG on the
 * phone, at most 2000 pixels a side, its metadata kept: the server does the rest, as for Android.
 */
@Composable
actual fun rememberPhotoPicker(onPicked: (ByteArray) -> Unit): (() -> Unit)? {
    val picked by rememberUpdatedState(onPicked)
    // Kept here: the picker holds its delegate weakly.
    val delegate = remember { PickerDelegate { picked(it) } }
    return remember {
        {
            val configuration =
                PHPickerConfiguration().apply {
                    filter = PHPickerFilter.imagesFilter
                    selectionLimit = 1
                }
            val picker = PHPickerViewController(configuration).apply { this.delegate = delegate }
            UIApplication.sharedApplication.keyWindow?.rootViewController?.let { root ->
                (root.presentedViewController ?: root).presentViewController(picker, animated = true, completion = null)
            }
        }
    }
}

private class PickerDelegate(
    private val onPicked: (ByteArray) -> Unit,
) : NSObject(),
    PHPickerViewControllerDelegateProtocol {
    override fun picker(
        picker: PHPickerViewController,
        didFinishPicking: List<*>,
    ) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val result = didFinishPicking.firstOrNull() as? PHPickerResult ?: return
        // Called away from the main thread: the photo is prepared there, then handed back on the main one.
        result.itemProvider.loadDataRepresentationForTypeIdentifier(IMAGE) { data, _ ->
            val jpeg = data?.let(::toJpeg) ?: return@loadDataRepresentationForTypeIdentifier
            dispatch_async(dispatch_get_main_queue()) { onPicked(jpeg) }
        }
    }
}

/** Any image the phone can read, as a JPEG with its metadata; null if it cannot be read. */
private fun toJpeg(data: NSData): ByteArray? {
    val sourceData = CFBridgingRetain(data) as CFDataRef? ?: return null
    val type = CFBridgingRetain(NSString.create(string = JPEG)) as CFStringRef?
    val options = CFBridgingRetain(mapOf(QUALITY to 0.85, MAX_PIXEL_SIZE to MAX_SIDE)) as CFDictionaryRef?
    val output = CFDataCreateMutable(null, 0)
    try {
        val source = CGImageSourceCreateWithData(sourceData, null) ?: return null
        try {
            val destination = CGImageDestinationCreateWithData(output, type, 1u, null) ?: return null
            try {
                CGImageDestinationAddImageFromSource(destination, source, 0u, options)
                if (!CGImageDestinationFinalize(destination)) return null
            } finally {
                CFRelease(destination)
            }
        } finally {
            CFRelease(source)
        }
        val length = CFDataGetLength(output).toInt()
        if (length <= 0) return null
        val bytes = CFDataGetBytePtr(output) ?: return null
        return ByteArray(length).apply { usePinned { memcpy(it.addressOf(0), bytes, length.toULong()) } }
    } finally {
        CFRelease(sourceData)
        type?.let(::CFRelease)
        options?.let(::CFRelease)
        output?.let(::CFRelease)
    }
}

private const val IMAGE = "public.image"
private const val JPEG = "public.jpeg"

// The values of kCGImageDestinationLossyCompressionQuality and kCGImageDestinationImageMaxPixelSize.
private const val QUALITY = "kCGImageDestinationLossyCompressionQuality"
private const val MAX_PIXEL_SIZE = "kCGImageDestinationImageMaxPixelSize"
private const val MAX_SIDE = 2000
