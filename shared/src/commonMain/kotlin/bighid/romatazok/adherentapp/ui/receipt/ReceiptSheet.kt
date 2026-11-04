package bighid.romatazok.adherentapp.ui.receipt

import adherentapp.shared.generated.resources.Res
import adherentapp.shared.generated.resources.ic_check
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bighid.romatazok.adherentapp.ui.theme.Brand
import org.jetbrains.compose.resources.painterResource

/** One receipt: always shown in a bottom sheet, wherever it is opened from. */
data class ReceiptView(
    val number: String,
    val label: String,
    val amount: String,
    /** "1 novembre 2026 à 21:12". */
    val paidAt: String,
    val method: String,
)

/** The content of the sheet, so that a sheet already open can show it in place. */
@Composable
fun ReceiptContent(
    receipt: ReceiptView,
    onDownload: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(Brand.Green.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(painterResource(Res.drawable.ic_check), null, Modifier.size(26.dp), tint = Brand.Green)
        }
        Spacer(Modifier.height(12.dp))
        Text(receipt.amount, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(receipt.label, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, fontSize = 15.sp)
        Spacer(Modifier.height(20.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.background).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Line("Reçu", receipt.number)
            Line("Payé le", receipt.paidAt)
            Line("Moyen", receipt.method)
        }
        Spacer(Modifier.height(18.dp))
        Button(
            onDownload,
            Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) { Text("Télécharger le reçu", fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun Line(
    label: String,
    value: String,
) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.width(90.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(value, Modifier.weight(1f), fontWeight = FontWeight.Medium, fontSize = 14.sp, textAlign = TextAlign.End)
    }
}
