package com.ssilencce.core.model.navigation

/**
 * Pure navigation rules that keep the Queues library category out of Aurora start/last-page
 * restoration. Aurora has one fixed Queue destination; the classic UI keeps the Queues category.
 */
object AuroraStartDestinationPolicy {
    fun isAllowedTopLevel(
        destinationId: Int,
        queuesCategoryId: Int,
    ): Boolean = destinationId != queuesCategoryId

    fun firstAllowedCategoryId(
        visibleCategoryIds: List<Int>,
        queuesCategoryId: Int,
        fallbackCategoryId: Int,
    ): Int = visibleCategoryIds.firstOrNull { it != queuesCategoryId } ?: fallbackCategoryId
}
