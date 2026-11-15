package com.ssilencce.data.local

import com.ssilencce.core.model.tag.FieldOperation
import com.ssilencce.core.model.tag.FieldWriteResult
import com.ssilencce.core.model.tag.FieldWriteStatus
import com.ssilencce.core.model.tag.FileWriteStatus
import com.ssilencce.core.model.tag.TagEditField
import com.ssilencce.core.model.tag.TagEditPlan

/**
 * Compares the requested plan with the property map that TagLib persisted.
 *
 * TagLib's savePropertyMap only returns a boolean and discards its unsupported-property map, so
 * this read-back is the source of truth for per-field success.
 */
object TagWriteVerifier {
    fun verify(
        plan: TagEditPlan,
        before: Map<String, Array<String>>,
        after: Map<String, Array<String>>,
    ): List<FieldWriteResult> =
        TagEditField.entries.map { field ->
            val operation = plan.operationFor(field)
            val beforeValue = normalize(before[field.propertyKey])
            val afterValue = normalize(after[field.propertyKey])
            val status =
                when (operation) {
                    FieldOperation.Keep -> {
                        if (beforeValue == afterValue) {
                            FieldWriteStatus.KEPT
                        } else {
                            FieldWriteStatus.KEPT_CHANGED
                        }
                    }

                    is FieldOperation.Set -> {
                        val expected = operation.value.trim()
                        when {
                            expected.isEmpty() -> FieldWriteStatus.NOT_ATTEMPTED
                            afterValue == null -> FieldWriteStatus.UNSUPPORTED
                            afterValue == expected -> FieldWriteStatus.WRITTEN
                            else -> FieldWriteStatus.MISMATCH
                        }
                    }

                    FieldOperation.Clear -> {
                        if (afterValue == null) {
                            FieldWriteStatus.CLEARED
                        } else {
                            FieldWriteStatus.MISMATCH
                        }
                    }
                }
            FieldWriteResult(field, operation, status)
        }

    fun fileStatus(results: List<FieldWriteResult>): FileWriteStatus {
        val requested = results.filter { it.operation !is FieldOperation.Keep }
        val successful =
            requested.count {
                it.status == FieldWriteStatus.WRITTEN || it.status == FieldWriteStatus.CLEARED
            }
        val keptChanged = results.any { it.status == FieldWriteStatus.KEPT_CHANGED }
        return when {
            requested.isEmpty() -> FileWriteStatus.UNCHANGED
            successful == 0 -> FileWriteStatus.FAILED
            successful < requested.size -> FileWriteStatus.PARTIAL
            keptChanged -> FileWriteStatus.PARTIAL
            else -> FileWriteStatus.SUCCEEDED
        }
    }

    fun normalize(values: Array<String>?): String? {
        if (values == null) {
            return null
        }
        val normalized = values.map { it.trim() }.filter { it.isNotEmpty() }
        return normalized.takeIf { it.isNotEmpty() }?.joinToString("; ")
    }
}
