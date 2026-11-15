package com.ssilencce.ui.screen.audiocutter

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.ssilencce.R
import com.ssilencce.data.local.audio.clip.AudioClipFormat
import com.ssilencce.data.local.audio.clip.AudioClipNaming
import com.ssilencce.data.local.audio.clip.AudioClipRange
import com.ssilencce.data.local.audio.clip.AudioClipUnsupportedReason
import com.ssilencce.data.local.audio.clip.ClipToneKind
import com.ssilencce.extensions.media.asReadableDuration
import com.ssilencce.ui.component.compose.CollapsibleAppBarScaffold
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

private const val MIN_UI_CLIP_DURATION_MS = 1000L

@Composable
fun AudioCutterRoute(
    viewModel: AudioCutterViewModel,
    onBackClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val currentResources by rememberUpdatedState(resources)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val song = state.song
    val inspection = state.inspection
    val format = inspection?.format
    val durationMs =
        ((inspection?.durationMs?.takeIf { it > 0L } ?: song?.duration) ?: 0L)
            .coerceAtLeast(1L)

    var startMs by rememberSaveable(song?.id) { mutableLongStateOf(0L) }
    var endMs by rememberSaveable(song?.id) { mutableLongStateOf(durationMs) }
    var initializedSongId by rememberSaveable { mutableStateOf<Long?>(null) }
    var knownDurationMs by rememberSaveable { mutableLongStateOf(0L) }
    val minClipMs = minOf(MIN_UI_CLIP_DURATION_MS, durationMs)

    LaunchedEffect(song?.id, durationMs) {
        val loadedSong = song ?: return@LaunchedEffect
        if (initializedSongId != loadedSong.id) {
            initializedSongId = loadedSong.id
            knownDurationMs = durationMs
            startMs = 0L
            endMs = durationMs
        } else if (durationMs != knownDurationMs) {
            // The extractor may report a more precise duration than MediaStore.
            if (endMs >= knownDurationMs) {
                endMs = durationMs
            }
            if (startMs > endMs - minClipMs) {
                startMs = 0L
            }
            knownDurationMs = durationMs
        }
    }

    val onRangeChange: (Long, Long) -> Unit = { rawStart, rawEnd ->
        if (rawStart != startMs) {
            startMs = rawStart.coerceIn(0L, (endMs - minClipMs).coerceAtLeast(0L))
        } else if (rawEnd != endMs) {
            endMs = rawEnd.coerceIn((startMs + minClipMs).coerceAtMost(durationMs), durationMs)
        }
    }

    val previewPlayer =
        remember {
            ExoPlayer
                .Builder(context)
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    true,
                ).build()
        }
    var previewing by remember { mutableStateOf(false) }
    DisposableEffect(previewPlayer) {
        val listener =
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    previewing = isPlaying
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        previewing = false
                    }
                }
            }
        previewPlayer.addListener(listener)
        onDispose {
            previewPlayer.removeListener(listener)
            previewPlayer.release()
        }
    }
    LaunchedEffect(startMs, endMs) {
        if (previewing) {
            previewPlayer.pause()
            previewPlayer.seekTo(0)
        }
    }

    var pendingExportRange by rememberSaveable { mutableStateOf<LongArray?>(null) }
    var exportFileName by rememberSaveable { mutableStateOf("clip.m4a") }
    val exportMimeType = format?.mimeType ?: "audio/mpeg"
    val createDocumentLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument(exportMimeType),
        ) { uri: Uri? ->
            val range = pendingExportRange
            pendingExportRange = null
            if (uri != null && range != null && range.size == 2) {
                viewModel.exportToDocument(uri, range[0], range[1])
            }
        }
    val onExport: () -> Unit = {
        if (song != null && format != null) {
            exportFileName = AudioClipNaming.clipFileName(song.title, startMs, endMs, format.extension)
            pendingExportRange = longArrayOf(startMs, endMs)
            runCatching { createDocumentLauncher.launch(exportFileName) }
                .onFailure {
                    scope.launch {
                        snackbarHostState.showSnackbar(resources.getString(R.string.audio_cutter_export_failed))
                    }
                }
        }
    }

    var pendingToneName by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingToneRange by rememberSaveable { mutableStateOf<LongArray?>(null) }
    val writeSettingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val kind = pendingToneName?.let { name -> ClipToneKind.entries.firstOrNull { it.name == name } }
            val range = pendingToneRange
            pendingToneName = null
            pendingToneRange = null
            if (kind != null && range != null && range.size == 2) {
                if (Settings.System.canWrite(context)) {
                    viewModel.setAsTone(kind, range[0], range[1])
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            resources.getString(R.string.audio_cutter_write_settings_required),
                        )
                    }
                }
            }
        }
    val onSetTone: (ClipToneKind) -> Unit = { kind ->
        if (Settings.System.canWrite(context)) {
            viewModel.setAsTone(kind, startMs, endMs)
        } else {
            pendingToneName = kind.name
            pendingToneRange = longArrayOf(startMs, endMs)
            val intent =
                Intent(
                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                )
            runCatching { writeSettingsLauncher.launch(intent) }
                .onFailure {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            resources.getString(R.string.audio_cutter_write_settings_unavailable),
                        )
                    }
                }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val message =
                when (event) {
                    is AudioCutterEvent.ClipExported -> {
                        currentResources.getString(R.string.audio_cutter_exported, event.displayName)
                    }

                    is AudioCutterEvent.ToneSet -> {
                        currentResources.getString(
                            when (event.kind) {
                                ClipToneKind.Ringtone -> R.string.audio_cutter_tone_set_ringtone
                                ClipToneKind.Notification -> R.string.audio_cutter_tone_set_notification
                                ClipToneKind.Alarm -> R.string.audio_cutter_tone_set_alarm
                            },
                        )
                    }

                    is AudioCutterEvent.ExportFailed -> {
                        event.detail?.let {
                            currentResources.getString(R.string.audio_cutter_export_failed_detail, it)
                        } ?: currentResources.getString(R.string.audio_cutter_export_failed)
                    }

                    is AudioCutterEvent.ToneFailed -> {
                        event.detail?.let {
                            currentResources.getString(R.string.audio_cutter_tone_failed_detail, it)
                        } ?: currentResources.getString(R.string.audio_cutter_tone_failed)
                    }

                    AudioCutterEvent.WriteSettingsDenied -> {
                        currentResources.getString(R.string.audio_cutter_write_settings_required)
                    }

                    AudioCutterEvent.StoragePermissionDenied -> {
                        currentResources.getString(R.string.audio_cutter_storage_required)
                    }
                }
            snackbarHostState.showSnackbar(message)
        }
    }

    val onPreviewToggle: () -> Unit = {
        if (previewing) {
            previewPlayer.pause()
            previewPlayer.seekTo(0)
        } else if (song != null && format != null) {
            previewPlayer.setMediaItem(
                MediaItem
                    .Builder()
                    .setUri(song.uri)
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration
                            .Builder()
                            .setStartPositionMs(startMs)
                            .setEndPositionMs(endMs)
                            .build(),
                    ).build(),
            )
            previewPlayer.prepare()
            previewPlayer.playWhenReady = true
        }
    }

    AudioCutterContent(
        state = state,
        startMs = startMs,
        endMs = endMs,
        durationMs = durationMs,
        previewing = previewing,
        snackbarHostState = snackbarHostState,
        onRangeChange = onRangeChange,
        onPreviewToggle = onPreviewToggle,
        onExport = onExport,
        onSetTone = onSetTone,
        onBackClick = onBackClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioCutterContent(
    state: AudioCutterUiState,
    startMs: Long,
    endMs: Long,
    durationMs: Long,
    previewing: Boolean,
    snackbarHostState: SnackbarHostState,
    onRangeChange: (Long, Long) -> Unit,
    onPreviewToggle: () -> Unit,
    onExport: () -> Unit,
    onSetTone: (ClipToneKind) -> Unit,
    onBackClick: () -> Unit,
) {
    CollapsibleAppBarScaffold(
        title = stringResource(R.string.audio_cutter_title),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        onBackClick = onBackClick,
    ) { contentPadding ->
        val song = state.song
        when {
            state.loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(contentPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.missing || song == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(contentPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.audio_cutter_missing_song),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }

            else -> {
                val inspection = state.inspection
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(contentPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (song.artistName.isNotBlank()) {
                            Text(
                                text = song.artistName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        inspection?.format?.let { format ->
                            Text(
                                text =
                                    stringResource(
                                        R.string.audio_cutter_format_summary,
                                        formatLabel(format),
                                    ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (inspection?.isSupported == true) {
                        SelectionCard(
                            startMs = startMs,
                            endMs = endMs,
                            durationMs = durationMs,
                            previewing = previewing,
                            busy = state.exporting,
                            onRangeChange = onRangeChange,
                            onPreviewToggle = onPreviewToggle,
                        )
                        ExportCard(
                            exporting = state.exporting,
                            enabled = endMs - startMs >= AudioClipRange.MIN_CLIP_DURATION_MS,
                            onExport = onExport,
                        )
                        ToneCard(
                            exporting = state.exporting,
                            enabled = endMs - startMs >= AudioClipRange.MIN_CLIP_DURATION_MS,
                            onSetTone = onSetTone,
                        )
                    } else {
                        UnsupportedCard(inspectionReason = inspection?.reason, detail = inspection?.detail)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionCard(
    startMs: Long,
    endMs: Long,
    durationMs: Long,
    previewing: Boolean,
    busy: Boolean,
    onRangeChange: (Long, Long) -> Unit,
    onPreviewToggle: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.audio_cutter_selection_title),
                style = MaterialTheme.typography.titleMedium,
            )
            RangeSlider(
                value = startMs.toFloat()..endMs.toFloat(),
                onValueChange = { range ->
                    onRangeChange(range.start.roundToLong(), range.endInclusive.roundToLong())
                },
                valueRange = 0f..durationMs.toFloat(),
                enabled = !busy,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.audio_cutter_start_label, startMs.asReadableDuration()),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.audio_cutter_end_label, endMs.asReadableDuration()),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text =
                        stringResource(
                            R.string.audio_cutter_length_label,
                            (endMs - startMs).coerceAtLeast(0L).asReadableDuration(),
                        ),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = stringResource(R.string.audio_cutter_total_label, durationMs.asReadableDuration()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(
                onClick = onPreviewToggle,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter =
                        painterResource(
                            if (previewing) R.drawable.ic_pause_24dp else R.drawable.ic_play_24dp,
                        ),
                    contentDescription = null,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(
                        if (previewing) R.string.audio_cutter_stop_preview else R.string.audio_cutter_preview,
                    ),
                )
            }
            Text(
                text = stringResource(R.string.audio_cutter_trim_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExportCard(
    exporting: Boolean,
    enabled: Boolean,
    onExport: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.audio_cutter_export_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Button(
                onClick = onExport,
                enabled = enabled && !exporting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_file_export_24dp),
                    contentDescription = null,
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.audio_cutter_export))
            }
            if (exporting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = stringResource(R.string.audio_cutter_exporting),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ToneCard(
    exporting: Boolean,
    enabled: Boolean,
    onSetTone: (ClipToneKind) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.audio_cutter_set_as_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.audio_cutter_set_as_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ToneButton(
                iconRes = R.drawable.ic_phonelink_ring_24dp,
                labelRes = R.string.audio_cutter_set_ringtone,
                enabled = enabled && !exporting,
                onClick = { onSetTone(ClipToneKind.Ringtone) },
            )
            ToneButton(
                iconRes = R.drawable.ic_notifications_24dp,
                labelRes = R.string.audio_cutter_set_notification,
                enabled = enabled && !exporting,
                onClick = { onSetTone(ClipToneKind.Notification) },
            )
            ToneButton(
                iconRes = R.drawable.ic_timer_24dp,
                labelRes = R.string.audio_cutter_set_alarm,
                enabled = enabled && !exporting,
                onClick = { onSetTone(ClipToneKind.Alarm) },
            )
        }
    }
}

@Composable
private fun ToneButton(
    iconRes: Int,
    labelRes: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(painter = painterResource(iconRes), contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(labelRes))
    }
}

@Composable
private fun UnsupportedCard(
    inspectionReason: AudioClipUnsupportedReason?,
    detail: String?,
) {
    val messageRes =
        when (inspectionReason) {
            AudioClipUnsupportedReason.RequiresNewerAndroid -> R.string.audio_cutter_unsupported_api
            AudioClipUnsupportedReason.Unreadable -> R.string.audio_cutter_unsupported_unreadable
            AudioClipUnsupportedReason.Format, null -> R.string.audio_cutter_unsupported_format
        }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.audio_cutter_unsupported_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = stringResource(messageRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun formatLabel(format: AudioClipFormat): String =
    stringResource(
        when (format) {
            AudioClipFormat.M4A -> R.string.audio_cutter_format_m4a
            AudioClipFormat.MP3 -> R.string.audio_cutter_format_mp3
            AudioClipFormat.OGG -> R.string.audio_cutter_format_ogg
        },
    )
