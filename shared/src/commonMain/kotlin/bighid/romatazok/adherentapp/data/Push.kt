package bighid.romatazok.adherentapp.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where a notification or a `paribabi://` link opens the app. */
sealed interface DeepLink {
    /** The receipt of a payment, in its bottom sheet over the home. */
    data class Receipt(
        val paymentId: String,
    ) : DeepLink

    /** The page of the membership. */
    data object Membership : DeepLink

    companion object {
        /** From a notification: what the server put in it, never shown. */
        fun fromNotification(
            target: String?,
            id: String?,
        ): DeepLink? = when (target) {
            "RECEIPT" -> id?.takeIf { it.isNotBlank() }?.let(::Receipt)
            "MEMBERSHIP" -> Membership
            else -> null
        }

        /** "paribabi://recu/<payment>" or "paribabi://adhesion"; anything else opens the home. */
        fun fromUri(uri: String?): DeepLink? {
            val rest = uri?.removePrefix("paribabi://")?.takeIf { it != uri } ?: return null
            val parts = rest.trim('/').split('/')
            return when (parts.firstOrNull()) {
                "recu" -> parts.getOrNull(1)?.takeIf { it.isNotBlank() }?.let(::Receipt)
                "adhesion" -> Membership
                else -> null
            }
        }
    }
}

/**
 * The notifications of this phone: its token, given by the platform (Firebase on Android) when it has one or
 * renews it, and the place a tapped notification asks to open, kept until the app can show it.
 */
object Push {
    private val current = MutableStateFlow<String?>(null)
    private val waiting = MutableStateFlow<DeepLink?>(null)

    val token: StateFlow<String?> = current.asStateFlow()

    /** Opened once the member is signed in, past the PIN, with the home read. */
    val opening: StateFlow<DeepLink?> = waiting.asStateFlow()

    fun received(token: String) {
        if (token.isNotBlank()) current.value = token
    }

    fun open(link: DeepLink?) {
        if (link != null) waiting.value = link
    }

    /** Shown, or no longer for this member. */
    fun opened() {
        waiting.value = null
    }
}
