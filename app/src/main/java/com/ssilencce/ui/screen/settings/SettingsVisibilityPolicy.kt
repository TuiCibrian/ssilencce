package com.ssilencce.ui.screen.settings

import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import com.ssilencce.util.ALBUM_MINIMUM_SONGS
import com.ssilencce.util.APPBAR_MODE
import com.ssilencce.util.ARTIST_MINIMUM_SONGS
import com.ssilencce.util.AUTO_DELETE_OLDEST_NAMED_QUEUE
import com.ssilencce.util.EXPERIMENTAL_UPDATES
import com.ssilencce.util.FADE_IN_DURATION
import com.ssilencce.util.FADE_OUT_DURATION
import com.ssilencce.util.GAPLESS_PLAYBACK
import com.ssilencce.util.GHOST_MODE
import com.ssilencce.util.HEADSET_BUTTON_MAPPING
import com.ssilencce.util.HOLD_TAB_TO_SEARCH
import com.ssilencce.util.IGNORE_MEDIA_STORE
import com.ssilencce.util.LARGER_HEADER_IMAGE
import com.ssilencce.util.MAXIMUM_SONG_DURATION
import com.ssilencce.util.MINIMUM_SONG_DURATION
import com.ssilencce.util.MP3_INDEX_SEEKING
import com.ssilencce.util.PAUSE_ON_ZERO_VOLUME
import com.ssilencce.util.PREFERRED_IMAGE_SIZE
import com.ssilencce.util.PREVIOUS_BUTTON_ACTION
import com.ssilencce.util.RECURSIVE_FOLDER_ACTIONS
import com.ssilencce.util.REMEMBER_LAST_PAGE
import com.ssilencce.util.REMEMBER_TRACK_POSITION
import com.ssilencce.util.SEEK_CONTROLS_MIN_DURATION
import com.ssilencce.util.SESSION_RECOVERY
import com.ssilencce.util.SHOW_LIST_DATE_MODIFIED
import com.ssilencce.util.STOP_WHEN_CLOSED_FROM_RECENTS
import com.ssilencce.util.TAB_TITLES_MODE
import com.ssilencce.util.UPDATE_SEARCH_MODE
import com.ssilencce.util.USE_ALBUM_ARTIST_IF_ARTIST_MISSING
import com.ssilencce.util.USE_FOLDER_ART
import com.ssilencce.util.WIDGET_SMALL_LAYOUT_STYLE
import com.ssilencce.util.WIDGET_THIRD_LINE_CONTENT

/**
 * Curated set of rarely used preferences hidden by the "Show unimportant settings" toggle.
 * Preferences with their own visibility rules (SDK checks, build flags) are intentionally left
 * out so this policy never overrides them.
 */
object SettingsVisibilityPolicy {
    val unimportantKeys: Set<String> =
        setOf(
            APPBAR_MODE,
            REMEMBER_LAST_PAGE,
            TAB_TITLES_MODE,
            HOLD_TAB_TO_SEARCH,
            LARGER_HEADER_IMAGE,
            SHOW_LIST_DATE_MODIFIED,
            WIDGET_SMALL_LAYOUT_STYLE,
            WIDGET_THIRD_LINE_CONTENT,
            PREVIOUS_BUTTON_ACTION,
            SEEK_CONTROLS_MIN_DURATION,
            FADE_IN_DURATION,
            FADE_OUT_DURATION,
            GAPLESS_PLAYBACK,
            HEADSET_BUTTON_MAPPING,
            REMEMBER_TRACK_POSITION,
            SESSION_RECOVERY,
            PAUSE_ON_ZERO_VOLUME,
            MP3_INDEX_SEEKING,
            STOP_WHEN_CLOSED_FROM_RECENTS,
            AUTO_DELETE_OLDEST_NAMED_QUEUE,
            RECURSIVE_FOLDER_ACTIONS,
            USE_ALBUM_ARTIST_IF_ARTIST_MISSING,
            ARTIST_MINIMUM_SONGS,
            ALBUM_MINIMUM_SONGS,
            MINIMUM_SONG_DURATION,
            MAXIMUM_SONG_DURATION,
            IGNORE_MEDIA_STORE,
            USE_FOLDER_ART,
            PREFERRED_IMAGE_SIZE,
            UPDATE_SEARCH_MODE,
            EXPERIMENTAL_UPDATES,
            GHOST_MODE,
        )

    fun apply(
        screen: PreferenceScreen,
        showUnimportant: Boolean,
        affectedCategories: MutableMap<Preference, Boolean>,
    ) {
        unimportantKeys.forEach { key ->
            screen.findPreference<Preference>(key)?.isVisible = showUnimportant
        }
        hideEmptyAffectedCategories(screen, affectedCategories)
    }

    private fun hideEmptyAffectedCategories(
        group: PreferenceGroup,
        affectedCategories: MutableMap<Preference, Boolean>,
    ) {
        for (index in 0 until group.preferenceCount) {
            val child = group.getPreference(index)
            if (child !is PreferenceGroup) continue
            hideEmptyAffectedCategories(child, affectedCategories)
            if (child is PreferenceCategory && child.containsUnimportantPreference()) {
                val baseVisibility = affectedCategories.getOrPut(child) { child.isVisible }
                child.isVisible = baseVisibility && child.hasVisiblePreference()
            }
        }
    }

    private fun PreferenceGroup.containsUnimportantPreference(): Boolean {
        for (index in 0 until preferenceCount) {
            if (getPreference(index).key in unimportantKeys) return true
        }
        return false
    }

    private fun PreferenceGroup.hasVisiblePreference(): Boolean {
        for (index in 0 until preferenceCount) {
            if (getPreference(index).isVisible) return true
        }
        return false
    }
}
