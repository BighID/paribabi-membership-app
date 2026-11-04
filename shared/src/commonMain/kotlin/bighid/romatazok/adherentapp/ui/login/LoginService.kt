package bighid.romatazok.adherentapp.ui.login

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay

/** The countries offered before the number; the club joins Abidjan and Paris. */
enum class Country(
    val code: String,
    val dial: String,
) {
    CI("CI", "+225"),
    FR("FR", "+33"),
}

sealed interface Start {
    /** This phone is already linked to the number: the PIN alone. */
    data object Pin : Start

    /** Always the same answer from another phone, whether the number is known or not. */
    data object CodeSent : Start

    data class TooMany(
        val minutes: Int,
    ) : Start
}

sealed interface CodeCheck {
    /** First connection, or a forgotten PIN: the member chooses one. */
    data object CreatePin : CodeCheck

    /** A new phone for an account that has a PIN. */
    data object EnterPin : CodeCheck

    data object Wrong : CodeCheck

    data class Blocked(
        val minutes: Int,
    ) : CodeCheck
}

sealed interface PinCheck {
    data object SignedIn : PinCheck

    /** Another phone is connected: it must agree first, or the team. */
    data object OtherPhone : PinCheck

    data class Wrong(
        val triesLeft: Int,
    ) : PinCheck

    /** After 5 wrong PINs: only "Code PIN oublié ?" opens it again. */
    data object Locked : PinCheck

    data object TooEasy : PinCheck
}

sealed interface OtherPhoneAnswer {
    data object Accepted : OtherPhoneAnswer

    data object Refused : OtherPhoneAnswer
}

/** What the screen asks the server; the real one is plugged later, the sample answers like it. */
interface LoginService {
    /** How long the screen says it waits for the other phone, before saying the team can help. */
    val answerWindowSeconds: Int

    suspend fun start(phone: String): Start

    suspend fun forgotPin(phone: String): Start

    suspend fun checkCode(
        phone: String,
        code: String,
    ): CodeCheck

    suspend fun createPin(
        phone: String,
        pin: String,
    ): PinCheck

    suspend fun checkPin(
        phone: String,
        pin: String,
    ): PinCheck

    /** Waits for the other phone or the team; a request without answer never opens the account. */
    suspend fun awaitOtherPhone(phone: String): OtherPhoneAnswer

    /**
     * From the phone signed out by a change the team validated: the new phone is signed out, the QR replaced, the team
     * told, and phone changes stay blocked until the team checks.
     */
    suspend fun reportNotMe()
}

/**
 * Fictitious answers, to walk every path on the emulator. The code sent is always 123456, the PIN of the accounts
 * that have one is 2580, and the number picks the case:
 * - 07 00 00 00 01: a PIN, another phone connected that says yes after a few seconds;
 * - 07 00 00 00 02: a PIN, another phone that does not answer;
 * - 07 00 00 00 04: a PIN, another phone that refuses;
 * - 07 00 00 00 05, once signed in: this phone is asked by a new one;
 * - 07 00 00 00 06, once signed in: this phone is signed out by a change the team validated;
 * - 07 00 00 00 07: a PIN, no other phone, like a reinstalled app;
 * - any other number: a first connection, the code then a new PIN.
 * Once signed in then signed out, this phone is linked to the number: the PIN alone.
 */
class SampleLogin : LoginService {
    override val answerWindowSeconds = 20

    private var linked: String? = null
    private val pins = mutableMapOf("0700000001" to "2580", "0700000002" to "2580", "0700000004" to "2580", "0700000007" to "2580")
    private val pinTries = mutableMapOf<String, Int>()
    private var codeTries = 0
    private var resetting = false

    override suspend fun start(phone: String): Start {
        delay(600)
        resetting = false
        return if (phone == linked && phone in pins) Start.Pin else Start.CodeSent
    }

    override suspend fun forgotPin(phone: String): Start {
        delay(600)
        resetting = true
        return Start.CodeSent
    }

    override suspend fun checkCode(
        phone: String,
        code: String,
    ): CodeCheck {
        delay(600)
        if (code != SAMPLE_CODE) {
            codeTries++
            return if (codeTries >= 5) CodeCheck.Blocked(15) else CodeCheck.Wrong
        }
        codeTries = 0
        return if (resetting || phone !in pins) CodeCheck.CreatePin else CodeCheck.EnterPin
    }

    override suspend fun createPin(
        phone: String,
        pin: String,
    ): PinCheck {
        delay(500)
        if (pin.tooEasy()) return PinCheck.TooEasy
        pins[phone] = pin
        pinTries[phone] = 0
        resetting = false
        return signIn(phone)
    }

    override suspend fun checkPin(
        phone: String,
        pin: String,
    ): PinCheck {
        delay(500)
        val tries = pinTries[phone] ?: 0
        if (tries >= PIN_TRIES) return PinCheck.Locked
        if (pin != pins[phone]) {
            pinTries[phone] = tries + 1
            return if (tries + 1 >= PIN_TRIES) PinCheck.Locked else PinCheck.Wrong(PIN_TRIES - tries - 1)
        }
        pinTries[phone] = 0
        return signIn(phone)
    }

    override suspend fun awaitOtherPhone(phone: String): OtherPhoneAnswer =
        when (phone.case()) {
            1 -> {
                delay(4_000)
                linked = phone
                OtherPhoneAnswer.Accepted
            }
            4 -> {
                delay(4_000)
                OtherPhoneAnswer.Refused
            }
            // Without an answer, nothing opens: only the other phone or the team.
            else -> awaitCancellation()
        }

    override suspend fun reportNotMe() = delay(700)

    private fun signIn(phone: String): PinCheck {
        if (phone != linked && phone.case() in listOf(1, 2, 4)) return PinCheck.OtherPhone
        linked = phone
        return PinCheck.SignedIn
    }

    private fun String.case() = if (startsWith("07000000")) last().digitToIntOrNull() ?: 0 else 0
}

/** 0000, 1111, 1234, 4321 and the like: the same digit four times, or a run up or down. */
fun String.tooEasy(): Boolean {
    val steps = zipWithNext { a, b -> b - a }.toSet()
    return steps == setOf(0) || steps == setOf(1) || steps == setOf(-1)
}

const val PIN_LENGTH = 4
private const val PIN_TRIES = 5
private const val SAMPLE_CODE = "123456"
