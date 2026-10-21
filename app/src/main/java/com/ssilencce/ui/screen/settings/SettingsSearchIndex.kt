/*
 * Copyright (c) 2024 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ssilencce.ui.screen.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.XmlResourceParser
import org.xmlpull.v1.XmlPullParser

data class SettingsSearchResult(
    val screen: SettingsScreen,
    val key: String?,
    val title: String,
    val summary: String?,
)

object SettingsSearchIndex {
    @SuppressLint("ResourceType")
    fun build(context: Context): List<SettingsSearchResult> =
        SettingsScreen.entries.flatMap { screen ->
            val parser = context.resources.getXml(screen.layoutRes)
            try {
                parseScreen(context, screen, parser)
            } finally {
                parser.close()
            }
        }

    fun filter(
        index: List<SettingsSearchResult>,
        query: String,
    ): List<SettingsSearchResult> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return emptyList()
        return index
            .filter { result ->
                result.title.contains(normalizedQuery, ignoreCase = true) ||
                    result.summary?.contains(normalizedQuery, ignoreCase = true) == true
            }.sortedWith(compareBy({ it.screen.ordinal }, { it.title }))
    }

    private fun parseScreen(
        context: Context,
        screen: SettingsScreen,
        parser: XmlResourceParser,
    ): List<SettingsSearchResult> {
        val results = mutableListOf<SettingsSearchResult>()
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                val title = parser.readTextAttribute(context, "title")
                if (title != null) {
                    results +=
                        SettingsSearchResult(
                            screen = screen,
                            key = parser.readStringAttribute("key"),
                            title = title,
                            summary = parser.readTextAttribute(context, "summary"),
                        )
                }
            }
            eventType = parser.next()
        }
        return results
    }

    private fun XmlResourceParser.readTextAttribute(
        context: Context,
        attributeName: String,
    ): String? {
        for (index in 0 until attributeCount) {
            if (getAttributeName(index) != attributeName) continue
            val resourceId = getAttributeResourceValue(index, 0)
            val value =
                if (resourceId != 0) {
                    runCatching { context.getString(resourceId) }.getOrNull()
                } else {
                    getAttributeValue(index)
                }
            return value?.trim()?.takeIf { it.isNotEmpty() }
        }
        return null
    }

    private fun XmlResourceParser.readStringAttribute(attributeName: String): String? {
        for (index in 0 until attributeCount) {
            if (getAttributeName(index) != attributeName) continue
            return getAttributeValue(index)?.trim()?.takeIf { it.isNotEmpty() }
        }
        return null
    }
}
