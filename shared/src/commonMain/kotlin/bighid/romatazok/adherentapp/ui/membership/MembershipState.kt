package bighid.romatazok.adherentapp.ui.membership

import bighid.romatazok.adherentapp.ui.receipt.ReceiptView

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
    /** The day the membership of this season was paid. */
    val joinedOn: String,
    val endsOn: String,
    /** The membership paid for this season; null when paid by hand without a receipt. */
    val receipt: ReceiptView?,
    /** Every event of the season, as the club adds them, the nearest first. */
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
    /** "Entré à 21:12", "Billet non utilisé", "Billet acheté"; null when nothing to add. */
    val detail: String? = null,
    /** The ticket or the entry paid; null without a payment. */
    val receipt: ReceiptView? = null,
)

/** Fictitious data, until the screen is plugged to the server. */
val SampleMembership =
    MembershipState(
        seasons =
            listOf(
                SeasonView(
                    "2026-2027",
                    SeasonStatus.ACTIVE,
                    "1 sept. 2026",
                    "2 sept. 2026",
                    "31 août 2027",
                    ReceiptView("R-2026-00301", "Renouvellement PariBabi · Saison 2026-2027", "40 000 F", "2 septembre 2026 à 18:40", "Wave"),
                    listOf(
                        SeasonEvent("e1", "Afterwork du Plateau", "14", "nov.", "Vendredi · 19 h", "Rooftop, Cocody", Presence.UPCOMING, "Billet acheté",
                            ReceiptView("R-2026-00451", "Billet · Afterwork du Plateau", "5 000 F", "3 novembre 2026 à 12:20", "Wave")),
                        SeasonEvent("e2", "Soirée des adhérents", "1", "nov.", "Samedi · 20 h", "Zone 4, Marcory", Presence.PRESENT, "Entré à 21:12",
                            ReceiptView("R-2026-00430", "Entrée · Soirée des adhérents", "5 000 F", "1 novembre 2026 à 21:12", "Espèces")),
                        SeasonEvent("e3", "Afterwork d'octobre", "10", "oct.", "Samedi · 19 h", "Plateau", Presence.ABSENT, "Billet non utilisé",
                            ReceiptView("R-2026-00388", "Billet · Afterwork d'octobre", "5 000 F", "6 octobre 2026 à 09:14", "Orange Money")),
                        SeasonEvent("e4", "Soirée de rentrée", "12", "sept.", "Samedi · 20 h", "Rooftop, Cocody", Presence.PRESENT, "Entré à 20:41"),
                        SeasonEvent("e5", "Brunch de bienvenue", "6", "sept.", "Dimanche · 11 h", "Riviera", Presence.ABSENT),
                    ),
                ),
                SeasonView(
                    "2025-2026",
                    SeasonStatus.ENDED,
                    "1 sept. 2025",
                    "18 oct. 2025",
                    "31 août 2026",
                    ReceiptView("R-2025-00112", "Adhésion PariBabi · Saison 2025-2026", "40 000 F", "18 octobre 2025 à 11:05", "Orange Money"),
                    listOf(
                        SeasonEvent("p1", "Fête de l'été", "20", "juin", "Samedi · 16 h", "Assinie", Presence.PRESENT, "Entré à 16:30"),
                        SeasonEvent("p2", "Dîner de la Saint-Valentin", "14", "févr.", "Samedi · 20 h", "Zone 4, Marcory", Presence.PRESENT, "Entré à 20:05"),
                        SeasonEvent("p3", "Gala de fin d'année", "13", "déc.", "Samedi · 21 h", "Sofitel, Plateau", Presence.ABSENT),
                        SeasonEvent("p4", "Soirée des nouveaux", "25", "oct.", "Samedi · 19 h", "Cocody", Presence.PRESENT, "Entré à 19:22"),
                    ),
                ),
            ),
    )
