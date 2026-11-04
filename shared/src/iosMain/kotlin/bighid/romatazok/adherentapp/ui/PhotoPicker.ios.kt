package bighid.romatazok.adherentapp.ui

import androidx.compose.runtime.Composable

/** The iPhone picker comes with the first build on an iPhone; until then, no photo button. */
@Composable
actual fun rememberPhotoPicker(onPicked: (ByteArray) -> Unit): (() -> Unit)? = null
