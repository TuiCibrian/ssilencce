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

package com.ssilencce.ui.screen.player

/**
 * Pure end-of-queue decisions shared by PlayerViewModel, QueueScreen and the JVM probe.
 * Playback "ended" never means merely paused; an empty queue never shows the footer.
 */
object QueueEndPolicy {
    fun isEnded(
        playbackStateEnded: Boolean,
        mediaItemCount: Int,
    ): Boolean = playbackStateEnded && mediaItemCount > 0

    fun shouldShowFooter(
        ended: Boolean,
        queueSize: Int,
    ): Boolean = ended && queueSize > 0

    /**
     * Replay uses logical queue position 0. With shuffle enabled, [mappedFirstIndex] is the
     * first item of the current shuffled order. The mapping can be stale, so it is validated
     * against the current [mediaItemCount]; an unset or out-of-range value falls back to the
     * valid fallback (timeline 0 by default).
     */
    fun isValidTimelineIndex(
        index: Int,
        mediaItemCount: Int,
    ): Boolean = index in 0 until mediaItemCount

    fun replayTimelineIndex(
        mappedFirstIndex: Int,
        mediaItemCount: Int,
        fallbackIndex: Int = 0,
    ): Int {
        if (mediaItemCount <= 0) return 0
        val validFallback = fallbackIndex.takeIf { isValidTimelineIndex(it, mediaItemCount) } ?: 0
        return mappedFirstIndex.takeIf { isValidTimelineIndex(it, mediaItemCount) } ?: validFallback
    }

    /**
     * Generic Play/toggle restarts the displayed current item. The mapped logical current index
     * wins when valid; a stale/out-of-range value falls back to the controller's current timeline
     * index, then to 0.
     */
    fun restartTimelineIndex(
        mappedCurrentIndex: Int,
        mediaItemCount: Int,
        fallbackIndex: Int,
    ): Int {
        if (mediaItemCount <= 0) return 0
        if (isValidTimelineIndex(mappedCurrentIndex, mediaItemCount)) return mappedCurrentIndex
        return fallbackIndex.takeIf { isValidTimelineIndex(it, mediaItemCount) } ?: 0
    }
}
