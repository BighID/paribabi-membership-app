package bighid.romatazok.adherentapp.ui.theme

import adherentapp.shared.generated.resources.Res
import adherentapp.shared.generated.resources.outfit_bold
import adherentapp.shared.generated.resources.outfit_medium
import adherentapp.shared.generated.resources.outfit_regular
import adherentapp.shared.generated.resources.outfit_semibold
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.Font

/** The colours of PariBabi, as in the back-office and the scanner app. */
object Brand {
    val Blue = Color(0xFF2A62AA)
    val BlueDark = Color(0xFF255696)
    val BlueDeep = Color(0xFF173A66)
    val BlueSoft = Color(0xFFEEF4FB)
    val Orange = Color(0xFFEA5D2D)
    val OrangeSoft = Color(0xFFFDEEE8)
    val Green = Color(0xFF12B76A)
    val Ink = Color(0xFF101828)
    val Grey = Color(0xFF667085)
    val Line = Color(0xFFE4E7EC)
    val Paper = Color(0xFFF7F8FA)
    val Night = Color(0xFF0B1220)
    val NightCard = Color(0xFF151E2E)
}

private val Light =
    // The soft style: white everywhere, one pale grey for what stands out, navy for what acts.
    lightColorScheme(
        primary = Color(0xFF1D3770),
        onPrimary = Color.White,
        primaryContainer = Brand.BlueSoft,
        onPrimaryContainer = Brand.BlueDark,
        secondary = Brand.Orange,
        onSecondary = Color.White,
        secondaryContainer = Brand.OrangeSoft,
        background = Color.White,
        onBackground = Color(0xFF1B2333),
        surface = Color.White,
        onSurface = Color(0xFF1B2333),
        surfaceVariant = Color(0xFFF5F6F9),
        onSurfaceVariant = Color(0xFF737B8C),
        outline = Color(0xFFEEF0F4),
    )

private val Dark =
    darkColorScheme(
        primary = Color(0xFF6E9BD6),
        onPrimary = Brand.Night,
        primaryContainer = Color(0xFF1C2D47),
        onPrimaryContainer = Color(0xFFD5E3F5),
        secondary = Brand.Orange,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFF3A2219),
        background = Brand.Night,
        onBackground = Color(0xFFE6E9EF),
        surface = Brand.NightCard,
        onSurface = Color(0xFFE6E9EF),
        surfaceVariant = Color(0xFF1D2738),
        onSurfaceVariant = Color(0xFF98A2B3),
        outline = Color(0xFF2B3648),
    )

/** Outfit, the typeface of the back-office, for every text. */
@Composable
private fun outfitTypography(): Typography {
    val outfit =
        FontFamily(
            Font(Res.font.outfit_regular, FontWeight.Normal),
            Font(Res.font.outfit_medium, FontWeight.Medium),
            Font(Res.font.outfit_semibold, FontWeight.SemiBold),
            Font(Res.font.outfit_bold, FontWeight.Bold),
        )
    val base = Typography()
    return Typography(
        headlineSmall = base.headlineSmall.copy(fontFamily = outfit),
        titleLarge = base.titleLarge.copy(fontFamily = outfit),
        titleMedium = base.titleMedium.copy(fontFamily = outfit),
        titleSmall = base.titleSmall.copy(fontFamily = outfit),
        bodyLarge = base.bodyLarge.copy(fontFamily = outfit),
        bodyMedium = base.bodyMedium.copy(fontFamily = outfit),
        bodySmall = base.bodySmall.copy(fontFamily = outfit),
        labelLarge = base.labelLarge.copy(fontFamily = outfit),
        labelMedium = base.labelMedium.copy(fontFamily = outfit),
        labelSmall = base.labelSmall.copy(fontFamily = outfit),
    )
}

@Composable
fun ParibabiTheme(
    dark: Boolean,
    content: @Composable () -> Unit,
) {
    val typography = outfitTypography()
    val colors = if (dark) Dark else Light
    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        shapes = Shapes(small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(24.dp)),
    ) {
        // Without a style or a colour, a text takes Outfit and the colour of the theme: never black on a dark background.
        CompositionLocalProvider(LocalContentColor provides colors.onBackground) {
            ProvideTextStyle(typography.bodyLarge, content)
        }
    }
}
