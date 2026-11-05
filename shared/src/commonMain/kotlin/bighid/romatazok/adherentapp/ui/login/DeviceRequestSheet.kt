package bighid.romatazok.adherentapp.ui.login

import adherentapp.shared.generated.resources.Res
import adherentapp.shared.generated.resources.ic_smartphone
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import bighid.romatazok.adherentapp.ui.Loader
import bighid.romatazok.adherentapp.ui.events.NAVY_LIGHT
import bighid.romatazok.adherentapp.ui.events.ORANGE
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/**
 * On the phone already connected, over the home: another phone asks to connect. Opened by the notification, or on
 * opening the app while the request waits. Yes hands the account over; no blocks the request for 24 hours.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceRequestSheet(
    /** "Mar. 3 nov. · 17:42". */
    askedAt: String,
    /** Each answer is sent: null once the server took it, otherwise what to tell, the sheet staying open. */
    onAccept: suspend () -> String?,
    onRefuse: suspend () -> String?,
    onDismiss: () -> Unit,
) {
    var refused by remember { mutableStateOf(false) }
    // The answer on its way: its button turns, the other waits.
    var sending by remember { mutableStateOf<Boolean?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val answer: (Boolean) -> Unit = { accept ->
        if (sending == null) {
            sending = accept
            error = null
            scope.launch {
                error = if (accept) onAccept() else onRefuse()
                if (error == null && !accept) refused = true
                sending = null
            }
        }
    }
    ModalBottomSheet(onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(ORANGE.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(painterResource(Res.drawable.ic_smartphone), null, Modifier.size(30.dp), tint = ORANGE)
            }
            Spacer(Modifier.height(16.dp))
            if (refused) {
                Text("Demande refusée", fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Ton compte reste ici. Nouvelle demande possible dans 24 h.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(22.dp))
                SheetButton("Fermer", NAVY_LIGHT, Modifier.fillMaxWidth(), onClick = onDismiss)
            } else {
                Text("Nouveau téléphone. C'est toi ?", fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(
                    "$askedAt. Si oui, celui-ci sera déconnecté.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(22.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SheetButton("Non", MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f), outlined = true, busy = sending == false) { answer(false) }
                    SheetButton("Oui, c'est moi", ORANGE, Modifier.weight(1f), busy = sending == true) { answer(true) }
                }
                error?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun SheetButton(
    label: String,
    color: Color,
    modifier: Modifier,
    outlined: Boolean = false,
    busy: Boolean = false,
    onClick: () -> Unit,
) {
    val ink = if (outlined) MaterialTheme.colorScheme.onSurface else Color.White
    Box(
        modifier.height(52.dp).clip(RoundedCornerShape(14.dp)).background(if (outlined) MaterialTheme.colorScheme.background else color).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (busy) {
            Loader(size = 22.dp, onAccent = !outlined)
        } else {
            Text(label, color = ink, fontWeight = FontWeight.SemiBold)
        }
    }
}
