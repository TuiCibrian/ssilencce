/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.ssilencce.util.backup

import com.ssilencce.data.local.repository.QueueNamePolicy
import com.ssilencce.util.RestoreEntryKind

/**
 * Pure backup archive layout, merge and named-queue planning rules. Kept free of Android imports
 * so restore behavior can be probed on the JVM.
 */
object BackupArchivePolicy {
    const val PLAYLISTS_PATH = "Playlists"
    const val FAVORITES_PATH = "Favorites"
    const val FAVORITES_FILE = "favorites.m3u"
    const val SETTINGS_PATH = "prefs"
    const val LYRICS_PATH = "lyrics"
    const val CUSTOM_ARTISTS_PATH = "artistImages"
    const val CUSTOM_ARTIST_PREFS_FILE = "custom_artist_images.xml"
    const val PLAY_COUNTS_PATH = "statistics"
    const val PLAY_COUNTS_FILE = "play_counts.json"
    const val NAMED_QUEUES_PATH = "queues"
    const val NAMED_QUEUES_FILE = "named_queues.json"
    const val TRACK_POSITIONS_PATH = "positions"
    const val TRACK_POSITIONS_FILE = "track_positions.json"

    private const val PLAY_COUNTS_ENTRY = "$PLAY_COUNTS_PATH/$PLAY_COUNTS_FILE"
    private const val NAMED_QUEUES_ENTRY = "$NAMED_QUEUES_PATH/$NAMED_QUEUES_FILE"
    private const val TRACK_POSITIONS_ENTRY = "$TRACK_POSITIONS_PATH/$TRACK_POSITIONS_FILE"
    private const val LEGACY_FAVORITES_ENTRY = "$PLAYLISTS_PATH/Favorites.m3u"

    data class PlayCountCounters(
        val timePlayed: Long,
        val playCount: Int,
        val skipCount: Int,
    )

    data class NamedQueueRestorePlan(
        val id: String,
        val name: String,
    )

    fun entryKind(
        name: String,
        isDirectory: Boolean,
    ): RestoreEntryKind {
        if (isDirectory) return RestoreEntryKind.Unrecognized
        return when {
            name.hasArchivePrefix(FAVORITES_PATH) -> {
                RestoreEntryKind.Favorites
            }

            name == PLAY_COUNTS_ENTRY -> {
                RestoreEntryKind.PlayCounts
            }

            name == NAMED_QUEUES_ENTRY -> {
                RestoreEntryKind.Queues
            }

            name == TRACK_POSITIONS_ENTRY -> {
                RestoreEntryKind.PerFileSettings
            }

            name.hasArchivePrefix(PLAYLISTS_PATH) -> {
                RestoreEntryKind.Playlists
            }

            name.hasArchivePrefix(SETTINGS_PATH) -> {
                RestoreEntryKind.Settings
            }

            name.hasArchivePrefix(LYRICS_PATH) -> {
                RestoreEntryKind.Lyrics
            }

            isCustomArtistPreferenceEntry(name) || isCustomArtistImageEntry(name) -> {
                RestoreEntryKind.ArtistImages
            }

            else -> {
                RestoreEntryKind.Unrecognized
            }
        }
    }

    fun isFavoritesPlaylist(
        playlistName: String,
        favoritesName: String,
    ): Boolean = playlistName == favoritesName

    /**
     * Legacy archives stored the localized favorites playlist as `Playlists/Favorites.m3u`.
     * Match the ZIP entry, not the current localized name, so old archives keep their meaning.
     */
    fun isLegacyFavoritesEntry(name: String): Boolean = name.equals(LEGACY_FAVORITES_ENTRY, ignoreCase = true)

    fun isCustomArtistPreferenceEntry(name: String): Boolean = name.hasArchivePrefix(CUSTOM_ARTISTS_PATH) && name.contains("prefs")

    fun isCustomArtistImageEntry(name: String): Boolean = name.hasArchivePrefix(CUSTOM_ARTISTS_PATH) && name.contains("custom_artist_images")

    /**
     * Restore is additive and monotonic: neither side may lower a stored statistic.
     */
    fun mergePlayCounts(
        current: PlayCountCounters?,
        incoming: PlayCountCounters,
    ): PlayCountCounters =
        PlayCountCounters(
            timePlayed = maxOf(current?.timePlayed ?: incoming.timePlayed, incoming.timePlayed),
            playCount = maxOf(current?.playCount ?: incoming.playCount, incoming.playCount),
            skipCount = maxOf(current?.skipCount ?: incoming.skipCount, incoming.skipCount),
        )

    fun shouldApplyTrackPosition(
        currentUpdatedAt: Long?,
        incomingUpdatedAt: Long,
    ): Boolean = currentUpdatedAt == null || incomingUpdatedAt > currentUpdatedAt

    /**
     * Returns null when the entry must be skipped: a blank id or a new queue above the cap.
     * Replacements keep their id and do not consume another slot.
     */
    fun planNamedQueueRestore(
        id: String,
        requestedName: String,
        namesById: Map<String, String>,
    ): NamedQueueRestorePlan? {
        if (id.isBlank()) return null
        if (id !in namesById && !QueueNamePolicy.canCreate(namesById.size)) return null
        val otherNames = namesById.filterKeys { it != id }.values
        return NamedQueueRestorePlan(id = id, name = QueueNamePolicy.uniqueName(requestedName, otherNames))
    }

    private fun String.hasArchivePrefix(prefix: String): Boolean = this == prefix || startsWith("$prefix/")
}
