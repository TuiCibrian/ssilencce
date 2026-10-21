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

package com.ssilencce.ui.adapters.artist

import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.fragment.app.FragmentActivity
import com.ssilencce.R
import com.ssilencce.coil.DEFAULT_ARTIST_IMAGE
import com.ssilencce.core.sort.ArtistSortMode
import com.ssilencce.core.sort.SongSortMode
import com.ssilencce.data.model.Artist
import com.ssilencce.data.model.ArtistPlayStat
import com.ssilencce.extensions.isActivated
import com.ssilencce.extensions.loadPaletteImage
import com.ssilencce.extensions.media.artistInfo
import com.ssilencce.extensions.media.asSectionName
import com.ssilencce.extensions.media.displayArtistName
import com.ssilencce.extensions.media.displayName
import com.ssilencce.extensions.utilities.buildInfoString
import com.ssilencce.ui.IArtistCallback
import com.ssilencce.ui.component.base.AbsMultiSelectAdapter
import com.ssilencce.ui.component.base.MediaEntryViewHolder
import com.ssilencce.ui.component.menu.OnClickMenu
import com.ssilencce.ui.screen.player.PlayerViewModel
import com.ssilencce.util.Preferences
import me.zhanghai.android.fastscroll.PopupTextProvider
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

class ArtistAdapter constructor(
    activity: FragmentActivity,
    dataSet: List<Artist>,
    private val itemLayoutRes: Int,
    private val sortMode: ArtistSortMode? = null,
    private val callback: IArtistCallback? = null,
) : AbsMultiSelectAdapter<ArtistAdapter.ViewHolder, Artist>(activity, R.menu.menu_media_selection),
    PopupTextProvider {
    private var albumArtistsOnly = false

    var dataSet by Delegates.observable(dataSet) { _: KProperty<*>, _: List<Artist>, _: List<Artist> ->
        albumArtistsOnly = Preferences.onlyAlbumArtists
        notifyDataSetChanged()
    }

    private var playStats: List<ArtistPlayStat>? = null
    private val playStatIds = mutableMapOf<ArtistPlayStat.Key, Long>()
    private var nextPlayStatId = 0L

    fun submitPlayStats(stats: List<ArtistPlayStat>) {
        // Allocate by the full snapshot key, not a name hash or a possibly missing current artist id.
        stats.forEach { stat -> playStatIds.getOrPut(stat.key) { nextPlayStatId++ } }
        playStats = stats
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(itemLayoutRes, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val artist = getIdentifier(position)
        val stats = playStats?.get(position)
        val isChecked = artist?.let { isChecked(it) } ?: false
        holder.isActivated = isChecked
        holder.menu?.isGone = isChecked || artist == null
        holder.itemView.isClickable = artist != null
        holder.itemView.isLongClickable = artist != null
        if (stats != null) holder.play?.isGone = artist == null
        holder.title?.text = stats?.artistName?.displayArtistName() ?: artist?.displayName()
        holder.text?.text = getArtistText(holder, artist, stats)
        val transitionName =
            when {
                artist == null -> "artist-stat-${getItemId(position)}"
                stats == null && albumArtistsOnly -> artist.name
                else -> artist.id.toString()
            }
        if (holder.imageContainer != null) {
            holder.imageContainer.transitionName = transitionName
        } else {
            holder.image?.transitionName = transitionName
        }
        holder.loadPaletteImage(artist ?: Artist.empty, DEFAULT_ARTIST_IMAGE)
    }

    private fun getArtistText(
        holder: ViewHolder,
        artist: Artist?,
        stats: ArtistPlayStat?,
    ): String =
        if (stats != null) {
            buildInfoString(
                artist?.artistInfo(holder.itemView.context),
                holder.itemView.context.getString(R.string.artist_play_stats_x, stats.playCount, stats.percentage),
            )
        } else {
            artist?.artistInfo(holder.itemView.context).orEmpty()
        }

    override fun getItemCount(): Int = playStats?.size ?: dataSet.size

    override fun getItemId(position: Int): Long = playStats?.let { playStatIds.getValue(it[position].key) } ?: dataSet[position].id

    override fun getIdentifier(position: Int): Artist? = if (playStats != null) playStats?.getOrNull(position)?.artist else dataSet.getOrNull(position)

    override fun getName(item: Artist): String? = item.displayName()

    override fun onMultipleItemAction(
        menuItem: MenuItem,
        selection: List<Artist>,
    ) {
        callback?.artistsMenuItemClick(selection, menuItem)
    }

    override fun getPopupText(
        view: View,
        position: Int,
    ): CharSequence =
        (playStats?.getOrNull(position)?.artistName?.displayArtistName() ?: dataSet.getOrNull(position)?.displayName())
            ?.asSectionName(sortMode) ?: ""

    open inner class ViewHolder(
        itemView: View,
    ) : MediaEntryViewHolder(itemView) {
        protected val artist: Artist?
            get() = getIdentifier(bindingAdapterPosition)

        protected val sharedElements: Array<Pair<View, String>>?
            get() =
                if (imageContainer != null) {
                    arrayOf(imageContainer to imageContainer.transitionName)
                } else if (image != null) {
                    arrayOf(image to image.transitionName)
                } else {
                    null
                }

        protected open fun onArtistMenuItemClick(item: MenuItem): Boolean {
            val artist = artist ?: return false
            return callback?.artistMenuItemClick(artist, item, sharedElements) ?: false
        }

        override fun onClick(view: View) {
            val artist = artist ?: return
            if (isInQuickSelectMode) {
                toggleChecked(bindingAdapterPosition)
            } else {
                callback?.artistClick(artist, sharedElements)
            }
        }

        override fun onLongClick(view: View): Boolean = artist != null && toggleChecked(bindingAdapterPosition)

        init {
            menu?.setOnClickListener(
                object : OnClickMenu() {
                    override val popupMenuRes: Int
                        get() = R.menu.menu_item_artist

                    override fun onMenuItemClick(item: MenuItem): Boolean = onArtistMenuItemClick(item)
                },
            )
            play?.setOnClickListener {
                val artist = artist ?: return@setOnClickListener
                val artistSongs =
                    with(SongSortMode.ArtistSongs) {
                        artist.songs.sorted()
                    }
                getViewModel<PlayerViewModel>()?.openQueue(artistSongs)
            }
        }
    }

    init {
        setHasStableIds(true)
    }
}
