package bighid.romatazok.adherentapp.data

import io.ktor.client.engine.HttpClientEngine

/** The phone's safe (Android Keystore, iOS Keychain): the refresh token and the phone's identifier live here only. */
interface SecureStore {
    fun get(key: String): String?

    fun put(
        key: String,
        value: String,
    )

    fun remove(key: String)
}

/** What differs between Android and iOS; everything else is common. */
interface Platform {
    val secure: SecureStore

    /** The server of the development machine, as the emulator or the simulator sees it. */
    val defaultServerUrl: String

    /** "Google Pixel 9", "iPhone": the name of the phone, shown to the connected one when it asks. */
    val deviceModel: String

    fun httpEngine(): HttpClientEngine
}

expect fun platform(): Platform
