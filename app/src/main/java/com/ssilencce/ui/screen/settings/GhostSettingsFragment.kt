/*
 * Copyright (c) 2026 Christians Martínez Alvarado
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

package com.ssilencce.ui.screen.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.ssilencce.R
import com.ssilencce.extensions.navigation.findActivityNavController
import com.ssilencce.extensions.showToast
import com.ssilencce.ui.dialogs.library.FolderChooserDialog
import com.ssilencce.ui.screen.library.LibraryViewModel
import com.ssilencce.ui.theme.SsilencceTheme
import com.ssilencce.util.GhostMode
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import java.io.File

class GhostSettingsFragment :
    Fragment(),
    FolderChooserDialog.FolderCallback {
    private val libraryViewModel: LibraryViewModel by activityViewModel()

    private var enabled by mutableStateOf(GhostMode.isEnabled)
    private var folders by mutableStateOf(GhostMode.folderPaths)
    private var pinEnabled by mutableStateOf(GhostMode.hasPin)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SsilencceTheme {
                    GhostSettingsScreen(
                        enabled = enabled,
                        folders = folders,
                        pinEnabled = pinEnabled,
                        onEnabledChange = ::updateEnabled,
                        onAddFolder = ::openFolderPicker,
                        onRemoveFolder = ::removeFolder,
                        onSetPin = ::setPin,
                        onRemovePin = ::removePin,
                        onOpenSearch = {
                            findActivityNavController(R.id.fragment_container)
                                .navigate(R.id.nav_ghost_search)
                        },
                    )
                }
            }
        }

    override fun onResume() {
        super.onResume()
        refreshState()
    }

    override fun onFolderSelection(
        dialog: FolderChooserDialog,
        folder: File,
    ) {
        if (GhostMode.addFolder(folder.absolutePath)) {
            requireContext().showToast(R.string.ghost_folder_added)
        } else {
            requireContext().showToast(R.string.ghost_folder_already_added)
        }
        refreshState()
        reloadLibrary()
    }

    private fun updateEnabled(value: Boolean) {
        GhostMode.setEnabled(value)
        enabled = value
        reloadLibrary()
    }

    private fun openFolderPicker() {
        FolderChooserDialog()
            .also { it.setCallback(this) }
            .show(childFragmentManager, FOLDER_CHOOSER_TAG)
    }

    private fun removeFolder(path: String) {
        if (GhostMode.removeFolder(path)) {
            refreshState()
            reloadLibrary()
        }
    }

    private fun setPin(pin: String) {
        GhostMode.setPin(pin)
        pinEnabled = GhostMode.hasPin
    }

    private fun removePin() {
        GhostMode.clearPin()
        pinEnabled = false
    }

    private fun refreshState() {
        enabled = GhostMode.isEnabled
        folders = GhostMode.folderPaths
        pinEnabled = GhostMode.hasPin
    }

    private fun reloadLibrary() {
        libraryViewModel.reloadAll()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Restore the callback when the folder picker survives a configuration change.
        (childFragmentManager.findFragmentByTag(FOLDER_CHOOSER_TAG) as? FolderChooserDialog)?.setCallback(this)
    }

    private companion object {
        const val FOLDER_CHOOSER_TAG = "GHOST_FOLDER_CHOOSER"
    }
}
