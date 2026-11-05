package bighid.romatazok.adherentapp.ui

import androidx.compose.runtime.Composable

/**
 * Hands a file to the phone's share sheet: save it, send it by WhatsApp or by email. Null when the phone cannot.
 */
@Composable
expect fun rememberShareFile(): ((content: ByteArray, fileName: String, mimeType: String) -> Unit)?
