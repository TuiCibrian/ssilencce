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

package com.ssilencce.ui.screen.lockscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.session.MediaController
import com.ssilencce.MediaControllerOwner
import com.ssilencce.data.local.repository.GenreRepository
import com.ssilencce.data.local.repository.PlaylistRepository
import com.ssilencce.ui.component.base.AbsBaseActivity
import com.ssilencce.ui.screen.lyrics.LyricsViewModel
import com.ssilencce.ui.screen.player.PlayerViewModel
import com.ssilencce.ui.theme.SsilencceTheme
import com.ssilencce.util.GhostMode
import com.ssilencce.util.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Opt-in full-screen player shown over the keyguard. It owns its own media controller so it can
 * be started by the screen-off receiver without waking the main activity.
 */
class LockScreenPlayerActivity :
    AbsBaseActivity(),
    MediaController.Listener {
    private val mediaControllerOwner by lazy { MediaControllerOwner(this, this) }
    private val playerViewModel: PlayerViewModel by viewModel()
    private val lyricsViewModel: LyricsViewModel by viewModel()
    private val genreRepository: GenreRepository by inject()
    private val playlistRepository: PlaylistRepository by inject()

    private var preview = false

    private val userPresentReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent?,
            ) {
                if (intent?.action != Intent.ACTION_USER_PRESENT) return
                if (!preview && Preferences.lockScreenSkipWhenUnlocked) finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preview = intent.getBooleanExtra(EXTRA_PREVIEW, false)
        applyLockScreenWindowFlags()
        if (!LockScreenLaunchPolicy.shouldShow(this, preview)) {
            finish()
            return
        }

        setContent {
            SsilencceTheme(
                darkTheme = true,
                dynamicColor = false,
            ) {
                LockScreenPlayerScreen(
                    playerViewModel = playerViewModel,
                    lyricsViewModel = lyricsViewModel,
                    preview = preview,
                    onDismiss = ::finish,
                    onOpenArtist = { song ->
                        if (LockScreenEntityLauncher.openArtist(this, song)) finish()
                    },
                    onOpenFolder = { folderPath ->
                        if (LockScreenEntityLauncher.openFolder(this, folderPath)) finish()
                    },
                    onOpenGenre = { song ->
                        openGenre(song.genreName.orEmpty())
                    },
                    onOpenPlaylist = { openFavoritesPlaylist() },
                    ghostModeEnabled = GhostMode.isEnabled,
                    onOpenGhostSearch = {
                        if (LockScreenEntityLauncher.openGhostSearch(this)) finish()
                    },
                )
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mediaControllerOwner.isConnected.collect { event ->
                    if (event.getContentIfNotConsumed() == true) {
                        mediaControllerOwner.get()?.let { playerViewModel.setMediaController(it) }
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                playerViewModel.currentSongFlow.collect { song ->
                    if (song.id >= 0) lyricsViewModel.updateSong(song)
                }
            }
        }

        mediaControllerOwner.attachTo(this)
    }

    override fun onStart() {
        super.onStart()
        isShowing = true
        ContextCompat.registerReceiver(
            this,
            userPresentReceiver,
            IntentFilter(Intent.ACTION_USER_PRESENT),
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    override fun onResume() {
        super.onResume()
        if (!preview &&
            Preferences.lockScreenSkipWhenUnlocked &&
            (getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager)?.isKeyguardLocked != true
        ) {
            finish()
        }
    }

    override fun onStop() {
        super.onStop()
        isShowing = false
        runCatching { unregisterReceiver(userPresentReceiver) }
    }

    override fun onDisconnected(controller: MediaController) {
        playerViewModel.setMediaController(null)
    }

    private fun openGenre(genreName: String) {
        if (genreName.isBlank()) return
        val song = playerViewModel.currentSong
        if (song.id < 0) return
        lifecycleScope.launch {
            val genre = runCatching { withContext(Dispatchers.IO) { genreRepository.genre(song) } }.getOrNull()
            if (genre != null &&
                genre.id >= 0 &&
                LockScreenEntityLauncher.openGenre(this@LockScreenPlayerActivity, genre)
            ) {
                finish()
            }
        }
    }

    private fun openFavoritesPlaylist() {
        lifecycleScope.launch {
            val playlist =
                runCatching { withContext(Dispatchers.IO) { playlistRepository.favoritePlaylist() } }.getOrNull()
            if (playlist != null &&
                playlist.playListId > 0 &&
                LockScreenEntityLauncher.openPlaylist(this@LockScreenPlayerActivity, playlist.playListId)
            ) {
                finish()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun applyLockScreenWindowFlags() {
        window.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        // The lock screen player is a full-screen experience: hide the bars but let a swipe
        // reveal them temporarily.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    companion object {
        const val EXTRA_PREVIEW = "lock_screen_preview"

        @Volatile
        var isShowing: Boolean = false
            private set

        fun launch(
            context: Context,
            preview: Boolean,
        ) {
            if (isShowing && !preview) return
            val intent =
                Intent(context, LockScreenPlayerActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra(EXTRA_PREVIEW, preview)
            runCatching { context.startActivity(intent) }
        }
    }
}
