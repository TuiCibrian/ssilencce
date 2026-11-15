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

package com.ssilencce.data.local.notes

import android.content.Context
import com.ssilencce.data.model.Song
import com.ssilencce.data.model.notes.SongBookmark
import com.ssilencce.data.model.notes.SongNotes
import com.ssilencce.data.model.notes.SongNotesCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Stores songs notes as individual UTF-8 text files in a user-visible app-specific external
 * directory. Files are named `<song id> - <title>.txt`; the song id keeps the association stable
 * when a tag edit changes the title (the file is renamed on the next save).
 */
class SongNotesRepository(
    private val context: Context,
) {
    private val writeMutex = Mutex()

    fun notesDirectory(): File {
        val directory = context.getExternalFilesDir(NOTES_DIRECTORY) ?: File(context.filesDir, NOTES_DIRECTORY)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    /** Backs up only the files this app can read back as song notes. */
    fun noteFiles(): List<File> =
        notesDirectory()
            .listFiles { file -> file.isFile && isNoteFileName(file.name) }
            ?.sortedBy { it.name }
            .orEmpty()

    /** True for files this app can read back as song notes (`<id> - <title>.txt`). */
    fun isNoteFileName(fileName: String): Boolean =
        fileName.substringAfterLast('.', "").equals(TXT_EXTENSION, ignoreCase = true) &&
            songIdOf(fileName) != null

    /** Returns null when an existing file cannot be read; an absent file is empty notes. */
    suspend fun load(song: Song): SongNotes? = withContext(Dispatchers.IO) { read(song) }

    suspend fun save(
        song: Song,
        notes: String,
        bookmarks: List<SongBookmark>,
    ): SongNotes? =
        withContext(Dispatchers.IO) {
            writeMutex.withLock { write(song, notes, bookmarks) }
        }

    /**
     * Convenience for one-tap bookmarks outside the notes screen. Reads the current file so a
     * bookmark never overwrites notes written by another flow.
     */
    suspend fun addBookmark(
        song: Song,
        timestampMs: Long,
        text: String,
    ): Boolean =
        withContext(Dispatchers.IO) {
            writeMutex.withLock {
                val current = read(song) ?: return@withLock false
                write(song, current.notes, current.bookmarks + SongBookmark(timestampMs, text)) != null
            }
        }

    private fun read(song: Song): SongNotes? {
        if (song.id <= 0L) return SongNotes(song.id)
        val file = findFile(song.id) ?: return SongNotes(song.id)
        return try {
            val content = SongNotesCodec.decode(file.readText())
            SongNotes(song.id, content.notes, content.bookmarks)
        } catch (error: IOException) {
            null
        } catch (error: SecurityException) {
            null
        }
    }

    private fun write(
        song: Song,
        notes: String,
        bookmarks: List<SongBookmark>,
    ): SongNotes? {
        if (song.id <= 0L) return null
        val directory = notesDirectory()
        if (!directory.isDirectory && !directory.mkdirs()) return null
        val existing = findFile(song.id)
        val normalizedNotes = notes.trim('\n', '\r').let { if (it.isBlank()) "" else it }
        val normalizedBookmarks =
            bookmarks
                .map { SongBookmark(it.positionMs, it.text.replace(Regex("\\s+"), " ").trim()) }
                .sortedBy { it.positionMs }
        if (normalizedNotes.isBlank() && normalizedBookmarks.isEmpty()) {
            val deleted = existing == null || existing.delete() || !existing.exists()
            return if (deleted) SongNotes(song.id) else null
        }
        val target = File(directory, fileNameFor(song))
        val content =
            SongNotesCodec.encode(
                songId = song.id,
                title = song.title,
                artist = song.artistName,
                notes = normalizedNotes,
                bookmarks = normalizedBookmarks,
            )
        if (!writeAtomically(target, content)) return null
        if (existing != null && existing.absolutePath != target.absolutePath) {
            // A previous title produced a different file name; keep only the current one.
            existing.delete()
        }
        return SongNotes(song.id, normalizedNotes, normalizedBookmarks)
    }

    private fun findFile(songId: Long): File? =
        notesDirectory()
            .listFiles { file ->
                file.isFile && file.extension.equals(TXT_EXTENSION, ignoreCase = true) && songIdOf(file.name) == songId
            }?.sortedByDescending { it.lastModified() }
            ?.firstOrNull()

    private fun fileNameFor(song: Song): String {
        val title = safeTitle(song.title)
        return if (title.isEmpty()) {
            "${song.id}.$TXT_EXTENSION"
        } else {
            "${song.id} - $title.$TXT_EXTENSION"
        }
    }

    private fun safeTitle(title: String): String =
        title
            .replace(INVALID_FILE_NAME_CHARACTERS, "_")
            .replace(WHITESPACE, " ")
            .trim(' ', '.')
            .take(MAX_TITLE_LENGTH)
            .trim(' ', '.')

    private fun songIdOf(fileName: String): Long? {
        val baseName = fileName.substringBeforeLast('.')
        return baseName
            .substringBefore(" - ")
            .trim()
            .toLongOrNull()
            ?.takeIf { it > 0L }
    }

    private fun writeAtomically(
        target: File,
        content: String,
    ): Boolean {
        val temporary = File(target.parentFile, ".${target.name}.tmp")
        return try {
            temporary.writeText(content)
            if (temporary.renameTo(target)) {
                true
            } else {
                target.writeText(content)
                true
            }
        } catch (error: IOException) {
            false
        } catch (error: SecurityException) {
            false
        } finally {
            if (temporary.exists()) temporary.delete()
        }
    }

    companion object {
        const val NOTES_DIRECTORY = "notes"
        const val TXT_EXTENSION = "txt"
        private const val MAX_TITLE_LENGTH = 80
        private val INVALID_FILE_NAME_CHARACTERS = Regex("""[\\/:*?"<>|\u0000-\u001F\u007F]""")
        private val WHITESPACE = Regex("\\s+")
    }
}
