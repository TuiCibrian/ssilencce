package com.ssilencce.ui.screen.player

/** Active in-track loop range, in milliseconds from the start of the current song. */
data class AbRepeatState(
    val startMs: Long,
    val endMs: Long,
)

/** Result of advancing the A → B → clear cycle from the player menu. */
sealed interface AbRepeatToggleResult {
    data class PointASet(
        val positionMs: Long,
    ) : AbRepeatToggleResult

    data class RangeSet(
        val startMs: Long,
        val endMs: Long,
    ) : AbRepeatToggleResult

    data object Cleared : AbRepeatToggleResult

    data object TooClose : AbRepeatToggleResult

    data object Unavailable : AbRepeatToggleResult
}

/** Smallest accepted A→B distance; shorter ranges are treated as an accidental second tap. */
const val MIN_AB_REPEAT_RANGE_MS = 500L
