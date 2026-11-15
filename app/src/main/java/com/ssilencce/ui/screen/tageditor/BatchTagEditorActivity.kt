package com.ssilencce.ui.screen.tageditor

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssilencce.extensions.hasR
import com.ssilencce.extensions.hasT
import com.ssilencce.ui.component.base.AbsBaseActivity
import com.ssilencce.ui.theme.SsilencceTheme
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class BatchTagEditorActivity : AbsBaseActivity() {
    private val viewModel: BatchTagEditorViewModel by viewModel {
        parametersOf(intent.getLongArrayExtra(EXTRA_SONG_IDS)?.toList().orEmpty())
    }

    private var permissionRequested = false
    private var prepared = false
    private var writePermissionRequested = false

    private val writeRequestLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            writePermissionRequested = false
            if (result.resultCode == RESULT_OK) {
                viewModel.write(this)
            } else {
                viewModel.onWritePermissionDenied()
            }
        }

    private val backCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.state.value is BatchTagEditorUiState.Writing) {
                    return
                }
                if (viewModel.state.value is BatchTagEditorUiState.Loading) {
                    viewModel.cancelPrepare()
                }
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SsilencceTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                BatchTagEditorScreen(
                    state = state,
                    onOperationChange = viewModel::setOperation,
                    onPerFileOperation = viewModel::setPerFileOperation,
                    onOpenPerFile = viewModel::openPerFile,
                    onClosePerFile = viewModel::closePerFile,
                    onSave = ::requestSave,
                    onStop = viewModel::stopWriting,
                    onClose = ::finish,
                )
            }
        }
        onBackPressedDispatcher.addCallback(this, backCallback)
        prepareIfPermitted()
    }

    override fun getPermissionsToRequest(): Array<String> =
        buildList {
            if (hasT()) add(READ_MEDIA_AUDIO) else add(READ_EXTERNAL_STORAGE)
            if (!hasR()) add(WRITE_EXTERNAL_STORAGE)
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
        if (prepared) {
            return
        }
        prepared = true
        viewModel.prepare()
    }

    private fun requestSave() {
        val state = viewModel.state.value as? BatchTagEditorUiState.Ready ?: return
        if (!state.canSave) {
            return
        }
        val uris = viewModel.writableUris()
        if (uris.isEmpty()) {
            viewModel.onWritePermissionDenied()
            return
        }
        if (!hasR()) {
            viewModel.write(this)
            return
        }
        if (writePermissionRequested) {
            return
        }
        writePermissionRequested = true
        runCatching { MediaStore.createWriteRequest(contentResolver, uris) }
            .onSuccess { pendingIntent ->
                writeRequestLauncher.launch(IntentSenderRequest.Builder(pendingIntent).build())
            }.onFailure {
                writePermissionRequested = false
                viewModel.onWritePermissionDenied()
            }
    }

    companion object {
        const val EXTRA_SONG_IDS = "extra_song_ids"

        fun intent(
            context: Context,
            songIds: LongArray,
        ): Intent =
            Intent(context, BatchTagEditorActivity::class.java)
                .putExtra(EXTRA_SONG_IDS, songIds.distinct().toLongArray())
    }
}
