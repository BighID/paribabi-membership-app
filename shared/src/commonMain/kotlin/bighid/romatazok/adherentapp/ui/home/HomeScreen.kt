package bighid.romatazok.adherentapp.ui.home

import adherentapp.shared.generated.resources.Res
import adherentapp.shared.generated.resources.ic_bell
import adherentapp.shared.generated.resources.ic_calendar
import adherentapp.shared.generated.resources.ic_card
import adherentapp.shared.generated.resources.ic_check
import adherentapp.shared.generated.resources.ic_chevron_right
import adherentapp.shared.generated.resources.ic_edit
import adherentapp.shared.generated.resources.ic_eye
import adherentapp.shared.generated.resources.ic_eye_off
import adherentapp.shared.generated.resources.ic_payments
import adherentapp.shared.generated.resources.ic_qr
import adherentapp.shared.generated.resources.wordmark_dark
import adherentapp.shared.generated.resources.wordmark_light
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bighid.romatazok.adherentapp.ui.Loader
import bighid.romatazok.adherentapp.ui.theme.Brand
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * The whole app starts here, without tabs: the card and its two gestures, my membership in one line, the nearest
 * event, and what happened. Each line opens what it is about.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeState,
    onMembership: () -> Unit,
    onEvent: (String) -> Unit,
    onAllEvents: () -> Unit,
    onActivity: (ActivityRow) -> Unit,
    onAllActivity: () -> Unit,
    onEditProfile: () -> Unit,
    /** Pulled down: the home is read again, the ring turning until it comes. */
    refreshing: Boolean = false,
    onRefresh: () -> Unit = {},
) {
    var masked by remember { mutableStateOf(false) }
    var showBack by remember { mutableStateOf(false) }
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val pull = rememberPullToRefreshState()
    PullToRefreshBox(
        refreshing,
        onRefresh,
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding(),
        state = pull,
        indicator = { RefreshRing(refreshing, pull.distanceFraction, Modifier.align(Alignment.TopCenter)) },
    ) {
        HomeList(state, masked, showBack, bottom, onMask = { masked = !masked }, onFlip = { showBack = !showBack }, onMembership, onEvent, onAllEvents, onActivity, onAllActivity, onEditProfile)
    }
}

/** The ring of the app in a small white disc, coming down with the finger and staying while the home is read. */
@Composable
private fun RefreshRing(
    refreshing: Boolean,
    pulled: Float,
    modifier: Modifier,
) {
    val reach = if (refreshing) 1f else pulled.coerceIn(0f, 1f)
    if (reach == 0f) return
    Box(
        modifier
            .graphicsLayer {
                translationY = reach * 64.dp.toPx() - 40.dp.toPx()
                alpha = reach
                scaleX = 0.6f + 0.4f * reach
                scaleY = 0.6f + 0.4f * reach
            }
            .size(40.dp)
            .shadow(6.dp, CircleShape)
            .background(MaterialTheme.colorScheme.surface, CircleShape),
        contentAlignment = Alignment.Center,
    ) { Loader(size = 22.dp) }
}

@Composable
private fun HomeList(
    state: HomeState,
    masked: Boolean,
    showBack: Boolean,
    bottom: Dp,
    onMask: () -> Unit,
    onFlip: () -> Unit,
    onMembership: () -> Unit,
    onEvent: (String) -> Unit,
    onAllEvents: () -> Unit,
    onActivity: (ActivityRow) -> Unit,
    onAllActivity: () -> Unit,
    onEditProfile: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 8.dp, bottom = bottom + 16.dp)) {
        item { TopBar(Modifier.padding(start = SIDE, end = SIDE, bottom = 10.dp)) }
        item { MemberCard(state.card, showBack, masked, onFlip = onFlip, Modifier.padding(horizontal = TIGHT)) }
        item {
            CardActions(
                showBack,
                masked,
                onQr = onFlip,
                onMask = onMask,
                onEdit = onEditProfile,
                Modifier.padding(start = TIGHT, end = TIGHT, top = 12.dp),
            )
        }
        item { MembershipLine(state, onMembership, Modifier.padding(start = TIGHT, end = TIGHT, top = 10.dp)) }
        state.nextEvent?.let { event ->
            item { SectionTitle("À venir", "Tous les événements", onAllEvents) }
            item { EventLine(event) { onEvent(event.id) } }
        }
        if (state.activity.isNotEmpty()) {
            item { SectionTitle("Activité", "Tout voir", onAllActivity) }
            items(state.activity, key = { it.id }) { row ->
                ActivityLine(row) { onActivity(row) }
                Box(Modifier.padding(start = 70.dp).fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline))
            }
        }
    }
}

/** The logo alone for now; its right side stays free for an action to come. */
@Composable
private fun TopBar(modifier: Modifier) {
    val dark = isSystemInDarkTheme()
    Row(modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(if (dark) Res.drawable.wordmark_dark else Res.drawable.wordmark_light), "PariBabi", Modifier.height(30.dp))
    }
}

/** Under the card: show the QR, hide what is written, complete my profile. */
@Composable
private fun CardActions(
    showBack: Boolean,
    masked: Boolean,
    onQr: () -> Unit,
    onMask: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier
                .weight(1f)
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.onBackground)
                .clickable(onClick = onQr),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(if (showBack) Res.drawable.ic_card else Res.drawable.ic_qr), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.background)
            Spacer(Modifier.width(10.dp))
            Text(if (showBack) "Voir ma carte" else "Afficher mon QR", color = MaterialTheme.colorScheme.background, fontWeight = FontWeight.SemiBold)
        }
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).clickable(onClick = onMask),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(if (masked) Res.drawable.ic_eye_off else Res.drawable.ic_eye),
                if (masked) "Afficher mes informations" else "Masquer mes informations",
                Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).clickable(onClick = onEdit),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(Res.drawable.ic_edit), "Compléter mon profil", Modifier.size(21.dp), tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** My membership in one line; it opens the membership, or the payment when it is due. */
@Composable
private fun MembershipLine(
    state: HomeState,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val (dot, text, action) =
        when (val status = state.status) {
            is MemberStatus.Active -> Triple(Brand.Green, "Adhérent jusqu'au ${status.until}", null)
            is MemberStatus.Expired -> Triple(Brand.Orange, "Adhésion expirée le ${status.since}", "Renouveler")
            MemberStatus.AwaitingPayment -> Triple(Brand.Orange, "Candidature acceptée", "Payer")
        }
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text("Mon adhésion · ${state.seasonName}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        if (action != null) {
            Text(action, Modifier.clip(RoundedCornerShape(50)).background(Brand.Orange).padding(horizontal = 12.dp, vertical = 6.dp), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        } else {
            Icon(painterResource(Res.drawable.ic_chevron_right), null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    action: String,
    onAction: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(start = SIDE, end = SIDE - 4.dp, top = 28.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(action, Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(4.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

/** The nearest event, small and edge to edge like the activity: its poster, its name, when and where, the price. */
@Composable
private fun EventLine(
    event: EventRow,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = SIDE, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The poster of the event, once plugged to the server.
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(Brand.BlueDeep, Brand.Blue, Brand.Orange))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(Res.drawable.ic_calendar), null, Modifier.size(20.dp), tint = Color.White.copy(alpha = 0.9f))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(event.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOf(event.date, event.venue).filter { it.isNotBlank() }.joinToString(" · "),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(event.price, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
    }
}

/**
 * One notification, edge to edge. Always the same height, whether the title takes one line or two: the title is
 * cut after two lines, and the row is centred in a fixed height.
 */
@Composable
internal fun ActivityLine(
    row: ActivityRow,
    onClick: () -> Unit,
) {
    val (icon, tint) = row.kind.look()
    Row(
        Modifier
            .fillMaxWidth()
            .height(ACTIVITY_HEIGHT)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = SIDE),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KindIcon(icon, tint)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                row.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                lineHeight = 19.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(row.time, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 16.sp, maxLines = 1)
        }
        row.amount?.let {
            Spacer(Modifier.width(12.dp))
            Text(it, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
        }
    }
}

/** The icon of a kind of notification and its colour. */
internal fun ActivityKind.look(): Pair<DrawableResource, Color> =
    when (this) {
        ActivityKind.PAYMENT -> Res.drawable.ic_payments to Brand.Blue
        ActivityKind.MEMBERSHIP -> Res.drawable.ic_card to Brand.Green
        ActivityKind.REMINDER -> Res.drawable.ic_bell to Brand.Orange
        ActivityKind.APPLICATION -> Res.drawable.ic_check to Brand.Grey
    }

@Composable
internal fun KindIcon(
    icon: DrawableResource,
    tint: Color,
) {
    Box(Modifier.size(38.dp).clip(CircleShape).background(tint.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), null, Modifier.size(19.dp), tint = tint)
    }
}

private val SIDE = 20.dp

/** Around the card and the lines under it: the card takes almost the whole width. */
private val TIGHT = 10.dp

/** Two lines of title (2 × 19) and the time (16), with their gap and the margins. */
private val ACTIVITY_HEIGHT = 80.dp
