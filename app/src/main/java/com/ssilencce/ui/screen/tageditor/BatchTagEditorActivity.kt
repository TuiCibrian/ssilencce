package com.ssilencce.ui.screen.tageditor

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssilencce.R
import com.ssilencce.extensions.hasR
import com.ssilencce.extensions.hasT
import com.ssilencce.extensions.showToast
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
    private var writeRequestPurpose: WriteRequestPurpose? = null

    private enum class WriteRequestPurpose {
        SAVE,
        UNDO,
    }

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                viewModel.setCoverImage(this, uri)
            }
        }

    private val writeRequestLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            val purpose = writeRequestPurpose
            writeRequestPurpose = null
            when (purpose) {
                WriteRequestPurpose.SAVE -> {
                    if (result.resultCode == RESULT_OK) {
                        viewModel.write(this)
                    } else {
                        viewModel.onWritePermissionDenied()
                    }
                }

                WriteRequestPurpose.UNDO -> {
                    if (result.resultCode == RESULT_OK) {
                        viewModel.undo(this)
                    } else {
                        viewModel.onUndoUnavailable()
                        showToast(R.string.batch_tag_editor_undo_permission_denied)
                    }
                }

                null -> {
                    Unit
                }
            }
        }

    private val backCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val state = viewModel.state.value
                if (state is BatchTagEditorUiState.Writing || state is BatchTagEditorUiState.Undoing) {
                    return
                }
                if (state is BatchTagEditorUiState.Loading) {
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
                    onPickCover = ::pickCover,
                    onDeleteCover = viewModel::deleteCover,
                    onKeepCover = viewModel::keepCover,
                    onSave = ::requestSave,
                    onStop = viewModel::stopWriting,
                    onUndo = ::requestUndo,
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

    private fun pickCover() {
        imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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
        if (!requestWriteAccess(uris, WriteRequestPurpose.SAVE)) {
            viewModel.onWritePermissionDenied()
        }
    }

    private fun requestUndo() {
        if (viewModel.state.value !is BatchTagEditorUiState.Result) {
            return
        }
        val uris = viewModel.undoUris()
        if (uris.isEmpty()) {
            viewModel.onUndoUnavailable()
            showToast(R.string.batch_tag_editor_undo_unavailable)
            return
        }
        if (!hasR()) {
            viewModel.undo(this)
            return
        }
        if (!requestWriteAccess(uris, WriteRequestPurpose.UNDO)) {
            viewModel.onUndoUnavailable()
            showToast(R.string.batch_tag_editor_undo_permission_denied)
        }
    }

    private fun requestWriteAccess(
        uris: List<Uri>,
        purpose: WriteRequestPurpose,
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return false
        }
        if (writeRequestPurpose != null) {
            return true
        }
        writeRequestPurpose = purpose
        return runCatching { MediaStore.createWriteRequest(contentResolver, uris) }
            .onSuccess { pendingIntent ->
                writeRequestLauncher.launch(IntentSenderRequest.Builder(pendingIntent).build())
            }.onFailure {
                writeRequestPurpose = null
            }.isSuccess
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
