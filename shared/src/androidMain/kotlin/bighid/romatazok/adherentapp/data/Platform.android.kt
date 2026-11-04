package bighid.romatazok.adherentapp.data

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Set by the activity before anything else. */
object AndroidContext {
    lateinit var app: Context
}

private val instance: Platform by lazy { AndroidPlatform(AndroidContext.app) }

actual fun platform(): Platform = instance

private class AndroidPlatform(
    context: Context,
) : Platform {
    override val secure: SecureStore = KeystoreStore(context)

    /** The development machine is 10.0.2.2 from the emulator; a real phone is given the server's address. */
    override val defaultServerUrl: String =
        if (Build.FINGERPRINT.contains("generic") || Build.HARDWARE.contains("ranchu") || Build.PRODUCT.contains("sdk")) "http://10.0.2.2:8080" else ""

    override val deviceModel: String = listOfNotNull(Build.MANUFACTURER?.replaceFirstChar { it.uppercase() }, Build.MODEL).joinToString(" ").ifBlank { "Android" }

    override fun httpEngine(): HttpClientEngine = OkHttp.create()
}

/** Values encrypted with a key that never leaves the Android Keystore (AES-GCM). */
private class KeystoreStore(
    context: Context,
) : SecureStore {
    private val preferences = context.getSharedPreferences("secure", Context.MODE_PRIVATE)

    override fun get(key: String): String? {
        val stored = preferences.getString(key, null) ?: return null
        return runCatching {
            val bytes = Base64.decode(stored, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, bytes, 0, IV_SIZE))
            String(cipher.doFinal(bytes, IV_SIZE, bytes.size - IV_SIZE), Charsets.UTF_8)
        }.getOrNull()
    }

    override fun put(
        key: String,
        value: String,
    ) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        preferences.edit().putString(key, Base64.encodeToString(encrypted, Base64.NO_WRAP)).apply()
    }

    override fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).run {
            init(
                KeyGenParameterSpec
                    .Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "paribabi-adherent"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_BITS = 128
    }
}
