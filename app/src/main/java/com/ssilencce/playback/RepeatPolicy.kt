/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.ssilencce.playback

import androidx.media3.common.Player

/**
 * Player-level repeat policy. [Mode.LoadNextAndPause] is intentionally not one of the three
 * Media3 repeat modes: the service keeps Media3 at [Player.REPEAT_MODE_OFF] and pauses each
 * automatically loaded item at its start while this mode is active.
 */
object RepeatPolicy {
    enum class Mode {
        Off,
        All,
        One,
        LoadNextAndPause,
    }

    fun current(
        media3RepeatMode: Int,
        loadNextAndPauseActive: Boolean,
    ): Mode =
        when {
            loadNextAndPauseActive -> Mode.LoadNextAndPause
            media3RepeatMode == Player.REPEAT_MODE_ONE -> Mode.One
            media3RepeatMode == Player.REPEAT_MODE_ALL -> Mode.All
            else -> Mode.Off
        }

    /**
     * The extra state joins the cycle only when [loadNextAndPauseAvailable] is true. When the
     * setting is turned off while the extra state is active, the next press returns to Off.
     */
    fun next(
        current: Mode,
        loadNextAndPauseAvailable: Boolean,
    ): Mode =
        when (current) {
            Mode.Off -> Mode.All
            Mode.All -> Mode.One
            Mode.One -> if (loadNextAndPauseAvailable) Mode.LoadNextAndPause else Mode.Off
            Mode.LoadNextAndPause -> Mode.Off
        }

    fun media3RepeatMode(mode: Mode): Int =
        when (mode) {
            Mode.All -> Player.REPEAT_MODE_ALL

            Mode.One -> Player.REPEAT_MODE_ONE

            Mode.Off,
            Mode.LoadNextAndPause,
            -> Player.REPEAT_MODE_OFF
        }

    fun isRepeatActive(mode: Mode): Boolean = mode != Mode.Off

    /**
     * Only automatic item transitions are candidates: a user-requested seek must keep playing
     * normally, and a playlist change is not a finished song.
     */
    fun shouldPauseLoadedItem(
        mode: Mode,
        transitionReason: Int,
    ): Boolean = mode == Mode.LoadNextAndPause && transitionReason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
}
