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

/** Fictitious data, until the screen is plugged to the server. */
val SampleHome =
    HomeState(
        firstName = "Awa",
        seasonName = "Saison 2026-2027",
        status = MemberStatus.Active("31 août 2027"),
        nextEvent = EventRow("e1", "Afterwork du Plateau", "Ven. 14 nov. · 19 h", "Rooftop, Cocody", "Mixte", "5 000 F"),
        activity =
            listOf(
                ActivityRow("a1", ActivityKind.PAYMENT, "Entrée à la Soirée des adhérents", "Dim. 1 nov. · 21:12", "5 000 F", Target.Receipt("r1")),
                ActivityRow("a2", ActivityKind.MEMBERSHIP, "Ton adhésion est active jusqu'au 31 août 2027", "Dim. 1 nov. · 20:58", "40 000 F", Target.Receipt("r2")),
                ActivityRow("a3", ActivityKind.REMINDER, "Rappel : le Dîner des adhérents a lieu demain soir à Marcory, pense à ta carte", "Mar. 21 oct. · 10:00", target = Target.Event("e2")),
                ActivityRow("a4", ActivityKind.APPLICATION, "Ta candidature est acceptée", "Lun. 20 oct. · 18:31", target = Target.Membership),
                ActivityRow("a5", ActivityKind.APPLICATION, "Candidature reçue", "Sam. 18 oct. · 09:05", target = Target.Membership),
            ),
        profile =
            Profile(
                "Awa Koné",
                "07 01 02 03 04",
                "awa.kone@exemple.test",
                "Responsable marketing",
                "Société fictive SA",
                "Abidjan",
                "Communication, marketing et médias",
            ),
        card =
            CardData(
                fullName = "Awa Koné",
                jobTitle = "Responsable marketing",
                company = "Société fictive SA",
                memberNumber = "PB-00079",
                memberSince = "18/10/2025",
                validUntil = "31/08/2027",
                city = "Abidjan",
                sector = "Communication, marketing et médias",
                qrContent = "PBCARD:exemple-fictif-0079",
            ),
    )
