package bighid.romatazok.adherentapp.ui

import androidx.compose.runtime.Composable

/** The iPhone calendar comes with the first build on an iPhone; until then, no calendar button. */
@Composable
actual fun rememberAddToCalendar(): ((CalendarEntry) -> Unit)? = null
