package com.ssilencce.core.model.equalizer

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import com.ssilencce.extensions.resolveActivity

/**
 * Hand-off contract with the system/OEM audio effect control panel.
 *
 * The panel owns the actual equalizer when the user selects the System engine; the app only opens
 * and closes the effect-control session so the panel can attach to the current playback session.
 */
object SystemEqualizer {
    private const val NO_SESSION_ID = 0

    fun isAvailable(context: Context): Boolean = context.packageManager.resolveActivity(displayPanelIntent()) != null

    fun displayPanelIntent(sessionId: Int = NO_SESSION_ID): Intent =
        Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
            if (sessionId > NO_SESSION_ID) {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
            }
            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        }

    fun sessionIntent(
        action: String,
        sessionId: Int,
        packageName: String,
    ): Intent =
        Intent(action).apply {
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            if (action == AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION) {
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
        }
}
