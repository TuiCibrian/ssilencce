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

/**
 * Pure availability rule for the explicit reshuffle command. Reshuffle never enables shuffle;
 * it is available only for a non-empty queue with shuffle already on.
 */
object QueueReshufflePolicy {
    fun canReshuffle(
        shuffleEnabled: Boolean,
        mediaItemCount: Int,
    ): Boolean = shuffleEnabled && mediaItemCount > 0
}
