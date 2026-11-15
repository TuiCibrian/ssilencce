package com.ssilencce.data.local

import android.content.ContentResolver
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Session-scoped backup of the original file bytes for the batch tag writer.
 *
 * Every file that is about to be opened for a destructive write is copied to the app cache
 * directory first. The writer restores the current file when a write fails and the editor offers
 * a bounded session undo (see `BatchTagEditorViewModel`) for files that were written successfully.
 * Backups live only while the editor session is alive; [close] removes them.
 */
class BatchWriteJournal(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val contentResolver: ContentResolver = appContext.contentResolver
    private val root = File(appContext.cacheDir, "$DIRECTORY/${UUID.randomUUID()}")
    private val entries = java.util.concurrent.ConcurrentHashMap<Long, Entry>()
    private var backedUpBytes = 0L
    private var pruned = false

    class Entry internal constructor(
        val songId: Long,
        val uri: Uri,
        val path: String,
        internal val backupFile: File,
        val originalSize: Long,
    )

    fun entries(): List<Entry> = entries.values.toList()

    fun hasEntries(): Boolean = entries.isNotEmpty()

    /**
     * Copies the current bytes of [content] into the cache dir. Returns null when the copy cannot
     * be verified or exceeds the per-file cap; the caller must then leave the file untouched.
     */
    suspend fun backup(content: EditTarget.Content): Entry? =
        withContext(Dispatchers.IO) {
            pruneStaleSessions()
            try {
                if (!root.isDirectory && !root.mkdirs()) {
                    return@withContext null
                }
                val target = File(root, "${content.id}.bak")
                var size = 0L
                contentResolver.openInputStream(content.uri)?.use { input ->
                    target.outputStream().use { output ->
                        val copied = input.copyTo(output, bufferSize = DEFAULT_BUFFER_SIZE)
                        size = copied
                    }
                }
                if (size <= 0L || size > MAX_BACKUP_BYTES || backedUpBytes + size > MAX_JOURNAL_BYTES) {
                    target.delete()
                    Log.e(TAG, "Refusing to back up ${content.uri}: $size bytes")
                    return@withContext null
                }
                val entry =
                    Entry(
                        songId = content.id,
                        uri = content.uri,
                        path = content.path,
                        backupFile = target,
                        originalSize = size,
                    )
                entries[content.id] = entry
                backedUpBytes += size
                entry
            } catch (throwable: Throwable) {
                Log.e(TAG, "Failed to back up ${content.uri}", throwable)
                null
            }
        }

    /**
     * Streams the backed-up bytes over the current file and verifies the restored size. The
     * MediaStore row is notified and rescanned so the library reloads the reverted tags.
     */
    suspend fun restore(entry: Entry): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val backup = entry.backupFile
                if (!backup.isFile || backup.length() != entry.originalSize) {
                    Log.e(TAG, "Missing or changed backup for ${entry.uri}")
                    return@withContext false
                }
                val descriptor =
                    contentResolver.openFileDescriptor(entry.uri, "rwt")
                        ?: return@withContext false
                ParcelFileDescriptor.AutoCloseOutputStream(descriptor).use { output ->
                    backup.inputStream().use { input -> input.copyTo(output, bufferSize = DEFAULT_BUFFER_SIZE) }
                }
                val restoredSize =
                    contentResolver.openFileDescriptor(entry.uri, "r")?.use { it.statSize } ?: -1L
                if (restoredSize != entry.originalSize) {
                    Log.e(TAG, "Restored size $restoredSize does not match ${entry.originalSize} for ${entry.uri}")
                    return@withContext false
                }
                contentResolver.notifyChange(entry.uri, null)
                if (entry.path.isNotBlank()) {
                    MediaScannerConnection.scanFile(appContext, arrayOf(entry.path), null, null)
                }
                true
            } catch (throwable: Throwable) {
                Log.e(TAG, "Failed to restore ${entry.uri}", throwable)
                false
            }
        }

    /** Drops one entry after a verified restore so it is not restored twice. */
    fun discard(entry: Entry) {
        entry.backupFile.delete()
        entries.remove(entry.songId)
        backedUpBytes = (backedUpBytes - entry.originalSize).coerceAtLeast(0L)
    }

    /** Removes every backup of this session. Safe to call from any thread. */
    fun close() {
        entries.clear()
        backedUpBytes = 0L
        root.deleteRecursively()
    }

    private fun pruneStaleSessions() {
        if (pruned) {
            return
        }
        pruned = true
        val directory = root.parentFile ?: return
        val cutoff = System.currentTimeMillis() - STALE_SESSION_MILLIS
        directory.listFiles()?.forEach { child ->
            if (child != root && child.lastModified() < cutoff) {
                child.deleteRecursively()
            }
        }
    }

    private companion object {
        const val TAG = "BatchWriteJournal"
        const val DIRECTORY = "tag-write-backup"
        const val MAX_BACKUP_BYTES = 2L * 1024L * 1024L * 1024L
        const val MAX_JOURNAL_BYTES = 4L * 1024L * 1024L * 1024L
        val STALE_SESSION_MILLIS = TimeUnit.DAYS.toMillis(7)
    }
}
