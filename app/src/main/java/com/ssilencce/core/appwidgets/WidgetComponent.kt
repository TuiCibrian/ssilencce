package com.ssilencce.core.appwidgets

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.wrapContentSize
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ssilencce.R
import com.ssilencce.core.appwidgets.state.PlaybackState
import com.ssilencce.ui.screen.MainActivity

@Composable
fun AlbumArtGlance(
    playbackState: PlaybackState,
    placeholderColor: ColorProvider = GlanceTheme.colors.tertiaryContainer,
    placeholderIconColor: ColorProvider = GlanceTheme.colors.onTertiaryContainer,
    modifier: GlanceModifier = GlanceModifier,
) {
    val bitmap =
        playbackState.artworkData?.let {
            try {
                val options = BitmapFactory.Options()
                BitmapFactory.decodeByteArray(it, 0, it.size, options)
            } catch (t: Throwable) {
                Log.e("SsilencceGlanceWidget", "Cannot decode artwork bitmap", t)
                null
            }
        }
    if (bitmap != null) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = "Album Art",
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier.background(placeholderColor),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_music_note_24dp),
                colorFilter = ColorFilter.tint(placeholderIconColor),
                contentDescription = "Album Art",
                modifier = GlanceModifier.wrapContentSize(),
            )
        }
    }
}

@Composable
fun ControlIconGlance(
    resId: Int,
    tint: ColorProvider,
    contentDescription: String,
    iconSize: Dp = 24.dp,
    modifier: GlanceModifier = GlanceModifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            GlanceModifier
                .size(48.dp)
                .then(modifier),
    ) {
        Image(
            provider = ImageProvider(resId),
            contentDescription = contentDescription,
            modifier = GlanceModifier.size(iconSize),
            colorFilter = ColorFilter.tint(tint),
        )
    }
}

@Composable
fun CircularControlIconGlance(
    resId: Int,
    size: Dp,
    iconTint: ColorProvider,
    backgroundTint: ColorProvider,
    contentDescription: String,
    onClick: GlanceModifier,
) {
    ShapeableControlIconGlance(
        resId = resId,
        size = size,
        cornerRadius = size / 2,
        innerPadding = size / 6,
        iconTint = iconTint,
        backgroundTint = backgroundTint,
        contentDescription = contentDescription,
        onClick = onClick,
    )
}

@Composable
fun ShapeableControlIconGlance(
    resId: Int,
    size: Dp = 48.dp,
    cornerRadius: Dp = 16.dp,
    innerPadding: Dp = 16.dp,
    iconTint: ColorProvider = GlanceTheme.colors.onPrimaryContainer,
    backgroundTint: ColorProvider = GlanceTheme.colors.primaryContainer,
    contentDescription: String? = null,
    onClick: GlanceModifier = GlanceModifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            GlanceModifier
                .size(size)
                .cornerRadius(cornerRadius)
                .then(onClick)
                .padding(innerPadding)
                .background(backgroundTint),
    ) {
        Image(
            provider = ImageProvider(resId),
            contentDescription = contentDescription,
            modifier = GlanceModifier.fillMaxSize(),
            colorFilter = ColorFilter.tint(iconTint),
        )
    }
}

private val COMPACT_EMPTY_STATE_MAX_HEIGHT = 160.dp

@Composable
fun WidgetEmptyState(
    context: Context,
    cornerRadius: Dp = 16.dp,
) {
    val compact = LocalSize.current.height < COMPACT_EMPTY_STATE_MAX_HEIGHT
    Box(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .cornerRadius(cornerRadius)
                .background(GlanceTheme.colors.surface)
                .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = GlanceModifier.padding(if (compact) 6.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!compact) {
                Image(
                    provider = ImageProvider(R.mipmap.ic_launcher),
                    contentDescription = null,
                    modifier = GlanceModifier.size(48.dp),
                )
            }
            Text(
                text = context.getString(R.string.widget_empty_nothing_playing),
                style =
                    if (compact) {
                        TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                    } else {
                        TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                modifier = GlanceModifier.padding(top = if (compact) 0.dp else 8.dp),
                maxLines = if (compact) 1 else Int.MAX_VALUE,
            )
            Text(
                text = context.getString(R.string.widget_empty_open_app),
                style =
                    if (compact) {
                        TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp,
                        )
                    } else {
                        TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
                    },
                modifier = GlanceModifier.padding(top = if (compact) 2.dp else 4.dp),
                maxLines = if (compact) 1 else Int.MAX_VALUE,
            )
        }
    }
}
