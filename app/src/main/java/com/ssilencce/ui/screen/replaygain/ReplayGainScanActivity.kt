package com.ssilencce.ui.screen.replaygain

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ssilencce.R
import com.ssilencce.extensions.hasR
import com.ssilencce.extensions.hasT
import com.ssilencce.ui.component.base.AbsBaseActivity
import com.ssilencce.ui.theme.SsilencceTheme
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class ReplayGainScanActivity : AbsBaseActivity() {
    private val viewModel: ReplayGainScanViewModel by viewModel()

    private var permissionRequested = false
    private var prepared = false
    private var writeRequested = false

    private val writeRequestLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                viewModel.start()
            } else {
                viewModel.onWritePermissionDenied()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            SsilencceTheme {
                val state by viewModel.state.collectAsState()
                ReplayGainScanScreen(
                    state = state,
                    onStart = viewModel::start,
                    onCancel = viewModel::cancel,
                    onClose = ::finish,
                )
            }
        }
        observeReadyState()
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

    override fun onDestroy() {
        if (isFinishing) {
            viewModel.cancel()
        }
        super.onDestroy()
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
        viewModel.prepare(intent.getLongArrayExtra(EXTRA_SONG_IDS) ?: longArrayOf())
    }

    private fun observeReadyState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    if (state is ReplayGainScanUiState.Ready && !writeRequested) {
                        writeRequested = true
                        requestWriteAccess(state)
                    }
                }
            }
        }
    }

    private fun requestWriteAccess(state: ReplayGainScanUiState.Ready) {
        if (!hasR()) {
            viewModel.start()
            return
        }
        val uris = state.songs.map { it.uri }
        if (uris.isEmpty()) {
            viewModel.onWritePermissionDenied()
            return
        }
        runCatching {
            MediaStore.createWriteRequest(contentResolver, uris)
        }.onSuccess { pendingIntent ->
            writeRequestLauncher.launch(IntentSenderRequest.Builder(pendingIntent).build())
        }.onFailure {
            viewModel.onWritePermissionDenied()
        }
    }

    companion object {
        const val EXTRA_SONG_IDS = "extra_song_ids"

        fun intent(
            context: android.content.Context,
            songIds: LongArray,
        ): Intent =
            Intent(context, ReplayGainScanActivity::class.java)
                .putExtra(EXTRA_SONG_IDS, songIds.distinct().toLongArray())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReplayGainScanScreen(
    state: ReplayGainScanUiState,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onClose: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.replay_gain_scan_title)) })
        },
    ) { contentPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (state) {
                ReplayGainScanUiState.Loading -> {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.replay_gain_scan_loading))
                }

                is ReplayGainScanUiState.Ready -> {
                    Text(
                        text = stringResource(R.string.replay_gain_scan_summary, state.songs.size),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.replay_gain_scan_album_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = onStart,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.replay_gain_scan_start))
                    }
                    TextButton(onClick = onClose) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }

                is ReplayGainScanUiState.Running -> {
                    Text(
                        text =
                            stringResource(
                                R.string.replay_gain_scan_progress,
                                state.completed,
                                state.total,
                            ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    state.currentTitle?.let { title ->
                        Text(
                            text = stringResource(R.string.replay_gain_scan_analyzing, title),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (state.failed > 0) {
                        Text(
                            text = stringResource(R.string.replay_gain_scan_failed_count, state.failed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    TextButton(onClick = onCancel) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }

                is ReplayGainScanUiState.Finished -> {
                    Text(
                        text =
                            if (state.cancelled) {
                                stringResource(R.string.replay_gain_scan_cancelled, state.written)
                            } else {
                                stringResource(
                                    R.string.replay_gain_scan_finished,
                                    state.written,
                                    state.failed,
                                )
                            },
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                    if (state.failures.isNotEmpty()) {
                        Text(
                            text =
                                stringResource(
                                    R.string.replay_gain_scan_failed_count,
                                    state.failures.size,
                                ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        state.failures.forEach { failure ->
                            Text(
                                text =
                                    stringResource(
                                        R.string.replay_gain_scan_failure_detail,
                                        failure.title,
                                        failure.reason,
                                    ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    Button(
                        onClick = onClose,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.close_action))
                    }
                }

                ReplayGainScanUiState.Empty -> {
                    Text(
                        text = stringResource(R.string.replay_gain_scan_empty),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Button(
                        onClick = onClose,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.close_action))
                    }
                }

                is ReplayGainScanUiState.Error -> {
                    Text(
                        text =
                            when (state.error) {
                                ReplayGainScanError.PermissionDenied -> {
                                    stringResource(R.string.replay_gain_scan_permission_denied)
                                }

                                ReplayGainScanError.Unexpected -> {
                                    stringResource(
                                        R.string.replay_gain_scan_error,
                                        state.detail.orEmpty(),
                                    )
                                }
                            },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = onClose,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.close_action))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
