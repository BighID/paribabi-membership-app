package bighid.romatazok.adherentapp.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val ApiJson =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

/** The server refused, with its French message and the reason the app acts on. */
class ApiException(
    val status: Int,
    message: String,
    val reason: String?,
    val triesLeft: Int?,
) : Exception(message)

/** No answer from the server: no network, server stopped, or too slow. */
class NetworkException(
    cause: Throwable? = null,
) : Exception("Pas de réseau. Réessaie dans un instant.", cause)

/** The session cannot be renewed any more: back to the connection. */
class SessionExpiredException : Exception("Session expirée : reconnecte-toi.")

@Serializable
data class Tokens(
    val accessToken: String,
    val accessExpiresIn: Long,
    val refreshToken: String,
    val refreshExpiresIn: Long,
)

@Serializable
data class AuthAnswer(
    /** `PIN`, `CODE`, `CREATE_PIN`, `ENTER_PIN`, `SIGNED_IN`, `WAIT_FOR_OTHER_PHONE`. */
    val next: String,
    val ticket: String? = null,
    val tokens: Tokens? = null,
    val requestSecret: String? = null,
)

@Serializable
data class RequestAnswer(
    /** `PENDING`, `ACCEPTED`, `REFUSED`, `EXPIRED`, `CANCELLED`. */
    val status: String,
    val tokens: Tokens? = null,
)

@Serializable
data class PendingRequest(
    val id: String,
    val label: String? = null,
    val askedAt: String,
)

@Serializable
data class ProfileDto(
    val firstName: String? = null,
    val fullName: String,
    val phone: String? = null,
    val email: String? = null,
    val jobTitle: String? = null,
    val company: String? = null,
    val city: String? = null,
    val sector: String? = null,
    val memberNumber: String? = null,
    /** "2025-10-18". */
    val memberSince: String? = null,
    val validUntil: String? = null,
    val qr: String? = null,
    /** `ACTIVE` or `EXPIRED`. */
    val status: String,
    val seasonName: String? = null,
    val hasPhoto: Boolean = false,
)

@Serializable
data class ActivityDto(
    val id: String,
    /** `PAYMENT`, `MEMBERSHIP`, `REMINDER`, `APPLICATION`. */
    val kind: String,
    val title: String,
    /** ISO instant. */
    val at: String,
    val amount: Long? = null,
    /** `RECEIPT`, `EVENT`, `MEMBERSHIP`. */
    val target: String,
    val targetId: String? = null,
)

@Serializable
data class SeasonEventDto(
    val id: String,
    val name: String,
    val startsAt: String,
    val venue: String? = null,
    /** `PRESENT`, `ABSENT`, `UPCOMING`. */
    val presence: String,
    val arrivedAt: String? = null,
    val ticketPaymentId: String? = null,
)

@Serializable
data class SeasonDto(
    val name: String,
    /** `ACTIVE`, `EXPIRED`, `ENDED`. */
    val status: String,
    val startsOn: String,
    val joinedOn: String? = null,
    val endsOn: String,
    val receiptPaymentId: String? = null,
    val events: List<SeasonEventDto> = emptyList(),
)

@Serializable
data class EventDto(
    val id: String,
    val name: String,
    val startsAt: String,
    val endsAt: String? = null,
    val venue: String? = null,
    /** `PUBLIC`, `MEMBERS_ONLY`, `MIXED`. */
    val access: String,
    val free: Boolean,
    val price: Long? = null,
    val description: String? = null,
    val practicalInfo: String? = null,
    val onlineSalesOpen: Boolean = true,
    val hasPoster: Boolean = false,
    val ticketPaymentId: String? = null,
)

@Serializable
data class ReceiptDto(
    val number: String,
    val title: String,
    val total: Long,
    val paidAt: String,
    val method: String,
    val refunded: Boolean = false,
)

@Serializable
private data class ProfileBody(
    val jobTitle: String?,
    val company: String?,
    val city: String?,
    val sector: String?,
)

@Serializable
private data class EmailBody(
    val email: String,
    val pin: String,
)

@Serializable
private data class EmailCodeBody(
    val code: String,
)

@Serializable
private data class Problem(
    val detail: String? = null,
    val reason: String? = null,
    val triesLeft: Int? = null,
)

@Serializable
private data class StartBody(
    val phone: String,
    val deviceId: String,
    val deviceLabel: String?,
)

@Serializable
private data class CodeBody(
    val phone: String,
    val deviceId: String,
    val code: String,
)

@Serializable
private data class PinBody(
    val phone: String,
    val deviceId: String,
    val deviceLabel: String?,
    val pin: String,
    val ticket: String?,
)

@Serializable
private data class NewPinBody(
    val deviceId: String,
    val deviceLabel: String?,
    val ticket: String,
    val pin: String,
)

@Serializable
private data class RequestBody(
    val deviceId: String,
    val secret: String,
)

@Serializable
private data class RefreshBody(
    val refreshToken: String,
)

@Serializable
private data class ChangePinBody(
    val current: String,
    val pin: String,
)

@Serializable
private data class VerifyPinBody(
    val pin: String,
)

@Serializable
data class RenewalLink(
    val url: String,
    val amount: Long,
)

@Serializable
private data class PushTokenBody(
    val token: String,
)

/**
 * The calls of the members' API. The access token lives in memory, the refresh token in the phone's safe. A 401 on
 * a member's call renews the session once, then replays it.
 */
class MemberApi(
    engine: HttpClientEngine,
    private val secure: SecureStore,
    private val baseUrl: String,
) {
    private var accessToken: String? = null
    private val refreshLock = Mutex()

    private val client =
        HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(ApiJson) }
            install(HttpTimeout) {
                connectTimeoutMillis = 8_000
                requestTimeoutMillis = 20_000
            }
        }

    /** A session kept in the safe: the member stays signed in. */
    val hasSession: Boolean get() = secure.get(REFRESH_KEY) != null

    suspend fun start(
        phone: String,
        deviceId: String,
        label: String?,
    ): AuthAnswer = open("/api/member/auth/start", StartBody(phone, deviceId, label)).ok()

    suspend fun forgotPin(
        phone: String,
        deviceId: String,
        label: String?,
    ): AuthAnswer = open("/api/member/auth/forgot-pin", StartBody(phone, deviceId, label)).ok()

    suspend fun code(
        phone: String,
        deviceId: String,
        code: String,
    ): AuthAnswer = open("/api/member/auth/code", CodeBody(phone, deviceId, code)).ok()

    suspend fun pin(
        phone: String,
        deviceId: String,
        label: String?,
        pin: String,
        ticket: String?,
    ): AuthAnswer = open("/api/member/auth/pin", PinBody(phone, deviceId, label, pin, ticket)).ok<AuthAnswer>().also(::keep)

    suspend fun newPin(
        deviceId: String,
        label: String?,
        ticket: String,
        pin: String,
    ): AuthAnswer = open("/api/member/auth/new-pin", NewPinBody(deviceId, label, ticket, pin)).ok<AuthAnswer>().also(::keep)

    suspend fun request(
        deviceId: String,
        secret: String,
    ): RequestAnswer = open("/api/member/auth/request", RequestBody(deviceId, secret)).ok<RequestAnswer>().also { answer -> answer.tokens?.let(::keep) }

    suspend fun pendingRequests(): List<PendingRequest> = get("/api/member/device-requests").ok()

    suspend fun answerRequest(
        id: String,
        accept: Boolean,
    ): Unit = authorized { token -> client.post("$baseUrl/api/member/device-requests/$id/${if (accept) "accept" else "refuse"}") { bearerAuth(token) } }.ok()

    suspend fun changePin(
        current: String,
        pin: String,
    ): Unit = authorizedPost("/api/member/pin", ChangePinBody(current, pin)).ok()

    suspend fun verifyPin(pin: String): Unit = authorizedPost("/api/member/pin/verify", VerifyPinBody(pin)).ok()

    /** The phone's notification token, kept by the server on the phone signed in. */
    suspend fun registerPush(token: String): Unit = authorized { access ->
        client.put("$baseUrl/api/member/push-token") {
            bearerAuth(access)
            contentType(ContentType.Application.Json)
            setBody(PushTokenBody(token))
        }
    }.ok()

    suspend fun profile(): ProfileDto = get("/api/member/profile").ok()

    suspend fun activity(): List<ActivityDto> = get("/api/member/activity").ok()

    suspend fun seasons(): List<SeasonDto> = get("/api/member/seasons").ok()

    suspend fun events(): List<EventDto> = get("/api/member/events").ok()

    suspend fun receipt(paymentId: String): ReceiptDto = get("/api/member/receipts/$paymentId").ok()

    /** The receipt's PDF and the name the server gives it; null when it could not be read. */
    suspend fun receiptPdf(paymentId: String): Pair<String, ByteArray>? = runCatching {
        val response = get("/api/member/receipts/$paymentId/pdf")
        if (!response.status.isSuccess()) return@runCatching null
        val name =
            response.headers[HttpHeaders.ContentDisposition]
                ?.let { ContentDisposition.parse(it).parameter(ContentDisposition.Parameters.FileName) }
                ?.takeIf { it.isNotBlank() } ?: "recu.pdf"
        name to response.body<ByteArray>()
    }.getOrNull()

    suspend fun sectors(): List<String> = get("/api/member/sectors").ok()

    /** The payment page of my renewal, opened in the browser. */
    suspend fun renewal(): RenewalLink = authorizedPost("/api/member/renewal", emptyMap<String, String>()).ok()

    /** The poster's image; null when the event has none, or the network failed. */
    suspend fun poster(eventId: String): ByteArray? = runCatching {
        val response = get("/api/member/events/$eventId/poster")
        if (response.status.isSuccess()) response.body<ByteArray>() else null
    }.getOrNull()

    /** The member's own photo, as the server prepared it; null when it could not be read. */
    suspend fun photo(): ByteArray? = runCatching {
        val response = get("/api/member/photo")
        if (response.status.isSuccess()) response.body<ByteArray>() else null
    }.getOrNull()

    /** The file as it left the gallery: the server cuts it and keeps what it says of itself apart. */
    suspend fun uploadPhoto(content: ByteArray): Unit = authorized { token ->
        client.put("$baseUrl/api/member/photo") {
            bearerAuth(token)
            // Up to 10 MB on a slow network: more time than a reading.
            timeout { requestTimeoutMillis = UPLOAD_TIMEOUT }
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            "photo",
                            content,
                            Headers.build {
                                append(HttpHeaders.ContentType, "image/jpeg")
                                append(HttpHeaders.ContentDisposition, "filename=\"photo.jpg\"")
                            },
                        )
                    },
                ),
            )
        }
    }.ok()

    suspend fun updateProfile(
        jobTitle: String?,
        company: String?,
        city: String?,
        sector: String?,
    ): Unit = authorized { token ->
        client.put("$baseUrl/api/member/profile") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(ProfileBody(jobTitle, company, city, sector))
        }
    }.ok()

    suspend fun requestEmail(
        email: String,
        pin: String,
    ): Unit = authorizedPost("/api/member/email", EmailBody(email, pin)).ok()

    suspend fun confirmEmail(code: String): Unit = authorizedPost("/api/member/email/confirm", EmailCodeBody(code)).ok()

    private suspend fun get(path: String): HttpResponse = authorized { token -> client.get("$baseUrl$path") { bearerAuth(token) } }

    /** The server ends the session; the phone stays linked, the PIN alone signs in again. */
    suspend fun logout() {
        val token = secure.get(REFRESH_KEY)
        forget()
        if (token != null) runCatching { open("/api/member/auth/logout", RefreshBody(token)) }
    }

    /** Signed out by the server (another phone took the account): nothing to tell it. */
    fun forget() {
        accessToken = null
        secure.remove(REFRESH_KEY)
    }

    private fun keep(answer: AuthAnswer) {
        answer.tokens?.let(::keep)
    }

    private fun keep(tokens: Tokens) {
        accessToken = tokens.accessToken
        secure.put(REFRESH_KEY, tokens.refreshToken)
    }

    private suspend inline fun <reified B : Any> open(
        path: String,
        body: B,
    ): HttpResponse = call {
        client.post("$baseUrl$path") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    private suspend inline fun <reified B : Any> authorizedPost(
        path: String,
        body: B,
    ): HttpResponse = authorized { token ->
        client.post("$baseUrl$path") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    /** The current token, renewed once on 401. */
    private suspend fun authorized(request: suspend (token: String) -> HttpResponse): HttpResponse {
        val token = accessToken ?: (if (refresh()) accessToken else null) ?: throw SessionExpiredException()
        val response = call { request(token) }
        if (response.status != HttpStatusCode.Unauthorized) return response
        // A wrong PIN is a 401 too: renewed only when the server says the session is over.
        if (problem(response)?.reason != "SESSION_EXPIRED" && response.headers["WWW-Authenticate"] == null) return response
        if (!refresh()) throw SessionExpiredException()
        return call { request(accessToken!!) }
    }

    /**
     * Signed out because the team validated another phone: this one may still say it was not its member, for 7 days,
     * with the token it kept for that alone.
     */
    val takenOver: Boolean get() = secure.get(TAKEOVER_KEY) != null

    /** "Ce n'était pas moi": true once done; the kept token is dropped either way. */
    suspend fun reportNotMe(): Boolean {
        val token = secure.get(TAKEOVER_KEY) ?: return false
        val response = call { client.post("$baseUrl/api/member/auth/not-me") { contentType(ContentType.Application.Json); setBody(RefreshBody(token)) } }
        if (response.status.isSuccess() || response.status == HttpStatusCode.NotFound) secure.remove(TAKEOVER_KEY)
        return response.status.isSuccess()
    }

    /** A notice read: the report is no longer offered. */
    fun dropTakeover() = secure.remove(TAKEOVER_KEY)

    /** One renewal at a time: the server treats two uses of one refresh token as theft. */
    private suspend fun refresh(): Boolean = refreshLock.withLock {
        val token = secure.get(REFRESH_KEY) ?: return false
        val response = call { client.post("$baseUrl/api/member/auth/refresh") { contentType(ContentType.Application.Json); setBody(RefreshBody(token)) } }
        if (!response.status.isSuccess()) {
            if (response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden) {
                if (problem(response)?.reason == "TAKEN_OVER") secure.put(TAKEOVER_KEY, token)
                forget()
            }
            return false
        }
        keep(response.body<Tokens>())
        true
    }

    private suspend fun call(block: suspend () -> HttpResponse): HttpResponse = try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        throw NetworkException(error)
    }

    private suspend fun problem(response: HttpResponse): Problem? = runCatching { ApiJson.decodeFromString<Problem>(response.bodyAsText()) }.getOrNull()

    private suspend inline fun <reified T> HttpResponse.ok(): T {
        if (status.isSuccess()) {
            @Suppress("UNCHECKED_CAST")
            return if (T::class == Unit::class) Unit as T else body()
        }
        val problem = problem(this)
        throw ApiException(status.value, problem?.detail ?: "Erreur du serveur (${status.value}).", problem?.reason, problem?.triesLeft)
    }

    private companion object {
        const val REFRESH_KEY = "refresh_token"
        const val TAKEOVER_KEY = "takeover_token"
        const val UPLOAD_TIMEOUT = 120_000L
    }
}
