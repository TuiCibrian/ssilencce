package com.ssilencce.data.local

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.Parcelable
import android.util.Log
import com.kyant.taglib.Picture
import com.kyant.taglib.TagLib
import com.ssilencce.core.model.tag.BatchTagEditPlan
import com.ssilencce.core.model.tag.BatchWriteReport
import com.ssilencce.core.model.tag.FieldWriteResult
import com.ssilencce.core.model.tag.FieldWriteStatus
import com.ssilencce.core.model.tag.FileWriteReport
import com.ssilencce.core.model.tag.FileWriteStatus
import com.ssilencce.data.local.replaygain.ReplayGainTags
import com.ssilencce.data.model.Album
import com.ssilencce.data.model.Artist
import com.ssilencce.data.model.Song
import com.ssilencce.extensions.media.createAlbumArtThumbFile
import com.ssilencce.extensions.media.deleteAlbumArt
import com.ssilencce.extensions.media.insertAlbumArt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.IOException
import kotlinx.parcelize.Parcelize
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

private const val SCAN_TIMEOUT_MS = 60_000L

class MetadataWriter : KoinComponent {
    private val contentResolver: ContentResolver by inject()
    private val lock = Any()

    private var properties = mapOf<String, String?>()
    private var explicitReplayGainProperties: Map<String, String?>? = null
    private var pictureBitmap: Bitmap? = null
    private var pictureDeleted = false

    fun picture(pictureBitmap: Bitmap?) =
        synchronized(lock) {
            this.pictureBitmap = pictureBitmap
            this.pictureDeleted = false
        }

    fun pictureDeleted(pictureDeleted: Boolean) =
        synchronized(lock) {
            this.pictureDeleted = pictureDeleted
            this.pictureBitmap = if (pictureDeleted) null else pictureBitmap
        }

    fun propertyMap(propertyMap: Map<String, String?>) =
        synchronized(lock) {
            this.properties = propertyMap
        }

    /**
     * Replaces the ReplayGain values for the next write. Null preserves the canonical values that
     * are already present in each file.
     */
    fun replayGainProperties(propertyMap: Map<String, String?>?) =
        synchronized(lock) {
            this.explicitReplayGainProperties = propertyMap
        }

    suspend fun write(
        context: Context,
        target: EditTarget,
    ) = withContext(IO) {
        try {
            val results = mutableListOf<WriteResultInternal>()
            val picture = createPicture(target)
            val pictureThumbFile = createPictureThumbFile(picture)
            val attempted = target.contents.size
            for (content in target.contents) {
                currentCoroutineContext().ensureActive()
                val result: kotlin.Result<WriteResultInternal> =
                    try {
                        kotlin.Result.success(
                            contentResolver.openFileDescriptor(content.uri, "rw")?.use { fd ->
                                val pictureResult = writePicture(picture, fd)
                                val propertyWrite = writePropertyMap(fd, properties)
                                WriteResultInternal(
                                    content = content,
                                    pictureResult = pictureResult,
                                    propertiesResult = if (propertyWrite.saved) Result.Wrote else Result.Failed,
                                )
                            } ?: WriteResultInternal(content),
                        )
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (throwable: Throwable) {
                        kotlin.Result.failure(throwable)
                    }
                if (result.isSuccess) {
                    results.add(result.getOrThrow())
                    contentResolver.notifyChange(content.uri, null)
                } else {
                    Log.e("MetadataWriter", "Failed to write metadata for ${content.uri}", result.exceptionOrNull())
                }
            }
            if (target.hasArtwork) {
                val wrotePicture = results.any { it.pictureResult == Result.Wrote }
                val deletedPicture = results.any { it.pictureResult == Result.Deleted }
                if (wrotePicture) {
                    if (pictureThumbFile != null) {
                        contentResolver.insertAlbumArt(target.artworkId, pictureThumbFile.path)
                    }
                } else if (deletedPicture) {
                    contentResolver.deleteAlbumArt(target.artworkId)
                }
            }
            val successContents =
                results
                    .filter { it.isSuccess }
                    .map { it -> it.content }
            val writeFailures = attempted - successContents.size
            if (successContents.isNotEmpty()) {
                val scanFailures = scanFiles(context, successContents)
                val scanFailureCount = successContents.count { content -> content.path in scanFailures }
                WriteResult(
                    successContents,
                    failed = writeFailures + scanFailureCount,
                    scanned = successContents.size - scanFailureCount,
                )
            } else {
                WriteResult(emptyList(), failed = attempted, scanned = 0)
            }
        } finally {
            synchronized(lock) { explicitReplayGainProperties = null }
        }
    }

    suspend fun writeBatch(
        context: Context,
        contents: List<EditTarget.Content>,
        plan: BatchTagEditPlan,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
        shouldContinue: () -> Boolean = { true },
    ) = withContext(IO) {
        require(plan.isValid) { "Batch tag plan contains a blank Set value" }
        val requestedFields = plan.changedFields
        if (contents.isEmpty() || requestedFields.isEmpty()) {
            return@withContext BatchWriteReport(emptyList(), requestedFields)
        }
        val reports = mutableListOf<FileWriteReport>()
        val savedForScan = mutableListOf<EditTarget.Content>()
        try {
            var cancelled = false
            contents.forEachIndexed { index, content ->
                if (cancelled || !shouldContinue()) {
                    cancelled = true
                    reports.add(cancelledReport(content, plan))
                    return@forEachIndexed
                }
                currentCoroutineContext().ensureActive()
                onProgress(index, contents.size)
                val filePlan = plan.planFor(content.id)
                if (!filePlan.hasChanges) {
                    reports.add(
                        FileWriteReport(
                            songId = content.id,
                            fileName = content.displayName(),
                            status = FileWriteStatus.UNCHANGED,
                            detail = "no_changes",
                            overrides = plan.perSong[content.id].orEmpty(),
                        ),
                    )
                    onProgress(index + 1, contents.size)
                    return@forEachIndexed
                }
                val overrides = filePlan.toPropertyOverrides()
                val report =
                    try {
                        val propertyWrite =
                            contentResolver.openFileDescriptor(content.uri, "rw")?.use { fd ->
                                writePropertyMap(fd, overrides)
                            }
                        when {
                            propertyWrite == null -> {
                                failedReport(content, plan, "open_failed")
                            }

                            !propertyWrite.saved -> {
                                failedReport(content, plan, "save_failed")
                            }

                            else -> {
                                contentResolver.notifyChange(content.uri, null)
                                savedForScan.add(content)
                                val after = readPropertyMap(content.uri)
                                if (after == null) {
                                    failedReport(content, plan, "read_back_failed")
                                } else {
                                    val fieldResults =
                                        TagWriteVerifier.verify(filePlan, propertyWrite.before, after)
                                    FileWriteReport(
                                        songId = content.id,
                                        fileName = content.displayName(),
                                        status = TagWriteVerifier.fileStatus(fieldResults),
                                        fields = fieldResults,
                                        overrides = plan.perSong[content.id].orEmpty(),
                                    )
                                }
                            }
                        }
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (throwable: Throwable) {
                        Log.e("MetadataWriter", "Failed to write batch metadata for ${content.uri}", throwable)
                        failedReport(content, plan, throwable.message ?: "write_failed")
                    }
                reports.add(report)
                onProgress(index + 1, contents.size)
            }
        } catch (cancellation: CancellationException) {
            withContext(NonCancellable) {
                runCatching { scanFiles(context, savedForScan) }
            }
            throw cancellation
        } finally {
            synchronized(lock) { explicitReplayGainProperties = null }
        }
        val scanFailures =
            try {
                scanFiles(context, savedForScan)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                savedForScan.map { it.path }.toSet()
            }
        val pathBySongId = contents.associate { it.id to it.path }
        val finalReports =
            reports.map { report ->
                val path = pathBySongId[report.songId]
                if (path != null &&
                    path in scanFailures &&
                    report.status != FileWriteStatus.FAILED &&
                    report.status != FileWriteStatus.SKIPPED &&
                    report.status != FileWriteStatus.UNCHANGED &&
                    report.status != FileWriteStatus.CANCELLED
                ) {
                    report.copy(status = FileWriteStatus.PARTIAL, detail = report.detail ?: "scan_failed")
                } else {
                    report
                }
            }
        BatchWriteReport(finalReports, requestedFields)
    }

    private fun createPicture(target: EditTarget): Picture? {
        val pictureBitmap = this.pictureBitmap
        if (target.artworkId == -1L || pictureBitmap == null) {
            return null
        }
        val byteArray =
            ByteArrayOutputStream(pictureBitmap.byteCount).use {
                pictureBitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                it.toByteArray()
            }
        return Picture(
            data = byteArray,
            description = "Embedded Front Cover - ssilencce",
            pictureType = "Front Cover",
            mimeType = "image/jpeg",
        )
    }

    private fun createPictureThumbFile(picture: Picture?): File? {
        if (picture != null) {
            val result = createAlbumArtThumbFile().canonicalFile
            try {
                return result.also { it.writeBytes(picture.data) }
            } catch (e: IOException) {
                Log.e("MetadataWriter", "Failed to create album thumb file", e)
                try {
                    if (result.delete()) {
                        Log.v("MetadataWriter", "Deleted empty album thumb file")
                    }
                } catch (e: Exception) {
                    Log.e("MetadataWriter", "Failed to delete empty album thumb file", e)
                }
            }
        }
        return null
    }

    private fun writePicture(
        picture: Picture?,
        fd: ParcelFileDescriptor,
    ): Result {
        if (picture == null) {
            if (pictureDeleted) {
                if (TagLib.savePictures(fd.dup().detachFd(), arrayOf())) {
                    return Result.Deleted
                }
            } else {
                return Result.None
            }
        } else {
            if (TagLib.savePictures(fd.dup().detachFd(), arrayOf(picture))) {
                return Result.Wrote
            }
        }
        return Result.Failed
    }

    private fun writePropertyMap(
        fd: ParcelFileDescriptor,
        overrides: Map<String, String?>,
    ): PropertyWriteResult {
        val currentProperties =
            TagLib
                .getMetadata(fd.dup().detachFd(), false)
                ?.propertyMap ?: hashMapOf()

        // TagLib's savePropertyMap replaces the tag map. Keep one canonical ReplayGain value per
        // field so duplicate aliases are not regenerated and ordinary edits do not drop the tags.
        val replayGainProperties =
            explicitReplayGainProperties?.let { explicit ->
                explicit.entries
                    .filter { !it.value.isNullOrBlank() }
                    .associate { it.key to arrayOf(it.value!!.trim()) }
            } ?: ReplayGainTags.canonicalProperties(currentProperties)

        val newProperties = hashMapOf<String, Array<String>>()
        currentProperties.forEach { (key, value) ->
            if (!ReplayGainTags.isReplayGainKey(key) && value.isNotEmpty()) {
                newProperties[key] = value
            }
        }
        newProperties.putAll(replayGainProperties)

        for ((key, newValueRaw) in overrides) {
            if (ReplayGainTags.isReplayGainKey(key)) continue
            if (newValueRaw.isNullOrBlank()) {
                newProperties.remove(key)
            } else {
                newProperties[key] = arrayOf(newValueRaw.trim())
            }
        }

        val saved = TagLib.savePropertyMap(fd.dup().detachFd(), newProperties)
        return PropertyWriteResult(currentProperties, saved)
    }

    private fun readPropertyMap(uri: Uri): Map<String, Array<String>>? =
        try {
            contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
                TagLib.getMetadata(fd.dup().detachFd(), false)?.propertyMap
            }
        } catch (throwable: Throwable) {
            Log.e("MetadataWriter", "Failed to read back metadata for $uri", throwable)
            null
        }

    private fun cancelledReport(
        content: EditTarget.Content,
        plan: BatchTagEditPlan,
    ): FileWriteReport {
        val filePlan = plan.planFor(content.id)
        return FileWriteReport(
            songId = content.id,
            fileName = content.displayName(),
            status = FileWriteStatus.CANCELLED,
            fields =
                filePlan.changedFields.map { field ->
                    FieldWriteResult(field, filePlan.operationFor(field), FieldWriteStatus.NOT_ATTEMPTED)
                },
            detail = "cancelled",
            overrides = plan.perSong[content.id].orEmpty(),
        )
    }

    private fun failedReport(
        content: EditTarget.Content,
        plan: BatchTagEditPlan,
        detail: String,
    ): FileWriteReport {
        val filePlan = plan.planFor(content.id)
        return FileWriteReport(
            songId = content.id,
            fileName = content.displayName(),
            status = FileWriteStatus.FAILED,
            fields =
                filePlan.changedFields.map { field ->
                    FieldWriteResult(field, filePlan.operationFor(field), FieldWriteStatus.NOT_ATTEMPTED)
                },
            detail = detail,
            overrides = plan.perSong[content.id].orEmpty(),
        )
    }

    private suspend fun scanFiles(
        context: Context,
        contents: List<EditTarget.Content>,
    ): Set<String> {
        if (contents.isEmpty()) {
            return emptySet()
        }
        val paths = contents.map { it.path }
        val pending = ConcurrentHashMap.newKeySet<String>().apply { addAll(paths) }
        val failed = ConcurrentHashMap.newKeySet<String>()
        val finished =
            withTimeoutOrNull(SCAN_TIMEOUT_MS) {
                suspendCancellableCoroutine { continuation ->
                    MediaScannerConnection.scanFile(context, paths.toTypedArray(), null) { path, uri ->
                        pending.remove(path)
                        if (uri == null) {
                            failed.add(path)
                        }
                        if (pending.isEmpty() && continuation.isActive) {
                            continuation.resume(Unit)
                        }
                    }
                }
            }
        if (finished == null) {
            failed.addAll(pending)
        }
        return failed
    }

    class WriteResult(
        val contents: List<EditTarget.Content>,
        val failed: Int = 0,
        val scanned: Int = 0,
    ) {
        val isSuccess: Boolean
            get() = contents.isNotEmpty() && failed == 0 && scanned == contents.size
    }

    private class PropertyWriteResult(
        val before: Map<String, Array<String>>,
        val saved: Boolean,
    )

    private class WriteResultInternal(
        val content: EditTarget.Content,
        val pictureResult: Result = Result.Failed,
        val propertiesResult: Result = Result.Failed,
    ) {
        val isSuccess: Boolean
            get() = pictureResult != Result.Failed && propertiesResult != Result.Failed
    }

    private enum class Result {
        None,
        Wrote,
        Deleted,
        Failed,
    }
}

private fun EditTarget.Content.displayName(): String = path.substringAfterLast('/').takeIf { it.isNotBlank() } ?: uri.lastPathSegment ?: id.toString()

@Parcelize
data class EditTarget(
    val type: Type,
    val name: String,
    val id: Long,
    val artworkId: Long,
    val contents: List<Content>,
) : Parcelable {
    val hasContent get() = contents.isNotEmpty()

    val hasArtwork get() = artworkId > -1

    val first get() = contents.first()

    @Parcelize
    data class Content(
        val id: Long,
        val uri: Uri,
        val path: String,
    ) : Parcelable

    enum class Type {
        Song,
        Artist,
        AlbumArtist,
        Album,
    }

    companion object {
        val Empty = EditTarget(Type.Song, "", -1, -1, emptyList())

        fun song(song: Song) =
            EditTarget(
                type = Type.Song,
                name = song.title,
                id = song.id,
                artworkId = song.albumId,
                contents = listOf(Content(song.id, song.uri, song.data)),
            )

        fun album(album: Album) =
            EditTarget(
                type = Type.Album,
                name = album.name,
                id = album.id,
                artworkId = album.id,
                contents = album.songs.map { Content(it.id, it.uri, it.data) },
            )

        fun artist(artist: Artist) =
            EditTarget(
                type = if (artist.isAlbumArtist) Type.AlbumArtist else Type.Artist,
                name = artist.name,
                id = artist.id,
                artworkId = -1,
                contents = artist.songs.map { Content(it.id, it.uri, it.data) },
            )
    }
}
