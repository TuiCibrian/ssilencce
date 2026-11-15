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

package com.ssilencce.ui.screen.library.albums

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.content.edit
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.ssilencce.R
import com.ssilencce.core.model.ComposerNamePolicy
import com.ssilencce.core.model.GridViewType
import com.ssilencce.core.model.sort.SortKey
import com.ssilencce.core.sort.AlbumSortMode
import com.ssilencce.core.sort.SongSortMode
import com.ssilencce.core.sort.sortedByName
import com.ssilencce.data.model.Album
import com.ssilencce.extensions.hasR
import com.ssilencce.extensions.navigation.albumDetailArgs
import com.ssilencce.extensions.navigation.asFragmentExtras
import com.ssilencce.extensions.showToast
import com.ssilencce.ui.IAlbumCallback
import com.ssilencce.ui.adapters.album.AlbumAdapter
import com.ssilencce.ui.component.base.AbsRecyclerViewCustomGridSizeFragment
import com.ssilencce.ui.component.base.QuickSearchable
import com.ssilencce.ui.component.base.matchesQuickSearch
import com.ssilencce.ui.component.menu.onAlbumMenu
import com.ssilencce.ui.component.menu.onAlbumsMenu
import com.ssilencce.ui.dialogs.ComposeChoiceDialogFragment
import com.ssilencce.ui.dialogs.showComposeChoice
import com.ssilencce.ui.screen.library.ReloadType
import com.ssilencce.util.Preferences

class AlbumListFragment :
    AbsRecyclerViewCustomGridSizeFragment<AlbumAdapter, GridLayoutManager>(),
    IAlbumCallback,
    QuickSearchable {
    override val titleRes: Int = R.string.albums_label
    override val isShuffleVisible: Boolean = true
    override val emptyMessageRes: Int
        get() = R.string.no_albums_label

    private var allAlbums: List<Album> = emptyList()
    private var composerNames: List<String> = emptyList()
    private var selectedComposerKey: String? = null
    override var quickSearchQuery: String = ""

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        selectedComposerKey = savedInstanceState?.getString(STATE_SELECTED_COMPOSER)
        parentFragmentManager.setFragmentResultListener(COMPOSER_RESULT_KEY, this) { _, result ->
            val index = result.getInt(ComposeChoiceDialogFragment.RESULT_SELECTED_INDEX, -1)
            selectedComposerKey =
                if (index <= 0) {
                    null
                } else {
                    composerNames.getOrNull(index - 1)?.let(ComposerNamePolicy::key)
                }
            applyQuickSearch()
        }
        libraryViewModel.getAlbums().observe(viewLifecycleOwner) { albums ->
            allAlbums = albums
            updateComposerNames()
            applyQuickSearch()
        }
    }

    override fun onQuickSearch(query: String) {
        quickSearchQuery = query
        applyQuickSearch()
    }

    private fun applyQuickSearch() {
        val filteredByComposer =
            selectedComposerKey?.let { composerKey ->
                allAlbums.filter { album ->
                    album.songs.any { ComposerNamePolicy.key(it.composerName) == composerKey }
                }
            } ?: allAlbums
        adapter?.dataSet =
            if (quickSearchQuery.isBlank()) {
                filteredByComposer
            } else {
                filteredByComposer.filter { album ->
                    listOf(album.name, album.artistName, album.albumArtistName).matchesQuickSearch(quickSearchQuery)
                }
            }
    }

    private fun updateComposerNames() {
        composerNames =
            allAlbums
                .asSequence()
                .flatMap { it.songs.asSequence() }
                .mapNotNull { ComposerNamePolicy.normalize(it.composerName) }
                .distinctBy { ComposerNamePolicy.key(it) }
                .toList()
                .sortedByName { it }
        if (selectedComposerKey != null && composerNames.none { ComposerNamePolicy.key(it) == selectedComposerKey }) {
            selectedComposerKey = null
        }
        activity?.invalidateOptionsMenu()
    }

    private fun showComposerChoice() {
        if (!hasR() || composerNames.isEmpty()) return
        val selectedIndex =
            selectedComposerKey
                ?.let { key -> composerNames.indexOfFirst { ComposerNamePolicy.key(it) == key } }
                ?.takeIf { it >= 0 }
                ?.plus(1)
                ?: 0
        showComposeChoice(
            title = getString(R.string.filter_by_composer),
            options = listOf(getString(R.string.all_composers)) + composerNames,
            selectedIndex = selectedIndex,
            resultKey = COMPOSER_RESULT_KEY,
        )
    }

    override fun onShuffleClicked() {
        super.onShuffleClicked()
        adapter?.dataSet?.let {
            playerViewModel
                .openShuffle(
                    providers = it,
                    mode = Preferences.albumShuffleMode,
                    sortMode = SongSortMode.Dynamic(SortKey.Track),
                ).observe(viewLifecycleOwner) { success ->
                    if (success) {
                        showToast(R.string.albums_shuffle)
                    }
                }
        }
    }

    override fun onResume() {
        super.onResume()
        libraryViewModel.forceReload(ReloadType.Albums)
    }

    override fun onPause() {
        super.onPause()
        adapter?.actionMode?.finish()
    }

    override fun createLayoutManager(): GridLayoutManager = GridLayoutManager(activity, gridSize)

    override fun createAdapter(): AlbumAdapter {
        val itemLayoutRes = itemLayoutRes
        notifyLayoutResChanged(itemLayoutRes)
        val dataSet: List<Album> = adapter?.dataSet ?: ArrayList()
        return AlbumAdapter(
            activity = mainActivity,
            dataSet = dataSet,
            itemLayoutRes = itemLayoutRes,
            sortMode = AlbumSortMode.AllAlbums,
            callback = this,
        )
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

    override fun getSavedViewType(): GridViewType =
        GridViewType.entries.firstOrNull {
            it.name == sharedPreferences.getString(VIEW_TYPE, null)
        } ?: GridViewType.Normal

    override fun saveViewType(viewType: GridViewType) {
        sharedPreferences.edit { putString(VIEW_TYPE, viewType.name) }
    }

    override fun getSavedGridSize(): Int = sharedPreferences.getInt(GRID_SIZE, defaultGridSize)

    override fun saveGridSize(newGridSize: Int) {
        sharedPreferences.edit { putInt(GRID_SIZE, newGridSize) }
    }

    override fun onGridSizeChanged(
        isLand: Boolean,
        gridColumns: Int,
    ) {
        layoutManager?.spanCount = gridColumns
        adapter?.notifyDataSetChanged()
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        menu.findItem(R.id.action_filter_composer)?.isVisible = hasR() && composerNames.isNotEmpty()
    }

    override fun onCreateMenu(
        menu: Menu,
        inflater: MenuInflater,
    ) {
        super.onCreateMenu(menu, inflater)
        menu.findItem(R.id.action_filter_composer)?.isVisible = hasR() && composerNames.isNotEmpty()
        AlbumSortMode.AllAlbums.createMenu(menu)
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_filter_composer) {
            showComposerChoice()
            return true
        }
        if (AlbumSortMode.AllAlbums.sortItemSelected(item)) {
            libraryViewModel.forceReload(ReloadType.Albums)
            return true
        }
        return super.onMenuItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_SELECTED_COMPOSER, selectedComposerKey)
        super.onSaveInstanceState(outState)
    }

    override fun onMediaContentChanged() {
        super.onMediaContentChanged()
        libraryViewModel.forceReload(ReloadType.Albums)
    }

    companion object {
        private const val VIEW_TYPE = "albums_view_type"
        private const val GRID_SIZE = "albums_grid_size"
        private const val COMPOSER_RESULT_KEY = "album_composer_filter"
        private const val STATE_SELECTED_COMPOSER = "selected_composer"
    }
}
