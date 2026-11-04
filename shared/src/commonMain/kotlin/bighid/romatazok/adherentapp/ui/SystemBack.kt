package bighid.romatazok.adherentapp.ui

import androidx.compose.runtime.Composable

/** The back gesture of the phone, while a page is open over the home. */
@Composable
expect fun SystemBack(
    enabled: Boolean,
    onBack: () -> Unit,
)
