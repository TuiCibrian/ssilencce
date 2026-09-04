package com.ssilencce.ui.screen.player.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

@Composable
fun AuroraSlider(
    value: Float,
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
    trackHeight: androidx.compose.ui.unit.Dp = 4.dp,
    thumbSize: androidx.compose.ui.unit.Dp = 17.dp,
    containerHeight: androidx.compose.ui.unit.Dp = 28.dp,
) {
    val primary = MaterialTheme.colorScheme.primary
    var dragOffset by remember { mutableFloatStateOf(value) }
    val fraction by animateFloatAsState(if (enabled) dragOffset else value)

    BoxWithConstraints(
        modifier =
            modifier
                .height(containerHeight)
                .pointerInput(enabled) {
                    if (enabled) {
                        detectDragGestures(
                            onDrag = { change, _ ->
                                change.consume()
                                dragOffset = (change.position.x / size.width).coerceIn(0f, 1f)
                                onValueChange(dragOffset)
                            },
                            onDragEnd = {
                                onValueChangeFinished(dragOffset)
                            },
                        )
                    }
                }
                .pointerInput(enabled) {
                    if (enabled) {
                        detectTapGestures { position ->
                            dragOffset = (position.x / size.width).coerceIn(0f, 1f)
                            onValueChange(dragOffset)
                            onValueChangeFinished(dragOffset)
                        }
                    }
                },
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .clip(CircleShape),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(trackHeight)
                        .background(primary.copy(alpha = 0.15f), CircleShape),
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(trackHeight)
                        .shadow(10.dp, CircleShape, ambientColor = Color.White, spotColor = Color.White)
                        .background(primary, CircleShape),
            )
            Box(
                modifier =
                    Modifier
                        .offset(x = this@BoxWithConstraints.maxWidth * fraction - thumbSize / 2)
                        .size(thumbSize)
                        .shadow(12.dp, CircleShape, ambientColor = Color.White, spotColor = Color.White)
                        .background(primary, CircleShape),
            )
        }
    }
}
