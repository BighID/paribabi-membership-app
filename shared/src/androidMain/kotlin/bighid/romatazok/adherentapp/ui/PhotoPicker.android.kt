package bighid.romatazok.adherentapp.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** The photo picker of Android: no permission to ask, the person chooses one photo. */
@Composable
actual fun rememberPhotoPicker(onPicked: (ByteArray) -> Unit): (() -> Unit)? {
    val context = LocalContext.current
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            context.contentResolver.openInputStream(uri)?.use { onPicked(it.readBytes()) }
        }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}
