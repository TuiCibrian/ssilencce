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

package com.ssilencce.data.local.repository

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore.Audio.AudioColumns
import com.ssilencce.core.sort.AlbumSortMode
import com.ssilencce.data.local.MediaQueryDispatcher
import com.ssilencce.data.local.room.ArtistEventPlayCount
import com.ssilencce.data.local.room.ArtistIdPlayCount
import com.ssilencce.data.local.room.HistoryDao
import com.ssilencce.data.local.room.HistoryEntity
import com.ssilencce.data.local.room.PlayCountDao
import com.ssilencce.data.local.room.PlayCountEntity
import com.ssilencce.data.local.room.PlayEventDao
import com.ssilencce.data.mapper.toHistoryEntity
import com.ssilencce.data.mapper.toSong
import com.ssilencce.data.model.Album
import com.ssilencce.data.model.Artist
import com.ssilencce.data.model.ArtistPlayStat
import com.ssilencce.data.model.ContentType
import com.ssilencce.data.model.ListeningPeriod
import com.ssilencce.data.model.Song
import com.ssilencce.util.Preferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

interface SmartRepository {
    suspend fun topAlbums(): List<Album>

    suspend fun topAlbumArtists(): List<Artist>

    suspend fun recentSongs(): List<Song>

    suspend fun recentSongs(
        query: String,
        contentType: ContentType,
    ): List<Song>

    suspend fun recentAlbums(): List<Album>

    suspend fun recentAlbumArtists(): List<Artist>

    suspend fun notRecentlyPlayedSongs(): List<Song>

    suspend fun playCountSongs(): List<Song>

    fun playCountSongsFlow(): Flow<List<Song>>

    fun playCountSongsFlow(period: ListeningPeriod): Flow<List<Song>>

    suspend fun playCountSongs(period: ListeningPeriod): List<Song>

    suspend fun artistPlayStats(period: ListeningPeriod): List<ArtistPlayStat>

    fun artistPlayStatsFlow(period: ListeningPeriod): Flow<List<ArtistPlayStat>>

    suspend fun findSongsInPlayCount(songs: List<Song>): List<PlayCountEntity>

    suspend fun findSongInPlayCount(songId: Long): PlayCountEntity?

    suspend fun deleteSongInPlayCount(songId: Long)

    suspend fun deleteSongsInPlayCount(songIds: List<Long>)

    suspend fun insetOrIncrementPlayCount(
        song: Song,
        timePlayed: Long,
    )

    suspend fun insetOrIncrementSkipCount(song: Song)

    suspend fun clearPlayCount()

    suspend fun historySongs(): List<Song>

    fun historySongsFlow(): Flow<List<Song>>

    suspend fun upsertSongInHistory(currentSong: Song)

    suspend fun deleteSongInHistory(songId: Long)

    suspend fun deleteSongsInHistory(songIds: List<Long>)

    suspend fun clearSongHistory()
}

class RealSmartRepository(
    private val context: Context,
    private val songRepository: RealSongRepository,
    private val albumRepository: RealAlbumRepository,
    private val artistRepository: RealArtistRepository,
    private val historyDao: HistoryDao,
    private val playCountDao: PlayCountDao,
    private val playEventDao: PlayEventDao,
) : SmartRepository {
    override suspend fun topAlbums(): List<Album> = albumRepository.splitIntoAlbums(playCountSongs(), sorted = false)

    override suspend fun topAlbumArtists(): List<Artist> = artistRepository.splitIntoAlbumArtists(topAlbums())

    override suspend fun recentSongs(): List<Song> = songRepository.songs(makeLastAddedCursor(null, ContentType.RecentSongs))

    override suspend fun recentSongs(
        query: String,
        contentType: ContentType,
    ): List<Song> = songRepository.songs(makeLastAddedCursor(query, contentType))

    override suspend fun recentAlbums(): List<Album> = albumRepository.splitIntoAlbums(recentSongs(), sorted = false)

    override suspend fun recentAlbumArtists(): List<Artist> = artistRepository.splitIntoAlbumArtists(recentAlbums())

    override suspend fun notRecentlyPlayedSongs(): List<Song> =
        buildList {
            addAll(songRepository.songs())

            val playedSongIds = historyDao.playedSongIds()
            removeAll { it.id in playedSongIds }

            val oldSongIds =
                historyDao.notPlayedSongIds(
                    cutoff = Preferences.getHistoryCutoff(context).interval,
                )
            val oldSongs =
                songRepository.songs(
                    songRepository.makeSongCursor(
                        selection = "${AudioColumns._ID} IN (${oldSongIds.joinToString(",") { "?" }})",
                        selectionValues = oldSongIds.map { it.toString() }.toTypedArray(),
                    ),
                )
            addAll(oldSongs)
        }

    override suspend fun playCountSongs(): List<Song> =
        playCountDao
            .playCountSongs()
            .fromPlayCountToSongs()

    override fun playCountSongsFlow(): Flow<List<Song>> =
        playCountDao.playCountSongsFlow().map { playCountEntities ->
            playCountEntities.fromPlayCountToSongs()
        }

    override fun playCountSongsFlow(period: ListeningPeriod): Flow<List<Song>> {
        if (period == ListeningPeriod.AllTime) return playCountSongsFlow()
        val cutoff = period.cutoffMillis() ?: return playCountSongsFlow(ListeningPeriod.AllTime)
        return playEventDao
            .topSongsSinceFlow(cutoff, PLAY_STATS_LIMIT)
            .map { playCountEntities -> playCountEntities.fromPlayCountToSongs() }
    }

    override suspend fun playCountSongs(period: ListeningPeriod): List<Song> {
        if (period == ListeningPeriod.AllTime) return playCountSongs()
        val cutoff = period.cutoffMillis() ?: return playCountSongs()
        val topSongs = playEventDao.topSongsSince(cutoff, PLAY_STATS_LIMIT)
        if (topSongs.isEmpty()) return emptyList()
        val entities =
            playCountDao
                .findSongsExistInPlayCount(topSongs.map { it.songId })
                .associateBy { it.id }
        return topSongs
            .mapNotNull { entities[it.songId] }
            .fromPlayCountToSongs()
    }

    override suspend fun artistPlayStats(period: ListeningPeriod): List<ArtistPlayStat> = artistPlayStatsFlow(period).first()

    override fun artistPlayStatsFlow(period: ListeningPeriod): Flow<List<ArtistPlayStat>> {
        if (period == ListeningPeriod.AllTime) {
            return playCountDao.topArtistsByPlayCountFlow(PLAY_STATS_LIMIT).map { counts ->
                counts.toAggregateArtistPlayStats()
            }
        }
        val cutoff = period.cutoffMillis() ?: return artistPlayStatsFlow(ListeningPeriod.AllTime)
        return playEventDao.topArtistsSinceFlow(cutoff, PLAY_STATS_LIMIT).map { counts ->
            counts.toRecordedArtistPlayStats()
        }
    }

    override suspend fun findSongsInPlayCount(songs: List<Song>): List<PlayCountEntity> {
        if (songs.isEmpty()) return emptyList()
        return buildList {
            songs.map { it.id }.chunked(MAX_ITEMS_PER_CHUNK).forEach { chunkIds ->
                addAll(playCountDao.findSongsExistInPlayCount(chunkIds))
            }
        }
    }

    override suspend fun findSongInPlayCount(songId: Long): PlayCountEntity? = playCountDao.findSongExistInPlayCount(songId)

    override suspend fun deleteSongInPlayCount(songId: Long) = playCountDao.deleteSongInPlayCount(songId)

    override suspend fun deleteSongsInPlayCount(songIds: List<Long>) {
        if (songIds.isEmpty()) return
        songIds.chunked(MAX_ITEMS_PER_CHUNK).forEach { chunkIds ->
            playCountDao.deleteSongsInPlayCount(chunkIds)
        }
    }

    override suspend fun insetOrIncrementPlayCount(
        song: Song,
        timePlayed: Long,
    ) = playEventDao.recordPlay(song, timePlayed, playCountDao)

    override suspend fun insetOrIncrementSkipCount(song: Song) = playCountDao.insertOrIncrementSkipCount(song)

    override suspend fun clearPlayCount() {
        playCountDao.clearPlayCount()
    }

    override suspend fun historySongs(): List<Song> =
        historyDao
            .historySongs(Preferences.getHistoryCutoff(context).interval)
            .fromHistoryToSongs()

    override fun historySongsFlow(): Flow<List<Song>> =
        historyDao
            .historySongsFlow(Preferences.getHistoryCutoff(context).interval)
            .map { historyEntities -> historyEntities.fromHistoryToSongs() }

    override suspend fun upsertSongInHistory(currentSong: Song) = historyDao.upsertSongInHistory(currentSong.toHistoryEntity(System.currentTimeMillis()))

    override suspend fun deleteSongInHistory(songId: Long) {
        historyDao.deleteSongInHistory(songId)
    }

    override suspend fun deleteSongsInHistory(songIds: List<Long>) {
        if (songIds.isEmpty()) return
        songIds.chunked(MAX_ITEMS_PER_CHUNK).forEach { chunkIds ->
            historyDao.deleteSongsInHistory(chunkIds)
        }
    }

    override suspend fun clearSongHistory() {
        historyDao.clearHistory()
    }

    private fun makeLastAddedCursor(
        query: String?,
        contentType: ContentType,
    ): Cursor? {
        val cutoff = Preferences.getLastAddedCutoff(context).interval
        val queryDispatcher =
            MediaQueryDispatcher()
                .setProjection(RealSongRepository.getBaseProjection())
                .setSelection("${AudioColumns.DATE_ADDED}>?")
                .setSelectionArguments(arrayOf(cutoff.toString()))
                .setSortOrder("${AudioColumns.DATE_ADDED} DESC")
        if (!query.isNullOrEmpty()) {
            when (contentType) {
                ContentType.RecentAlbums -> queryDispatcher.addSelection("${AudioColumns.ALBUM} LIKE ?")
                ContentType.RecentArtists -> queryDispatcher.addSelection("${AudioColumns.ALBUM_ARTIST} LIKE ?")
                ContentType.RecentSongs -> queryDispatcher.addSelection("${AudioColumns.TITLE} LIKE ?")
                else -> error("Content type is not valid: $contentType")
            }
            queryDispatcher.addArguments("%$query%")
        }
        return songRepository.makeSongCursor(queryDispatcher)
    }

    private suspend fun List<ArtistIdPlayCount>.toAggregateArtistPlayStats(): List<ArtistPlayStat> =
        withContext(IO) {
            map { count ->
                currentCoroutineContext().ensureActive()
                val artist =
                    try {
                        artistRepository.artist(count.artistId).takeIf {
                            it.songs.isNotEmpty() && it.name == count.artistName
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        null
                    }
                ArtistPlayStat(
                    key = ArtistPlayStat.Key.Aggregate(count.artistId),
                    artistName = count.artistName,
                    artist = artist,
                    playCount = count.playCount,
                    totalPlayCount = count.totalPlayCount,
                )
            }
        }

    private suspend fun List<ArtistEventPlayCount>.toRecordedArtistPlayStats(): List<ArtistPlayStat> =
        withContext(IO) {
            if (isEmpty()) return@withContext emptyList()
            val artistsByName =
                try {
                    val names = map { it.artistName }
                    val songs =
                        songRepository.songs(
                            songRepository.makeSongCursor(
                                selection = "${AudioColumns.ARTIST} IN (${names.joinToString(",") { "?" }})",
                                selectionValues = names.toTypedArray(),
                            ),
                        )
                    songs
                        .groupBy { it.artistName }
                        .mapNotNull { (name, artistSongs) ->
                            currentCoroutineContext().ensureActive()
                            val artistIds = artistSongs.map { it.artistId }.distinct()
                            if (artistIds.size != 1) {
                                null
                            } else {
                                name to
                                    Artist(
                                        id = artistIds.single(),
                                        albums = albumRepository.splitIntoAlbums(artistSongs, sortMode = AlbumSortMode.ArtistAlbums),
                                        filterSingles = Preferences.ignoreSingles,
                                    )
                            }
                        }.toMap()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    emptyMap()
                }
            // Missing or ambiguous current media must not erase the recorded artist's statistics.
            map { count ->
                ArtistPlayStat(
                    key = ArtistPlayStat.Key.Recorded(count.artistName),
                    artistName = count.artistName,
                    artist = artistsByName[count.artistName],
                    playCount = count.playCount,
                    totalPlayCount = count.totalPlayCount,
                )
            }
        }

    private suspend fun List<PlayCountEntity>.fromPlayCountToSongs(): List<Song> =
        withContext(IO) {
            val (deletedTracks, validTracks) = partition { it.id == -1L || !File(it.data).exists() }
            if (deletedTracks.isNotEmpty()) {
                deletedTracks
                    .map { it.id }
                    .chunked(MAX_ITEMS_PER_CHUNK)
                    .forEach { chunkIds ->
                        deleteSongsInPlayCount(chunkIds)
                    }
            }

            validTracks.map { it.toSong() }
        }

    private suspend fun List<HistoryEntity>.fromHistoryToSongs(): List<Song> =
        withContext(IO) {
            val (deletedTracks, validTracks) = partition { it.id == -1L || !File(it.data).exists() }
            if (deletedTracks.isNotEmpty()) {
                deletedTracks
                    .map { it.id }
                    .chunked(MAX_ITEMS_PER_CHUNK)
                    .forEach { chunkIds ->
                        deleteSongsInHistory(chunkIds)
                    }
            }

            validTracks.map { it.toSong() }
        }

    companion object {
        private const val PLAY_STATS_LIMIT = 100
    }
}
