package com.ssilencce.ui.screen.player.screencast

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssilencce.ui.theme.SsilencceTheme
import org.koin.android.ext.android.inject

/**
 * Screencast toggle and external-display picker.
 */
class ScreencastPickerDialogFragment : DialogFragment() {
    private val screencastManager: ScreencastManager by inject()

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
                    val displays by screencastManager.displays.collectAsStateWithLifecycle()
                    val activeDisplayId by screencastManager.activeDisplayId.collectAsStateWithLifecycle()
                    ScreencastPickerScreen(
                        displays = displays,
                        activeDisplayId = activeDisplayId,
                        onToggle = { enabled ->
                            if (enabled) {
                                if (!screencastManager.startFirstAvailable()) {
                                    screencastManager.showDisplayChooser(requireActivity())
                                }
                            } else {
                                screencastManager.stop()
                            }
                        },
                        onSelectDisplay = { displayId -> screencastManager.startDisplay(displayId) },
                        onFindDisplays = { screencastManager.showDisplayChooser(requireActivity()) },
                        onOpenSettings = { screencastManager.openDisplaySettings(requireActivity()) },
                        onClose = { dismissAllowingStateLoss() },
                    )
                }
            }
        }

    override fun onResume() {
        super.onResume()
        screencastManager.refreshDisplays()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                (resources.displayMetrics.widthPixels * 0.92f).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
    }

    companion object {
        const val TAG = "ScreencastPickerDialogFragment"

        fun newInstance() = ScreencastPickerDialogFragment()
    }
}
