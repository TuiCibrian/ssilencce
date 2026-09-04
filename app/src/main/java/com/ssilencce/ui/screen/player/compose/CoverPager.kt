package com.ssilencce.ui.screen.player.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.ssilencce.R
import com.ssilencce.core.model.player.PlayerTransition
import com.ssilencce.data.model.Song
import com.ssilencce.ui.component.compose.MediaImage
import kotlin.math.absoluteValue

@Composable
fun CoverPager(
    queue: List<Song>,
    currentPosition: Int,
    transition: PlayerTransition,
    isCarouselEffect: Boolean,
    isLandscape: Boolean,
    coverGestures: Modifier,
    onSongSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 24.dp,
    enableShadow: Boolean = true,
) {
    val pagerState =
        rememberPagerState(
            pageCount = { queue.size },
            initialPage = currentPosition,
        )

    LaunchedEffect(currentPosition) {
        if (pagerState.currentPage != currentPosition) {
            pagerState.animateScrollToPage(currentPosition)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow {
            pagerState.isScrollInProgress to pagerState.currentPage
        }.collect { (isScrolling, page) ->
            if (!isScrolling && page != currentPosition && queue.isNotEmpty()) {
                onSongSelected(page)
            }
        }
    }

    val isCarousel = isCarouselEffect && !isLandscape && queue.size > 1
    val carouselPadding = if (isCarousel) 100.dp else 0.dp

    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = carouselPadding),
        pageSpacing = if (isCarousel) 0.dp else 8.dp,
    ) { page ->
        CoverPage(
            song = queue[page],
            pageOffset = pagerState.pageOffset(page),
            transition = transition,
            isCarousel = isCarousel,
            coverGestures = coverGestures,
            cornerRadius = cornerRadius,
            enableShadow = enableShadow,
        )
    }
}

@Composable
private fun CoverPage(
    song: Song,
    pageOffset: Float,
    transition: PlayerTransition,
    isCarousel: Boolean,
    coverGestures: Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 24.dp,
    enableShadow: Boolean = true,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val offset = pageOffset.coerceIn(-1f, 1f)
    val scale =
        when {
            isCarousel || transition == PlayerTransition.HorizontalFlip || transition == PlayerTransition.VerticalFlip -> 1f - offset.absoluteValue * 0.1f
            transition == PlayerTransition.Stack -> 1f
            else -> 1f - offset.absoluteValue * 0.06f
        }
    val alpha =
        when {
            isCarousel -> 1f - offset.absoluteValue * 0.4f
            transition == PlayerTransition.Stack || transition == PlayerTransition.Cascading -> 1f - offset.absoluteValue * 0.55f
            transition == PlayerTransition.Depth || transition == PlayerTransition.ZoomOut -> 1f - offset.absoluteValue * 0.45f
            else -> 1f - offset.absoluteValue * 0.3f
        }
    val rotationY =
        if (transition == PlayerTransition.HorizontalFlip || transition == PlayerTransition.Hinge) {
            offset * 40f
        } else {
            0f
        }
    val rotationX =
        if (transition == PlayerTransition.VerticalFlip) {
            offset * 40f
        } else {
            0f
        }
    val translationY =
        when (transition) {
            PlayerTransition.Stack -> offset * -48f
            PlayerTransition.Cascading -> offset * -20f
            else -> 0f
        }

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .fillMaxSize()
                .padding(if (cornerRadius > 0.dp) 8.dp else 0.dp),
    ) {
        val coverModifier =
            Modifier
                .fillMaxWidth(if (cornerRadius > 0.dp) 0.9f else 1f)
                .aspectRatio(1f)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                    this.rotationY = rotationY
                    this.rotationX = rotationX
                    this.translationY = translationY
                }
        val shadowedModifier =
            if (enableShadow) {
                coverModifier.shadow(
                    elevation = 42.dp,
                    shape = shape,
                    ambientColor = Color.White,
                    spotColor = Color.White,
                )
            } else {
                coverModifier
            }
        Box(
            modifier =
                shadowedModifier
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            MediaImage(
                model = song,
                placeholderIcon = R.drawable.ic_music_note_24dp,
                contentDescription = song.title,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier.fillMaxSize().then(coverGestures),
            )
        }
    }
}

private fun PagerState.pageOffset(page: Int): Float {
    val current = currentPage + currentPageOffsetFraction
    if (page < current - 1 || page > current + 1) return 0f
    return (page - current).coerceIn(-1f, 1f)
}
