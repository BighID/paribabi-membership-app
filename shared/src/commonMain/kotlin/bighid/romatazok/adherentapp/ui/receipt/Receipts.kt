package bighid.romatazok.adherentapp.ui.receipt

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * A receipt fetched when it is opened, by the payment it belongs to; null when it cannot be read (no network). Given
 * by the app to every screen that shows one.
 */
val LocalReceipts = staticCompositionLocalOf<suspend (String) -> ReceiptView?> { { null } }
