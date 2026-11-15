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

package com.ssilencce.ui.screen.library.genres

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ssilencce.R
import com.ssilencce.core.model.GenreSubcategory
import com.ssilencce.core.model.shuffle.OpenShuffleMode
import com.ssilencce.core.sort.AlbumSortMode
import com.ssilencce.core.sort.ArtistSortMode
import com.ssilencce.core.sort.SongSortMode
import com.ssilencce.data.mapper.searchFilter
import com.ssilencce.data.model.Album
import com.ssilencce.data.model.Artist
import com.ssilencce.data.model.Genre
import com.ssilencce.data.model.GenreContent
import com.ssilencce.data.model.Song
import com.ssilencce.databinding.FragmentDetailListBinding
import com.ssilencce.extensions.applyHorizontalWindowInsets
import com.ssilencce.extensions.materialSharedAxis
import com.ssilencce.extensions.media.songCountStr
import com.ssilencce.extensions.media.songsDurationStr
import com.ssilencce.extensions.navigation.albumDetailArgs
import com.ssilencce.extensions.navigation.artistDetailArgs
import com.ssilencce.extensions.navigation.asFragmentExtras
import com.ssilencce.extensions.navigation.searchArgs
import com.ssilencce.extensions.setSupportActionBar
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
import com.ssilencce.ui.component.menu.onAlbumMenu
import com.ssilencce.ui.component.menu.onAlbumsMenu
import com.ssilencce.ui.component.menu.onArtistMenu
import com.ssilencce.ui.component.menu.onArtistsMenu
import com.ssilencce.ui.component.menu.onSongMenu
import com.ssilencce.ui.component.menu.onSongsMenu
import com.ssilencce.ui.dialogs.ComposeChoiceDialogFragment
import com.ssilencce.ui.dialogs.showComposeChoice
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class GenreDetailFragment :
    AbsMainActivityFragment(R.layout.fragment_detail_list),
    ISongCallback,
    IAlbumCallback,
    IArtistCallback {
    private val arguments by navArgs<GenreDetailFragmentArgs>()
    private val detailViewModel: GenreDetailViewModel by viewModel {
        parametersOf(arguments.extraGenre)
    }
    private lateinit var genre: Genre
    private lateinit var songAdapter: SongAdapter
    private var albumAdapter: AlbumAdapter? = null
    private var artistAdapter: ArtistAdapter? = null
    private var _binding: FragmentDetailListBinding? = null
    private val binding get() = _binding!!
    private var allSongs: List<Song> = emptyList()
    private var allAlbums: List<Album> = emptyList()
    private var allArtists: List<Artist> = emptyList()
    private var subcategory = GenreSubcategory.Songs
    private var quickSearchQuery: String = ""
    private var quickSearchHost: QuickSearchHost? = null

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDetailListBinding.bind(view)
        genre = arguments.extraGenre
        subcategory = GenreSubcategory.fromName(savedInstanceState?.getString(STATE_SUBCATEGORY))

        materialSharedAxis(view)
        setSupportActionBar(binding.toolbar)

        view.applyHorizontalWindowInsets()

        binding.collapsingAppBarLayout.title = genre.name
        binding.title.text = genre.name

        setupButtons()
        setupRecyclerView()

        parentFragmentManager.setFragmentResultListener(SUBCATEGORY_RESULT_KEY, this) { _, result ->
            val index = result.getInt(ComposeChoiceDialogFragment.RESULT_SELECTED_INDEX, -1)
            GenreSubcategory.entries.getOrNull(index)?.let { switchSubcategory(it) }
        }

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
                    applySubcategory()
                },
            ).also { it.setup() }

        detailViewModel.getContent().observe(viewLifecycleOwner) { content ->
            showContent(content)
        }
    }

    private fun setupButtons() {
        binding.playAction.setOnClickListener {
            playerViewModel.openQueue(visibleSongs(), shuffleMode = OpenShuffleMode.Off)
        }
        binding.shuffleAction.setOnClickListener {
            playerViewModel.openAndShuffleQueue(visibleSongs())
        }
    }

    private fun setupRecyclerView() {
        songAdapter =
            SongAdapter(
                activity = requireActivity(),
                dataSet = ArrayList(),
                itemLayoutRes = R.layout.item_list,
                sortMode = SongSortMode.GenreSongs,
                callback = this,
            )
        binding.recyclerView.apply {
            itemAnimator = DefaultItemAnimator()
            layoutManager = LinearLayoutManager(requireContext())
            adapter = songAdapter
        }
        songAdapter.registerAdapterDataObserver(
            object : RecyclerView.AdapterDataObserver() {
                override fun onChanged() {
                    super.onChanged()
                    updateEmptyState()
                }
            },
        )
    }

    private fun showContent(content: GenreContent) {
        binding.progressIndicator.hide()
        allSongs = content.songs
        allAlbums = content.albums
        allArtists = content.artists
        binding.subtitle.text =
            buildInfoString(
                content.songs.songCountStr(requireContext()),
                content.songs.songsDurationStr(),
            )
        if (content.songs.isEmpty()) {
            findNavController().navigateUp()
            return
        }
        applySubcategory()
    }

    private fun switchSubcategory(newSubcategory: GenreSubcategory) {
        if (subcategory == newSubcategory) return
        songAdapter.actionMode?.finish()
        albumAdapter?.actionMode?.finish()
        artistAdapter?.actionMode?.finish()
        subcategory = newSubcategory
        activity?.invalidateOptionsMenu()
        applySubcategory()
    }

    private fun applySubcategory() {
        val adapter: RecyclerView.Adapter<*> =
            when (subcategory) {
                GenreSubcategory.Songs -> {
                    songAdapter.dataSet =
                        allSongs
                            .filter { it.matchesDetailQuery(quickSearchQuery) }
                            .let { with(SongSortMode.GenreSongs) { it.sorted() } }
                    songAdapter
                }

                GenreSubcategory.Albums -> {
                    val current =
                        albumAdapter ?: AlbumAdapter(
                            activity = requireActivity(),
                            dataSet = emptyList(),
                            itemLayoutRes = R.layout.item_list,
                            sortMode = AlbumSortMode.GenreAlbums,
                            callback = this,
                        ).also { albumAdapter = it }
                    current.dataSet =
                        allAlbums
                            .filter { it.matchesDetailQuery(quickSearchQuery) }
                            .let { with(AlbumSortMode.GenreAlbums) { it.sorted() } }
                    current
                }

                GenreSubcategory.Artists -> {
                    val current =
                        artistAdapter ?: ArtistAdapter(
                            activity = requireActivity(),
                            dataSet = emptyList(),
                            itemLayoutRes = R.layout.item_list,
                            sortMode = ArtistSortMode.GenreArtists,
                            callback = this,
                        ).also { artistAdapter = it }
                    current.dataSet =
                        allArtists
                            .filter { it.matchesDetailQuery(quickSearchQuery) }
                            .let { with(ArtistSortMode.GenreArtists) { it.sorted() } }
                    current
                }
            }
        if (binding.recyclerView.adapter !== adapter) {
            binding.recyclerView.adapter = adapter
        }
        updateEmptyState()
    }

    private fun visibleSongs(): List<Song> {
        val songs =
            when (subcategory) {
                GenreSubcategory.Songs -> songAdapter.dataSet
                GenreSubcategory.Albums -> albumAdapter?.dataSet?.flatMap { it.songs }.orEmpty()
                GenreSubcategory.Artists -> artistAdapter?.dataSet?.flatMap { it.songs }.orEmpty()
            }
        return with(SongSortMode.GenreSongs) { songs.sorted() }
    }

    private fun updateEmptyState() {
        val fullCount =
            when (subcategory) {
                GenreSubcategory.Songs -> allSongs.size
                GenreSubcategory.Albums -> allAlbums.size
                GenreSubcategory.Artists -> allArtists.size
            }
        val visibleCount =
            when (subcategory) {
                GenreSubcategory.Songs -> songAdapter.dataSet.size
                GenreSubcategory.Albums -> albumAdapter?.dataSet?.size ?: 0
                GenreSubcategory.Artists -> artistAdapter?.dataSet?.size ?: 0
            }
        val noMatches = QuickSearchPolicy.isNoMatch(quickSearchQuery, fullCount, visibleCount)
        binding.empty.setText(if (noMatches) R.string.quick_search_empty else R.string.empty_label)
        binding.empty.isVisible = visibleCount == 0
    }

    private fun showSubcategoryChoice() {
        showComposeChoice(
            title = getString(R.string.genre_subcategory_title),
            options = GenreSubcategory.entries.map { getString(it.labelRes()) },
            selectedIndex = subcategory.ordinal,
            resultKey = SUBCATEGORY_RESULT_KEY,
        )
    }

    override fun songMenuItemClick(
        song: Song,
        menuItem: MenuItem,
        sharedElements: Array<Pair<View, String>>?,
    ): Boolean = song.onSongMenu(this, menuItem)

    override fun songsMenuItemClick(
        songs: List<Song>,
        menuItem: MenuItem,
    ) {
        songs.onSongsMenu(this, menuItem)
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
    ): Boolean = album.onAlbumMenu(this, menuItem)

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
    ): Boolean = artist.onArtistMenu(this, menuItem)

    override fun artistsMenuItemClick(
        artists: List<Artist>,
        menuItem: MenuItem,
    ) {
        artists.onArtistsMenu(this, menuItem)
    }

    override fun onCreateMenu(
        menu: Menu,
        inflater: MenuInflater,
    ) {
        inflater.inflate(R.menu.menu_genre_detail, menu)
        when (subcategory) {
            GenreSubcategory.Songs -> SongSortMode.GenreSongs.createMenu(menu)
            GenreSubcategory.Albums -> AlbumSortMode.GenreAlbums.createMenu(menu)
            GenreSubcategory.Artists -> ArtistSortMode.GenreArtists.createMenu(menu)
        }
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                findNavController().navigateUp()
                return true
            }

            R.id.action_search -> {
                findNavController().navigate(
                    R.id.nav_search,
                    searchArgs(genre.searchFilter(requireContext())),
                )
                return true
            }

            R.id.action_genre_subcategory -> {
                showSubcategoryChoice()
                return true
            }
        }
        val sortHandled =
            when (subcategory) {
                GenreSubcategory.Songs -> SongSortMode.GenreSongs.sortItemSelected(item)
                GenreSubcategory.Albums -> AlbumSortMode.GenreAlbums.sortItemSelected(item)
                GenreSubcategory.Artists -> ArtistSortMode.GenreArtists.sortItemSelected(item)
            }
        if (sortHandled) {
            applySubcategory()
            return true
        }
        return when (subcategory) {
            GenreSubcategory.Songs -> songAdapter.dataSet.onSongsMenu(this, item)
            GenreSubcategory.Albums -> albumAdapter?.dataSet?.onAlbumsMenu(this, item) ?: false
            GenreSubcategory.Artists -> artistAdapter?.dataSet?.onArtistsMenu(this, item) ?: false
        }
    }

    override fun onMediaContentChanged() {
        super.onMediaContentChanged()
        detailViewModel.loadGenreContent()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        QuickSearchState.save(outState, quickSearchQuery)
        outState.putString(STATE_SUBCATEGORY, subcategory.name)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        quickSearchHost = null
        super.onDestroyView()
        _binding = null
    }

    @StringRes
    private fun GenreSubcategory.labelRes(): Int =
        when (this) {
            GenreSubcategory.Songs -> R.string.songs_label
            GenreSubcategory.Albums -> R.string.albums_label
            GenreSubcategory.Artists -> R.string.artists_label
        }

    companion object {
        private const val SUBCATEGORY_RESULT_KEY = "genre_subcategory"
        private const val STATE_SUBCATEGORY = "genre_subcategory_state"
    }
}
