package bighid.romatazok.adherentapp.ui

import androidx.compose.runtime.Composable

/** Face ID and Touch ID come with the iPhone build; until then the PIN alone. */
@Composable
actual fun rememberBiometricUnlock(): ((onSuccess: () -> Unit) -> Unit)? = null
