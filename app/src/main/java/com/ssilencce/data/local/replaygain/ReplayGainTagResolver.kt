package com.ssilencce.data.local.replaygain

import com.ssilencce.data.model.replaygain.ReplayGain

/**
 * Parses ReplayGain tags into nullable values.
 *
 * Standard tags win over iTunes freeform tags, which win over Opus R128 tags. A malformed or blank
 * value is treated as missing instead of a measured 0 dB / 1.0 peak. For a field with several
 * aliases, the first alias that yields a valid value wins; a broken higher-priority alias never
 * hides a valid lower-priority one.
 */
object ReplayGainTagResolver {
    fun resolve(tags: Map<String, Array<String>>): ReplayGain {
        val candidates = mutableMapOf<String, Candidate>()
        for ((key, values) in tags) {
            val suffix = ReplayGainTags.suffixOf(key) ?: continue
            val priority = ReplayGainTags.aliasPriority(key)
            val value = values.firstNotNullOfOrNull { parseValue(suffix, key, it) } ?: continue
            val candidate = Candidate(priority = priority, key = key, value = value)
            val current = candidates[suffix]
            if (current == null || candidate.isPreferredOver(current)) {
                candidates[suffix] = candidate
            }
        }
        return ReplayGain(
            albumGain = candidates[ReplayGainTags.ALBUM_GAIN]?.value,
            trackGain = candidates[ReplayGainTags.TRACK_GAIN]?.value,
            albumPeak = candidates[ReplayGainTags.ALBUM_PEAK]?.value,
            trackPeak = candidates[ReplayGainTags.TRACK_PEAK]?.value,
        )
    }

    fun parseGain(raw: String?): Float? = parseNumber(raw)?.takeIf { it.isFinite() }

    fun parsePeak(raw: String?): Float? = parseNumber(raw)?.takeIf { it.isFinite() && it >= 0f }

    fun parseR128Gain(raw: String?): Float? =
        raw
            ?.trim()
            ?.toIntOrNull()
            ?.let { (it / 256f) + 5f }
            ?.takeIf { it.isFinite() }

    private fun parseValue(
        suffix: String,
        key: String,
        raw: String,
    ): Float? =
        when {
            key.equals(ReplayGainTags.R128_TRACK_GAIN, ignoreCase = true) ||
                key.equals(ReplayGainTags.R128_ALBUM_GAIN, ignoreCase = true) -> parseR128Gain(raw)

            suffix == ReplayGainTags.TRACK_PEAK || suffix == ReplayGainTags.ALBUM_PEAK -> parsePeak(raw)

            else -> parseGain(raw)
        }

    private fun parseNumber(raw: String?): Float? {
        val cleaned =
            raw
                ?.trim()
                ?.replace("dB", "", ignoreCase = true)
                ?.trim()
                ?: return null
        if (!NUMBER_PATTERN.matches(cleaned)) return null
        return cleaned.toFloatOrNull()
    }

    private data class Candidate(
        val priority: Int,
        val key: String,
        val value: Float,
    ) {
        fun isPreferredOver(other: Candidate): Boolean = priority < other.priority || (priority == other.priority && key < other.key)
    }

    private val NUMBER_PATTERN = Regex("^[-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)$")
}
