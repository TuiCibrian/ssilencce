package com.ssilencce.core.sort

import android.icu.text.Collator
import android.icu.text.RuleBasedCollator
import com.ssilencce.util.Preferences
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private val collators = ConcurrentHashMap<Locale, Collator>()

// Frozen
fun sortingCollator(locale: Locale = Locale.getDefault()): Collator =
    collators.computeIfAbsent(locale) {
        Collator
            .getInstance(it)
            .apply {
                strength = Collator.PRIMARY
                (this as? RuleBasedCollator)?.numericCollation = true
            }.freeze()
    }

fun <T> Iterable<T>.sortedByName(selector: (T) -> String): List<T> = sortedWith(compareBy(sortingCollator()) { selector(it) })

fun <T> Iterable<T>.sortedByMediaName(selector: (T) -> String): List<T> {
    val normalizer =
        IgnoreKeywordPolicy.normalizer(
            Preferences.ignoreArticlesWhenSorting,
            Locale.getDefault().language,
            Preferences.sortingIgnoreKeywords,
        )
    return sortedByName { normalizer.normalize(selector(it)) }
}
