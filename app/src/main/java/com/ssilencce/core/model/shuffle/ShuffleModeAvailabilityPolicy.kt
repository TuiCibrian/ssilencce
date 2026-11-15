package com.ssilencce.core.model.shuffle

/**
 * Platform gate for shuffle modes that depend on composer metadata.
 *
 * Composer tags are only exposed by the library on Android 11 (API 30) and later, matching the
 * Composers category. Album-artist modes remain available on every supported API.
 */
object ShuffleModeAvailabilityPolicy {
    const val COMPOSER_MIN_SDK = 30

    fun isAvailable(
        composerDependent: Boolean,
        sdkInt: Int,
    ): Boolean = !composerDependent || sdkInt >= COMPOSER_MIN_SDK

    fun <T> available(
        items: Iterable<T>,
        sdkInt: Int,
        composerDependent: (T) -> Boolean,
    ): List<T> = items.filter { isAvailable(composerDependent(it), sdkInt) }
}
