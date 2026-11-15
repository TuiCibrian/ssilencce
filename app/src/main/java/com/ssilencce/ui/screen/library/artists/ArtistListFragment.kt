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

package com.ssilencce.ui.screen.library.artists

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.content.edit
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.ssilencce.R
import com.ssilencce.core.model.CategoryInfo
import com.ssilencce.core.model.GridViewType
import com.ssilencce.core.model.sort.SortKey
import com.ssilencce.core.sort.ArtistSortMode
import com.ssilencce.core.sort.SongSortMode
import com.ssilencce.data.model.Artist
import com.ssilencce.extensions.navigation.artistDetailArgs
import com.ssilencce.extensions.navigation.asFragmentExtras
import com.ssilencce.extensions.showToast
import com.ssilencce.ui.IArtistCallback
import com.ssilencce.ui.adapters.artist.ArtistAdapter
import com.ssilencce.ui.component.base.AbsRecyclerViewCustomGridSizeFragment
import com.ssilencce.ui.component.base.QuickSearchable
import com.ssilencce.ui.component.base.matchesQuickSearch
import com.ssilencce.ui.component.menu.onArtistMenu
import com.ssilencce.ui.component.menu.onArtistsMenu
import com.ssilencce.ui.screen.library.ReloadType
import com.ssilencce.util.Preferences

open class ArtistListFragment :
    AbsRecyclerViewCustomGridSizeFragment<ArtistAdapter, GridLayoutManager>(),
    IArtistCallback,
    QuickSearchable {
    open val kind: ArtistListKind = ArtistListKind.Artists

    override val titleRes: Int
        get() =
            when (kind) {
                ArtistListKind.Artists -> R.string.artists_label
                ArtistListKind.AlbumArtists -> R.string.album_artists_label
                ArtistListKind.Composers -> R.string.composers_label
            }

    override val isShuffleVisible: Boolean = true

    override val emptyMessageRes: Int
        get() =
            when (kind) {
                ArtistListKind.Artists, ArtistListKind.AlbumArtists -> R.string.no_artists_label
                ArtistListKind.Composers -> R.string.no_composers_label
            }

    private var allArtists: List<Artist> = emptyList()
    override var quickSearchQuery: String = ""

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val liveData =
            when (kind) {
                ArtistListKind.Artists -> libraryViewModel.getArtists()
                ArtistListKind.AlbumArtists -> libraryViewModel.getAlbumArtists()
                ArtistListKind.Composers -> libraryViewModel.getComposers()
            }
        liveData.observe(viewLifecycleOwner) { artists ->
            allArtists = artists
            applyQuickSearch()
        }
    }

    override fun onQuickSearch(query: String) {
        quickSearchQuery = query
        applyQuickSearch()
    }

    private fun applyQuickSearch() {
        adapter?.dataSet =
            if (quickSearchQuery.isBlank()) {
                allArtists
            } else {
                allArtists.filter { artist ->
                    listOf(artist.name).matchesQuickSearch(quickSearchQuery)
                }
            }
    }

    override fun onShuffleClicked() {
        super.onShuffleClicked()
        adapter?.dataSet?.let {
            playerViewModel
                .openShuffle(
                    providers = it,
                    mode = Preferences.artistShuffleMode,
                    sortMode = SongSortMode.Dynamic(SortKey.Name),
                ).observe(viewLifecycleOwner) { success ->
                    if (success) {
                        showToast(R.string.artists_shuffle)
                    }
                }
        }
    }

    override fun onResume() {
        super.onResume()
        reloadArtists()
    }

    private fun reloadArtists() {
        libraryViewModel.forceReload(
            when (kind) {
                ArtistListKind.Artists -> ReloadType.Artists
                ArtistListKind.AlbumArtists -> ReloadType.AlbumArtists
                ArtistListKind.Composers -> ReloadType.Composers
            },
        )
    }

    private val albumArtistsTabVisible: Boolean
        get() =
            Preferences.libraryCategories.any {
                it.category == CategoryInfo.Category.AlbumArtists && it.visible
            }

    override fun createLayoutManager(): GridLayoutManager = GridLayoutManager(activity, gridSize)

    override fun createAdapter(): ArtistAdapter {
        val itemLayoutRes = itemLayoutRes
        notifyLayoutResChanged(itemLayoutRes)
        val dataSet: List<Artist> = if (adapter == null) ArrayList() else adapter!!.dataSet
        return ArtistAdapter(mainActivity, dataSet, itemLayoutRes, ArtistSortMode.AllArtists, this)
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
        super.onCreateMenu(menu, inflater)
        ArtistSortMode.AllArtists.createMenu(menu)
        if (kind == ArtistListKind.Artists && !albumArtistsTabVisible) {
            menu.add(0, R.id.action_album_artist, 0, R.string.show_album_artists).apply {
                isCheckable = true
                isChecked = Preferences.onlyAlbumArtists
            }
        }
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_album_artist && kind == ArtistListKind.Artists) {
            item.isChecked = !item.isChecked
            Preferences.onlyAlbumArtists = item.isChecked
            reloadArtists()
            return true
        }
        if (ArtistSortMode.AllArtists.sortItemSelected(item)) {
            reloadArtists()
            return true
        }
        return super.onMenuItemSelected(item)
    }

    private val viewTypeKey: String
        get() =
            when (kind) {
                ArtistListKind.Artists -> "artists_view_type"
                ArtistListKind.AlbumArtists -> "album_artists_view_type"
                ArtistListKind.Composers -> "composers_view_type"
            }

    private val gridSizeKey: String
        get() =
            when (kind) {
                ArtistListKind.Artists -> "artists_grid_size"
                ArtistListKind.AlbumArtists -> "album_artists_grid_size"
                ArtistListKind.Composers -> "composers_grid_size"
            }

    override fun getSavedViewType(): GridViewType =
        GridViewType.entries.firstOrNull {
            it.name == sharedPreferences.getString(viewTypeKey, null)
        } ?: GridViewType.Circle

    override fun saveViewType(viewType: GridViewType) {
        sharedPreferences.edit { putString(viewTypeKey, viewType.name) }
    }

    override fun getSavedGridSize(): Int = sharedPreferences.getInt(gridSizeKey, defaultGridSize)

    override fun saveGridSize(newGridSize: Int) {
        sharedPreferences.edit { putInt(gridSizeKey, newGridSize) }
    }

    override fun onGridSizeChanged(
        isLand: Boolean,
        gridColumns: Int,
    ) {
        layoutManager?.spanCount = gridColumns
        adapter?.notifyDataSetChanged()
    }

    override fun onMediaContentChanged() {
        super.onMediaContentChanged()
        reloadArtists()
    }

    override fun onPause() {
        super.onPause()
        adapter?.actionMode?.finish()
    }
}

enum class ArtistListKind {
    Artists,
    AlbumArtists,
    Composers,
}
