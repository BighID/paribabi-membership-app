package bighid.romatazok.adherentapp.ui

import androidx.compose.runtime.Composable

/** No system back button on an iPhone: each page has its own back arrow. */
@Composable
actual fun SystemBack(
    enabled: Boolean,
    onBack: () -> Unit,
) = Unit
