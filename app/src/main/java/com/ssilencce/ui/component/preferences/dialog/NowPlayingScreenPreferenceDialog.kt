package com.ssilencce.ui.component.preferences.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ssilencce.R
import com.ssilencce.core.model.player.PlayerColorSchemeMode
import com.ssilencce.core.model.player.PlayerTransition
import com.ssilencce.core.model.theme.NowPlayingScreen
import com.ssilencce.ui.theme.SsilencceTheme
import com.ssilencce.util.Preferences

class NowPlayingScreenPreferenceDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val composeView = ComposeView(requireContext()).apply {
            setContent {
                SsilencceTheme {
                    StylePickerContent(
                        onStyleSelected = { screen ->
                            pendingScreen = screen
                        },
                    )
                }
            }
        }
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.now_playing_screen_title)
            .setView(composeView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                pendingScreen?.let { Preferences.nowPlayingScreen = it }
            }.setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    private var pendingScreen: NowPlayingScreen? = null

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun StylePickerContent(onStyleSelected: (NowPlayingScreen) -> Unit) {
        val screens = NowPlayingScreen.entries
        val initialPage = Preferences.nowPlayingScreen.ordinal.coerceIn(screens.indices)
        val pagerState = rememberPagerState(pageCount = { screens.size }, initialPage = initialPage)
        var currentPage by remember { mutableStateOf(initialPage) }

        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }.collect { page ->
                currentPage = page
                onStyleSelected(screens[page])
            }
        }
        LaunchedEffect(Unit) { onStyleSelected(screens[initialPage]) }

        val currentScreen = screens[currentPage]
        var selectedScheme by remember(currentPage) {
            mutableStateOf(Preferences.getNowPlayingColorSchemeMode(currentScreen))
        }
        var selectedTransition by remember(currentPage) {
            mutableStateOf(Preferences.getNowPlayingTransition(currentScreen))
        }
        var schemeExpanded by remember { mutableStateOf(false) }
        var transitionExpanded by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
            ) { page ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Image(
                        painter = painterResource(screens[page].drawableResId),
                        contentDescription = stringResource(screens[page].titleRes),
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                    )
                    Text(
                        text = stringResource(screens[page].titleRes),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            val schemes = currentScreen.supportedColorSchemes
            ExposedDropdownMenuBox(
                expanded = schemeExpanded,
                onExpandedChange = { if (schemes.size > 1) schemeExpanded = !schemeExpanded },
            ) {
                OutlinedTextField(
                    value = schemeName(selectedScheme),
                    onValueChange = {},
                    readOnly = true,
                    enabled = schemes.size > 1,
                    label = { Text(stringResource(R.string.now_playing_color_scheme)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = schemeExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = schemeExpanded,
                    onDismissRequest = { schemeExpanded = false },
                ) {
                    schemes.forEach { scheme ->
                        DropdownMenuItem(
                            text = { Text(schemeName(scheme)) },
                            onClick = {
                                selectedScheme = scheme
                                Preferences.setNowPlayingColorSchemeMode(currentScreen, scheme)
                                schemeExpanded = false
                            },
                        )
                    }
                }
            }

            val transitions = currentScreen.supportedTransitions
            ExposedDropdownMenuBox(
                expanded = transitionExpanded,
                onExpandedChange = { if (transitions.size > 1) transitionExpanded = !transitionExpanded },
            ) {
                OutlinedTextField(
                    value = transitionName(selectedTransition),
                    onValueChange = {},
                    readOnly = true,
                    enabled = transitions.size > 1,
                    label = { Text(stringResource(R.string.now_playing_transition)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = transitionExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = transitionExpanded,
                    onDismissRequest = { transitionExpanded = false },
                ) {
                    transitions.forEach { transition ->
                        DropdownMenuItem(
                            text = { Text(transitionName(transition)) },
                            onClick = {
                                selectedTransition = transition
                                Preferences.setNowPlayingTransition(currentScreen, transition)
                                transitionExpanded = false
                            },
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun schemeName(scheme: PlayerColorSchemeMode): String = stringResource(scheme.titleRes)

    @Composable
    private fun transitionName(transition: PlayerTransition): String = stringResource(transition.nameRes)
}
