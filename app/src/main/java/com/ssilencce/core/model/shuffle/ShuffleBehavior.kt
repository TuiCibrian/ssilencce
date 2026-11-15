package com.ssilencce.core.model.shuffle

/**
 * How a new shuffle order is built. [AllSongs] reorders every song of the queue (the historical
 * behavior); [BelowCurrent] keeps the songs up to the current one in their original order and
 * shuffles only the upcoming ones.
 */
enum class ShuffleBehavior(
    val value: String,
) {
    AllSongs("all_songs"),
    BelowCurrent("below_current"),
    ;

    companion object {
        fun fromValue(value: String?): ShuffleBehavior = entries.firstOrNull { it.value == value } ?: AllSongs
    }
}
