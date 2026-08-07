package com.ssilencce.playback

object Playback {
    // Custom commands
    const val TOGGLE_SHUFFLE = "com.ssilencce.command.shuffle.toggle"
    const val CYCLE_REPEAT = "com.ssilencce.command.repeat.cycle"
    const val TOGGLE_FAVORITE = "com.ssilencce.command.toggle_favorite"
    const val RESTORE_PLAYBACK = "com.ssilencce.command.restore_playback"

    const val SET_UNSHUFFLED_ORDER = "com.ssilencce.command.set.unshuffled_order"
    const val SET_STOP_POSITION = "com.ssilencce.command.set.stop_position"

    // Custom events
    const val EVENT_MEDIA_CONTENT_CHANGED = "com.ssilencce.event.media_content_changed"
    const val EVENT_FAVORITE_CONTENT_CHANGED = "com.ssilencce.event.favorite_content_changed"
    const val EVENT_PLAYBACK_RESTORED = "com.ssilencce.event.playback_restored"
    const val EVENT_PLAYBACK_STARTED = "com.ssilencce.event.playback_started"
}
