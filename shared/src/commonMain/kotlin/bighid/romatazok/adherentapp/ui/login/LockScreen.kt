package bighid.romatazok.adherentapp.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bighid.romatazok.adherentapp.ui.rememberBiometricUnlock
import kotlinx.coroutines.launch

/**
 * Over everything, when the member asked for it: the band with the card, the fingerprint or the face at once if the
 * phone has one, its key in the bottom left of the keypad, the PIN otherwise. The PIN tries count with the
 * connection's, and a locked PIN leads back to the connection.
 */
@Composable
fun LockScreen(
    service: LoginService,
    onUnlocked: () -> Unit,
    /** Forgotten or locked PIN: signed out, to choose a new one with "Code PIN oublié ?". */
    onSignOut: () -> Unit,
    /** The session ended: no PIN opens it again, the connection says why. */
    onSessionOver: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val biometric = rememberBiometricUnlock()
    var error by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { biometric?.invoke(onUnlocked) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).navigationBarsPadding()) {
        AuthHero(onBack = null, size = HeroSize.KEYPAD)
        PinEntry(
            title = "Ton code PIN",
            message = if (biometric != null) "Ou ton empreinte." else "Pour ouvrir PariBabi.",
            error = error,
            onChange = { error = null },
            onPin = { pin ->
                checking = true
                scope.launch {
                    val check = service.verifyPin(pin)
                    checking = false
                    when (check) {
                        PinCheck.SignedIn, PinCheck.OtherPhone -> onUnlocked()
                        is PinCheck.Wrong -> error = wrongPin(check.triesLeft)
                        PinCheck.Locked -> onSignOut()
                        PinCheck.TooEasy -> Unit
                        PinCheck.SessionOver -> onSessionOver()
                        is PinCheck.Failed -> error = check.message
                    }
                }
            },
            modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
            checking = checking,
            bottomLeft = biometric?.let { unlock -> { BiometricKey { unlock(onUnlocked) } } },
            footer = { ForgotLink(onSignOut) },
        )
    }
}
