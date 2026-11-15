/*
 * Copyright (c) 2024 Christians Martínez Alvarado
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

package com.ssilencce.util

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigation.NavigationBarView.LabelVisibility
import com.ssilencce.R
import com.ssilencce.core.model.CategoryInfo
import com.ssilencce.core.model.Cutoff
import com.ssilencce.core.model.NotPlayedDaysPolicy
import com.ssilencce.core.model.action.FolderAction
import com.ssilencce.core.model.action.HeadsetButtonAction
import com.ssilencce.core.model.action.NowPlayingAction
import com.ssilencce.core.model.action.QueueClearingBehavior
import com.ssilencce.core.model.action.QuickAccessAction
import com.ssilencce.core.model.action.SongClickBehavior
import com.ssilencce.core.model.list.SongTitleMode
import com.ssilencce.core.model.lockscreen.LockScreenWallpaper
import com.ssilencce.core.model.menu.MenuCustomizationGroup
import com.ssilencce.core.model.menu.MenuCustomizationState
import com.ssilencce.core.model.player.MetadataField
import com.ssilencce.core.model.player.NotificationBackgroundMode
import com.ssilencce.core.model.player.NotificationButtonLayout
import com.ssilencce.core.model.player.NotificationButtonLayoutPolicy
import com.ssilencce.core.model.player.NotificationStyle
import com.ssilencce.core.model.player.PlayerColorSchemeMode
import com.ssilencce.core.model.player.PlayerTransition
import com.ssilencce.core.model.shuffle.GroupShuffleMode
import com.ssilencce.core.model.shuffle.ShuffleBehavior
import com.ssilencce.core.model.theme.AppTheme
import com.ssilencce.core.model.theme.NowPlayingScreen
import com.ssilencce.core.sort.IgnoreKeywordPolicy
import com.ssilencce.extensions.files.getCanonicalPathSafe
import com.ssilencce.extensions.hasQ
import com.ssilencce.extensions.hasS
import com.ssilencce.extensions.intRes
import com.ssilencce.extensions.utilities.calendarSingleton
import com.ssilencce.extensions.utilities.deserialize
import com.ssilencce.extensions.utilities.getCutoffTimeMillis
import com.ssilencce.extensions.utilities.serialize
import com.ssilencce.extensions.utilities.toEnum
import com.ssilencce.playback.HeadsetButtonPolicy
import com.ssilencce.ui.component.views.TopAppBarLayout
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

/**
 * @author Christians M. A. (mardous)
 */
object Preferences : KoinComponent {
    private val preferences: SharedPreferences by inject()

    fun registerOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener) {
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener) {
        preferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    fun getGeneralTheme(isBlackMode: Boolean): String =
        if (isBlackMode) {
            GeneralTheme.BLACK
        } else {
            preferences.requireString(GENERAL_THEME, GeneralTheme.AUTO)
        }

    var generalTheme: String
        get() = getGeneralTheme(blackTheme)
        set(value) = preferences.edit { putString(GENERAL_THEME, value) }

    fun getThemeMode(themeName: String) =
        when (themeName) {
            GeneralTheme.LIGHT -> AppTheme.Mode.Light
            GeneralTheme.DARK -> AppTheme.Mode.Dark
            GeneralTheme.BLACK -> AppTheme.Mode.Black
            else -> AppTheme.Mode.FollowSystem
        }

    fun getDayNightMode(themeName: String = generalTheme) =
        if (isAuroraAppearance) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            when (themeName) {
                GeneralTheme.LIGHT -> {
                    AppCompatDelegate.MODE_NIGHT_NO
                }

                GeneralTheme.DARK,
                GeneralTheme.BLACK,
                -> {
                    AppCompatDelegate.MODE_NIGHT_YES
                }

                else -> {
                    if (hasQ()) {
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    } else {
                        AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                    }
                }
            }
        }

    val blackTheme: Boolean
        get() = preferences.getBoolean(BLACK_THEME, false)

    val isAuroraAppearance: Boolean
        get() = preferences.getBoolean(AURORA_APPEARANCE, false)

    val auroraMotion: Boolean
        get() = preferences.getBoolean(AURORA_MOTION, true)

    val isMaterialYouTheme: Boolean
        get() = preferences.getBoolean(MATERIAL_YOU, hasS())

    val isCustomFont: Boolean
        get() = preferences.getBoolean(USE_CUSTOM_FONT, true)

    /**
     * Custom accent color as an ARGB int, or 0 when the theme-derived accent should be used.
     */
    var accentColor: Int
        get() = preferences.getInt(ACCENT_COLOR, 0)
        set(value) =
            preferences.edit {
                if (value == 0) remove(ACCENT_COLOR) else putInt(ACCENT_COLOR, value)
            }

    /**
     * Whether rarely used preferences are shown. Defaults to true so the settings layout is
     * unchanged until the user opts into a simpler list.
     */
    val showUnimportantSettings: Boolean
        get() = preferences.getBoolean(SHOW_UNIMPORTANT_SETTINGS, true)

    /**
     * When true, artwork fetchers skip decoding and the UI falls back to placeholders.
     */
    val disableAlbumArt: Boolean
        get() = preferences.getBoolean(DISABLE_ALBUM_ART, false)

    val appBarMode: TopAppBarLayout.AppBarMode
        get() =
            when (preferences.requireString(APPBAR_MODE, AppBarMode.COMPACT)) {
                AppBarMode.COMPACT -> TopAppBarLayout.AppBarMode.SIMPLE
                AppBarMode.EXPANDED -> TopAppBarLayout.AppBarMode.COLLAPSING
                else -> TopAppBarLayout.AppBarMode.SIMPLE
            }

    var libraryCategories: List<CategoryInfo>
        get() {
            val stored =
                preferences.nullString(LIBRARY_CATEGORIES).deserialize(
                    getDefaultLibraryCategoryInfos(),
                )
            val available = stored.filter { it.category.isAvailable }
            val present = available.map { it.category }.toSet()
            val missing =
                CategoryInfo.Category.entries
                    .filter { it.isAvailable && it !in present }
                    .map { CategoryInfo(it, false) }
            return available + missing
        }
        set(categories) = preferences.edit { putString(LIBRARY_CATEGORIES, categories.serialize()) }

    fun getDefaultLibraryCategoryInfos() =
        CategoryInfo.Category.entries
            .filter { it.isAvailable }
            .mapIndexed { index, category ->
                CategoryInfo(category, index < CategoryInfo.MAX_VISIBLE_CATEGORIES)
            }

    val isRememberLastPage: Boolean
        get() = preferences.getBoolean(REMEMBER_LAST_PAGE, true)

    var lastPage: Int
        get() = preferences.getInt(LAST_PAGE, 0)
        set(value) = preferences.edit { putInt(LAST_PAGE, value) }

    @LabelVisibility
    val bottomTitlesMode: Int
        get() =
            when (preferences.nullString(TAB_TITLES_MODE)) {
                BottomTitlesMode.SELECTED -> NavigationBarView.LABEL_VISIBILITY_SELECTED
                BottomTitlesMode.LABELED -> NavigationBarView.LABEL_VISIBILITY_LABELED
                BottomTitlesMode.UNLABELED -> NavigationBarView.LABEL_VISIBILITY_UNLABELED
                else -> NavigationBarView.LABEL_VISIBILITY_SELECTED
            }

    val holdTabToSearch: Boolean
        get() = preferences.getBoolean(HOLD_TAB_TO_SEARCH, true)

    val ignoreArticlesWhenSorting: Boolean
        get() = preferences.getBoolean(IGNORE_ARTICLES_WHEN_SORTING, false)

    var sortingIgnoreKeywords: Set<String>
        get() =
            IgnoreKeywordPolicy
                .sanitize(preferences.getStringSet(SORTING_IGNORE_KEYWORDS, emptySet()).orEmpty())
                .toSet()
        set(value) =
            preferences.edit {
                putStringSet(
                    SORTING_IGNORE_KEYWORDS,
                    IgnoreKeywordPolicy.sanitize(value).toSet(),
                )
            }

    var lockedPlaylists: Boolean
        get() = preferences.getBoolean(LOCKED_PLAYLISTS, false)
        set(value) = preferences.edit { putBoolean(LOCKED_PLAYLISTS, value) }

    var queueHeight: Boolean
        get() = preferences.getBoolean(QUEUE_HEIGHT, false)
        set(value) = preferences.edit { putBoolean(QUEUE_HEIGHT, value) }

    val largerHeaderImage: Boolean
        get() = preferences.getBoolean(LARGER_HEADER_IMAGE, false)

    var horizontalArtistAlbums: Boolean
        get() = preferences.getBoolean(HORIZONTAL_ARTIST_ALBUMS, true)
        set(value) = preferences.edit { putBoolean(HORIZONTAL_ARTIST_ALBUMS, value) }

    var compactAlbumSongView: Boolean
        get() = preferences.getBoolean(COMPACT_ALBUM_SONG_VIEW, false)
        set(value) = preferences.edit { putBoolean(COMPACT_ALBUM_SONG_VIEW, value) }

    var compactArtistSongView: Boolean
        get() = preferences.getBoolean(COMPACT_ARTIST_SONG_VIEW, false)
        set(value) = preferences.edit { putBoolean(COMPACT_ARTIST_SONG_VIEW, value) }

    var nowPlayingScreen: NowPlayingScreen
        get() = preferences.enumValue(NOW_PLAYING_SCREEN, NowPlayingScreen.Default)
        set(value) = preferences.edit { putString(NOW_PLAYING_SCREEN, value.name) }

    val extraControls: Boolean
        get() = preferences.getBoolean(ADD_EXTRA_CONTROLS, false)

    val adaptiveControls: Boolean
        get() = preferences.getBoolean(ADAPTIVE_CONTROLS, false)

    val squigglySeekBar: Boolean
        get() = preferences.getBoolean(SQUIGGLY_SEEK_BAR, false)

    val swipeDownToDismiss: Boolean
        get() = preferences.getBoolean(SWIPE_DOWN_TO_DISMISS, false)

    var showLyricsOnCover: Boolean
        get() = preferences.getBoolean(LYRICS_ON_COVER, false)
        set(value) = preferences.edit { putBoolean(LYRICS_ON_COVER, value) }

    val swipeOnCover: Boolean
        get() = preferences.getBoolean(SWIPE_ON_COVER, true)

    val miniPlayerSwipeToSkip: Boolean
        get() = preferences.getBoolean(MINI_PLAYER_SWIPE_TO_SKIP, true)

    var isQueueLocked: Boolean
        get() = preferences.getBoolean(LOCKED_QUEUE, false)
        set(value) = preferences.edit { putBoolean(LOCKED_QUEUE, value) }

    fun getNowPlayingColorSchemeKey(nps: NowPlayingScreen) = "player_${nps.name.lowercase()}_color_scheme"

    val audioFocusMode: AudioFocusMode
        get() =
            if (preferences.getBoolean(IGNORE_AUDIO_FOCUS, false)) {
                AudioFocusMode.Ignore
            } else {
                AudioFocusMode.fromPreference(preferences.nullString(AUDIO_FOCUS_MODE))
            }

    fun getNowPlayingColorSchemeMode(nps: NowPlayingScreen): PlayerColorSchemeMode {
        val defaultScheme = nps.defaultColorScheme
        val schemeName =
            preferences.nullString(getNowPlayingColorSchemeKey(nps))
                ?: defaultScheme.name
        if (nps.supportedColorSchemes.any { it.name == schemeName }) {
            return schemeName.toEnum<PlayerColorSchemeMode>() ?: defaultScheme
        }
        return defaultScheme
    }

    fun setNowPlayingColorSchemeMode(
        nps: NowPlayingScreen,
        schemeMode: PlayerColorSchemeMode,
    ) {
        val schemeName = schemeMode.name
        if (nps.supportedColorSchemes.any { it.name == schemeName }) {
            preferences.edit {
                putString(getNowPlayingColorSchemeKey(nps), schemeName)
            }
        }
    }

    val isSmallImage: Boolean
        get() = preferences.getBoolean(NOW_PLAYING_SMALL_IMAGE, false)

    fun getNowPlayingImageCornerRadius(context: Context): Int = preferences.getInt(NOW_PLAYING_IMAGE_CORNER_RADIUS, context.intRes(R.integer.now_playing_corner_radius))

    val isCarouselEffect: Boolean
        get() = preferences.getBoolean(CAROUSEL_EFFECT, false)

    fun getNowPlayingTransitionKey(nps: NowPlayingScreen) = "player_${nps.name.lowercase()}_transition"

    fun getNowPlayingTransition(nps: NowPlayingScreen): PlayerTransition {
        val defaultTransition = nps.defaultTransition
        val transitionName =
            preferences.nullString(getNowPlayingTransitionKey(nps))
                ?: defaultTransition.name
        if (nps.supportedTransitions.any { it.name == transitionName }) {
            return transitionName.toEnum<PlayerTransition>() ?: defaultTransition
        }
        return defaultTransition
    }

    fun setNowPlayingTransition(
        nps: NowPlayingScreen,
        transition: PlayerTransition,
    ) {
        if (nps.supportedTransitions.contains(transition)) {
            preferences.edit {
                putString(getNowPlayingTransitionKey(nps), transition.name)
            }
        }
    }

    val coverSingleTapAction: NowPlayingAction
        get() = preferences.enumValue(COVER_SINGLE_TAP_ACTION, NowPlayingAction.TogglePlayState)

    val coverDoubleTapAction: NowPlayingAction
        get() = preferences.enumValue(COVER_DOUBLE_TAP_ACTION, NowPlayingAction.WebSearch)

    val coverLeftDoubleTapAction: NowPlayingAction
        get() = preferences.enumValue(COVER_LEFT_DOUBLE_TAP_ACTION, NowPlayingAction.SeekBackward)

    val coverRightDoubleTapAction: NowPlayingAction
        get() = preferences.enumValue(COVER_RIGHT_DOUBLE_TAP_ACTION, NowPlayingAction.SeekForward)

    val coverLongPressAction: NowPlayingAction
        get() = preferences.enumValue(COVER_LONG_PRESS_ACTION, NowPlayingAction.SleepTimer)

    val keepScreenOnForNowPlaying: Boolean
        get() = preferences.getBoolean(KEEP_SCREEN_ON_NOW_PLAYING, false)

    var lockScreenPlayerEnabled: Boolean
        get() = preferences.getBoolean(LOCK_SCREEN_PLAYER_ENABLED, false)
        set(value) = preferences.edit { putBoolean(LOCK_SCREEN_PLAYER_ENABLED, value) }

    var lockScreenWallpaper: LockScreenWallpaper
        get() = preferences.enumValue(LOCK_SCREEN_WALLPAPER, LockScreenWallpaper.Cover)
        set(value) = preferences.edit { putString(LOCK_SCREEN_WALLPAPER, value.name) }

    var lockScreenWallpaperUri: String?
        get() = preferences.nullString(LOCK_SCREEN_WALLPAPER_URI)
        set(value) = preferences.edit { putString(LOCK_SCREEN_WALLPAPER_URI, value) }

    var lockScreenSkipWhenUnlocked: Boolean
        get() = preferences.getBoolean(LOCK_SCREEN_SKIP_WHEN_UNLOCKED, true)
        set(value) = preferences.edit { putBoolean(LOCK_SCREEN_SKIP_WHEN_UNLOCKED, value) }

    var lockScreenSkipWhenPaused: Boolean
        get() = preferences.getBoolean(LOCK_SCREEN_SKIP_WHEN_PAUSED, true)
        set(value) = preferences.edit { putBoolean(LOCK_SCREEN_SKIP_WHEN_PAUSED, value) }

    var lockScreenGesturesEnabled: Boolean
        get() = preferences.getBoolean(LOCK_SCREEN_GESTURES, true)
        set(value) = preferences.edit { putBoolean(LOCK_SCREEN_GESTURES, value) }

    var lockScreenShortcutsEnabled: Boolean
        get() = preferences.getBoolean(LOCK_SCREEN_SHORTCUTS, true)
        set(value) = preferences.edit { putBoolean(LOCK_SCREEN_SHORTCUTS, value) }

    val seekBarTouchOnlyOnThumb: Boolean
        get() = preferences.getBoolean(SEEK_BAR_TOUCH_ONLY_ON_THUMB, false)

    var quickAccessActions: List<QuickAccessAction>
        get() = QuickAccessAction.fromPreference(preferences.getString(NOW_PLAYING_QUICK_ACCESS, null))
        set(value) = preferences.edit { putString(NOW_PLAYING_QUICK_ACCESS, QuickAccessAction.toPreference(value)) }

    val quickAccessActionsRaw: String
        get() = preferences.getString(NOW_PLAYING_QUICK_ACCESS, "").orEmpty()

    fun menuCustomization(group: MenuCustomizationGroup): MenuCustomizationState =
        MenuCustomizationState.fromPreference(
            group,
            preferences.getString(group.preferenceKey, null),
        )

    fun setMenuCustomization(
        group: MenuCustomizationGroup,
        state: MenuCustomizationState,
    ) {
        preferences.edit { putString(group.preferenceKey, state.toPreference()) }
    }

    val animateControls: Boolean
        get() = preferences.getBoolean(ANIMATE_PLAYER_CONTROL, true)

    val circularPlayButton: Boolean
        get() = preferences.getBoolean(CIRCLE_PLAY_BUTTON, false)

    val enableScrollingText: Boolean
        get() = preferences.getBoolean(ENABLE_SCROLLING_TEXT, false)

    val displayAlbumTitle
        get() = preferences.getBoolean(DISPLAY_ALBUM_TITLE, true)

    val displayExtraInfo: Boolean
        get() = preferences.getBoolean(DISPLAY_EXTRA_INFO, false)

    var songTitleMode: SongTitleMode
        get() = preferences.enumValue(SONG_TITLE_MODE, SongTitleMode.FollowSort)
        set(value) = preferences.edit { putString(SONG_TITLE_MODE, value.name) }

    val showListDateModified: Boolean
        get() = preferences.getBoolean(SHOW_LIST_DATE_MODIFIED, false)

    fun getExtraInfoContent(
        key: String,
        defaultContent: List<MetadataField>,
    ) = preferences.nullString(key).deserialize(defaultContent)

    fun setExtraInfoContent(
        key: String,
        newContent: List<MetadataField>,
    ) {
        preferences.edit { putString(key, newContent.serialize()) }
    }

    fun getDefaultNowPlayingInfo(): List<MetadataField> =
        MetadataField.Content.entries.map { content ->
            MetadataField(
                content,
                content == MetadataField.Content.Format ||
                    content == MetadataField.Content.Bitrate ||
                    content == MetadataField.Content.SampleRate,
            )
        }

    fun getDefaultWidgetInfo(): List<MetadataField> =
        MetadataField.Content.entries.map { tag ->
            MetadataField(tag, tag == MetadataField.Content.Album)
        }

    var preferRemainingTime: Boolean
        get() = preferences.getBoolean(PREFER_REMAINING_TIME, false)
        set(value) = preferences.edit { putBoolean(PREFER_REMAINING_TIME, value) }

    var preferAlbumArtistName: Boolean
        get() = preferences.getBoolean(PREFER_ALBUM_ARTIST_NAME, false)
        set(value) = preferences.edit { putBoolean(PREFER_ALBUM_ARTIST_NAME, value) }

    var useAlbumArtistIfArtistMissing: Boolean
        get() = preferences.getBoolean(USE_ALBUM_ARTIST_IF_ARTIST_MISSING, false)
        set(value) = preferences.edit { putBoolean(USE_ALBUM_ARTIST_IF_ARTIST_MISSING, value) }

    var clearQueueAction: QueueClearingBehavior
        get() = preferences.enumValueByOrdinal(ON_CLEAR_QUEUE_ACTION, QueueClearingBehavior.RemoveAllSongs)
        set(value) = preferences.edit { putInt(ON_CLEAR_QUEUE_ACTION, value.ordinal) }

    /**
     * Shows the fourth repeat state in the repeat-button cycle. Off by default so the existing
     * three-state button behavior is unchanged.
     */
    val repeatLoadNextAndPauseAvailable: Boolean
        get() = preferences.getBoolean(REPEAT_LOAD_NEXT_AND_PAUSE_AVAILABLE, false)

    /** The player-level "load next and pause" policy is independent from Media3 repeat mode. */
    var repeatLoadNextAndPauseActive: Boolean
        get() = preferences.getBoolean(REPEAT_LOAD_NEXT_AND_PAUSE_ACTIVE, false)
        set(value) = preferences.edit { putBoolean(REPEAT_LOAD_NEXT_AND_PAUSE_ACTIVE, value) }

    val autoDeleteOldestNamedQueue: Boolean
        get() = preferences.getBoolean(AUTO_DELETE_OLDEST_NAMED_QUEUE, false)

    var songClickAction: SongClickBehavior
        get() = preferences.enumValueByOrdinal(ON_SONG_CLICK_ACTION, SongClickBehavior.PlayWholeList)
        set(value) = preferences.edit { putInt(ON_SONG_CLICK_ACTION, value.ordinal) }

    val playOptionAlwaysVisible: Boolean
        get() = preferences.getBoolean(PLAY_OPTION_ALWAYS_VISIBLE, false)

    val playOptionClickBehavior: SongClickBehavior
        get() =
            if (preferences.getBoolean(PLAY_OPTION_PLAYS_WHOLE_LIST, false)) {
                SongClickBehavior.PlayWholeList
            } else {
                SongClickBehavior.PlayOnlyThisSong
            }

    val playAllSongsWhenSearching: Boolean
        get() = preferences.getBoolean(PLAY_ALL_SONGS_WHEN_SEARCHING, false)

    fun searchHistory(): List<String> =
        preferences
            .getString(SEARCH_HISTORY, null)
            ?.split(SEARCH_HISTORY_SEPARATOR)
            ?.filter { it.isNotBlank() }
            .orEmpty()

    fun addSearchHistory(query: String) {
        val normalized = query.trim()
        if (normalized.isEmpty()) return
        val updated =
            (listOf(normalized) + searchHistory().filterNot { it.equals(normalized, ignoreCase = true) })
                .take(MAX_SEARCH_HISTORY)
        preferences.edit { putString(SEARCH_HISTORY, updated.joinToString(SEARCH_HISTORY_SEPARATOR)) }
    }

    fun removeSearchHistory(query: String) {
        val updated = searchHistory().filterNot { it == query }
        if (updated.isEmpty()) {
            preferences.edit { remove(SEARCH_HISTORY) }
        } else {
            preferences.edit { putString(SEARCH_HISTORY, updated.joinToString(SEARCH_HISTORY_SEPARATOR)) }
        }
    }

    fun clearSearchHistory() {
        preferences.edit { remove(SEARCH_HISTORY) }
    }

    val albumShuffleMode: GroupShuffleMode
        get() = getGroupShuffleMode(ALBUM_SHUFFLE_MODE, SelectedShuffleMode.SHUFFLE_ALBUMS)

    val artistShuffleMode: GroupShuffleMode
        get() = getGroupShuffleMode(ARTIST_SHUFFLE_MODE, SelectedShuffleMode.SHUFFLE_ALL)

    private fun getGroupShuffleMode(
        key: String,
        default: String,
    ) = when (preferences.requireString(key, default)) {
        SelectedShuffleMode.SHUFFLE_ARTISTS,
        SelectedShuffleMode.SHUFFLE_ALBUMS,
        -> GroupShuffleMode.ByGroup

        SelectedShuffleMode.SHUFFLE_SONGS -> GroupShuffleMode.BySong

        else -> GroupShuffleMode.FullRandom
    }

    fun isResumeOnConnect(bluetooth: Boolean) =
        when {
            bluetooth -> preferences.getBoolean(RESUME_ON_BLUETOOTH_CONNECT, false)
            else -> preferences.getBoolean(RESUME_ON_CONNECT, false)
        }

    fun isPauseOnDisconnect(bluetooth: Boolean) =
        when {
            bluetooth -> preferences.getBoolean(PAUSE_ON_BLUETOOTH_DISCONNECT, false)
            else -> preferences.getBoolean(PAUSE_ON_DISCONNECT, false)
        }

    fun isResumeOnlyIfPausedByDisconnect(): Boolean = preferences.getBoolean(RESUME_ONLY_IF_PAUSED_BY_DISCONNECT, false)

    val preventUnwantedAutoplay: Boolean
        get() = preferences.getBoolean(PREVENT_UNWANTED_AUTOPLAY, false)

    val preventUnwantedAutoplayStrict: Boolean
        get() = preventUnwantedAutoplay && preferences.getBoolean(PREVENT_UNWANTED_AUTOPLAY_STRICT, false)

    val preventPlaybackWhenMuted: Boolean
        get() = preferences.getBoolean(PREVENT_PLAYBACK_WHEN_MUTED, false)

    val fadeInDurationMs: Int
        get() = preferences.getInt(FADE_IN_DURATION, 0).coerceIn(0, MAX_FADE_DURATION_MS)

    val fadeOutDurationMs: Int
        get() = preferences.getInt(FADE_OUT_DURATION, 0).coerceIn(0, MAX_FADE_DURATION_MS)

    val crossfadeDurationMs: Int
        get() = preferences.getInt(CROSSFADE_DURATION, 0).coerceIn(0, MAX_CROSSFADE_DURATION_MS)

    val crossfadeOnManualSkip: Boolean
        get() = preferences.getBoolean(CROSSFADE_ON_MANUAL_SKIP, false)

    val crossfadeRespectsGaplessAlbums: Boolean
        get() = preferences.getBoolean(CROSSFADE_GAPLESS_ALBUMS, true)

    val seekForwardIntervalSeconds: Int
        get() = preferences.getInt(SEEK_FORWARD_INTERVAL, preferences.getInt(SEEK_INTERVAL, 10)).coerceIn(5, 60)

    val seekBackIntervalSeconds: Int
        get() = preferences.getInt(SEEK_BACK_INTERVAL, preferences.getInt(SEEK_INTERVAL, 10)).coerceIn(5, 60)

    /**
     * Minimum current-track duration (ms) required to keep the fast-forward/rewind controls.
     * Zero means the controls are always available.
     */
    val seekControlsMinDurationMs: Long
        get() = (preferences.nullString(SEEK_CONTROLS_MIN_DURATION)?.toIntOrNull() ?: 0).coerceAtLeast(0) * 60_000L

    fun isSeekControlsAvailable(currentDurationMs: Long): Boolean {
        val minDurationMs = seekControlsMinDurationMs
        return minDurationMs <= 0L || currentDurationMs <= 0L || currentDurationMs >= minDurationMs
    }

    fun previousButtonRestartsTrack(): Boolean {
        val configured = preferences.nullString(PREVIOUS_BUTTON_ACTION)
        if (configured != null) {
            return configured != PREVIOUS_BUTTON_ALWAYS_PREVIOUS
        }
        return preferences.getBoolean(REWIND_WITH_BACK, true)
    }

    val shuffleBehavior: ShuffleBehavior
        get() = ShuffleBehavior.fromValue(preferences.nullString(SHUFFLE_BEHAVIOR))

    val shuffleStandardEnabled: Boolean
        get() = preferences.getBoolean(SHUFFLE_STANDARD_ENABLED, true)

    val shuffleRandomizeEnabled: Boolean
        get() = preferences.getBoolean(SHUFFLE_RANDOMIZE_ENABLED, true)

    val isGaplessPlaybackEnabled: Boolean
        get() = preferences.getBoolean(GAPLESS_PLAYBACK, true)

    /**
     * Copies legacy transport preferences into their replacements once, so the settings
     * screens show the value the player is actually using.
     */
    fun migrateLegacyPlaybackPreferences() {
        val legacySeekInterval = preferences.getInt(SEEK_INTERVAL, DEFAULT_SEEK_INTERVAL_SECONDS)
        preferences.edit {
            if (!preferences.contains(SEEK_FORWARD_INTERVAL)) {
                putInt(SEEK_FORWARD_INTERVAL, legacySeekInterval)
            }
            if (!preferences.contains(SEEK_BACK_INTERVAL)) {
                putInt(SEEK_BACK_INTERVAL, legacySeekInterval)
            }
            if (!preferences.contains(PREVIOUS_BUTTON_ACTION) &&
                preferences.contains(REWIND_WITH_BACK)
            ) {
                putString(
                    PREVIOUS_BUTTON_ACTION,
                    if (preferences.getBoolean(REWIND_WITH_BACK, true)) {
                        PREVIOUS_BUTTON_RESTART
                    } else {
                        PREVIOUS_BUTTON_ALWAYS_PREVIOUS
                    },
                )
            }
        }
    }

    var headsetSingleClickAction: HeadsetButtonAction
        get() = preferences.enumValue(HEADSET_SINGLE_CLICK_ACTION, HeadsetButtonAction.PlayPause)
        set(value) = preferences.edit { putString(HEADSET_SINGLE_CLICK_ACTION, value.name) }

    var headsetDoubleClickAction: HeadsetButtonAction
        get() = preferences.enumValue(HEADSET_DOUBLE_CLICK_ACTION, HeadsetButtonAction.Next)
        set(value) = preferences.edit { putString(HEADSET_DOUBLE_CLICK_ACTION, value.name) }

    var headsetTripleClickAction: HeadsetButtonAction
        get() = preferences.enumValue(HEADSET_TRIPLE_CLICK_ACTION, HeadsetButtonAction.Previous)
        set(value) = preferences.edit { putString(HEADSET_TRIPLE_CLICK_ACTION, value.name) }

    var headsetLongPressAction: HeadsetButtonAction
        get() = preferences.enumValue(HEADSET_LONG_PRESS_ACTION, HeadsetButtonAction.PlayPause)
        set(value) = preferences.edit { putString(HEADSET_LONG_PRESS_ACTION, value.name) }

    var headsetFourPlusSeekForward: Boolean
        get() = preferences.getBoolean(HEADSET_FOUR_PLUS_SEEK_FORWARD, false)
        set(value) = preferences.edit { putBoolean(HEADSET_FOUR_PLUS_SEEK_FORWARD, value) }

    var headsetClickDelayMs: Int
        get() = HeadsetButtonPolicy.normalizeDelay(preferences.nullString(HEADSET_CLICK_DELAY_MS))
        set(value) =
            preferences.edit {
                putString(
                    HEADSET_CLICK_DELAY_MS,
                    HeadsetButtonPolicy.normalizeDelay(value.toString()).toString(),
                )
            }

    fun resetHeadsetButtonMapping() {
        preferences.edit {
            putString(HEADSET_SINGLE_CLICK_ACTION, HeadsetButtonAction.PlayPause.name)
            putString(HEADSET_DOUBLE_CLICK_ACTION, HeadsetButtonAction.Next.name)
            putString(HEADSET_TRIPLE_CLICK_ACTION, HeadsetButtonAction.Previous.name)
            putString(HEADSET_LONG_PRESS_ACTION, HeadsetButtonAction.PlayPause.name)
            putBoolean(HEADSET_FOUR_PLUS_SEEK_FORWARD, false)
            putString(HEADSET_CLICK_DELAY_MS, HeadsetButtonPolicy.DEFAULT_DELAY_MS.toString())
        }
    }

    var notificationButtonLayout: NotificationButtonLayout
        get() =
            NotificationButtonLayoutPolicy.fromPreferences(
                expandedValue = preferences.nullString(NOTIFICATION_EXPANDED_BUTTONS),
                compactValue = preferences.nullString(NOTIFICATION_COMPACT_BUTTONS),
            )
        set(value) {
            val normalized = NotificationButtonLayoutPolicy.normalize(value)
            preferences.edit {
                putString(
                    NOTIFICATION_COMPACT_BUTTONS,
                    normalized.compact.joinToString(",") { it.storedValue },
                )
                putString(
                    NOTIFICATION_EXPANDED_BUTTONS,
                    normalized.expanded.joinToString(",") { it.storedValue },
                )
            }
        }

    fun resetNotificationButtonLayout() {
        preferences.edit {
            remove(NOTIFICATION_COMPACT_BUTTONS)
            remove(NOTIFICATION_EXPANDED_BUTTONS)
        }
    }

    /** Replaces the close notification action with an add-to-playlist action. */
    var notificationReplaceCloseWithAddToPlaylist: Boolean
        get() = preferences.getBoolean(NOTIFICATION_REPLACE_CLOSE_WITH_ADD_TO_PLAYLIST, false)
        set(value) = preferences.edit { putBoolean(NOTIFICATION_REPLACE_CLOSE_WITH_ADD_TO_PLAYLIST, value) }

    /** Includes artwork in the session metadata used by the system lock screen. */
    var notificationBroadcastAlbumArt: Boolean
        get() = preferences.getBoolean(NOTIFICATION_BROADCAST_ALBUM_ART, true)
        set(value) = preferences.edit { putBoolean(NOTIFICATION_BROADCAST_ALBUM_ART, value) }

    /** Notification presentation; [NotificationStyle.System] keeps the Media3 layout. */
    var notificationStyle: NotificationStyle
        get() =
            NotificationStyle.fromStoredValue(preferences.nullString(NOTIFICATION_STYLE))
                ?: NotificationStyle.System
        set(value) = preferences.edit { putString(NOTIFICATION_STYLE, value.storedValue) }

    /** Background used by the custom notification layouts. */
    var notificationBackgroundMode: NotificationBackgroundMode
        get() =
            NotificationBackgroundMode.fromStoredValue(preferences.nullString(NOTIFICATION_BACKGROUND_MODE))
                ?: NotificationBackgroundMode.Theme
        set(value) = preferences.edit { putString(NOTIFICATION_BACKGROUND_MODE, value.storedValue) }

    /** User-picked custom notification background, or 0 when it was never set. */
    var notificationCustomBackgroundColor: Int
        get() = preferences.getInt(NOTIFICATION_CUSTOM_BACKGROUND_COLOR, 0)
        set(value) = preferences.edit { putInt(NOTIFICATION_CUSTOM_BACKGROUND_COLOR, value) }

    /** Uses the big custom layout even while the notification is collapsed. */
    var notificationAlwaysExpanded: Boolean
        get() = preferences.getBoolean(NOTIFICATION_ALWAYS_EXPANDED, false)
        set(value) = preferences.edit { putBoolean(NOTIFICATION_ALWAYS_EXPANDED, value) }

    var onlyAlbumArtists: Boolean
        get() = preferences.getBoolean(ONLY_ALBUM_ARTISTS, true)
        set(value) = preferences.edit { putBoolean(ONLY_ALBUM_ARTISTS, value) }

    val trashMusicFiles: Boolean
        get() = preferences.getBoolean(TRASH_MUSIC_FILES, false)

    val recursiveFolderActions: Set<FolderAction>
        get() {
            val notNullSet = mutableSetOf<FolderAction>()
            preferences
                .getStringSet(RECURSIVE_FOLDER_ACTIONS, null)
                ?.mapNotNullTo(notNullSet) { string ->
                    FolderAction.entries.firstOrNull { it.preferenceValue == string }
                }
            return notNullSet
        }

    fun getLastAddedCutoff(context: Context): Cutoff = getCutoff(context, LAST_ADDED_CUTOFF, true)

    fun getHistoryCutoff(context: Context): Cutoff = getCutoff(context, HISTORY_CUTOFF)

    var notPlayedDays: Int
        get() =
            NotPlayedDaysPolicy.normalizeDays(
                preferences.getInt(NOT_PLAYED_DAYS, NotPlayedDaysPolicy.USE_HISTORY_INTERVAL),
            )
        set(value) =
            preferences.edit {
                putInt(NOT_PLAYED_DAYS, NotPlayedDaysPolicy.normalizeDays(value))
            }

    fun getNotPlayedCutoff(context: Context): Cutoff {
        val days = notPlayedDays
        if (days == NotPlayedDaysPolicy.USE_HISTORY_INTERVAL) {
            return getHistoryCutoff(context)
        }
        val description = context.resources.getQuantityString(R.plurals.not_played_days, days, days)
        return Cutoff(description, NotPlayedDaysPolicy.cutoffMillis(System.currentTimeMillis(), days))
    }

    private fun getCutoff(
        context: Context,
        preferenceKey: String,
        asSeconds: Boolean = false,
    ): Cutoff {
        val cutoff = preferences.requireString(preferenceKey, "")
        val description =
            when (cutoff) {
                PlaylistCutoff.TODAY -> context.getString(R.string.today)
                PlaylistCutoff.YESTERDAY -> context.getString(R.string.yesterday)
                PlaylistCutoff.THIS_WEEK -> context.getString(R.string.this_week)
                PlaylistCutoff.PAST_THREE_MONTHS -> context.getString(R.string.past_three_months)
                PlaylistCutoff.THIS_YEAR -> context.getString(R.string.this_year)
                PlaylistCutoff.THIS_MONTH -> context.getString(R.string.this_month)
                else -> context.getString(R.string.this_month)
            }
        val interval =
            calendarSingleton.getCutoffTimeMillis(cutoff).let { cutoffTimeMillis ->
                if (asSeconds) cutoffTimeMillis / 1000 else cutoffTimeMillis
            }
        return Cutoff(description, interval)
    }

    var ignoreSingles: Boolean
        get() = preferences.getBoolean(IGNORE_SINGLES, false)
        set(value) = preferences.edit { putBoolean(IGNORE_SINGLES, value) }

    var showAlbumDuration: Boolean
        get() = preferences.getBoolean(SHOW_TOTAL_DURATION, false)
        set(value) = preferences.edit { putBoolean(SHOW_TOTAL_DURATION, value) }

    var whitelistEnabled: Boolean
        get() = preferences.getBoolean(WHITELIST_ENABLED, true)
        set(value) = preferences.edit { putBoolean(WHITELIST_ENABLED, value) }

    var blacklistEnabled: Boolean
        get() = preferences.getBoolean(BLACKLIST_ENABLED, true)
        set(value) = preferences.edit { putBoolean(BLACKLIST_ENABLED, value) }

    var ghostModeEnabled: Boolean
        get() = preferences.getBoolean(GHOST_MODE_ENABLED, false)
        set(value) = preferences.edit { putBoolean(GHOST_MODE_ENABLED, value) }

    var ghostFolders: List<String>
        get() = preferences.nullString(GHOST_FOLDERS).deserialize(emptyList())
        set(value) = preferences.edit { putString(GHOST_FOLDERS, value.serialize()) }

    var ghostPinHash: String?
        get() = preferences.nullString(GHOST_PIN_HASH)
        set(value) = preferences.edit { putString(GHOST_PIN_HASH, value) }

    var ghostPinSalt: String?
        get() = preferences.nullString(GHOST_PIN_SALT)
        set(value) = preferences.edit { putString(GHOST_PIN_SALT, value) }

    var autoRescanOnMediaMount: Boolean
        get() = preferences.getBoolean(AUTO_RESCAN_ON_MEDIA_MOUNT, false)
        set(value) = preferences.edit { putBoolean(AUTO_RESCAN_ON_MEDIA_MOUNT, value) }

    var setupWizardCompleted: Boolean
        get() = preferences.getBoolean(SETUP_WIZARD_COMPLETED, true)
        set(value) = preferences.edit { putBoolean(SETUP_WIZARD_COMPLETED, value) }

    val minimumSongCountForArtist: Int
        get() = preferences.getInt(ARTIST_MINIMUM_SONGS, 1)

    val minimumSongCountForAlbum: Int
        get() = preferences.getInt(ALBUM_MINIMUM_SONGS, 1)

    val minimumSongDuration: Int
        get() = preferences.getInt(MINIMUM_SONG_DURATION, 15)

    val maximumSongDuration: Int
        get() = preferences.getInt(MAXIMUM_SONG_DURATION, 0)

    var extraFormatsScanEnabled: Boolean
        get() = preferences.getBoolean(EXTRA_FORMATS_SCAN, false)
        set(value) = preferences.edit { putBoolean(EXTRA_FORMATS_SCAN, value) }

    val rotationLockEnabled: Boolean
        get() = preferences.getBoolean(ENABLE_ROTATION_LOCK, false)

    val updateSearchMode: String
        get() = preferences.requireString(UPDATE_SEARCH_MODE, UpdateSearchMode.WEEKLY)

    val experimentalUpdates: Boolean
        get() = preferences.getBoolean(EXPERIMENTAL_UPDATES, false)

    var lastUpdateSearch: Long
        get() = preferences.getLong(LAST_UPDATE_SEARCH, -1)
        set(value) = preferences.edit { putLong(LAST_UPDATE_SEARCH, value) }

    var lastUpdateId: Long
        get() = preferences.getLong(LAST_UPDATE_ID, -1)
        set(value) = preferences.edit { putLong(LAST_UPDATE_ID, value) }

    var hierarchyFolderView: Boolean
        get() = preferences.getBoolean(HIERARCHY_FOLDER_VIEW, false)
        set(value) = preferences.edit { putBoolean(HIERARCHY_FOLDER_VIEW, value) }

    var startDirectory: File
        get() = File(preferences.requireString(START_DIRECTORY, FileUtil.getDefaultStartDirectory().path))
        set(file) = preferences.edit { putString(START_DIRECTORY, file.getCanonicalPathSafe()) }

    var savedArtworkCopyrightNoticeShown: Boolean
        get() = preferences.getBoolean(SAVED_ARTWORK_COPYRIGHT_NOTICE_SHOWN, false)
        set(value) = preferences.edit { putBoolean(SAVED_ARTWORK_COPYRIGHT_NOTICE_SHOWN, value) }

    var initializedBlacklist: Boolean
        get() = preferences.getBoolean(INITIALIZED_BLACKLIST, false)
        set(value) = preferences.edit { putBoolean(INITIALIZED_BLACKLIST, value) }

    var isSwipeAnywhere: Boolean
        get() = preferences.getBoolean(SWIPE_ANYWHERE, false)
        set(value) = preferences.edit { putBoolean(SWIPE_ANYWHERE, value) }

    var isSwipeUpQueue: Boolean
        get() = preferences.getBoolean(SWIPE_UP_QUEUE, false)
        set(value) = preferences.edit { putBoolean(SWIPE_UP_QUEUE, value) }

    var isShowNextSong: Boolean
        get() = preferences.getBoolean(DISPLAY_NEXT_SONG, true)
        set(value) = preferences.edit { putBoolean(DISPLAY_NEXT_SONG, value) }

    var sleepTimerStopMusicOnlyInForeground: Boolean
        get() = preferences.getBoolean(SLEEP_TIMER_STOP_MUSIC_ONLY_IN_FOREGROUND, false)
        set(value) = preferences.edit { putBoolean(SLEEP_TIMER_STOP_MUSIC_ONLY_IN_FOREGROUND, value) }

    fun SharedPreferences.nullString(key: String): String? = getString(key, null)

    fun SharedPreferences.requireString(
        key: String,
        defaultValue: String,
    ): String = requireNotNull(getString(key, defaultValue))

    inline fun <reified T : Enum<T>> SharedPreferences.enumValue(
        key: String,
        defaultValue: T,
    ): T = nullString(key)?.toEnum<T>() ?: defaultValue

    inline fun <reified T : Enum<T>> SharedPreferences.enumValueByOrdinal(
        key: String,
        defaultValue: T,
    ): T = getInt(key, defaultValue.ordinal).toEnum<T>() ?: defaultValue
}

interface GeneralTheme {
    companion object {
        const val LIGHT = "light"
        const val DARK = "dark"
        const val BLACK = "black"
        const val AUTO = "auto"
    }
}

interface BottomTitlesMode {
    companion object {
        const val SELECTED = "selected"
        const val LABELED = "labeled"
        const val UNLABELED = "unlabeled"
    }
}

interface AppBarMode {
    companion object {
        const val COMPACT = "compact"
        const val EXPANDED = "expanded"
    }
}

interface PlayOnStartupMode {
    companion object {
        const val NEVER = "never"
        const val WITH_MINIMIZED_PLAYER = "with_minimized_player"
        const val WITH_EXPANDED_PLAYER = "with_expanded_player"
    }
}

interface SelectedShuffleMode {
    companion object {
        const val SHUFFLE_ARTISTS = "shuffle_artists"
        const val SHUFFLE_ALBUMS = "shuffle_albums"
        const val SHUFFLE_SONGS = "shuffle_songs"
        const val SHUFFLE_ALL = "shuffle_all"
    }
}

interface PlaylistCutoff {
    companion object {
        const val TODAY = "today"
        const val YESTERDAY = "yesterday"
        const val THIS_WEEK = "this_week"
        const val THIS_MONTH = "this_month"
        const val PAST_THREE_MONTHS = "past_three_months"
        const val THIS_YEAR = "this_year"
    }
}

interface ImageSize {
    companion object {
        const val LARGE = "large"
        const val MEDIUM = "medium"
        const val SMALL = "small"
    }
}

interface UpdateSearchMode {
    companion object {
        const val EVERY_DAY = "every_day"
        const val WEEKLY = "weekly"
        const val EVERY_FIFTEEN_DAYS = "every_fifteen_days"
        const val MONTHLY = "monthly"
        const val NEVER = "never"
    }
}

const val BLACK_THEME = "black_theme"
const val MATERIAL_YOU = "material_you"
const val ACCENT_COLOR = "accent_color"
const val USE_CUSTOM_FONT = "use_custom_font"
const val APPBAR_MODE = "appbar_mode"
const val GENERAL_THEME = "general_theme"
const val LIBRARY_CATEGORIES = "library_categories"
const val REMEMBER_LAST_PAGE = "remember_last_page"
const val TAB_TITLES_MODE = "tab_titles_mode"
const val HOLD_TAB_TO_SEARCH = "hold_tab_to_search"
const val IGNORE_ARTICLES_WHEN_SORTING = "ignore_articles_when_sorting"
const val SORTING_IGNORE_KEYWORDS = "sorting_ignore_keywords"
const val LAST_PAGE = "last_page"
const val LARGER_HEADER_IMAGE = "larger_header_image"
const val HORIZONTAL_ARTIST_ALBUMS = "horizontal_artist_albums"
const val COMPACT_ALBUM_SONG_VIEW = "compact_album_song_view"
const val COMPACT_ARTIST_SONG_VIEW = "compact_artist_song_view"
const val NOW_PLAYING_SCREEN = "now_playing_screen"
const val OPEN_ON_PLAY = "open_on_play"
const val ADD_EXTRA_CONTROLS = "add_extra_controls"
const val ADAPTIVE_CONTROLS = "adaptive_controls"
const val SQUIGGLY_SEEK_BAR = "squiggly_seek_bar"
const val SWIPE_DOWN_TO_DISMISS = "swipe_down_to_dismiss"
const val LYRICS_ON_COVER = "lyrics_on_cover"
const val SWIPE_ON_COVER = "swipe_on_cover"
const val MINI_PLAYER_SWIPE_TO_SKIP = "mini_player_swipe_to_skip"
const val NOW_PLAYING_SMALL_IMAGE = "now_playing_small_image"
const val NOW_PLAYING_IMAGE_CORNER_RADIUS = "now_playing_corner_radius"
const val PLAYER_BLUR_RADIUS = "player_blur_radius"
const val CAROUSEL_EFFECT = "carousel_effect"
const val COVER_SINGLE_TAP_ACTION = "cover_single_tap_action"
const val COVER_DOUBLE_TAP_ACTION = "cover_double_tap_action"
const val COVER_LEFT_DOUBLE_TAP_ACTION = "cover_left_double_tap_action"
const val COVER_RIGHT_DOUBLE_TAP_ACTION = "cover_right_double_tap_action"
const val COVER_LONG_PRESS_ACTION = "cover_long_press_action"
const val KEEP_SCREEN_ON_NOW_PLAYING = "keep_screen_on_now_playing"
const val LOCK_SCREEN_PLAYER_ENABLED = "lock_screen_player_enabled"
const val LOCK_SCREEN_WALLPAPER = "lock_screen_wallpaper"
const val LOCK_SCREEN_WALLPAPER_URI = "lock_screen_wallpaper_uri"
const val LOCK_SCREEN_SKIP_WHEN_UNLOCKED = "lock_screen_skip_when_unlocked"
const val LOCK_SCREEN_SKIP_WHEN_PAUSED = "lock_screen_skip_when_paused"
const val LOCK_SCREEN_GESTURES = "lock_screen_gestures"
const val LOCK_SCREEN_SHORTCUTS = "lock_screen_shortcuts"
const val SEEK_BAR_TOUCH_ONLY_ON_THUMB = "seek_bar_touch_only_on_thumb"
const val NOW_PLAYING_QUICK_ACCESS = "now_playing_quick_access"
const val ANIMATE_PLAYER_CONTROL = "animate_player_control"
const val CIRCLE_PLAY_BUTTON = "circle_play_button"
const val ENABLE_SCROLLING_TEXT = "enable_scrolling_text"
const val DISPLAY_ALBUM_TITLE = "display_album_title"
const val SONG_TITLE_MODE = "song_title_mode"
const val SHOW_LIST_DATE_MODIFIED = "show_list_date_modified"
const val DISPLAY_EXTRA_INFO = "display_extra_info"
const val NOW_PLAYING_EXTRA_INFO = "now_playing_extra_info"
const val WIDGET_DYNAMIC_COLORS = "widget_dynamic_colors"
const val WIDGET_SMALL_LAYOUT_STYLE = "widget_small_layout_style"
const val WIDGET_IMAGE_CORNER_RADIUS = "widget_image_corner_radius"
const val WIDGET_THIRD_LINE_CONTENT = "widget_third_line_content"
const val PREFER_REMAINING_TIME = "prefer_remaining_time"
const val PREFER_ALBUM_ARTIST_NAME = "prefer_album_artist_name_on_np"
const val USE_ALBUM_ARTIST_IF_ARTIST_MISSING = "use_album_artist_if_artist_missing"

const val SEARCH_HISTORY = "search_history"
private const val SEARCH_HISTORY_SEPARATOR = "\u001F"
private const val MAX_SEARCH_HISTORY = 20
const val REWIND_WITH_BACK = "rewind_with_back"
const val SEEK_INTERVAL = "seek_interval"
const val SEEK_FORWARD_INTERVAL = "seek_forward_interval"
const val SEEK_BACK_INTERVAL = "seek_back_interval"
const val SEEK_CONTROLS_MIN_DURATION = "seek_controls_min_duration"
const val PREVIOUS_BUTTON_ACTION = "previous_button_action"
const val FADE_IN_DURATION = "playback_fade_in_duration"
const val FADE_OUT_DURATION = "playback_fade_out_duration"
const val CROSSFADE_DURATION = "playback_crossfade_duration"
const val CROSSFADE_ON_MANUAL_SKIP = "playback_crossfade_on_manual_skip"
const val CROSSFADE_GAPLESS_ALBUMS = "playback_crossfade_gapless_albums"
const val GAPLESS_PLAYBACK = "gapless_playback"
const val PREVIOUS_BUTTON_RESTART = "restart"
const val PREVIOUS_BUTTON_ALWAYS_PREVIOUS = "previous"
const val MAX_FADE_DURATION_MS = 4000
const val MAX_CROSSFADE_DURATION_MS = 12_000

const val DEFAULT_SEEK_INTERVAL_SECONDS = 10
const val QUEUE_NEXT_MODE = "queue_next_mode"
const val PLAY_ON_STARTUP_MODE = "play_on_startup_mode"
const val ON_SONG_CLICK_ACTION = "on_song_click_action"
const val ON_CLEAR_QUEUE_ACTION = "on_clear_queue_action"
const val PLAY_OPTION_ALWAYS_VISIBLE = "play_option_always_visible"
const val PLAY_OPTION_PLAYS_WHOLE_LIST = "play_option_whole_list"
const val PLAY_ALL_SONGS_WHEN_SEARCHING = "play_all_songs_when_searching"
const val CLEAR_QUEUE_ON_COMPLETION = "clear_queue_on_completion"
const val REPEAT_LOAD_NEXT_AND_PAUSE_AVAILABLE = "repeat_load_next_and_pause_available"
const val REPEAT_LOAD_NEXT_AND_PAUSE_ACTIVE = "repeat_load_next_and_pause_active"
const val AUTO_DELETE_OLDEST_NAMED_QUEUE = "auto_delete_oldest_named_queue"
const val REMEMBER_SHUFFLE_MODE = "remember_shuffle_mode"
const val SHUFFLE_BEHAVIOR = "shuffle_behavior"
const val SHUFFLE_STANDARD_ENABLED = "shuffle_standard_enabled"
const val SHUFFLE_RANDOMIZE_ENABLED = "shuffle_randomize_enabled"
const val ALBUM_SHUFFLE_MODE = "album_shuffle_mode"
const val ARTIST_SHUFFLE_MODE = "artist_shuffle_mode"
const val RESUME_ON_CONNECT = "resume_on_connect"
const val PAUSE_ON_DISCONNECT = "pause_on_disconnect"
const val RESUME_ONLY_IF_PAUSED_BY_DISCONNECT = "resume_only_if_paused_by_disconnect"
const val PREVENT_UNWANTED_AUTOPLAY = "prevent_unwanted_autoplay"
const val PREVENT_UNWANTED_AUTOPLAY_STRICT = "prevent_unwanted_autoplay_strict"
const val RESUME_ON_BLUETOOTH_CONNECT = "resume_on_bluetooth_connect"
const val PAUSE_ON_BLUETOOTH_DISCONNECT = "pause_on_bluetooth_disconnect"
const val HEADSET_SINGLE_CLICK_ACTION = "headset_single_click_action"
const val HEADSET_DOUBLE_CLICK_ACTION = "headset_double_click_action"
const val HEADSET_TRIPLE_CLICK_ACTION = "headset_triple_click_action"
const val HEADSET_LONG_PRESS_ACTION = "headset_long_press_action"
const val HEADSET_FOUR_PLUS_SEEK_FORWARD = "headset_four_plus_seek_forward"
const val HEADSET_CLICK_DELAY_MS = "headset_click_delay_ms"
const val HEADSET_BUTTON_MAPPING = "headset_button_mapping"
const val NOTIFICATION_BUTTON_LAYOUT = "notification_button_layout"
const val NOTIFICATION_COMPACT_BUTTONS = "notification_compact_buttons"
const val NOTIFICATION_EXPANDED_BUTTONS = "notification_expanded_buttons"
const val NOTIFICATION_REPLACE_CLOSE_WITH_ADD_TO_PLAYLIST = "notification_replace_close_with_add_to_playlist"
const val NOTIFICATION_BROADCAST_ALBUM_ART = "notification_broadcast_album_art"
const val NOTIFICATION_STYLE = "notification_style"
const val NOTIFICATION_BACKGROUND_MODE = "notification_background"
const val NOTIFICATION_CUSTOM_BACKGROUND_COLOR = "notification_background_color"
const val NOTIFICATION_ALWAYS_EXPANDED = "notification_always_expanded"
const val IGNORE_AUDIO_FOCUS = "ignore_audio_focus"
const val PAUSE_ON_ZERO_VOLUME = "pause_on_zero_volume"
const val PREVENT_PLAYBACK_WHEN_MUTED = "prevent_playback_when_muted"
const val MP3_INDEX_SEEKING = "mp3_index_seeking"
const val IGNORE_MEDIA_STORE = "ignore_media_store"
const val USE_FOLDER_ART = "use_folder_art"
const val DISABLE_ALBUM_ART = "disable_album_art"
const val PREFERRED_IMAGE_SIZE = "preferred_image_size"
const val ONLY_ALBUM_ARTISTS = "only_album_artists"
const val TRASH_MUSIC_FILES = "trash_music_files"
const val RECURSIVE_FOLDER_ACTIONS = "recursive_folder_actions"
const val ENABLE_HISTORY = "enable_history_playlist"
const val HISTORY_CUTOFF = "history_interval"
const val LAST_ADDED_CUTOFF = "last_added_interval"
const val NOT_PLAYED_DAYS = "not_played_days"
const val IGNORE_SINGLES = "ignore_singles"
const val SHOW_TOTAL_DURATION = "show_total_duration"
const val WHITELIST_ENABLED = "whitelist_enabled"
const val BLACKLIST_ENABLED = "blacklist_enabled"
const val GHOST_MODE = "ghost_mode"
const val GHOST_MODE_ENABLED = "ghost_mode_enabled"
const val GHOST_FOLDERS = "ghost_folders"
const val GHOST_PIN_HASH = "ghost_pin_hash"
const val GHOST_PIN_SALT = "ghost_pin_salt"
const val AUTO_RESCAN_ON_MEDIA_MOUNT = "auto_rescan_on_media_mount"
const val SETUP_WIZARD_COMPLETED = "setup_wizard_completed"
const val LIBRARY_RESCAN = "library_rescan"
const val SETUP_WIZARD = "setup_wizard"
const val ARTIST_MINIMUM_SONGS = "artist_minimum_songs"
const val ALBUM_MINIMUM_SONGS = "album_minimum_songs"
const val MINIMUM_SONG_DURATION = "minimum_song_duration"
const val MAXIMUM_SONG_DURATION = "maximum_song_duration"
const val EXTRA_FORMATS_SCAN = "extra_formats_scan"
const val ENABLE_ROTATION_LOCK = "enable_rotation_lock"
const val SHOW_UNIMPORTANT_SETTINGS = "show_unimportant_settings"
const val STOP_WHEN_CLOSED_FROM_RECENTS = "stop_when_closed_from_recents"
const val SLEEP_TIMER_STOP_MUSIC_ONLY_IN_FOREGROUND = "sleep_timer_stop_music_only_in_foreground"
const val LANGUAGE_NAME = "language_name"
const val BACKUP_DATA = "backup_data"
const val RESTORE_DATA = "restore_data"
const val REMOVE_MISSING_SONGS = "remove_missing_songs"
const val UPDATE_SEARCH_MODE = "update_search_mode"
const val ONLY_WIFI = "update_only_wifi"
const val LAST_UPDATE_SEARCH = "last_update_search"
const val LAST_UPDATE_ID = "last_update_id"
const val EXPERIMENTAL_UPDATES = "experimental_updates"
const val START_DIRECTORY = "start_directory"
const val SAVED_ARTWORK_COPYRIGHT_NOTICE_SHOWN = "saved_artwork_copyright_notice_shown"
const val INITIALIZED_BLACKLIST = "initialized_blacklist"
const val HIERARCHY_FOLDER_VIEW = "hierarchy_folder_view"
const val SWIPE_ANYWHERE = "swipe_anywhere"
const val SWIPE_UP_QUEUE = "swipe_up_queue"
const val DISPLAY_NEXT_SONG = "display_next_song"
const val LOCKED_QUEUE = "locked_queue"
const val LOCKED_PLAYLISTS = "locked_playlists"
const val QUEUE_HEIGHT = "queue_height"
const val ACTIVE_NAMED_QUEUE_ID = "active_named_queue_id"
const val REMEMBER_TRACK_POSITION = "remember_track_position"
const val AUDIO_FOCUS_MODE = "audio_focus_mode"
const val SESSION_RECOVERY = "session_recovery"
const val LASTFM_LOGIN = "lastfm_login"
const val LISTENBRAINZ_LOGIN = "listenbrainz_login"
