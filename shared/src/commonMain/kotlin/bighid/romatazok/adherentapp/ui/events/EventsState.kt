package bighid.romatazok.adherentapp.ui.events

import bighid.romatazok.adherentapp.ui.receipt.ReceiptView

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
    /** The ticket already bought, with its receipt. */
    val ticket: ReceiptView? = null,
    /** For the calendar of the phone, in milliseconds since 1970. */
    val startsAt: Long,
    val endsAt: Long,
)

/** Fictitious data, until the screen is plugged to the server. */
val SampleEvents =
    listOf(
        UpcomingEvent(
            "e1",
            "Afterwork du Plateau",
            "14",
            "nov.",
            "Vendredi 14 novembre",
            "19 h – 23 h",
            "Rooftop, Cocody",
            "Cocody, Abidjan",
            "Un afterwork détendu sur le toit, pour se retrouver après la semaine et rencontrer les nouveaux adhérents.",
            "Tenue décontractée chic. Parking disponible.",
            "5 000 F",
            membersOnly = false,
            ticket = ReceiptView("R-2026-00451", "Billet · Afterwork du Plateau", "5 000 F", "3 novembre 2026 à 12:20", "Wave"),
            startsAt = 1_794_682_800_000,
            endsAt = 1_794_697_200_000,
        ),
        UpcomingEvent(
            "e2",
            "Dîner des adhérents",
            "22",
            "nov.",
            "Samedi 22 novembre",
            "20 h – 23 h 30",
            "Zone 4, Marcory",
            "Zone 4, Marcory, Abidjan",
            "Le dîner de saison, réservé aux adhérents, autour d'un menu de saison.",
            null,
            "15 000 F",
            membersOnly = true,
            startsAt = 1_795_377_600_000,
            endsAt = 1_795_390_200_000,
        ),
        UpcomingEvent(
            "e3",
            "Forum de rentrée",
            "6",
            "déc.",
            "Samedi 6 décembre",
            "16 h – 19 h",
            "Plateau",
            "Plateau, Abidjan",
            "Rencontres et ateliers pour présenter les projets du club.",
            null,
            "Gratuit",
            membersOnly = false,
            startsAt = 1_796_572_800_000,
            endsAt = 1_796_583_600_000,
        ),
        UpcomingEvent(
            "e4",
            "Gala de fin d'année",
            "13",
            "déc.",
            "Samedi 13 décembre",
            "21 h – 2 h",
            "Sofitel, Plateau",
            "Sofitel Hôtel Ivoire, Abidjan",
            "La grande soirée de fin d'année.",
            "Tenue de soirée exigée.",
            "25 000 F",
            membersOnly = false,
            onlineSalesOpen = false,
            startsAt = 1_797_195_600_000,
            endsAt = 1_797_213_600_000,
        ),
    )
