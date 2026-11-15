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

import com.ssilencce.data.local.room.NamedQueueWithItems
import com.ssilencce.data.local.room.PlayCountEntity
import com.ssilencce.data.local.room.SongPlaybackParamEntity
import com.ssilencce.data.local.room.TrackPositionEntity
import kotlinx.serialization.Serializable

@Serializable
data class PlayCountBackupEntry(
    val id: Long,
    val data: String,
    val title: String,
    val trackNumber: Int,
    val year: Int,
    val size: Long,
    val duration: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val albumId: Long,
    val albumName: String,
    val artistId: Long,
    val artistName: String,
    val albumArtistName: String? = null,
    val genreName: String? = null,
    val timePlayed: Long,
    val playCount: Int,
    val skipCount: Int,
)

@Serializable
data class NamedQueueBackupEntry(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val resumeIndex: Int,
    val resumePositionMs: Long,
    val repeatMode: Int,
    val shuffleEnabled: Boolean,
    val mediaIds: List<String>,
)

@Serializable
data class TrackPositionBackupEntry(
    val mediaId: String,
    val positionMs: Long,
    val updatedAt: Long,
)

@Serializable
data class SongPlaybackParamBackupEntry(
    val songId: Long,
    val speed: Float,
    val pitch: Float,
)

fun PlayCountEntity.toBackupEntry(): PlayCountBackupEntry =
    PlayCountBackupEntry(
        id = id,
        data = data,
        title = title,
        trackNumber = trackNumber,
        year = year,
        size = size,
        duration = duration,
        dateAdded = dateAdded,
        dateModified = dateModified,
        albumId = albumId,
        albumName = albumName,
        artistId = artistId,
        artistName = artistName,
        albumArtistName = albumArtistName,
        genreName = genreName,
        timePlayed = timePlayed,
        playCount = playCount,
        skipCount = skipCount,
    )

fun PlayCountBackupEntry.toEntity(): PlayCountEntity =
    PlayCountEntity(
        id = id,
        data = data,
        title = title,
        trackNumber = trackNumber,
        year = year,
        size = size,
        duration = duration,
        dateAdded = dateAdded,
        dateModified = dateModified,
        albumId = albumId,
        albumName = albumName,
        artistId = artistId,
        artistName = artistName,
        albumArtistName = albumArtistName,
        genreName = genreName,
        timePlayed = timePlayed,
        playCount = playCount,
        skipCount = skipCount,
    )

fun NamedQueueWithItems.toBackupEntry(): NamedQueueBackupEntry =
    NamedQueueBackupEntry(
        id = queue.id,
        name = queue.name,
        createdAt = queue.createdAt,
        updatedAt = queue.updatedAt,
        resumeIndex = queue.resumeIndex,
        resumePositionMs = queue.resumePositionMs,
        repeatMode = queue.repeatMode,
        shuffleEnabled = queue.shuffleEnabled,
        mediaIds = orderedItems().map { it.mediaId },
    )

fun TrackPositionEntity.toBackupEntry(): TrackPositionBackupEntry =
    TrackPositionBackupEntry(
        mediaId = mediaId,
        positionMs = positionMs,
        updatedAt = updatedAt,
    )

fun TrackPositionBackupEntry.toEntity(): TrackPositionEntity =
    TrackPositionEntity(
        mediaId = mediaId,
        positionMs = positionMs,
        updatedAt = updatedAt,
    )

fun SongPlaybackParamEntity.toBackupEntry(): SongPlaybackParamBackupEntry =
    SongPlaybackParamBackupEntry(
        songId = songId,
        speed = speed,
        pitch = pitch,
    )

fun SongPlaybackParamBackupEntry.toEntity(): SongPlaybackParamEntity =
    SongPlaybackParamEntity(
        songId = songId,
        speed = speed,
        pitch = pitch,
    )
