package bighid.romatazok.adherentapp.ui.login

import adherentapp.shared.generated.resources.Res
import adherentapp.shared.generated.resources.ic_backspace
import adherentapp.shared.generated.resources.ic_fingerprint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bighid.romatazok.adherentapp.ui.events.NAVY_LIGHT
import bighid.romatazok.adherentapp.ui.events.ORANGE
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

/**
 * The layout of every PIN: centred in the free height, what is asked, one short line (the error takes its place) and
 * the four dots; anchored at the bottom, the keypad then [footer]. In a bottom sheet, [fill] is false.
 */
@Composable
fun PinPanel(
    title: String,
    message: String,
    isError: Boolean,
    pin: String,
    shake: Int,
    enabled: Boolean,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    fill: Boolean = true,
    bottomLeft: (@Composable () -> Unit)? = null,
    footer: @Composable () -> Unit = {},
) {
    Column(modifier.fillMaxWidth()) {
        Column(
            (if (fill) Modifier.weight(1f) else Modifier.padding(vertical = 16.dp)).fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Box(Modifier.fillMaxWidth().height(28.dp).padding(top = 4.dp), contentAlignment = Alignment.TopCenter) {
                Text(
                    message,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = if (isError) FontWeight.Medium else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.height(18.dp))
            PinDots(pin.length, shake, isError)
        }
        Spacer(Modifier.height(16.dp))
        PinKeypad(enabled, onDigit, onDelete, bottomLeft)
        footer()
    }
}

/**
 * Four dots, filled as the digits come, never the digits themselves. Empty, a light tint of the colour; filled, the
 * orange of the card; in error, the error colour, and [shake] changing shakes them once.
 */
@Composable
fun PinDots(
    filled: Int,
    shake: Int,
    error: Boolean,
) {
    val offset = remember { Animatable(0f) }
    LaunchedEffect(shake) {
        if (shake == 0) return@LaunchedEffect
        for (x in listOf(-10f, 10f, -8f, 8f, 0f)) offset.animateTo(x, tween(80))
    }
    Row(Modifier.offset(x = offset.value.dp), horizontalArrangement = Arrangement.Center) {
        repeat(PIN_LENGTH) { i ->
            val isFilled = i < filled
            val full = if (error) MaterialTheme.colorScheme.error else ORANGE
            val fillColor by animateColorAsState(if (isFilled) full else NAVY_LIGHT.copy(alpha = 0.12f), tween(180))
            val borderColor by animateColorAsState(if (isFilled) full else NAVY_LIGHT.copy(alpha = 0.35f), tween(180))
            Box(
                Modifier
                    .padding(horizontal = 10.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(fillColor)
                    .border(2.dp, borderColor, CircleShape),
            )
        }
    }
}

/**
 * Twelve framed keys in four rows, 8 apart: the digits, then [bottomLeft] (the fingerprint, or nothing), 0 and the
 * erasing key. A light vibration on each touch.
 */
@Composable
fun PinKeypad(
    enabled: Boolean,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    bottomLeft: (@Composable () -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("123", "456", "789").forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { digit ->
                    Key(Modifier.weight(1f), enabled, onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDigit(digit)
                    }) {
                        Text(digit.toString(), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { bottomLeft?.invoke() }
            Key(Modifier.weight(1f), enabled, onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDigit('0')
            }) {
                Text("0", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            }
            Key(Modifier.weight(1f), enabled = true, onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDelete()
            }) {
                Icon(painterResource(Res.drawable.ic_backspace), "Effacer", Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** The fingerprint key, in the bottom left of the keypad. */
@Composable
fun BiometricKey(onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Key(Modifier.fillMaxWidth(), enabled = true, onClick = {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onClick()
    }) {
        Icon(painterResource(Res.drawable.ic_fingerprint), "Empreinte ou visage", Modifier.size(26.dp), tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun Key(
    modifier: Modifier,
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .height(60.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/**
 * A PIN to type, in [PinPanel]: handed over at the fourth digit, emptied after a refusal while the dots shake. The
 * line under the title says [message], or the error.
 */
@Composable
fun PinEntry(
    title: String,
    message: String,
    error: String?,
    onChange: () -> Unit,
    onPin: (String) -> Unit,
    modifier: Modifier = Modifier,
    fill: Boolean = true,
    bottomLeft: (@Composable () -> Unit)? = null,
    footer: @Composable () -> Unit = {},
) {
    var pin by remember { mutableStateOf("") }
    var shake by remember { mutableStateOf(0) }
    LaunchedEffect(error) {
        if (error == null) return@LaunchedEffect
        shake++
        delay(450)
        pin = ""
    }
    PinPanel(
        title,
        error ?: message,
        isError = error != null,
        pin = pin,
        shake = shake,
        enabled = pin.length < PIN_LENGTH,
        onDigit = { digit ->
            if (error != null) onChange()
            pin += digit
            if (pin.length == PIN_LENGTH) onPin(pin)
        },
        onDelete = { pin = pin.dropLast(1) },
        modifier = modifier,
        fill = fill,
        bottomLeft = bottomLeft,
        footer = footer,
    )
}

/**
 * A new PIN then its confirmation, held outside the view so that the app bar's way back can lead from the
 * confirmation to the first PIN.
 */
@Stable
class NewPin {
    var first by mutableStateOf("")
    var second by mutableStateOf("")
    var confirming by mutableStateOf(false)
    var problem by mutableStateOf<String?>(null)
    var shake by mutableStateOf(0)

    fun restart() {
        first = ""
        second = ""
        confirming = false
    }

    /** Back from the confirmation to the first PIN. */
    fun back() {
        problem = null
        restart()
    }
}

/**
 * A new PIN, then its confirmation, on the same view and the same four dots: the title changes from one to the other.
 * A code too easy is refused at once; a confirmation that differs shakes and starts again.
 */
@Composable
fun NewPinEntry(
    state: NewPin,
    resetting: Boolean,
    /** Refused by the server. */
    error: String?,
    onChange: () -> Unit,
    onPin: (String) -> Unit,
    modifier: Modifier = Modifier,
    fill: Boolean = true,
) {
    LaunchedEffect(error) {
        if (error == null) return@LaunchedEffect
        state.shake++
        delay(450)
        state.restart()
    }
    val shown = error ?: state.problem
    PinPanel(
        title = if (state.confirming) "Confirme ton code" else if (resetting) "Nouveau code PIN" else "Crée ton code PIN",
        message = shown ?: if (state.confirming) "Tape-le à nouveau." else "4 chiffres, pas de suite simple.",
        isError = shown != null,
        pin = if (state.confirming) state.second else state.first,
        shake = state.shake,
        enabled = (if (state.confirming) state.second else state.first).length < PIN_LENGTH,
        onDigit = { digit ->
            if (error != null) onChange()
            state.problem = null
            if (!state.confirming) {
                state.first += digit
                if (state.first.length == PIN_LENGTH) {
                    if (state.first.tooEasy()) {
                        state.problem = "Trop facile. Choisis-en un autre."
                        state.shake++
                        state.first = ""
                    } else {
                        state.confirming = true
                    }
                }
            } else {
                state.second += digit
                if (state.second.length == PIN_LENGTH) {
                    if (state.second == state.first) {
                        onPin(state.first)
                    } else {
                        state.problem = "Les codes diffèrent. Recommence."
                        state.shake++
                        state.restart()
                    }
                }
            }
        },
        onDelete = { if (state.confirming) state.second = state.second.dropLast(1) else state.first = state.first.dropLast(1) },
        modifier = modifier,
        fill = fill,
    )
}

/** "Code PIN oublié ?", under the keypad. */
@Composable
fun ForgotLink(onClick: () -> Unit) {
    Text(
        "Code PIN oublié ?",
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(vertical = 10.dp),
        color = MaterialTheme.colorScheme.primary,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
    )
}

/** A short wrong-PIN message, with the tries left near the end. */
fun wrongPin(triesLeft: Int) = if (triesLeft <= 2) "Code incorrect. Encore $triesLeft essai${if (triesLeft > 1) "s" else ""}." else "Code incorrect."
