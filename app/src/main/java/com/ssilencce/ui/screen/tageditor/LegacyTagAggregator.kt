package com.ssilencce.ui.screen.tageditor

import android.net.Uri
import com.ssilencce.core.model.tag.TagEditField
import com.ssilencce.data.local.MetadataReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

data class AggregatedTagValues(
    val common: Map<TagEditField, String?>,
    val varies: Set<TagEditField>,
    val readableUris: List<Uri>,
    val readableCount: Int,
    val totalCount: Int,
) {
    fun value(field: TagEditField): String? = common[field]

    fun isVaries(field: TagEditField): Boolean = field in varies
}

/**
 * Reads every selected file and returns the values shared by all readable files.
 *
 * A field whose values differ (including present/absent mixes) is returned as [varies] with a
 * null common value. This is deliberately a read-only aggregation: the legacy album/artist
 * editors use it to avoid treating the first file as the source of truth.
 */
suspend fun aggregateTagValues(
    uris: List<Uri>,
    fields: List<TagEditField>,
): AggregatedTagValues =
    withContext(Dispatchers.IO) {
        val accumulators = fields.associateWith { ValueAccumulator() }
        val readableUris = mutableListOf<Uri>()
        uris.forEach { uri ->
            currentCoroutineContext().ensureActive()
            val reader = MetadataReader(uri, readPictures = false)
            if (!reader.hasMetadata) {
                return@forEach
            }
            readableUris.add(uri)
            fields.forEach { field ->
                val value = reader.merge(field.propertyKey)?.trim()?.takeIf { it.isNotEmpty() }
                accumulators.getValue(field).add(value)
            }
        }
        AggregatedTagValues(
            common = fields.associateWith { accumulators.getValue(it).commonValue() },
            varies = fields.filterTo(mutableSetOf()) { accumulators.getValue(it).varies },
            readableUris = readableUris,
            readableCount = readableUris.size,
            totalCount = uris.size,
        )
    }

private class ValueAccumulator {
    private var first: String? = null
    private var initialized = false
    var varies: Boolean = false
        private set

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

    fun commonValue(): String? = if (varies) null else first
}
