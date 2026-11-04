package bighid.romatazok.adherentapp.ui

import adherentapp.shared.generated.resources.Res
import adherentapp.shared.generated.resources.wordmark_dark
import adherentapp.shared.generated.resources.wordmark_light
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import bighid.romatazok.adherentapp.ui.theme.Brand
import org.jetbrains.compose.resources.painterResource

/** While the member's data comes: the wordmark and a wheel; without network, the way to try again. */
@Composable
fun Loading(
    failed: Boolean,
    onRetry: () -> Unit,
) {
    val dark = isSystemInDarkTheme()
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(if (dark) Res.drawable.wordmark_dark else Res.drawable.wordmark_light), "PariBabi", Modifier.height(40.dp))
        if (failed) {
            Text("Pas de réseau.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Réessayer",
                Modifier.clip(RoundedCornerShape(50)).background(Brand.Orange).clickable(onClick = onRetry).padding(horizontal = 22.dp, vertical = 11.dp),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
        } else {
            CircularProgressIndicator(Modifier.size(28.dp), color = Brand.Orange, strokeWidth = 3.dp)
        }
    }
}
