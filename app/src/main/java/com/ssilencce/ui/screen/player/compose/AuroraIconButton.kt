package com.ssilencce.ui.screen.player.compose

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ssilencce.R
import com.ssilencce.ui.theme.AuroraTokens

@Composable
fun AuroraIconButton(
    icon: Painter,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    size: Dp = 44.dp,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    activeTint: Color = MaterialTheme.colorScheme.primary,
    onLongClick: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by updateTransition(pressed).animateFloat { if (it) .85f else 1f }
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(size)
                .clip(MaterialTheme.shapes.small)
                .background(if (pressed) AuroraTokens.surface2 else Color.Transparent)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = { onLongClick() },
                    )
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
    ) {
        Icon(
            painter = icon,
            contentDescription = contentDescription,
            tint = if (isActive) activeTint else tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
fun AuroraPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    playIcon: Painter = painterResource(R.drawable.ic_play_24dp),
    pauseIcon: Painter = painterResource(R.drawable.ic_pause_24dp),
    size: Dp = 62.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by updateTransition(pressed).animateFloat { if (it) .92f else 1f }
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(size)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(
                    elevation = 18.dp,
                    shape = CircleShape,
                    ambientColor = Color.White,
                    spotColor = Color.White,
                )
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onClick() })
                },
    ) {
        Icon(
            painter = if (isPlaying) pauseIcon else playIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(44.dp),
        )
    }
}
