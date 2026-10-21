package com.ssilencce.ui.dialogs

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.ssilencce.R
import com.ssilencce.ui.theme.SsilencceTheme

class AudioFocusAskDialog : DialogFragment() {
    val requestId: String? get() = arguments?.getString(ARG_REQUEST_ID)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SsilencceTheme {
                    Surface(shape = MaterialTheme.shapes.extraLarge) {
                        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                stringResource(R.string.audio_focus_ask_title),
                                modifier = Modifier.semantics { heading() },
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Text(stringResource(R.string.audio_focus_ask_message))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { submit(false) }) {
                                    Text(stringResource(R.string.action_cancel))
                                }
                                TextButton(onClick = { submit(true) }) {
                                    Text(stringResource(R.string.audio_focus_ask_positive))
                                }
                            }
                        }
                    }
                }
            }
        }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            val margin = (48 * resources.displayMetrics.density).toInt()
            setLayout(resources.displayMetrics.widthPixels - margin, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        submitResult(false)
        super.onCancel(dialog)
    }

    private fun submit(confirm: Boolean) {
        submitResult(confirm)
        dismiss()
    }

    private fun submitResult(confirm: Boolean) {
        parentFragmentManager.setFragmentResult(
            RESULT_KEY,
            bundleOf(ARG_REQUEST_ID to requestId, ARG_CONFIRM to confirm),
        )
    }

    companion object {
        const val TAG = "AudioFocusAsk"
        const val RESULT_KEY = "audio_focus_ask_result"
        const val ARG_REQUEST_ID = "request_id"
        const val ARG_CONFIRM = "confirm"

        fun newInstance(requestId: String) = AudioFocusAskDialog().apply { arguments = bundleOf(ARG_REQUEST_ID to requestId) }
    }
}
