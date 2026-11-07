package bighid.romatazok.adherentapp.ui.home

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bighid.romatazok.adherentapp.ui.Loader
import bighid.romatazok.adherentapp.ui.events.NAVY_LIGHT
import bighid.romatazok.adherentapp.ui.login.PinEntry
import bighid.romatazok.adherentapp.ui.theme.Brand
import kotlinx.coroutines.launch

private enum class EmailStep { PIN, CODE, DONE }

/**
 * The email receives the login codes: to change it, the PIN, then the code sent to the new address. The record
 * keeps the old one until the code comes back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailChangeSheet(
    email: String,
    /** Null when the PIN was right and the code left; otherwise what to say. */
    onPin: suspend (String) -> String?,
    onCode: suspend (String) -> String?,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(EmailStep.PIN) }
    var error by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().imePadding().padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
            Text("Changer mon email", Modifier.fillMaxWidth(), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            AnimatedContent(step) { current ->
                when (current) {
                    EmailStep.PIN ->
                        PinEntry(
                            title = "Ton code PIN",
                            message = "Pour confirmer que c'est toi.",
                            error = error,
                            onChange = { error = null },
                            onPin = { pin ->
                                checking = true
                                scope.launch {
                                    val problem = onPin(pin)
                                    checking = false
                                    if (problem == null) step = EmailStep.CODE else error = problem
                                }
                            },
                            fill = false,
                            checking = checking,
                        )
                    EmailStep.CODE ->
                        CodeStep(email, error, checking, onChange = { error = null }) { code ->
                            checking = true
                            scope.launch {
                                val problem = onCode(code)
                                checking = false
                                if (problem == null) step = EmailStep.DONE else error = problem
                            }
                        }
                    EmailStep.DONE ->
                        Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Email changé", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(email, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(22.dp))
                            Text(
                                "Fermer",
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(NAVY_LIGHT).clickable(onClick = onDone).padding(vertical = 15.dp),
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

/** The six digits sent to the new address, checked as soon as they are whole. */
@Composable
private fun CodeStep(
    email: String,
    error: String?,
    /** The code is being checked: a small wheel in place of the error line, the boxes held. */
    checking: Boolean,
    onChange: () -> Unit,
    onCode: (String) -> Unit,
) {
    var code by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(error) { if (error != null) code = "" }
    Column(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Le code reçu", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text("Envoyé à $email", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        BasicTextField(
            code,
            { typed ->
                val digits = typed.filter(Char::isDigit).take(6)
                if (digits != code) onChange()
                code = digits
                if (digits.length == 6) onCode(digits)
            },
            Modifier.fillMaxWidth().focusRequester(focus),
            readOnly = checking,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            cursorBrush = SolidColor(Color.Transparent),
            decorationBox = {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(6) { i ->
                        Box(
                            Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(2.dp, if (error != null) Brand.Orange else if (i == code.length) NAVY_LIGHT else Color.Transparent, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(code.getOrNull(i)?.toString().orEmpty(), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
        )
        Box(Modifier.fillMaxWidth().height(40.dp).padding(top = 10.dp), contentAlignment = Alignment.TopCenter) {
            if (checking) {
                Loader(size = 20.dp)
            } else {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp) }
            }
        }
    }
}
