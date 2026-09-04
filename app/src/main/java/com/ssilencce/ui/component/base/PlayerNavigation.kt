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

package com.ssilencce.ui.component.base

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.os.postDelayed
import androidx.navigation.findNavController
import androidx.navigation.navOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.ssilencce.R
import com.ssilencce.data.model.Genre
import com.ssilencce.data.model.Song
import com.ssilencce.extensions.currentFragment
import com.ssilencce.extensions.navigation.albumDetailArgs
import com.ssilencce.extensions.navigation.artistDetailArgs
import com.ssilencce.extensions.navigation.genreDetailArgs
import com.ssilencce.ui.screen.MainActivity

fun goToArtist(
    activity: Activity,
    song: Song,
) {
    goToDestination(
        activity,
        R.id.nav_artist_detail,
        artistDetailArgs(song),
        removeTransition = true,
        singleTop = false,
    )
}

fun goToAlbum(
    activity: Activity,
    song: Song,
) {
    goToDestination(
        activity,
        R.id.nav_album_detail,
        albumDetailArgs(song.albumId),
        removeTransition = true,
        singleTop = false,
    )
}

fun goToGenre(
    activity: Activity,
    genre: Genre,
) {
    goToDestination(
        activity,
        R.id.nav_genre_detail,
        genreDetailArgs(genre),
        singleTop = false,
    )
}

fun goToDestination(
    activity: Activity,
    destinationId: Int,
    args: Bundle? = null,
    removeTransition: Boolean = false,
    singleTop: Boolean = true,
) {
    if (activity !is MainActivity) return
    activity.apply {
        if (removeTransition) {
            // Remove exit transition of current fragment, so
            // it doesn't exit with a weird transition
            currentFragment(R.id.fragment_container)?.exitTransition = null
        }

        // Hide Bottom Bar First, else Bottom Sheet doesn't collapse fully
        setBottomNavVisibility(false)
        if (getBottomSheetBehavior().state == BottomSheetBehavior.STATE_EXPANDED) {
            collapsePanel()
        }

        Handler(Looper.getMainLooper()).postDelayed(250) {
            val navOptions =
                when {
                    singleTop -> navOptions { launchSingleTop = true }
                    else -> null
                }
            findNavController(R.id.fragment_container)
                .navigate(destinationId, args, navOptions)
        }
    }
}
