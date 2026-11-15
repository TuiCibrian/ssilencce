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

package com.ssilencce.ui.screen.library.years

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ssilencce.R
import com.ssilencce.core.model.shuffle.OpenShuffleMode
import com.ssilencce.core.sort.SongSortMode
import com.ssilencce.data.mapper.searchFilter
import com.ssilencce.data.model.ReleaseYear
import com.ssilencce.data.model.Song
import com.ssilencce.databinding.FragmentDetailListBinding
import com.ssilencce.extensions.applyHorizontalWindowInsets
import com.ssilencce.extensions.materialSharedAxis
import com.ssilencce.extensions.media.songCountStr
import com.ssilencce.extensions.media.songsDurationStr
import com.ssilencce.extensions.navigation.searchArgs
import com.ssilencce.extensions.setSupportActionBar
import com.ssilencce.extensions.utilities.buildInfoString
import com.ssilencce.ui.ISongCallback
import com.ssilencce.ui.adapters.song.SongAdapter
import com.ssilencce.ui.component.base.AbsMainActivityFragment
import com.ssilencce.ui.component.base.QuickSearchHost
import com.ssilencce.ui.component.base.QuickSearchPolicy
import com.ssilencce.ui.component.base.QuickSearchState
import com.ssilencce.ui.component.base.matchesDetailQuery
import com.ssilencce.ui.component.menu.onSongMenu
import com.ssilencce.ui.component.menu.onSongsMenu
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class YearDetailFragment :
    AbsMainActivityFragment(R.layout.fragment_detail_list),
    ISongCallback {
    private val arguments by navArgs<YearDetailFragmentArgs>()
    private val detailViewModel: YearDetailViewModel by viewModel {
        parametersOf(arguments.extraYear)
    }

    private var _binding: FragmentDetailListBinding? = null
    private val binding get() = _binding!!

    private lateinit var songAdapter: SongAdapter
    private var allSongs: List<Song> = emptyList()
    private var quickSearchQuery: String = ""
    private var quickSearchHost: QuickSearchHost? = null

    private val year: ReleaseYear
        get() = detailViewModel.getYear().value ?: ReleaseYear.Empty

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDetailListBinding.bind(view)

        setSupportActionBar(binding.toolbar)
        materialSharedAxis(view)
        view.applyHorizontalWindowInsets()

        setupButtons()
        setupRecyclerView()

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

        detailViewModel.getYear().observe(viewLifecycleOwner) { year ->
            binding.collapsingAppBarLayout.title = year.name
            binding.title.text = year.name
            songs(year.songs)
        }
    }

    private fun setupButtons() {
        binding.playAction.setOnClickListener {
            playerViewModel.openQueue(songAdapter.dataSet, shuffleMode = OpenShuffleMode.Off)
        }
        binding.shuffleAction.setOnClickListener {
            playerViewModel.openAndShuffleQueue(songAdapter.dataSet)
        }
    }

    private fun setupRecyclerView() {
        songAdapter = SongAdapter(requireActivity(), ArrayList(), R.layout.item_list, callback = this)
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

    fun songs(songs: List<Song>) {
        allSongs = songs
        binding.progressIndicator.hide()
        binding.subtitle.text = buildInfoString(songs.songCountStr(requireContext()), songs.songsDurationStr())
        if (songs.isEmpty()) {
            findNavController().navigateUp()
            return
        }
        applyQuickSearch()
    }

    private fun applyQuickSearch() {
        songAdapter.dataSet = allSongs.filter { it.matchesDetailQuery(quickSearchQuery) }
        updateEmptyState()
    }

    private fun updateEmptyState() {
        val visibleCount = songAdapter.dataSet.size
        val noMatches = QuickSearchPolicy.isNoMatch(quickSearchQuery, allSongs.size, visibleCount)
        binding.empty.setText(if (noMatches) R.string.quick_search_empty else R.string.empty_label)
        binding.empty.isVisible = visibleCount == 0
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

    override fun onCreateMenu(
        menu: Menu,
        inflater: MenuInflater,
    ) {
        inflater.inflate(R.menu.menu_genre_detail, menu)
        SongSortMode.YearSongs.createMenu(menu)
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean =
        if (SongSortMode.YearSongs.sortItemSelected(item)) {
            detailViewModel.loadDetail()
            true
        } else {
            when (item.itemId) {
                android.R.id.home -> {
                    findNavController().navigateUp()
                    true
                }

                R.id.action_search -> {
                    findNavController().navigate(R.id.nav_search, searchArgs(year.searchFilter(requireContext())))
                    true
                }

                else -> {
                    songAdapter.dataSet.onSongsMenu(this, item)
                }
            }
        }

    override fun onSaveInstanceState(outState: Bundle) {
        QuickSearchState.save(outState, quickSearchQuery)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        quickSearchHost = null
        super.onDestroyView()
        _binding = null
    }
}
