package com.fittrack.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fittrack.app.ui.theme.HeroGradientEnd
import com.fittrack.app.ui.theme.HeroGradientStart
import com.fittrack.app.ui.theme.VoltInk
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider

/**
 * A gradient "hero" surface for headline stats. Two soft translucent circles
 * sit behind the content for a bit of depth without competing with text.
 * Content is drawn in white; use [HeroStat] for the number/label pairs.
 */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(
                Brush.linearGradient(
                    colors = listOf(HeroGradientStart, HeroGradientEnd),
                )
            )
            // Soft translucent circles painted behind the content. Drawn, not
            // laid out, so they never affect the card's height.
            .drawBehind {
                drawCircle(
                    color = Color.White.copy(alpha = 0.22f),
                    radius = size.height * 0.9f,
                    center = Offset(size.width * 0.92f, -size.height * 0.1f),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.16f),
                    radius = size.height * 0.6f,
                    center = Offset(size.width * 0.05f, size.height * 1.05f),
                )
            }
            .padding(20.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides VoltInk) {
            content()
        }
    }
}

/** Big number + small caps label, white-on-gradient, for use inside [HeroCard]. */
@Composable
fun HeroStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.displaySmall,
            color = VoltInk,
            maxLines = 1,
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = VoltInk.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
        )
    }
}
