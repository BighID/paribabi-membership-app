package bighid.romatazok.adherentapp.data

import androidx.compose.ui.graphics.ImageBitmap
import bighid.romatazok.adherentapp.ui.events.UpcomingEvent
import bighid.romatazok.adherentapp.ui.home.ActivityKind
import bighid.romatazok.adherentapp.ui.home.ActivityRow
import bighid.romatazok.adherentapp.ui.home.CardData
import bighid.romatazok.adherentapp.ui.home.EventRow
import bighid.romatazok.adherentapp.ui.home.HomeState
import bighid.romatazok.adherentapp.ui.home.MemberStatus
import bighid.romatazok.adherentapp.ui.home.Profile
import bighid.romatazok.adherentapp.ui.home.Target
import bighid.romatazok.adherentapp.ui.membership.MembershipState
import bighid.romatazok.adherentapp.ui.membership.Presence
import bighid.romatazok.adherentapp.ui.membership.SeasonEvent
import bighid.romatazok.adherentapp.ui.membership.SeasonStatus
import bighid.romatazok.adherentapp.ui.membership.SeasonView
import bighid.romatazok.adherentapp.ui.receipt.ReceiptView
import org.jetbrains.compose.resources.decodeToImageBitmap

/** What the screens show, from the server's answers: the only place that knows both. */
class MemberRepository(
    private val api: MemberApi,
) {
    private val posters = mutableMapOf<String, ImageBitmap?>()

    /** The home, the activity and the events, read together. */
    suspend fun home(): Triple<HomeState, List<ActivityRow>, List<UpcomingEvent>> {
        val profile = api.profile()
        val activity = api.activity().map(::row)
        val events = api.events()
        val home =
            HomeState(
                firstName = profile.firstName.orEmpty(),
                // The server names it already: "Saison 2026-2027".
                seasonName = profile.seasonName.orEmpty(),
                status =
                    profile.validUntil?.let { until ->
                        if (profile.status == "ACTIVE") MemberStatus.Active(Formats.longDate(until)) else MemberStatus.Expired(Formats.longDate(until))
                    } ?: MemberStatus.AwaitingPayment,
                nextEvent = events.firstOrNull()?.let(::line),
                activity = activity.take(HOME_ACTIVITY),
                profile = Profile(profile.fullName, phone(profile.phone), profile.email, profile.jobTitle, profile.company, profile.city, profile.sector),
                card =
                    CardData(
                        fullName = profile.fullName,
                        jobTitle = profile.jobTitle,
                        company = profile.company,
                        memberNumber = profile.memberNumber.orEmpty(),
                        memberSince = profile.memberSince?.let(Formats::cardDate).orEmpty(),
                        validUntil = profile.validUntil?.let(Formats::cardDate).orEmpty(),
                        city = profile.city,
                        sector = profile.sector,
                        qrContent = profile.qr.orEmpty(),
                    ),
            )
        return Triple(home, activity, events.map(::event))
    }

    suspend fun membership(): MembershipState = MembershipState(api.seasons().map(::season))

    suspend fun receipt(paymentId: String): ReceiptView? = runCatching {
        val receipt = api.receipt(paymentId)
        ReceiptView(receipt.number, receipt.title, Formats.fcfa(receipt.total), Formats.receiptDate(receipt.paidAt), receipt.method)
    }.getOrNull()

    /** Kept once read: a poster does not change while the app is open. */
    suspend fun poster(eventId: String): ImageBitmap? = posters.getOrPut(eventId) { api.poster(eventId)?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() } }

    suspend fun sectors(): List<String> = runCatching { api.sectors() }.getOrDefault(emptyList())

    private fun row(activity: ActivityDto) = ActivityRow(
        activity.id,
        when (activity.kind) {
            "PAYMENT" -> ActivityKind.PAYMENT
            "MEMBERSHIP" -> ActivityKind.MEMBERSHIP
            "REMINDER" -> ActivityKind.REMINDER
            else -> ActivityKind.APPLICATION
        },
        activity.title,
        Formats.stamp(activity.at),
        activity.amount?.let(Formats::fcfa),
        when {
            activity.target == "RECEIPT" && activity.targetId != null -> Target.Receipt(activity.targetId)
            activity.target == "EVENT" && activity.targetId != null -> Target.Event(activity.targetId)
            else -> Target.Membership
        },
    )

    private fun line(event: EventDto) = EventRow(
        event.id,
        event.name,
        "${Formats.stamp(event.startsAt).substringBefore(" · ")} · ${Formats.hours(event.startsAt, null)}",
        event.venue.orEmpty(),
        when (event.access) {
            "MEMBERS_ONLY" -> "Réservé aux adhérents"
            "PUBLIC" -> "Public"
            else -> "Mixte"
        },
        price(event),
    )

    private fun event(event: EventDto) = UpcomingEvent(
        id = event.id,
        name = event.name,
        day = Formats.day(event.startsAt),
        month = Formats.month(event.startsAt),
        date = Formats.fullDate(event.startsAt),
        time = Formats.hours(event.startsAt, event.endsAt),
        venue = event.venue.orEmpty(),
        address = event.venue.orEmpty(),
        description = event.description,
        practicalInfo = event.practicalInfo,
        price = price(event),
        membersOnly = event.access == "MEMBERS_ONLY",
        onlineSalesOpen = event.onlineSalesOpen,
        ticket = event.ticketPaymentId,
        hasPoster = event.hasPoster,
        startsAt = Formats.millis(event.startsAt),
        endsAt = Formats.millis(event.endsAt ?: event.startsAt) + if (event.endsAt == null) DEFAULT_LENGTH else 0,
    )

    private fun season(season: SeasonDto) = SeasonView(
        name = season.name,
        status =
            when (season.status) {
                "ACTIVE" -> SeasonStatus.ACTIVE
                "EXPIRED" -> SeasonStatus.EXPIRED
                else -> SeasonStatus.ENDED
            },
        startsOn = Formats.shortDate(season.startsOn),
        joinedOn = season.joinedOn?.let(Formats::shortDate).orEmpty(),
        endsOn = Formats.shortDate(season.endsOn),
        receipt = season.receiptPaymentId,
        events =
            season.events.map { event ->
                val presence =
                    when (event.presence) {
                        "PRESENT" -> Presence.PRESENT
                        "UPCOMING" -> Presence.UPCOMING
                        else -> Presence.ABSENT
                    }
                SeasonEvent(
                    id = event.id,
                    name = event.name,
                    day = Formats.day(event.startsAt),
                    month = Formats.month(event.startsAt),
                    time = Formats.weekdayHour(event.startsAt),
                    venue = event.venue.orEmpty(),
                    presence = presence,
                    detail =
                        when {
                            event.arrivedAt != null -> "Entré à ${Formats.clockOf(event.arrivedAt)}"
                            event.ticketPaymentId != null && presence == Presence.UPCOMING -> "Billet acheté"
                            event.ticketPaymentId != null -> "Billet non utilisé"
                            else -> null
                        },
                    receipt = event.ticketPaymentId,
                )
            },
    )

    private fun price(event: EventDto) = if (event.free || event.price == null) "Gratuit" else Formats.fcfa(event.price)

    /** "+2250701020304" as the card writes it: "07 01 02 03 04"; another country as it is. */
    private fun phone(e164: String?): String {
        val number = e164 ?: return ""
        return if (number.startsWith("+225")) number.removePrefix("+225").chunked(2).joinToString(" ") else number
    }

    private companion object {
        const val HOME_ACTIVITY = 10

        /** An event without an end is put in the calendar for three hours. */
        const val DEFAULT_LENGTH = 3 * 60 * 60 * 1000L
    }
}
