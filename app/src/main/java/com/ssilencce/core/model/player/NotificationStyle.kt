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

package com.ssilencce.core.model.player

import androidx.annotation.StringRes
import com.ssilencce.R

/**
 * Notification presentation. [System] keeps the Media3 system media style, which is the
 * default; the other values are rendered by a custom RemoteViews provider.
 *
 * The stored value is stable and must not be renamed (it is persisted in preferences).
 */
enum class NotificationStyle(
    val storedValue: String,
    @StringRes val labelRes: Int,
) {
    System("system", R.string.notification_style_system),
    Small("small", R.string.notification_style_small),
    BigSimple("big_simple", R.string.notification_style_big_simple),
    BigAdvanced("big_advanced", R.string.notification_style_big_advanced),
    ;

    val isCustom: Boolean
        get() = this != System

    /** Whether the style includes a position seek bar and needs periodic refreshes. */
    val showsSeekBar: Boolean
        get() = this == BigSimple || this == BigAdvanced

    val showsUpcomingSongs: Boolean
        get() = this == BigAdvanced

    companion object {
        fun fromStoredValue(value: String?): NotificationStyle? = entries.firstOrNull { it.storedValue == value }
    }
}

/**
 * Background used by the custom notification layouts.
 *
 * The stored value is stable and must not be renamed (it is persisted in preferences).
 */
enum class NotificationBackgroundMode(
    val storedValue: String,
    @StringRes val labelRes: Int,
) {
    Theme("theme", R.string.notification_background_theme),
    Cover("cover", R.string.notification_background_cover),
    Custom("custom", R.string.notification_background_custom),
    ;

    companion object {
        fun fromStoredValue(value: String?): NotificationBackgroundMode? = entries.firstOrNull { it.storedValue == value }
    }
}

/**
 * Selects the buttons used by each custom notification style.
 *
 * Small uses the configured compact list and big simple uses the configured expanded list.
 * Big advanced has a fixed set because it always exposes the favorite and add-to-playlist
 * actions that are shown next to the upcoming songs.
 */
object NotificationStylePolicy {
    const val MAX_COMPACT_BUTTONS = 3
    const val MAX_BUTTONS = 5

    val bigAdvancedButtons: List<NotificationButton> =
        listOf(
            NotificationButton.Previous,
            NotificationButton.PlayPause,
            NotificationButton.Next,
            NotificationButton.Favorite,
            NotificationButton.AddToPlaylist,
        )

    fun buttonsFor(
        style: NotificationStyle,
        layout: NotificationButtonLayout,
        isAvailable: (NotificationButton) -> Boolean,
    ): List<NotificationButton> {
        val requested =
            when (style) {
                NotificationStyle.System -> return emptyList()
                NotificationStyle.Small -> compactButtonsFor(layout, isAvailable)
                NotificationStyle.BigSimple -> layout.expanded.ifEmpty { NotificationButtonLayout.DEFAULT.expanded }
                NotificationStyle.BigAdvanced -> bigAdvancedButtons
            }
        val limit = if (style == NotificationStyle.Small) MAX_COMPACT_BUTTONS else MAX_BUTTONS
        return requested.filter(isAvailable).distinct().take(limit)
    }

    fun compactButtonsFor(
        layout: NotificationButtonLayout,
        isAvailable: (NotificationButton) -> Boolean,
    ): List<NotificationButton> =
        layout.compact
            .ifEmpty { NotificationButtonLayout.DEFAULT.compact }
            .filter(isAvailable)
            .distinct()
            .take(MAX_COMPACT_BUTTONS)
}
