package com.ssilencce.core.appwidgets

import android.content.Context
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
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
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.ssilencce.R
import com.ssilencce.core.appwidgets.state.PlaybackState
import com.ssilencce.core.appwidgets.state.PlaybackStateDefinition
import com.ssilencce.core.appwidgets.state.WidgetSettings
import com.ssilencce.core.appwidgets.state.WidgetSettingsStore
import com.ssilencce.extensions.utilities.DEFAULT_INFO_DELIMITER
import com.ssilencce.ui.screen.MainActivity

private val NARROW_TALL_HEIGHT = 240.dp
private val NARROW_ADDITIONAL_INFO_HEIGHT = 180.dp
private const val WIDGET_LINE_HEIGHT_FACTOR = 1.2f
private const val WIDGET_SELECTION_MARGIN_DP = 1f

// Shared by the Large/XL layout and the font-scale required-height model.
private val LARGE_ART_SIZE = 104.dp
private val LARGE_OUTER_PADDING = 16.dp
private val LARGE_CONTROLLER_TOP_PADDING = 4.dp
private val WIDE_PLAY_ICON_SIZE = 64.dp
private val EXTRA_LARGE_OUTER_TOP_PADDING = 16.dp
private val EXTRA_LARGE_OUTER_BOTTOM_PADDING = 24.dp
private val EXTRA_LARGE_SECTION_SPACER = 24.dp
private val EXTRA_LARGE_INFO_TOP_PADDING = 4.dp

private fun lineHeightDp(
    spValue: Float,
    fontScale: Float,
): Dp = (spValue * fontScale * WIDGET_LINE_HEIGHT_FACTOR).dp

private fun fitsHeight(
    height: Dp,
    required: Dp,
): Boolean = height >= required + WIDGET_SELECTION_MARGIN_DP.dp

private fun classicRequiredHeight(fontScale: Float): Dp = maxOf(56.dp, lineHeightDp(14f, fontScale) + 2.dp + 48.dp)

private fun simplifiedRequiredHeight(fontScale: Float): Dp = maxOf(56.dp, lineHeightDp(14f, fontScale) + lineHeightDp(12f, fontScale)) + 16.dp

private fun mediumRequiredHeight(fontScale: Float): Dp =
    16.dp +
        maxOf(
            72.dp,
            lineHeightDp(18f, fontScale) +
                lineHeightDp(14f, fontScale) +
                lineHeightDp(14f, fontScale),
        ) +
        48.dp

private fun largeRequiredHeight(fontScale: Float): Dp =
    LARGE_OUTER_PADDING +
        LARGE_OUTER_PADDING +
        maxOf(
            LARGE_ART_SIZE,
            lineHeightDp(20f, fontScale) +
                lineHeightDp(16f, fontScale) +
                lineHeightDp(14f, fontScale) * 2f,
        ) +
        LARGE_CONTROLLER_TOP_PADDING +
        WIDE_PLAY_ICON_SIZE

private fun extraLargeRequiredHeight(fontScale: Float): Dp =
    EXTRA_LARGE_OUTER_TOP_PADDING +
        EXTRA_LARGE_OUTER_BOTTOM_PADDING +
        (
            lineHeightDp(18f, fontScale) +
                lineHeightDp(16f, fontScale) +
                lineHeightDp(14f, fontScale) * 2f
        ) +
        EXTRA_LARGE_INFO_TOP_PADDING +
        EXTRA_LARGE_SECTION_SPACER +
        EXTRA_LARGE_SECTION_SPACER +
        WIDE_PLAY_ICON_SIZE

class SsilencceGlanceWidget : GlanceAppWidget() {
    companion object {
        private val SMALL_LAYOUT_SIZE = DpSize(width = 120.dp, height = 60.dp)
        private val MEDIUM_LAYOUT_SIZE = DpSize(width = 240.dp, height = 140.dp)
        private val LARGE_LAYOUT_SIZE = DpSize(width = 360.dp, height = 240.dp)
        private val EXTRA_LARGE_LAYOUT_SIZE = DpSize(width = 360.dp, height = 300.dp)
        private val MEDIUM_MIN_WIDTH = 240.dp
        private val WIDE_MIN_WIDTH = 320.dp

        private enum class ResponsiveLayout {
            CLASSIC,
            CLASSIC_COMPACT,
            SIMPLIFIED,
            SIMPLIFIED_COMPACT,
            NARROW,
            MEDIUM,
            LARGE,
            EXTRA_LARGE,
        }

        private fun responsiveLayoutFor(
            width: Dp,
            height: Dp,
            fontScale: Float,
            simplified: Boolean,
        ): ResponsiveLayout =
            when {
                height >= EXTRA_LARGE_LAYOUT_SIZE.height &&
                    width >= WIDE_MIN_WIDTH &&
                    fitsHeight(height, extraLargeRequiredHeight(fontScale)) -> {
                    ResponsiveLayout.EXTRA_LARGE
                }

                height >= LARGE_LAYOUT_SIZE.height &&
                    width >= WIDE_MIN_WIDTH &&
                    fitsHeight(height, largeRequiredHeight(fontScale)) -> {
                    ResponsiveLayout.LARGE
                }

                height >= MEDIUM_LAYOUT_SIZE.height &&
                    width >= MEDIUM_MIN_WIDTH &&
                    fitsHeight(height, mediumRequiredHeight(fontScale)) -> {
                    ResponsiveLayout.MEDIUM
                }

                height >= MEDIUM_LAYOUT_SIZE.height -> {
                    ResponsiveLayout.NARROW
                }

                simplified && !fitsHeight(height, simplifiedRequiredHeight(fontScale)) -> {
                    ResponsiveLayout.SIMPLIFIED_COMPACT
                }

                !simplified && !fitsHeight(height, classicRequiredHeight(fontScale)) -> {
                    ResponsiveLayout.CLASSIC_COMPACT
                }

                simplified -> {
                    ResponsiveLayout.SIMPLIFIED
                }

                else -> {
                    ResponsiveLayout.CLASSIC
                }
            }
    }

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
                    WidgetEmptyState(context = context)
                } else {
                    when (
                        responsiveLayoutFor(
                            width = currentSize.width,
                            height = currentSize.height,
                            fontScale = fontScale,
                            simplified = playbackState.isSimplifiedSmallLayout,
                        )
                    ) {
                        ResponsiveLayout.EXTRA_LARGE -> {
                            ExtraLargeWidget(context, playbackState)
                        }

                        ResponsiveLayout.LARGE -> {
                            LargeWidget(context, playbackState)
                        }

                        ResponsiveLayout.MEDIUM -> {
                            MediumWidget(context, playbackState)
                        }

                        ResponsiveLayout.NARROW -> {
                            NarrowWidget(context, playbackState, currentSize)
                        }

                        ResponsiveLayout.SIMPLIFIED -> {
                            SimplifiedWidget(context, playbackState)
                        }

                        ResponsiveLayout.SIMPLIFIED_COMPACT -> {
                            SimplifiedCompactWidget(context, playbackState)
                        }

                        ResponsiveLayout.CLASSIC -> {
                            ClassicWidget(context, playbackState)
                        }

                        ResponsiveLayout.CLASSIC_COMPACT -> {
                            ClassicCompactWidget(context, playbackState)
                        }
                    }
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
}

@Composable
private fun ClassicCompactWidget(
    context: Context,
    playbackState: PlaybackState,
) {
    Row(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .clickable(actionStartActivity<MainActivity>())
                .background(GlanceTheme.colors.surface)
                .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmallControllerGlance(
            context = context,
            playbackState = playbackState,
            modifier = GlanceModifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SimplifiedCompactWidget(
    context: Context,
    playbackState: PlaybackState,
) {
    Row(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .clickable(actionStartActivity<MainActivity>())
                .background(GlanceTheme.colors.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShapeableControlIconGlance(
            resId =
                if (playbackState.isPlaying) {
                    R.drawable.ic_pause_m3_24dp
                } else {
                    R.drawable.ic_play_m3_24dp
                },
            innerPadding = 8.dp,
            contentDescription = "Play/Pause",
            onClick =
                GlanceModifier
                    .clickable(playbackAction(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)),
        )
    }
}

@Composable
private fun ClassicWidget(
    context: Context,
    playbackState: PlaybackState,
) {
    Row(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .padding(horizontal = 8.dp)
                .clickable(actionStartActivity<MainActivity>())
                .background(GlanceTheme.colors.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArtGlance(
            playbackState = playbackState,
            modifier =
                GlanceModifier
                    .size(56.dp)
                    .cornerRadius(playbackState.imageCornerRadius?.dp ?: 8.dp),
        )

        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    GlanceModifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp),
            ) {
                Text(
                    text = playbackState.currentTitle.orEmpty(),
                    style =
                        TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        ),
                    maxLines = 1,
                )
                if (!playbackState.currentArtist.isNullOrEmpty()) {
                    Spacer(GlanceModifier.width(2.dp))

                    Text(
                        text = DEFAULT_INFO_DELIMITER,
                        style =
                            TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 14.sp,
                            ),
                        maxLines = 1,
                    )

                    Spacer(GlanceModifier.width(2.dp))

                    Text(
                        text = playbackState.currentArtist,
                        style =
                            TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 14.sp,
                            ),
                        maxLines = 1,
                    )
                }
            }

            SmallControllerGlance(
                context = context,
                playbackState = playbackState,
                modifier =
                    GlanceModifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun SimplifiedWidget(
    context: Context,
    playbackState: PlaybackState,
) {
    Row(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .padding(vertical = 8.dp, horizontal = 8.dp)
                .clickable(actionStartActivity<MainActivity>())
                .background(GlanceTheme.colors.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArtGlance(
            playbackState = playbackState,
            modifier =
                GlanceModifier
                    .size(56.dp)
                    .cornerRadius(playbackState.imageCornerRadius?.dp ?: 8.dp),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier.fillMaxWidth(),
        ) {
            Column(
                modifier =
                    GlanceModifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                        .defaultWeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = playbackState.currentTitle.orEmpty(),
                    style =
                        TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        ),
                    maxLines = 1,
                )

                Text(
                    text = playbackState.currentArtist.orEmpty(),
                    style =
                        TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp,
                        ),
                    maxLines = 1,
                )
            }

            ShapeableControlIconGlance(
                resId =
                    if (playbackState.isPlaying) {
                        R.drawable.ic_pause_m3_24dp
                    } else {
                        R.drawable.ic_play_m3_24dp
                    },
                innerPadding = 8.dp,
                contentDescription = "Play/Pause",
                onClick =
                    GlanceModifier
                        .clickable(playbackAction(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)),
            )
        }
    }
}

@Composable
private fun MediumWidget(
    context: Context,
    playbackState: PlaybackState,
) {
    Column(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .clickable(actionStartActivity<MainActivity>())
                .background(GlanceTheme.colors.surface),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            modifier =
                GlanceModifier
                    .fillMaxWidth()
                    .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumArtGlance(
                playbackState = playbackState,
                modifier =
                    GlanceModifier
                        .size(72.dp)
                        .cornerRadius(playbackState.imageCornerRadius?.dp ?: 8.dp),
            )

            Column(
                modifier =
                    GlanceModifier
                        .padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = playbackState.currentTitle.orEmpty(),
                    style =
                        TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                        ),
                    maxLines = 1,
                )

                Text(
                    text = playbackState.currentArtist.orEmpty(),
                    style =
                        TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 14.sp,
                        ),
                    maxLines = 1,
                )

                if (!playbackState.additionalInfo.isNullOrEmpty()) {
                    Text(
                        text = playbackState.additionalInfo,
                        style =
                            TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 14.sp,
                            ),
                        maxLines = 1,
                    )
                }
            }
        }

        MediumControllerGlance(
            context = context,
            playbackState = playbackState,
            modifier = GlanceModifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LargeWidget(
    context: Context,
    playbackState: PlaybackState,
) {
    Column(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .padding(LARGE_OUTER_PADDING)
                .clickable(actionStartActivity<MainActivity>())
                .background(GlanceTheme.colors.surface),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumArtGlance(
                playbackState = playbackState,
                modifier =
                    GlanceModifier
                        .size(LARGE_ART_SIZE)
                        .cornerRadius(playbackState.imageCornerRadius?.dp ?: 8.dp),
            )

            Column(
                modifier =
                    GlanceModifier
                        .padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = playbackState.currentTitle.orEmpty(),
                    style =
                        TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                        ),
                    maxLines = 1,
                )

                Text(
                    text = playbackState.currentArtist.orEmpty(),
                    style =
                        TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 16.sp,
                        ),
                    maxLines = 1,
                )

                if (!playbackState.additionalInfo.isNullOrEmpty()) {
                    Text(
                        text = playbackState.additionalInfo,
                        style =
                            TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 14.sp,
                            ),
                        maxLines = 2,
                    )
                }
            }
        }

        MediumControllerGlance(
            context = context,
            playbackState = playbackState,
            playIconSize = WIDE_PLAY_ICON_SIZE,
            modifier =
                GlanceModifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = LARGE_CONTROLLER_TOP_PADDING),
        )
    }
}

@Composable
private fun ExtraLargeWidget(
    context: Context,
    playbackState: PlaybackState,
) {
    Column(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = EXTRA_LARGE_OUTER_TOP_PADDING,
                    bottom = EXTRA_LARGE_OUTER_BOTTOM_PADDING,
                ).cornerRadius(24.dp)
                .clickable(actionStartActivity<MainActivity>())
                .background(GlanceTheme.colors.surface),
    ) {
        AlbumArtGlance(
            playbackState = playbackState,
            modifier =
                GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .cornerRadius(playbackState.imageCornerRadius?.dp?.times(2) ?: 16.dp),
        )

        Spacer(GlanceModifier.height(EXTRA_LARGE_SECTION_SPACER))

        Column(
            modifier =
                GlanceModifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = playbackState.currentTitle.orEmpty(),
                style =
                    TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    ),
                maxLines = 1,
            )

            Text(
                text = playbackState.currentArtist.orEmpty(),
                style =
                    TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 16.sp,
                    ),
                maxLines = 1,
            )

            if (!playbackState.additionalInfo.isNullOrEmpty()) {
                Text(
                    text = playbackState.additionalInfo,
                    style =
                        TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                        ),
                    maxLines = 2,
                    modifier = GlanceModifier.padding(top = EXTRA_LARGE_INFO_TOP_PADDING),
                )
            }
        }

        Spacer(GlanceModifier.height(EXTRA_LARGE_SECTION_SPACER))

        MediumControllerGlance(
            context = context,
            playbackState = playbackState,
            playIconSize = WIDE_PLAY_ICON_SIZE,
            modifier =
                GlanceModifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp),
        )
    }
}

@Composable
private fun NarrowWidget(
    context: Context,
    playbackState: PlaybackState,
    size: DpSize,
) {
    val tall = size.height >= NARROW_TALL_HEIGHT
    Column(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .clickable(actionStartActivity<MainActivity>())
                .background(GlanceTheme.colors.surface)
                .padding(if (tall) 6.dp else 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier.fillMaxWidth(),
        ) {
            AlbumArtGlance(
                playbackState = playbackState,
                modifier =
                    GlanceModifier
                        .size(if (tall) 56.dp else 32.dp)
                        .cornerRadius(playbackState.imageCornerRadius?.dp ?: 8.dp),
            )

            Spacer(GlanceModifier.width(if (tall) 8.dp else 6.dp))

            if (tall) {
                Column(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.Start,
                    modifier = GlanceModifier.defaultWeight(),
                ) {
                    Text(
                        text = playbackState.currentTitle.orEmpty(),
                        style =
                            TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            ),
                        maxLines = 1,
                    )
                    Text(
                        text = playbackState.currentArtist.orEmpty(),
                        style =
                            TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 12.sp,
                            ),
                        maxLines = 1,
                    )
                    if (!playbackState.additionalInfo.isNullOrEmpty()) {
                        Text(
                            text = playbackState.additionalInfo,
                            style =
                                TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 12.sp,
                                ),
                            maxLines = 2,
                        )
                    }
                }
            } else {
                Text(
                    text =
                        listOfNotNull(playbackState.currentTitle, playbackState.currentArtist)
                            .filter { it.isNotEmpty() }
                            .joinToString(DEFAULT_INFO_DELIMITER),
                    style =
                        TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        ),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
            }
        }

        if (tall) {
            Spacer(GlanceModifier.height(6.dp))
        } else {
            Spacer(GlanceModifier.height(2.dp))
            if (!playbackState.additionalInfo.isNullOrEmpty() && size.height >= NARROW_ADDITIONAL_INFO_HEIGHT) {
                Text(
                    text = playbackState.additionalInfo,
                    style =
                        TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp,
                        ),
                    maxLines = 1,
                )
                Spacer(GlanceModifier.height(2.dp))
            }
        }

        NarrowControllerGlance(
            context = context,
            playbackState = playbackState,
            modifier = GlanceModifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SmallControllerGlance(
    context: Context,
    playbackState: PlaybackState,
    modifier: GlanceModifier = GlanceModifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (playbackState.isForeground) {
            // Previous
            ControlIconGlance(
                resId = R.drawable.ic_previous_24dp,
                tint = GlanceTheme.colors.onSurface,
                contentDescription = "Previous",
                iconSize = 24.dp,
                modifier =
                    GlanceModifier
                        .clickable(playbackAction(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)),
            )
        }

        Spacer(GlanceModifier.defaultWeight())

        // Play/pause
        ControlIconGlance(
            resId = if (playbackState.isPlaying) R.drawable.ic_pause_24dp else R.drawable.ic_play_24dp,
            tint = GlanceTheme.colors.onSurface,
            contentDescription = "Play/Pause",
            iconSize = 24.dp,
            modifier = GlanceModifier.clickable(playbackAction(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)),
        )

        Spacer(GlanceModifier.defaultWeight())

        if (playbackState.isForeground) {
            // Next
            ControlIconGlance(
                resId = R.drawable.ic_next_24dp,
                tint = GlanceTheme.colors.onSurface,
                contentDescription = "Next",
                iconSize = 24.dp,
                modifier =
                    GlanceModifier
                        .clickable(playbackAction(context, KeyEvent.KEYCODE_MEDIA_NEXT)),
            )
        }
    }
}

@Composable
private fun MediumControllerGlance(
    context: Context,
    playbackState: PlaybackState,
    playIconSize: Dp = 48.dp,
    modifier: GlanceModifier = GlanceModifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (playbackState.isForeground) {
            // Shuffle
            ControlIconGlance(
                resId = if (playbackState.isShuffleMode) R.drawable.ic_shuffle_on_24dp else R.drawable.ic_shuffle_24dp,
                tint = GlanceTheme.colors.onSurface,
                contentDescription = "Toggle shuffle mode",
                iconSize = 28.dp,
                modifier = GlanceModifier.clickable(toggleShuffleAction(context)),
            )

            Spacer(GlanceModifier.defaultWeight())

            // Previous
            ControlIconGlance(
                resId = R.drawable.ic_previous_24dp,
                tint = GlanceTheme.colors.onSurface,
                contentDescription = "Previous",
                iconSize = 32.dp,
                modifier =
                    GlanceModifier
                        .clickable(playbackAction(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)),
            )
        }

        Spacer(GlanceModifier.defaultWeight())

        // Play/pause
        CircularControlIconGlance(
            resId = if (playbackState.isPlaying) R.drawable.ic_pause_24dp else R.drawable.ic_play_24dp,
            size = playIconSize,
            iconTint = GlanceTheme.colors.onPrimaryContainer,
            backgroundTint = GlanceTheme.colors.primaryContainer,
            contentDescription = "Play/Pause",
            onClick = GlanceModifier.clickable(playbackAction(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)),
        )

        Spacer(GlanceModifier.defaultWeight())

        if (playbackState.isForeground) {
            // Next
            ControlIconGlance(
                resId = R.drawable.ic_next_24dp,
                tint = GlanceTheme.colors.onSurface,
                contentDescription = "Next",
                iconSize = 32.dp,
                modifier =
                    GlanceModifier
                        .clickable(playbackAction(context, KeyEvent.KEYCODE_MEDIA_NEXT)),
            )

            Spacer(GlanceModifier.defaultWeight())

            // Favorite
            ControlIconGlance(
                resId = if (playbackState.isFavorite) R.drawable.ic_favorite_24dp else R.drawable.ic_favorite_outline_24dp,
                tint = GlanceTheme.colors.onSurface,
                contentDescription = "Toggle favorite",
                iconSize = 28.dp,
                modifier = GlanceModifier.clickable(toggleFavoriteAction(context)),
            )
        }
    }
}

@Composable
private fun NarrowControllerGlance(
    context: Context,
    playbackState: PlaybackState,
    modifier: GlanceModifier = GlanceModifier,
) {
    Column(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier.fillMaxWidth(),
        ) {
            if (playbackState.isForeground) {
                // Shuffle
                ControlIconGlance(
                    resId = if (playbackState.isShuffleMode) R.drawable.ic_shuffle_on_24dp else R.drawable.ic_shuffle_24dp,
                    tint = GlanceTheme.colors.onSurface,
                    contentDescription = "Toggle shuffle mode",
                    iconSize = 28.dp,
                    modifier = GlanceModifier.clickable(toggleShuffleAction(context)),
                )

                Spacer(GlanceModifier.defaultWeight())

                // Previous
                ControlIconGlance(
                    resId = R.drawable.ic_previous_24dp,
                    tint = GlanceTheme.colors.onSurface,
                    contentDescription = "Previous",
                    iconSize = 32.dp,
                    modifier =
                        GlanceModifier
                            .clickable(playbackAction(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)),
                )

                Spacer(GlanceModifier.defaultWeight())
            }

            // Play/pause
            CircularControlIconGlance(
                resId = if (playbackState.isPlaying) R.drawable.ic_pause_24dp else R.drawable.ic_play_24dp,
                size = 48.dp,
                iconTint = GlanceTheme.colors.onPrimaryContainer,
                backgroundTint = GlanceTheme.colors.primaryContainer,
                contentDescription = "Play/Pause",
                onClick = GlanceModifier.clickable(playbackAction(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)),
            )

            if (playbackState.isForeground) {
                Spacer(GlanceModifier.defaultWeight())

                // Next
                ControlIconGlance(
                    resId = R.drawable.ic_next_24dp,
                    tint = GlanceTheme.colors.onSurface,
                    contentDescription = "Next",
                    iconSize = 32.dp,
                    modifier =
                        GlanceModifier
                            .clickable(playbackAction(context, KeyEvent.KEYCODE_MEDIA_NEXT)),
                )
            }
        }

        if (playbackState.isForeground) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = GlanceModifier.fillMaxWidth(),
            ) {
                Spacer(GlanceModifier.defaultWeight())

                // Favorite
                ControlIconGlance(
                    resId = if (playbackState.isFavorite) R.drawable.ic_favorite_24dp else R.drawable.ic_favorite_outline_24dp,
                    tint = GlanceTheme.colors.onSurface,
                    contentDescription = "Toggle favorite",
                    iconSize = 28.dp,
                    modifier = GlanceModifier.clickable(toggleFavoriteAction(context)),
                )

                Spacer(GlanceModifier.defaultWeight())
            }
        }
    }
}
