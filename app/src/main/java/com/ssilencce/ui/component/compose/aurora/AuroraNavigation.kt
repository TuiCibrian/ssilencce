package com.ssilencce.ui.component.compose.aurora

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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

@Composable
fun AuroraCategoryTabs(
    categories: List<CategoryInfo>,
    destination: Int,
    onNavigate: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onSearchCategory: ((Int) -> Unit)? = null,
) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp)) {
            categories.filter { it.visible }.forEach { info ->
                val selected = destination == info.category.id
                Column(
                    Modifier.combinedClickable(role = Role.Tab, onClick = { onNavigate(info.category.id) }, onLongClick = onSearchCategory?.let { { it(info.category.id) } }, onLongClickLabel = stringResource(R.string.search_label)).semantics { this.selected = selected }.heightIn(min = 48.dp).drawBehind {
                        if (selected) drawLine(Color.White, Offset(12.dp.toPx(), size.height - 1.5.dp.toPx()), Offset(size.width - 12.dp.toPx(), size.height - 1.5.dp.toPx()), 3.dp.toPx())
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(info.category.titleRes), Modifier.padding(horizontal = 13.dp, vertical = 13.dp), fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, color = if (selected) Color.White else AuroraDesign.muted)
                    Spacer(Modifier.height(3.dp))
                }
            }
        }
        HorizontalDivider(color = AuroraDesign.border)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AuroraLibraryDrawer(
    categories: List<CategoryInfo>,
    destination: Int,
    onNavigate: (Int) -> Unit,
    onDismiss: () -> Unit,
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
                        .padding(12.dp),
                ) {
                    Text("ssilencce", Modifier.padding(16.dp).semantics { heading() }, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    categories.forEach { info ->
                        ListItem(
                            content = { Text(stringResource(info.category.titleRes), fontSize = 15.sp) },
                            leadingContent = { Icon(painterResource(auroraNavigationIcon(info.category.iconRes, info.category.id == destination)), null) },
                            colors = ListItemDefaults.colors(containerColor = if (info.category.id == destination) Color.White.copy(alpha = .06f) else Color.Transparent),
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).selectable(info.category.id == destination, role = Role.Tab, onClick = { onNavigate(info.category.id) }),
                        )
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = AuroraDesign.border)
                    listOf(
                        Triple(R.id.nav_search, R.string.search_label, R.drawable.ic_search_24dp),
                        Triple(R.id.nav_equalizer, R.string.equalizer_label, R.drawable.ic_equalizer_24dp),
                        Triple(R.id.nav_settings, R.string.settings_title, R.drawable.ic_settings_24dp),
                    ).forEach { (id, title, icon) ->
                        ListItem(content = { Text(stringResource(title)) }, leadingContent = { Icon(painterResource(icon), null) }, colors = ListItemDefaults.colors(containerColor = Color.Transparent), modifier = Modifier.clickable { onNavigate(id) })
                    }
                    AuroraUnavailableRow(R.string.aurora_composers)
                    AuroraUnavailableRow(R.string.aurora_custom_tags)
                }
            }
        }
    }
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
