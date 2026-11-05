package bighid.romatazok.adherentapp.ui

import androidx.compose.runtime.Composable

/**
 * Asks once to show notifications, when the member reaches the home for the first time: never before they know
 * why. A refusal is kept, and the question not asked again.
 */
@Composable
expect fun AskNotificationsOnce()
