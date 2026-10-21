package com.ssilencce.playback

object Playback {
    // Custom commands
    const val TOGGLE_SHUFFLE = "com.ssilencce.command.shuffle.toggle"
    const val CYCLE_REPEAT = "com.ssilencce.command.repeat.cycle"
    const val TOGGLE_FAVORITE = "com.ssilencce.command.toggle_favorite"
    const val RESTORE_PLAYBACK = "com.ssilencce.command.restore_playback"
    const val CONFIRM_AUDIO_FOCUS = "com.ssilencce.command.confirm_audio_focus"
    const val CANCEL_AUDIO_FOCUS = "com.ssilencce.command.cancel_audio_focus"
    const val GET_AUDIO_FOCUS_STATE = "com.ssilencce.command.get_audio_focus_state"
    const val SWITCH_NAMED_QUEUE = "com.ssilencce.command.switch_named_queue"
    const val EXTRA_AUDIO_FOCUS_REQUEST_ID = "audio_focus_request_id"

    const val SET_UNSHUFFLED_ORDER = "com.ssilencce.command.set.unshuffled_order"
    const val SET_STOP_POSITION = "com.ssilencce.command.set.stop_position"

    // Custom events
    const val EVENT_MEDIA_CONTENT_CHANGED = "com.ssilencce.event.media_content_changed"
    const val EVENT_FAVORITE_CONTENT_CHANGED = "com.ssilencce.event.favorite_content_changed"
    const val EVENT_PLAYBACK_RESTORED = "com.ssilencce.event.playback_restored"
    const val EVENT_PLAYBACK_STARTED = "com.ssilencce.event.playback_started"
    const val EVENT_AUDIO_FOCUS_ASK = "com.ssilencce.event.audio_focus_ask"
}
