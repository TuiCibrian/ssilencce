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

package com.ssilencce.core.model.equalizer

/**
 * Pure exact-device/type matching for equalizer profile associations.
 */
object EqOutputAssociationPolicy {
    data class Association(
        val typeNames: Set<String>,
        val deviceKeys: Set<String>,
    )

    data class Profile(
        val name: String,
        val association: Association,
    )

    fun matches(
        association: Association,
        deviceTypeName: String,
        deviceKey: String?,
    ): Boolean =
        (deviceKey != null && association.deviceKeys.contains(deviceKey)) ||
            association.typeNames.contains(deviceTypeName)

    /**
     * Exact device key wins; otherwise the broad output type is used. Null means no associated
     * profile, so callers preserve the current profile.
     */
    fun resolve(
        profiles: List<Profile>,
        deviceTypeName: String,
        deviceKey: String?,
    ): String? {
        if (deviceKey != null) {
            profiles.firstOrNull { it.association.deviceKeys.contains(deviceKey) }?.let {
                return it.name
            }
        }
        return profiles.firstOrNull { it.association.typeNames.contains(deviceTypeName) }?.name
    }

    /** Removes the target profile's type/device claims from another profile. */
    fun withoutOverlaps(
        target: Association,
        other: Association,
    ): Association =
        Association(
            typeNames = other.typeNames - target.typeNames,
            deviceKeys = other.deviceKeys - target.deviceKeys,
        )
}
