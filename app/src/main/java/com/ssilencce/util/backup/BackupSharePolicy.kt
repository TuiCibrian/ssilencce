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

package com.ssilencce.util.backup

/**
 * Pure staging rules for the manual backup share copy. Kept free of Android imports so the
 * naming, size, containment and stale-file rules can be probed on the JVM.
 */
internal object BackupSharePolicy {
    const val MAX_SHARE_BYTES = 256L * 1024 * 1024
    const val MAX_NAME_CHARS = 120
    const val TEMP_PREFIX = ".backup-share-"
    const val TEMP_SUFFIX = ".tmp"

    private const val DEFAULT_NAME = "Backup"
    private const val SEPARATOR = '/'

    fun sanitizeDisplayName(
        rawName: String,
        extension: String,
    ): String {
        val cleaned =
            rawName
                .replace('/', '_')
                .replace('\\', '_')
                .replace("\u0000", "")
                .trim()
                .trimStart('.')
                .take(MAX_NAME_CHARS)
                .ifBlank { DEFAULT_NAME }
        val suffix = ".$extension"
        return if (cleaned.endsWith(suffix, ignoreCase = true)) cleaned else cleaned + suffix
    }

    fun isSizeValid(size: Long): Boolean = size in 1..MAX_SHARE_BYTES

    /**
     * Returns true only when [candidateCanonicalPath] is strictly inside
     * [directoryCanonicalPath], never equal to it.
     */
    fun isContained(
        candidateCanonicalPath: String,
        directoryCanonicalPath: String,
    ): Boolean {
        val root = directoryCanonicalPath.trimEnd(SEPARATOR)
        return candidateCanonicalPath.length > root.length &&
            candidateCanonicalPath.startsWith(root + SEPARATOR)
    }

    fun isTemporaryName(name: String): Boolean = name.startsWith(TEMP_PREFIX) && name.endsWith(TEMP_SUFFIX)

    fun staleFileNames(
        names: List<String>,
        keepName: String?,
    ): List<String> = names.filter { it != keepName }
}
