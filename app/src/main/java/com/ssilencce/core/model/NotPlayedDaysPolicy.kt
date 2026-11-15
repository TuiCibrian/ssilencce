package com.ssilencce.core.model

/**
 * Pure rules for the Not Recently Played threshold.
 *
 * A value of [USE_HISTORY_INTERVAL] keeps the existing history-cutoff behavior. Positive values are
 * a custom number of days, clamped to [MIN_DAYS]..[MAX_DAYS].
 */
object NotPlayedDaysPolicy {
    const val USE_HISTORY_INTERVAL = 0
    const val MIN_DAYS = 1
    const val MAX_DAYS = 3650

    private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

    fun normalizeDays(days: Int): Int =
        if (days <= USE_HISTORY_INTERVAL) {
            USE_HISTORY_INTERVAL
        } else {
            days.coerceAtMost(MAX_DAYS)
        }

    fun parseDays(input: String): Int? = input.trim().toIntOrNull()?.takeIf { it in MIN_DAYS..MAX_DAYS }

    fun cutoffMillis(
        nowMillis: Long,
        days: Int,
    ): Long = nowMillis - normalizeDays(days).coerceAtLeast(MIN_DAYS) * MILLIS_PER_DAY

    fun effectiveCutoffMillis(
        nowMillis: Long,
        days: Int,
        historyCutoffMillis: Long,
    ): Long =
        if (normalizeDays(days) == USE_HISTORY_INTERVAL) {
            historyCutoffMillis
        } else {
            cutoffMillis(nowMillis, days)
        }
}
