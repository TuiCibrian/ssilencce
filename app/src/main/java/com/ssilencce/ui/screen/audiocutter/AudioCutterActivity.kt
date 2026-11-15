package com.ssilencce.ui.screen.audiocutter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import com.ssilencce.extensions.hasQ
import com.ssilencce.extensions.hasT
import com.ssilencce.ui.component.base.AbsBaseActivity
import com.ssilencce.ui.theme.SsilencceTheme
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class AudioCutterActivity : AbsBaseActivity() {
    private val songId: Long
        get() = intent.getLongExtra(EXTRA_SONG_ID, -1L)

    private val viewModel: AudioCutterViewModel by viewModel {
        parametersOf(songId)
    }

    private var prepared = false
    private var permissionRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (songId == -1L) {
            finish()
            return
        }
        setContent {
            SsilencceTheme {
                AudioCutterRoute(
                    viewModel = viewModel,
                    onBackClick = ::finish,
                )
            }
        }
        prepareIfPermitted()
    }

    override fun getPermissionsToRequest(): Array<String> =
        buildList {
            if (hasT()) {
                add(android.Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (!hasQ()) {
                add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()

    override fun onHasPermissionsChanged(hasPermissions: Boolean) {
        if (hasPermissions) {
            prepareIfPermitted()
        }
    }

    private fun prepareIfPermitted() {
        if (!hasPermissions()) {
            if (!permissionRequested) {
                permissionRequested = true
                requestPermissions()
            }
            return
        }
        if (prepared) return
        prepared = true
        viewModel.prepare()
    }

    companion object {
        private const val EXTRA_SONG_ID = "extra_song_id"

        fun intent(
            context: Context,
            songId: Long,
        ): Intent =
            Intent(context, AudioCutterActivity::class.java)
                .putExtra(EXTRA_SONG_ID, songId)
    }
}
