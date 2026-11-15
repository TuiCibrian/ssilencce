package com.ssilencce.ui.screen.player.screencast

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ssilencce.R

@Composable
fun ScreencastPickerScreen(
    displays: List<ScreencastDisplayInfo>,
    activeDisplayId: Int?,
    onToggle: (Boolean) -> Unit,
    onSelectDisplay: (Int) -> Unit,
    onFindDisplays: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
) {
    Surface(shape = MaterialTheme.shapes.extraLarge) {
        val containerHeight =
            with(LocalDensity.current) {
                LocalWindowInfo.current.containerSize.height
                    .toDp()
            }
        Column(
            modifier =
                Modifier
                    .padding(24.dp)
                    .heightIn(max = containerHeight * 0.85f)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_screencast_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    text = stringResource(R.string.screencast_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
            }
            Text(
                text = stringResource(R.string.screencast_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val toggleEnabled = displays.isNotEmpty() || activeDisplayId != null
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = activeDisplayId != null,
                            enabled = toggleEnabled,
                            role = Role.Switch,
                            onValueChange = onToggle,
                        ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.screencast_enable_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text =
                            if (activeDisplayId == null) {
                                stringResource(R.string.screencast_enable_summary_off)
                            } else {
                                val activeName =
                                    displays
                                        .firstOrNull { it.id == activeDisplayId }
                                        ?.name
                                        ?.takeIf { it.isNotBlank() }
                                        ?: stringResource(
                                            R.string.screencast_display_unknown,
                                            activeDisplayId,
                                        )
                                stringResource(R.string.screencast_enable_summary_on, activeName)
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = activeDisplayId != null,
                    onCheckedChange = null,
                    enabled = toggleEnabled,
                )
            }
            if (displays.isEmpty()) {
                Text(
                    text = stringResource(R.string.screencast_no_displays),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(R.string.screencast_displays_header),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                displays.forEach { display ->
                    val selected = display.id == activeDisplayId
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selected,
                                    onClick = { onSelectDisplay(display.id) },
                                    role = Role.RadioButton,
                                ).padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text =
                                    display.name.ifBlank {
                                        stringResource(
                                            R.string.screencast_display_unknown,
                                            display.id,
                                        )
                                    },
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            if (display.widthPx > 0 && display.heightPx > 0) {
                                Text(
                                    text =
                                        stringResource(
                                            R.string.screencast_display_resolution,
                                            display.widthPx,
                                            display.heightPx,
                                        ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ScreencastActionButton(
                    iconRes = R.drawable.ic_search_24dp,
                    titleRes = R.string.screencast_find_displays,
                    onClick = onFindDisplays,
                )
                ScreencastActionButton(
                    iconRes = R.drawable.ic_settings_24dp,
                    titleRes = R.string.screencast_display_settings,
                    onClick = onOpenSettings,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onClose) {
                    Text(stringResource(R.string.screencast_close))
                }
            }
        }
    }
}

@Composable
private fun ScreencastActionButton(
    @DrawableRes iconRes: Int,
    @StringRes titleRes: Int,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(stringResource(titleRes))
        }
    }
}
