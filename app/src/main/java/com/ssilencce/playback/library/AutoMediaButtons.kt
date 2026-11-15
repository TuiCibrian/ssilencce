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

package com.ssilencce.playback.library

import android.content.Context
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.media3.session.CommandButton
import androidx.media3.session.SessionCommand
import com.ssilencce.R
import com.ssilencce.playback.Playback

/**
 * Media button preferences for Android Auto and Auto companion controllers.
 *
 * Seek controls are omitted for tracks shorter than the configurable seek threshold,
 * matching the media notification. Repeat is appended by the service because it depends
 * on the player's current repeat policy.
 */
object AutoMediaButtons {
    fun build(
        context: Context,
        hasTimeline: Boolean,
        seekControlsAvailable: Boolean,
        favorite: Boolean,
        shuffleEnabled: Boolean,
    ): List<CommandButton> {
        if (!hasTimeline) return emptyList()
        return buildList {
            if (seekControlsAvailable) {
                add(
                    button(
                        context = context,
                        icon = CommandButton.ICON_REWIND,
                        action = Playback.SEEK_BACK,
                        titleRes = R.string.action_seek_backward,
                    ),
                )
                add(
                    button(
                        context = context,
                        icon = CommandButton.ICON_FAST_FORWARD,
                        action = Playback.SEEK_FORWARD,
                        titleRes = R.string.action_seek_forward,
                    ),
                )
            }
            add(
                button(
                    context = context,
                    icon =
                        if (favorite) {
                            CommandButton.ICON_HEART_FILLED
                        } else {
                            CommandButton.ICON_HEART_UNFILLED
                        },
                    action = Playback.TOGGLE_FAVORITE,
                    titleRes =
                        if (favorite) {
                            R.string.notification_favorite_remove
                        } else {
                            R.string.notification_favorite_add
                        },
                ),
            )
            add(
                button(
                    context = context,
                    icon =
                        if (shuffleEnabled) {
                            CommandButton.ICON_SHUFFLE_ON
                        } else {
                            CommandButton.ICON_SHUFFLE_OFF
                        },
                    action = Playback.TOGGLE_SHUFFLE,
                    titleRes = R.string.shuffle_mode,
                ),
            )
        }
    }

    private fun button(
        context: Context,
        icon: Int,
        action: String,
        @StringRes titleRes: Int,
    ): CommandButton =
        CommandButton
            .Builder(icon)
            .setSessionCommand(SessionCommand(action, Bundle.EMPTY))
            .setDisplayName(context.getString(titleRes))
            .build()
}
