package bighid.romatazok.adherentapp.ui.home

import adherentapp.shared.generated.resources.Res
import adherentapp.shared.generated.resources.carte_identite
import adherentapp.shared.generated.resources.carte_qr
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.qrose.options.QrBrush
import io.github.alexzhirkevich.qrose.options.solid
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import org.jetbrains.compose.resources.painterResource

/**
 * The card of PariBabi, as designed: its identity first, its QR on the back. A tap or "Afficher mon QR" turns it
 * over. The visual is an image; the member's data are written over it at the positions of the design (in units of
 * its 856 × 540 view box), so that a new season or a new job shows at once.
 */
@Composable
fun MemberCard(
    card: CardData,
    showBack: Boolean,
    masked: Boolean,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val angle by animateFloatAsState(if (showBack) 180f else 0f, tween(FLIP_MS, easing = FastOutSlowInEasing))
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(WIDTH / HEIGHT)) {
        val unit = maxWidth / WIDTH
        val shape = RoundedCornerShape(unit * CORNER)
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = angle
                    // Far enough that the card never looks distorted while it turns.
                    cameraDistance = 14f * density
                }.shadow(14.dp, shape, ambientColor = NAVY, spotColor = NAVY)
                .clip(shape)
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onFlip),
        ) {
            if (angle <= 90f) {
                Identity(card, masked, unit)
            } else {
                // The back is drawn turned over again, so that it reads the right way once the card has turned.
                Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) { QrSide(card, unit) }
            }
        }
    }
}

@Composable
private fun Identity(
    card: CardData,
    masked: Boolean,
    unit: Dp,
) {
    fun shown(value: String?) = if (masked) HIDDEN else value.orEmpty()
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(Res.drawable.carte_identite), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        // Inside the white frame of the photo (27, 100, 236 × 290, its border 6 wide).
        card.photo?.let { photo ->
            Image(
                photo,
                "Photo",
                Modifier.frame(unit, 33, 106, 224, 278).clip(RoundedCornerShape(unit * 16)),
                contentScale = ContentScale.Crop,
            )
        }
        Field(shown(card.fullName), 314, 190, 34, FontWeight.Bold, NAVY, unit, maxWidth = 520)
        Field(shown(card.jobTitle), 314, 220, 20, FontWeight.SemiBold, ORANGE, unit, maxWidth = 520)
        Field(shown(card.company), 314, 246, 18, FontWeight.Normal, SLATE, unit, maxWidth = 520)
        Field(shown(card.memberSince), 314, 326, 22, FontWeight.Bold, NAVY, unit, maxWidth = 250)
        Field(shown(card.validUntil), 576, 326, 22, FontWeight.Bold, NAVY, unit, maxWidth = 258)
        Field(shown(card.city), 314, 388, 18, FontWeight.SemiBold, NAVY, unit, maxWidth = 250)
        // A long sector ("Communication, marketing et médias") goes on two lines rather than being cut.
        Field(shown(card.sector), 576, 388, 18, FontWeight.SemiBold, NAVY, unit, maxWidth = 258, lines = 2)
        Field(shown(card.memberNumber), 145, 444, 18, FontWeight.Bold, Color.White, unit, maxWidth = 240, centered = true, spacing = 1f)
    }
}

@Composable
private fun QrSide(
    card: CardData,
    unit: Dp,
) {
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(Res.drawable.carte_qr), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        // Inside the white frame (270, 92, 316 × 316), with its margin.
        val qr = rememberQrCodePainter(card.qrContent) { colors { dark = QrBrush.solid(NAVY) } }
        Image(qr, "QR code de ma carte", Modifier.place(unit, 428, 250, 276))
    }
}

/** A text of the design: [x] and [baseline] in units of the view box, [centered] for a text anchored in its middle. */
@Composable
private fun Field(
    text: String,
    x: Int,
    baseline: Int,
    size: Int,
    weight: FontWeight,
    color: Color,
    unit: Dp,
    maxWidth: Int,
    centered: Boolean = false,
    spacing: Float = 0f,
    lines: Int = 1,
) {
    if (text.isEmpty()) return
    val fontSize = with(androidx.compose.ui.platform.LocalDensity.current) { (unit * size).toSp() }
    val letterSpacing = with(androidx.compose.ui.platform.LocalDensity.current) { (unit * spacing).toSp() }
    Text(
        text,
        Modifier.layout { measurable, constraints ->
            val limit = (unit * maxWidth).roundToPx()
            val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0, maxWidth = limit))
            val left = (unit * x).roundToPx() - if (centered) placeable.width / 2 else 0
            val top = (unit * (baseline - TOP)).roundToPx() - placeable[FirstBaseline]
            layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(left, top) }
        },
        color = color,
        fontSize = fontSize,
        fontWeight = weight,
        letterSpacing = letterSpacing,
        // Tight but readable when a value takes two lines.
        lineHeight = fontSize * 1.22f,
        maxLines = lines,
        overflow = TextOverflow.Ellipsis,
        softWrap = lines > 1,
    )
}

/** A square of [side] units centred on ([cx], [cy]). */
private fun Modifier.place(
    unit: Dp,
    cx: Int,
    cy: Int,
    side: Int,
) = layout { measurable, constraints ->
    val px = (unit * side).roundToPx()
    val placeable = measurable.measure(constraints.copy(minWidth = px, maxWidth = px, minHeight = px, maxHeight = px))
    layout(constraints.maxWidth, constraints.maxHeight) {
        placeable.place((unit * cx).roundToPx() - px / 2, (unit * (cy - TOP)).roundToPx() - px / 2)
    }
}

/** A rectangle of the design: its top left corner ([x], [y]) and its size, in units of the view box. */
private fun Modifier.frame(
    unit: Dp,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
) = layout { measurable, constraints ->
    val w = (unit * width).roundToPx()
    val h = (unit * height).roundToPx()
    val placeable = measurable.measure(constraints.copy(minWidth = w, maxWidth = w, minHeight = h, maxHeight = h))
    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place((unit * x).roundToPx(), (unit * (y - TOP)).roundToPx()) }
}

/** The colours of the design. */
private val NAVY = Color(0xFF13264D)
private val ORANGE = Color(0xFFEE6A2F)
private val SLATE = Color(0xFF5B6475)

/**
 * The design is 856 × 540; the app cuts only its top, from y = 30, so that the card is less high and the skyline at
 * the bottom stays whole. Positions below stay those of the design.
 */
private const val WIDTH = 856f
private const val HEIGHT = 510f
private const val TOP = 30
private const val CORNER = 34f
private const val HIDDEN = "••••••"
private const val FLIP_MS = 550
