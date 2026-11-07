package bighid.romatazok.adherentapp.ui.membership

import adherentapp.shared.generated.resources.Res
import adherentapp.shared.generated.resources.ic_chevron_right
import adherentapp.shared.generated.resources.ic_edit
import adherentapp.shared.generated.resources.ic_person
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bighid.romatazok.adherentapp.ui.Hair
import bighid.romatazok.adherentapp.ui.Loader
import bighid.romatazok.adherentapp.ui.PageBar
import bighid.romatazok.adherentapp.ui.SoftLabel
import bighid.romatazok.adherentapp.ui.SoftRow
import bighid.romatazok.adherentapp.ui.SoftTabs
import bighid.romatazok.adherentapp.ui.StateDot
import bighid.romatazok.adherentapp.ui.events.ORANGE
import bighid.romatazok.adherentapp.ui.home.HomeState
import bighid.romatazok.adherentapp.ui.home.MemberStatus
import bighid.romatazok.adherentapp.ui.home.Profile
import bighid.romatazok.adherentapp.ui.receipt.ReceiptPanel
import bighid.romatazok.adherentapp.ui.rememberPhotoPicker
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/** The two sides of the page. */
enum class MembershipTab { SEASON, PROFILE }

/** What the member changes from "Mon profil"; a suspended answer is null once the server accepted. */
class ProfileActions(
    val onSave: suspend (Profile) -> String?,
    /** A new address: the PIN, then its code, in their own sheet. */
    val onEmail: (String) -> Unit,
    val onPhoto: suspend (ByteArray) -> String?,
    val lockOnOpen: Boolean,
    val onLockOnOpen: (Boolean) -> Unit,
    val onChangePin: () -> Unit,
    val onLogout: () -> Unit,
)

private val SIDE = 20.dp
private val PRESENT = Color(0xFF3DBE8B)
private val ABSENT = Color(0xFFD5D9E2)

/**
 * All that concerns me, on one page: who I am at the top, then my season or my profile. An event, a receipt or a line
 * to change open in a bottom sheet: nothing goes deeper than this page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembershipScreen(
    state: MembershipState,
    home: HomeState,
    tab: MembershipTab,
    onTab: (MembershipTab) -> Unit,
    onBack: () -> Unit,
    /** Opens the payment page; null once it is open, otherwise what to tell. */
    onRenew: suspend () -> String?,
    actions: ProfileActions,
) {
    val scope = rememberCoroutineScope()
    var sending by remember { mutableStateOf(false) }
    var photoError by remember { mutableStateOf<String?>(null) }
    val picker =
        rememberPhotoPicker { bytes ->
            sending = true
            photoError = null
            scope.launch {
                photoError = actions.onPhoto(bytes)
                sending = false
            }
        }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        PageBar("", onBack)
        Hero(home, tab, sending, photoError, if (tab == MembershipTab.PROFILE) picker else null, onRenew)
        SoftTabs(listOf("Ma saison", "Mon profil"), tab.ordinal, { onTab(MembershipTab.entries[it]) })
        AnimatedContent(tab, Modifier.weight(1f), transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }) { shown ->
            when (shown) {
                MembershipTab.SEASON -> SeasonTab(state)
                MembershipTab.PROFILE -> ProfileTab(home.profile, actions)
            }
        }
    }
}

/** The round photo, the name, one line, and the one action when the membership has ended. */
@Composable
private fun Hero(
    home: HomeState,
    tab: MembershipTab,
    sending: Boolean,
    photoError: String?,
    onPhoto: (() -> Unit)?,
    onRenew: suspend () -> String?,
) {
    val scope = rememberCoroutineScope()
    var renewing by remember { mutableStateOf(false) }
    var renewError by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxWidth().padding(start = SIDE, end = SIDE, bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Photo(home.profile.photo, sending, onPhoto)
        Spacer(Modifier.height(10.dp))
        Text(home.profile.fullName, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        if (tab == MembershipTab.PROFILE) {
            Text(listOf(home.card.memberNumber, home.profile.phone).filter { it.isNotBlank() }.joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        } else {
            val (dot, line) =
                when (val status = home.status) {
                    is MemberStatus.Active -> PRESENT to "Adhérent jusqu'au ${status.until}"
                    is MemberStatus.Expired -> ORANGE to "Adhésion expirée le ${status.since}"
                    MemberStatus.AwaitingPayment -> ORANGE to "Candidature acceptée"
                }
            Row(verticalAlignment = Alignment.CenterVertically) {
                StateDot(dot, Modifier.size(7.dp))
                Spacer(Modifier.width(6.dp))
                Text(line, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        }
        photoError?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
        if (home.status is MemberStatus.Expired && tab == MembershipTab.SEASON) {
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(ORANGE)
                    .clickable(enabled = !renewing) {
                        renewing = true
                        renewError = null
                        scope.launch {
                            renewError = onRenew()
                            renewing = false
                        }
                    }.padding(horizontal = 26.dp, vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (renewing) Loader(size = 20.dp, onAccent = true) else Text("Renouveler", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            renewError?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

/** Round and pale; a tap opens the gallery, where a small round key says so. */
@Composable
private fun Photo(
    photo: ImageBitmap?,
    sending: Boolean,
    onPick: (() -> Unit)?,
) {
    Box {
        Box(
            Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(enabled = onPick != null && !sending) { onPick?.invoke() },
            contentAlignment = Alignment.Center,
        ) {
            if (photo != null) {
                Image(photo, "Ma photo", Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(painterResource(Res.drawable.ic_person), null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f))
            }
            if (sending) {
                Box(Modifier.matchParentSize().background(Color.White.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) { Loader(size = 26.dp) }
            }
        }
        if (onPick != null) {
            Box(
                Modifier.align(Alignment.BottomEnd).size(28.dp).shadow(3.dp, CircleShape).clip(CircleShape).background(MaterialTheme.colorScheme.surface).clickable(enabled = !sending, onClick = onPick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(Res.drawable.ic_edit), if (photo == null) "Ajouter une photo" else "Changer la photo", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/** One sentence on the season, its receipt, then its events. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeasonTab(state: MembershipState) {
    var index by remember { mutableStateOf(0) }
    var opened by remember { mutableStateOf<SeasonEvent?>(null) }
    // The payment whose receipt is open: the sheet opens at once and reads it.
    var receipt by remember { mutableStateOf<String?>(null) }
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    if (state.seasons.isEmpty()) {
        Text(
            "Ton adhésion apparaîtra ici dès qu'elle sera payée.",
            Modifier.fillMaxWidth().padding(32.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        return
    }
    val season = state.seasons[index.coerceAtMost(state.seasons.lastIndex)]
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = SIDE, end = SIDE, top = 14.dp, bottom = bottom + 24.dp)) {
        // Older seasons on the left, like a calendar.
        if (state.seasons.size > 1) {
            item {
                SeasonArrows(
                    season.name,
                    canPrevious = index < state.seasons.lastIndex,
                    canNext = index > 0,
                    onPrevious = { index++ },
                    onNext = { index-- },
                )
                Spacer(Modifier.height(10.dp))
            }
        }
        item { Summary(season, showName = state.seasons.size == 1) }
        item {
            Spacer(Modifier.height(8.dp))
            SoftRow("Date d'adhésion", value = season.joinedOn)
        }
        season.receipt?.let { paid ->
            item {
                Hair()
                SoftRow("Mon reçu d'adhésion", onClick = { receipt = paid })
            }
        }
        item {
            Spacer(Modifier.height(10.dp))
            SoftLabel("Événements")
        }
        if (season.events.isEmpty()) {
            item {
                Text(
                    "Les événements de la saison apparaîtront ici dès que le club les annoncera.",
                    Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
            }
        }
        itemsIndexed(season.events, key = { _, event -> event.id }) { position, event ->
            if (position > 0) Hair()
            val (word, color) = event.presence.look()
            SoftRow(event.name, detail = "${event.day} ${event.month} · $word", leading = { StateDot(color) }, onClick = { opened = event })
        }
    }
    opened?.let { event ->
        ModalBottomSheet(
            {
                opened = null
                receipt = null
            },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            val shown = receipt
            if (shown != null) ReceiptPanel(shown) else EventContent(event) { receipt = it }
        }
    }
    // The receipt of the membership, opened from its line.
    if (opened == null) {
        receipt?.let {
            ModalBottomSheet({ receipt = null }, containerColor = MaterialTheme.colorScheme.surface) { ReceiptPanel(it) }
        }
    }
}

/** The name of the season between its two arrows, when there is more than one. */
@Composable
private fun SeasonArrows(
    name: String,
    canPrevious: Boolean,
    canNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Arrow(canPrevious, flipped = true, onPrevious)
        Text(name, Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Arrow(canNext, flipped = false, onNext)
    }
}

@Composable
private fun Arrow(
    enabled: Boolean,
    flipped: Boolean,
    onClick: () -> Unit,
) {
    Box(Modifier.size(44.dp).clip(CircleShape).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(
            painterResource(Res.drawable.ic_chevron_right),
            if (flipped) "Saison précédente" else "Saison suivante",
            Modifier.size(20.dp).rotate(if (flipped) 180f else 0f),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.2f),
        )
    }
}

/** A pale panel: the ring of my presences and one sentence. */
@Composable
private fun Summary(
    season: SeasonView,
    showName: Boolean,
) {
    val past = season.events.filter { it.presence != Presence.UPCOMING }
    val present = past.count { it.presence == Presence.PRESENT }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Ring(if (past.isEmpty()) 0f else present.toFloat() / past.size)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(if (past.isEmpty()) "Pas encore d'événement passé" else "$present ${if (present > 1) "présences" else "présence"} sur ${past.size}", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            if (showName) Text(season.name, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
    }
}

/** How often I came, drawn: it fills softly when shown. */
@Composable
private fun Ring(share: Float) {
    val filled by animateFloatAsState(share, tween(700))
    val track = MaterialTheme.colorScheme.outline
    Canvas(Modifier.size(58.dp)) {
        val stroke = 6.5.dp.toPx()
        val inset = stroke / 2
        val box = Size(size.width - stroke, size.height - stroke)
        drawArc(track, 0f, 360f, useCenter = false, topLeft = Offset(inset, inset), size = box, style = Stroke(stroke))
        if (filled > 0f) {
            drawArc(PRESENT, -90f, 360f * filled, useCenter = false, topLeft = Offset(inset, inset), size = box, style = Stroke(stroke, cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun Presence.look(): Pair<String, Color> =
    when (this) {
        Presence.PRESENT -> "Présent" to PRESENT
        Presence.ABSENT -> "Absent" to ABSENT
        Presence.UPCOMING -> "À venir" to ORANGE
    }

/** The event in a bottom sheet: when, where, my presence, and its receipt in the same sheet. */
@Composable
private fun EventContent(
    event: SeasonEvent,
    onReceipt: (String) -> Unit,
) {
    val (word, color) = event.presence.look()
    Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 28.dp)) {
        Text(event.name, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text("${event.day} ${event.month}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(listOf(event.time, event.venue).filter { it.isNotBlank() }.joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Spacer(Modifier.height(14.dp))
        SoftRow(word, detail = event.detail, leading = { StateDot(color) })
        event.receipt?.let { paid ->
            Hair()
            SoftRow("Voir le reçu", onClick = { onReceipt(paid) })
        }
    }
}
