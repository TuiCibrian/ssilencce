package com.ssilencce.playback

object Playback {
    // Custom commands
    const val TOGGLE_SHUFFLE = "com.ssilencce.command.shuffle.toggle"
    const val RESHUFFLE_QUEUE = "com.ssilencce.command.shuffle.reshuffle_queue"
    const val GET_SHUFFLE_STATE = "com.ssilencce.command.shuffle.get_state"
    const val CYCLE_REPEAT = "com.ssilencce.command.repeat.cycle"
    const val TOGGLE_FAVORITE = "com.ssilencce.command.toggle_favorite"
    const val SEEK_FORWARD = "com.ssilencce.command.seek_forward"
    const val SEEK_BACK = "com.ssilencce.command.seek_back"
    const val ADD_TO_PLAYLIST = "com.ssilencce.command.add_to_playlist"
    const val OPEN_GHOST_SEARCH = "com.ssilencce.command.open_ghost_search"
    const val CLOSE_NOTIFICATION = "com.ssilencce.command.close_notification"
    const val RESTORE_PLAYBACK = "com.ssilencce.command.restore_playback"
    const val CONFIRM_AUDIO_FOCUS = "com.ssilencce.command.confirm_audio_focus"
    const val CANCEL_AUDIO_FOCUS = "com.ssilencce.command.cancel_audio_focus"
    const val GET_AUDIO_FOCUS_STATE = "com.ssilencce.command.get_audio_focus_state"
    const val SWITCH_NAMED_QUEUE = "com.ssilencce.command.switch_named_queue"
    const val MOVE_QUEUE_BLOCK = "com.ssilencce.command.move_queue_block"
    const val EXTRA_AUDIO_FOCUS_REQUEST_ID = "audio_focus_request_id"
    const val EXTRA_MOVE_FROM_INDICES = "move_from_indices"
    const val EXTRA_MOVE_TO_INDICES = "move_to_indices"

    const val SET_UNSHUFFLED_ORDER = "com.ssilencce.command.set.unshuffled_order"
    const val SET_STOP_POSITION = "com.ssilencce.command.set.stop_position"
    const val SET_AB_REPEAT = "com.ssilencce.command.set.ab_repeat"
    const val GET_AB_REPEAT = "com.ssilencce.command.get.ab_repeat"
    const val SET_SONG_PLAYBACK_PARAMS = "com.ssilencce.command.set.song_playback_params"
    const val RESET_SONG_PLAYBACK_PARAMS = "com.ssilencce.command.reset.song_playback_params"

    const val EXTRA_AB_ACTIVE = "ab_active"
    const val EXTRA_AB_START_MS = "ab_start_ms"
    const val EXTRA_AB_END_MS = "ab_end_ms"
    const val EXTRA_SONG_ID = "song_id"
    const val EXTRA_SONG_SPEED = "song_speed"
    const val EXTRA_SONG_PITCH = "song_pitch"

    // Custom events
    const val EVENT_MEDIA_CONTENT_CHANGED = "com.ssilencce.event.media_content_changed"
    const val EVENT_FAVORITE_CONTENT_CHANGED = "com.ssilencce.event.favorite_content_changed"
    const val EVENT_PLAYBACK_RESTORED = "com.ssilencce.event.playback_restored"
    const val EVENT_PLAYBACK_STARTED = "com.ssilencce.event.playback_started"
    const val EVENT_AUDIO_FOCUS_ASK = "com.ssilencce.event.audio_focus_ask"
    const val EVENT_REPEAT_LOAD_NEXT_PAUSE_CHANGED = "com.ssilencce.event.repeat_load_next_pause_changed"
    const val EVENT_SHUFFLE_STATE_CHANGED = "com.ssilencce.event.shuffle_state_changed"
    const val EXTRA_LOAD_NEXT_PAUSE_ACTIVE = "load_next_pause_active"
    const val EXTRA_SHUFFLE_STATE = "shuffle_state"
}
