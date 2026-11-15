package com.ssilencce.core.model.tag

enum class FieldWriteStatus {
    KEPT,
    WRITTEN,
    CLEARED,
    UNSUPPORTED,
    MISMATCH,
    KEPT_CHANGED,
    NOT_ATTEMPTED,
}

enum class FileWriteStatus {
    SUCCEEDED,
    PARTIAL,
    FAILED,
    SKIPPED,
    UNCHANGED,
    CANCELLED,
}

data class FieldWriteResult(
    val field: TagEditField,
    val operation: FieldOperation,
    val status: FieldWriteStatus,
)

data class FileWriteReport(
    val songId: Long,
    val fileName: String,
    val status: FileWriteStatus,
    val fields: List<FieldWriteResult> = emptyList(),
    val detail: String? = null,
    /** Explicit per-file operations, including [FieldOperation.Keep] overrides. */
    val overrides: Map<TagEditField, FieldOperation> = emptyMap(),
)

data class BatchWriteReport(
    val files: List<FileWriteReport>,
    val requestedFields: Set<TagEditField>,
) {
    val succeededFiles: Int
        get() = files.count { it.status == FileWriteStatus.SUCCEEDED }

    val partialFiles: Int
        get() = files.count { it.status == FileWriteStatus.PARTIAL }

    val failedFiles: Int
        get() = files.count { it.status == FileWriteStatus.FAILED }

    val skippedFiles: Int
        get() = files.count { it.status == FileWriteStatus.SKIPPED }

    val unchangedFiles: Int
        get() = files.count { it.status == FileWriteStatus.UNCHANGED }

    val cancelledFiles: Int
        get() = files.count { it.status == FileWriteStatus.CANCELLED }

    val isFullSuccess: Boolean
        get() =
            requestedFields.isNotEmpty() &&
                files.any { it.status != FileWriteStatus.UNCHANGED } &&
                files.all {
                    it.status == FileWriteStatus.SUCCEEDED || it.status == FileWriteStatus.UNCHANGED
                }
}
