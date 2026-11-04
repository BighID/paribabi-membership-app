package bighid.romatazok.adherentapp.data

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** The dates and the amounts as the screens write them, in French, at the phone's own hour. */
object Formats {
    private val days = listOf("lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche")
    private val months =
        listOf("janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre")
    private val shortMonths = listOf("janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.", "août", "sept.", "oct.", "nov.", "déc.")

    fun local(instant: String): LocalDateTime = Instant.parse(instant).toLocalDateTime(TimeZone.currentSystemDefault())

    fun millis(instant: String): Long = Instant.parse(instant).toEpochMilliseconds()

    /** "5 000 F". */
    fun fcfa(amount: Long): String = amount.toString().reversed().chunked(3).joinToString(" ").reversed() + " F"

    /** "Dim. 1 nov. · 21:12". */
    fun stamp(instant: String): String = local(instant).let { "${dayName(it).take(3).cap()}. ${it.day} ${shortMonth(it)} · ${clock(it)}" }

    /** "14". */
    fun day(instant: String): String = local(instant).day.toString()

    /** "nov.". */
    fun month(instant: String): String = shortMonth(local(instant))

    /** "Vendredi · 19 h". */
    fun weekdayHour(instant: String): String = local(instant).let { "${dayName(it).cap()} · ${hour(it)}" }

    /** "Vendredi 14 novembre". */
    fun fullDate(instant: String): String = local(instant).let { "${dayName(it).cap()} ${it.day} ${months[it.month.ordinal]}" }

    /** "19 h – 23 h", or "19 h" without an end. */
    fun hours(
        start: String,
        end: String?,
    ): String = hour(local(start)) + (end?.let { " – " + hour(local(it)) } ?: "")

    /** "Entré à 21:12". */
    fun clockOf(instant: String): String = clock(local(instant))

    /** "1 novembre 2026 à 21:12". */
    fun receiptDate(instant: String): String = local(instant).let { "${it.day} ${months[it.month.ordinal]} ${it.year} à ${clock(it)}" }

    /** "1 sept. 2026". */
    fun shortDate(date: String): String = LocalDate.parse(date).let { "${it.day} ${shortMonths[it.month.ordinal]} ${it.year}" }

    /** "31 août 2027". */
    fun longDate(date: String): String = LocalDate.parse(date).let { "${it.day} ${months[it.month.ordinal]} ${it.year}" }

    /** "18/10/2025", as the card writes it. */
    fun cardDate(date: String): String = LocalDate.parse(date).let { "${two(it.day)}/${two(it.month.ordinal + 1)}/${it.year}" }

    private fun dayName(time: LocalDateTime) = days[time.dayOfWeek.ordinal]

    private fun shortMonth(time: LocalDateTime) = shortMonths[time.month.ordinal]

    private fun clock(time: LocalDateTime) = "${two(time.hour)}:${two(time.minute)}"

    /** "19 h", "19 h 30". */
    private fun hour(time: LocalDateTime) = if (time.minute == 0) "${time.hour} h" else "${time.hour} h ${two(time.minute)}"

    private fun two(value: Int) = value.toString().padStart(2, '0')

    private fun String.cap() = replaceFirstChar { it.uppercase() }
}
