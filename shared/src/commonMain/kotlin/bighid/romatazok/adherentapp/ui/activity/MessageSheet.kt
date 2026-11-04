package bighid.romatazok.adherentapp.ui.activity

import adherentapp.shared.generated.resources.Res
import adherentapp.shared.generated.resources.ic_chevron_right
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bighid.romatazok.adherentapp.ui.home.ActivityRow
import bighid.romatazok.adherentapp.ui.home.KindIcon
import bighid.romatazok.adherentapp.ui.home.look
import bighid.romatazok.adherentapp.ui.events.NAVY_LIGHT
import org.jetbrains.compose.resources.painterResource

/**
 * A message about my membership or my application, in full. From the activity, it does not open a second page: the
 * button leads to my membership, which stays one page from the home.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageSheet(
    row: ActivityRow,
    onDismiss: () -> Unit,
    onMembership: () -> Unit,
) {
    val (icon, tint) = row.kind.look()
    ModalBottomSheet(onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            KindIcon(icon, tint)
            Spacer(Modifier.height(6.dp))
            Text(row.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
            Text(row.time, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            row.amount?.let { Text(it, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(NAVY_LIGHT).clickable(onClick = onMembership).padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Voir mon adhésion", Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Icon(painterResource(Res.drawable.ic_chevron_right), null, Modifier.size(20.dp), tint = Color.White)
            }
        }
    }
}
