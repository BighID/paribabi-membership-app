package bighid.romatazok.adherentapp.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bighid.romatazok.adherentapp.ui.PageBar

/** The events to come, the nearest first; a tap opens one in a bottom sheet. Past events are in "Mon adhésion". */
@Composable
fun EventsScreen(
    events: List<UpcomingEvent>,
    onBack: () -> Unit,
    onOpen: (UpcomingEvent) -> Unit,
) {
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        PageBar("Événements", onBack)
        if (events.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Aucun événement annoncé pour le moment. Tu seras prévenu dès qu'il y en aura un.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 4.dp, bottom = bottom + 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(events, key = { it.id }) { event -> EventCard(event) { onOpen(event) } }
            }
        }
    }
}

/** The poster, then the date like a calendar sheet beside what, when, where and my price. */
@Composable
private fun EventCard(
    event: UpcomingEvent,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
    ) {
        Box {
            Poster(event, Modifier.fillMaxWidth().height(150.dp))
            Row(Modifier.align(Alignment.TopStart).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (event.membersOnly) PosterTag("Réservé aux adhérents")
                if (event.ticket != null) PosterTag("Billet acheté")
            }
        }
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(CREAM),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(event.day, color = NAVY, fontSize = 19.sp, fontWeight = FontWeight.Bold, lineHeight = 21.sp)
                Text(event.month, color = ORANGE, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 14.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(event.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${event.time} · ${event.venue}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(10.dp))
            Text(event.price, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** Readable on any poster. */
@Composable
private fun PosterTag(text: String) {
    Text(
        text,
        Modifier.clip(RoundedCornerShape(50)).background(Color.White).padding(horizontal = 10.dp, vertical = 4.dp),
        color = NAVY,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
    )
}
