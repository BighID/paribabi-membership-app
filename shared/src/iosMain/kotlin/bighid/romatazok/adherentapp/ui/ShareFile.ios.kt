@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package bighid.romatazok.adherentapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.writeToURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

/** Written in the temporary folder, then the share sheet of iOS over the app. */
@Composable
actual fun rememberShareFile(): ((content: ByteArray, fileName: String, mimeType: String) -> Unit)? = remember {
    { content, fileName, _ ->
        val url = NSURL.fileURLWithPath(NSTemporaryDirectory() + fileName)
        val data = content.usePinned { NSData.create(bytes = it.addressOf(0), length = content.size.toULong()) }
        data.writeToURL(url, atomically = true)
        val sheet = UIActivityViewController(listOf(url), null)
        UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(sheet, animated = true, completion = null)
    }
}
