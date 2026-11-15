package com.ssilencce.ui.component.compose.aurora

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ssilencce.R
import com.ssilencce.core.model.CategoryInfo
import com.ssilencce.ui.theme.AuroraDesign
import com.ssilencce.ui.theme.SsilencceTheme

@Composable
fun AuroraNavigation(
    destination: Int,
    playerExpanded: Boolean,
    playerAvailable: Boolean,
    moreExpanded: Boolean,
    onNavigate: (Int) -> Unit,
    onPlayer: () -> Unit,
    onMore: () -> Unit,
    onLibrary: () -> Unit,
    modifier: Modifier = Modifier,
    onSearchCategory: ((Int) -> Unit)? = null,
) {
    Column(modifier.background(AuroraDesign.glassStrong).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)).navigationBarsPadding()) {
        if (moreExpanded) {
            HorizontalDivider(color = AuroraDesign.border)
            NavigationRow(64.dp) { item ->
                NavigationItem(R.drawable.ic_search_24dp, R.string.search_label, destination == R.id.nav_search, { onNavigate(R.id.nav_search) }, item)
                NavigationItem(R.drawable.icon_artist, R.string.artists_label, destination == R.id.nav_artists, { onNavigate(R.id.nav_artists) }, item, onLongClick = onSearchCategory?.let { { it(R.id.nav_artists) } })
                NavigationItem(R.drawable.icon_playlist, R.string.playlists_label, destination == R.id.nav_playlists, { onNavigate(R.id.nav_playlists) }, item, onLongClick = onSearchCategory?.let { { it(R.id.nav_playlists) } })
                NavigationItem(R.drawable.ic_lyrics_outline_24dp, R.string.action_show_lyrics, destination == R.id.nav_lyrics, { onNavigate(R.id.nav_lyrics) }, item, playerAvailable)
                NavigationItem(R.drawable.icon_music, R.string.aurora_library_drawer, false, onLibrary, item)
                NavigationItem(R.drawable.ic_settings_24dp, R.string.settings_title, destination == R.id.nav_settings, { onNavigate(R.id.nav_settings) }, item)
            }
        }
        HorizontalDivider(color = AuroraDesign.border)
        NavigationRow(71.dp) { item ->
            NavigationItem(R.drawable.icon_home, R.string.home_label, !playerExpanded && destination == R.id.nav_home, { onNavigate(R.id.nav_home) }, item, onLongClick = onSearchCategory?.let { { it(R.id.nav_home) } })
            NavigationItem(R.drawable.ic_queue_music_24dp, R.string.aurora_queue_label, !playerExpanded && destination == R.id.nav_queue, { onNavigate(R.id.nav_queue) }, item)
            NavigationItem(R.drawable.ic_play_24dp, R.string.aurora_player_label, playerExpanded, onPlayer, item, playerAvailable)
            NavigationItem(R.drawable.icon_folder, R.string.folders_label, !playerExpanded && destination == R.id.nav_folders, { onNavigate(R.id.nav_folders) }, item, onLongClick = onSearchCategory?.let { { it(R.id.nav_folders) } })
            NavigationItem(R.drawable.icon_album, R.string.albums_label, !playerExpanded && destination == R.id.nav_albums, { onNavigate(R.id.nav_albums) }, item, onLongClick = onSearchCategory?.let { { it(R.id.nav_albums) } })
            NavigationItem(R.drawable.ic_more_horiz_24dp, R.string.action_more, moreExpanded, onMore, item)
        }
    }
}

@Composable
private fun NavigationRow(
    height: Dp,
    content: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = ((maxWidth - 4.dp) / 6).coerceAtLeast(48.dp)
        Row(Modifier.horizontalScroll(rememberScrollState()).height(height).padding(horizontal = 2.dp)) {
            content(Modifier.width(width))
        }
    }
}

@Composable
private fun NavigationItem(
    icon: Int,
    label: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
) {
    val description = stringResource(label)
    Box(
        modifier
            .fillMaxHeight()
            .widthIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color.White.copy(alpha = .06f) else Color.Transparent)
            .combinedClickable(enabled = enabled, role = Role.Tab, onClick = onClick, onLongClick = onLongClick, onLongClickLabel = stringResource(R.string.search_label))
            .semantics {
                contentDescription = description
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .width(42.dp)
                    .height(3.dp)
                    .background(Color.White, RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp)),
            )
        }
        Icon(painterResource(auroraNavigationIcon(icon, selected)), null, Modifier.size(26.dp), tint = if (selected) Color.White else AuroraDesign.muted.copy(alpha = if (enabled) 1f else .38f))
    }
}

// Historical: foreground used by the retired category strip to keep selectable
// normal text at 4.5:1 or more over the moving ribbon. Kept intentionally with no
// current callers; AuroraDesign.muted stays unchanged for captions elsewhere.
private val AuroraInactiveTabColor = Color(0xFFDCDCDC)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AuroraLibraryDrawer(
    categories: List<CategoryInfo>,
    destination: Int,
    onNavigate: (Int) -> Unit,
    onDismiss: () -> Unit,
    onTopTracks: () -> Unit,
    onRecentlyAdded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(modifier.fillMaxSize().background(Color.Black.copy(alpha = .52f)).clickable(onClickLabel = stringResource(R.string.close_action), onClick = onDismiss)) {
            Surface(Modifier.fillMaxHeight().fillMaxWidth(.84f).clickable(enabled = false) {}, color = AuroraDesign.glassStrong) {
                Column(
                    Modifier
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp),
                ) {
                    AuroraDrawerHeader()
                    AuroraDrawerGroup(R.string.aurora_drawer_group_library) {
                        val referenceOrder = listOf(R.id.nav_songs, R.id.nav_albums, R.id.nav_artists, R.id.nav_folders, R.id.nav_playlists)
                        categories
                            .filterNot { it.category == CategoryInfo.Category.Queues }
                            .sortedBy { info -> referenceOrder.indexOf(info.category.id).let { if (it < 0) Int.MAX_VALUE else it } }
                            .forEach { info ->
                                AuroraDrawerTabItem(
                                    icon = auroraNavigationIcon(info.category.iconRes, info.category.id == destination),
                                    title = info.category.titleRes,
                                    selected = info.category.id == destination,
                                    onClick = { onNavigate(info.category.id) },
                                )
                            }
                        AuroraDrawerTabItem(
                            icon = R.drawable.ic_queue_music_24dp,
                            title = R.string.aurora_queue_label,
                            selected = destination == R.id.nav_queue,
                            onClick = { onNavigate(R.id.nav_queue) },
                        )
                    }
                    AuroraDrawerGroup(R.string.aurora_drawer_group_discover) {
                        AuroraDrawerActionItem(R.drawable.ic_trending_up_24dp, R.string.aurora_most_played, onTopTracks)
                        AuroraDrawerActionItem(R.drawable.ic_history_24dp, R.string.aurora_recently_added, onRecentlyAdded)
                        AuroraDrawerActionItem(R.drawable.ic_search_24dp, R.string.search_label) { onNavigate(R.id.nav_search) }
                    }
                    AuroraDrawerGroup(R.string.aurora_drawer_group_system) {
                        AuroraDrawerActionItem(R.drawable.ic_equalizer_24dp, R.string.equalizer_label) { onNavigate(R.id.nav_equalizer) }
                        AuroraDrawerActionItem(R.drawable.ic_settings_24dp, R.string.settings_title) { onNavigate(R.id.nav_settings) }
                    }
                    AuroraUnavailableRow(R.string.aurora_composers)
                    AuroraUnavailableRow(R.string.aurora_custom_tags)
                }
            }
        }
    }
}

@Composable
private fun AuroraDrawerHeader() {
    Row(
        Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
                .border(1.dp, Color.White.copy(alpha = .08f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.app_logo_splash), contentDescription = null, modifier = Modifier.size(46.dp))
        }
        Column {
            Text(stringResource(R.string.app_name), Modifier.semantics { heading() }, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = AuroraDesign.foreground)
            Text(stringResource(R.string.aurora_drawer_subtitle), Modifier.padding(top = 2.dp), fontSize = 12.sp, color = AuroraDesign.muted)
        }
    }
}

@Composable
private fun AuroraDrawerGroup(
    title: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    Text(
        stringResource(title),
        Modifier.padding(start = 22.dp, top = 10.dp, bottom = 5.dp).semantics { heading() },
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = .08.em,
        color = AuroraDesign.muted,
    )
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp), content = content)
}

@Composable
private fun AuroraDrawerTabItem(
    icon: Int,
    title: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        content = { Text(stringResource(title), fontSize = 15.sp) },
        leadingContent = { Icon(painterResource(icon), null) },
        colors = ListItemDefaults.colors(containerColor = if (selected) Color.White.copy(alpha = .06f) else Color.Transparent),
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).selectable(selected, role = Role.Tab, onClick = onClick),
    )
}

@Composable
private fun AuroraDrawerActionItem(
    icon: Int,
    title: Int,
    onClick: () -> Unit,
) {
    ListItem(
        content = { Text(stringResource(title), fontSize = 15.sp) },
        leadingContent = { Icon(painterResource(icon), null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onClick),
    )
}

private fun auroraNavigationIcon(
    icon: Int,
    selected: Boolean,
): Int =
    when (icon) {
        R.drawable.icon_home -> if (selected) R.drawable.ic_home_filled_24dp else R.drawable.ic_home_24dp
        R.drawable.icon_album -> if (selected) R.drawable.ic_album_filled_24dp else R.drawable.ic_album_24dp
        R.drawable.icon_artist -> if (selected) R.drawable.ic_artist_filled_24dp else R.drawable.ic_artist_24dp
        R.drawable.icon_folder -> if (selected) R.drawable.ic_folder_filled_24dp else R.drawable.ic_folder_24dp
        R.drawable.icon_genre -> if (selected) R.drawable.ic_radio_filled_24dp else R.drawable.ic_radio_24dp
        R.drawable.icon_year -> if (selected) R.drawable.ic_event_filled_24dp else R.drawable.ic_event_24dp
        R.drawable.icon_playlist -> R.drawable.ic_queue_music_24dp
        R.drawable.icon_music -> R.drawable.ic_music_note_24dp
        else -> icon
    }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AuroraUnavailableRow(
    title: Int,
    modifier: Modifier = Modifier,
) {
    ListItem(
        content = { Text(stringResource(title), color = AuroraDesign.muted) },
        supportingContent = { Text(stringResource(R.string.aurora_future_summary), fontSize = 12.sp, color = AuroraDesign.muted) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier.semantics { disabled() },
    )
}

@Preview(widthDp = 392)
@Composable
private fun AuroraNavigationPreview() {
    SsilencceTheme(dynamicColor = false) {
        AuroraNavigation(R.id.nav_home, false, true, false, {}, {}, {}, {})
    }
}
