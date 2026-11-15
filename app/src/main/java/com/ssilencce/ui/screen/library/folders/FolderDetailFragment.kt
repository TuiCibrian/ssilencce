/*
 * Copyright (c) 2025 Christians Martínez Alvarado
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

package com.ssilencce.ui.screen.library.folders

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.ssilencce.R
import com.ssilencce.core.model.shuffle.OpenShuffleMode
import com.ssilencce.core.sort.SongSortMode
import com.ssilencce.data.mapper.searchFilter
import com.ssilencce.data.model.Folder
import com.ssilencce.data.model.Song
import com.ssilencce.databinding.FragmentDetailListBinding
import com.ssilencce.extensions.applyHorizontalWindowInsets
import com.ssilencce.extensions.materialSharedAxis
import com.ssilencce.extensions.media.songCountStr
import com.ssilencce.extensions.media.songsDurationStr
import com.ssilencce.extensions.navigation.searchArgs
import com.ssilencce.extensions.setSupportActionBar
import com.ssilencce.extensions.showToast
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
import com.ssilencce.ui.component.menu.showAddShortcutDialog
import com.ssilencce.util.GhostMode
import com.ssilencce.util.ShortcutEntityType
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class FolderDetailFragment :
    AbsMainActivityFragment(R.layout.fragment_detail_list),
    ISongCallback {
    private val arguments by navArgs<FolderDetailFragmentArgs>()
    private val detailViewModel: FolderDetailViewModel by viewModel {
        parametersOf(arguments.extraFolderPath)
    }

    private var _binding: FragmentDetailListBinding? = null
    private val binding get() = _binding!!

    private lateinit var songAdapter: SongAdapter
    private var allSongs: List<Song> = emptyList()
    private var quickSearchQuery: String = ""
    private var quickSearchHost: QuickSearchHost? = null

    private val folder: Folder
        get() = detailViewModel.getFolder().value ?: Folder.empty

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDetailListBinding.bind(view)
        materialSharedAxis(view)
        setSupportActionBar(binding.toolbar)

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

        detailViewModel.getFolder().observe(viewLifecycleOwner) {
            binding.collapsingAppBarLayout.title = it.fileName
            binding.title.text = it.fileName
            songs(it.songs)
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
        songAdapter =
            SongAdapter(
                activity = requireActivity(),
                dataSet = ArrayList(),
                itemLayoutRes = R.layout.item_list,
                sortMode = SongSortMode.FolderSongs,
                callback = this,
            )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = songAdapter
        }
    }

    fun songs(songs: List<Song>) {
        allSongs = songs
        if (songs.isEmpty()) {
            findNavController().popBackStack()
            return
        }
        binding.progressIndicator.hide()
        binding.subtitle.text =
            buildInfoString(songs.songCountStr(requireContext()), songs.songsDurationStr())
        applyQuickSearch()
    }

    private fun applyQuickSearch() {
        songAdapter.dataSet =
            allSongs.filter { it.matchesDetailQuery(quickSearchQuery, includeFileName = true) }
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
        inflater.inflate(R.menu.menu_folder_detail, menu)
        SongSortMode.FolderSongs.createMenu(menu)
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean =
        when {
            SongSortMode.FolderSongs.sortItemSelected(item) -> {
                detailViewModel.loadDetail()
                true
            }

            item.itemId == R.id.action_search -> {
                findNavController().navigate(
                    R.id.nav_search,
                    searchArgs(folder.searchFilter(requireContext())),
                )
                true
            }

            item.itemId == R.id.action_add_to_home -> {
                if (folder == Folder.empty) {
                    false
                } else {
                    showAddShortcutDialog(
                        fragment = this,
                        type = ShortcutEntityType.Folder,
                        id = folder.filePath.hashCode().toString(),
                        name = folder.fileName,
                        path = folder.filePath,
                    )
                    true
                }
            }

            item.itemId == R.id.action_folder_info -> {
                if (folder == Folder.empty) {
                    false
                } else {
                    FolderInfoDialog.show(this, folder.filePath)
                    true
                }
            }

            item.itemId == R.id.action_rename_folder -> {
                if (folder == Folder.empty) {
                    false
                } else {
                    FolderNameDialog.showRename(this, folder.filePath, folder.fileName)
                    true
                }
            }

            item.itemId == R.id.action_ghost_folder -> {
                if (folder == Folder.empty) {
                    false
                } else {
                    if (GhostMode.addFolder(folder.filePath)) {
                        showToast(R.string.ghost_folder_added)
                        libraryViewModel.reloadAll()
                        findNavController().navigateUp()
                    } else {
                        showToast(R.string.ghost_folder_already_added)
                    }
                    true
                }
            }

            item.itemId == R.id.action_exclude_folder -> {
                if (folder == Folder.empty) {
                    false
                } else {
                    ExcludeFolderDialog.show(this, folder.filePath, folder.fileName)
                    true
                }
            }

            item.itemId == android.R.id.home -> {
                findNavController().navigateUp()
                true
            }

            else -> {
                songAdapter.dataSet.onSongsMenu(this, item)
            }
        }

    override fun onMediaContentChanged() {
        super.onMediaContentChanged()
        detailViewModel.loadDetail()
    }

    /** Called by [FolderNameDialog] when this screen's folder was renamed; its path is now stale. */
    fun onFolderRenamed() {
        findNavController().navigateUp()
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
