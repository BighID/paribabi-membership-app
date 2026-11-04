package bighid.romatazok.adherentapp.ui.membership

/** Everything "Mon adhésion" shows: one season at a time, the latest first. */
data class MembershipState(
    /** The seasons the person paid a membership for, the latest first. */
    val seasons: List<SeasonView>,
)

enum class SeasonStatus { ACTIVE, EXPIRED, ENDED }

data class SeasonView(
    /** "2026-2027". */
    val name: String,
    val status: SeasonStatus,
    /** "1 sept. 2026". */
    val startsOn: String,
    /** The day the membership of this season started. */
    val joinedOn: String,
    val endsOn: String,
    /** The payment of the membership, whose receipt opens; null when paid by hand without a receipt. */
    val receipt: String?,
    /** Every event of the season, as the club adds them, the latest first. */
    val events: List<SeasonEvent>,
)

/** Where the member stands for one event. */
enum class Presence { PRESENT, ABSENT, UPCOMING }

data class SeasonEvent(
    val id: String,
    val name: String,
    /** "14". */
    val day: String,
    /** "nov.". */
    val month: String,
    /** "Vendredi · 19 h". */
    val time: String,
    val venue: String,
    val presence: Presence,
    /** "Entré à 21:12", "Billet acheté"; null when nothing to add. */
    val detail: String? = null,
    /** The payment of the ticket, whose receipt opens; null without a payment. */
    val receipt: String? = null,
)
