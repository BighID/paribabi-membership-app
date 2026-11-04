package bighid.romatazok.adherentapp.ui.activity

import bighid.romatazok.adherentapp.ui.home.ActivityKind
import bighid.romatazok.adherentapp.ui.home.ActivityRow
import bighid.romatazok.adherentapp.ui.home.Target

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

/** Fictitious history, most recent first, until the screen is plugged to the server. */
val SampleActivity =
    listOf(
        ActivityRow("a1", ActivityKind.PAYMENT, "Entrée à la Soirée des adhérents", "Dim. 1 nov. · 21:12", "5 000 F", Target.Receipt("r1")),
        ActivityRow("a2", ActivityKind.MEMBERSHIP, "Ton adhésion est active jusqu'au 31 août 2027", "Dim. 1 nov. · 20:58", "40 000 F", Target.Receipt("r2")),
        ActivityRow("a3", ActivityKind.REMINDER, "Rappel : le Dîner des adhérents a lieu demain soir à Marcory, pense à ta carte", "Mar. 21 oct. · 10:00", target = Target.Event("e2")),
        ActivityRow("a4", ActivityKind.APPLICATION, "Ta candidature est acceptée", "Lun. 20 oct. · 18:31", target = Target.Membership),
        ActivityRow("a5", ActivityKind.APPLICATION, "Candidature reçue", "Sam. 18 oct. · 09:05", target = Target.Membership),
        ActivityRow("a6", ActivityKind.REMINDER, "Nouvel événement : Afterwork du Plateau, vendredi 14 novembre", "Ven. 17 oct. · 12:00", target = Target.Event("e1")),
        ActivityRow("a7", ActivityKind.PAYMENT, "Billet pour le Forum de l'entrepreneuriat", "Jeu. 16 oct. · 19:44", "Gratuit", Target.Receipt("r3")),
        ActivityRow("a8", ActivityKind.REMINDER, "Rappel : le Forum de l'entrepreneuriat commence samedi à 9 h", "Mer. 15 oct. · 10:00", target = Target.Event("e3")),
        ActivityRow("a9", ActivityKind.MEMBERSHIP, "Ton adhésion 2025-2026 se termine le 31 août", "Ven. 1 août · 10:00", target = Target.Membership),
        ActivityRow("a10", ActivityKind.PAYMENT, "Entrée au Gala de fin de saison", "Sam. 28 juin · 20:31", "15 000 F", Target.Receipt("r4")),
        ActivityRow("a11", ActivityKind.REMINDER, "Rappel : le Gala de fin de saison a lieu samedi au Plateau", "Jeu. 26 juin · 10:00", target = Target.Event("e4")),
        ActivityRow("a12", ActivityKind.PAYMENT, "Entrée à l'Afterwork de mai", "Ven. 16 mai · 19:20", "5 000 F", Target.Receipt("r5")),
        ActivityRow("a13", ActivityKind.PAYMENT, "Entrée au Dîner des adhérents de mars", "Ven. 14 mars · 20:05", "10 000 F", Target.Receipt("r6")),
        ActivityRow("a14", ActivityKind.PAYMENT, "Entrée à l'Afterwork de janvier", "Ven. 17 janv. · 19:12", "5 000 F", Target.Receipt("r7")),
        ActivityRow("a15", ActivityKind.MEMBERSHIP, "Ton adhésion est active jusqu'au 31 août 2026", "Sam. 18 oct. 2025 · 11:40", "40 000 F", Target.Receipt("r8")),
        ActivityRow("a16", ActivityKind.APPLICATION, "Ta candidature est acceptée", "Ven. 17 oct. 2025 · 17:02", target = Target.Membership),
        ActivityRow("a17", ActivityKind.APPLICATION, "Candidature reçue", "Mer. 15 oct. 2025 · 08:47", target = Target.Membership),
    )
