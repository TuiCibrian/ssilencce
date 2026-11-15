package com.ssilencce.core.appwidgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.media3.common.Player
import com.ssilencce.R
import com.ssilencce.core.appwidgets.state.PlaybackState
import com.ssilencce.core.appwidgets.state.PlaybackStateDefinition
import com.ssilencce.core.appwidgets.state.WidgetSettings
import com.ssilencce.core.appwidgets.state.WidgetSettingsStore
import com.ssilencce.ui.component.compose.color.isDark
import com.ssilencce.ui.screen.MainActivity

private const val FULL_LINE_HEIGHT_FACTOR = 1.2f
private const val FULL_SELECTION_MARGIN_DP = 1f

private fun fullLineHeightDp(
    spValue: Float,
    fontScale: Float,
): Dp = (spValue * fontScale * FULL_LINE_HEIGHT_FACTOR).dp

private fun fullNormalRequiredHeight(fontScale: Float): Dp =
    16.dp +
        fullLineHeightDp(18f, fontScale) +
        fullLineHeightDp(16f, fontScale) +
        fullLineHeightDp(14f, fontScale) * 2f +
        8.dp +
        48.dp +
        4.dp +
        56.dp +
        12.dp +
        FULL_SELECTION_MARGIN_DP.dp

class FullWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override val stateDefinition = PlaybackStateDefinition

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val appWidgetId = WidgetSettingsStore.appWidgetIdKey(id)
        val settings = WidgetSettingsStore.load(context, appWidgetId)
        val wallpaperTheme = settings.resolveWallpaperTheme(context, appWidgetId)

        provideContent {
            val playbackState = currentState<PlaybackState>()
            val currentSize = LocalSize.current
            val fontScale = context.resources.configuration.fontScale
            val widgetTheme = playbackState.widgetTheme.resolveTheme(settings.themeMode, wallpaperTheme)

            GlanceTheme(
                colors = widgetTheme.getColors(context, settings.themeMode, settings.transparencyPercent),
            ) {
                if (WidgetSettingsStore.isEmpty(playbackState.currentTitle, playbackState.artworkData)) {
                    WidgetEmptyState(context = context, cornerRadius = playbackState.imageCornerRadius?.dp?.times(2) ?: 16.dp)
                } else {
                    val scrimColor =
                        widgetTheme.getScrimColor(
                            context = context,
                            themeMode = settings.themeMode,
                            transparencyPercent = settings.transparencyPercent,
                        )
                    FullWidgetContent(
                        context = context,
                        size = currentSize,
                        playbackState = playbackState,
                        fontScale = fontScale,
                        scrimColor = scrimColor,
                        onScrimColor =
                            if (scrimColor.getColor(context).isDark()) {
                                androidx.glance.color.ColorProvider(
                                    day = androidx.compose.ui.graphics.Color.White,
                                    night = androidx.compose.ui.graphics.Color.White,
                                )
                            } else {
                                androidx.glance.color.ColorProvider(
                                    day = androidx.compose.ui.graphics.Color.Black,
                                    night = androidx.compose.ui.graphics.Color.Black,
                                )
                            },
                        showAllActions = currentSize.width >= 220.dp,
                    )
                }
            }
        }
    }

    override suspend fun onDelete(
        context: Context,
        id: GlanceId,
    ) {
        WidgetSettingsStore.delete(context, WidgetSettingsStore.appWidgetIdKey(id))
        super.onDelete(context, id)
    }

    @Composable
    private fun FullWidgetContent(
        context: Context,
        size: DpSize,
        playbackState: PlaybackState,
        fontScale: Float,
        scrimColor: ColorProvider,
        onScrimColor: ColorProvider,
        showAllActions: Boolean,
    ) {
        val compactHeight = size.height < fullNormalRequiredHeight(fontScale)
        Box(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(playbackState.imageCornerRadius?.dp?.times(2) ?: 16.dp)
                    .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.BottomCenter,
        ) {
            AlbumArtGlance(
                playbackState = playbackState,
                modifier = GlanceModifier.fillMaxSize(),
            )

            Column(
                verticalAlignment = Alignment.Bottom,
                modifier =
                    GlanceModifier
                        .fillMaxWidth()
                        .background(
                            imageProvider =
                                ImageProvider(
                                    createGradientScrim(
                                        context = context,
                                        color = scrimColor,
                                        size = size,
                                    ),
                                ),
                        ).padding(horizontal = 16.dp)
                        .padding(
                            top = if (compactHeight) 8.dp else 16.dp,
                            bottom = if (compactHeight) 8.dp else 12.dp,
                        ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = GlanceModifier.defaultWeight(),
                    ) {
                        Text(
                            text = playbackState.currentTitle.orEmpty(),
                            style =
                                TextStyle(
                                    color = onScrimColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (compactHeight) 14.sp else 18.sp,
                                ),
                            maxLines = 1,
                        )

                        Text(
                            text = playbackState.currentArtist.orEmpty(),
                            style =
                                TextStyle(
                                    color = onScrimColor,
                                    fontSize = if (compactHeight) 12.sp else 16.sp,
                                ),
                            maxLines = 1,
                        )

                        if (!playbackState.additionalInfo.isNullOrEmpty()) {
                            Text(
                                text = playbackState.additionalInfo,
                                style =
                                    TextStyle(
                                        color = onScrimColor,
                                        fontSize = if (compactHeight) 12.sp else 14.sp,
                                    ),
                                maxLines = if (compactHeight) 1 else 2,
                            )
                        }
                    }

                    if (playbackState.isForeground) {
                        Spacer(GlanceModifier.width(8.dp))

                        ControlIconGlance(
                            resId =
                                if (playbackState.isFavorite) {
                                    R.drawable.ic_favorite_24dp
                                } else {
                                    R.drawable.ic_favorite_outline_24dp
                                },
                            tint = onScrimColor,
                            contentDescription = "Toggle favorite",
                            iconSize = 28.dp,
                            modifier = GlanceModifier.clickable(toggleFavoriteAction(context)),
                        )
                    }
                }

                Spacer(GlanceModifier.height(if (compactHeight) 4.dp else 8.dp))

                FullWidgetController(context, playbackState, onScrimColor, showAllActions, compactHeight)
            }
        }
    }

    @Composable
    private fun FullWidgetController(
        context: Context,
        playbackState: PlaybackState,
        controlsColor: ColorProvider,
        showAllActions: Boolean,
        compactHeight: Boolean,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier.fillMaxWidth(),
        ) {
            if (playbackState.isForeground && showAllActions) {
                Row(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier.fillMaxWidth(),
                ) {
                    // Shuffle
                    ControlIconGlance(
                        resId =
                            if (playbackState.isShuffleMode) {
                                R.drawable.ic_shuffle_on_24dp
                            } else {
                                R.drawable.ic_shuffle_24dp
                            },
                        tint = controlsColor,
                        contentDescription = "Toggle shuffle mode",
                        iconSize = 24.dp,
                        modifier = GlanceModifier.clickable(toggleShuffleAction(context)),
                    )

                    Spacer(GlanceModifier.defaultWeight())

                    // Repeat
                    ControlIconGlance(
                        resId =
                            when (playbackState.repeatMode) {
                                Player.REPEAT_MODE_ALL -> R.drawable.ic_repeat_on_24dp
                                Player.REPEAT_MODE_ONE -> R.drawable.ic_repeat_one_on_24dp
                                else -> R.drawable.ic_repeat_24dp
                            },
                        tint = controlsColor,
                        contentDescription = "Cycle repeat mode",
                        iconSize = 24.dp,
                        modifier = GlanceModifier.clickable(cycleRepeatAction(context)),
                    )
                }

                Spacer(GlanceModifier.height(if (compactHeight) 2.dp else 4.dp))
            }

            Row(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
                modifier = GlanceModifier.fillMaxWidth(),
            ) {
                if (playbackState.isForeground) {
                    // Previous
                    ControlIconGlance(
                        resId = R.drawable.ic_previous_24dp,
                        tint = controlsColor,
                        contentDescription = "Previous",
                        iconSize = 32.dp,
                        modifier = GlanceModifier.clickable(playbackAction(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)),
                    )
                }

                Spacer(GlanceModifier.defaultWeight())

                // Play/pause
                ShapeableControlIconGlance(
                    resId =
                        if (playbackState.isPlaying) {
                            R.drawable.ic_pause_24dp
                        } else {
                            R.drawable.ic_play_24dp
                        },
                    size = if (compactHeight) 48.dp else 56.dp,
                    contentDescription = "Play/Pause",
                    onClick =
                        GlanceModifier
                            .clickable(playbackAction(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)),
                )

                Spacer(GlanceModifier.defaultWeight())

                if (playbackState.isForeground) {
                    // Next
                    ControlIconGlance(
                        resId = R.drawable.ic_next_24dp,
                        tint = controlsColor,
                        contentDescription = "Next",
                        iconSize = 32.dp,
                        modifier = GlanceModifier.clickable(playbackAction(context, KeyEvent.KEYCODE_MEDIA_NEXT)),
                    )
                }
            }
        }
    }

    fun createGradientScrim(
        context: Context,
        color: ColorProvider,
        size: DpSize,
    ): Bitmap {
        val width = size.width.toPx(context)
        val height = (size.height / 2).toPx(context)

        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        val gradient =
            LinearGradient(
                0f,
                height.toFloat(),
                0f,
                0f,
                color.getColor(context).toArgb(),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )

        paint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }
}
