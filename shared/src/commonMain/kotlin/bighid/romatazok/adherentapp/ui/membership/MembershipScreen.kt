package bighid.romatazok.adherentapp.ui.membership

import adherentapp.shared.generated.resources.Res
import adherentapp.shared.generated.resources.ic_check
import adherentapp.shared.generated.resources.ic_chevron_right
import adherentapp.shared.generated.resources.ic_payments
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bighid.romatazok.adherentapp.ui.PageBar
import bighid.romatazok.adherentapp.ui.receipt.ReceiptContent
import bighid.romatazok.adherentapp.ui.receipt.ReceiptView
import org.jetbrains.compose.resources.painterResource

/**
 * Soft and light, in the colours of the card: the season and its three dates, how often I came, then every event of
 * it. An event and a receipt open in a bottom sheet: nothing goes deeper than this page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembershipScreen(
    state: MembershipState,
    onBack: () -> Unit,
    onRenew: () -> Unit = {},
) {
    var index by remember { mutableStateOf(0) }
    var filter by remember { mutableStateOf<Presence?>(null) }
    var opened by remember { mutableStateOf<SeasonEvent?>(null) }
    var receipt by remember { mutableStateOf<ReceiptView?>(null) }
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val current = state.seasons[index]
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        PageBar("Mon adhésion", onBack) { StatusChip(current.status) }
        // Older seasons on the left, like a calendar.
        SeasonSwitcher(
            current.name,
            canPrevious = index < state.seasons.lastIndex,
            canNext = index > 0,
            onPrevious = {
                index++
                filter = null
            },
            onNext = {
                index--
                filter = null
            },
        )
        AnimatedContent(
            index,
            transitionSpec = {
                val older = targetState > initialState
                (slideInHorizontally { if (older) -it / 4 else it / 4 } + fadeIn()) togetherWith
                    (slideOutHorizontally { if (older) it / 4 else -it / 4 } + fadeOut())
            },
        ) { shown ->
            val season = state.seasons[shown]
            val events = season.events.filter { filter == null || it.presence == filter }
            LazyColumn(contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = bottom + 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Summary(season, onReceipt = { receipt = it }, onRenew = onRenew) }
                item { Header(filter) { filter = it } }
                if (events.isEmpty()) {
                    item {
                        Text(
                            if (season.events.isEmpty()) "Les événements de la saison apparaîtront ici dès que le club les annoncera." else "Aucun événement ici.",
                            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                        )
                    }
                }
                items(events, key = { it.id }) { event -> EventCard(event) { opened = event } }
            }
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
            if (shown != null) ReceiptContent(shown, onDownload = {}) else EventContent(event) { receipt = it }
        }
    }
    // The receipt of the membership, opened from the summary.
    if (opened == null) {
        receipt?.let {
            ModalBottomSheet({ receipt = null }, containerColor = MaterialTheme.colorScheme.surface) { ReceiptContent(it, onDownload = {}) }
        }
    }
}

@Composable
private fun StatusChip(status: SeasonStatus) {
    val (label, color) =
        when (status) {
            SeasonStatus.ACTIVE -> "Active" to GREEN
            SeasonStatus.EXPIRED -> "Expirée" to ORANGE
            SeasonStatus.ENDED -> "Terminée" to MaterialTheme.colorScheme.onSurfaceVariant
        }
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SeasonSwitcher(
    name: String,
    canPrevious: Boolean,
    canNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 14.dp, top = 2.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("SAISON", color = ORANGE, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text(name, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
        if (canPrevious || canNext) {
            Arrow(enabled = canPrevious, flipped = true, onPrevious)
            Spacer(Modifier.width(10.dp))
            Arrow(enabled = canNext, flipped = false, onNext)
        }
    }
}

@Composable
private fun Arrow(
    enabled: Boolean,
    flipped: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(Res.drawable.ic_chevron_right),
            if (flipped) "Saison précédente" else "Saison suivante",
            Modifier.size(24.dp).rotate(if (flipped) 180f else 0f),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.25f),
        )
    }
}

/** The season on a soft panel: its three dates on a line, my presences, the receipt. */
@Composable
private fun Summary(
    season: SeasonView,
    onReceipt: (ReceiptView) -> Unit,
    onRenew: () -> Unit,
) {
    val past = season.events.filter { it.presence != Presence.UPCOMING }
    val present = past.count { it.presence == Presence.PRESENT }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
    ) {
        Dates(season)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Count(present, if (present > 1) "présences" else "présence", GREEN, Modifier.weight(1f))
            Count(past.size - present, if (past.size - present > 1) "absences" else "absence", NAVY_LIGHT, Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            season.receipt?.let { paid ->
                Row(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(NAVY_LIGHT)
                        .clickable { onReceipt(paid) }
                        .padding(vertical = 13.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(Res.drawable.ic_payments), null, Modifier.size(18.dp), tint = GOLD)
                    Spacer(Modifier.width(8.dp))
                    Text("Reçu de l'adhésion", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
            if (season.status == SeasonStatus.EXPIRED) {
                Text(
                    "Renouveler",
                    Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(ORANGE).clickable(onClick = onRenew).padding(vertical = 13.dp),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Start of the season, my membership, end of the season: three points on a gold line. */
@Composable
private fun Dates(season: SeasonView) {
    Box(Modifier.fillMaxWidth()) {
        Box(Modifier.padding(start = 6.dp, end = 6.dp).offset(y = 5.dp).fillMaxWidth().height(2.dp).background(GOLD.copy(alpha = 0.45f)))
        Row(Modifier.fillMaxWidth()) {
            DatePoint("Début", season.startsOn, GOLD, Alignment.Start, Modifier.weight(1f))
            DatePoint("Mon adhésion", season.joinedOn, ORANGE, Alignment.CenterHorizontally, Modifier.weight(1f))
            DatePoint("Fin", season.endsOn, GOLD, Alignment.End, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DatePoint(
    label: String,
    date: String,
    color: Color,
    alignment: Alignment.Horizontal,
    modifier: Modifier,
) {
    Column(modifier, horizontalAlignment = alignment) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface).border(3.dp, color, CircleShape))
        Spacer(Modifier.height(8.dp))
        Text(label.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(date, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Count(
    value: Int,
    label: String,
    color: Color,
    modifier: Modifier,
) {
    Row(
        modifier.clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = 0.08f)).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(value.toString(), color = color, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
    }
}

/** "Événements" and a soft segmented filter, the selection gliding from one choice to the other. */
@Composable
private fun Header(
    filter: Presence?,
    onFilter: (Presence?) -> Unit,
) {
    val choices = listOf(null to "Tous", Presence.PRESENT to "Présent", Presence.ABSENT to "Absent")
    Column(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 4.dp)) {
        Text("Événements", Modifier.padding(start = 10.dp, bottom = 10.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        BoxWithConstraints(
            Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surface).padding(4.dp),
        ) {
            val width = maxWidth / choices.size
            val selected = choices.indexOfFirst { it.first == filter }
            val x by animateDpAsState(width * selected)
            Box(Modifier.offset(x = x).width(width).fillMaxHeight().clip(RoundedCornerShape(50)).background(NAVY_LIGHT))
            Row(Modifier.fillMaxSize()) {
                choices.forEachIndexed { i, (value, label) ->
                    val color by animateColorAsState(if (i == selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(50)).clickable { onFilter(value) }, contentAlignment = Alignment.Center) {
                        Text(label, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/** One event on a soft card: its date like a calendar sheet, where and when, and my presence. */
@Composable
private fun EventCard(
    event: SeasonEvent,
    onClick: () -> Unit,
) {
    val (label, color) = event.presence.look()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DateSheet(event)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(event.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${event.time} · ${event.venue}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(6.dp))
                Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                event.detail?.let { Text("  ·  $it", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
        Icon(painterResource(Res.drawable.ic_chevron_right), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
    }
}

@Composable
private fun DateSheet(event: SeasonEvent) {
    val upcoming = event.presence == Presence.UPCOMING
    Column(
        Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (upcoming) NAVY_LIGHT else CREAM),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(event.day, color = if (upcoming) Color.White else NAVY, fontSize = 19.sp, fontWeight = FontWeight.Bold, lineHeight = 21.sp)
        Text(event.month, color = if (upcoming) GOLD else ORANGE, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 14.sp)
    }
}

@Composable
private fun Presence.look(): Pair<String, Color> =
    when (this) {
        Presence.PRESENT -> "Présent" to GREEN
        Presence.ABSENT -> "Absent" to MaterialTheme.colorScheme.onSurfaceVariant
        Presence.UPCOMING -> "À venir" to NAVY_LIGHT
    }

/** The event in a bottom sheet: when, where, my presence, and its receipt in the same sheet. */
@Composable
private fun EventContent(
    event: SeasonEvent,
    onReceipt: (ReceiptView) -> Unit,
) {
    val (label, color) = event.presence.look()
    Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DateSheet(event)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(event.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${event.time} · ${event.venue}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = 0.10f)).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (event.presence == Presence.PRESENT) {
                Icon(painterResource(Res.drawable.ic_check), null, Modifier.size(20.dp), tint = color)
                Spacer(Modifier.width(10.dp))
            }
            Column {
                Text(label, color = color, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                event.detail?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }
            }
        }
        event.receipt?.let { paid ->
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(NAVY_LIGHT)
                    .clickable { onReceipt(paid) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(Res.drawable.ic_payments), null, Modifier.size(18.dp), tint = GOLD)
                Spacer(Modifier.width(10.dp))
                Text("Voir le reçu", Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(paid.amount, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** The colours of the card of PariBabi. */
private val NAVY = Color(0xFF13264D)
private val NAVY_LIGHT = Color(0xFF1D3770)
private val ORANGE = Color(0xFFEE6A2F)
private val GOLD = Color(0xFFD9B77E)
private val CREAM = Color(0xFFFBF6EF)
private val GREEN = Color(0xFF12B76A)
