package com.ssilencce.playback.cast

import android.app.Activity
import android.view.Menu
import androidx.media3.common.Player

/**
 * Chromecast integration boundary. The real implementation lives in the `cast` flavor source set
 * (github and playstore); the `nocast` source set provides a no-op implementation for fdroid.
 */
interface CastPlayback {
    val isAvailable: Boolean

    /** Hides or enables the Cast entry of a player menu. */
    fun setupMenu(
        menu: Menu,
        itemId: Int,
    )

    fun showDeviceChooser(activity: Activity)

    /**
     * Wraps the local player with a player that switches between local playback and a connected
     * Cast receiver. In flavors without Cast support the local player is returned unchanged.
     */
    fun createSessionPlayer(localPlayer: Player): Player

    /** Stops the embedded server and releases Cast-related listeners. */
    fun release()
}
