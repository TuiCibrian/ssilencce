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

package com.ssilencce.playback.notification

import android.app.Notification
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.LruCache
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.IconCompat
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.ssilencce.R
import com.ssilencce.core.model.PaletteColor
import com.ssilencce.core.model.player.NotificationBackgroundMode
import com.ssilencce.core.model.player.NotificationButton
import com.ssilencce.core.model.player.NotificationStyle
import com.ssilencce.core.model.player.NotificationStylePolicy
import com.ssilencce.core.palette.PaletteProcessor
import com.ssilencce.data.model.Song
import com.ssilencce.extensions.resolveColor
import com.ssilencce.playback.Playback
import com.ssilencce.playback.RepeatPolicy
import com.ssilencce.util.GhostMode
import com.ssilencce.util.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Arrays
import java.util.Locale

/**
 * Media3 notification provider that renders the notification styles with RemoteViews.
 *
 * The system style remains available through [SsilencceMediaNotificationProvider]; this provider
 * is only installed when the user selects a custom style. Artwork and palette extraction are
 * asynchronous and cached, so the periodic seek-bar refresh only rebuilds the RemoteViews.
 *
 * Android 13 and later may ignore custom media notification layouts and show the system media
 * controls instead. The setting summary documents that limitation.
 */
@UnstableApi
class CustomLayoutMediaNotificationProvider(
    context: Context,
    private val notificationIdProvider: (MediaSession) -> Int,
    private val channelId: String,
    @StringRes private val channelNameResourceId: Int,
    private val favoriteStateProvider: () -> Boolean,
    private val seekControlsAvailableProvider: () -> Boolean,
) : MediaNotification.Provider {
    private val appContext: Context = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val artworkCache = LruCache<String, Bitmap>(MAX_CACHED_ARTWORK)
    private val failedArtworkKeys = mutableSetOf<String>()

    private var cachedPaletteKey: String? = null
    private var cachedPalette: PaletteColor? = null
    private var artworkLoadingKey: String? = null
    private var paletteLoadingKey: String? = null

    private var lastSession: MediaSession? = null
    private var lastActionFactory: MediaNotification.ActionFactory? = null
    private var lastCallback: MediaNotification.Provider.Callback? = null

    override fun createNotification(
        session: MediaSession,
        mediaButtonPreferences: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback,
    ): MediaNotification {
        lastSession = session
        lastActionFactory = actionFactory
        lastCallback = onNotificationChangedCallback

        val player = session.player
        val metadata =
            if (player.isCommandAvailable(Player.COMMAND_GET_METADATA)) {
                player.mediaMetadata
            } else {
                null
            }
        val ghostPlayback = isGhostPlayback(player)
        val artworkKey = if (ghostPlayback) null else artworkKey(metadata)
        val artwork = artworkKey?.let(artworkCache::get)
        if (!ghostPlayback && artwork == null) {
            requestArtwork(session, metadata, artworkKey)
        }
        if (!ghostPlayback) {
            computePaletteIfNeeded(artwork, artworkKey)
        }

        val style =
            Preferences.notificationStyle.let { selected ->
                if (selected.isCustom) selected else NotificationStyle.BigSimple
            }
        val colors = resolveColors(artworkKey)
        val showPauseButton = !Util.shouldShowPlayButton(player, session.showPlayButtonIfPlaybackIsSuppressed)
        val isAvailable = { button: NotificationButton -> isButtonAvailable(button, player) }
        val layout = Preferences.notificationButtonLayout
        val compactActionSpecs =
            buildActionSpecs(
                buttons = applyCloseReplacement(NotificationStylePolicy.compactButtonsFor(layout, isAvailable)),
                session = session,
                actionFactory = actionFactory,
                showPauseButton = showPauseButton,
            )
        val expandedActionSpecs =
            if (style == NotificationStyle.Small) {
                compactActionSpecs
            } else {
                buildActionSpecs(
                    buttons =
                        applyCloseReplacement(
                            NotificationStylePolicy.buttonsFor(
                                style = style,
                                layout = layout,
                                isAvailable = isAvailable,
                            ),
                        ),
                    session = session,
                    actionFactory = actionFactory,
                    showPauseButton = showPauseButton,
                )
            }

        val durationMs = player.duration.coerceAtLeast(0L)
        val positionMs = player.currentPosition.coerceAtLeast(0L)
        val content =
            NotificationContent(
                style = style,
                title = if (ghostPlayback) appContext.getString(R.string.ghost_hidden_track) else metadata?.title?.toString().orEmpty(),
                artist = if (ghostPlayback) appContext.getString(R.string.ghost_mode_title) else metadata?.artist?.toString().orEmpty(),
                artwork = if (ghostPlayback) null else artwork,
                colors = colors,
                compactActionSpecs = compactActionSpecs,
                expandedActionSpecs = expandedActionSpecs,
                durationMs = durationMs,
                positionMs = positionMs,
                upcoming = if (!ghostPlayback && style.showsUpcomingSongs) upcomingTracks(player) else emptyList(),
                alwaysExpanded = Preferences.notificationAlwaysExpanded,
            )
        val notification = buildNotification(session, actionFactory, content)
        return MediaNotification(notificationIdProvider(session), notification)
    }

    override fun handleCustomCommand(
        session: MediaSession,
        action: String,
        extras: Bundle,
    ): Boolean = false

    override fun getNotificationChannelInfo(): MediaNotification.Provider.NotificationChannelInfo =
        MediaNotification.Provider.NotificationChannelInfo(
            channelId,
            appContext.getString(channelNameResourceId),
        )

    /** Releases the provider scope and its artwork caches. */
    fun release() {
        scope.cancel()
        artworkCache.evictAll()
        failedArtworkKeys.clear()
        cachedPaletteKey = null
        cachedPalette = null
        artworkLoadingKey = null
        paletteLoadingKey = null
        lastSession = null
        lastActionFactory = null
        lastCallback = null
    }

    private fun buildNotification(
        session: MediaSession,
        actionFactory: MediaNotification.ActionFactory,
        content: NotificationContent,
    ): Notification {
        val smallView = buildSmallView(content)
        val bigView = buildBigView(content)
        val builder =
            NotificationCompat
                .Builder(appContext, channelId)
                .setContentTitle(content.title)
                .setContentText(content.artist)
                .setDeleteIntent(actionFactory.createNotificationDismissalIntent(session))
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_stat_music_playback)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(false)
                .setShowWhen(false)
                .setGroup(DefaultMediaNotificationProvider.GROUP_KEY)
        session.sessionActivity?.let { builder.setContentIntent(it) }
        content.artwork?.let { builder.setLargeIcon(it) }
        builder.setStyle(MediaStyleNotificationHelper.DecoratedMediaCustomViewStyle(session))

        if (content.style == NotificationStyle.Small) {
            builder.setCustomContentView(smallView)
        } else {
            builder.setCustomBigContentView(bigView)
            builder.setCustomContentView(if (content.alwaysExpanded) bigView else smallView)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }
        return builder.build()
    }

    private fun buildSmallView(content: NotificationContent): RemoteViews {
        val views = RemoteViews(appContext.packageName, R.layout.notification_custom_small)
        views.setInt(R.id.notification_root, "setBackgroundColor", content.colors.background)
        bindArtwork(views, content)
        views.setTextViewText(R.id.notification_title, content.title)
        views.setTextColor(R.id.notification_title, content.colors.title)
        views.setTextViewText(R.id.notification_artist, content.artist)
        views.setTextColor(R.id.notification_artist, content.colors.subtitle)
        bindActions(
            views = views,
            ids = SMALL_ACTION_IDS,
            specs = content.compactActionSpecs,
            iconColor = content.colors.icon,
        )
        return views
    }

    private fun buildBigView(content: NotificationContent): RemoteViews {
        val views = RemoteViews(appContext.packageName, R.layout.notification_custom_big)
        views.setInt(R.id.notification_root, "setBackgroundColor", content.colors.background)
        bindArtwork(views, content)
        views.setTextViewText(R.id.notification_title, content.title)
        views.setTextColor(R.id.notification_title, content.colors.title)
        views.setTextViewText(R.id.notification_artist, content.artist)
        views.setTextColor(R.id.notification_artist, content.colors.subtitle)

        val showProgress =
            content.style.showsSeekBar &&
                content.durationMs > 0L &&
                seekControlsAvailableProvider()
        if (showProgress) {
            views.setViewVisibility(R.id.notification_progress_container, View.VISIBLE)
            val max = content.durationMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            val progress = content.positionMs.coerceIn(0L, content.durationMs).toInt()
            views.setProgressBar(R.id.notification_progress, max, progress, false)
            views.setTextViewText(R.id.notification_elapsed, formatTime(content.positionMs))
            views.setTextViewText(R.id.notification_duration, formatTime(content.durationMs))
            views.setTextColor(R.id.notification_elapsed, content.colors.subtitle)
            views.setTextColor(R.id.notification_duration, content.colors.subtitle)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                views.setColorStateList(
                    R.id.notification_progress,
                    "setProgressTintList",
                    ColorStateList.valueOf(content.colors.icon),
                )
            }
        } else {
            views.setViewVisibility(R.id.notification_progress_container, View.GONE)
        }

        if (content.upcoming.isEmpty()) {
            views.setViewVisibility(R.id.notification_up_next_container, View.GONE)
        } else {
            views.setViewVisibility(R.id.notification_up_next_container, View.VISIBLE)
            views.setTextViewText(R.id.notification_up_next, appContext.getString(R.string.notification_up_next))
            views.setTextColor(R.id.notification_up_next, content.colors.subtitle)
            UPCOMING_IDS.forEachIndexed { index, id ->
                val track = content.upcoming.getOrNull(index)
                if (track == null) {
                    views.setViewVisibility(id, View.GONE)
                } else {
                    views.setViewVisibility(id, View.VISIBLE)
                    views.setTextViewText(id, upcomingText(track))
                    views.setTextColor(id, content.colors.title)
                }
            }
        }

        bindActions(
            views = views,
            ids = BIG_ACTION_IDS,
            specs = content.expandedActionSpecs,
            iconColor = content.colors.icon,
        )
        return views
    }

    private fun bindArtwork(
        views: RemoteViews,
        content: NotificationContent,
    ) {
        val artwork = content.artwork
        if (artwork == null) {
            views.setViewVisibility(R.id.notification_artwork, View.GONE)
        } else {
            views.setViewVisibility(R.id.notification_artwork, View.VISIBLE)
            views.setImageViewBitmap(R.id.notification_artwork, artwork)
        }
    }

    private fun bindActions(
        views: RemoteViews,
        ids: IntArray,
        specs: List<ActionSpec>,
        iconColor: Int,
    ) {
        ids.forEachIndexed { index, id ->
            val spec = specs.getOrNull(index)
            if (spec == null) {
                views.setViewVisibility(id, View.GONE)
            } else {
                views.setViewVisibility(id, View.VISIBLE)
                views.setImageViewResource(id, spec.iconRes)
                views.setInt(id, "setColorFilter", iconColor)
                spec.action.actionIntent?.let { intent ->
                    views.setOnClickPendingIntent(id, intent)
                }
                views.setContentDescription(id, spec.action.title)
            }
        }
    }

    private fun isGhostPlayback(player: Player): Boolean = (player.currentMediaItem?.localConfiguration?.tag as? Song)?.let(GhostMode::isGhostSong) == true

    private fun applyCloseReplacement(buttons: List<NotificationButton>): List<NotificationButton> =
        if (!Preferences.notificationReplaceCloseWithAddToPlaylist) {
            buttons
        } else {
            buttons
                .map { button ->
                    if (button == NotificationButton.Close) NotificationButton.AddToPlaylist else button
                }.distinct()
        }

    private fun buildActionSpecs(
        buttons: List<NotificationButton>,
        session: MediaSession,
        actionFactory: MediaNotification.ActionFactory,
        showPauseButton: Boolean,
    ): List<ActionSpec> =
        buttons.mapNotNull { button ->
            val action = buildAction(button, session, actionFactory, showPauseButton) ?: return@mapNotNull null
            if (action.actionIntent == null) return@mapNotNull null
            ActionSpec(iconResFor(button, session.player, showPauseButton), action)
        }

    private fun buildAction(
        button: NotificationButton,
        session: MediaSession,
        actionFactory: MediaNotification.ActionFactory,
        showPauseButton: Boolean,
    ): NotificationCompat.Action? {
        val player = session.player
        val icon = IconCompat.createWithResource(appContext, iconResFor(button, player, showPauseButton))
        val title = appContext.getString(titleResFor(button, player))
        return when (button) {
            NotificationButton.Previous -> {
                actionFactory.createMediaAction(
                    session,
                    icon,
                    title,
                    if (player.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)) {
                        Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
                    } else {
                        Player.COMMAND_SEEK_TO_PREVIOUS
                    },
                )
            }

            NotificationButton.Next -> {
                actionFactory.createMediaAction(
                    session,
                    icon,
                    title,
                    if (player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)) {
                        Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
                    } else {
                        Player.COMMAND_SEEK_TO_NEXT
                    },
                )
            }

            NotificationButton.PlayPause -> {
                actionFactory.createMediaAction(session, icon, title, Player.COMMAND_PLAY_PAUSE)
            }

            NotificationButton.SeekForward -> {
                actionFactory.createMediaAction(session, icon, title, Player.COMMAND_SEEK_FORWARD)
            }

            NotificationButton.SeekBackward -> {
                actionFactory.createMediaAction(session, icon, title, Player.COMMAND_SEEK_BACK)
            }

            NotificationButton.Favorite -> {
                actionFactory.createCustomAction(session, icon, title, Playback.TOGGLE_FAVORITE, Bundle.EMPTY)
            }

            NotificationButton.Close -> {
                actionFactory.createCustomAction(session, icon, title, Playback.CLOSE_NOTIFICATION, Bundle.EMPTY)
            }

            NotificationButton.AddToPlaylist -> {
                actionFactory.createCustomAction(session, icon, title, Playback.ADD_TO_PLAYLIST, Bundle.EMPTY)
            }

            NotificationButton.Shuffle -> {
                actionFactory.createCustomAction(session, icon, title, Playback.TOGGLE_SHUFFLE, Bundle.EMPTY)
            }

            NotificationButton.Repeat -> {
                actionFactory.createCustomAction(session, icon, title, Playback.CYCLE_REPEAT, Bundle.EMPTY)
            }

            NotificationButton.GhostSearch -> {
                actionFactory.createCustomAction(session, icon, title, Playback.OPEN_GHOST_SEARCH, Bundle.EMPTY)
            }
        }
    }

    private fun isButtonAvailable(
        button: NotificationButton,
        player: Player,
    ): Boolean =
        when (button) {
            NotificationButton.Previous -> {
                player.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM) ||
                    player.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS)
            }

            NotificationButton.Next -> {
                player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM) ||
                    player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT)
            }

            NotificationButton.PlayPause -> {
                player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)
            }

            NotificationButton.SeekForward -> {
                player.isCommandAvailable(Player.COMMAND_SEEK_FORWARD) && seekControlsAvailableProvider()
            }

            NotificationButton.SeekBackward -> {
                player.isCommandAvailable(Player.COMMAND_SEEK_BACK) && seekControlsAvailableProvider()
            }

            NotificationButton.Favorite,
            NotificationButton.Close,
            NotificationButton.AddToPlaylist,
            -> {
                player.isCommandAvailable(Player.COMMAND_GET_METADATA)
            }

            NotificationButton.Shuffle -> {
                player.isCommandAvailable(Player.COMMAND_SET_SHUFFLE_MODE)
            }

            NotificationButton.Repeat -> {
                player.isCommandAvailable(Player.COMMAND_SET_REPEAT_MODE)
            }

            NotificationButton.GhostSearch -> {
                Preferences.ghostModeEnabled
            }
        }

    private fun iconResFor(
        button: NotificationButton,
        player: Player,
        showPauseButton: Boolean,
    ): Int =
        when (button) {
            NotificationButton.Previous -> {
                R.drawable.ic_previous_24dp
            }

            NotificationButton.Next -> {
                R.drawable.ic_next_24dp
            }

            NotificationButton.PlayPause -> {
                if (showPauseButton) R.drawable.ic_pause_24dp else R.drawable.ic_play_24dp
            }

            NotificationButton.SeekForward -> {
                R.drawable.ic_fast_forward_24dp
            }

            NotificationButton.SeekBackward -> {
                R.drawable.ic_fast_rewind_24dp
            }

            NotificationButton.Favorite -> {
                if (favoriteStateProvider()) R.drawable.ic_favorite_24dp else R.drawable.ic_favorite_outline_24dp
            }

            NotificationButton.Close -> {
                R.drawable.ic_close_24dp
            }

            NotificationButton.Shuffle -> {
                if (player.shuffleModeEnabled) R.drawable.ic_shuffle_on_24dp else R.drawable.ic_shuffle_24dp
            }

            NotificationButton.Repeat -> {
                when (RepeatPolicy.current(player.repeatMode, Preferences.repeatLoadNextAndPauseActive)) {
                    RepeatPolicy.Mode.Off -> R.drawable.ic_repeat_24dp
                    RepeatPolicy.Mode.All -> R.drawable.ic_repeat_on_24dp
                    RepeatPolicy.Mode.One -> R.drawable.ic_repeat_one_on_24dp
                    RepeatPolicy.Mode.LoadNextAndPause -> R.drawable.ic_repeat_pause_24dp
                }
            }

            NotificationButton.AddToPlaylist -> {
                R.drawable.ic_playlist_add_24dp
            }

            NotificationButton.GhostSearch -> {
                R.drawable.ic_visibility_off_24dp
            }
        }

    private fun titleResFor(
        button: NotificationButton,
        player: Player,
    ): Int =
        when (button) {
            NotificationButton.Favorite -> {
                if (favoriteStateProvider()) R.string.notification_favorite_remove else R.string.notification_favorite_add
            }

            NotificationButton.Repeat -> {
                when (RepeatPolicy.current(player.repeatMode, Preferences.repeatLoadNextAndPauseActive)) {
                    RepeatPolicy.Mode.Off -> R.string.repeat_mode_off
                    RepeatPolicy.Mode.All -> R.string.repeat_mode_all
                    RepeatPolicy.Mode.One -> R.string.repeat_mode_one
                    RepeatPolicy.Mode.LoadNextAndPause -> R.string.repeat_mode_load_next_pause
                }
            }

            else -> {
                button.labelRes
            }
        }

    private fun resolveColors(artworkKey: String?): NotificationColors {
        val mode = Preferences.notificationBackgroundMode
        val palette = cachedPalette
        if (mode == NotificationBackgroundMode.Cover &&
            artworkKey != null &&
            cachedPaletteKey == artworkKey &&
            palette != null
        ) {
            return NotificationColors(
                background = palette.backgroundColor,
                title = palette.primaryTextColor,
                subtitle = palette.secondaryTextColor,
                icon = palette.primaryTextColor,
            )
        }
        val customColor = Preferences.notificationCustomBackgroundColor
        if (mode == NotificationBackgroundMode.Custom && customColor != 0) {
            return NotificationColors.fromBackground(customColor)
        }
        val background =
            appContext.resolveColor(
                com.google.android.material.R.attr.colorSurfaceContainerHigh,
                Color.DKGRAY,
            )
        if (mode == NotificationBackgroundMode.Theme) {
            return NotificationColors(
                background = background,
                title = appContext.resolveColor(com.google.android.material.R.attr.colorOnSurface, Color.WHITE),
                subtitle =
                    appContext.resolveColor(
                        com.google.android.material.R.attr.colorOnSurfaceVariant,
                        Color.LTGRAY,
                    ),
                icon = appContext.resolveColor(com.google.android.material.R.attr.colorOnSurface, Color.WHITE),
            )
        }
        return NotificationColors.fromBackground(background)
    }

    private fun requestArtwork(
        session: MediaSession,
        metadata: MediaMetadata?,
        key: String?,
    ) {
        if (metadata == null || key == null) return
        if (key == artworkLoadingKey || key in failedArtworkKeys) return
        val future = session.bitmapLoader.loadBitmapFromMetadata(metadata) ?: return
        artworkLoadingKey = key
        Futures.addCallback(
            future,
            object : FutureCallback<Bitmap> {
                override fun onSuccess(result: Bitmap?) {
                    artworkLoadingKey = null
                    if (result == null) {
                        failedArtworkKeys += key
                        return
                    }
                    val scaled = scaleArtwork(result)
                    artworkCache.put(key, scaled)
                    if (Preferences.notificationBackgroundMode == NotificationBackgroundMode.Cover) {
                        computePalette(key, scaled)
                    }
                    rebuild()
                }

                override fun onFailure(throwable: Throwable) {
                    artworkLoadingKey = null
                    failedArtworkKeys += key
                }
            },
            ContextCompat.getMainExecutor(appContext),
        )
    }

    private fun computePaletteIfNeeded(
        artwork: Bitmap?,
        key: String?,
    ) {
        if (artwork == null || key == null) return
        if (Preferences.notificationBackgroundMode != NotificationBackgroundMode.Cover) return
        if (cachedPaletteKey == key || paletteLoadingKey == key) return
        computePalette(key, artwork)
    }

    private fun computePalette(
        key: String,
        artwork: Bitmap,
    ) {
        if (paletteLoadingKey == key) return
        paletteLoadingKey = key
        scope.launch {
            val palette = PaletteProcessor.getPaletteColor(appContext, artwork)
            paletteLoadingKey = null
            if (artworkCache.get(key) == null) return@launch
            cachedPaletteKey = key
            cachedPalette = palette
            rebuild()
        }
    }

    private fun rebuild() {
        val session = lastSession ?: return
        val actionFactory = lastActionFactory ?: return
        val callback = lastCallback ?: return
        callback.onNotificationChanged(
            createNotification(session, ImmutableList.of(), actionFactory, callback),
        )
    }

    private fun scaleArtwork(source: Bitmap): Bitmap {
        val maxSize =
            (MAX_ARTWORK_DP * appContext.resources.displayMetrics.density)
                .toInt()
                .coerceAtLeast(MIN_ARTWORK_PX)
        if (source.width <= maxSize && source.height <= maxSize) return source
        val scale = maxSize.toFloat() / maxOf(source.width, source.height)
        return Bitmap.createScaledBitmap(
            source,
            (source.width * scale).toInt().coerceAtLeast(1),
            (source.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }

    private fun artworkKey(metadata: MediaMetadata?): String? {
        if (metadata == null) return null
        metadata.artworkUri?.let { return it.toString() }
        metadata.artworkData?.let { return "data:${it.size}:${Arrays.hashCode(it)}" }
        return null
    }

    private fun upcomingTracks(player: Player): List<UpcomingTrack> {
        val count = player.mediaItemCount
        if (count <= 1) return emptyList()
        val currentIndex = player.currentMediaItemIndex
        if (currentIndex !in 0 until count) return emptyList()
        val wrap = player.repeatMode == Player.REPEAT_MODE_ALL
        val tracks = mutableListOf<UpcomingTrack>()
        for (offset in 1..MAX_UPCOMING_SONGS) {
            var index = currentIndex + offset
            if (index >= count) {
                if (!wrap) break
                index %= count
            }
            if (index == currentIndex) break
            val metadata = player.getMediaItemAt(index).mediaMetadata
            val title = metadata.title?.toString() ?: metadata.displayTitle?.toString().orEmpty()
            if (title.isBlank()) continue
            tracks += UpcomingTrack(title, metadata.artist?.toString())
        }
        return tracks
    }

    private fun upcomingText(track: UpcomingTrack): String =
        if (track.artist.isNullOrBlank()) {
            track.title
        } else {
            appContext.getString(R.string.notification_upcoming_item, track.title, track.artist)
        }

    private fun formatTime(positionMs: Long): String {
        val totalSeconds = positionMs.coerceAtLeast(0L) / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0) {
            String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
        }
    }

    private data class ActionSpec(
        val iconRes: Int,
        val action: NotificationCompat.Action,
    )

    private data class UpcomingTrack(
        val title: String,
        val artist: String?,
    )

    private data class NotificationContent(
        val style: NotificationStyle,
        val title: String,
        val artist: String,
        val artwork: Bitmap?,
        val colors: NotificationColors,
        val compactActionSpecs: List<ActionSpec>,
        val expandedActionSpecs: List<ActionSpec>,
        val durationMs: Long,
        val positionMs: Long,
        val upcoming: List<UpcomingTrack>,
        val alwaysExpanded: Boolean,
    )

    private data class NotificationColors(
        val background: Int,
        val title: Int,
        val subtitle: Int,
        val icon: Int,
    ) {
        companion object {
            fun fromBackground(background: Int): NotificationColors {
                val titleColor =
                    if (ColorUtils.calculateLuminance(background) > 0.5) {
                        Color.BLACK
                    } else {
                        Color.WHITE
                    }
                return NotificationColors(
                    background = background,
                    title = titleColor,
                    subtitle = ColorUtils.setAlphaComponent(titleColor, SUBTITLE_ALPHA),
                    icon = titleColor,
                )
            }
        }
    }

    private companion object {
        const val MAX_UPCOMING_SONGS = 3
        const val MAX_CACHED_ARTWORK = 3
        const val MAX_ARTWORK_DP = 160
        const val MIN_ARTWORK_PX = 128
        const val SUBTITLE_ALPHA = 184

        val SMALL_ACTION_IDS =
            intArrayOf(
                R.id.notification_action_1,
                R.id.notification_action_2,
                R.id.notification_action_3,
            )

        val BIG_ACTION_IDS =
            intArrayOf(
                R.id.notification_action_1,
                R.id.notification_action_2,
                R.id.notification_action_3,
                R.id.notification_action_4,
                R.id.notification_action_5,
            )

        val UPCOMING_IDS =
            intArrayOf(
                R.id.notification_upcoming_1,
                R.id.notification_upcoming_2,
                R.id.notification_upcoming_3,
            )
    }
}
