package com.ssilencce.core.sort

import java.util.Locale

object IgnoreKeywordPolicy {
    const val MAX_KEYWORD_LENGTH = 40
    const val MAX_KEYWORD_COUNT = 50

    private val whitespaceRegex = Regex("\\s+")

    private val articlesByLanguage =
        mapOf(
            "en" to listOf("the", "a", "an"),
            "es" to listOf("el", "la", "los", "las", "un", "una"),
            "fr" to listOf("le", "la", "les", "un", "une"),
            "de" to listOf("der", "die", "das", "ein", "eine"),
            "it" to listOf("il", "lo", "la", "l’", "i", "gli", "un", "una"),
            "pt" to listOf("o", "a", "os", "as", "um", "uma"),
            "nl" to listOf("de", "het", "een"),
        )

    internal fun articlesFor(language: String): List<String> = articlesByLanguage[language].orEmpty()

    fun normalizeKeyword(raw: String): String = raw.trim().replace(whitespaceRegex, " ")

    fun sanitize(keywords: Iterable<String>): List<String> {
        val result = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        for (raw in keywords) {
            val keyword = normalizeKeyword(raw)
            if (keyword.isEmpty() || keyword.length > MAX_KEYWORD_LENGTH) continue
            if (seen.add(keyword.lowercase(Locale.ROOT))) {
                result += keyword
                if (result.size == MAX_KEYWORD_COUNT) break
            }
        }
        return result
    }

    fun add(
        existing: Collection<String>,
        raw: String,
    ): IgnoreKeywordAddResult {
        val keyword = normalizeKeyword(raw)
        if (keyword.isEmpty()) return IgnoreKeywordAddResult.Blank
        if (keyword.length > MAX_KEYWORD_LENGTH) return IgnoreKeywordAddResult.TooLong
        val current = sanitize(existing)
        if (current.any { it.equals(keyword, ignoreCase = true) }) {
            return IgnoreKeywordAddResult.Duplicate
        }
        if (current.size >= MAX_KEYWORD_COUNT) return IgnoreKeywordAddResult.Full
        return IgnoreKeywordAddResult.Added(current + keyword)
    }

    fun remove(
        existing: Collection<String>,
        keyword: String,
    ): List<String> = sanitize(existing).filterNot { it.equals(keyword, ignoreCase = true) }

    fun normalizer(
        ignoreArticles: Boolean,
        language: String,
        customKeywords: Collection<String>,
    ): SortNormalizer =
        SortNormalizer(
            ignoreArticles = ignoreArticles,
            language = language,
            customKeywords = customKeywords,
        )
}

sealed interface IgnoreKeywordAddResult {
    data class Added(
        val keywords: List<String>,
    ) : IgnoreKeywordAddResult

    data object Blank : IgnoreKeywordAddResult

    data object TooLong : IgnoreKeywordAddResult

    data object Duplicate : IgnoreKeywordAddResult

    data object Full : IgnoreKeywordAddResult
}

class SortNormalizer(
    ignoreArticles: Boolean,
    language: String,
    customKeywords: Collection<String>,
) {
    private val leadingKeywordRegex: Regex? =
        if (ignoreArticles) {
            val keywords =
                (IgnoreKeywordPolicy.articlesFor(language) + IgnoreKeywordPolicy.sanitize(customKeywords))
                    .distinctBy { it.lowercase(Locale.ROOT) }
                    .sortedByDescending { it.length }
            if (keywords.isEmpty()) {
                null
            } else {
                Regex(
                    "^(${keywords.joinToString("|") { Regex.escape(it) }})\\s+",
                    RegexOption.IGNORE_CASE,
                )
            }
        } else {
            null
        }

    fun normalize(value: String): String {
        val regex = leadingKeywordRegex ?: return value
        return value.trim().replace(regex, "")
    }
}
