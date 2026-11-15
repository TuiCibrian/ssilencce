/*
 * Copyright (c) 2026 Christians Martínez Alvarado
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

package com.ssilencce.util

import android.provider.MediaStore.Audio.AudioColumns
import android.util.Base64
import com.ssilencce.data.local.room.SongEntity
import com.ssilencce.data.model.Song
import com.ssilencce.extensions.files.getCanonicalPathSafe
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Ghost mode hides the songs stored inside user-selected folders from every library surface while
 * keeping them reachable through the dedicated ghost search screen.
 *
 * The folder list and the optional PIN live in preferences, so no database migration is required.
 * The PIN is only an access barrier for the ghost search screen: it does not encrypt the files or
 * the preference values.
 */
object GhostMode {
    data class Selection(
        val selection: String,
        val arguments: Array<String>,
    )

    private const val PIN_ITERATIONS = 10_000
    private const val PIN_SALT_BYTES = 16

    private val secureRandom = SecureRandom()

    @Volatile
    private var sessionUnlocked = false

    val isEnabled: Boolean
        get() = Preferences.ghostModeEnabled

    val folderPaths: List<String>
        get() = Preferences.ghostFolders

    val hasFolders: Boolean
        get() = folderPaths.isNotEmpty()

    val isFilteringActive: Boolean
        get() = isEnabled && hasFolders

    val hasPin: Boolean
        get() = !Preferences.ghostPinHash.isNullOrEmpty() && !Preferences.ghostPinSalt.isNullOrEmpty()

    val isLocked: Boolean
        get() = hasPin && !sessionUnlocked

    fun setEnabled(enabled: Boolean) {
        Preferences.ghostModeEnabled = enabled
    }

    /**
     * Canonicalizes a folder path before storing it. Folder paths are matched by prefix, so the
     * canonical form keeps the filter stable across navigation and storage aliases.
     */
    fun normalizeFolderPath(path: String): String = File(path).getCanonicalPathSafe().trimEnd('/')

    fun addFolder(path: String): Boolean {
        if (path.isBlank()) return false
        val normalized = normalizeFolderPath(path)
        if (normalized.isEmpty()) return false
        val folders = folderPaths
        if (normalized in folders) return false
        Preferences.ghostFolders = (folders + normalized).sorted()
        // Choosing a folder is an explicit request to hide it; enabling here avoids the confusing
        // state where the folder is listed as ghost content but still visible everywhere.
        setEnabled(true)
        return true
    }

    fun removeFolder(path: String): Boolean {
        val normalized = normalizeFolderPath(path)
        val folders = folderPaths
        if (normalized !in folders) return false
        Preferences.ghostFolders = folders.filterNot { it == normalized }
        return true
    }

    fun isGhostFolder(path: String): Boolean {
        val normalized = normalizeFolderPath(path)
        return normalized in folderPaths
    }

    fun isGhostPath(path: String?): Boolean {
        if (!isFilteringActive || path.isNullOrEmpty()) return false
        val normalized = path.replace('\\', '/')
        return folderPaths.any { folder ->
            normalized == folder || normalized.startsWith("$folder/")
        }
    }

    fun isGhostSong(song: Song): Boolean = isGhostPath(song.data)

    /**
     * Selection that removes ghost folders from a MediaStore audio query.
     */
    fun exclusionSelection(): Selection? = buildFolderSelection(negated = true)

    /**
     * Selection that only keeps songs stored inside ghost folders.
     */
    fun inclusionSelection(): Selection? = buildFolderSelection(negated = false)

    fun filterSongs(songs: List<Song>): List<Song> = if (isFilteringActive) songs.filterNot(::isGhostSong) else songs

    fun filterSongEntities(songs: List<SongEntity>): List<SongEntity> = if (isFilteringActive) songs.filterNot { isGhostPath(it.data) } else songs

    fun setPin(pin: String) {
        val salt = ByteArray(PIN_SALT_BYTES).also(secureRandom::nextBytes)
        Preferences.ghostPinSalt = Base64.encodeToString(salt, Base64.NO_WRAP)
        Preferences.ghostPinHash = Base64.encodeToString(hashPin(pin, salt), Base64.NO_WRAP)
        sessionUnlocked = true
    }

    fun verifyPin(pin: String): Boolean {
        val salt = Preferences.ghostPinSalt?.let(::decodeBase64) ?: return false
        val expected = Preferences.ghostPinHash?.let(::decodeBase64) ?: return false
        val actual = hashPin(pin, salt)
        return MessageDigest.isEqual(expected, actual)
    }

    fun unlock(pin: String): Boolean {
        if (!hasPin) {
            sessionUnlocked = true
            return true
        }
        val valid = verifyPin(pin)
        if (valid) sessionUnlocked = true
        return valid
    }

    fun clearPin() {
        Preferences.ghostPinHash = null
        Preferences.ghostPinSalt = null
        sessionUnlocked = true
    }

    fun lock() {
        if (hasPin) sessionUnlocked = false
    }

    private fun buildFolderSelection(negated: Boolean): Selection? {
        val folders = folderPaths
        if (folders.isEmpty()) return null
        val operator = if (negated) "NOT LIKE" else "LIKE"
        val clauses = folders.map { "${AudioColumns.DATA} $operator ? ESCAPE '\\'" }
        val errors = folders.map { escapeLike("$it/%") }.toTypedArray()
        val joined = clauses.joinToString(if (negated) " AND " else " OR ")
        return Selection("($joined)", errors)
    }

    private fun escapeLike(value: String): String =
        value
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")

    private fun hashPin(
        pin: String,
        salt: ByteArray,
    ): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        var result = digest.digest(pin.toByteArray(Charsets.UTF_8))
        repeat(PIN_ITERATIONS - 1) {
            digest.reset()
            digest.update(salt)
            result = digest.digest(result)
        }
        return result
    }

    private fun decodeBase64(value: String): ByteArray? = runCatching { Base64.decode(value, Base64.NO_WRAP) }.getOrNull()
}
