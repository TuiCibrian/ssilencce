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

package com.ssilencce

import androidx.preference.PreferenceManager
import androidx.room.Room
import com.ssilencce.coil.CustomArtistImageManager
import com.ssilencce.coil.CustomPlaylistImageManager
import com.ssilencce.core.SsilencceDatabase
import com.ssilencce.core.audio.AudioOutputObserver
import com.ssilencce.data.local.EditTarget
import com.ssilencce.data.local.MediaStoreWriter
import com.ssilencce.data.local.repository.AlbumRepository
import com.ssilencce.data.local.repository.ArtistRepository
import com.ssilencce.data.local.repository.GenreRepository
import com.ssilencce.data.local.repository.LyricsRepository
import com.ssilencce.data.local.repository.NetworkRepository
import com.ssilencce.data.local.repository.NetworkRepositoryImpl
import com.ssilencce.data.local.repository.PlaylistRepository
import com.ssilencce.data.local.repository.RealAlbumRepository
import com.ssilencce.data.local.repository.RealArtistRepository
import com.ssilencce.data.local.repository.RealGenreRepository
import com.ssilencce.data.local.repository.RealLyricsRepository
import com.ssilencce.data.local.repository.RealPlaylistRepository
import com.ssilencce.data.local.repository.RealRepository
import com.ssilencce.data.local.repository.RealSearchRepository
import com.ssilencce.data.local.repository.RealSmartRepository
import com.ssilencce.data.local.repository.RealSongRepository
import com.ssilencce.data.local.repository.RealSpecialRepository
import com.ssilencce.data.local.repository.Repository
import com.ssilencce.data.local.repository.SearchRepository
import com.ssilencce.data.local.repository.SmartRepository
import com.ssilencce.data.local.repository.SongRepository
import com.ssilencce.data.local.repository.SpecialRepository
import com.ssilencce.data.model.Genre
import com.ssilencce.data.remote.deezer.DeezerService
import com.ssilencce.data.remote.github.GitHubService
import com.ssilencce.data.remote.jsonHttpClient
import com.ssilencce.data.remote.lastfm.LastFmService
import com.ssilencce.data.remote.listenbrainz.ListenBrainzService
import com.ssilencce.data.remote.lyrics.LyricsDownloadService
import com.ssilencce.data.remote.musicbrainz.MusicBrainzService
import com.ssilencce.data.remote.provideOkHttp
import com.ssilencce.playback.SleepTimer
import com.ssilencce.playback.equalizer.EqualizerManager
import com.ssilencce.playback.processor.BalanceAudioProcessor
import com.ssilencce.playback.processor.ReplayGainAudioProcessor
import com.ssilencce.ui.screen.equalizer.EqualizerViewModel
import com.ssilencce.ui.screen.info.InfoViewModel
import com.ssilencce.ui.screen.library.LibraryViewModel
import com.ssilencce.ui.screen.library.albums.AlbumDetailViewModel
import com.ssilencce.ui.screen.library.artists.ArtistDetailViewModel
import com.ssilencce.ui.screen.library.folders.FolderDetailViewModel
import com.ssilencce.ui.screen.library.genres.GenreDetailViewModel
import com.ssilencce.ui.screen.library.playlists.PlaylistDetailViewModel
import com.ssilencce.ui.screen.library.search.SearchViewModel
import com.ssilencce.ui.screen.library.years.YearDetailViewModel
import com.ssilencce.ui.screen.lyrics.LyricsViewModel
import com.ssilencce.ui.screen.player.PlayerViewModel
import com.ssilencce.ui.screen.sleeptimer.SleepTimerViewModel
import com.ssilencce.ui.screen.tageditor.TagEditorViewModel
import com.ssilencce.ui.screen.update.UpdateViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val networkModule =
    module {
        factory {
            jsonHttpClient(okHttpClient = get())
        }
        factory {
            provideOkHttp(context = get())
        }
        single {
            GitHubService(context = androidContext(), client = get())
        }
        single {
            DeezerService(client = get())
        }
        single {
            LastFmService(client = get())
        }
        single {
            ListenBrainzService(client = get())
        }
        single {
            MusicBrainzService(client = get())
        }
        single {
            LyricsDownloadService(client = get())
        }
    }

private val mainModule =
    module {
        single {
            androidContext().contentResolver
        }
        single {
            PreferenceManager.getDefaultSharedPreferences(androidContext())
        }
        single {
            SleepTimer(context = androidContext())
        }
        single {
            BalanceAudioProcessor()
        }
        single {
            ReplayGainAudioProcessor()
        }
        single {
            EqualizerManager(
                context = androidContext(),
                balanceProcessor = get(),
                replayGainProcessor = get(),
                audioOutputObserver = get(),
            )
        }
        single {
            MediaStoreWriter(context = androidContext(), contentResolver = get())
        }
        single {
            CustomArtistImageManager(context = androidContext())
        }
        single {
            CustomPlaylistImageManager(context = androidContext())
        }
        single {
            AudioOutputObserver(context = androidContext())
        }
    }

private val roomModule =
    module {
        single {
            Room
                .databaseBuilder(androidContext(), SsilencceDatabase::class.java, "music_database.db")
                .addMigrations(
                    SsilencceDatabase.MIGRATION_1_2,
                    SsilencceDatabase.MIGRATION_2_3,
                    SsilencceDatabase.MIGRATION_3_4,
                    SsilencceDatabase.MIGRATION_4_5,
                    SsilencceDatabase.MIGRATION_5_6,
                ).build()
        }

        factory {
            get<SsilencceDatabase>().playlistDao()
        }

        factory {
            get<SsilencceDatabase>().playCountDao()
        }

        factory {
            get<SsilencceDatabase>().historyDao()
        }

        factory {
            get<SsilencceDatabase>().queueDao()
        }

        factory {
            get<SsilencceDatabase>().inclExclDao()
        }

        factory {
            get<SsilencceDatabase>().lyricsDao()
        }
    }

private val dataModule =
    module {
        single {
            RealRepository(
                context = androidContext(),
                songRepository = get(),
                albumRepository = get(),
                artistRepository = get(),
                genreRepository = get(),
                smartRepository = get(),
                specialRepository = get(),
                playlistRepository = get(),
                searchRepository = get(),
                networkRepository = get(),
            )
        } bind Repository::class

        single {
            RealSongRepository(context = get(), inclExclDao = get())
        } bind SongRepository::class

        single {
            RealAlbumRepository(songRepository = get())
        } bind AlbumRepository::class

        single {
            RealArtistRepository(songRepository = get(), albumRepository = get())
        } bind ArtistRepository::class

        single {
            RealPlaylistRepository(
                context = androidContext(),
                songRepository = get(),
                playlistDao = get(),
            )
        } bind PlaylistRepository::class

        single {
            RealGenreRepository(contentResolver = get(), songRepository = get())
        } bind GenreRepository::class

        single {
            RealSearchRepository(
                albumRepository = get(),
                songRepository = get(),
                artistRepository = get(),
                playlistRepository = get(),
                genreRepository = get(),
                specialRepository = get(),
            )
        } bind SearchRepository::class

        single {
            RealSmartRepository(
                context = androidContext(),
                songRepository = get(),
                albumRepository = get(),
                artistRepository = get(),
                historyDao = get(),
                playCountDao = get(),
            )
        } bind SmartRepository::class

        single {
            RealSpecialRepository(songRepository = get())
        } bind SpecialRepository::class

        single {
            RealLyricsRepository(
                context = androidContext(),
                preferences = get(),
                lyricsDownloadService = get(),
                lyricsDao = get(),
            )
        } bind LyricsRepository::class

        single {
            NetworkRepositoryImpl(
                context = androidContext(),
                preferences = get(),
                lastFmService = get(),
                listenBrainzService = get(),
                deezerService = get(),
                musicBrainzService = get(),
            )
        } bind NetworkRepository::class
    }

private val viewModule =
    module {
        viewModel {
            LibraryViewModel(repository = get(), inclExclDao = get(), customPlaylistImageManager = get())
        }

        viewModel {
            PlayerViewModel(preferences = get(), repository = get())
        }

        viewModel {
            EqualizerViewModel(
                contentResolver = get(),
                equalizerManager = get(),
                audioOutputObserver = get(),
                mediaStoreWriter = get(),
            )
        }

        viewModel {
            SleepTimerViewModel(
                application = androidApplication(),
                sleepTimer = get(),
            )
        }

        viewModel { (albumId: Long) ->
            AlbumDetailViewModel(
                application = androidApplication(),
                repository = get(),
                albumId = albumId,
            )
        }

        viewModel { (artistId: Long, artistName: String?) ->
            ArtistDetailViewModel(
                application = androidApplication(),
                repository = get(),
                artistId = artistId,
                artistName = artistName,
            )
        }

        viewModel { (playlistId: Long) ->
            PlaylistDetailViewModel(playlistRepository = get(), playlistId = playlistId)
        }

        viewModel { (genre: Genre) ->
            GenreDetailViewModel(repository = get(), genre = genre)
        }

        viewModel { (year: Int) ->
            YearDetailViewModel(repository = get(), year = year)
        }

        viewModel { (path: String) ->
            FolderDetailViewModel(repository = get(), folderPath = path)
        }

        viewModel {
            SearchViewModel(repository = get())
        }

        viewModel { (target: EditTarget) ->
            TagEditorViewModel(
                repository = get(),
                customArtistImageManager = get(),
                target = target,
            )
        }

        viewModel {
            LyricsViewModel(application = androidApplication(), preferences = get(), repository = get())
        }

        viewModel {
            InfoViewModel(repository = get())
        }

        viewModel {
            UpdateViewModel(updateService = get())
        }
    }

val appModules = listOf(networkModule, mainModule, roomModule, dataModule, viewModule)
