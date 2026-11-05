package bighid.romatazok.adherentapp.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The token of this phone for the notifications, given by the platform (Firebase on Android) when it has one or
 * renews it. The app sends it to the server once signed in; null on a phone that cannot be notified.
 */
object Push {
    private val current = MutableStateFlow<String?>(null)

    val token: StateFlow<String?> = current.asStateFlow()

    fun received(token: String) {
        if (token.isNotBlank()) current.value = token
    }
}
