package bighid.romatazok.adherentapp.ui.receipt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bighid.romatazok.adherentapp.ui.Loader
import bighid.romatazok.adherentapp.ui.rememberShareFile
import bighid.romatazok.adherentapp.ui.theme.Brand
import kotlinx.coroutines.launch

/** Where the screens read a receipt and its PDF, by the payment it belongs to; null when it cannot be read. */
interface ReceiptSource {
    suspend fun view(paymentId: String): ReceiptView?

    /** The file's name and its content. */
    suspend fun pdf(paymentId: String): Pair<String, ByteArray>?
}

/** Given by the app to every screen that shows a receipt. */
val LocalReceipts =
    staticCompositionLocalOf<ReceiptSource> {
        object : ReceiptSource {
            override suspend fun view(paymentId: String): ReceiptView? = null

            override suspend fun pdf(paymentId: String): Pair<String, ByteArray>? = null
        }
    }

/**
 * A receipt in its sheet, opened at once: a wheel while it comes, a way to try again when it does not. Its PDF goes to
 * the phone's share sheet, the button turning while it comes.
 */
@Composable
fun ReceiptPanel(paymentId: String) {
    val source = LocalReceipts.current
    val share = rememberShareFile()
    val scope = rememberCoroutineScope()
    // Changes with each try, to read again.
    var attempt by remember { mutableStateOf(0) }
    var receipt by remember { mutableStateOf<ReceiptView?>(null) }
    var failed by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var downloadFailed by remember { mutableStateOf(false) }
    LaunchedEffect(paymentId, attempt) {
        failed = false
        receipt = source.view(paymentId)
        failed = receipt == null
    }
    val shown = receipt
    when {
        shown != null ->
            ReceiptContent(
                shown,
                downloading = downloading,
                downloadError = if (downloadFailed) "Téléchargement impossible pour le moment." else null,
                onDownload = {
                    downloading = true
                    downloadFailed = false
                    scope.launch {
                        val file = source.pdf(paymentId)
                        if (file != null && share != null) share(file.second, file.first, "application/pdf") else downloadFailed = true
                        downloading = false
                    }
                },
            )
        failed ->
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Reçu indisponible pour le moment.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                TextButton({ attempt++ }) { Text("Réessayer", color = Brand.Orange, fontWeight = FontWeight.SemiBold) }
            }
        else ->
            Column(Modifier.fillMaxWidth().height(260.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Loader(size = 30.dp)
            }
    }
}
