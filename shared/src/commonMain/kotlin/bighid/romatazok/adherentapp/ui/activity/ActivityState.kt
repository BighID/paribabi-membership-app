package bighid.romatazok.adherentapp.ui.activity

import bighid.romatazok.adherentapp.ui.home.ActivityKind

/** The four choices of the filter; the application messages go with the membership. */
enum class ActivityFilter(
    val label: String,
) {
    ALL("Tous"),
    PAYMENTS("Paiements"),
    MEMBERSHIP("Adhésion"),
    REMINDERS("Rappels"),
    ;

    fun keeps(kind: ActivityKind) =
        when (this) {
            ALL -> true
            PAYMENTS -> kind == ActivityKind.PAYMENT
            MEMBERSHIP -> kind == ActivityKind.MEMBERSHIP || kind == ActivityKind.APPLICATION
            REMINDERS -> kind == ActivityKind.REMINDER
        }
}
