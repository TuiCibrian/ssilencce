package com.ssilencce.ui.screen.player

import android.app.Dialog
import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ssilencce.R
import com.ssilencce.ui.theme.SsilencceTheme
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * Small Compose dialog that positions playback using a `mm:ss` value.
 */
class SeekToDialogFragment : DialogFragment() {
    private val playerViewModel: PlayerViewModel by activityViewModel()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext())
            .setView(
                ComposeView(requireContext()).apply {
                    setViewCompositionStrategy(
                        ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
                    )
                    setContent {
                        SsilencceTheme {
                            SeekToScreen(
                                initialText = SeekTimeParser.format(playerViewModel.progress),
                                duration = playerViewModel.duration,
                                onDismiss = { dismissAllowingStateLoss() },
                                onSeek = { input ->
                                    val parsed = SeekTimeParser.parse(input) ?: return@SeekToScreen false
                                    playerViewModel.seekTo(
                                        SeekTimeParser.clamp(parsed, playerViewModel.duration),
                                    )
                                    dismissAllowingStateLoss()
                                    true
                                },
                            )
                        }
                    }
                },
            ).create()

    companion object {
        const val TAG = "SeekToDialogFragment"

        fun show(fragmentManager: androidx.fragment.app.FragmentManager) {
            if (fragmentManager.isStateSaved) return
            SeekToDialogFragment().show(fragmentManager, TAG)
        }
    }
}

@Composable
private fun SeekToScreen(
    initialText: String,
    duration: Long,
    onDismiss: () -> Unit,
    onSeek: (String) -> Boolean,
) {
    var input by remember { mutableStateOf(initialText) }
    var invalid by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.seek_to_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            if (duration > 0) {
                Text(
                    text =
                        stringResource(
                            R.string.seek_to_duration_hint,
                            SeekTimeParser.format(duration),
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = input,
                onValueChange = {
                    input = it
                    invalid = false
                },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                label = { Text(stringResource(R.string.seek_to_hint)) },
                singleLine = true,
                isError = invalid,
                supportingText =
                    if (invalid) {
                        { Text(stringResource(R.string.seek_to_invalid)) }
                    } else {
                        null
                    },
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
                TextButton(
                    onClick = {
                        if (!onSeek(input)) invalid = true
                    },
                ) {
                    Text(stringResource(R.string.seek_to_confirm))
                }
            }
        }
    }
}
