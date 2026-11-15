/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.ssilencce.ui.component.base

/**
 * Pure, in-memory matching rules shared by the detail-list quick-search hosts. No Android or
 * repository dependency: fragments filter their already-loaded snapshots and never query again.
 */
object QuickSearchPolicy {
    fun matches(
        query: String,
        fields: Iterable<String?>,
    ): Boolean = query.isBlank() || fields.any { it?.contains(query, ignoreCase = true) == true }

    fun matches(
        query: String,
        vararg fields: String?,
    ): Boolean = matches(query, fields.asIterable())

    /**
     * True only for a real no-match: a non-blank query, a non-empty source and zero visible rows.
     * A genuinely empty source keeps its normal empty state.
     */
    fun isNoMatch(
        query: String,
        sourceCount: Int,
        visibleCount: Int,
    ): Boolean = query.isNotBlank() && sourceCount > 0 && visibleCount == 0
}
