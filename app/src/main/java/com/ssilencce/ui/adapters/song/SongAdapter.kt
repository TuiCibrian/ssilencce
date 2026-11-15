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

package com.ssilencce.ui.adapters.song

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import com.ssilencce.R
import com.ssilencce.coil.DEFAULT_SONG_IMAGE
import com.ssilencce.core.model.action.SongClickBehavior
import com.ssilencce.core.model.list.SongListDisplayPolicy
import com.ssilencce.core.model.sort.SortKey
import com.ssilencce.core.sort.SongSortMode
import com.ssilencce.data.model.Song
import com.ssilencce.extensions.isActivated
import com.ssilencce.extensions.loadPaletteImage
import com.ssilencce.extensions.media.asSectionName
import com.ssilencce.extensions.media.displayArtistName
import com.ssilencce.extensions.media.listDateModified
import com.ssilencce.extensions.media.songInfo
import com.ssilencce.extensions.resources.hide
import com.ssilencce.extensions.showToast
import com.ssilencce.extensions.utilities.buildInfoString
import com.ssilencce.ui.ISongCallback
import com.ssilencce.ui.component.base.AbsMultiSelectAdapter
import com.ssilencce.ui.component.base.MediaEntryViewHolder
import com.ssilencce.ui.component.menu.OnClickMenu
import com.ssilencce.ui.screen.player.PlayerViewModel
import com.ssilencce.util.Preferences
import me.zhanghai.android.fastscroll.PopupTextProvider
import org.koin.androidx.viewmodel.ext.android.getViewModel

@SuppressLint("NotifyDataSetChanged")
@Suppress("LeakingThis")
open class SongAdapter(
    protected val activity: FragmentActivity,
    dataSet: List<Song>,
    @LayoutRes protected val itemLayoutRes: Int = R.layout.item_list,
    protected val sortMode: SongSortMode? = null,
    protected val callback: ISongCallback? = null,
    protected val historyMode: Boolean = false,
) : AbsMultiSelectAdapter<SongAdapter.ViewHolder, Song>(activity, R.menu.menu_media_selection),
    PopupTextProvider {
    open var dataSet: List<Song> = dataSet
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    protected open fun createViewHolder(
        view: View,
        viewType: Int,
    ): ViewHolder = ViewHolder(view)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(itemLayoutRes, parent, false)
        return createViewHolder(view, viewType)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val song: Song = dataSet[position]
        val isChecked = isChecked(song)
        holder.isActivated = isChecked
        holder.menu?.isGone = isChecked
        holder.title?.text = getSongTitle(song)
        holder.text?.text = getSongText(song)
        // Check if imageContainer exists, so we can have a smooth transition without
        // CardView clipping, if it doesn't exist in current layout set transition name to image instead.
        if (holder.imageContainer != null) {
            holder.imageContainer.transitionName = song.id.toString()
        } else {
            holder.image?.transitionName = song.id.toString()
        }
        holder.loadPaletteImage(song, DEFAULT_SONG_IMAGE)
    }

    protected fun getSongTitle(song: Song): String = SongListDisplayPolicy.title(song, sortMode?.selectedKey, Preferences.songTitleMode)

    protected open fun getSongText(song: Song): String? =
        appendListDateModified(
            when (sortMode?.selectedKey) {
                SortKey.Year -> {
                    if (song.year > 0) {
                        buildInfoString(song.displayArtistName(), song.year.toString())
                    } else {
                        song.displayArtistName()
                    }
                }

                SortKey.Album -> {
                    buildInfoString(song.displayArtistName(), song.albumName)
                }

                else -> {
                    song.songInfo()
                }
            },
            song,
        )

    protected fun appendListDateModified(
        baseText: String?,
        song: Song,
    ): String? {
        if (!Preferences.showListDateModified) return baseText
        val dateModified = activity.getString(R.string.list_date_modified, song.listDateModified(activity))
        return SongListDisplayPolicy.appendDateModified(baseText, dateModified)
    }

    override fun getItemCount(): Int = dataSet.size

    override fun getItemId(position: Int): Long = dataSet[position].id

    override fun getIdentifier(position: Int): Song? = dataSet[position]

    override fun getName(item: Song): String? = getSongTitle(item)

    override fun onMultipleItemAction(
        menuItem: MenuItem,
        selection: List<Song>,
    ) {
        callback?.songsMenuItemClick(selection, menuItem)
    }

    override fun onPrepareActionModeMenu(menu: Menu) {
        menu.findItem(R.id.action_remove_from_history)?.isVisible = historyMode
    }

    override fun getPopupText(
        view: View,
        position: Int,
    ): CharSequence {
        val song = dataSet.getOrNull(position) ?: return ""
        val displayTitle = getSongTitle(song)
        return when (sortMode?.selectedKey) {
            SortKey.Album -> song.albumName.asSectionName(sortMode)
            SortKey.Artist -> song.displayArtistName().asSectionName(sortMode)
            SortKey.Name -> displayTitle.asSectionName(sortMode)
            SortKey.Year -> ""
            SortKey.FileName -> song.fileName.asSectionName(sortMode)
            SortKey.Path -> ""
            else -> displayTitle.asSectionName(sortMode)
        }
    }

    open inner class ViewHolder(
        view: View,
    ) : MediaEntryViewHolder(view) {
        protected open val song: Song
            get() = dataSet[bindingAdapterPosition]

        @get:MenuRes
        protected open val songMenuRes: Int
            get() = R.menu.menu_item_song

        protected val sharedElements: Array<Pair<View, String>>?
            get() = if (image != null && image.isVisible) arrayOf(image to image.transitionName) else null

        protected val songClickBehavior: SongClickBehavior
            get() = Preferences.songClickAction

        @CallSuper
        protected open fun onPrepareSongMenu(menu: Menu) {
            menu
                .findItem(R.id.action_play)
                ?.isVisible = !songClickBehavior.isAbleToPlay || Preferences.playOptionAlwaysVisible
            menu.findItem(R.id.action_remove_from_history)?.isVisible = historyMode
        }

        protected open fun onSongMenuItemClick(item: MenuItem): Boolean {
            if (item.itemId == R.id.action_play) {
                val playerViewModel = activity.getViewModel<PlayerViewModel>()
                val playOptionBehavior = Preferences.playOptionClickBehavior
                playerViewModel.openSongs(bindingAdapterPosition, dataSet, playOptionBehavior)
                return true
            }
            return callback?.songMenuItemClick(song, item, sharedElements) ?: false
        }

        override fun onClick(view: View) {
            if (isInQuickSelectMode) {
                toggleChecked(bindingAdapterPosition)
            } else {
                val songClickBehavior = Preferences.songClickAction
                val playerViewModel = activity.getViewModel<PlayerViewModel>()
                playerViewModel.openSongs(bindingAdapterPosition, dataSet, songClickBehavior)
                if (!songClickBehavior.isAbleToPlay) {
                    activity.showToast(R.string.added_title_to_playing_queue)
                }
            }
        }

        override fun onLongClick(view: View): Boolean = toggleChecked(bindingAdapterPosition)

        init {
            play?.hide()
            menu?.setOnClickListener(
                object : OnClickMenu() {
                    override val popupMenuRes: Int
                        get() = songMenuRes

                    override fun onPreparePopup(menu: Menu) {
                        onPrepareSongMenu(menu)
                    }

                    override fun onMenuItemClick(item: MenuItem): Boolean = onSongMenuItemClick(item)
                },
            )
        }
    }

    init {
        setHasStableIds(true)
    }
}
