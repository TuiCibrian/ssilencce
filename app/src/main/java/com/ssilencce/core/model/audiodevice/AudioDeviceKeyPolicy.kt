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

package com.ssilencce.core.model.audiodevice

/**
 * Turns an OS-provided identity into an opaque, export-safe device key.
 *
 * Raw Bluetooth addresses/product names are never stored in profiles or shown in the UI. A missing
 * identity returns null so callers fall back to the broad [AudioDeviceType] association.
 */
object AudioDeviceKeyPolicy {
    private val HEX = "0123456789abcdef".toCharArray()
    private const val KEY_BYTES = 12

    fun opaqueKey(
        typeName: String,
        identity: String?,
    ): String? {
        val normalizedIdentity = identity?.takeIf { it.isNotBlank() } ?: return null
        val digest =
            java.security.MessageDigest
                .getInstance("SHA-256")
                .digest("$typeName:$normalizedIdentity".toByteArray(Charsets.UTF_8))
        val suffix =
            buildString(KEY_BYTES * 2) {
                repeat(KEY_BYTES) { index ->
                    val value = digest[index].toInt() and 0xFF
                    append(HEX[value ushr 4])
                    append(HEX[value and 0x0F])
                }
            }
        return "$typeName:$suffix"
    }
}
