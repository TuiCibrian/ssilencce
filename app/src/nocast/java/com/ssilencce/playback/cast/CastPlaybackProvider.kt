package com.ssilencce.playback.cast

import android.app.Activity
import android.content.Context
import android.view.Menu
import androidx.media3.common.Player

/** fdroid build: no Google Cast SDK, so the feature is hidden and playback stays local. */
private object NoOpCastPlayback : CastPlayback {
    override val isAvailable: Boolean = false

    override fun setupMenu(
        menu: Menu,
        itemId: Int,
    ) {
        menu.findItem(itemId)?.isVisible = false
    }

    override fun showDeviceChooser(activity: Activity) = Unit

    override fun createSessionPlayer(localPlayer: Player): Player = localPlayer

    override fun release() = Unit
}

object CastPlaybackProvider {
    fun get(context: Context): CastPlayback = NoOpCastPlayback
}
