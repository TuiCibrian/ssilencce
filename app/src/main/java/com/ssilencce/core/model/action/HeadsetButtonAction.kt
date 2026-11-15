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

package com.ssilencce.core.model.action

/**
 * Safe, transport-only actions available to headset gestures. Deliberately separate from
 * [NowPlayingAction], which contains UI and destructive operations that are meaningless or
 * unsafe from a media button.
 */
enum class HeadsetButtonAction {
    PlayPause,
    Next,
    Previous,
    SeekForward,
    SeekBackward,
    Nothing,
}
