package com.ssilencce.core.model.tag

sealed interface FieldOperation {
    data object Keep : FieldOperation

    data class Set(
        val value: String,
    ) : FieldOperation

    data object Clear : FieldOperation
}

/**
 * A batch plan containing only the fields the user explicitly opted into.
 *
 * Keep is the default and is never sent to the writer, so each file keeps its current value.
 * Set must be non-blank; Clear is the only removal operation.
 */
data class TagEditPlan(
    val operations: Map<TagEditField, FieldOperation> = emptyMap(),
) {
    val changedFields: Set<TagEditField>
        get() = operations.filterValues { it !is FieldOperation.Keep }.keys

    val hasChanges: Boolean
        get() = changedFields.isNotEmpty()

    val isValid: Boolean
        get() = operations.values.none { it is FieldOperation.Set && it.value.isBlank() }

    fun operationFor(field: TagEditField): FieldOperation = operations[field] ?: FieldOperation.Keep

    fun toPropertyOverrides(): Map<String, String?> =
        buildMap {
            for ((field, operation) in operations) {
                when (operation) {
                    FieldOperation.Keep -> {
                        Unit
                    }

                    is FieldOperation.Set -> {
                        val value = operation.value.trim()
                        if (value.isNotEmpty()) {
                            put(field.propertyKey, value)
                        }
                    }

                    FieldOperation.Clear -> {
                        put(field.propertyKey, null)
                    }
                }
            }
        }
}

/**
 * A batch plan with one common operation per field and optional sparse per-song overrides.
 *
 * A per-song [FieldOperation.Keep] deliberately overrides a common [FieldOperation.Set] for that
 * file; it is removed from the effective plan instead of falling back to the common operation.
 */
data class BatchTagEditPlan(
    val common: Map<TagEditField, FieldOperation> = emptyMap(),
    val perSong: Map<Long, Map<TagEditField, FieldOperation>> = emptyMap(),
) {
    fun planFor(songId: Long): TagEditPlan {
        val effective = LinkedHashMap(common)
        perSong[songId]?.forEach { (field, operation) ->
            if (operation is FieldOperation.Keep) {
                effective.remove(field)
            } else {
                effective[field] = operation
            }
        }
        return TagEditPlan(effective)
    }

    val changedFields: Set<TagEditField>
        get() =
            buildSet {
                common.forEach { (field, operation) ->
                    if (operation !is FieldOperation.Keep) add(field)
                }
                perSong.values.forEach { operations ->
                    operations.forEach { (field, operation) ->
                        if (operation !is FieldOperation.Keep) add(field)
                    }
                }
            }

    val perSongOverrideCount: Int
        get() = perSong.values.sumOf { operations -> operations.size }

    val hasChanges: Boolean
        get() = changedFields.isNotEmpty()

    val isValid: Boolean
        get() =
            common.values.none { it is FieldOperation.Set && it.value.isBlank() } &&
                perSong.values.none { operations ->
                    operations.values.any { it is FieldOperation.Set && it.value.isBlank() }
                }
}
