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

package com.ssilencce.ui.screen.other

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.ssilencce.R
import com.ssilencce.core.model.shuffle.OpenShuffleMode
import com.ssilencce.data.mapper.lastAddedSearchFilter
import com.ssilencce.data.mapper.searchFilter
import com.ssilencce.data.model.Album
import com.ssilencce.data.model.Artist
import com.ssilencce.data.model.ArtistPlayStat
import com.ssilencce.data.model.ContentType
import com.ssilencce.data.model.ListeningPeriod
import com.ssilencce.data.model.Song
import com.ssilencce.databinding.FragmentDetailListBinding
import com.ssilencce.extensions.*
import com.ssilencce.extensions.media.playlistInfo
import com.ssilencce.extensions.navigation.albumDetailArgs
import com.ssilencce.extensions.navigation.artistDetailArgs
import com.ssilencce.extensions.navigation.asFragmentExtras
import com.ssilencce.extensions.navigation.searchArgs
import com.ssilencce.extensions.resources.createFastScroller
import com.ssilencce.extensions.resources.hide
import com.ssilencce.extensions.utilities.buildInfoString
import com.ssilencce.ui.IAlbumCallback
import com.ssilencce.ui.IArtistCallback
import com.ssilencce.ui.ISongCallback
import com.ssilencce.ui.adapters.album.AlbumAdapter
import com.ssilencce.ui.adapters.artist.ArtistAdapter
import com.ssilencce.ui.adapters.song.SongAdapter
import com.ssilencce.ui.component.base.AbsMainActivityFragment
import com.ssilencce.ui.component.base.QuickSearchHost
import com.ssilencce.ui.component.base.QuickSearchPolicy
import com.ssilencce.ui.component.base.QuickSearchState
import com.ssilencce.ui.component.base.matchesDetailQuery
import com.ssilencce.ui.component.menu.onAlbumsMenu
import com.ssilencce.ui.component.menu.onArtistsMenu
import com.ssilencce.ui.component.menu.onSongMenu
import com.ssilencce.ui.component.menu.onSongsMenu
import com.ssilencce.ui.dialogs.ComposeChoiceDialogFragment
import com.ssilencce.ui.dialogs.NotPlayedDaysDialogFragment
import com.ssilencce.ui.dialogs.showComposeChoice
import com.ssilencce.util.Preferences
import com.ssilencce.util.statistics.StatisticsCsvDocument
import com.ssilencce.util.statistics.StatisticsCsvExporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DetailListFragment :
    AbsMainActivityFragment(R.layout.fragment_detail_list),
    ISongCallback,
    IArtistCallback,
    IAlbumCallback {
    private val args by navArgs<DetailListFragmentArgs>()

    private var _binding: FragmentDetailListBinding? = null
    private val binding get() = _binding!!

    private lateinit var contentType: ContentType

    private var songList: List<Song> = arrayListOf()
    private var allSongs: List<Song> = emptyList()
    private var allArtists: List<Artist> = emptyList()
    private var allAlbums: List<Album> = emptyList()
    private var allArtistStats: List<ArtistPlayStat> = emptyList()
    private var currentSongAdapter: SongAdapter? = null
    private var currentArtistAdapter: ArtistAdapter? = null
    private var currentAlbumAdapter: AlbumAdapter? = null
    private var listEmptyMessageRes: Int = R.string.empty_label
    private var quickSearchQuery: String = ""
    private var quickSearchHost: QuickSearchHost? = null

    private var selectedPeriod: ListeningPeriod = ListeningPeriod.AllTime
    private val selectedPeriodFlow = MutableStateFlow(ListeningPeriod.AllTime)
    private var pendingStatisticsCsv: StatisticsCsvDocument? = null

    private val isStatsContent: Boolean
        get() = contentType == ContentType.TopTracks || contentType == ContentType.TopArtists

    private val createStatisticsCsv =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            val document = pendingStatisticsCsv
            if (uri == null) {
                pendingStatisticsCsv = null
            } else if (document != null) {
                val appContext = context?.applicationContext ?: return@registerForActivityResult
                lifecycleScope.launch {
                    try {
                        StatisticsCsvExporter.write(appContext.contentResolver, uri, document)
                        Toast.makeText(appContext, R.string.statistics_exported_successfully, Toast.LENGTH_SHORT).show()
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        Toast.makeText(appContext, R.string.statistics_export_failed, Toast.LENGTH_SHORT).show()
                    } finally {
                        pendingStatisticsCsv = null
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingStatisticsCsv =
            savedInstanceState?.let { state ->
                val name = state.getString(STATE_PENDING_STATISTICS_NAME) ?: return@let null
                val content = state.getString(STATE_PENDING_STATISTICS_CONTENT) ?: return@let null
                StatisticsCsvDocument(name, content)
            }
        parentFragmentManager.setFragmentResultListener(periodRequestKey, this) { _, result ->
            val selected = result.getInt(ComposeChoiceDialogFragment.RESULT_SELECTED_INDEX, -1)
            ListeningPeriod.entries.getOrNull(selected)?.let(::applyPeriod)
        }
        parentFragmentManager.setFragmentResultListener(NotPlayedDaysDialogFragment.RESULT_KEY, this) { _, result ->
            if (args.type != ContentType.NotRecentlyPlayed) return@setFragmentResultListener
            val days = result.getInt(NotPlayedDaysDialogFragment.RESULT_DAYS, Preferences.notPlayedDays)
            if (days == Preferences.notPlayedDays) return@setFragmentResultListener
            Preferences.notPlayedDays = days
            libraryViewModel.loadNotRecentlyPlayedSongs()
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        contentType = args.type
        selectedPeriod =
            savedInstanceState
                ?.getString(STATE_SELECTED_PERIOD)
                ?.let { name -> ListeningPeriod.entries.firstOrNull { it.name == name } }
                ?: ListeningPeriod.AllTime
        selectedPeriodFlow.value = selectedPeriod
        _binding = FragmentDetailListBinding.bind(view)
        materialSharedAxis(view)
        view.applyHorizontalWindowInsets()

        mainActivity.setSupportActionBar(binding.toolbar)
        binding.progressIndicator.hide()

        quickSearchQuery = QuickSearchState.restore(savedInstanceState)
        quickSearchHost =
            QuickSearchHost(
                fragment = this,
                bar = binding.quickSearchBar!!,
                recyclerView = binding.recyclerView,
                miniPlayerMargin = libraryViewModel.getMiniPlayerMargin(),
                barMargin = libraryViewModel.getFabMargin(),
                navigationHeightProvider = { mainActivity.navigationHeightWithInsets },
                queryProvider = { quickSearchQuery },
                onQueryChanged = { query ->
                    quickSearchQuery = query
                    applyQuickSearch()
                },
            ).also { it.setup() }

        setupButtons()
        loadContent()
        binding.toolbar.setTitle(contentType.titleRes)
        binding.title.setText(contentType.titleRes)
    }

    private fun setupButtons() {
        if (contentType == ContentType.Favorites || contentType == ContentType.History ||
            contentType == ContentType.TopTracks || contentType == ContentType.RecentSongs ||
            contentType == ContentType.NotRecentlyPlayed
        ) {
            binding.shuffleAction.setOnClickListener {
                playerViewModel.openAndShuffleQueue(songList)
            }
        } else {
            binding.shuffleAction.hide()
        }
        binding.playAction.setOnClickListener {
            playerViewModel.openQueue(songList, shuffleMode = OpenShuffleMode.Off)
        }
    }

    private fun songs(
        songs: List<Song>,
        emptyMessageRes: Int = 0,
    ) {
        allSongs = songs
        allArtists = emptyList()
        allAlbums = emptyList()
        allArtistStats = emptyList()
        listEmptyMessageRes = if (emptyMessageRes == 0) R.string.empty_label else emptyMessageRes
        updateSongMetadata(songs)
        if (songs.isEmpty() && emptyMessageRes == 0) {
            findNavController().navigateUp()
            return
        }
        applyQuickSearch()
    }

    private fun updateSongMetadata(songs: List<Song>) {
        binding.empty.setText(if (isStatsContent) R.string.no_play_stats_period else R.string.empty_label)
        binding.description.isVisible = isStatsContent
        if (isStatsContent) {
            binding.description.setText(R.string.stats_history_note)
        }
        binding.subtitle.text =
            when (contentType) {
                ContentType.RecentSongs -> {
                    buildInfoString(
                        Preferences.getLastAddedCutoff(requireContext()).description,
                        songs.playlistInfo(requireContext()),
                    )
                }

                ContentType.History -> {
                    buildInfoString(
                        Preferences.getHistoryCutoff(requireContext()).description,
                        songs.playlistInfo(requireContext()),
                    )
                }

                ContentType.TopTracks -> {
                    buildInfoString(
                        getString(selectedPeriod.titleRes()),
                        songs.playlistInfo(requireContext()),
                    )
                }

                ContentType.Favorites -> {
                    songs.playlistInfo(requireContext())
                }

                ContentType.NotRecentlyPlayed -> {
                    buildInfoString(
                        Preferences.getNotPlayedCutoff(requireContext()).description,
                        songs.playlistInfo(requireContext()),
                    )
                }

                else -> {
                    null
                }
            }
    }

    private fun applyQuickSearch() {
        val query = quickSearchQuery
        when (contentType) {
            ContentType.TopArtists -> {
                val filtered = allArtistStats.filter { it.matchesDetailQuery(query) }
                currentArtistAdapter?.submitPlayStats(filtered)
                songList = filtered.mapNotNull { it.artist }.flatMap { it.songs }
                binding.playAction.isEnabled = songList.isNotEmpty()
                updateListEmpty(filtered.size, allArtistStats.size)
            }

            ContentType.RecentArtists -> {
                val filtered = allArtists.filter { it.matchesDetailQuery(query) }
                currentArtistAdapter?.dataSet = filtered
                songList = filtered.flatMap { it.songs }
                updateListEmpty(filtered.size, allArtists.size)
            }

            ContentType.TopAlbums,
            ContentType.RecentAlbums,
            -> {
                val filtered = allAlbums.filter { it.matchesDetailQuery(query) }
                currentAlbumAdapter?.dataSet = filtered
                songList = filtered.flatMap { it.songs }
                updateListEmpty(filtered.size, allAlbums.size)
            }

            else -> {
                val filtered = allSongs.filter { it.matchesDetailQuery(query) }
                currentSongAdapter?.dataSet = filtered
                songList = filtered
                updateListEmpty(filtered.size, allSongs.size)
            }
        }
    }

    private fun updateListEmpty(
        visibleCount: Int,
        sourceCount: Int,
    ) {
        val noMatches = QuickSearchPolicy.isNoMatch(quickSearchQuery, sourceCount, visibleCount)
        binding.empty.setText(if (noMatches) R.string.quick_search_empty else listEmptyMessageRes)
        binding.empty.isVisible = visibleCount == 0
    }

    private fun loadContent() {
        when (contentType) {
            ContentType.TopArtists -> loadTopArtists()
            ContentType.RecentArtists -> loadArtists(ContentType.RecentArtists)
            ContentType.TopAlbums -> loadAlbums(ContentType.TopAlbums)
            ContentType.RecentAlbums -> loadAlbums(ContentType.RecentAlbums)
            ContentType.TopTracks -> topPlayed()
            ContentType.History -> loadHistory()
            ContentType.RecentSongs -> lastAddedSongs()
            ContentType.Favorites -> loadFavoriteSongs()
            ContentType.NotRecentlyPlayed -> loadNotRecentlyPlayed()
        }
    }

    private fun lastAddedSongs() {
        val songAdapter = songAdapter()
        binding.recyclerView.apply {
            adapter = songAdapter
            layoutManager = linearLayoutManager()
            createFastScroller(disablePopup = true)
        }
        libraryViewModel.lastAddedSongs().observe(viewLifecycleOwner) { songs ->
            songs(songs, R.string.playlist_empty_text)
        }
    }

    private fun topPlayed() {
        val songAdapter = songAdapter()
        binding.recyclerView.apply {
            adapter = songAdapter
            layoutManager = linearLayoutManager()
            createFastScroller(disablePopup = true)
        }
        launchAndRepeatWithViewLifecycle {
            selectedPeriodFlow.collectLatest { period ->
                libraryViewModel.playCountSongsFlow(period).collect { songs ->
                    songs(songs, R.string.no_play_stats_period)
                }
            }
        }
    }

    private fun loadHistory() {
        val songAdapter = songAdapter()
        binding.recyclerView.apply {
            adapter = songAdapter
            layoutManager = linearLayoutManager()
            createFastScroller(disablePopup = true)
        }
        launchAndRepeatWithViewLifecycle {
            libraryViewModel.historySongsFlow().collect { songs ->
                songs(songs, R.string.playlist_empty_text)
            }
        }
    }

    private fun loadNotRecentlyPlayed() {
        val songAdapter = songAdapter()
        binding.recyclerView.apply {
            adapter = songAdapter
            layoutManager = linearLayoutManager()
            createFastScroller(disablePopup = true)
        }
        libraryViewModel.notRecentlyPlayedSongs().observe(viewLifecycleOwner) { songs ->
            songs(songs)
        }
    }

    private fun loadFavoriteSongs() {
        val songAdapter = songAdapter()
        binding.recyclerView.apply {
            adapter = songAdapter
            layoutManager = linearLayoutManager()
            createFastScroller(disablePopup = true)
        }
        launchAndRepeatWithViewLifecycle {
            libraryViewModel.favoriteSongsFlow().collect { songs ->
                songs(songs)
            }
        }
    }

    private fun loadArtists(type: ContentType) {
        val artistAdapter = artistAdapter()
        val padding = dip(R.dimen.grid_item_margin)
        binding.recyclerView.apply {
            adapter = artistAdapter
            layoutManager = gridLayoutManager()
            updatePadding(left = padding, right = padding)
        }
        libraryViewModel.artists(type).observe(viewLifecycleOwner) { artists ->
            allArtists = artists
            allSongs = artists.flatMap { it.songs }
            allAlbums = emptyList()
            allArtistStats = emptyList()
            listEmptyMessageRes = R.string.empty_label
            binding.description.isVisible = false
            // Subtitle won't set automatically for albums and artists
            binding.subtitle.text = plurals(R.plurals.x_artists, artists.size)
            if (artists.isEmpty()) {
                findNavController().navigateUp()
            } else {
                applyQuickSearch()
            }
        }
    }

    private fun loadTopArtists() {
        val artistAdapter = artistAdapter()
        val padding = dip(R.dimen.grid_item_margin)
        binding.recyclerView.apply {
            adapter = artistAdapter
            layoutManager = gridLayoutManager()
            updatePadding(left = padding, right = padding)
        }
        launchAndRepeatWithViewLifecycle {
            selectedPeriodFlow.collectLatest { period ->
                libraryViewModel.artistPlayStatsFlow(period).collect { stats ->
                    allArtistStats = stats
                    allArtists = stats.mapNotNull { it.artist }
                    allSongs = allArtists.flatMap { it.songs }
                    allAlbums = emptyList()
                    listEmptyMessageRes = R.string.no_play_stats_period
                    binding.description.isVisible = true
                    binding.description.setText(R.string.stats_history_note)
                    binding.subtitle.text =
                        buildInfoString(
                            getString(selectedPeriod.titleRes()),
                            plurals(R.plurals.x_artists, stats.size),
                        )
                    applyQuickSearch()
                }
            }
        }
    }

    private fun loadAlbums(type: ContentType) {
        val albumAdapter = albumAdapter()
        val padding = dip(R.dimen.grid_item_margin)
        binding.recyclerView.apply {
            adapter = albumAdapter
            layoutManager = gridLayoutManager()
            updatePadding(left = padding, right = padding)
        }
        libraryViewModel.albums(type).observe(viewLifecycleOwner) { albums ->
            allAlbums = albums
            allSongs = albums.flatMap { it.songs }
            allArtists = emptyList()
            allArtistStats = emptyList()
            listEmptyMessageRes = R.string.empty_label
            binding.description.isVisible = false
            // Subtitle won't set automatically for albums and artists
            binding.subtitle.text = plurals(R.plurals.x_albums, albums.size)
            if (albums.isEmpty()) {
                findNavController().navigateUp()
            } else {
                applyQuickSearch()
            }
        }
    }

    private fun songAdapter(songs: List<Song> = listOf()): SongAdapter =
        SongAdapter(
            requireActivity(),
            songs,
            R.layout.item_list,
            callback = this,
            historyMode = contentType.isHistoryContent,
        ).also { currentSongAdapter = it }

    private fun artistAdapter(artists: List<Artist> = listOf()): ArtistAdapter =
        ArtistAdapter(requireActivity(), artists, R.layout.item_grid_circle, callback = this)
            .also { currentArtistAdapter = it }

    private fun albumAdapter(albums: List<Album> = listOf()): AlbumAdapter =
        AlbumAdapter(requireActivity(), albums, R.layout.item_grid, callback = this)
            .also { currentAlbumAdapter = it }

    private fun linearLayoutManager(): LinearLayoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

    private fun gridLayoutManager(): GridLayoutManager = GridLayoutManager(requireContext(), defaultGridColumns(), GridLayoutManager.VERTICAL, false)

    override fun onMediaContentChanged() {
        super.onMediaContentChanged()
        if (!contentType.isObservableContent && !isStatsContent) {
            loadContent()
        }
    }

    override fun onDestroyView() {
        quickSearchHost = null
        currentSongAdapter = null
        currentArtistAdapter = null
        currentAlbumAdapter = null
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateMenu(
        menu: Menu,
        menuInflater: MenuInflater,
    ) {
        menuInflater.inflate(R.menu.menu_list_detail, menu)
        if (!contentType.isSearchableContent) {
            menu.removeItem(R.id.action_search)
        }
        menu.findItem(R.id.action_clear_history).isVisible = contentType.isHistoryContent
        menu.findItem(R.id.action_most_played_period)?.isVisible = isStatsContent
        menu.findItem(R.id.action_not_played_period)?.isVisible = contentType == ContentType.NotRecentlyPlayed
        menu.findItem(R.id.action_export_statistics)?.isVisible = isStatsContent
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
        when (menuItem.itemId) {
            android.R.id.home -> {
                findNavController().navigateUp()
                true
            }

            R.id.action_search -> {
                if (contentType.isFavoriteContent) {
                    libraryViewModel.favoritePlaylist().observe(viewLifecycleOwner) {
                        findNavController().navigate(
                            R.id.nav_search,
                            searchArgs(it.searchFilter(requireContext())),
                        )
                    }
                } else if (contentType.isRecentContent) {
                    findNavController().navigate(
                        R.id.nav_search,
                        searchArgs(lastAddedSearchFilter(requireContext())),
                    )
                }
                true
            }

            R.id.action_most_played_period -> {
                showPeriodDialog()
                true
            }

            R.id.action_not_played_period -> {
                showNotPlayedDaysDialog()
                true
            }

            R.id.action_export_statistics -> {
                exportStatistics()
                true
            }

            R.id.action_clear_history -> {
                libraryViewModel.clearHistory()
                val translationY = -(libraryViewModel.getMiniPlayerMargin().value?.getWithSpace() ?: 0)
                val snackBar = Snackbar.make(binding.root, getString(R.string.history_cleared), Snackbar.LENGTH_LONG)
                val snackBarView = snackBar.view
                snackBarView.translationY = translationY.toFloat()
                snackBar.show()
                true
            }

            else -> {
                songList.onSongsMenu(this, menuItem)
            }
        }

    private fun exportStatistics() {
        val contentType = contentType
        val period = selectedPeriod
        val appContext = context?.applicationContext ?: return
        lifecycleScope.launch {
            try {
                val document = StatisticsCsvExporter.prepare(contentType, period)
                if (document == null) {
                    Toast.makeText(appContext, R.string.no_play_stats_period, Toast.LENGTH_SHORT).show()
                    return@launch
                }
                pendingStatisticsCsv = document
                createStatisticsCsv.launch(document.fileName)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                pendingStatisticsCsv = null
                Toast.makeText(appContext, R.string.statistics_export_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPeriodDialog() {
        val periods = ListeningPeriod.entries
        showComposeChoice(
            title = getString(R.string.most_played_period),
            options = periods.map { getString(it.titleRes()) },
            selectedIndex = periods.indexOf(selectedPeriod),
            resultKey = periodRequestKey,
        )
    }

    private fun showNotPlayedDaysDialog() {
        NotPlayedDaysDialogFragment
            .newInstance(
                currentDays = Preferences.notPlayedDays,
                historyDescription = Preferences.getHistoryCutoff(requireContext()).description,
            ).show(parentFragmentManager, "not-played-days")
    }

    private fun applyPeriod(period: ListeningPeriod) {
        if (period == selectedPeriod) return
        selectedPeriod = period
        selectedPeriodFlow.value = period
    }

    private fun ListeningPeriod.titleRes(): Int =
        when (this) {
            ListeningPeriod.ThisWeek -> R.string.period_this_week
            ListeningPeriod.ThisMonth -> R.string.period_this_month
            ListeningPeriod.Last12Months -> R.string.period_last_12_months
            ListeningPeriod.ThisYear -> R.string.period_this_year
            ListeningPeriod.AllTime -> R.string.period_all_time
        }

    override fun albumClick(
        album: Album,
        sharedElements: Array<Pair<View, String>>?,
    ) {
        findNavController().navigate(
            R.id.nav_album_detail,
            albumDetailArgs(album.id),
            null,
            sharedElements.asFragmentExtras(),
        )
    }

    override fun albumMenuItemClick(
        album: Album,
        menuItem: MenuItem,
        sharedElements: Array<Pair<View, String>>?,
    ): Boolean = false

    override fun albumsMenuItemClick(
        albums: List<Album>,
        menuItem: MenuItem,
    ) {
        albums.onAlbumsMenu(this, menuItem)
    }

    override fun artistClick(
        artist: Artist,
        sharedElements: Array<Pair<View, String>>?,
    ) {
        findNavController().navigate(
            R.id.nav_artist_detail,
            artistDetailArgs(artist),
            null,
            sharedElements.asFragmentExtras(),
        )
    }

    override fun artistMenuItemClick(
        artist: Artist,
        menuItem: MenuItem,
        sharedElements: Array<Pair<View, String>>?,
    ): Boolean = false

    override fun artistsMenuItemClick(
        artists: List<Artist>,
        menuItem: MenuItem,
    ) {
        artists.onArtistsMenu(this, menuItem)
    }

    override fun songMenuItemClick(
        song: Song,
        menuItem: MenuItem,
        sharedElements: Array<Pair<View, String>>?,
    ): Boolean =
        if (menuItem.itemId == R.id.action_remove_from_history) {
            if (contentType.isHistoryContent) {
                removeSongsFromHistory(listOf(song))
                true
            } else {
                false
            }
        } else {
            song.onSongMenu(this, menuItem)
        }

    override fun songsMenuItemClick(
        songs: List<Song>,
        menuItem: MenuItem,
    ) {
        if (menuItem.itemId == R.id.action_remove_from_history) {
            if (contentType.isHistoryContent && songs.isNotEmpty()) {
                if (songs.size == 1) {
                    removeSongsFromHistory(songs)
                } else {
                    confirmRemoveSongsFromHistory(songs)
                }
            }
            return
        }
        songs.onSongsMenu(this, menuItem)
    }

    private fun confirmRemoveSongsFromHistory(songs: List<Song>) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.remove_x_songs_from_history_title)
            .setMessage(getString(R.string.remove_x_songs_from_history_message, songs.size))
            .setPositiveButton(android.R.string.ok) { _, _ -> removeSongsFromHistory(songs) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun removeSongsFromHistory(songs: List<Song>) {
        if (songs.isEmpty()) return
        libraryViewModel.deleteSongsFromHistory(songs.map { it.id })
        val translationY = -(libraryViewModel.getMiniPlayerMargin().value?.getWithSpace() ?: 0)
        val message = getString(R.string.removed_x_songs_from_history, songs.size)
        val snackBar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        snackBar.view.translationY = translationY.toFloat()
        snackBar.show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        QuickSearchState.save(outState, quickSearchQuery)
        outState.putString(STATE_SELECTED_PERIOD, selectedPeriod.name)
        pendingStatisticsCsv?.let { document ->
            outState.putString(STATE_PENDING_STATISTICS_NAME, document.fileName)
            outState.putString(STATE_PENDING_STATISTICS_CONTENT, document.content)
        }
        super.onSaveInstanceState(outState)
    }

    private val periodRequestKey: String
        get() = "compose-period-${args.type.name}"

    companion object {
        private const val STATE_SELECTED_PERIOD = "selected_listening_period"
        private const val STATE_PENDING_STATISTICS_NAME = "pending_statistics_csv_name"
        private const val STATE_PENDING_STATISTICS_CONTENT = "pending_statistics_csv_content"
    }
}
