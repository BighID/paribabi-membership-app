package bighid.romatazok.adherentapp.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
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

    suspend fun pendingRequests(): List<PendingRequest> = authorized { token -> client.get("$baseUrl/api/member/device-requests") { bearerAuth(token) } }.ok()

    suspend fun answerRequest(
        id: String,
        accept: Boolean,
    ): Unit = authorized { token -> client.post("$baseUrl/api/member/device-requests/$id/${if (accept) "accept" else "refuse"}") { bearerAuth(token) } }.ok()

    suspend fun changePin(
        current: String,
        pin: String,
    ): Unit = authorizedPost("/api/member/pin", ChangePinBody(current, pin)).ok()

    suspend fun verifyPin(pin: String): Unit = authorizedPost("/api/member/pin/verify", VerifyPinBody(pin)).ok()

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

    /** One renewal at a time: the server treats two uses of one refresh token as theft. */
    private suspend fun refresh(): Boolean = refreshLock.withLock {
        val token = secure.get(REFRESH_KEY) ?: return false
        val response = call { client.post("$baseUrl/api/member/auth/refresh") { contentType(ContentType.Application.Json); setBody(RefreshBody(token)) } }
        if (!response.status.isSuccess()) {
            if (response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden) forget()
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
    }
}
