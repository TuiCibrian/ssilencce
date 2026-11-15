package com.ssilencce.playback.cast

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.Menu
import androidx.annotation.OptIn
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.RemoteCastPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.mediarouter.app.MediaRouteChooserDialog
import androidx.mediarouter.media.MediaRouteSelector
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManager
import com.google.android.gms.cast.framework.SessionManagerListener
import com.ssilencce.R
import com.ssilencce.extensions.showToast

@OptIn(UnstableApi::class)
internal class CastPlaybackImpl(
    private val context: Context,
) : CastPlayback {
    private val converter = CastMediaItemConverter(context) { ensureServer() }
    private var castContext: CastContext? = null
    private var sessionManager: SessionManager? = null
    private var sessionPlayer: Player? = null
    private var mediaServer: CastMediaServer? = null
    private var sessionListenerRegistered = false
    private var noNetworkNoticeShown = false

    private val sessionListener =
        object : SessionManagerListener<CastSession> {
            override fun onSessionStarting(session: CastSession) = Unit

            override fun onSessionStarted(
                session: CastSession,
                sessionId: String,
            ) {
                if (ensureServer() == null) {
                    runCatching { sessionManager?.endCurrentSession(true) }
                    return
                }
                val deviceName = session.castDevice?.friendlyName
                if (!deviceName.isNullOrBlank()) {
                    context.showToast(context.getString(R.string.casting_to_device, deviceName))
                }
            }

            override fun onSessionStartFailed(
                session: CastSession,
                error: Int,
            ) {
                stopServer()
            }

            override fun onSessionEnding(session: CastSession) = Unit

            override fun onSessionEnded(
                session: CastSession,
                error: Int,
            ) {
                stopServer()
            }

            override fun onSessionResuming(
                session: CastSession,
                sessionId: String,
            ) = Unit

            override fun onSessionResumed(
                session: CastSession,
                wasSuspended: Boolean,
            ) = Unit

            override fun onSessionResumeFailed(
                session: CastSession,
                error: Int,
            ) {
                stopServer()
            }

            override fun onSessionSuspended(
                session: CastSession,
                reason: Int,
            ) = Unit
        }

    override val isAvailable: Boolean
        get() = ensureCastContext() != null

    override fun setupMenu(
        menu: Menu,
        itemId: Int,
    ) {
        menu.findItem(itemId)?.isVisible = isAvailable
    }

    override fun showDeviceChooser(activity: Activity) {
        val castContext = ensureCastContext()
        if (castContext == null) {
            context.showToast(R.string.cast_unavailable)
            return
        }
        val selector =
            MediaRouteSelector
                .Builder()
                .addControlCategory(
                    CastMediaControlIntent.categoryForCast(
                        SsilencceCastOptionsProvider.RECEIVER_APPLICATION_ID,
                    ),
                ).build()
        MediaRouteChooserDialog(activity).apply { routeSelector = selector }.show()
    }

    override fun createSessionPlayer(localPlayer: Player): Player {
        sessionPlayer?.let { return it }
        val castContext = ensureCastContext() ?: return localPlayer
        return try {
            val remotePlayer =
                RemoteCastPlayer
                    .Builder(context)
                    .setMediaItemConverter(converter)
                    .build()
            val player =
                CastPlayer
                    .Builder(context)
                    .setLocalPlayer(localPlayer)
                    .setRemotePlayer(remotePlayer)
                    .build()
            sessionPlayer = player
            registerSessionListener(castContext)
            player
        } catch (e: Exception) {
            Log.e(TAG, "Cast is not available, using local playback only", e)
            localPlayer
        }
    }

    override fun release() {
        sessionPlayer?.let { player -> runCatching { player.pause() } }
        val manager = sessionManager
        runCatching { manager?.endCurrentSession(true) }
        unregisterSessionListener()
        stopServer()
        sessionPlayer = null
    }

    private fun registerSessionListener(castContext: CastContext) {
        if (sessionListenerRegistered) return
        val manager = castContext.sessionManager
        sessionManager = manager
        manager.addSessionManagerListener(sessionListener, CastSession::class.java)
        sessionListenerRegistered = true
        if (manager.currentCastSession?.isConnected == true) {
            ensureServer()
        }
    }

    private fun unregisterSessionListener() {
        if (!sessionListenerRegistered) return
        val manager = sessionManager
        sessionManager = null
        runCatching {
            manager?.removeSessionManagerListener(sessionListener, CastSession::class.java)
        }
        sessionListenerRegistered = false
    }

    private fun ensureCastContext(): CastContext? {
        castContext?.let { return it }
        return runCatching { CastContext.getSharedInstance(context) }
            .onFailure { error -> Log.w(TAG, "Google Cast is unavailable", error) }
            .getOrNull()
            ?.also { castContext = it }
    }

    @Synchronized
    private fun ensureServer(): CastMediaServer? {
        mediaServer?.let { existing -> return if (existing.start()) existing else null }
        val server = CastMediaServer(context)
        if (!server.start()) {
            if (!noNetworkNoticeShown) {
                noNetworkNoticeShown = true
                context.showToast(R.string.cast_no_network)
            }
            return null
        }
        noNetworkNoticeShown = false
        mediaServer = server
        return server
    }

    @Synchronized
    private fun stopServer() {
        mediaServer?.stop()
        mediaServer = null
    }

    companion object {
        private const val TAG = "CastPlayback"
    }
}

/** Process-wide accessor for the Cast implementation. */
object CastPlaybackProvider {
    @Volatile
    private var instance: CastPlayback? = null

    fun get(context: Context): CastPlayback =
        instance ?: synchronized(this) {
            instance ?: CastPlaybackImpl(context.applicationContext).also { instance = it }
        }
}
