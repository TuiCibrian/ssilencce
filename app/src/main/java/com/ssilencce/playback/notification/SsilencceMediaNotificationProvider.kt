/*
 * Copyright (c) 2026 Christians MartÃ­nez Alvarado
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

import android.content.Context
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import com.google.common.collect.ImmutableList
import com.ssilencce.R
import com.ssilencce.core.model.player.NotificationButton
import com.ssilencce.core.model.player.NotificationButtonLayout
import com.ssilencce.data.model.Song
import com.ssilencce.playback.Playback
import com.ssilencce.playback.RepeatPolicy
import com.ssilencce.util.GhostMode
import com.ssilencce.util.Preferences

/**
 * Media3 notification provider that renders the user-configured button layout.
 *
 * Media3's default provider always inserts previous/play-pause/next and only appends
 * preference buttons after them, so a fully configurable arrangement requires overriding
 * [getMediaButtons]. The compact-view indices are passed through the supported
 * [COMMAND_KEY_COMPACT_VIEW_INDEX] extra.
 */
@UnstableApi
class SsilencceMediaNotificationProvider(
    context: Context,
    notificationIdProvider: DefaultMediaNotificationProvider.NotificationIdProvider,
    channelId: String,
    @StringRes channelNameResourceId: Int,
    private val favoriteStateProvider: () -> Boolean,
    private val seekControlsAvailableProvider: () -> Boolean,
) : DefaultMediaNotificationProvider(
        context,
        notificationIdProvider,
        channelId,
        channelNameResourceId,
    ) {
    private val appContext: Context = context
    private var ghostPlayback = false

    override fun getNotificationContentTitle(metadata: MediaMetadata): CharSequence =
        if (ghostPlayback) {
            appContext.getString(R.string.ghost_hidden_track)
        } else {
            super.getNotificationContentTitle(metadata) ?: ""
        }

    override fun getNotificationContentText(metadata: MediaMetadata): CharSequence =
        if (ghostPlayback) {
            appContext.getString(R.string.ghost_mode_title)
        } else {
            super.getNotificationContentText(metadata) ?: ""
        }

    override fun getMediaButtons(
        session: MediaSession,
        playerCommands: Player.Commands,
        mediaButtonPreferences: ImmutableList<CommandButton>,
        showPauseButton: Boolean,
    ): ImmutableList<CommandButton> {
        val layout = Preferences.notificationButtonLayout
        val replaceClose = Preferences.notificationReplaceCloseWithAddToPlaylist
        val visibleButtons =
            layout.expanded
                .map { button ->
                    if (replaceClose && button == NotificationButton.Close) {
                        NotificationButton.AddToPlaylist
                    } else {
                        button
                    }
                }.distinct()
                .filter { isButtonAvailable(it, playerCommands) }
        if (visibleButtons.isEmpty()) {
            return super.getMediaButtons(session, playerCommands, mediaButtonPreferences, showPauseButton)
        }
        val compactButtons = layout.compact.filter { it in visibleButtons }.take(MAX_COMPACT_BUTTONS)
        return ImmutableList.copyOf(
            visibleButtons.mapIndexed { index, button ->
                val compactIndex = compactButtons.indexOf(button).takeIf { it >= 0 }
                buildButton(
                    session = session,
                    button = button,
                    playerCommands = playerCommands,
                    showPauseButton = showPauseButton,
                    compactIndex = compactIndex,
                )
            },
        )
    }

    override fun addNotificationActions(
        mediaSession: MediaSession,
        mediaButtons: ImmutableList<CommandButton>,
        builder: NotificationCompat.Builder,
        actionFactory: MediaNotification.ActionFactory,
    ): IntArray {
        ghostPlayback = isGhostPlayback(mediaSession.player)
        val compactIndices = IntArray(MAX_COMPACT_BUTTONS) { C.INDEX_UNSET }
        var hasCustomCompactIndices = false
        mediaButtons.forEachIndexed { index, commandButton ->
            if (commandButton.sessionCommand != null) {
                builder.addAction(
                    actionFactory.createCustomActionFromCustomCommandButton(mediaSession, commandButton),
                )
            } else {
                builder.addAction(
                    actionFactory.createMediaAction(
                        mediaSession,
                        IconCompat.createWithResource(appContext, commandButton.iconResId),
                        commandButton.displayName,
                        commandButton.playerCommand,
                    ),
                )
            }
            val compactIndex =
                commandButton.extras.getInt(
                    COMMAND_KEY_COMPACT_VIEW_INDEX,
                    C.INDEX_UNSET,
                )
            if (compactIndex in compactIndices.indices) {
                hasCustomCompactIndices = true
                compactIndices[compactIndex] = index
            }
        }
        if (!hasCustomCompactIndices) {
            return IntArray(0)
        }
        return compactIndices
            .takeWhile { it != C.INDEX_UNSET }
            .toIntArray()
    }

    private fun isGhostPlayback(player: Player): Boolean = (player.currentMediaItem?.localConfiguration?.tag as? Song)?.let(GhostMode::isGhostSong) == true

    private fun isButtonAvailable(
        button: NotificationButton,
        playerCommands: Player.Commands,
    ): Boolean =
        when (button) {
            NotificationButton.Previous -> {
                playerCommands.contains(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            }

            NotificationButton.Next -> {
                playerCommands.contains(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            }

            NotificationButton.PlayPause -> {
                playerCommands.contains(Player.COMMAND_PLAY_PAUSE)
            }

            NotificationButton.SeekForward -> {
                playerCommands.contains(Player.COMMAND_SEEK_FORWARD) && seekControlsAvailableProvider()
            }

            NotificationButton.SeekBackward -> {
                playerCommands.contains(Player.COMMAND_SEEK_BACK) && seekControlsAvailableProvider()
            }

            NotificationButton.Favorite,
            NotificationButton.Close,
            NotificationButton.AddToPlaylist,
            -> {
                playerCommands.contains(Player.COMMAND_GET_METADATA)
            }

            NotificationButton.Shuffle -> {
                playerCommands.contains(Player.COMMAND_SET_SHUFFLE_MODE)
            }

            NotificationButton.Repeat -> {
                playerCommands.contains(Player.COMMAND_SET_REPEAT_MODE)
            }

            NotificationButton.GhostSearch -> {
                Preferences.ghostModeEnabled
            }
        }

    private fun buildButton(
        session: MediaSession,
        button: NotificationButton,
        playerCommands: Player.Commands,
        showPauseButton: Boolean,
        compactIndex: Int?,
    ): CommandButton {
        val player = session.player
        val extras = compactExtras(compactIndex)
        return when (button) {
            NotificationButton.Previous -> {
                mediaButton(
                    icon = CommandButton.ICON_PREVIOUS,
                    playerCommand = Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                    titleRes = R.string.action_previous,
                    extras = extras,
                )
            }

            NotificationButton.Next -> {
                mediaButton(
                    icon = CommandButton.ICON_NEXT,
                    playerCommand = Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    titleRes = R.string.action_next,
                    extras = extras,
                )
            }

            NotificationButton.PlayPause -> {
                if (showPauseButton) {
                    mediaButton(
                        icon = CommandButton.ICON_PAUSE,
                        playerCommand = Player.COMMAND_PLAY_PAUSE,
                        titleRes = R.string.action_play_pause,
                        extras = extras,
                    )
                } else {
                    mediaButton(
                        icon = CommandButton.ICON_PLAY,
                        playerCommand = Player.COMMAND_PLAY_PAUSE,
                        titleRes = R.string.action_play_pause,
                        extras = extras,
                    )
                }
            }

            NotificationButton.SeekForward -> {
                mediaButton(
                    icon = CommandButton.ICON_FAST_FORWARD,
                    playerCommand = Player.COMMAND_SEEK_FORWARD,
                    titleRes = R.string.action_seek_forward,
                    extras = extras,
                )
            }

            NotificationButton.SeekBackward -> {
                mediaButton(
                    icon = CommandButton.ICON_REWIND,
                    playerCommand = Player.COMMAND_SEEK_BACK,
                    titleRes = R.string.action_seek_backward,
                    extras = extras,
                )
            }

            NotificationButton.Favorite -> {
                sessionButton(
                    icon = if (favoriteStateProvider()) CommandButton.ICON_HEART_FILLED else CommandButton.ICON_HEART_UNFILLED,
                    action = Playback.TOGGLE_FAVORITE,
                    titleRes =
                        if (favoriteStateProvider()) {
                            R.string.notification_favorite_remove
                        } else {
                            R.string.notification_favorite_add
                        },
                    extras = extras,
                )
            }

            NotificationButton.Close -> {
                sessionButton(
                    icon = CommandButton.ICON_UNDEFINED,
                    customIconRes = R.drawable.ic_close_24dp,
                    action = Playback.CLOSE_NOTIFICATION,
                    titleRes = R.string.close_action,
                    extras = extras,
                )
            }

            NotificationButton.AddToPlaylist -> {
                sessionButton(
                    icon = CommandButton.ICON_PLAYLIST_ADD,
                    action = Playback.ADD_TO_PLAYLIST,
                    titleRes = R.string.action_add_to_playlist,
                    extras = extras,
                )
            }

            NotificationButton.Shuffle -> {
                sessionButton(
                    icon = if (player.shuffleModeEnabled) CommandButton.ICON_SHUFFLE_ON else CommandButton.ICON_SHUFFLE_OFF,
                    action = Playback.TOGGLE_SHUFFLE,
                    titleRes = R.string.shuffle_mode,
                    extras = extras,
                )
            }

            NotificationButton.Repeat -> {
                repeatButton(player, extras)
            }

            NotificationButton.GhostSearch -> {
                sessionButton(
                    icon = CommandButton.ICON_UNDEFINED,
                    customIconRes = R.drawable.ic_visibility_off_24dp,
                    action = Playback.OPEN_GHOST_SEARCH,
                    titleRes = R.string.notification_button_ghost_search,
                    extras = extras,
                )
            }
        }
    }

    private fun repeatButton(
        player: Player,
        extras: Bundle,
    ): CommandButton {
        val mode = RepeatPolicy.current(player.repeatMode, Preferences.repeatLoadNextAndPauseActive)
        return when (mode) {
            RepeatPolicy.Mode.Off -> {
                sessionButton(
                    icon = CommandButton.ICON_REPEAT_OFF,
                    action = Playback.CYCLE_REPEAT,
                    titleRes = R.string.repeat_mode_off,
                    extras = extras,
                )
            }

            RepeatPolicy.Mode.All -> {
                sessionButton(
                    icon = CommandButton.ICON_REPEAT_ALL,
                    action = Playback.CYCLE_REPEAT,
                    titleRes = R.string.repeat_mode_all,
                    extras = extras,
                )
            }

            RepeatPolicy.Mode.One -> {
                sessionButton(
                    icon = CommandButton.ICON_REPEAT_ONE,
                    action = Playback.CYCLE_REPEAT,
                    titleRes = R.string.repeat_mode_one,
                    extras = extras,
                )
            }

            RepeatPolicy.Mode.LoadNextAndPause -> {
                sessionButton(
                    icon = CommandButton.ICON_UNDEFINED,
                    customIconRes = R.drawable.ic_repeat_pause_24dp,
                    action = Playback.CYCLE_REPEAT,
                    titleRes = R.string.repeat_mode_load_next_pause,
                    extras = extras,
                )
            }
        }
    }

    private fun mediaButton(
        icon: Int,
        playerCommand: Int,
        @StringRes titleRes: Int,
        extras: Bundle,
    ): CommandButton =
        CommandButton
            .Builder(icon)
            .setPlayerCommand(playerCommand)
            .setDisplayName(appContext.getString(titleRes))
            .setExtras(extras)
            .build()

    private fun sessionButton(
        icon: Int,
        action: String,
        @StringRes titleRes: Int,
        extras: Bundle,
        customIconRes: Int? = null,
    ): CommandButton =
        CommandButton
            .Builder(icon)
            .setSessionCommand(SessionCommand(action, Bundle.EMPTY))
            .setDisplayName(appContext.getString(titleRes))
            .apply { customIconRes?.let { setCustomIconResId(it) } }
            .setExtras(extras)
            .build()

    private fun compactExtras(compactIndex: Int?): Bundle =
        Bundle().apply {
            if (compactIndex != null) {
                putInt(COMMAND_KEY_COMPACT_VIEW_INDEX, compactIndex)
            }
        }

    private companion object {
        const val MAX_COMPACT_BUTTONS = 3
    }
}
