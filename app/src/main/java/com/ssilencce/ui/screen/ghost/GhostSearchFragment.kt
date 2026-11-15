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

package com.ssilencce.ui.screen.ghost

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
import androidx.navigation.fragment.findNavController
import com.ssilencce.core.model.shuffle.OpenShuffleMode
import com.ssilencce.data.model.Song
import com.ssilencce.ui.screen.player.PlayerViewModel
import com.ssilencce.ui.theme.SsilencceTheme
import com.ssilencce.util.GhostMode
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class GhostSearchFragment : Fragment() {
    private val viewModel: GhostSearchViewModel by viewModel()
    private val playerViewModel: PlayerViewModel by activityViewModel()

    private var unlocked by mutableStateOf(!GhostMode.isLocked)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SsilencceTheme {
                    GhostSearchRoute(
                        viewModel = viewModel,
                        locked = !unlocked,
                        onUnlock = { pin ->
                            val valid = GhostMode.unlock(pin)
                            if (valid) {
                                unlocked = true
                                viewModel.refresh()
                            }
                            valid
                        },
                        onBackClick = { findNavController().navigateUp() },
                        onPlay = { songs, position -> play(songs, position) },
                    )
                }
            }
        }

    override fun onResume() {
        super.onResume()
        if (GhostMode.isLocked) {
            unlocked = false
        } else {
            if (!unlocked) unlocked = true
            viewModel.refresh()
        }
    }

    private fun play(
        songs: List<Song>,
        position: Int,
    ) {
        if (songs.isEmpty()) return
        playerViewModel.openQueue(
            queue = songs,
            position = position.coerceIn(songs.indices),
            shuffleMode = OpenShuffleMode.Off,
        )
    }
}
