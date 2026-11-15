package com.ssilencce.ui.screen.miniplayer

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssilencce.core.model.theme.AppTheme
import com.ssilencce.data.model.Song
import com.ssilencce.ui.screen.MainActivity
import com.ssilencce.ui.theme.SsilencceTheme
import com.ssilencce.util.Preferences
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

/**
 * Dialog-style lightweight player for external audio VIEW intents and library previews.
 * Playback is local to this popup so the main queue is preserved until "open in app".
 */
class MiniPlayerActivity : AppCompatActivity() {
    private val viewModel: MiniPlayerViewModel by viewModel {
        parametersOf(MiniPlayerRequest.fromIntent(intent))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC
        setFinishOnTouchOutside(true)
        setContent {
            val themeMode = remember { Preferences.getThemeMode(Preferences.generalTheme) }
            val darkTheme =
                when (themeMode) {
                    AppTheme.Mode.Light -> false
                    AppTheme.Mode.Dark, AppTheme.Mode.Black -> true
                    AppTheme.Mode.FollowSystem -> isSystemInDarkTheme()
                }
            SsilencceTheme(darkTheme = darkTheme) {
                val state by viewModel.state.collectAsStateWithLifecycle()
                MiniPlayerScreen(
                    state = state,
                    onPlayPause = viewModel::togglePlayPause,
                    onSeek = viewModel::seekTo,
                    onOpenInApp = ::openInApp,
                    onClose = ::finish,
                )
            }
        }
        onBackPressedDispatcher.addCallback(this) { finish() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.replaceRequest(MiniPlayerRequest.fromIntent(intent))
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations && !isFinishing) {
            finish()
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            viewModel.close()
        }
        super.onDestroy()
    }

    private fun openInApp() {
        val uri = viewModel.prepareOpenInApp() ?: return
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = uri
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                )
            },
        )
        finish()
    }

    companion object {
        fun previewIntent(
            context: Context,
            song: Song,
        ): Intent = Intent(context, MiniPlayerActivity::class.java).putExtra(MiniPlayerRequest.EXTRA_SONG, song)
    }
}
