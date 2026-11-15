package com.ssilencce.ui.component.compose.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssilencce.R
import com.ssilencce.extensions.media.asReadableDuration
import com.ssilencce.ui.screen.player.AbRepeatState

/**
 * Compact A / A-B marker shown over the player cover. Tapping it clears the loop.
 */
@Composable
fun AbRepeatChip(
    abRepeat: AbRepeatState?,
    abRepeatStart: Long?,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = abRepeat != null || abRepeatStart != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        val clearLabel = stringResource(R.string.ab_repeat_clear)
        val text =
            when {
                abRepeat != null -> {
                    stringResource(
                        R.string.ab_repeat_active_range,
                        abRepeat.startMs.asReadableDuration(),
                        abRepeat.endMs.asReadableDuration(),
                    )
                }

                abRepeatStart != null -> {
                    stringResource(R.string.ab_repeat_start_set, abRepeatStart.asReadableDuration())
                }

                else -> {
                    ""
                }
            }
        Box(
            modifier =
                Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = .68f))
                    .clickable(onClickLabel = clearLabel, onClick = onClear)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
