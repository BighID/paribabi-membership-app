package bighid.romatazok.adherentapp.ui.events

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap

/** An event to come, as this member sees it. */
data class UpcomingEvent(
    val id: String,
    val name: String,
    /** "14". */
    val day: String,
    /** "nov.". */
    val month: String,
    /** "Vendredi 14 novembre". */
    val date: String,
    /** "19 h – 23 h". */
    val time: String,
    val venue: String,
    /** Searched in the maps of the phone. */
    val address: String,
    val description: String?,
    val practicalInfo: String?,
    /** What this member pays: "5 000 F", or "Gratuit". */
    val price: String,
    val membersOnly: Boolean,
    /** The team cut the online sale: it stays possible at the entrance. */
    val onlineSalesOpen: Boolean = true,
    /** The payment of the ticket already bought, whose receipt opens. */
    val ticket: String? = null,
    /** The server has a poster for it; the colours of the card otherwise. */
    val hasPoster: Boolean = false,
    /** For the calendar of the phone, in milliseconds since 1970. */
    val startsAt: Long,
    val endsAt: Long,
)

/** The poster of an event, fetched once it is shown; null without network. Given by the app. */
val LocalPosters = staticCompositionLocalOf<suspend (String) -> ImageBitmap?> { { null } }
