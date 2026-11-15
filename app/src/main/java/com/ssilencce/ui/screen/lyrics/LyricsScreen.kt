package com.ssilencce.ui.screen.lyrics

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.ssilencce.R
import com.ssilencce.core.model.LibraryMargin
import com.ssilencce.core.model.lyrics.LyricsViewSettings
import com.ssilencce.core.model.lyrics.LyricsViewSettings.BackgroundEffect
import com.ssilencce.core.model.lyrics.LyricsViewState
import com.ssilencce.core.model.player.PlayerColorScheme
import com.ssilencce.data.model.Song
import com.ssilencce.data.model.lyrics.LyricsSourceChoice
import com.ssilencce.data.model.lyrics.LyricsSourceSelection
import com.ssilencce.data.model.lyrics.SyncedLyrics
import com.ssilencce.extensions.files.getDocumentDisplayName
import com.ssilencce.extensions.isPowerSaveMode
import com.ssilencce.extensions.media.asReadableDuration
import com.ssilencce.extensions.resolveColor
import com.ssilencce.extensions.showToast
import com.ssilencce.ui.component.compose.AnimatedEqBars
import com.ssilencce.ui.component.compose.color.extractGradientColors
import com.ssilencce.ui.component.compose.decoration.FadingEdges
import com.ssilencce.ui.component.compose.decoration.animatedGradient
import com.ssilencce.ui.component.compose.decoration.fadingEdges
import com.ssilencce.ui.component.compose.lyrics.AbRepeatLineSheet
import com.ssilencce.ui.component.compose.lyrics.LyricsSelectionBar
import com.ssilencce.ui.component.compose.lyrics.LyricsView
import com.ssilencce.ui.component.views.PlaceholderDrawable
import com.ssilencce.ui.screen.library.LibraryViewModel
import com.ssilencce.ui.screen.player.MIN_AB_REPEAT_RANGE_MS
import com.ssilencce.ui.screen.player.PlayerViewModel
import com.ssilencce.ui.theme.PlayerTheme
import com.ssilencce.util.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinActivityViewModel

sealed class LyricsUiState(
    open val id: Long,
) {
    data class Loading(
        override val id: Long,
    ) : LyricsUiState(id)

    data class Empty(
        override val id: Long,
    ) : LyricsUiState(id)

    data class Instrumental(
        override val id: Long,
    ) : LyricsUiState(id)

    data class Plain(
        override val id: Long,
        val lyrics: String,
    ) : LyricsUiState(id)

    data class Synced(
        override val id: Long,
        val syncedLyrics: SyncedLyrics,
    ) : LyricsUiState(id)
}

@Composable
private fun rememberLyricsViewState(lyrics: SyncedLyrics): LyricsViewState = remember(lyrics) { LyricsViewState(lyrics) }

private data class SelectableLyricLine(
    val id: Long,
    val text: String,
)

private fun plainLyricLines(lyrics: String): List<Pair<Long, String>> =
    lyrics.lines().mapIndexedNotNull { index, line ->
        if (line.isBlank()) null else index.toLong() to line.trim()
    }

private fun LyricsUiState.selectableLines(): List<SelectableLyricLine> =
    when (this) {
        is LyricsUiState.Synced -> {
            syncedLyrics.lines.mapNotNull { line ->
                line.content.content.takeIf { it.isNotBlank() }?.let { text ->
                    SelectableLyricLine(line.id, text)
                }
            }
        }

        is LyricsUiState.Plain -> {
            plainLyricLines(lyrics).map { (id, text) -> SelectableLyricLine(id, text) }
        }

        else -> {
            emptyList()
        }
    }

private val LongSetSaver =
    Saver<Set<Long>, LongArray>(
        save = { it.toLongArray() },
        restore = { it.toSet() },
    )

@Composable
fun rememberSmoothPlaybackPosition(
    playerPosition: Long,
    playbackSpeed: Float,
    isPlaying: Boolean,
): State<Long> {
    val position = remember { mutableLongStateOf(playerPosition) }
    LaunchedEffect(playerPosition, isPlaying) {
        val baseRealtime = SystemClock.elapsedRealtime()
        if (!isPlaying) {
            position.longValue = playerPosition
            return@LaunchedEffect
        }

        while (isActive) {
            withFrameNanos {
                val elapsed = SystemClock.elapsedRealtime() - baseRealtime
                position.longValue = playerPosition + (elapsed * playbackSpeed).toLong()
            }
        }
    }

    return position
}

@Composable
fun LyricsScreen(
    libraryViewModel: LibraryViewModel = koinActivityViewModel(),
    lyricsViewModel: LyricsViewModel = koinActivityViewModel(),
    playerViewModel: PlayerViewModel = koinActivityViewModel(),
    onEditClick: (Song) -> Unit,
    onBackgroundVisibilityChanged: (Boolean) -> Unit = {},
    contentVisible: Boolean = true,
) {
    val context = LocalContext.current
    val isPowerSaveMode = context.isPowerSaveMode()
    val animateDecorations =
        !Preferences.isAuroraAppearance || com.ssilencce.ui.component.compose.aurora
            .rememberAuroraMotionScale(contentVisible, com.ssilencce.util.ANIMATE_PLAYER_CONTROL) > 0f

    val miniPlayerMargin by libraryViewModel.getMiniPlayerMargin().observeAsState(LibraryMargin(0))

    val lyricsViewSettings by lyricsViewModel.fullLyricsViewSettings.collectAsState()
    val uiState by lyricsViewModel.lyricsUiState.collectAsState()

    val song by playerViewModel.currentSongFlow.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlayingFlow.collectAsStateWithLifecycle()

    val lyricsSourceSelection by lyricsViewModel.lyricsSourceSelection.collectAsStateWithLifecycle()

    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedLineIds by rememberSaveable(stateSaver = LongSetSaver) { mutableStateOf(emptySet<Long>()) }
    var showSourceDialog by rememberSaveable { mutableStateOf(false) }

    val selectableLines = remember(uiState) { uiState.selectableLines() }

    LaunchedEffect(uiState) {
        selectionMode = false
        selectedLineIds = emptySet()
    }

    val lyricsFilePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            val fileName = context.getDocumentDisplayName(uri)
            if (fileName != null &&
                !fileName.endsWith(".lrc", ignoreCase = true) &&
                !fileName.endsWith(".ttml", ignoreCase = true)
            ) {
                context.showToast(R.string.lyrics_source_invalid_file)
                return@rememberLauncherForActivityResult
            }
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            lyricsViewModel.setLyricsSourceSelection(
                song,
                LyricsSourceSelection(
                    source = LyricsSourceChoice.File,
                    fileUri = uri.toString(),
                    fileName = fileName ?: uri.lastPathSegment,
                ),
            )
        }

    fun toggleLine(lineId: Long) {
        val updated = if (lineId in selectedLineIds) selectedLineIds - lineId else selectedLineIds + lineId
        selectedLineIds = updated
        selectionMode = updated.isNotEmpty()
    }

    fun clearSelection() {
        selectedLineIds = emptySet()
        selectionMode = false
    }

    fun selectedLyricsText(): String? =
        selectableLines
            .filter { it.id in selectedLineIds }
            .joinToString("\n") { it.text }
            .takeIf { it.isNotBlank() }

    val lyricsLabel = stringResource(R.string.lyrics)
    val shareChooserTitle = stringResource(R.string.action_share)

    fun copySelection() {
        val text = selectedLyricsText() ?: return
        context
            .getSystemService<ClipboardManager>()
            ?.setPrimaryClip(ClipData.newPlainText(lyricsLabel, text))
        context.showToast(R.string.lyrics_copied_to_clipboard)
        clearSelection()
    }

    fun shareSelection() {
        val text = selectedLyricsText() ?: return
        val subject =
            if (song == Song.emptySong) {
                lyricsLabel
            } else {
                "${song.title} - ${song.artistName}"
            }
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_SUBJECT, subject)
            }
        runCatching {
            context.startActivity(Intent.createChooser(intent, shareChooserTitle))
        }
        clearSelection()
    }

    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    LaunchedEffect(song, lyricsViewSettings.backgroundEffect, isPowerSaveMode) {
        if (isPowerSaveMode) {
            return@LaunchedEffect
        }

        if (lyricsViewSettings.backgroundEffect == BackgroundEffect.Gradient) {
            withContext(Dispatchers.Default) {
                val result =
                    SingletonImageLoader.get(context).execute(
                        ImageRequest
                            .Builder(context)
                            .data(song)
                            .build(),
                    )
                gradientColors =
                    if (result is SuccessResult) {
                        result.image.toBitmap().extractGradientColors(
                            context.resolveColor(PlaceholderDrawable.BACKGROUND_COLOR),
                        )
                    } else {
                        emptyList()
                    }
            }
        }
    }

    val hasBackgroundEffects =
        lyricsViewSettings.backgroundEffect.isBlur ||
            (lyricsViewSettings.backgroundEffect.isGradient && gradientColors.size >= 2)
    val backgroundCallback by rememberUpdatedState(onBackgroundVisibilityChanged)
    DisposableEffect(hasBackgroundEffects) {
        backgroundCallback(hasBackgroundEffects)
        onDispose { backgroundCallback(false) }
    }

    Scaffold(
        containerColor = if (Preferences.isAuroraAppearance) Color.Transparent else MaterialTheme.colorScheme.background,
        contentWindowInsets =
            WindowInsets
                .navigationBars
                .add(WindowInsets(bottom = miniPlayerMargin.totalMarginWithNavigation)),
        topBar = {
            if (selectionMode) {
                LyricsSelectionBar(
                    selectedCount = selectedLineIds.size,
                    onClose = { clearSelection() },
                    onSelectAll = {
                        selectedLineIds = selectableLines.map { it.id }.toSet()
                    },
                    onCopy = { copySelection() },
                    onShare = { shareSelection() },
                )
            }
        },
        floatingActionButton = {
            if (!selectionMode) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (selectableLines.isNotEmpty()) {
                        FloatingActionButton(
                            onClick = {
                                selectedLineIds = emptySet()
                                selectionMode = true
                            },
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_select_all_24dp),
                                contentDescription = stringResource(R.string.lyrics_select_lines),
                            )
                        }
                    }
                    if (song != Song.emptySong) {
                        FloatingActionButton(
                            onClick = { showSourceDialog = true },
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_tune_24dp),
                                contentDescription = stringResource(R.string.lyrics_source_title),
                            )
                        }
                    }
                    FloatingActionButton(
                        onClick = { onEditClick(song) },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_edit_note_24dp),
                            contentDescription = stringResource(R.string.action_lyrics_editor),
                        )
                    }
                }
            }
        },
        modifier = Modifier.keepScreenOn(),
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = Pair(lyricsViewSettings.backgroundEffect, gradientColors),
                transitionSpec = {
                    fadeIn(tween(1000)).togetherWith(fadeOut(tween(1000)))
                },
            ) { (effect, gradientColors) ->
                when {
                    effect.isGradient && gradientColors.size >= 2 -> {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .animatedGradient(gradientColors, isPlaying && animateDecorations),
                        )
                    }

                    effect.isBlur -> {
                        val backgroundColor = Color(0xFF1A1A1A)

                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = song,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .blur(90.dp)
                                        .drawWithContent {
                                            drawContent()

                                            drawRect(
                                                brush =
                                                    Brush.radialGradient(
                                                        colors =
                                                            listOf(
                                                                Color.Transparent,
                                                                backgroundColor.copy(alpha = 0.8f),
                                                                backgroundColor,
                                                            ),
                                                        radius = size.minDimension * 0.9f,
                                                    ),
                                            )
                                        },
                            )
                        }
                    }

                    else -> {
                    }
                }
            }

            LyricsSurface(
                playerViewModel = playerViewModel,
                uiState = uiState,
                settings = lyricsViewSettings,
                contentPadding = PaddingValues(vertical = 96.dp, horizontal = 16.dp),
                fadingEdges = FadingEdges(top = 56.dp, bottom = 32.dp),
                textAlign = if (Preferences.isAuroraAppearance) TextAlign.Center else TextAlign.Start,
                isPlaying = isPlaying,
                isPowerSaveMode = isPowerSaveMode,
                hasBackgroundEffects = hasBackgroundEffects,
                animateDecorations = animateDecorations,
                selectionMode = selectionMode,
                selectedLineIds = selectedLineIds,
                onLineSelectionToggle = { line -> toggleLine(line.id) },
                onPlainLineLongClick = { lineId -> toggleLine(lineId) },
                onPlainLineSelectionToggle = { lineId -> toggleLine(lineId) },
                onSeekTo = { position ->
                    playerViewModel.seekTo(position)
                    if (lyricsViewSettings.resumeOnSeek) {
                        playerViewModel.play()
                    }
                },
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
            )
        }
    }

    if (showSourceDialog) {
        LyricsSourceDialog(
            selection = lyricsSourceSelection,
            onDismissRequest = { showSourceDialog = false },
            onSourceSelected = { choice ->
                showSourceDialog = false
                lyricsViewModel.setLyricsSourceSelection(
                    song = song,
                    selection = lyricsSourceSelection.copy(source = choice),
                )
            },
            onPickFile = {
                showSourceDialog = false
                lyricsFilePicker.launch(arrayOf("*/*"))
            },
        )
    }
}

@Composable
fun CoverLyricsScreen(
    lyricsViewModel: LyricsViewModel,
    playerViewModel: PlayerViewModel,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentVisible: Boolean = true,
) {
    val context = LocalContext.current
    val isPowerSaveMode = context.isPowerSaveMode()
    val animateDecorations =
        !Preferences.isAuroraAppearance || com.ssilencce.ui.component.compose.aurora
            .rememberAuroraMotionScale(contentVisible, com.ssilencce.util.ANIMATE_PLAYER_CONTROL) > 0f

    val isPlaying by playerViewModel.isPlayingFlow.collectAsStateWithLifecycle()

    val lyricsViewSettings by lyricsViewModel.playerLyricsViewSettings.collectAsState()
    val uiState by lyricsViewModel.lyricsUiState.collectAsState()

    val playerColorScheme by playerViewModel.colorSchemeFlow.collectAsState(
        initial = PlayerColorScheme.themeColorScheme(context),
    )

    PlayerTheme(playerColorScheme) {
        Box(modifier = modifier.fillMaxSize()) {
            LyricsSurface(
                uiState = uiState,
                playerViewModel = playerViewModel,
                settings = lyricsViewSettings,
                contentPadding = PaddingValues(vertical = 72.dp, horizontal = 12.dp),
                fadingEdges = FadingEdges(top = 72.dp, bottom = 64.dp),
                textAlign = TextAlign.Center,
                isPlaying = isPlaying,
                isPowerSaveMode = isPowerSaveMode,
                hasBackgroundEffects = false,
                animateDecorations = animateDecorations,
                onSeekTo = { position ->
                    playerViewModel.seekTo(position)
                    if (lyricsViewSettings.resumeOnSeek) {
                        playerViewModel.play()
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            FilledIconButton(
                modifier =
                    Modifier
                        .wrapContentSize()
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                colors =
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface,
                    ),
                onClick = onExpandClick,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_open_in_full_24dp),
                    contentDescription = stringResource(R.string.action_show_lyrics),
                )
            }
        }
    }
}

// Aurora needs a higher inactive-line alpha to meet contrast over bright/photographic lyrics backgrounds
// (measured inactive cores 2.47-2.63:1 at the legacy 0.4 alpha; .92 still measured 4.42-4.50 on the
// brightest normal-surface band, so .96 holds the >=4.5 target there). Legacy appearances keep 0.4.
private const val AURORA_INACTIVE_LYRIC_ALPHA = .96f

// Over unbounded bright surfaces (blur/gradient lyrics backgrounds and inline cover lyrics) even a high
// inactive alpha cannot reach 4.5:1; a bounded dark backplate restores the measured contrast floor.
private val AURORA_LYRICS_BACKPLATE = Color.Black.copy(alpha = .72f)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LyricsSurface(
    playerViewModel: PlayerViewModel,
    uiState: LyricsUiState,
    settings: LyricsViewSettings,
    contentPadding: PaddingValues,
    fadingEdges: FadingEdges,
    textAlign: TextAlign?,
    isPlaying: Boolean,
    isPowerSaveMode: Boolean,
    hasBackgroundEffects: Boolean,
    animateDecorations: Boolean,
    selectionMode: Boolean = false,
    selectedLineIds: Set<Long> = emptySet(),
    onLineSelectionToggle: (SyncedLyrics.Line) -> Unit = {},
    onPlainLineLongClick: (Long) -> Unit = {},
    onPlainLineSelectionToggle: (Long) -> Unit = {},
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val contentColor =
        when {
            hasBackgroundEffects -> {
                Color.White
            }

            // Aurora cover lyrics sit on a fixed dark backplate; a media-derived player text color can be dark.
            Preferences.isAuroraAppearance && settings.mode == LyricsViewSettings.Mode.Player -> {
                Color.White
            }

            else -> {
                when (settings.mode) {
                    LyricsViewSettings.Mode.Player -> colorScheme.onSurface
                    else -> colorScheme.secondary
                }
            }
        }
    val auroraLyricsBackplate =
        Preferences.isAuroraAppearance &&
            (settings.mode == LyricsViewSettings.Mode.Player || hasBackgroundEffects)
    Box(modifier.then(if (auroraLyricsBackplate) Modifier.background(AURORA_LYRICS_BACKPLATE) else Modifier)) {
        when (uiState) {
            is LyricsUiState.Empty -> {
                Text(
                    text = stringResource(R.string.no_lyrics_found),
                    color = contentColor,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier =
                        Modifier
                            .padding(horizontal = 16.dp)
                            .align(Alignment.Center),
                )
            }

            is LyricsUiState.Loading -> {
                if (animateDecorations) {
                    CircularWavyProgressIndicator(
                        color = contentColor,
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.lyrics_loading),
                        color = contentColor,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }

            is LyricsUiState.Instrumental -> {
                AnimatedEqBars(
                    color = contentColor,
                    isPlaying = isPlaying,
                    barCount = 5,
                    animate = animateDecorations,
                    modifier =
                        Modifier
                            .size(56.dp)
                            .align(Alignment.Center),
                )
            }

            is LyricsUiState.Plain -> {
                val lines = remember(uiState.lyrics) { uiState.lyrics.lines() }
                val scrollState = rememberLazyListState()

                val song by playerViewModel.currentSongFlow.collectAsStateWithLifecycle()
                LaunchedEffect(song) {
                    scrollState.scrollToItem(0)
                }

                LazyColumn(
                    state = scrollState,
                    contentPadding = contentPadding,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .nestedScroll(rememberNestedScrollInteropConnection())
                            .fadingEdges(fadingEdges),
                ) {
                    itemsIndexed(lines) { index, line ->
                        val lineId = index.toLong()
                        val isSelected = selectionMode && lineId in selectedLineIds
                        Text(
                            text = line.ifBlank { " " },
                            color = contentColor,
                            textAlign = textAlign,
                            style = settings.unsyncedStyle,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color =
                                            if (isSelected) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = .14f)
                                            } else {
                                                Color.Transparent
                                            },
                                        shape = MaterialTheme.shapes.small,
                                    ).combinedClickable(
                                        onClick = {
                                            if (selectionMode && line.isNotBlank()) {
                                                onPlainLineSelectionToggle(lineId)
                                            }
                                        },
                                        onLongClick = {
                                            if (line.isNotBlank()) {
                                                onPlainLineLongClick(lineId)
                                            }
                                        },
                                    ).then(
                                        if (selectionMode) {
                                            Modifier.semantics { selected = isSelected }
                                        } else {
                                            Modifier
                                        },
                                    ),
                        )
                    }
                }
            }

            is LyricsUiState.Synced -> {
                val lyricsViewState = rememberLyricsViewState(uiState.syncedLyrics)

                val playerPosition by playerViewModel.progressFlow.collectAsStateWithLifecycle()
                val playbackSpeed by playerViewModel.playbackSpeed.collectAsStateWithLifecycle()
                val abRepeat by playerViewModel.abRepeat.collectAsStateWithLifecycle()
                val abRepeatStart by playerViewModel.abRepeatStart.collectAsStateWithLifecycle()
                var abRepeatLine by remember { mutableStateOf<SyncedLyrics.Line?>(null) }
                val context = LocalContext.current

                val smoothProgress by rememberSmoothPlaybackPosition(
                    playerPosition = playerPosition,
                    playbackSpeed = playbackSpeed,
                    isPlaying = isPlaying && animateDecorations,
                )

                LaunchedEffect(playerPosition) {
                    lyricsViewState.updatePosition(smoothProgress)
                }

                LyricsView(
                    state = lyricsViewState,
                    settings = settings,
                    contentPadding = contentPadding,
                    fadingEdges = fadingEdges,
                    contentColor = contentColor,
                    isPowerSaveMode = isPowerSaveMode,
                    hasBackgroundEffects = hasBackgroundEffects,
                    inactiveTextAlpha = if (Preferences.isAuroraAppearance) AURORA_INACTIVE_LYRIC_ALPHA else .4f,
                    selectionMode = selectionMode,
                    selectedLineIds = selectedLineIds,
                    onSeekTo = onSeekTo,
                    onLineLongPress = { abRepeatLine = it },
                    onLineSelectionToggle = onLineSelectionToggle,
                )

                val offset = uiState.syncedLyrics.offset
                abRepeatLine?.let { line ->
                    val lineStart = (line.start - offset).coerceAtLeast(0L)
                    val lineEnd = (line.end - offset).coerceAtLeast(0L)
                    val stagedStart = abRepeatStart
                    val lineRangeSetMessage =
                        stringResource(
                            R.string.ab_repeat_range_set,
                            lineStart.asReadableDuration(),
                            lineEnd.asReadableDuration(),
                        )
                    val pointASetMessage =
                        stringResource(
                            R.string.ab_repeat_point_a_set,
                            lineStart.asReadableDuration(),
                        )
                    val stagedRangeSetMessage =
                        stringResource(
                            R.string.ab_repeat_range_set,
                            (stagedStart ?: lineStart).asReadableDuration(),
                            lineEnd.asReadableDuration(),
                        )
                    AbRepeatLineSheet(
                        lineText = line.content.content,
                        abRepeatActive = abRepeat != null,
                        abStartSet = stagedStart != null,
                        canSetEnd =
                            stagedStart != null &&
                                lineEnd - stagedStart >= MIN_AB_REPEAT_RANGE_MS,
                        onDismiss = { abRepeatLine = null },
                        onRepeatLine = {
                            if (lineEnd - lineStart >= MIN_AB_REPEAT_RANGE_MS) {
                                playerViewModel.setAbRepeat(lineStart, lineEnd)
                                context.showToast(lineRangeSetMessage)
                            }
                            abRepeatLine = null
                        },
                        onSetStart = {
                            playerViewModel.setAbRepeatStart(lineStart)
                            context.showToast(pointASetMessage)
                            abRepeatLine = null
                        },
                        onSetEnd = {
                            if (playerViewModel.setAbRepeatEnd(lineEnd)) {
                                context.showToast(stagedRangeSetMessage)
                            } else {
                                context.showToast(R.string.ab_repeat_too_close)
                            }
                            abRepeatLine = null
                        },
                        onClear = {
                            playerViewModel.clearAbRepeat()
                            context.showToast(R.string.ab_repeat_cleared)
                            abRepeatLine = null
                        },
                    )
                }
            }
        }
    }
}
