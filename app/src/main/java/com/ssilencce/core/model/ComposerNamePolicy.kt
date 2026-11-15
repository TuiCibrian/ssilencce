package com.ssilencce.core.model

import java.util.Locale

object ComposerNamePolicy {
    private val whitespace = Regex("\\s+")

    fun normalize(name: String?): String? =
        name
            ?.trim()
            ?.replace(whitespace, " ")
            ?.takeIf { it.isNotEmpty() }

    fun key(name: String?): String? = normalize(name)?.lowercase(Locale.ROOT)

    fun matches(
        left: String?,
        right: String?,
    ): Boolean {
        val leftKey = key(left) ?: return false
        return leftKey == key(right)
    }

    fun stableId(name: String?): Long {
        val key = key(name) ?: return -1L
        return key.hashCode().toLong() and 0xFFFFFFFFL
    }
}
