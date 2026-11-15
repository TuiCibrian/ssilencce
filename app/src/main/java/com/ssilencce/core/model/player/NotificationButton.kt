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
 * Actions that can be placed on the media notification.
 *
 * The stored value is stable and must not be renamed (it is persisted in preferences).
 */
enum class NotificationButton(
    val storedValue: String,
    @StringRes val labelRes: Int,
) {
    Previous("previous", R.string.action_previous),
    PlayPause("play_pause", R.string.action_play_pause),
    Next("next", R.string.action_next),
    Favorite("favorite", R.string.notification_button_favorite),
    SeekForward("seek_forward", R.string.action_seek_forward),
    SeekBackward("seek_backward", R.string.action_seek_backward),
    Close("close", R.string.close_action),
    Shuffle("shuffle", R.string.shuffle_mode),
    Repeat("repeat", R.string.repeat_mode),
    AddToPlaylist("add_to_playlist", R.string.action_add_to_playlist),
    GhostSearch("ghost_search", R.string.notification_button_ghost_search),
    ;

    companion object {
        fun fromStoredValue(value: String?): NotificationButton? = entries.firstOrNull { it.storedValue == value }
    }
}

/**
 * Ordered notification buttons. [compact] must be a subset of [expanded] because the
 * platform compact view indexes into the expanded action list.
 */
data class NotificationButtonLayout(
    val compact: List<NotificationButton>,
    val expanded: List<NotificationButton>,
) {
    companion object {
        val DEFAULT =
            NotificationButtonLayout(
                compact =
                    listOf(
                        NotificationButton.Previous,
                        NotificationButton.PlayPause,
                        NotificationButton.Next,
                    ),
                expanded =
                    listOf(
                        NotificationButton.Previous,
                        NotificationButton.PlayPause,
                        NotificationButton.Next,
                        NotificationButton.Repeat,
                        NotificationButton.Shuffle,
                    ),
            )
    }
}

object NotificationButtonLayoutPolicy {
    const val MAX_COMPACT_BUTTONS = 3
    const val MAX_EXPANDED_BUTTONS = 5

    fun fromPreferences(
        expandedValue: String?,
        compactValue: String?,
    ): NotificationButtonLayout {
        if (expandedValue == null && compactValue == null) {
            return NotificationButtonLayout.DEFAULT
        }
        val expanded = parse(expandedValue)
        val compact = parse(compactValue)
        if (expanded.isEmpty() && compact.isEmpty()) {
            return NotificationButtonLayout.DEFAULT
        }
        val resolvedExpanded =
            if (expanded.isEmpty()) {
                NotificationButtonLayout.DEFAULT.expanded
            } else {
                expanded
            }
        return normalize(
            NotificationButtonLayout(
                compact =
                    if (compactValue == null) {
                        NotificationButtonLayout.DEFAULT.compact.filter { it in resolvedExpanded }
                    } else {
                        compact
                    },
                expanded = resolvedExpanded,
            ),
        )
    }

    fun normalize(layout: NotificationButtonLayout): NotificationButtonLayout {
        val compact = layout.compact.distinct().take(MAX_COMPACT_BUTTONS)
        val expanded =
            layout.expanded
                .distinct()
                .take(MAX_EXPANDED_BUTTONS)
                .toMutableList()
        compact.forEach { button ->
            if (button !in expanded) {
                if (expanded.size >= MAX_EXPANDED_BUTTONS) {
                    val removable = expanded.indexOfLast { it !in compact }
                    if (removable >= 0) {
                        expanded.removeAt(removable)
                    }
                }
                if (expanded.size < MAX_EXPANDED_BUTTONS) {
                    expanded.add(button)
                }
            }
        }
        val visibleCompact = compact.filter { it in expanded }.take(MAX_COMPACT_BUTTONS)
        return NotificationButtonLayout(compact = visibleCompact, expanded = expanded)
    }

    fun setCompactSlot(
        layout: NotificationButtonLayout,
        slot: Int,
        button: NotificationButton?,
    ): NotificationButtonLayout {
        if (slot !in 0 until MAX_COMPACT_BUTTONS) return layout
        val compact = MutableList<NotificationButton?>(MAX_COMPACT_BUTTONS) { layout.compact.getOrNull(it) }
        if (button != null) {
            compact.indices.forEach { index ->
                if (index != slot && compact[index] == button) compact[index] = null
            }
        }
        compact[slot] = button
        val selected = compact.filterNotNull()
        val expanded = layout.expanded.toMutableList()
        selected.forEach { selectedButton ->
            if (selectedButton !in expanded) {
                if (expanded.size >= MAX_EXPANDED_BUTTONS) {
                    val removable = expanded.indexOfLast { it !in selected }
                    if (removable >= 0) expanded.removeAt(removable)
                }
                if (expanded.size < MAX_EXPANDED_BUTTONS) {
                    expanded.add(selectedButton)
                }
            }
        }
        return normalize(NotificationButtonLayout(selected, expanded))
    }

    fun setExpandedSlot(
        layout: NotificationButtonLayout,
        slot: Int,
        button: NotificationButton?,
    ): NotificationButtonLayout {
        if (slot !in 0 until MAX_EXPANDED_BUTTONS) return layout
        val expanded = MutableList<NotificationButton?>(MAX_EXPANDED_BUTTONS) { layout.expanded.getOrNull(it) }
        if (button != null) {
            expanded.indices.forEach { index ->
                if (index != slot && expanded[index] == button) expanded[index] = null
            }
        }
        expanded[slot] = button
        val selected = expanded.filterNotNull()
        return normalize(
            NotificationButtonLayout(
                compact = layout.compact.filter { it in selected },
                expanded = selected,
            ),
        )
    }

    private fun parse(value: String?): List<NotificationButton> =
        value
            ?.split(',')
            ?.mapNotNull { NotificationButton.fromStoredValue(it.trim()) }
            ?.distinct()
            .orEmpty()
}
