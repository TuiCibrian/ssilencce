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
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ssilencce.R
import com.ssilencce.core.model.menu.MenuCustomizationGroup
import com.ssilencce.core.model.menu.MenuCustomizationState
import com.ssilencce.ui.theme.SsilencceTheme
import com.ssilencce.util.Preferences

/**
 * Settings dialog to enable, disable and reorder the actions of one entity option menu.
 * Changes are persisted as soon as a checkbox or an arrow is used.
 */
class MenuCustomizationDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val group =
            arguments
                ?.getString(ARG_GROUP)
                ?.let { name -> MenuCustomizationGroup.entries.firstOrNull { it.name == name } }
                ?: MenuCustomizationGroup.Song
        return MaterialAlertDialogBuilder(requireContext())
            .setView(
                ComposeView(requireContext()).apply {
                    setViewCompositionStrategy(
                        ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
                    )
                    setContent {
                        SsilencceTheme {
                            MenuCustomizationScreen(
                                group = group,
                                onClose = { dismissAllowingStateLoss() },
                            )
                        }
                    }
                },
            ).create()
    }

    companion object {
        const val TAG = "MENU_CUSTOMIZATION"
        private const val ARG_GROUP = "group"

        fun newInstance(group: MenuCustomizationGroup): MenuCustomizationDialog =
            MenuCustomizationDialog().apply {
                arguments = bundleOf(ARG_GROUP to group.name)
            }
    }
}

@Composable
private fun MenuCustomizationScreen(
    group: MenuCustomizationGroup,
    onClose: () -> Unit,
) {
    var state by remember { mutableStateOf(Preferences.menuCustomization(group)) }
    val maxHeight =
        with(LocalDensity.current) {
            LocalWindowInfo.current.containerSize.height
                .toDp() * 0.85f
        }

    fun persist(next: MenuCustomizationState) {
        state = next
        Preferences.setMenuCustomization(group, next)
    }

    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(
            modifier =
                Modifier
                    .heightIn(max = maxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(group.titleRes),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.menu_customization_description),
                modifier = Modifier.padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )

            state.order.forEachIndexed { index, key ->
                val item = group.itemByKey(key) ?: return@forEachIndexed
                val enabled = state.isEnabled(key)
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = enabled,
                                role = Role.Checkbox,
                                onValueChange = { checked -> persist(state.withEnabled(key, checked)) },
                            ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = enabled, onCheckedChange = null)
                    Text(
                        text = stringResource(item.titleRes),
                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    IconButton(
                        enabled = index > 0,
                        onClick = { persist(state.moved(key, -1)) },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_keyboard_arrow_up_24dp),
                            contentDescription = stringResource(R.string.quick_access_move_up),
                        )
                    }
                    IconButton(
                        enabled = index < state.order.lastIndex,
                        onClick = { persist(state.moved(key, 1)) },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_keyboard_arrow_down_24dp),
                            contentDescription = stringResource(R.string.quick_access_move_down),
                        )
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
