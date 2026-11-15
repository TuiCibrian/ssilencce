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

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.content.edit
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.ssilencce.R
import com.ssilencce.core.model.GridViewType
import com.ssilencce.core.model.action.isPresent
import com.ssilencce.core.model.filesystem.FileSystemItem
import com.ssilencce.core.model.filesystem.FileSystemQuery
import com.ssilencce.core.sort.FileSortMode
import com.ssilencce.data.model.Folder
import com.ssilencce.data.model.Song
import com.ssilencce.extensions.files.getCanonicalPathSafe
import com.ssilencce.extensions.media.displayArtistName
import com.ssilencce.extensions.navigation.folderDetailArgs
import com.ssilencce.extensions.showToast
import com.ssilencce.ui.IBackConsumer
import com.ssilencce.ui.IFileCallback
import com.ssilencce.ui.adapters.FileAdapter
import com.ssilencce.ui.component.base.AbsRecyclerViewCustomGridSizeFragment
import com.ssilencce.ui.component.base.QuickSearchable
import com.ssilencce.ui.component.base.matchesQuickSearch
import com.ssilencce.ui.component.menu.onSongMenu
import com.ssilencce.ui.component.menu.onSongsMenu
import com.ssilencce.ui.screen.library.ReloadType
import com.ssilencce.util.GhostMode
import com.ssilencce.util.Preferences
import java.io.File

class FoldersListFragment :
    AbsRecyclerViewCustomGridSizeFragment<FileAdapter, GridLayoutManager>(),
    IFileCallback,
    IBackConsumer,
    QuickSearchable {
    override val titleRes: Int = R.string.folders_label
    override val isShuffleVisible: Boolean = false

    private var allFiles: List<FileSystemItem> = emptyList()
    override var quickSearchQuery: String = ""

    override val defaultGridSize: Int
        get() =
            if (isLandscape) {
                resources.getInteger(R.integer.default_list_columns_land)
            } else {
                resources.getInteger(R.integer.default_list_columns)
            }

    override val maxGridSize: Int
        get() =
            if (isLandscape) {
                resources.getInteger(R.integer.default_grid_columns_land)
            } else {
                resources.getInteger(R.integer.default_grid_columns)
            }

    override val maxGridSizeForList: Int
        get() = gridSize

    override val itemLayoutRes: Int
        get() = R.layout.item_list

    private val sortMode: FileSortMode
        get() = if (isFlatView) FileSortMode.AllFolders else FileSortMode.AllFiles

    private val fileSystem: FileSystemQuery?
        get() = libraryViewModel.getFileSystem().value

    private val isFlatView: Boolean
        get() = fileSystem?.isFlatView == true

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        libraryViewModel.getFileSystem().observe(viewLifecycleOwner) { fileSystem ->
            showFolders(fileSystem)
        }
    }

    override fun handleBackPress(): Boolean {
        val handledBackPress =
            fileSystem?.let {
                if (it.canGoUp) {
                    libraryViewModel.navigateToPath(it.parentPath)
                    true
                } else {
                    false
                }
            }
        return handledBackPress == true
    }

    override fun onResume() {
        super.onResume()
        toolbar.menu.findItem(R.id.action_hierarchy_view)?.isChecked = Preferences.hierarchyFolderView
        libraryViewModel.forceReload(ReloadType.Folders)
    }

    override fun createLayoutManager(): GridLayoutManager = GridLayoutManager(requireActivity(), gridSize)

    override fun createAdapter(): FileAdapter {
        notifyLayoutResChanged(itemLayoutRes)
        val dataSet = adapter?.files ?: ArrayList()
        return FileAdapter(
            activity = mainActivity,
            files = dataSet,
            itemLayoutRes = itemLayoutRes,
            sortMode = sortMode,
            songClickBehavior = Preferences.songClickAction,
            playOptionAlwaysVisible = Preferences.playOptionAlwaysVisible,
            callback = this,
        )
    }

    override fun fileClick(file: FileSystemItem) {
        when (file) {
            is Song -> {
                libraryViewModel
                    .listSongsFromFiles(
                        song = file,
                        files = fileSystem?.getSortedChildren(FileSortMode.AllFiles),
                    ).observe(viewLifecycleOwner) { (songs, position) ->
                        playerViewModel.openSongs(position, songs, Preferences.songClickAction)
                        if (adapter?.songClickBehavior?.isAbleToPlay == false) {
                            showToast(R.string.added_title_to_playing_queue)
                        }
                    }
            }

            else -> {
                if (Preferences.hierarchyFolderView) {
                    libraryViewModel.navigateToPath(file.filePath)
                } else if (file is Folder) {
                    findNavController().navigate(R.id.nav_folder_detail, folderDetailArgs(file))
                }
            }
        }
    }

    override fun fileMenuItemClick(
        file: FileSystemItem,
        menuItem: MenuItem,
    ): Boolean {
        val recursiveActions = Preferences.recursiveFolderActions
        return when (file) {
            is Folder -> {
                when (menuItem.itemId) {
                    R.id.action_blacklist -> {
                        ExcludeFolderDialog.show(this, file.filePath, file.fileName)
                    }

                    R.id.action_ghost_folder -> {
                        addGhostFolder(file.filePath)
                    }

                    R.id.action_folder_info -> {
                        FolderInfoDialog.show(this, file.filePath)
                    }

                    R.id.action_rename_folder -> {
                        FolderNameDialog.showRename(this, file.filePath, file.fileName)
                    }

                    R.id.action_set_as_start_directory -> {
                        val startDirectory = File(file.filePath)
                        Preferences.startDirectory = startDirectory
                        showToast(getString(R.string.start_directory_set, startDirectory.name))
                    }

                    R.id.action_scan -> {
                        libraryViewModel
                            .scanPaths(requireContext(), arrayOf(file.filePath))
                            .observe(viewLifecycleOwner) {
                                showToast(R.string.scan_finished)
                            }
                    }

                    R.id.action_equalizer -> {
                        findNavController().navigate(R.id.nav_equalizer)
                    }

                    else -> {
                        if (isFlatView) {
                            file.songs.onSongsMenu(this, menuItem)
                        } else {
                            val isRecursive = recursiveActions.isPresent(menuItem.itemId)
                            libraryViewModel
                                .songs(
                                    file.musicFiles,
                                    includeFolders = isRecursive,
                                    deepListing = isRecursive,
                                ).observe(viewLifecycleOwner) {
                                    it.onSongsMenu(this, menuItem)
                                }
                        }
                    }
                }
                true
            }

            is Song -> {
                when (menuItem.itemId) {
                    R.id.action_play -> {
                        libraryViewModel
                            .listSongsFromFiles(
                                song = file,
                                files = fileSystem?.getSortedChildren(FileSortMode.AllFiles),
                            ).observe(viewLifecycleOwner) { (songs, position) ->
                                playerViewModel.openSongs(
                                    position = position,
                                    songs = songs,
                                    behavior = Preferences.playOptionClickBehavior,
                                )
                            }
                        true
                    }

                    else -> {
                        file.onSongMenu(this, menuItem)
                    }
                }
            }

            else -> {
                false
            }
        }
    }

    override fun filesMenuItemClick(
        selection: List<FileSystemItem>,
        menuItem: MenuItem,
    ): Boolean {
        if (isFlatView) {
            libraryViewModel.songs(selection).observe(viewLifecycleOwner) {
                it.onSongsMenu(this, menuItem)
            }
        } else {
            libraryViewModel
                .songs(
                    selection,
                    includeFolders = true,
                    deepListing = Preferences.recursiveFolderActions.isPresent(menuItem.itemId),
                ).observe(viewLifecycleOwner) { songs ->
                    songs.onSongsMenu(this, menuItem)
                }
        }
        return true
    }

    private fun addGhostFolder(path: String) {
        if (GhostMode.addFolder(path)) {
            showToast(R.string.ghost_folder_added)
            libraryViewModel.reloadAll()
        } else {
            showToast(R.string.ghost_folder_already_added)
        }
    }

    override fun onCreateMenu(
        menu: Menu,
        inflater: MenuInflater,
    ) {
        super.onCreateMenu(menu, inflater)
        inflater.inflate(R.menu.menu_folders, menu)
        menu.removeItem(R.id.action_view_type)
        menu.findItem(R.id.action_hierarchy_view)?.isChecked = Preferences.hierarchyFolderView
        updateFolderActionVisibility(menu, fileSystem)
    }

    private fun updateFolderActionVisibility(
        menu: Menu,
        fileSystem: FileSystemQuery?,
    ) {
        val hasFolder =
            fileSystem != null && !fileSystem.isFlatView && !fileSystem.isStorageRoot && fileSystem.path != null
        menu.findItem(R.id.action_set_as_start_directory)?.isVisible = hasFolder
        menu.findItem(R.id.action_go_to_start_directory)?.isVisible = fileSystem?.isFlatView == false
        menu.findItem(R.id.action_create_folder)?.isVisible =
            fileSystem != null && !fileSystem.isFlatView && fileSystem.path != null
        menu.findItem(R.id.action_folder_info)?.isVisible = hasFolder
        menu.findItem(R.id.action_exclude_folder)?.isVisible = hasFolder
        menu.findItem(R.id.action_ghost_folder)?.isVisible = hasFolder
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        sortMode.createMenu(menu)
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        if (sortMode.sortItemSelected(item)) {
            libraryViewModel.forceReload(ReloadType.Folders)
            return true
        }
        when (item.itemId) {
            R.id.action_hierarchy_view -> {
                val isChecked = !item.isChecked
                Preferences.hierarchyFolderView = isChecked
                item.isChecked = isChecked
                libraryViewModel.forceReload(ReloadType.Folders)
                return true
            }

            R.id.action_set_as_start_directory -> {
                fileSystem?.let { currentFileSystem ->
                    val currentPath = currentFileSystem.path
                    if (currentPath != null) {
                        val startDirectory = File(currentPath)
                        Preferences.startDirectory = startDirectory
                        showToast(getString(R.string.start_directory_set, currentFileSystem.name))
                    }
                }
                return true
            }

            R.id.action_create_folder -> {
                fileSystem?.path?.let { currentPath ->
                    FolderNameDialog.showCreate(this, currentPath)
                }
                return true
            }

            R.id.action_folder_info -> {
                fileSystem?.path?.let { currentPath ->
                    FolderInfoDialog.show(this, currentPath)
                }
                return true
            }

            R.id.action_exclude_folder -> {
                fileSystem?.let { currentFileSystem ->
                    val currentPath = currentFileSystem.path
                    if (currentPath != null) {
                        ExcludeFolderDialog.show(
                            this,
                            currentPath,
                            currentFileSystem.name ?: currentPath,
                        )
                    }
                }
                return true
            }

            R.id.action_ghost_folder -> {
                fileSystem?.path?.let { addGhostFolder(it) }
                return true
            }

            R.id.action_go_to_start_directory -> {
                libraryViewModel.navigateToPath(Preferences.startDirectory.getCanonicalPathSafe())
                return true
            }

            else -> {
                return super.onMenuItemSelected(item)
            }
        }
    }

    override fun onMediaContentChanged() {
        super.onMediaContentChanged()
        libraryViewModel.forceReload(ReloadType.Folders)
    }

    override fun onQuickSearch(query: String) {
        quickSearchQuery = query
        applyQuickSearch()
    }

    private fun applyQuickSearch() {
        val files =
            if (quickSearchQuery.isBlank()) {
                allFiles
            } else {
                allFiles.filter { file -> file.matchesQuickSearch(quickSearchQuery) }
            }
        adapter?.submitList(files, sortMode)
    }

    private fun FileSystemItem.matchesQuickSearch(query: String): Boolean =
        when (this) {
            is Song -> listOf(title, albumName, displayArtistName(), fileName).matchesQuickSearch(query)
            else -> listOf(fileName).matchesQuickSearch(query)
        }

    private fun showFolders(fileSystem: FileSystemQuery) {
        updateFolderActionVisibility(toolbar.menu, fileSystem)
        allFiles = fileSystem.getNavigableChildren(sortMode)
        applyQuickSearch()
    }

    override fun getSavedViewType(): GridViewType = GridViewType.Normal

    override fun saveViewType(viewType: GridViewType) {}

    override fun getSavedGridSize(): Int = sharedPreferences.getInt(FOLDERS_GRID_SIZE_KEY, defaultGridSize)

    override fun saveGridSize(newGridSize: Int) {
        sharedPreferences.edit {
            putInt(FOLDERS_GRID_SIZE_KEY, newGridSize)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onGridSizeChanged(
        isLand: Boolean,
        gridColumns: Int,
    ) {
        layoutManager?.spanCount = gridColumns
        adapter?.notifyDataSetChanged()
    }

    companion object {
        private const val FOLDERS_GRID_SIZE_KEY = "folders_grid_size"
    }
}
