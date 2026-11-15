package com.ssilencce.core.model.menu

import androidx.annotation.IdRes
import androidx.annotation.MenuRes
import androidx.annotation.StringRes
import com.ssilencce.R

/**
 * A single configurable action of an entity option menu.
 *
 * [key] is stored in preferences instead of [itemId] because resource ids are not
 * stable across app builds.
 */
data class MenuCustomizationItem(
    val key: String,
    @param:IdRes val itemId: Int,
    @param:StringRes val titleRes: Int,
)

/**
 * Persisted customization of one entity menu: display order (keys of all known items)
 * and the keys of the hidden actions.
 */
data class MenuCustomizationState(
    val order: List<String>,
    val hidden: Set<String>,
) {
    fun isEnabled(key: String): Boolean = key !in hidden

    fun withEnabled(
        key: String,
        enabled: Boolean,
    ): MenuCustomizationState = copy(hidden = if (enabled) hidden - key else hidden + key)

    fun moved(
        key: String,
        offset: Int,
    ): MenuCustomizationState {
        val index = order.indexOf(key)
        val target = index + offset
        if (index < 0 || target !in order.indices) return this
        return copy(
            order = order.toMutableList().apply { add(target, removeAt(index)) },
        )
    }

    fun toPreference(): String =
        order.joinToString(separator = ",") { key ->
            if (isEnabled(key)) key else hiddenPrefix + key
        }

    companion object {
        private const val hiddenPrefix = "-"

        fun default(group: MenuCustomizationGroup): MenuCustomizationState =
            MenuCustomizationState(
                order = group.items.map { it.key },
                hidden = emptySet(),
            )

        fun fromPreference(
            group: MenuCustomizationGroup,
            value: String?,
        ): MenuCustomizationState {
            if (value.isNullOrBlank()) return default(group)
            val order = mutableListOf<String>()
            val hidden = mutableSetOf<String>()
            value
                .split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { token ->
                    val isHidden = token.startsWith(hiddenPrefix)
                    val key = if (isHidden) token.substring(hiddenPrefix.length) else token
                    if (group.itemByKey(key) != null && key !in order) {
                        order += key
                        if (isHidden) hidden += key
                    }
                }
            // Actions added by a newer app version keep their default (enabled) state.
            group.items.forEach { item ->
                if (item.key !in order) order += item.key
            }
            return MenuCustomizationState(order, hidden)
        }
    }
}

/**
 * Configurable option menus, one per library entity.
 *
 * Each catalog lists the menu items in their current XML order; that order is the
 * default when the user has never customized the menu.
 */
enum class MenuCustomizationGroup(
    val preferenceKey: String,
    @param:StringRes val titleRes: Int,
    @param:MenuRes val menuRes: Int,
    val items: List<MenuCustomizationItem>,
) {
    Song(
        preferenceKey = "menu_customization_song",
        titleRes = R.string.menu_customization_song_title,
        menuRes = R.menu.menu_item_song,
        items =
            listOf(
                MenuCustomizationItem("play", R.id.action_play, R.string.action_play),
                MenuCustomizationItem("play_next", R.id.action_play_next, R.string.action_queue_next),
                MenuCustomizationItem(
                    "add_to_playing_queue",
                    R.id.action_add_to_playing_queue,
                    R.string.action_add_to_playing_queue,
                ),
                MenuCustomizationItem(
                    "pause_after_this_song",
                    R.id.action_pause_after_this_song,
                    R.string.queue_action_pause_after_this_song,
                ),
                MenuCustomizationItem(
                    "play_after_current",
                    R.id.action_play_after_current,
                    R.string.queue_action_play_after_current,
                ),
                MenuCustomizationItem(
                    "load_next_and_pause",
                    R.id.action_load_next_and_pause,
                    R.string.queue_action_load_next_and_pause,
                ),
                MenuCustomizationItem("add_to_playlist", R.id.action_add_to_playlist, R.string.action_add_to_playlist),
                MenuCustomizationItem("go_to", R.id.menu_go_to, R.string.go_to_label),
                MenuCustomizationItem("file_actions", R.id.menu_file_actions, R.string.action_file_actions),
                MenuCustomizationItem("tag_editor", R.id.action_tag_editor, R.string.action_tag_editor),
                MenuCustomizationItem(
                    "calculate_replay_gain",
                    R.id.action_calculate_replay_gain,
                    R.string.action_calculate_replay_gain,
                ),
                MenuCustomizationItem("share", R.id.action_share, R.string.action_share),
                MenuCustomizationItem(
                    "delete_from_device",
                    R.id.action_delete_from_device,
                    R.string.action_delete_from_device,
                ),
                MenuCustomizationItem("details", R.id.action_details, R.string.action_details),
            ),
    ),
    Album(
        preferenceKey = "menu_customization_album",
        titleRes = R.string.menu_customization_album_title,
        menuRes = R.menu.menu_item_album,
        items =
            listOf(
                MenuCustomizationItem("shuffle_play", R.id.action_shuffle_play, R.string.action_shuffle_play),
                MenuCustomizationItem("play_next", R.id.action_play_next, R.string.action_queue_next),
                MenuCustomizationItem(
                    "add_to_playing_queue",
                    R.id.action_add_to_playing_queue,
                    R.string.action_add_to_playing_queue,
                ),
                MenuCustomizationItem("add_to_playlist", R.id.action_add_to_playlist, R.string.action_add_to_playlist),
                MenuCustomizationItem("add_to_home", R.id.action_add_to_home, R.string.add_to_home),
                MenuCustomizationItem("go_to_artist", R.id.action_go_to_artist, R.string.action_go_to_artist),
                MenuCustomizationItem("tag_editor", R.id.action_tag_editor, R.string.action_tag_editor),
                MenuCustomizationItem(
                    "delete_from_device",
                    R.id.action_delete_from_device,
                    R.string.action_delete_from_device,
                ),
            ),
    ),
    Artist(
        preferenceKey = "menu_customization_artist",
        titleRes = R.string.menu_customization_artist_title,
        menuRes = R.menu.menu_item_artist,
        items =
            listOf(
                MenuCustomizationItem("shuffle_play", R.id.action_shuffle_play, R.string.action_shuffle_play),
                MenuCustomizationItem("play_next", R.id.action_play_next, R.string.action_queue_next),
                MenuCustomizationItem(
                    "add_to_playing_queue",
                    R.id.action_add_to_playing_queue,
                    R.string.action_add_to_playing_queue,
                ),
                MenuCustomizationItem("add_to_playlist", R.id.action_add_to_playlist, R.string.action_add_to_playlist),
                MenuCustomizationItem("add_to_home", R.id.action_add_to_home, R.string.add_to_home),
                MenuCustomizationItem("tag_editor", R.id.action_tag_editor, R.string.action_tag_editor),
                MenuCustomizationItem(
                    "delete_from_device",
                    R.id.action_delete_from_device,
                    R.string.action_delete_from_device,
                ),
            ),
    ),
    Genre(
        preferenceKey = "menu_customization_genre",
        titleRes = R.string.menu_customization_genre_title,
        menuRes = R.menu.menu_genre_detail,
        items =
            listOf(
                MenuCustomizationItem(
                    "genre_subcategory",
                    R.id.action_genre_subcategory,
                    R.string.action_genre_subcategory,
                ),
                MenuCustomizationItem("play_next", R.id.action_play_next, R.string.action_queue_next),
                MenuCustomizationItem("add_to", R.id.menu_add_to, R.string.add_to_label),
            ),
    ),
    Playlist(
        preferenceKey = "menu_customization_playlist",
        titleRes = R.string.menu_customization_playlist_title,
        menuRes = R.menu.menu_item_playlist,
        items =
            listOf(
                MenuCustomizationItem("shuffle_play", R.id.action_shuffle_play, R.string.action_shuffle_play),
                MenuCustomizationItem("play_next", R.id.action_play_next, R.string.action_queue_next),
                MenuCustomizationItem(
                    "add_to_playing_queue",
                    R.id.action_add_to_playing_queue,
                    R.string.action_add_to_playing_queue,
                ),
                MenuCustomizationItem("add_to_playlist", R.id.action_add_to_playlist, R.string.action_add_to_playlist),
                MenuCustomizationItem("edit_playlist", R.id.action_edit_playlist, R.string.action_edit_playlist),
                MenuCustomizationItem("export_playlist", R.id.action_export_playlist, R.string.action_export_playlist),
                MenuCustomizationItem("add_to_home", R.id.action_add_to_home, R.string.add_to_home),
                MenuCustomizationItem("delete_playlist", R.id.action_delete_playlist, R.string.action_delete),
            ),
    ),
    Folder(
        preferenceKey = "menu_customization_folder",
        titleRes = R.string.menu_customization_folder_title,
        menuRes = R.menu.menu_item_directory,
        items =
            listOf(
                MenuCustomizationItem("play", R.id.action_play, R.string.action_play),
                MenuCustomizationItem("shuffle_play", R.id.action_shuffle_play, R.string.action_shuffle_play),
                MenuCustomizationItem("play_next", R.id.action_play_next, R.string.action_queue_next),
                MenuCustomizationItem(
                    "add_to_playing_queue",
                    R.id.action_add_to_playing_queue,
                    R.string.action_add_to_playing_queue,
                ),
                MenuCustomizationItem("add_to_playlist", R.id.action_add_to_playlist, R.string.action_add_to_playlist),
                MenuCustomizationItem("folder_info", R.id.action_folder_info, R.string.folder_info_title),
                MenuCustomizationItem("rename_folder", R.id.action_rename_folder, R.string.action_rename_folder),
                MenuCustomizationItem("blacklist", R.id.action_blacklist, R.string.blacklist_directory),
                MenuCustomizationItem(
                    "set_as_start_directory",
                    R.id.action_set_as_start_directory,
                    R.string.action_set_as_start_directory,
                ),
                MenuCustomizationItem("scan", R.id.action_scan, R.string.action_scan),
                MenuCustomizationItem(
                    "delete_from_device",
                    R.id.action_delete_from_device,
                    R.string.action_delete_from_device,
                ),
            ),
    ),
    ;

    fun itemByKey(key: String): MenuCustomizationItem? = items.firstOrNull { it.key == key }

    companion object {
        fun fromPreferenceKey(key: String?): MenuCustomizationGroup? = entries.firstOrNull { it.preferenceKey == key }

        fun fromMenuRes(
            @MenuRes menuRes: Int,
        ): MenuCustomizationGroup? = entries.firstOrNull { it.menuRes == menuRes }
    }
}
