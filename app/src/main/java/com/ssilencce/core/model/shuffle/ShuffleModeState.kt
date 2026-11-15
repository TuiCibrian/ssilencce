package com.ssilencce.core.model.shuffle

/**
 * The persisted shuffle model. [Shuffle] keeps a fixed shuffled order, while [Randomize] picks a
 * random next song on every transition without building an order.
 */
enum class ShuffleModeState(
    val value: String,
) {
    Off("off"),
    Shuffle("shuffle"),
    Randomize("randomize"),
    ;

    val isEnabled: Boolean
        get() = this != Off

    companion object {
        fun fromValue(value: String?): ShuffleModeState? = entries.firstOrNull { it.value == value }
    }
}
