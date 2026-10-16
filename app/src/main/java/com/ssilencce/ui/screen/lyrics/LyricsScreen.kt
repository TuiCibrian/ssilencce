package com.ssilencce.ui.screen.lyrics

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import com.ssilencce.data.model.lyrics.SyncedLyrics
import com.ssilencce.extensions.isPowerSaveMode
import com.ssilencce.extensions.resolveColor
import com.ssilencce.ui.component.compose.AnimatedEqBars
import com.ssilencce.ui.component.compose.color.extractGradientColors
import com.ssilencce.ui.component.compose.decoration.FadingEdges
import com.ssilencce.ui.component.compose.decoration.animatedGradient
import com.ssilencce.ui.component.compose.decoration.fadingEdges
import com.ssilencce.ui.component.compose.lyrics.LyricsView
import com.ssilencce.ui.component.views.PlaceholderDrawable
import com.ssilencce.ui.screen.library.LibraryViewModel
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
        floatingActionButton = {
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
                val scrollState = rememberScrollState()

                val song by playerViewModel.currentSongFlow.collectAsStateWithLifecycle()
                LaunchedEffect(song) {
                    scrollState.scrollTo(0)
                }

                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .nestedScroll(rememberNestedScrollInteropConnection())
                            .fadingEdges(fadingEdges)
                            .verticalScroll(scrollState)
                            .padding(contentPadding),
                ) {
                    Text(
                        text = uiState.lyrics,
                        color = contentColor,
                        textAlign = textAlign,
                        style = settings.unsyncedStyle,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            is LyricsUiState.Synced -> {
                val lyricsViewState = rememberLyricsViewState(uiState.syncedLyrics)

                val playerPosition by playerViewModel.progressFlow.collectAsStateWithLifecycle()
                val playbackSpeed by playerViewModel.playbackSpeed.collectAsStateWithLifecycle()

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
                    onSeekTo = onSeekTo,
                )
            }
        }
    }
}
