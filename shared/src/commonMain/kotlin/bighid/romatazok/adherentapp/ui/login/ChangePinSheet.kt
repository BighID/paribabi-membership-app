package bighid.romatazok.adherentapp.ui.login

import adherentapp.shared.generated.resources.Res
import adherentapp.shared.generated.resources.ic_back
import adherentapp.shared.generated.resources.ic_check
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bighid.romatazok.adherentapp.ui.SystemBack
import bighid.romatazok.adherentapp.ui.events.GREEN
import bighid.romatazok.adherentapp.ui.events.NAVY_LIGHT
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

private enum class Change { CURRENT, NEW, DONE }

/**
 * From the profile: the current PIN, then the new one twice, in the same sheet. Its top row is its app bar: the way
 * back, from the confirmation to the new PIN, then to the current one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePinSheet(
    service: LoginService,
    phone: String,
    onDismiss: () -> Unit,
    /** The current PIN locked after 5 tries: signed out, to choose a new one with "Code PIN oublié ?". */
    onLocked: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(Change.CURRENT) }
    var error by remember { mutableStateOf<String?>(null) }
    val newPin = remember { NewPin() }
    val canGoBack = step == Change.NEW
    fun back() {
        error = null
        if (newPin.confirming) newPin.back() else step = Change.CURRENT
    }
    SystemBack(canGoBack) { back() }
    ModalBottomSheet(onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    if (canGoBack) {
                        Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = ::back), contentAlignment = Alignment.Center) {
                            Icon(painterResource(Res.drawable.ic_back), "Retour", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                Text("Changer mon code PIN", Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                Spacer(Modifier.size(44.dp))
            }
            AnimatedContent(step) { current ->
                when (current) {
                    Change.CURRENT ->
                        PinEntry(
                            title = "Ton code actuel",
                            message = "Celui que tu utilises aujourd'hui.",
                            error = error,
                            onChange = { error = null },
                            onPin = { pin ->
                                scope.launch {
                                    when (val check = service.checkPin(phone, pin)) {
                                        PinCheck.SignedIn, PinCheck.OtherPhone -> step = Change.NEW
                                        is PinCheck.Wrong -> error = wrongPin(check.triesLeft)
                                        PinCheck.Locked -> onLocked()
                                        PinCheck.TooEasy -> Unit
                                    }
                                }
                            },
                            fill = false,
                        )
                    Change.NEW ->
                        NewPinEntry(
                            state = newPin,
                            resetting = true,
                            error = error,
                            onChange = { error = null },
                            onPin = { pin ->
                                scope.launch {
                                    when (service.createPin(phone, pin)) {
                                        PinCheck.TooEasy -> error = "Trop facile. Choisis-en un autre."
                                        else -> step = Change.DONE
                                    }
                                }
                            },
                            fill = false,
                        )
                    Change.DONE ->
                        Column(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(64.dp).clip(CircleShape).background(GREEN.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                                Icon(painterResource(Res.drawable.ic_check), null, Modifier.size(30.dp), tint = GREEN)
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("Code PIN changé", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(22.dp))
                            Text(
                                "Fermer",
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(NAVY_LIGHT).clickable(onClick = onDismiss).padding(vertical = 15.dp),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                            )
                        }
                }
            }
        }
    }
}
