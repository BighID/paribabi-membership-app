package bighid.romatazok.adherentapp.ui.login

/** The countries offered before the number; the club joins Abidjan and Paris. */
enum class Country(
    val code: String,
    val dial: String,
) {
    CI("CI", "+225"),
    FR("FR", "+33"),
    ;

    /** "0701020304" as the server reads it: an Ivorian number keeps its 0, a French one loses it. */
    fun international(number: String): String = dial + if (this == FR) number.removePrefix("0") else number
}

sealed interface Start {
    /** This phone is already linked to the number: the PIN alone. */
    data object Pin : Start

    /** Always the same answer from another phone, whether the number is known or not. */
    data object CodeSent : Start

    data object TooMany : Start

    /** The network, or the server, said no: its message as it is. */
    data class Failed(
        val message: String,
    ) : Start
}

sealed interface CodeCheck {
    /** First connection, or a forgotten PIN: the member chooses one. */
    data object CreatePin : CodeCheck

    /** A new phone for an account that has a PIN. */
    data object EnterPin : CodeCheck

    data object Wrong : CodeCheck

    data object Blocked : CodeCheck

    data class Failed(
        val message: String,
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

    data class Failed(
        val message: String,
    ) : PinCheck
}

sealed interface OtherPhoneAnswer {
    data object Accepted : OtherPhoneAnswer

    data object Refused : OtherPhoneAnswer

    /** Seven days without an answer, or replaced by a newer request. */
    data object Expired : OtherPhoneAnswer
}

/** What the screens ask the server. Nothing in them knows how it is reached. */
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

    /** Signed in: the current PIN, then the new one. */
    suspend fun changePin(
        current: String,
        pin: String,
    ): PinCheck

    /** The lock on opening: the PIN checked, no new session. */
    suspend fun verifyPin(pin: String): PinCheck

    /**
     * From the phone signed out by a change the team validated: the new phone is signed out, the QR replaced, the team
     * told, and phone changes stay blocked until the team checks. False when it no longer can: already done, or
     * more than 7 days ago.
     */
    suspend fun reportNotMe(): Boolean
}

/** 0000, 1111, 1234, 4321 and the like: the same digit four times, or a run up or down. */
fun String.tooEasy(): Boolean {
    val steps = zipWithNext { a, b -> b - a }.toSet()
    return steps == setOf(0) || steps == setOf(1) || steps == setOf(-1)
}

const val PIN_LENGTH = 4
