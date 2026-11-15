/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ssilencce.playback

import com.ssilencce.core.model.action.HeadsetButtonAction

data class HeadsetButtonConfig(
    val single: HeadsetButtonAction = HeadsetButtonAction.PlayPause,
    val double: HeadsetButtonAction = HeadsetButtonAction.Next,
    val triple: HeadsetButtonAction = HeadsetButtonAction.Previous,
    val longPress: HeadsetButtonAction = HeadsetButtonAction.PlayPause,
    val fourPlusSeekForward: Boolean = false,
    val delayMs: Int = HeadsetButtonPolicy.DEFAULT_DELAY_MS,
) {
    val longPressIsDistinct: Boolean
        get() = longPress != single
}

/**
 * Pure count/timing rules for the two intercepted single-button media keycodes.
 *
 * The default config intentionally mirrors the legacy handler: triple dispatch is immediate
 * when 4+ is off, and a distinct long-press action raises the dispatch floor to 600 ms.
 */
object HeadsetButtonPolicy {
    const val MIN_DELAY_MS = 250
    const val DEFAULT_DELAY_MS = 300
    const val MAX_DELAY_MS = 600
    const val LONG_PRESS_GUARD_MS = 600L

    val DELAY_VALUES = listOf(250, 300, 400, 500, 600)

    fun normalizeDelay(raw: String?): Int = raw?.toIntOrNull()?.takeIf { it in DELAY_VALUES } ?: DEFAULT_DELAY_MS

    fun actionForCount(
        count: Int,
        config: HeadsetButtonConfig,
    ): HeadsetButtonAction =
        when {
            count <= 1 -> config.single
            count == 2 -> config.double
            count == 3 -> config.triple
            config.fourPlusSeekForward -> HeadsetButtonAction.SeekForward
            else -> config.triple
        }

    fun shouldDispatchImmediately(
        count: Int,
        fourPlusSeekForward: Boolean,
    ): Boolean =
        (fourPlusSeekForward && count >= 4) ||
            (!fourPlusSeekForward && count >= 3)

    fun effectiveDelayMs(
        configuredDelayMs: Int,
        longPressIsDistinct: Boolean,
    ): Long =
        if (longPressIsDistinct) {
            maxOf(configuredDelayMs.toLong(), LONG_PRESS_GUARD_MS)
        } else {
            configuredDelayMs.toLong()
        }
}

sealed interface HeadsetButtonDecision {
    data class Dispatch(
        val action: HeadsetButtonAction,
    ) : HeadsetButtonDecision

    data class Schedule(
        val delayMs: Long,
    ) : HeadsetButtonDecision

    data object Consumed : HeadsetButtonDecision
}

/**
 * Pure press-count and long-press state machine. The service owns the Handler callback and
 * calls [onDown], [onRepeat] or [onScheduledDispatch] with a fresh config.
 */
class HeadsetButtonController {
    private var clickCount = 0
    private var longPressHandled = false

    fun onDown(config: HeadsetButtonConfig): HeadsetButtonDecision {
        longPressHandled = false
        clickCount++
        if (HeadsetButtonPolicy.shouldDispatchImmediately(clickCount, config.fourPlusSeekForward)) {
            val action = HeadsetButtonPolicy.actionForCount(clickCount, config)
            reset()
            return HeadsetButtonDecision.Dispatch(action)
        }
        return HeadsetButtonDecision.Schedule(
            HeadsetButtonPolicy.effectiveDelayMs(config.delayMs, config.longPressIsDistinct),
        )
    }

    fun onRepeat(config: HeadsetButtonConfig): HeadsetButtonDecision {
        if (!config.longPressIsDistinct || longPressHandled || clickCount == 0) {
            return HeadsetButtonDecision.Consumed
        }
        longPressHandled = true
        clickCount = 0
        return HeadsetButtonDecision.Dispatch(config.longPress)
    }

    fun onScheduledDispatch(config: HeadsetButtonConfig): HeadsetButtonDecision {
        if (clickCount == 0) return HeadsetButtonDecision.Consumed
        val action = HeadsetButtonPolicy.actionForCount(clickCount, config)
        reset()
        return HeadsetButtonDecision.Dispatch(action)
    }

    fun reset() {
        clickCount = 0
        longPressHandled = false
    }
}
