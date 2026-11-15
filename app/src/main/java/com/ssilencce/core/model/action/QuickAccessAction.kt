package com.ssilencce.core.model.action

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.ssilencce.R

/**
 * Actions that can be shown in the configurable quick-access row of the Now Playing screen.
 *
 * The stored value is a comma-separated list of enum names in display order.
 */
enum class QuickAccessAction(
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val titleRes: Int,
) {
    Lyrics(
        iconRes = R.drawable.ic_lyrics_outline_24dp,
        titleRes = R.string.action_show_lyrics,
    ),
    Equalizer(
        iconRes = R.drawable.ic_equalizer_24dp,
        titleRes = R.string.equalizer_label,
    ),
    SleepTimer(
        iconRes = R.drawable.ic_timer_24dp,
        titleRes = R.string.action_sleep_timer,
    ),
    Tempo(
        iconRes = R.drawable.ic_speed_24dp,
        titleRes = R.string.quick_access_tempo_title,
    ),
    PlayQueue(
        iconRes = R.drawable.ic_queue_music_24dp,
        titleRes = R.string.playing_queue_label,
    ),
    TagEditor(
        iconRes = R.drawable.ic_edit_24dp,
        titleRes = R.string.action_tag_editor,
    ),
    AddToPlaylist(
        iconRes = R.drawable.ic_playlist_add_24dp,
        titleRes = R.string.action_add_to_playlist,
    ),
    Bookmark(
        iconRes = R.drawable.ic_bookmark_24dp,
        titleRes = R.string.action_add_bookmark,
    ),
    SongNotes(
        iconRes = R.drawable.ic_edit_note_24dp,
        titleRes = R.string.action_song_notes,
    ),
    ;

    companion object {
        fun fromPreference(value: String?): List<QuickAccessAction> {
            if (value.isNullOrBlank()) return emptyList()
            val result = mutableListOf<QuickAccessAction>()
            value.split(',').forEach { name ->
                val action = entries.firstOrNull { it.name == name.trim() }
                if (action != null && action !in result) result += action
            }
            return result
        }

        fun toPreference(actions: List<QuickAccessAction>): String = actions.distinct().joinToString(separator = ",") { it.name }
    }
}
