package bighid.romatazok.adherentapp.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

/** Written in the app's cache, shared through its file provider: the other apps read it, nothing else does. */
@Composable
actual fun rememberShareFile(): ((content: ByteArray, fileName: String, mimeType: String) -> Unit)? {
    val context = LocalContext.current
    return remember(context) {
        { content, fileName, mimeType ->
            val folder = File(context.cacheDir, "shared").apply { mkdirs() }
            val file = File(folder, fileName).apply { writeBytes(content) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
            val send =
                Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            context.startActivity(Intent.createChooser(send, fileName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
