package com.ssilencce.ui.adapters.song

import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
import com.ssilencce.R
import com.ssilencce.ui.component.base.MediaEntryViewHolder
import com.ssilencce.ui.component.views.EqMiniView

/**
 * Applies the Aurora playing-row highlight (white accent title with soft glow, accent index,
 * equalizer indicator) to a song row holder.
 */
internal fun MediaEntryViewHolder.applyPlayingStyle(playing: Boolean) {
    val accent = ContextCompat.getColor(itemView.context, R.color.aurora_nav_item_checked)
    val titleView = title
    if (titleView != null) {
        titleView.setTextColor(
            if (playing) {
                accent
            } else {
                MaterialColors.getColor(titleView, com.google.android.material.R.attr.colorOnSurface)
            },
        )
        if (playing) {
            titleView.setShadowLayer(10f, 0f, 0f, 0x4DFFFFFF.toInt())
        } else {
            titleView.paint.clearShadowLayer()
        }
    }
    val idxView = idx
    if (idxView != null) {
        idxView.setTextColor(
            if (playing) {
                accent
            } else {
                MaterialColors.getColor(
                    idxView,
                    com.google.android.material.R.attr.colorOnSurfaceVariant,
                )
            },
        )
    }
    val imageText = imageText
    if (imageText != null) {
        imageText.setTextColor(
            if (playing) {
                accent
            } else {
                MaterialColors.getColor(
                    imageText,
                    com.google.android.material.R.attr.colorOnSurfaceVariant,
                )
            },
        )
    }
    (playingIndicator as? EqMiniView)?.setPlaying(playing)
}
