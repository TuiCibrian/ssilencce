package com.ssilencce.core.model.tag

import android.graphics.Bitmap

/**
 * Cover operation for a batch write.
 *
 * [Keep] is the default and never touches the embedded pictures, so files keep their current
 * cover. [Replace] embeds the picked bitmap as the front cover in every selected file and
 * [Delete] removes every embedded picture.
 */
sealed interface CoverOperation {
    data object Keep : CoverOperation

    data class Replace(
        val bitmap: Bitmap,
    ) : CoverOperation

    data object Delete : CoverOperation

    val hasChange: Boolean
        get() = this !is Keep
}

/** Per-file result of an explicitly requested cover operation. */
enum class CoverWriteStatus {
    KEPT,
    WRITTEN,
    DELETED,
    MISMATCH,
    NOT_ATTEMPTED,
}
