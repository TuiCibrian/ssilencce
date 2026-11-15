package com.ssilencce.data.model.replaygain

enum class ReplayGainMode {
    Album,
    Track,
    Off,
    ;

    val isOn get() = this == Album || this == Track
}

/**
 * Null means "no tag present"; a non-null 0 dB gain is a real measurement and must not fall back
 * to the without-tag preamp.
 */
data class ReplayGain(
    val albumGain: Float?,
    val trackGain: Float?,
    val albumPeak: Float?,
    val trackPeak: Float?,
) {
    val hasAlbumGain: Boolean get() = albumGain != null
    val hasTrackGain: Boolean get() = trackGain != null
    val hasAlbumPeak: Boolean get() = albumPeak != null
    val hasTrackPeak: Boolean get() = trackPeak != null

    companion object {
        val Empty = ReplayGain(null, null, null, null)
    }
}
