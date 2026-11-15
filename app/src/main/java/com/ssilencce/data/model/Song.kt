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

package com.ssilencce.data.model

import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.ssilencce.coil.CoverProvider
import com.ssilencce.core.model.MediaItemExtras
import com.ssilencce.core.model.filesystem.FileSystemItem
import com.ssilencce.data.local.repository.RealSongRepository.Companion.getAudioContentUri
import com.ssilencce.extensions.hasQ
import com.ssilencce.extensions.media.resolvedArtistName
import com.ssilencce.extensions.media.songInfo
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.util.Date
import java.util.Objects

@Parcelize
open class Song(
    open val id: Long,
    open val data: String,
    open val title: String,
    open val trackNumber: Int,
    open val year: Int,
    open val size: Long,
    open val duration: Long,
    open val dateAdded: Long,
    open val rawDateModified: Long,
    open val albumId: Long,
    open val albumName: String,
    open val artistId: Long,
    open val artistName: String,
    open val albumArtistName: String?,
    open val genreName: String?,
    open val volumeName: String? = null,
    open val composerName: String? = null,
    open val discNumber: Int = 0,
) : Parcelable,
    FileSystemItem {
    val uri: Uri
        get() =
            ExtraFormatUriStore.uriFor(id)?.let { Uri.parse(it) }
                ?: if (hasQ()) {
                    volumeName.let { volume ->
                        if (volume != null && volume != MediaStore.VOLUME_EXTERNAL) {
                            ContentUris.withAppendedId(MediaStore.Audio.Media.getContentUri(volume), id)
                        } else {
                            ContentUris.withAppendedId(getAudioContentUri(), id)
                        }
                    }
                } else {
                    ContentUris.withAppendedId(getAudioContentUri(), id)
                }

    @IgnoredOnParcel
    val dateModified by lazy { Date(rawDateModified * 1000) }

    /** MIME type of the underlying audio file, resolved without touching the content resolver. */
    val mimeType: String?
        get() =
            data
                .substringAfterLast('.', "")
                .takeIf { it.isNotEmpty() }
                ?.lowercase()
                ?.let { extension -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) }

    override val fileName: String
        get() = data.substringAfterLast("/", title)

    override val filePath: String
        get() = data

    override val fileDateAdded: Long
        get() = dateAdded

    override val fileDateModified: Long
        get() = rawDateModified

    protected constructor(song: Song) : this(
        song.id,
        song.data,
        song.title,
        song.trackNumber,
        song.year,
        song.size,
        song.duration,
        song.dateAdded,
        song.rawDateModified,
        song.albumId,
        song.albumName,
        song.artistId,
        song.artistName,
        song.albumArtistName,
        song.genreName,
        song.volumeName,
        song.composerName,
        song.discNumber,
    )

    fun toMediaItem(itemId: String = id.toString()): MediaItem =
        if (this == emptySong) {
            MediaItem.EMPTY
        } else {
            MediaItem
                .Builder()
                .setUri(uri)
                .setMimeType(mimeType)
                .setMediaId(itemId)
                .setTag(this)
                .setMediaMetadata(
                    MediaMetadata
                        .Builder()
                        .setIsPlayable(true)
                        .setIsBrowsable(false)
                        .setArtworkUri(CoverProvider.getImageUri(CoverProvider.SONG_COVER_PATH, id))
                        .setTitle(title)
                        .setSubtitle(songInfo())
                        .setAlbumTitle(albumName)
                        .setArtist(resolvedArtistName())
                        .setAlbumArtist(albumArtistName)
                        .setGenre(genreName)
                        .setTrackNumber(trackNumber)
                        .setReleaseYear(year)
                        .setDurationMs(duration.coerceAtLeast(0))
                        .setExtras(
                            Bundle().apply {
                                putLong(MediaItemExtras.ALBUM_ID, albumId)
                                putInt(MediaItemExtras.DISC_NUMBER, discNumber)
                            },
                        ).build(),
                ).build()
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val song = other as Song
        if (id != song.id) return false
        if (trackNumber != song.trackNumber) return false
        if (year != song.year) return false
        if (size != song.size) return false
        if (duration != song.duration) return false
        if (dateAdded != song.dateAdded) return false
        if (rawDateModified != song.rawDateModified) return false
        if (albumId != song.albumId) return false
        if (albumName != song.albumName) return false
        if (artistId != song.artistId) return false
        if (artistName != song.artistName) return false
        if (data != song.data) return false
        if (title != song.title) return false
        if (albumArtistName != song.albumArtistName) return false
        if (genreName != song.genreName) return false
        if (discNumber != song.discNumber) return false
        return volumeName == song.volumeName
    }

    override fun hashCode(): Int =
        Objects.hash(
            id,
            data,
            title,
            trackNumber,
            year,
            size,
            duration,
            dateAdded,
            rawDateModified,
            albumId,
            albumName,
            artistId,
            artistName,
            albumArtistName,
            genreName,
            volumeName,
            discNumber,
        )

    override fun toString(): String =
        "Song{" +
            "id=" + id +
            ", data='" + data + '\'' +
            ", title='" + title + '\'' +
            ", asReadableTrackNumber=" + trackNumber +
            ", year=" + year +
            ", size=" + size +
            ", duration=" + duration +
            ", dateAdded=" + dateAdded +
            ", rawDateModified=" + rawDateModified +
            ", albumId=" + albumId +
            ", albumName='" + albumName + '\'' +
            ", artistId=" + artistId +
            ", artistName='" + artistName + '\'' +
            ", albumArtistName='" + albumArtistName + '\'' +
            ", genreName='" + genreName + '\'' +
            ", volumeName='" + volumeName + '\'' +
            ", composerName='" + composerName + '\'' +
            ", discNumber=" + discNumber +
            '}'

    companion object {
        val emptySong = Song(-1, "", "", -1, -1, -1, -1, -1, -1, -1, "", -1, "", "", "", null)
    }
}
