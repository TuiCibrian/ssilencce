package com.ssilencce.ui.screen.player.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
    maxLines: Int = 1,
    textAlign: TextAlign = TextAlign.Center,
    animationEnabled: Boolean = true,
) {
    var containerWidth by remember { mutableFloatStateOf(0f) }
    var textWidth by remember { mutableFloatStateOf(0f) }
    val shouldMarquee =
        animationEnabled && containerWidth > 0f && textWidth > containerWidth
    val travel = if (shouldMarquee) textWidth - containerWidth + 30f else 0f

    val transition = rememberInfiniteTransition(label = "marqueeText")
    val offset by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = -travel,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            durationMillis = if (shouldMarquee) (travel * 22f).toInt().coerceIn(2000, 12000) else 1,
                            easing = LinearEasing,
                        ),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "marqueeTextOffset",
        )

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .clipToBounds()
                .onSizeChanged { containerWidth = it.width.toFloat() }
                .graphicsLayer {
                    translationX = if (shouldMarquee) offset else 0f
                },
    ) {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = maxLines,
            softWrap = false,
            overflow = TextOverflow.Clip,
            textAlign = textAlign,
            onTextLayout = { textWidth = it.size.width.toFloat() },
        )
    }
}
