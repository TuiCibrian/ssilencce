/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.ssilencce.ui.component.base

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.ssilencce.R
import com.ssilencce.core.model.LibraryMargin
import com.ssilencce.extensions.dp
import com.ssilencce.ui.component.compose.QuickSearchBar
import com.ssilencce.ui.theme.SsilencceTheme

/**
 * Hosts the shared Compose [QuickSearchBar] inside a View-based detail screen. The fragment owns
 * the durable query and filtering; this host only mirrors it, applies the mini-player list padding,
 * the navigation-aware bar margin and the 72 dp list padding used by the top-level quick-search
 * screens.
 */
class QuickSearchHost(
    private val fragment: Fragment,
    private val bar: ComposeView,
    private val recyclerView: RecyclerView,
    private val miniPlayerMargin: LiveData<LibraryMargin>,
    private val barMargin: LiveData<LibraryMargin>,
    private val navigationHeightProvider: () -> Int,
    private val extraBottomPadding: Int = 0,
    private val queryProvider: () -> String,
    private val onQueryChanged: (String) -> Unit,
) {
    private val miniPlayerView: View? by lazy { fragment.activity?.findViewById(R.id.mini_player_container) }

    val query: String
        get() = queryProvider()

    fun showsNoMatch(
        sourceCount: Int,
        visibleCount: Int,
    ): Boolean = QuickSearchPolicy.isNoMatch(query, sourceCount, visibleCount)

    fun setup() {
        bar.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        bar.isVisible = true
        bar.setContent {
            SsilencceTheme {
                var text by remember { mutableStateOf(queryProvider()) }
                QuickSearchBar(
                    query = text,
                    onQueryChange = { value ->
                        text = value
                        onQueryChanged(value)
                    },
                )
            }
        }
        miniPlayerMargin.observe(fragment.viewLifecycleOwner) {
            applyMargins()
        }
        barMargin.observe(fragment.viewLifecycleOwner) {
            applyMargins()
        }
        bar.doOnLayout {
            applyMargins()
        }
        if (queryProvider().isNotBlank()) {
            onQueryChanged(queryProvider())
        }
    }

    /**
     * Re-read both margins after layout: the navigation height can still be 0 when the activity
     * first publishes the FAB margin, and no later emission is guaranteed.
     */
    private fun applyMargins() {
        bar.post {
            val navigationHeight = navigationHeightProvider()
            val miniPlayer = miniPlayerView
            val parentView = bar.parent as? View
            val miniPlayerMarginPx =
                if (miniPlayer != null && parentView != null && miniPlayer.isShown) {
                    val miniLocation = IntArray(2)
                    val parentLocation = IntArray(2)
                    miniPlayer.getLocationOnScreen(miniLocation)
                    parentView.getLocationOnScreen(parentLocation)
                    parentView.height -
                        (miniLocation[1] - parentLocation[1]) +
                        8.dp(fragment.resources)
                } else {
                    0
                }
            bar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin =
                    maxOf(
                        barMargin.value?.totalMarginWithNavigation ?: 0,
                        miniPlayerMargin.value?.totalMarginWithNavigation ?: 0,
                        miniPlayerMarginPx,
                        navigationHeight,
                    )
            }
            recyclerView.updatePadding(
                bottom =
                    maxOf(
                        miniPlayerMargin.value?.totalMarginWithNavigation ?: 0,
                        barMargin.value?.totalMarginWithNavigation ?: 0,
                        miniPlayerMarginPx,
                        navigationHeight,
                    ) +
                        extraBottomPadding +
                        QUICK_SEARCH_EXTRA_PADDING.dp(fragment.resources),
            )
        }
    }

    private companion object {
        const val QUICK_SEARCH_EXTRA_PADDING = 72
    }
}

/** Shared saved-state key, matching the existing top-level quick-search fragments. */
object QuickSearchState {
    const val KEY = "quick_search_query"

    fun restore(savedInstanceState: Bundle?): String = savedInstanceState?.getString(KEY).orEmpty()

    fun save(
        outState: Bundle,
        query: String,
    ) {
        outState.putString(KEY, query)
    }
}
