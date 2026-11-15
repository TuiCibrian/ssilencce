package com.ssilencce.data.local.replaygain

import java.util.Locale
import kotlin.math.round

/**
 * Canonical ReplayGain property names and format-specific mappings.
 *
 * TagLib exposes all of these as a property map; the exact key differs by container. Values are
 * kept as strings because that is what the tag writer and reader exchange.
 */
object ReplayGainTags {
    const val TRACK_GAIN = "REPLAYGAIN_TRACK_GAIN"
    const val ALBUM_GAIN = "REPLAYGAIN_ALBUM_GAIN"
    const val TRACK_PEAK = "REPLAYGAIN_TRACK_PEAK"
    const val ALBUM_PEAK = "REPLAYGAIN_ALBUM_PEAK"

    const val ITUNES_PREFIX = "----:com.apple.iTunes:"
    const val R128_TRACK_GAIN = "R128_TRACK_GAIN"
    const val R128_ALBUM_GAIN = "R128_ALBUM_GAIN"

    private val CANONICAL_SUFFIXES =
        listOf(TRACK_GAIN, ALBUM_GAIN, TRACK_PEAK, ALBUM_PEAK)

    fun isReplayGainKey(key: String): Boolean = suffixOf(key) != null

    fun suffixOf(key: String): String? {
        val normalized =
            if (key.startsWith(ITUNES_PREFIX, ignoreCase = true)) {
                key.substring(ITUNES_PREFIX.length)
            } else {
                key
            }
        return when (normalized.uppercase(Locale.ROOT)) {
            TRACK_GAIN, R128_TRACK_GAIN -> TRACK_GAIN
            ALBUM_GAIN, R128_ALBUM_GAIN -> ALBUM_GAIN
            TRACK_PEAK -> TRACK_PEAK
            ALBUM_PEAK -> ALBUM_PEAK
            else -> null
        }
    }

    /**
     * Keeps one canonical value per ReplayGain field, preferring the standard key, then the
     * iTunes freeform key, then an Opus R128 key. This removes duplicate aliases without dropping
     * the values when ordinary tags are edited.
     */
    fun canonicalProperties(tags: Map<String, Array<String>>): Map<String, Array<String>> {
        val result = LinkedHashMap<String, Array<String>>()
        for (suffix in CANONICAL_SUFFIXES) {
            val candidate =
                tags.entries
                    .filter { suffixOf(it.key) == suffix && it.value.isNotEmpty() }
                    .minByOrNull { aliasPriority(it.key) }
                    ?: continue
            result[candidate.key] = candidate.value
        }
        return result
    }

    fun standardProperties(
        trackGainDb: Float,
        trackPeak: Float,
        albumGainDb: Float?,
        albumPeak: Float?,
    ): Map<String, String> =
        buildMap {
            put(TRACK_GAIN, gainValue(trackGainDb))
            put(TRACK_PEAK, peakValue(trackPeak))
            albumGainDb?.let { put(ALBUM_GAIN, gainValue(it)) }
            albumPeak?.let { put(ALBUM_PEAK, peakValue(it)) }
        }

    /**
     * TagLib normalizes MP4 freeform atoms to the standard property names when reading and maps
     * those standard names back to `----:com.apple.iTunes:REPLAYGAIN_*` when saving. Passing the
     * prefixed keys directly makes TagLib return true without persisting the atoms, so the writer
     * deliberately uses the standard names for MP4/M4A.
     */
    fun mp4Properties(
        trackGainDb: Float,
        trackPeak: Float,
        albumGainDb: Float?,
        albumPeak: Float?,
    ): Map<String, String> = standardProperties(trackGainDb, trackPeak, albumGainDb, albumPeak)

    fun opusProperties(
        trackGainDb: Float,
        trackPeak: Float,
        albumGainDb: Float?,
        albumPeak: Float?,
    ): Map<String, String> =
        buildMap {
            put(R128_TRACK_GAIN, r128Value(trackGainDb))
            put(TRACK_PEAK, peakValue(trackPeak))
            albumGainDb?.let { put(R128_ALBUM_GAIN, r128Value(it)) }
            albumPeak?.let { put(ALBUM_PEAK, peakValue(it)) }
        }

    fun gainValue(gainDb: Float): String = String.format(Locale.ROOT, "%.2f dB", gainDb)

    fun peakValue(peak: Float): String = String.format(Locale.ROOT, "%.8f", peak)

    fun r128Value(gainDb: Float): String = round((gainDb - 5f) * 256f).toInt().toString()

    fun aliasPriority(key: String): Int =
        when {
            key.equals(R128_TRACK_GAIN, ignoreCase = true) ||
                key.equals(R128_ALBUM_GAIN, ignoreCase = true) -> 2

            key.startsWith(ITUNES_PREFIX, ignoreCase = true) -> 1

            else -> 0
        }
}
