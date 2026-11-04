package bighid.romatazok.adherentapp.ui.events

import adherentapp.shared.generated.resources.Res
import adherentapp.shared.generated.resources.ic_calendar
import adherentapp.shared.generated.resources.ic_check
import adherentapp.shared.generated.resources.ic_map
import adherentapp.shared.generated.resources.ic_payments
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bighid.romatazok.adherentapp.ui.CalendarEntry
import bighid.romatazok.adherentapp.ui.receipt.ReceiptContent
import bighid.romatazok.adherentapp.ui.rememberAddToCalendar
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * One event in a bottom sheet, from the list, the home or the activity: its poster, what and where, then what
 * concerns me. The receipt opens in the same sheet: never a page more.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventSheet(
    event: UpcomingEvent,
    memberActive: Boolean,
    onDismiss: () -> Unit,
    onRenew: () -> Unit,
) {
    var showReceipt by remember { mutableStateOf(false) }
    val uri = LocalUriHandler.current
    val addToCalendar = rememberAddToCalendar()
    ModalBottomSheet(onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.surface) {
        val ticket = event.ticket
        if (showReceipt && ticket != null) {
            ReceiptContent(ticket, onDownload = {})
            return@ModalBottomSheet
        }
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            Poster(event, Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(22.dp)))
            Spacer(Modifier.height(16.dp))
            if (event.membersOnly) {
                Tag("Réservé aux adhérents", NAVY_LIGHT)
                Spacer(Modifier.height(8.dp))
            }
            Text(event.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 29.sp)
            Spacer(Modifier.height(4.dp))
            Text("${event.date} · ${event.time}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
            Text(event.venue, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Shortcut(Res.drawable.ic_map, "Itinéraire", Modifier.weight(1f)) {
                    runCatching { uri.openUri("https://www.google.com/maps/search/?api=1&query=${event.address.replace(" ", "+")}") }
                }
                addToCalendar?.let { add ->
                    Shortcut(Res.drawable.ic_calendar, "Mon agenda", Modifier.weight(1f)) {
                        add(CalendarEntry(event.name, event.address, event.startsAt, event.endsAt))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Mine(event, memberActive, onReceipt = { showReceipt = true }, onRenew = onRenew)
            event.description?.let {
                Spacer(Modifier.height(18.dp))
                Text(it, fontSize = 15.sp, lineHeight = 22.sp)
            }
            event.practicalInfo?.let {
                Spacer(Modifier.height(14.dp))
                Label("INFORMATIONS PRATIQUES")
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}

/** What concerns me: my price, my ticket, or why I cannot take one here. */
@Composable
private fun Mine(
    event: UpcomingEvent,
    memberActive: Boolean,
    onReceipt: () -> Unit,
    onRenew: () -> Unit,
) {
    val ticket = event.ticket
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CREAM).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Label("MON PRIX")
                Text(event.price, color = NAVY, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            if (ticket != null) Tag("Billet acheté", GREEN)
        }
        when {
            ticket != null -> {
                Spacer(Modifier.height(12.dp))
                Text("À l'entrée, montre ta carte.", color = NAVY, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(NAVY_LIGHT).clickable(onClick = onReceipt).padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(Res.drawable.ic_payments), null, Modifier.size(18.dp), tint = GOLD)
                    Spacer(Modifier.width(10.dp))
                    Text("Voir le reçu", Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(ticket.amount, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
            event.membersOnly && !memberActive -> {
                Spacer(Modifier.height(10.dp))
                Text("Renouvelle ton adhésion pour y participer.", color = NAVY, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Renouveler",
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ORANGE).clickable(onClick = onRenew).padding(vertical = 13.dp),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            !event.onlineSalesOpen -> {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(Res.drawable.ic_check), null, Modifier.size(16.dp), tint = ORANGE)
                    Spacer(Modifier.width(8.dp))
                    Text("Vente possible à l'accueil.", color = NAVY, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun Shortcut(
    icon: DrawableResource,
    label: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.background).clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), null, Modifier.size(18.dp), tint = NAVY_LIGHT)
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun Label(text: String) {
    Text(text, color = GOLD_DARK, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
}

@Composable
internal fun Tag(
    text: String,
    color: Color,
) {
    Text(
        text,
        Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp),
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
    )
}

/** The poster of the event, once plugged to the server; until then the colours of the card. */
@Composable
internal fun Poster(
    event: UpcomingEvent,
    modifier: Modifier,
) {
    Box(modifier.background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(NAVY_DARK, NAVY_LIGHT, ORANGE))), contentAlignment = Alignment.Center) {
        Icon(painterResource(Res.drawable.ic_calendar), null, Modifier.size(28.dp), tint = Color.White.copy(alpha = 0.85f))
    }
}

/** The colours of the card of PariBabi. */
internal val NAVY = Color(0xFF13264D)
internal val NAVY_LIGHT = Color(0xFF1D3770)
internal val NAVY_DARK = Color(0xFF0E1D3D)
internal val ORANGE = Color(0xFFEE6A2F)
internal val GOLD = Color(0xFFD9B77E)
internal val GOLD_DARK = Color(0xFFB08A4E)
internal val CREAM = Color(0xFFFBF6EF)
internal val GREEN = Color(0xFF12B76A)
