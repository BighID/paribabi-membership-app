package bighid.romatazok.adherentapp.data

import bighid.romatazok.adherentapp.ui.login.CodeCheck
import bighid.romatazok.adherentapp.ui.login.LoginService
import bighid.romatazok.adherentapp.ui.login.OtherPhoneAnswer
import bighid.romatazok.adherentapp.ui.login.PinCheck
import bighid.romatazok.adherentapp.ui.login.Start
import kotlinx.coroutines.delay
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * The screens' login, on the server. Between two steps it keeps what the server gave: the ticket of a right code,
 * the secret of a request waiting for the other phone. The phone's identifier is drawn once and kept in the safe.
 */
class ApiLoginService(
    private val api: MemberApi,
    private val secure: SecureStore,
    private val label: String,
) : LoginService {
    override val answerWindowSeconds = 120

    private var ticket: String? = null
    private var requestSecret: String? = null

    @OptIn(ExperimentalUuidApi::class)
    val deviceId: String by lazy { secure.get(DEVICE_KEY) ?: Uuid.random().toString().also { secure.put(DEVICE_KEY, it) } }

    override suspend fun start(phone: String): Start = started { api.start(phone, deviceId, label) }

    override suspend fun forgotPin(phone: String): Start = started { api.forgotPin(phone, deviceId, label) }

    private suspend fun started(call: suspend () -> AuthAnswer): Start = try {
        ticket = null
        if (call().next == "PIN") Start.Pin else Start.CodeSent
    } catch (error: ApiException) {
        if (error.status == TOO_MANY) Start.TooMany else Start.Failed(error.message.orEmpty())
    } catch (error: NetworkException) {
        Start.Failed(error.message.orEmpty())
    }

    override suspend fun checkCode(
        phone: String,
        code: String,
    ): CodeCheck = try {
        val answer = api.code(phone, deviceId, code)
        ticket = answer.ticket
        if (answer.next == "CREATE_PIN") CodeCheck.CreatePin else CodeCheck.EnterPin
    } catch (error: ApiException) {
        when {
            error.status == TOO_MANY -> CodeCheck.Blocked
            error.reason == "CODE_WRONG" -> CodeCheck.Wrong
            else -> CodeCheck.Failed(error.message.orEmpty())
        }
    } catch (error: NetworkException) {
        CodeCheck.Failed(error.message.orEmpty())
    }

    override suspend fun createPin(
        phone: String,
        pin: String,
    ): PinCheck {
        val ticket = ticket ?: return PinCheck.Failed(RESTART)
        return pinned { api.newPin(deviceId, label, ticket, pin) }
    }

    /** On the linked phone without a ticket; on a new one with the ticket of its code. */
    override suspend fun checkPin(
        phone: String,
        pin: String,
    ): PinCheck = pinned { api.pin(phone, deviceId, label, pin, ticket) }

    private suspend fun pinned(call: suspend () -> AuthAnswer): PinCheck = checked {
        val answer = call()
        ticket = null
        if (answer.next == "WAIT_FOR_OTHER_PHONE") {
            requestSecret = answer.requestSecret
            PinCheck.OtherPhone
        } else {
            PinCheck.SignedIn
        }
    }

    /** Asked every few seconds; the network may come and go meanwhile. */
    override suspend fun awaitOtherPhone(phone: String): OtherPhoneAnswer {
        val secret = requestSecret ?: return OtherPhoneAnswer.Expired
        while (true) {
            delay(POLL_MILLIS)
            val answer = runCatching { api.request(deviceId, secret) }.getOrNull() ?: continue
            when (answer.status) {
                "ACCEPTED" -> return OtherPhoneAnswer.Accepted.also { requestSecret = null }
                "REFUSED" -> return OtherPhoneAnswer.Refused.also { requestSecret = null }
                "EXPIRED", "CANCELLED" -> return OtherPhoneAnswer.Expired.also { requestSecret = null }
            }
        }
    }

    override suspend fun changePin(
        current: String,
        pin: String,
    ): PinCheck = checked {
        api.changePin(current, pin)
        PinCheck.SignedIn
    }

    override suspend fun verifyPin(pin: String): PinCheck = checked {
        api.verifyPin(pin)
        PinCheck.SignedIn
    }

    override suspend fun reportNotMe(): Boolean = runCatching { api.reportNotMe() }.getOrDefault(false)

    private suspend fun checked(call: suspend () -> PinCheck): PinCheck = try {
        call()
    } catch (error: ApiException) {
        when {
            error.reason == "PIN_WRONG" -> PinCheck.Wrong(error.triesLeft ?: 0)
            error.reason == "PIN_LOCKED" -> PinCheck.Locked
            error.reason == "PIN_TOO_EASY" -> PinCheck.TooEasy
            else -> PinCheck.Failed(error.message.orEmpty())
        }
    } catch (error: NetworkException) {
        PinCheck.Failed(error.message.orEmpty())
    } catch (error: SessionExpiredException) {
        PinCheck.Failed(error.message.orEmpty())
    }

    private companion object {
        const val DEVICE_KEY = "device_id"
        const val TOO_MANY = 429
        const val POLL_MILLIS = 3_000L
        const val RESTART = "Recommence la connexion."
    }
}
