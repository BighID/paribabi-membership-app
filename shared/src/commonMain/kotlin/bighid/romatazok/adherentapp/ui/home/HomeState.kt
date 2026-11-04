package bighid.romatazok.adherentapp.ui.home

import androidx.compose.ui.graphics.ImageBitmap

/** What the first screen shows. */
data class HomeState(
    val firstName: String,
    val seasonName: String,
    val status: MemberStatus,
    /** The nearest event; nothing is shown without one. */
    val nextEvent: EventRow?,
    /** The 10 latest, most recent first. */
    val activity: List<ActivityRow>,
    val profile: Profile,
    val card: CardData,
)

/** What the card writes over its visual; null fields are left blank. */
data class CardData(
    val fullName: String,
    val jobTitle: String?,
    val company: String?,
    val memberNumber: String,
    /** "18/10/2025". */
    val memberSince: String,
    /** "31/08/2027". */
    val validUntil: String,
    val city: String?,
    val sector: String?,
    /** `PBCARD:<code>`, read by the entrance. */
    val qrContent: String,
    /** Null: the silhouette of the design. */
    val photo: ImageBitmap? = null,
) {
    /** What the member completed, shown at once on the card. */
    fun with(profile: Profile) =
        copy(jobTitle = profile.jobTitle, company = profile.company, city = profile.city, sector = profile.sector, photo = profile.photo)
}

sealed interface MemberStatus {
    data class Active(
        val until: String,
    ) : MemberStatus

    data class Expired(
        val since: String,
    ) : MemberStatus

    data object AwaitingPayment : MemberStatus
}

data class EventRow(
    val id: String,
    val name: String,
    /** "Ven. 14 nov. · 19 h". */
    val date: String,
    val venue: String,
    /** "Mixte", "Réservé aux adhérents", "Public". */
    val type: String,
    /** What this member pays: "5 000 F", or "Gratuit". */
    val price: String,
)

enum class ActivityKind { PAYMENT, MEMBERSHIP, REMINDER, APPLICATION }

/** One notification; [target] is what it opens. */
data class ActivityRow(
    val id: String,
    val kind: ActivityKind,
    val title: String,
    /** "Dim. 1 nov. · 21:12". */
    val time: String,
    /** "5 000 F"; null without a payment. */
    val amount: String? = null,
    val target: Target,
)

/** Where a tap leads. */
sealed interface Target {
    data class Event(
        val id: String,
    ) : Target

    data class Receipt(
        val code: String,
    ) : Target

    data object Membership : Target
}

/** What the member can complete; the name and the phone are the club's and are changed by the team. */
data class Profile(
    val fullName: String,
    val phone: String,
    val email: String?,
    val jobTitle: String?,
    val company: String?,
    val city: String?,
    val sector: String?,
    val photo: ImageBitmap? = null,
)
