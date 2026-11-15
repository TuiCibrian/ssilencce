package com.ssilencce.ui.component.preferences.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ssilencce.R
import com.ssilencce.core.model.action.QuickAccessAction
import com.ssilencce.ui.theme.SsilencceTheme
import com.ssilencce.util.Preferences

/**
 * Settings dialog to enable, disable and reorder the Now Playing quick-access row.
 */
class QuickAccessPreferenceDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext())
            .setView(
                ComposeView(requireContext()).apply {
                    setViewCompositionStrategy(
                        ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
                    )
                    setContent {
                        SsilencceTheme {
                            QuickAccessScreen(onClose = { dismissAllowingStateLoss() })
                        }
                    }
                },
            ).create()
}

@Composable
private fun QuickAccessScreen(onClose: () -> Unit) {
    var selected by remember { mutableStateOf(Preferences.quickAccessActions) }
    val maxHeight =
        with(LocalDensity.current) {
            LocalWindowInfo.current.containerSize.height
                .toDp() * 0.85f
        }

    fun persist(next: List<QuickAccessAction>) {
        selected = next
        Preferences.quickAccessActions = next
    }

    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(
            modifier =
                Modifier
                    .heightIn(max = maxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.quick_access_actions_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.quick_access_actions_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )

            val ordered = selected + QuickAccessAction.entries.filter { it !in selected }
            ordered.forEach { action ->
                val index = selected.indexOf(action)
                val isSelected = index >= 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { checked ->
                            if (checked) {
                                persist(selected + action)
                            } else {
                                persist(selected - action)
                            }
                        },
                    )
                    Icon(
                        painter = painterResource(action.iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(action.titleRes),
                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (isSelected) {
                        IconButton(
                            enabled = index > 0,
                            onClick = {
                                persist(selected.toMutableList().apply { add(index - 1, removeAt(index)) })
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_keyboard_arrow_up_24dp),
                                contentDescription = stringResource(R.string.quick_access_move_up),
                            )
                        }
                        IconButton(
                            enabled = index < selected.lastIndex,
                            onClick = {
                                persist(selected.toMutableList().apply { add(index + 1, removeAt(index)) })
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_keyboard_arrow_down_24dp),
                                contentDescription = stringResource(R.string.quick_access_move_down),
                            )
                        }
                    }
                }
            }

            TextButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(R.string.action_done))
            }
        }
    }
}
