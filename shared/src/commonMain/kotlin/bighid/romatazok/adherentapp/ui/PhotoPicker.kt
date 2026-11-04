package bighid.romatazok.adherentapp.ui

import androidx.compose.runtime.Composable

/** Opens the gallery of the phone; [onPicked] gets the bytes of the chosen photo. Null where it is not available yet. */
@Composable
expect fun rememberPhotoPicker(onPicked: (ByteArray) -> Unit): (() -> Unit)?
