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

package com.ssilencce.ui.adapters.album

import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.view.isGone
import androidx.fragment.app.FragmentActivity
import com.ssilencce.R
import com.ssilencce.coil.DEFAULT_ALBUM_IMAGE
import com.ssilencce.core.model.shuffle.OpenShuffleMode
import com.ssilencce.core.model.sort.SortKey
import com.ssilencce.core.sort.AlbumSortMode
import com.ssilencce.core.sort.SongSortMode
import com.ssilencce.data.model.Album
import com.ssilencce.extensions.isActivated
import com.ssilencce.extensions.loadPaletteImage
import com.ssilencce.extensions.media.albumInfo
import com.ssilencce.extensions.media.asSectionName
import com.ssilencce.extensions.media.displayArtistName
import com.ssilencce.extensions.media.songCountStr
import com.ssilencce.extensions.utilities.buildInfoString
import com.ssilencce.ui.IAlbumCallback
import com.ssilencce.ui.component.base.AbsMultiSelectAdapter
import com.ssilencce.ui.component.base.MediaEntryViewHolder
import com.ssilencce.ui.component.menu.OnClickMenu
import com.ssilencce.ui.screen.player.PlayerViewModel
import me.zhanghai.android.fastscroll.PopupTextProvider
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

open class AlbumAdapter(
    activity: FragmentActivity,
    dataSet: List<Album>,
    @LayoutRes
    protected val itemLayoutRes: Int,
    protected val sortMode: AlbumSortMode? = null,
    protected val callback: IAlbumCallback? = null,
) : AbsMultiSelectAdapter<AlbumAdapter.ViewHolder, Album>(activity, R.menu.menu_media_selection),
    PopupTextProvider {
    var dataSet by Delegates.observable(dataSet) { _: KProperty<*>, _: List<Album>, _: List<Album> ->
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
        val album: Album = dataSet[position]
        val isChecked = isChecked(album)
        holder.isActivated = isChecked
        holder.menu?.isGone = isChecked
        holder.title?.text = getAlbumTitle(album)
        holder.text?.text = getAlbumText(holder, album)
        // Check if imageContainer exists, so we can have a smooth transition without
        // CardView clipping, if it doesn't exist in current layout set transition name to image instead.
        if (holder.imageContainer != null) {
            holder.imageContainer.transitionName = album.id.toString()
        } else {
            holder.image?.transitionName = album.id.toString()
        }
        holder.loadPaletteImage(album, DEFAULT_ALBUM_IMAGE)
    }

    private fun getAlbumTitle(album: Album): String = album.name

    protected open fun getAlbumText(
        holder: ViewHolder,
        album: Album,
    ): String? {
        if (sortMode?.selectedKey == SortKey.SongCount) {
            return buildInfoString(album.displayArtistName(), album.songCountStr(holder.itemView.context))
        }
        return album.albumInfo()
    }

    override fun getItemCount(): Int = dataSet.size

    override fun getItemId(position: Int): Long = dataSet[position].id

    override fun getIdentifier(position: Int): Album? = dataSet[position]

    override fun getName(item: Album): String? = item.name

    override fun onMultipleItemAction(
        menuItem: MenuItem,
        selection: List<Album>,
    ) {
        callback?.albumsMenuItemClick(selection, menuItem)
    }

    override fun getPopupText(
        view: View,
        position: Int,
    ): CharSequence {
        val album = dataSet.getOrNull(position) ?: return ""
        return when (sortMode?.selectedKey) {
            SortKey.Artist -> album.displayArtistName().asSectionName(sortMode)
            SortKey.Name -> album.name.asSectionName(sortMode)
            else -> album.name.asSectionName(sortMode)
        }
    }

    open inner class ViewHolder(
        itemView: View,
    ) : MediaEntryViewHolder(itemView) {
        protected open val album: Album
            get() = dataSet[bindingAdapterPosition]

        protected val sharedElements: Array<Pair<View, String>>?
            get() =
                if (imageContainer != null) {
                    arrayOf(imageContainer to imageContainer.transitionName)
                } else if (image != null) {
                    arrayOf(image to image.transitionName)
                } else {
                    null
                }

        override fun onClick(view: View) {
            if (isInQuickSelectMode) {
                toggleChecked(bindingAdapterPosition)
            } else {
                callback?.albumClick(album, sharedElements)
            }
        }

        override fun onLongClick(view: View): Boolean {
            toggleChecked(bindingAdapterPosition)
            return true
        }

        init {
            menu?.setOnClickListener(
                object : OnClickMenu() {
                    override val popupMenuRes: Int
                        get() = R.menu.menu_item_album

                    override fun onMenuItemClick(item: MenuItem): Boolean = callback?.albumMenuItemClick(album, item, sharedElements) ?: false
                },
            )
            play?.setOnClickListener {
                val albumSongs =
                    with(SongSortMode.AlbumSongs) {
                        album.songs.sorted()
                    }
                getViewModel<PlayerViewModel>()
                    ?.openQueue(albumSongs, shuffleMode = OpenShuffleMode.Off)
            }
        }
    }

    init {
        setHasStableIds(true)
    }
}
