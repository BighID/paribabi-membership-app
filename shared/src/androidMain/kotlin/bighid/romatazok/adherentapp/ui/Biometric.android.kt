package bighid.romatazok.adherentapp.ui

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

@Composable
actual fun rememberBiometricUnlock(): ((onSuccess: () -> Unit) -> Unit)? {
    val activity = LocalContext.current as? FragmentActivity ?: return null
    return remember(activity) {
        if (BiometricManager.from(activity).canAuthenticate(BIOMETRIC_WEAK) != BiometricManager.BIOMETRIC_SUCCESS) return@remember null
        val prompt =
            BiometricPrompt.PromptInfo
                .Builder()
                .setTitle("Déverrouiller PariBabi")
                .setNegativeButtonText("Utiliser mon code PIN")
                .setAllowedAuthenticators(BIOMETRIC_WEAK)
                .build()
        val unlock: (() -> Unit) -> Unit = { onSuccess ->
            BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
                },
            ).authenticate(prompt)
        }
        unlock
    }
}
