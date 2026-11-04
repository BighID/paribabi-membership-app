package bighid.romatazok.adherentapp.ui

import androidx.compose.runtime.Composable

/**
 * The fingerprint or the face of the phone, asked to open the app: [onSuccess] once recognised. Null when the phone
 * has none set up, or cannot: the PIN alone then.
 */
@Composable
expect fun rememberBiometricUnlock(): ((onSuccess: () -> Unit) -> Unit)?
