package bighid.romatazok.adherentapp.ui

import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** The calendar app opens on the event, filled in: the person only confirms. No permission is asked. */
@Composable
actual fun rememberAddToCalendar(): ((CalendarEntry) -> Unit)? {
    val context = LocalContext.current
    return { entry ->
        val intent =
            Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, entry.title)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, entry.location)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, entry.startsAt)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, entry.endsAt)
        runCatching { context.startActivity(intent) }
    }
}
