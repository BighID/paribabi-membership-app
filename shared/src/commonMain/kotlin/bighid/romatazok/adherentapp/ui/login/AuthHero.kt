package bighid.romatazok.adherentapp.ui.login

import adherentapp.shared.generated.resources.Res
import adherentapp.shared.generated.resources.carte_identite
import adherentapp.shared.generated.resources.ic_back
import adherentapp.shared.generated.resources.wordmark_dark
import adherentapp.shared.generated.resources.wordmark_light
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource

/** How much room the top takes: all of it, less for a keypad, the least while the keyboard is open. */
enum class HeroSize { FULL, KEYPAD, KEYBOARD }

/**
 * The top of every authentication page, on the page's own background: the app bar and its way back, the club's card
 * slightly tilted, then the wordmark. Smaller for a keypad. When the keyboard opens, the card shrinks and the wordmark
 * folds away together, so that the form rises with them and stays whole above the keyboard; back when it closes.
 */
@Composable
fun AuthHero(
    onBack: (() -> Unit)?,
    size: HeroSize,
) {
    val dark = isSystemInDarkTheme()
    val spec = tween<Float>(HERO_MILLIS)
    val width by animateFloatAsState(
        when (size) {
            HeroSize.FULL -> 0.72f
            HeroSize.KEYPAD -> 0.5f
            HeroSize.KEYBOARD -> 0.4f
        },
        spec,
    )
    val gap by animateDpAsState(if (size == HeroSize.FULL) 28.dp else if (size == HeroSize.KEYPAD) 18.dp else 12.dp, tween(HERO_MILLIS))
    val mark by animateDpAsState(if (size == HeroSize.FULL) 36.dp else 30.dp, tween(HERO_MILLIS))
    Column(Modifier.fillMaxWidth().statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        // The app bar: the way back on the left, nothing else.
        Box(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
            if (onBack != null) {
                Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                    Icon(painterResource(Res.drawable.ic_back), "Retour", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
        Image(
            painterResource(Res.drawable.carte_identite),
            null,
            Modifier
                .fillMaxWidth(width)
                .rotate(-4f)
                // Corners in proportion to the card, so that it keeps its shape at every size.
                .shadow(18.dp, CardShape)
                .clip(CardShape)
                .aspectRatio(856f / 510f),
            contentScale = ContentScale.FillBounds,
        )
        Spacer(Modifier.height(gap))
        AnimatedVisibility(
            size != HeroSize.KEYBOARD,
            enter = expandVertically(tween(HERO_MILLIS)) + fadeIn(tween(HERO_MILLIS)),
            exit = shrinkVertically(tween(HERO_MILLIS)) + fadeOut(tween(HERO_MILLIS)),
        ) {
            Image(painterResource(if (dark) Res.drawable.wordmark_dark else Res.drawable.wordmark_light), "PariBabi", Modifier.height(mark))
        }
    }
}

/** Corners at 9 % of the card's height: the rounding of the full card of the login. */
private val CardShape = RoundedCornerShape(percent = 9)

/** About the time the keyboard takes to rise, so that the two move as one. */
private const val HERO_MILLIS = 280
