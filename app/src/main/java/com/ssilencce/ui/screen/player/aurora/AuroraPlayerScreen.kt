package com.ssilencce.ui.screen.player.aurora

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.preference.PreferenceManager
import com.ssilencce.R
import com.ssilencce.core.model.action.NowPlayingAction
import com.ssilencce.core.model.theme.NowPlayingScreen
import com.ssilencce.data.model.Song
import com.ssilencce.extensions.media.asReadableDuration
import com.ssilencce.extensions.observeKeyAsState
import com.ssilencce.ui.component.compose.aurora.rememberAuroraMotionScale
import com.ssilencce.ui.component.views.MorphicIconButton
import com.ssilencce.ui.component.views.MusicSlider
import com.ssilencce.ui.screen.player.PlayerGesturesController.GestureType
import com.ssilencce.ui.screen.player.PlayerViewModel
import com.ssilencce.ui.theme.AuroraDesign
import com.ssilencce.util.ANIMATE_PLAYER_CONTROL
import com.ssilencce.util.DISPLAY_ALBUM_TITLE
import com.ssilencce.util.DISPLAY_EXTRA_INFO
import com.ssilencce.util.ENABLE_SCROLLING_TEXT
import com.ssilencce.util.NOW_PLAYING_EXTRA_INFO
import com.ssilencce.util.PREFER_REMAINING_TIME
import com.ssilencce.util.Preferences
import com.ssilencce.util.SQUIGGLY_SEEK_BAR
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.roundToInt
import android.graphics.Canvas as AndroidCanvas

@Composable
internal fun AuroraPlayerScreen(
    playerViewModel: PlayerViewModel,
    coverView: View,
    favorite: Boolean,
    lyricsVisible: Boolean,
    shown: Boolean,
    artistName: (Song) -> CharSequence,
    nextSongInfo: (Song) -> String,
    onAction: (NowPlayingAction) -> Unit,
    onCollapse: () -> Unit,
    onEqualizer: () -> Unit,
    onDetails: () -> Unit,
    onOverflowItem: (Int) -> Unit,
    onShuffleOptions: () -> Unit,
    onExtraInfoOptions: () -> Unit,
    onGesture: (GestureType) -> Unit,
    navigationHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val song by playerViewModel.currentSongFlow.collectAsStateWithLifecycle()
    val nextSong by playerViewModel.nextSongFlow.collectAsStateWithLifecycle()
    val playing by playerViewModel.isPlayingFlow.collectAsStateWithLifecycle()
    val repeat by playerViewModel.repeatModeFlow.collectAsStateWithLifecycle()
    val shuffle by playerViewModel.shuffleModeFlow.collectAsStateWithLifecycle()
    val extraInfo by playerViewModel.extraInfoFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val preferences = remember(context) { PreferenceManager.getDefaultSharedPreferences(context) }
    val displayExtraInfo by preferences.observeKeyAsState(DISPLAY_EXTRA_INFO, Preferences.displayExtraInfo)
    val extraInfoFields by preferences.observeKeyAsState(NOW_PLAYING_EXTRA_INFO, "")
    val displayAlbumTitle by preferences.observeKeyAsState(DISPLAY_ALBUM_TITLE, Preferences.displayAlbumTitle)
    val marquee = rememberAuroraMotionScale(shown, ENABLE_SCROLLING_TEXT, preferenceDefault = false) > 0f
    LaunchedEffect(displayExtraInfo, extraInfoFields) { playerViewModel.generateExtraInfo() }
    val style = Preferences.nowPlayingScreen
    val fullCover = style == NowPlayingScreen.FullCover
    val compact = style == NowPlayingScreen.Peek || style == NowPlayingScreen.Plain
    val peek = style == NowPlayingScreen.Peek
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    val gestureCallback by rememberUpdatedState(onGesture)
    BoxWithConstraints(
        modifier
            .then(if (peek) Modifier.fillMaxWidth().heightIn(max = screenHeight) else Modifier.fillMaxSize())
            .playerFlingGestures(Preferences.isSwipeAnywhere, Preferences.isSwipeUpQueue) { gestureCallback(it) }
            .windowInsetsPadding(if (peek) WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom) else WindowInsets.safeDrawing)
            .padding(bottom = navigationHeight),
    ) {
        val landscape = maxWidth > maxHeight
        val controlsMaxHeight = maxHeight * if (compact) .72f else .58f
        val coverCorner = if (style == NowPlayingScreen.M3 || style == NowPlayingScreen.Expressive) 24.dp else 6.dp
        val coverShape = RoundedCornerShape(coverCorner)
        val cover: @Composable (Modifier) -> Unit = { coverModifier ->
            AndroidView(
                factory = { coverView },
                modifier =
                    coverModifier
                        .clip(coverShape)
                        .border(1.dp, AuroraDesign.border, coverShape),
            )
        }
        val metadata: @Composable () -> Unit = {
            val align = if (style == NowPlayingScreen.Plain || peek) TextAlign.Start else TextAlign.Center
            Text(
                song.title,
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .then(if (marquee) Modifier.basicMarquee() else Modifier)
                    .semantics { heading() },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = align,
            )
            Text(artistName(song).toString(), Modifier.fillMaxWidth().padding(top = 4.dp).then(if (marquee) Modifier.basicMarquee() else Modifier), fontSize = 15.sp, color = AuroraDesign.muted, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = align)
            if (displayAlbumTitle && song.albumName.isNotBlank()) {
                Text(song.albumName, Modifier.fillMaxWidth().padding(top = 3.dp).then(if (marquee) Modifier.basicMarquee() else Modifier), fontSize = 13.5.sp, color = AuroraDesign.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = align)
            }
        }
        val header: @Composable () -> Unit = {
            Row(Modifier.fillMaxWidth().height(48.dp).background(if (fullCover) AuroraDesign.glassStrong else Color.Transparent), verticalAlignment = Alignment.CenterVertically) {
                PlayerAction(R.drawable.ic_keyboard_arrow_down_24dp, R.string.aurora_collapse_player, onCollapse)
                Text(stringResource(R.string.now_playing), Modifier.weight(1f), fontSize = 13.sp, color = AuroraDesign.muted)
                PlayerAction(if (lyricsVisible) R.drawable.ic_lyrics_24dp else R.drawable.ic_lyrics_outline_24dp, if (lyricsVisible) R.string.action_hide_lyrics else R.string.action_show_lyrics, { onAction(NowPlayingAction.Lyrics) })
            }
        }
        val controls: @Composable () -> Unit = {
            Column(
                Modifier.fillMaxWidth().then(if (peek) Modifier else Modifier.verticalScroll(rememberScrollState())).padding(horizontal = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (!peek) metadata()
                if (displayExtraInfo && !extraInfo.isNullOrBlank()) {
                    Text(
                        extraInfo.orEmpty(),
                        Modifier
                            .heightIn(min = 48.dp)
                            .combinedClickable(onClick = onExtraInfoOptions, onLongClick = onExtraInfoOptions)
                            .padding(top = 8.dp)
                            .then(if (marquee) Modifier.basicMarquee() else Modifier),
                        fontSize = 12.sp,
                        color = AuroraDesign.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    PlayerAction(if (favorite) R.drawable.ic_favorite_24dp else R.drawable.ic_favorite_outline_24dp, if (favorite) R.string.action_remove_from_favorites else R.string.action_add_to_favorites, { onAction(NowPlayingAction.ToggleFavoriteState) }, tint = if (favorite) MaterialTheme.colorScheme.error else AuroraDesign.muted)
                    PlayerAction(R.drawable.ic_info_24dp, R.string.action_details, onDetails)
                    PlayerAction(R.drawable.ic_equalizer_24dp, R.string.equalizer_label, onEqualizer)
                    PlayerAction(R.drawable.ic_queue_music_24dp, R.string.playing_queue_label, { onAction(NowPlayingAction.OpenPlayQueue) })
                    Box {
                        var moreExpanded by remember { mutableStateOf(false) }
                        PlayerAction(R.drawable.ic_more_horiz_24dp, R.string.action_more, { moreExpanded = true })
                        DropdownMenu(
                            expanded = moreExpanded,
                            onDismissRequest = { moreExpanded = false },
                            shape = RoundedCornerShape(16.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            val onItemClick: (Int) -> Unit = {
                                moreExpanded = false
                                onOverflowItem(it)
                            }
                            val overflowItem: @Composable (Int, Int, Int?, (Int) -> Unit) -> Unit =
                                { id, label, icon, onClick ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(label)) },
                                        leadingIcon =
                                            if (icon != null) {
                                                val iconRes = icon
                                                { Icon(painterResource(iconRes), null) }
                                            } else {
                                                null
                                            },
                                        onClick = { onClick(id) },
                                    )
                                }
                            overflowItem(R.id.action_favorite, if (favorite) R.string.action_remove_from_favorites else R.string.action_add_to_favorites, if (favorite) R.drawable.ic_favorite_24dp else R.drawable.ic_favorite_outline_24dp, onItemClick)
                            overflowItem(R.id.action_playing_queue, R.string.playing_queue_label, R.drawable.ic_queue_music_24dp, onItemClick)
                            overflowItem(R.id.action_sleep_timer, R.string.action_sleep_timer, R.drawable.ic_timer_24dp, onItemClick)
                            overflowItem(R.id.action_show_lyrics, if (lyricsVisible) R.string.action_hide_lyrics else R.string.action_show_lyrics, if (lyricsVisible) R.drawable.ic_lyrics_24dp else R.drawable.ic_lyrics_outline_24dp, onItemClick)
                            overflowItem(R.id.action_equalizer, R.string.equalizer_label, R.drawable.ic_equalizer_24dp, onItemClick)
                            overflowItem(R.id.action_sound_settings, R.string.sound_settings, R.drawable.ic_volume_up_24dp, onItemClick)
                            overflowItem(R.id.action_add_to_playlist, R.string.action_add_to_playlist, R.drawable.ic_playlist_add_24dp, onItemClick)
                            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = AuroraDesign.border)
                            overflowItem(R.id.action_go_to_album, R.string.action_go_to_album, null, onItemClick)
                            overflowItem(R.id.action_go_to_artist, R.string.action_go_to_artist, null, onItemClick)
                            overflowItem(R.id.action_go_to_genre, R.string.action_go_to_genre, null, onItemClick)
                            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = AuroraDesign.border)
                            overflowItem(R.id.action_tag_editor, R.string.action_tag_editor, R.drawable.ic_edit_24dp, onItemClick)
                            overflowItem(R.id.action_web_search, R.string.web_search, R.drawable.ic_language_24dp, onItemClick)
                            overflowItem(R.id.action_share, R.string.action_share, R.drawable.ic_share_24dp, onItemClick)
                            overflowItem(R.id.action_delete_from_device, R.string.action_delete_from_device, R.drawable.ic_delete_24dp, onItemClick)
                            overflowItem(R.id.action_details, R.string.action_details, R.drawable.ic_info_24dp, onItemClick)
                        }
                    }
                    PlayerAction(
                        if (repeat == Player.REPEAT_MODE_ONE) R.drawable.ic_repeat_one_24dp else R.drawable.ic_repeat_24dp,
                        R.string.action_cycle_repeat,
                        playerViewModel::cycleRepeatMode,
                        tint = if (repeat == Player.REPEAT_MODE_OFF) AuroraDesign.muted else Color.White,
                        state =
                            stringResource(
                                when (repeat) {
                                    Player.REPEAT_MODE_ONE -> R.string.repeat_mode_one
                                    Player.REPEAT_MODE_ALL -> R.string.repeat_mode_all
                                    else -> R.string.repeat_mode_off
                                },
                            ),
                    )
                    PlayerAction(R.drawable.ic_shuffle_24dp, R.string.action_toggle_shuffle, playerViewModel::toggleShuffleMode, onLongClick = onShuffleOptions, tint = if (shuffle) Color.White else AuroraDesign.muted, state = stringResource(if (shuffle) R.string.shuffle_on else R.string.shuffle_off), longLabel = R.string.shuffle_mode_header)
                }
                PlayerProgress(playerViewModel, shown, playing)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    PlayerAction(R.drawable.ic_volume_up_24dp, R.string.sound_settings, { onAction(NowPlayingAction.SoundSettings) })
                    PlayerAction(if (style == NowPlayingScreen.M3) R.drawable.ic_previous_m3_24dp else R.drawable.ic_previous_24dp, R.string.action_previous, playerViewModel::seekToPrevious, playerViewModel::seekBack, iconSize = 28, repeatOnHold = true, longLabel = R.string.action_seek_backward)
                    PlayPauseButton(style, playing, shown, playerViewModel::togglePlayPause)
                    PlayerAction(if (style == NowPlayingScreen.M3) R.drawable.ic_next_m3_24dp else R.drawable.ic_next_24dp, R.string.action_next, playerViewModel::seekToNext, playerViewModel::seekForward, iconSize = 28, repeatOnHold = true, longLabel = R.string.action_seek_forward)
                    PlayerAction(R.drawable.ic_equalizer_24dp, R.string.equalizer_label, onEqualizer)
                }
                if (Preferences.isShowNextSong) {
                    Text(
                        nextSongInfo(nextSong),
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .combinedClickable(onClick = { onAction(NowPlayingAction.OpenPlayQueue) })
                            .padding(vertical = 12.dp)
                            .then(if (marquee) Modifier.basicMarquee() else Modifier),
                        fontSize = 12.sp,
                        color = AuroraDesign.muted,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (peek) {
            Column(Modifier.fillMaxWidth().background(AuroraDesign.glassStrong.copy(alpha = .98f)).verticalScroll(rememberScrollState())) {
                header()
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    cover(Modifier.size(80.dp))
                    Column(Modifier.weight(1f)) { metadata() }
                }
                controls()
            }
        } else {
            if (fullCover) {
                // The square cover now sits centered in its stage; the reference
                // backdrop stays behind header, cover and controls.
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .1f), Color.Black.copy(alpha = .9f)))))
            }
            Column(Modifier.fillMaxSize()) {
                header()
                if (landscape) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        BoxWithConstraints(Modifier.weight(1f).padding(10.dp), contentAlignment = Alignment.Center) {
                            val square = minOf(maxWidth, maxHeight)
                            Box(Modifier.size(square).coverHalo(coverCorner)) { cover(Modifier.fillMaxSize()) }
                        }
                        Box(Modifier.weight(1f)) { controls() }
                    }
                } else {
                    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal = if (compact) 46.dp else 10.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                        val square = minOf(maxWidth, maxHeight, if (compact) 280.dp else 360.dp)
                        Box(Modifier.size(square).coverHalo(coverCorner)) { cover(Modifier.fillMaxSize()) }
                        if (style == NowPlayingScreen.Gradient) Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .48f)))))
                    }
                    Box(Modifier.heightIn(max = controlsMaxHeight).background(if (fullCover) AuroraDesign.glass else Color.Transparent)) { controls() }
                }
            }
        }
    }
}

@Composable
private fun PlayPauseButton(
    style: NowPlayingScreen,
    playing: Boolean,
    shown: Boolean,
    onClick: () -> Unit,
) {
    val description = stringResource(if (playing) R.string.action_pause else R.string.action_play)
    val motionScale = rememberAuroraMotionScale(shown, ANIMATE_PLAYER_CONTROL)
    val click by rememberUpdatedState(onClick)
    if (style == NowPlayingScreen.Expressive) {
        Box(Modifier.size(72.dp).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = description }) {
            AndroidView(factory = { context ->
                MorphicIconButton(context).apply {
                    backgroundTintList = ColorStateList.valueOf(android.graphics.Color.WHITE)
                    setIconTintColor(android.graphics.Color.BLACK)
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                    isFocusable = false
                    setOnClickListener { click() }
                }
            }, update = { button ->
                button.contentDescription = description
                button.morphDuration = if (motionScale > 0f) (450 * motionScale).toLong() else 0
                val icon =
                    if (motionScale > 0f) {
                        if (playing) R.drawable.avd_play else R.drawable.avd_pause
                    } else {
                        if (playing) R.drawable.ic_pause_24dp else R.drawable.ic_play_24dp
                    }
                if (button.tag != icon) {
                    button.setIconResource(icon, motionScale > 0f)
                    button.morphToShape(if (playing && !Preferences.circularPlayButton) MorphicIconButton.SHAPE_COOKIE_9 else MorphicIconButton.SHAPE_CIRCLE)
                    button.tag = icon
                }
                button.isRotating = playing && motionScale > 0f && !Preferences.circularPlayButton
            }, onRelease = {
                it.isRotating = false
                it.setOnClickListener(null)
            }, modifier = Modifier.fillMaxSize())
        }
    } else {
        val shape = if (style == NowPlayingScreen.M3 && !Preferences.circularPlayButton) RoundedCornerShape(20.dp) else CircleShape
        FilledIconButton(onClick = onClick, modifier = Modifier.size(62.dp), shape = shape, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = AuroraDesign.background)) {
            Icon(painterResource(if (playing) R.drawable.ic_pause_24dp else R.drawable.ic_play_24dp), description, Modifier.size(42.dp))
        }
    }
}

// Mirrors .np-cover's white box-shadow halo (42px at .16 plus 90px at .05 in
// the reference). A CSS blur radius is a Gaussian sigma of half its value, so
// both layers are rendered once per size as a low-resolution blurred
// silhouette, composited and punched so the glow stays outside the frame
// (a page's own transparent margins stay clean). The result is one cached
// bitmap and a single drawImage per frame: no bands, no per-frame blur or
// allocation, and bounded memory.
private const val HALO_RASTER_SCALE = .4f
private const val HALO_RASTER_MAX_SIDE = 768
private const val HALO_MARGIN_DP = 96f

private class CoverHalo(
    val image: ImageBitmap,
    val marginPx: Float,
)

private fun Modifier.coverHalo(cornerRadius: Dp): Modifier =
    drawWithCache {
        val glow = buildCoverHalo(size, cornerRadius.toPx(), density)
        onDrawBehind {
            glow?.let {
                drawImage(
                    image = it.image,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(it.image.width, it.image.height),
                    dstOffset = IntOffset(-it.marginPx.roundToInt(), -it.marginPx.roundToInt()),
                    dstSize =
                        IntSize(
                            (size.width + it.marginPx * 2f).roundToInt(),
                            (size.height + it.marginPx * 2f).roundToInt(),
                        ),
                    filterQuality = FilterQuality.Low,
                )
            }
        }
    }

private fun buildCoverHalo(
    size: Size,
    cornerRadius: Float,
    density: Float,
): CoverHalo? {
    if (size.width < 1f || size.height < 1f) return null
    val marginPx = HALO_MARGIN_DP * density
    val fullWidth = size.width + marginPx * 2f
    val fullHeight = size.height + marginPx * 2f
    val scale = minOf(HALO_RASTER_SCALE, HALO_RASTER_MAX_SIDE / fullWidth, HALO_RASTER_MAX_SIDE / fullHeight)
    if (scale <= 0f) return null
    val bitmap =
        Bitmap.createBitmap(
            (fullWidth * scale).roundToInt().coerceAtLeast(1),
            (fullHeight * scale).roundToInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
    val canvas = AndroidCanvas(bitmap)
    val rect = RectF(marginPx * scale, marginPx * scale, (marginPx + size.width) * scale, (marginPx + size.height) * scale)
    val corner = cornerRadius * scale
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = android.graphics.Color.WHITE
    drawHaloLayer(canvas, paint, rect, corner, alpha = .16f, sigmaPx = 21f * density * scale)
    drawHaloLayer(canvas, paint, rect, corner, alpha = .05f, sigmaPx = 45f * density * scale)
    paint.alpha = 255
    paint.maskFilter = null
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    canvas.drawRoundRect(rect, corner, corner, paint)
    return CoverHalo(bitmap.asImageBitmap(), marginPx)
}

private fun drawHaloLayer(
    canvas: AndroidCanvas,
    paint: Paint,
    rect: RectF,
    cornerRadius: Float,
    alpha: Float,
    sigmaPx: Float,
) {
    paint.alpha = (255f * alpha).roundToInt()
    // Skia maps a BlurMaskFilter radius to sigma = radius * .57735 + .5. The
    // raster keeps an identity matrix, so this sigma is in bitmap pixels and
    // the upscaled draw yields the intended dp blur on screen.
    paint.maskFilter = BlurMaskFilter(((sigmaPx - .5f) / .57735f).coerceAtLeast(.1f), BlurMaskFilter.Blur.NORMAL)
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
}

private fun Modifier.playerFlingGestures(
    anywhere: Boolean,
    queue: Boolean,
    onGesture: (GestureType) -> Unit,
): Modifier =
    pointerInput(anywhere, queue) {
        if (!anywhere && !queue) return@pointerInput
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
            var handledByChild = false
            var multiplePointers = false
            var last = down
            do {
                val event = awaitPointerEvent(PointerEventPass.Final)
                multiplePointers = multiplePointers || event.changes.size > 1
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (change.position != change.previousPosition && change.isConsumed) handledByChild = true
                last = change
            } while (last.pressed)
            val distance = last.position - down.position
            val elapsed = (last.uptimeMillis - down.uptimeMillis).coerceAtLeast(1)
            val threshold = viewConfiguration.touchSlop * 4
            if (!handledByChild && !multiplePointers && distance.getDistance() / elapsed * 1000 > 300.dp.toPx()) {
                when {
                    anywhere && abs(distance.x) > threshold && abs(distance.x) > abs(distance.y) -> {
                        onGesture(GestureType.Fling(if (distance.x < 0) GestureType.Fling.DIRECTION_LEFT else GestureType.Fling.DIRECTION_RIGHT))
                    }

                    queue && distance.y < -threshold && abs(distance.y) > abs(distance.x) -> {
                        onGesture(GestureType.Fling(GestureType.Fling.DIRECTION_UP))
                    }
                }
            }
        }
    }

@Composable
private fun PlayerAction(
    icon: Int,
    label: Int,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    tint: Color = AuroraDesign.muted,
    iconSize: Int = 21,
    state: String? = null,
    repeatOnHold: Boolean = false,
    longLabel: Int? = null,
) {
    val description = stringResource(label)
    val longDescription = longLabel?.let { stringResource(it) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    var holding by remember { mutableStateOf(false) }
    val holdAction by rememberUpdatedState(onLongClick)
    LaunchedEffect(pressed, holding) {
        if (!pressed) holding = false
        if (repeatOnHold && pressed && holding) {
            while (isActive) {
                delay(250)
                holdAction?.invoke()
            }
        }
    }
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .combinedClickable(
                interactionSource = interaction,
                role = Role.Button,
                onClick = onClick,
                onLongClickLabel = longDescription,
                onLongClick =
                    onLongClick?.let {
                        {
                            it()
                            holding = repeatOnHold && pressed
                        }
                    },
            ).semantics {
                contentDescription = description
                if (state != null) stateDescription = state
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), null, Modifier.size(iconSize.dp), tint = tint)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerProgress(
    viewModel: PlayerViewModel,
    shown: Boolean,
    playing: Boolean,
) {
    val position = if (shown) viewModel.progressFlow.collectAsStateWithLifecycle().value else viewModel.progress
    val duration by viewModel.durationFlow.collectAsStateWithLifecycle()
    val motionScale = rememberAuroraMotionScale(shown, ANIMATE_PLAYER_CONTROL)
    val context = LocalContext.current
    val preferences = remember(context) { PreferenceManager.getDefaultSharedPreferences(context) }
    val remaining by preferences.observeKeyAsState(PREFER_REMAINING_TIME, Preferences.preferRemainingTime)
    val squiggly by preferences.observeKeyAsState(SQUIGGLY_SEEK_BAR, Preferences.squigglySeekBar)
    var seek by remember { mutableStateOf<Float?>(null) }
    val total = duration.coerceAtLeast(1L)
    val progress = seek ?: position.coerceAtLeast(0L).toFloat().coerceAtMost(total.toFloat())
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(progress.toLong().asReadableDuration(), fontSize = 12.sp, color = AuroraDesign.muted)
        if (squiggly) {
            AndroidView(factory = {
                MusicSlider(it).apply {
                    currentColor = android.graphics.Color.WHITE
                    setListener(
                        object : MusicSlider.Listener {
                            override fun onProgressChanged(
                                slider: MusicSlider,
                                progress: Int,
                                fromUser: Boolean,
                            ) {
                                if (fromUser) seek = progress.toFloat()
                            }

                            override fun onStartTrackingTouch(slider: MusicSlider) {}

                            override fun onStopTrackingTouch(slider: MusicSlider) {
                                viewModel.seekTo(slider.value.toLong())
                                seek = null
                            }
                        },
                    )
                }
            }, update = { slider ->
                slider.animateSquigglyProgress = motionScale > 0f && playing
                slider.valueTo = total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                if (!slider.isTrackingTouch) slider.value = progress.toInt()
            }, onRelease = {
                it.setListener(null)
                it.animateSquigglyProgress = false
            }, modifier = Modifier.weight(1f).height(48.dp))
        } else {
            Slider(
                value = progress,
                onValueChange = { seek = it },
                onValueChangeFinished = {
                    viewModel.seekTo(progress.toLong())
                    seek = null
                },
                valueRange = 0f..total.toFloat(),
                enabled = duration > 0,
                modifier = Modifier.weight(1f),
                thumb = { Box(Modifier.size(17.dp).background(Color.White, CircleShape)) },
                track = { state ->
                    Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                        drawLine(Color(0xFF181818), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 4.dp.toPx(), StrokeCap.Round)
                        drawLine(Color.White, Offset(0f, size.height / 2), Offset(size.width * (state.value / total.toFloat()).coerceIn(0f, 1f), size.height / 2), 4.dp.toPx(), StrokeCap.Round)
                    }
                },
            )
        }
        val timeDescription = stringResource(if (remaining) R.string.aurora_remaining_time else R.string.aurora_total_time)
        val toggleDescription = stringResource(if (remaining) R.string.aurora_total_time else R.string.aurora_remaining_time)
        val timeText = (if (remaining) "−" else "") + (if (remaining) total - progress.toLong() else duration).coerceAtLeast(0L).asReadableDuration()
        Text(
            timeText,
            Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .combinedClickable(onClick = { Preferences.preferRemainingTime = !remaining }, onClickLabel = toggleDescription)
                .semantics { contentDescription = "$timeDescription $timeText" }
                .wrapContentHeight(Alignment.CenterVertically),
            fontSize = 12.sp,
            color = AuroraDesign.muted,
        )
    }
}
