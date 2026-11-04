package bighid.romatazok.adherentapp.ui

import androidx.compose.runtime.Composable

/** An event to put in the calendar of the phone. */
data class CalendarEntry(
    val title: String,
    val location: String,
    val startsAt: Long,
    val endsAt: Long,
)

/** Opens the calendar of the phone on a new event, filled in. Null where it is not available yet. */
@Composable
expect fun rememberAddToCalendar(): ((CalendarEntry) -> Unit)?
