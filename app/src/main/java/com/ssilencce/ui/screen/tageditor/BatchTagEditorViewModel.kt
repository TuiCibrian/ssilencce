package com.ssilencce.ui.screen.tageditor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssilencce.core.model.tag.BatchTagEditPlan
import com.ssilencce.core.model.tag.BatchWriteReport
import com.ssilencce.core.model.tag.CoverOperation
import com.ssilencce.core.model.tag.CoverWriteStatus
import com.ssilencce.core.model.tag.FieldOperation
import com.ssilencce.core.model.tag.FieldWriteResult
import com.ssilencce.core.model.tag.FieldWriteStatus
import com.ssilencce.core.model.tag.FileWriteReport
import com.ssilencce.core.model.tag.FileWriteStatus
import com.ssilencce.core.model.tag.TagEditField
import com.ssilencce.data.local.BatchWriteJournal
import com.ssilencce.data.local.EditTarget
import com.ssilencce.data.local.MetadataReader
import com.ssilencce.data.local.MetadataWriter
import com.ssilencce.data.local.repository.Repository
import com.ssilencce.data.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread

sealed interface FieldAggregate {
    data class Common(
        val value: String,
    ) : FieldAggregate

    data object Varies : FieldAggregate

    data object Absent : FieldAggregate
}

data class BatchFieldState(
    val field: TagEditField,
    val aggregate: FieldAggregate,
    val operation: FieldOperation = FieldOperation.Keep,
) {
    val commonValue: String?
        get() = (aggregate as? FieldAggregate.Common)?.value

    val hasBlankSet: Boolean
        get() = operation is FieldOperation.Set && operation.value.isBlank()
}

data class BatchFileInfo(
    val songId: Long,
    val title: String,
    val fileName: String,
    val values: Map<TagEditField, String?>,
)

enum class BatchSkipReason {
    MISSING,
    UNREADABLE,
}

data class BatchSkippedFile(
    val songId: Long,
    val name: String,
    val reason: BatchSkipReason,
)

enum class BatchCoverAction {
    KEEP,
    REPLACE,
    DELETE,
}

data class BatchCoverState(
    val action: BatchCoverAction = BatchCoverAction.KEEP,
    /** Cover shown in the editor: the current common cover or the picked replacement. */
    val preview: Bitmap? = null,
    val loadFailed: Boolean = false,
) {
    val hasChange: Boolean
        get() = action != BatchCoverAction.KEEP
}

sealed interface BatchTagEditorUiState {
    data class Loading(
        val completed: Int = 0,
        val total: Int = 0,
        val currentTitle: String? = null,
    ) : BatchTagEditorUiState

    data class Ready(
        val selectedCount: Int,
        val readableCount: Int,
        val unreadableCount: Int,
        val missingCount: Int,
        val files: List<BatchFileInfo>,
        val skippedFiles: List<BatchSkippedFile>,
        val fields: List<BatchFieldState>,
        val perSong: Map<Long, Map<TagEditField, FieldOperation>> = emptyMap(),
        val perFileField: TagEditField? = null,
        val cover: BatchCoverState = BatchCoverState(),
    ) : BatchTagEditorUiState {
        val changedFieldCount: Int
            get() = BatchTagEditPlan(fields.associate { it.field to it.operation }, perSong).changedFields.size

        val hasBlankSet: Boolean
            get() =
                fields.any { it.hasBlankSet } ||
                    perSong.values.any { operations ->
                        operations.values.any { it is FieldOperation.Set && it.value.isBlank() }
                    }

        val perFileOverrideCount: Int
            get() = perSong.values.sumOf { operations -> operations.size }

        val hasChanges: Boolean
            get() = changedFieldCount > 0 || cover.hasChange

        val canSave: Boolean
            get() = readableCount > 0 && hasChanges && !hasBlankSet
    }

    data class Writing(
        val completed: Int,
        val total: Int,
        val currentTitle: String?,
    ) : BatchTagEditorUiState

    data class Result(
        val report: BatchWriteReport,
        val canUndo: Boolean = false,
    ) : BatchTagEditorUiState

    data class Undoing(
        val completed: Int,
        val total: Int,
    ) : BatchTagEditorUiState

    data class Undone(
        val restored: Int,
        val failed: Int,
    ) : BatchTagEditorUiState

    data class Error(
        val error: BatchTagEditorError,
        val detail: String? = null,
    ) : BatchTagEditorUiState

    data object Empty : BatchTagEditorUiState
}

enum class BatchTagEditorError {
    TOO_MANY,
    NO_READABLE_FILES,
    PERMISSION_DENIED,
    UNEXPECTED,
}

class BatchTagEditorViewModel(
    private val repository: Repository,
    private val songIds: List<Long>,
) : ViewModel() {
    private val metadataWriter = MetadataWriter()
    private val _state = MutableStateFlow<BatchTagEditorUiState>(BatchTagEditorUiState.Loading())
    val state: StateFlow<BatchTagEditorUiState> = _state.asStateFlow()

    private var readableFiles: List<BatchTagFile> = emptyList()
    private var unreadableSongs: List<Song> = emptyList()
    private var missingIds: List<Long> = emptyList()
    private var prepared = false
    private var prepareJob: Job? = null
    private var writeJob: Job? = null
    private var undoJob: Job? = null
    private var journal: BatchWriteJournal? = null
    private var originalCoverPreview: Bitmap? = null
    private val albumIdBySongId = mutableMapOf<Long, Long>()

    @Volatile
    private var stopRequested = false

    fun prepare() {
        if (prepared) {
            return
        }
        prepared = true
        val ids = songIds.distinct()
        when {
            ids.isEmpty() -> {
                _state.value = BatchTagEditorUiState.Empty
                return
            }

            ids.size > MAX_BATCH_SONGS -> {
                _state.value = BatchTagEditorUiState.Error(BatchTagEditorError.TOO_MANY)
                return
            }
        }

        prepareJob =
            viewModelScope.launch {
                _state.value = BatchTagEditorUiState.Loading(completed = 0, total = ids.size)
                try {
                    val selection = readSelection(ids)
                    if (selection.readable.isEmpty()) {
                        _state.value = BatchTagEditorUiState.Error(BatchTagEditorError.NO_READABLE_FILES)
                    } else {
                        readableFiles = selection.readable
                        unreadableSongs = selection.unreadableSongs
                        missingIds = selection.missingIds
                        originalCoverPreview = selection.coverPreview
                        _state.value =
                            BatchTagEditorUiState.Ready(
                                selectedCount = ids.size,
                                readableCount = selection.readable.size,
                                unreadableCount = selection.unreadableSongs.size,
                                missingCount = selection.missingIds.size,
                                files = selection.readable.map { it.toInfo() },
                                skippedFiles = selection.skippedFiles,
                                fields = selection.fields,
                                cover = BatchCoverState(preview = selection.coverPreview),
                            )
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (throwable: Throwable) {
                    _state.value =
                        BatchTagEditorUiState.Error(
                            error = BatchTagEditorError.UNEXPECTED,
                            detail = throwable.message,
                        )
                }
            }
    }

    fun cancelPrepare() {
        prepareJob?.cancel()
    }

    fun setCoverImage(
        context: Context,
        uri: Uri,
    ) {
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) { decodeCoverBitmap(context.applicationContext, uri) }
            val ready = _state.value as? BatchTagEditorUiState.Ready ?: return@launch
            _state.value =
                ready.copy(
                    cover =
                        if (bitmap != null) {
                            ready.cover.copy(
                                action = BatchCoverAction.REPLACE,
                                preview = bitmap,
                                loadFailed = false,
                            )
                        } else {
                            ready.cover.copy(loadFailed = true)
                        },
                )
        }
    }

    fun keepCover() {
        val ready = _state.value as? BatchTagEditorUiState.Ready ?: return
        _state.value =
            ready.copy(
                cover =
                    ready.cover.copy(
                        action = BatchCoverAction.KEEP,
                        preview = originalCoverPreview,
                        loadFailed = false,
                    ),
            )
    }

    fun deleteCover() {
        val ready = _state.value as? BatchTagEditorUiState.Ready ?: return
        _state.value =
            ready.copy(
                cover =
                    ready.cover.copy(
                        action = BatchCoverAction.DELETE,
                        preview = null,
                        loadFailed = false,
                    ),
            )
    }

    fun setOperation(
        field: TagEditField,
        operation: FieldOperation,
    ) {
        val ready = _state.value as? BatchTagEditorUiState.Ready ?: return
        val perSong =
            ready.perSong
                .mapNotNull { (songId, operations) ->
                    val updated = operations.toMutableMap()
                    if (updated[field] == operation) {
                        updated.remove(field)
                    }
                    if (updated.isEmpty()) null else songId to updated
                }.toMap()
        _state.value =
            ready.copy(
                fields =
                    ready.fields.map { fieldState ->
                        if (fieldState.field == field) {
                            fieldState.copy(operation = operation)
                        } else {
                            fieldState
                        }
                    },
                perSong = perSong,
            )
    }

    fun openPerFile(field: TagEditField) {
        val ready = _state.value as? BatchTagEditorUiState.Ready ?: return
        _state.value = ready.copy(perFileField = field)
    }

    fun closePerFile() {
        val ready = _state.value as? BatchTagEditorUiState.Ready ?: return
        _state.value = ready.copy(perFileField = null)
    }

    fun setPerFileOperation(
        songId: Long,
        field: TagEditField,
        operation: FieldOperation,
    ) {
        val ready = _state.value as? BatchTagEditorUiState.Ready ?: return
        val commonOperation = ready.fields.firstOrNull { it.field == field }?.operation ?: FieldOperation.Keep
        val perSong = ready.perSong.toMutableMap()
        val fieldOperations = perSong[songId]?.toMutableMap() ?: mutableMapOf()
        if (operation == commonOperation) {
            fieldOperations.remove(field)
        } else {
            fieldOperations[field] = operation
        }
        if (fieldOperations.isEmpty()) {
            perSong.remove(songId)
        } else {
            perSong[songId] = fieldOperations
        }
        _state.value = ready.copy(perSong = perSong)
    }

    fun perFileOperation(
        songId: Long,
        field: TagEditField,
    ): FieldOperation {
        val ready = _state.value as? BatchTagEditorUiState.Ready ?: return FieldOperation.Keep
        return ready.perSong[songId]?.get(field)
            ?: ready.fields.firstOrNull { it.field == field }?.operation
            ?: FieldOperation.Keep
    }

    fun writableUris(): List<Uri> = readableFiles.map { it.song.uri }

    fun write(context: Context) {
        val ready = _state.value as? BatchTagEditorUiState.Ready ?: return
        if (!ready.canSave || writeJob?.isActive == true) {
            return
        }
        val files = readableFiles
        if (files.isEmpty()) {
            _state.value = BatchTagEditorUiState.Error(BatchTagEditorError.NO_READABLE_FILES)
            return
        }
        val cover =
            when (ready.cover.action) {
                BatchCoverAction.KEEP -> {
                    CoverOperation.Keep
                }

                BatchCoverAction.DELETE -> {
                    CoverOperation.Delete
                }

                BatchCoverAction.REPLACE -> {
                    CoverOperation.Replace(ready.cover.preview ?: return)
                }
            }
        val plan =
            BatchTagEditPlan(
                common = ready.fields.associate { it.field to it.operation },
                perSong = ready.perSong,
            )
        if (!plan.isValid) {
            return
        }
        val journal = journal ?: BatchWriteJournal(context.applicationContext).also { journal = it }

        stopRequested = false
        writeJob =
            viewModelScope.launch {
                _state.value =
                    BatchTagEditorUiState.Writing(
                        completed = 0,
                        total = files.size,
                        currentTitle = files.firstOrNull()?.song?.title,
                    )
                try {
                    val report =
                        metadataWriter.writeBatch(
                            context = context.applicationContext,
                            contents = files.map { it.content },
                            plan = plan,
                            cover = cover,
                            journal = journal,
                            albumIdBySongId = albumIdBySongId,
                            onProgress = { done, total ->
                                _state.value =
                                    BatchTagEditorUiState.Writing(
                                        completed = done,
                                        total = total,
                                        currentTitle =
                                            files.getOrNull(done.coerceAtMost(files.lastIndex))?.song?.title,
                                    )
                            },
                            shouldContinue = { !stopRequested },
                        )
                    val fullReport = report.copy(files = report.files + buildSkippedReports(plan, cover.hasChange))
                    _state.value =
                        BatchTagEditorUiState.Result(
                            report = fullReport,
                            canUndo = journal.hasEntries() && fullReport.files.any { it.holdsChanges },
                        )
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (throwable: Throwable) {
                    _state.value =
                        BatchTagEditorUiState.Error(
                            error = BatchTagEditorError.UNEXPECTED,
                            detail = throwable.message,
                        )
                }
            }
    }

    fun stopWriting() {
        stopRequested = true
    }

    fun onWritePermissionDenied() {
        _state.value = BatchTagEditorUiState.Error(BatchTagEditorError.PERMISSION_DENIED)
    }

    /** Uris whose original bytes a session undo would restore; used for the write request. */
    fun undoUris(): List<Uri> = undoEntries().map { it.uri }

    fun undo(context: Context) {
        val result = _state.value as? BatchTagEditorUiState.Result ?: return
        if (undoJob?.isActive == true) {
            return
        }
        val journal = journal ?: return
        val entries = undoEntries()
        if (entries.isEmpty()) {
            _state.value = result.copy(canUndo = false)
            return
        }
        undoJob =
            viewModelScope.launch {
                _state.value = BatchTagEditorUiState.Undoing(completed = 0, total = entries.size)
                var restored = 0
                var failed = 0
                val restoredAlbumIds = mutableSetOf<Long>()
                entries.forEachIndexed { index, entry ->
                    currentCoroutineContext().ensureActive()
                    if (journal.restore(entry)) {
                        journal.discard(entry)
                        restored++
                        albumIdBySongId[entry.songId]?.let { restoredAlbumIds.add(it) }
                    } else {
                        failed++
                    }
                    _state.value = BatchTagEditorUiState.Undoing(completed = index + 1, total = entries.size)
                }
                withContext(Dispatchers.IO) {
                    metadataWriter.refreshAlbumArtwork(restoredAlbumIds, null)
                }
                _state.value = BatchTagEditorUiState.Undone(restored = restored, failed = failed)
            }
    }

    fun onUndoUnavailable() {
        val result = _state.value as? BatchTagEditorUiState.Result ?: return
        _state.value = result.copy(canUndo = false)
    }

    override fun onCleared() {
        super.onCleared()
        val activeJournal = journal
        journal = null
        if (activeJournal != null) {
            thread(name = "tag-write-journal-cleanup", isDaemon = true) {
                runBlocking {
                    runCatching { writeJob?.join() }
                    runCatching { undoJob?.join() }
                }
                runCatching { activeJournal.close() }
            }
        }
    }

    private fun undoEntries(): List<BatchWriteJournal.Entry> {
        val result = _state.value as? BatchTagEditorUiState.Result ?: return emptyList()
        return journal?.entries().orEmpty().filter { entry ->
            result.report.files.any { file -> file.songId == entry.songId && file.holdsChanges }
        }
    }

    private suspend fun readSelection(ids: List<Long>): PreparedSelection =
        withContext(Dispatchers.IO) {
            val readable = mutableListOf<BatchTagFile>()
            val unreadable = mutableListOf<Song>()
            val missing = mutableListOf<Long>()
            val skipped = mutableListOf<BatchSkippedFile>()
            val accumulators = TagEditField.entries.associateWith { FieldAccumulator() }

            ids.forEachIndexed { index, id ->
                currentCoroutineContext().ensureActive()
                val song = repository.songById(id)
                if (song == Song.emptySong || song.id == -1L) {
                    missing.add(id)
                    skipped.add(BatchSkippedFile(id, "#$id", BatchSkipReason.MISSING))
                    _state.value = BatchTagEditorUiState.Loading(index, ids.size, null)
                    return@forEachIndexed
                }
                val reader = MetadataReader(song.uri, readPictures = false)
                if (!reader.hasMetadata) {
                    unreadable.add(song)
                    skipped.add(BatchSkippedFile(song.id, song.fileName, BatchSkipReason.UNREADABLE))
                    _state.value = BatchTagEditorUiState.Loading(index, ids.size, song.title)
                    return@forEachIndexed
                }
                val values =
                    TagEditField.entries.associateWith { field ->
                        reader.merge(field.propertyKey)?.trim()?.takeIf { it.isNotEmpty() }
                    }
                values.forEach { (field, value) ->
                    accumulators.getValue(field).add(value)
                }
                readable.add(
                    BatchTagFile(
                        song = song,
                        content = EditTarget.Content(song.id, song.uri, song.data),
                        values = values,
                    ),
                )
                albumIdBySongId[song.id] = song.albumId
                _state.value = BatchTagEditorUiState.Loading(index + 1, ids.size, song.title)
            }

            val fields =
                TagEditField.entries.map { field ->
                    BatchFieldState(
                        field = field,
                        aggregate = accumulators.getValue(field).result(),
                    )
                }
            val coverPreview =
                readable.firstOrNull()?.let { file ->
                    MetadataReader(file.song.uri, readPictures = true)
                        .frontCover()
                        ?.data
                        ?.let { decodeCoverBytes(it) }
                }
            PreparedSelection(readable, unreadable, missing, skipped, fields, coverPreview)
        }

    private fun buildSkippedReports(
        plan: BatchTagEditPlan,
        coverRequested: Boolean,
    ): List<FileWriteReport> =
        buildList {
            missingIds.forEach { id ->
                add(skippedReport(id, "#$id", plan, "missing", coverRequested))
            }
            unreadableSongs.forEach { song ->
                add(skippedReport(song.id, song.fileName, plan, "unreadable", coverRequested))
            }
        }

    private fun skippedReport(
        songId: Long,
        fileName: String,
        plan: BatchTagEditPlan,
        detail: String,
        coverRequested: Boolean,
    ): FileWriteReport {
        val filePlan = plan.planFor(songId)
        return FileWriteReport(
            songId = songId,
            fileName = fileName,
            status = FileWriteStatus.SKIPPED,
            fields =
                filePlan.changedFields.map { field ->
                    FieldWriteResult(field, filePlan.operationFor(field), FieldWriteStatus.NOT_ATTEMPTED)
                },
            detail = detail,
            overrides = plan.perSong[songId].orEmpty(),
            coverStatus = if (coverRequested) CoverWriteStatus.NOT_ATTEMPTED else CoverWriteStatus.KEPT,
        )
    }

    private data class BatchTagFile(
        val song: Song,
        val content: EditTarget.Content,
        val values: Map<TagEditField, String?>,
    ) {
        fun toInfo(): BatchFileInfo =
            BatchFileInfo(
                songId = song.id,
                title = song.title,
                fileName = song.fileName,
                values = values,
            )
    }

    private data class PreparedSelection(
        val readable: List<BatchTagFile>,
        val unreadableSongs: List<Song>,
        val missingIds: List<Long>,
        val skippedFiles: List<BatchSkippedFile>,
        val fields: List<BatchFieldState>,
        val coverPreview: Bitmap?,
    )

    private class FieldAccumulator {
        private var first: String? = null
        private var initialized = false
        private var varies = false

        fun add(value: String?) {
            if (!initialized) {
                first = value
                initialized = true
                return
            }
            if (first != value) {
                varies = true
            }
        }

        fun result(): FieldAggregate =
            when {
                varies -> FieldAggregate.Varies
                first == null -> FieldAggregate.Absent
                else -> FieldAggregate.Common(first!!)
            }
    }

    private fun decodeCoverBitmap(
        context: Context,
        uri: Uri,
    ): Bitmap? {
        return try {
            val bytes =
                context.contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes()
                } ?: return null
            decodeCoverBytes(bytes)
        } catch (throwable: Throwable) {
            Log.e(TAG, "Failed to decode the picked cover $uri", throwable)
            null
        }
    }

    private fun decodeCoverBytes(bytes: ByteArray): Bitmap? {
        if (bytes.isEmpty() || bytes.size > MAX_COVER_BYTES) {
            return null
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null
        }
        var sampleSize = 1
        while (bounds.outWidth / (sampleSize * 2) >= MAX_COVER_DIMENSION ||
            bounds.outHeight / (sampleSize * 2) >= MAX_COVER_DIMENSION
        ) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    companion object {
        const val MAX_BATCH_SONGS = 500
        private const val MAX_COVER_DIMENSION = 1200
        private const val MAX_COVER_BYTES = 32 * 1024 * 1024
        private const val TAG = "BatchTagEditor"
    }
}
