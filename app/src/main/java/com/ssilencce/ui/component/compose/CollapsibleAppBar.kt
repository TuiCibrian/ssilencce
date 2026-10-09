package com.ssilencce.ui.component.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.ssilencce.R
import com.ssilencce.ui.component.views.TopAppBarLayout
import com.ssilencce.ui.theme.AuroraDesign
import com.ssilencce.util.Preferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsibleAppBarScaffold(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    showNavigationButton: Boolean = true,
    collapsibleAppBar: Boolean = Preferences.appBarMode == TopAppBarLayout.AppBarMode.COLLAPSING,
    miniPlayerMargin: Int = 0,
    onBackClick: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior =
        if (collapsibleAppBar) {
            TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        } else {
            TopAppBarDefaults.enterAlwaysScrollBehavior()
        }
    Scaffold(
        containerColor = if (Preferences.isAuroraAppearance) Color.Transparent else MaterialTheme.colorScheme.background,
        modifier =
            modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (collapsibleAppBar && !Preferences.isAuroraAppearance) {
                MediumTopAppBar(
                    title = { Text(text = title) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                    navigationIcon = {
                        if (showNavigationButton) {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_back_24dp),
                                    contentDescription = stringResource(R.string.back_action),
                                )
                            }
                        }
                    },
                    actions = actions,
                    scrollBehavior = scrollBehavior,
                )
            } else {
                TopAppBar(
                    title = { if (Preferences.isAuroraAppearance) Text(text = title, fontSize = 19.sp) else Text(text = title) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = if (Preferences.isAuroraAppearance) Color.Transparent else MaterialTheme.colorScheme.surface, scrolledContainerColor = if (Preferences.isAuroraAppearance) Color.Transparent else MaterialTheme.colorScheme.surface),
                    navigationIcon = {
                        if (showNavigationButton) {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_back_24dp),
                                    contentDescription = stringResource(R.string.back_action),
                                )
                            }
                        }
                    },
                    actions = actions,
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        snackbarHost = snackbarHost,
        contentWindowInsets =
            WindowInsets.safeDrawing
                .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                .add(WindowInsets(bottom = miniPlayerMargin)),
    ) { contentPadding ->
        content(contentPadding)
    }
}
