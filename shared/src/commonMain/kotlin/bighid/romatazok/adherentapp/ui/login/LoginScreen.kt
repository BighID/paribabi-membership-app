package bighid.romatazok.adherentapp.ui.login

import adherentapp.shared.generated.resources.Res
import adherentapp.shared.generated.resources.ic_smartphone
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bighid.romatazok.adherentapp.ui.SystemBack
import bighid.romatazok.adherentapp.ui.events.CREAM
import bighid.romatazok.adherentapp.ui.events.GOLD
import bighid.romatazok.adherentapp.ui.events.NAVY_LIGHT
import bighid.romatazok.adherentapp.ui.events.ORANGE
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

private enum class Step { PHONE, CODE, PIN, CREATE_PIN, CONFIRM }

private enum class Takeover { ASKING, REPORTING, REPORTED }

/**
 * One screen before the home: the band with the card stays, the steps glide under it. On its usual phone: the number,
 * then the PIN. On another phone: the number, a code sent, the PIN, and when another phone is connected, its agreement
 * or the team's. From another phone, a known number and an unknown one go through exactly the same screens.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
    service: LoginService,
    /** Said once on arrival, after this phone was signed out by another one. */
    notice: String?,
    /** The team handed the account to another phone: the notice comes with "Ce n'était pas moi". */
    takenOver: Boolean = false,
    /** With the number signed in. */
    onSignedIn: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val uri = LocalUriHandler.current
    var step by remember { mutableStateOf(Step.PHONE) }
    var country by remember { mutableStateOf(Country.CI) }
    var number by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // Changes with each code sent, to count the 60 seconds before another one.
    var sentAt by remember { mutableStateOf(0) }
    // A new PIN after "Code PIN oublié ?", rather than a first one.
    var resetting by remember { mutableStateOf(false) }
    var takeover by remember { mutableStateOf(if (takenOver) Takeover.ASKING else null) }
    val newPin = remember { NewPin() }
    val phone = number
    val shownNumber = "${country.dial} ${number.grouped()}"

    fun back() {
        error = null
        if (step == Step.CREATE_PIN && newPin.confirming) {
            newPin.back()
        } else {
            newPin.restart()
            step = Step.PHONE
        }
    }
    SystemBack(step != Step.PHONE) { back() }

    fun started(answer: Start) {
        when (answer) {
            Start.Pin -> step = Step.PIN
            Start.CodeSent -> {
                sentAt++
                step = Step.CODE
            }
            is Start.TooMany -> error = "Trop de demandes. Réessaie dans ${answer.minutes} min."
        }
    }

    fun start() {
        busy = true
        error = null
        resetting = false
        scope.launch {
            started(service.start(phone))
            busy = false
        }
    }

    fun forgot() {
        error = null
        resetting = true
        scope.launch { started(service.forgotPin(phone)) }
    }

    /** What a PIN, typed or chosen, leads to. */
    fun pinAnswer(check: PinCheck) {
        when (check) {
            PinCheck.SignedIn -> onSignedIn(phone)
            PinCheck.OtherPhone -> step = Step.CONFIRM
            is PinCheck.Wrong -> error = wrongPin(check.triesLeft)
            PinCheck.Locked -> error = "Code bloqué. Touche « Code PIN oublié ? »."
            PinCheck.TooEasy -> error = "Trop facile. Choisis-en un autre."
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).navigationBarsPadding().imePadding()) {
        val pinStep = step == Step.PIN || step == Step.CREATE_PIN
        val keyboard = WindowInsets.isImeVisible
        AuthHero(
            onBack = if (step == Step.PHONE) null else ::back,
            size =
                when {
                    pinStep -> HeroSize.KEYPAD
                    keyboard -> HeroSize.KEYBOARD
                    else -> HeroSize.FULL
                },
        )
        AnimatedContent(
            step,
            Modifier.weight(1f),
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                (slideInHorizontally { if (forward) it else -it } + fadeIn()) togetherWith (slideOutHorizontally { if (forward) -it else it } + fadeOut())
            },
        ) { current ->
            when (current) {
                Step.PHONE ->
                    FormStep {
                        PhoneStep(
                            notice,
                            takeover,
                            onNotMe = {
                                takeover = Takeover.REPORTING
                                scope.launch {
                                    service.reportNotMe()
                                    takeover = Takeover.REPORTED
                                }
                            },
                            country,
                            number,
                            busy,
                            error,
                            onCountry = { country = it },
                            onNumber = {
                                number = it
                                error = null
                            },
                            onSend = ::start,
                            onApply = { runCatching { uri.openUri(APPLY_URL) } },
                        )
                    }
                Step.CODE ->
                    FormStep {
                        CodeStep(
                            title = if (resetting) "Code PIN oublié" else "Ton code",
                            number = shownNumber,
                            sentAt = sentAt,
                            error = error,
                            onChange = { error = null },
                            onResend = { if (resetting) forgot() else start() },
                            onCode = { code ->
                                scope.launch {
                                    when (val check = service.checkCode(phone, code)) {
                                        CodeCheck.CreatePin -> step = Step.CREATE_PIN
                                        CodeCheck.EnterPin -> step = Step.PIN
                                        CodeCheck.Wrong -> error = "Code incorrect ou expiré."
                                        is CodeCheck.Blocked -> error = "Trop d'essais. Réessaie dans ${check.minutes} min."
                                    }
                                }
                            },
                        )
                    }
                Step.PIN ->
                    PinEntry(
                        title = "Ton code PIN",
                        message = shownNumber,
                        error = error,
                        onChange = { error = null },
                        onPin = { pin -> scope.launch { pinAnswer(service.checkPin(phone, pin)) } },
                        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                        footer = { ForgotLink(::forgot) },
                    )
                Step.CREATE_PIN ->
                    NewPinEntry(
                        state = newPin,
                        resetting = resetting,
                        error = error,
                        onChange = { error = null },
                        onPin = { pin -> scope.launch { pinAnswer(service.createPin(phone, pin)) } },
                        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 0.dp).padding(bottom = 16.dp),
                    )
                Step.CONFIRM ->
                    FormStep {
                        ConfirmStep(
                            service.answerWindowSeconds,
                            onTeam = { runCatching { uri.openUri(TEAM_URL) } },
                        ) {
                            scope.launch {
                                when (service.awaitOtherPhone(phone)) {
                                    OtherPhoneAnswer.Accepted -> onSignedIn(phone)
                                    OtherPhoneAnswer.Refused -> {
                                        step = Step.PHONE
                                        error = "Demande refusée par ton autre téléphone."
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }
}

/**
 * The steps that are not a PIN. They never scroll: with the keyboard open, the top shrinks to leave them whole above
 * it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormStep(content: @Composable ColumnScope.() -> Unit) {
    // Rises with the top, at the same pace.
    val top by animateDpAsState(if (WindowInsets.isImeVisible) 14.dp else 28.dp, tween(280))
    Column(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = top, bottom = 16.dp), content = content)
}

@Composable
private fun PhoneStep(
    notice: String?,
    takeover: Takeover?,
    onNotMe: () -> Unit,
    country: Country,
    number: String,
    busy: Boolean,
    error: String?,
    onCountry: (Country) -> Unit,
    onNumber: (String) -> Unit,
    onSend: () -> Unit,
    onApply: () -> Unit,
) {
    var choosing by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val complete = number.length == NUMBER_LENGTH
    Title("Connexion")
    Line("Ton numéro donné au club.")
    when (takeover) {
        null -> notice?.let {
            Spacer(Modifier.height(14.dp))
            Notice(it)
        }
        Takeover.REPORTED -> {
            Spacer(Modifier.height(14.dp))
            Notice("C'est fait : l'autre téléphone est déconnecté, ton QR est changé.")
        }
        else -> {
            Spacer(Modifier.height(14.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(CREAM).padding(14.dp)) {
                Text(notice.orEmpty(), color = NAVY_LIGHT, fontSize = 14.sp, lineHeight = 19.sp)
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.5.dp, ORANGE, RoundedCornerShape(12.dp))
                        .clickable(enabled = takeover == Takeover.ASKING, onClick = onNotMe),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (takeover == Takeover.REPORTING) "Envoi…" else "Ce n'était pas moi", color = ORANGE, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
    Spacer(Modifier.height(20.dp))
    Row(Modifier.fillMaxWidth().height(58.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box {
            Row(
                Modifier.height(58.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).clickable { choosing = true }.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(country.code, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(6.dp))
                Text(country.dial, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
            DropdownMenu(choosing, { choosing = false }) {
                Country.entries.forEach {
                    DropdownMenuItem(
                        text = { Text(if (it == Country.CI) "Côte d'Ivoire  ${it.dial}" else "France  ${it.dial}") },
                        onClick = {
                            onCountry(it)
                            choosing = false
                        },
                    )
                }
            }
        }
        BasicTextField(
            number,
            { typed -> onNumber(typed.filter(Char::isDigit).take(NUMBER_LENGTH)) },
            Modifier.weight(1f).height(58.dp).focusRequester(focus),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
            cursorBrush = SolidColor(ORANGE),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            visualTransformation = PairsTransformation,
            decorationBox = { field ->
                Box(
                    Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (number.isEmpty()) Text("07 00 00 00 00", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 18.sp)
                    field()
                }
            },
        )
    }
    Error(error)
    Spacer(Modifier.height(18.dp))
    MainButton(if (busy) "Un instant…" else "Continuer", enabled = complete && !busy, onClick = onSend)
    Spacer(Modifier.height(24.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Text("Pas encore adhérent ? ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text("Candidater", Modifier.clickable(onClick = onApply), color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CodeStep(
    title: String,
    number: String,
    /** Changes with each code sent, to restart the 60 seconds. */
    sentAt: Int,
    error: String?,
    onChange: () -> Unit,
    onResend: () -> Unit,
    onCode: (String) -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var wait by remember { mutableStateOf(RESEND_SECONDS) }
    val shake = remember { Animatable(0f) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    LaunchedEffect(sentAt) {
        wait = RESEND_SECONDS
        while (wait > 0) {
            delay(1_000)
            wait--
        }
    }
    LaunchedEffect(error) {
        if (error == null) return@LaunchedEffect
        // A soft shake, and the boxes empty for another try.
        for (x in listOf(14f, -12f, 9f, -6f, 3f, 0f)) shake.animateTo(x, tween(55))
        code = ""
        // A wrong code stays on this step: the keyboard comes back for another try.
        focus.requestFocus()
        keyboard?.show()
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Title(title)
    // Always the same line: nothing says whether the number is known, nor by which channel.
    Line("Si ce numéro est connu, un code arrive par email ou WhatsApp.")
    Spacer(Modifier.height(6.dp))
    Text(number, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(20.dp))
    BasicTextField(
        code,
        { typed ->
            val digits = typed.filter(Char::isDigit).take(CODE_LENGTH)
            if (digits != code) onChange()
            code = digits
            // Typed or pasted, the code is checked as soon as it is whole; the keyboard goes away first, the PIN
            // has its own keypad.
            if (digits.length == CODE_LENGTH) {
                keyboard?.hide()
                focusManager.clearFocus()
                onCode(digits)
            }
        },
        Modifier.fillMaxWidth().offset(x = shake.value.dp).focusRequester(focus),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        cursorBrush = SolidColor(Color.Transparent),
        decorationBox = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(CODE_LENGTH) { i ->
                    val filled = i < code.length
                    val current = i == code.length
                    Box(
                        Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(if (current || error != null) 2.dp else 0.dp, if (error != null) ORANGE else if (current) NAVY_LIGHT else Color.Transparent, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(if (filled) code[i].toString() else "", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
    )
    Error(error)
    Spacer(Modifier.height(18.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Pense aux spams.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Text(
            if (wait > 0) "Renvoyer dans $wait s" else "Renvoyer le code",
            Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = wait == 0, onClick = onResend).padding(4.dp),
            color = if (wait > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Two phones and a line pulsing between them, while the other phone is asked. Past the window, the request keeps
 * waiting, and the team is offered: nothing opens without one of them.
 */
@Composable
private fun ConfirmStep(
    seconds: Int,
    onTeam: () -> Unit,
    onStart: () -> Unit,
) {
    var left by remember { mutableStateOf(seconds) }
    LaunchedEffect(Unit) {
        onStart()
        while (left > 0) {
            delay(1_000)
            left--
        }
    }
    val pulse by rememberInfiniteTransition().animateFloat(0f, 1f, infiniteRepeatable(tween(1_400, easing = LinearEasing), RepeatMode.Restart))
    val waiting = left == 0
    Title(if (waiting) "En attente d'accord" else "Confirme sur ton autre téléphone")
    Line(if (waiting) "Ton autre téléphone ou l'équipe doit valider." else "Touche Oui dans la notification.")
    Spacer(Modifier.height(28.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        PhoneBadge(NAVY_LIGHT)
        Row(Modifier.width(120.dp).padding(horizontal = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            repeat(5) { i ->
                // Each dot lights up in turn, from the new phone to the other one.
                val lit = ((pulse * 5).toInt() == i)
                Box(Modifier.size(8.dp).clip(CircleShape).background(if (lit) ORANGE else GOLD.copy(alpha = 0.35f)))
            }
        }
        PhoneBadge(ORANGE)
    }
    Spacer(Modifier.height(28.dp))
    if (waiting) {
        MainButton("Contacter l'équipe", enabled = true, onClick = onTeam)
        Spacer(Modifier.height(10.dp))
        Text("Demande ouverte 7 jours.", Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, textAlign = TextAlign.Center)
    } else {
        Text(
            "Encore ${left / 60}:${(left % 60).toString().padStart(2, '0')}",
            Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PhoneBadge(color: Color) {
    Box(Modifier.size(64.dp).clip(CircleShape).background(color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
        Icon(painterResource(Res.drawable.ic_smartphone), null, Modifier.size(30.dp), tint = color)
    }
}

@Composable
internal fun Title(text: String) {
    Text(text, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 31.sp)
    Spacer(Modifier.height(4.dp))
}

/** One short line under a title. */
@Composable
private fun Line(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, lineHeight = 21.sp)
}

@Composable
private fun Error(text: String?) {
    text ?: return
    Spacer(Modifier.height(10.dp))
    Text(text, color = MaterialTheme.colorScheme.error, fontSize = 14.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun Notice(text: String) {
    Text(text, Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(CREAM).padding(14.dp), color = NAVY_LIGHT, fontSize = 14.sp, lineHeight = 19.sp)
}

@Composable
private fun MainButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) ORANGE else ORANGE.copy(alpha = 0.35f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** "0701020304" shown "07 01 02 03 04". */
private object PairsTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString) =
        TransformedText(
            AnnotatedString(text.text.grouped()),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = offset + maxOf(0, (offset - 1) / 2)

                override fun transformedToOriginal(offset: Int) = offset - offset / 3
            },
        )
}

private fun String.grouped() = chunked(2).joinToString(" ")

private const val NUMBER_LENGTH = 10
private const val CODE_LENGTH = 6
private const val RESEND_SECONDS = 60
private const val APPLY_URL = "https://paribabi.com/adherer"

/** The number of the club written on the card. */
private const val TEAM_URL = "https://wa.me/2250768339659"
