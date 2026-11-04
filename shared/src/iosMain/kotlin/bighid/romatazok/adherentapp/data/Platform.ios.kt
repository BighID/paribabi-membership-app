@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package bighid.romatazok.adherentapp.data

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryCreate
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.UIKit.UIDevice

private val instance: Platform by lazy { IosPlatform() }

actual fun platform(): Platform = instance

private class IosPlatform : Platform {
    override val secure: SecureStore = KeychainStore()

    /** The simulator reaches the development machine as localhost; a real iPhone is given the server's address. */
    override val defaultServerUrl: String = if (NSProcessInfo.processInfo.environment["SIMULATOR_DEVICE_NAME"] != null) "http://localhost:8080" else ""

    override val deviceModel: String = UIDevice.currentDevice.model

    override fun httpEngine(): HttpClientEngine = Darwin.create()
}

/** Generic passwords in the Keychain, readable after the first unlock, never synced to other devices. */
private class KeychainStore : SecureStore {
    override fun get(key: String): String? =
        memScoped {
            val query =
                dictionary(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrService to CFBridgingRetain(SERVICE),
                    kSecAttrAccount to CFBridgingRetain(key),
                    kSecReturnData to kCFBooleanTrue,
                    kSecMatchLimit to kSecMatchLimitOne,
                )
            val result = alloc<CFTypeRefVar>()
            if (SecItemCopyMatching(query, result.ptr) != errSecSuccess) return null
            val data = CFBridgingRelease(result.value) as? NSData ?: return null
            NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
        }

    override fun put(
        key: String,
        value: String,
    ) {
        remove(key)
        memScoped {
            val data = NSString.create(string = value).dataUsingEncoding(NSUTF8StringEncoding) ?: return
            SecItemAdd(
                dictionary(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrService to CFBridgingRetain(SERVICE),
                    kSecAttrAccount to CFBridgingRetain(key),
                    kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
                    kSecValueData to CFBridgingRetain(data),
                ),
                null,
            )
        }
    }

    override fun remove(key: String) {
        memScoped {
            SecItemDelete(
                dictionary(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrService to CFBridgingRetain(SERVICE),
                    kSecAttrAccount to CFBridgingRetain(key),
                ),
            )
        }
    }

    private fun MemScope.dictionary(vararg items: Pair<CFStringRef?, CFTypeRef?>): CFDictionaryRef? {
        val keys = allocArrayOf(*items.map { it.first }.toTypedArray())
        val values = allocArrayOf(*items.map { it.second }.toTypedArray())
        return CFDictionaryCreate(
            kCFAllocatorDefault,
            keys.reinterpret(),
            values.reinterpret(),
            items.size.convert(),
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr,
        )
    }

    private companion object {
        const val SERVICE = "paribabi.adherent"
    }
}
