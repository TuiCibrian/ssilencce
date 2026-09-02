package com.ssilencce.ui.screen.player.compose

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

private const val LEFT_EDGE_DOUBLE_TAP_THRESHOLD = 0.30f
private const val RIGHT_EDGE_DOUBLE_TAP_THRESHOLD = 0.70f

class CoverGestures(
    val onTap: () -> Unit = {},
    val onDoubleTapCenter: () -> Unit = {},
    val onDoubleTapLeft: () -> Unit = {},
    val onDoubleTapRight: () -> Unit = {},
    val onLongPress: () -> Unit = {},
)

fun Modifier.playerCoverGestures(gestures: CoverGestures): Modifier =
    pointerInput(gestures) {
        detectTapGestures(
            onTap = { gestures.onTap() },
            onDoubleTap = { tap ->
                val width = size.width
                when {
                    tap.x < width * LEFT_EDGE_DOUBLE_TAP_THRESHOLD -> gestures.onDoubleTapLeft()
                    tap.x > width * RIGHT_EDGE_DOUBLE_TAP_THRESHOLD -> gestures.onDoubleTapRight()
                    else -> gestures.onDoubleTapCenter()
                }
            },
            onLongPress = { gestures.onLongPress() },
        )
    }
