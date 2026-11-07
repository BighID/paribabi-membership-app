package bighid.romatazok.adherentapp.ui

import adherentapp.shared.generated.resources.Res
import adherentapp.shared.generated.resources.ic_chevron_right
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource

/*
 * The soft style of the whole app: white, one pale grey, navy for what acts. Air rather than frames, lines parted by a
 * thin stroke, a state told by a dot. Solid orange is kept for the one action that counts on a screen.
 */

/** The thin stroke between two lines. */
@Composable
fun Hair(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline))
}

/** A state, told by its colour. */
@Composable
fun StateDot(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier.size(8.dp).clip(CircleShape).background(color))
}

/** A small grey word above a group of lines. */
@Composable
fun SoftLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(text, modifier, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
}

/** Words side by side, a thin stroke under the one chosen. */
@Composable
fun SoftTabs(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterHorizontally)) {
        labels.forEachIndexed { index, label ->
            val chosen = index == selected
            val ink by animateColorAsState(if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            val stroke by animateColorAsState(if (chosen) MaterialTheme.colorScheme.primary else Color.Transparent)
            Column(
                Modifier.clip(RoundedCornerShape(8.dp)).clickable { onSelect(index) }.padding(horizontal = 2.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(label, color = ink, fontSize = 15.sp, fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
                Box(Modifier.padding(top = 6.dp).width(28.dp).height(2.dp).clip(RoundedCornerShape(1.dp)).background(stroke))
            }
        }
    }
}

/** One line: what it is, its value in grey, and a chevron when it opens something. */
@Composable
fun SoftRow(
    label: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    detail: String? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.let {
            it()
            Box(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            detail?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        value?.let {
            Box(Modifier.width(12.dp))
            Text(it, Modifier.widthIn(max = 190.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, textAlign = TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        when {
            trailing != null -> trailing()
            onClick != null -> {
                Box(Modifier.width(6.dp))
                Icon(painterResource(Res.drawable.ic_chevron_right), null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}
