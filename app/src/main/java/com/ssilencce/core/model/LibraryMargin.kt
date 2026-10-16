package com.ssilencce.core.model

data class LibraryMargin(
    val margin: Int,
    private val additionalSpace: Int = 0,
    private val bottomInsets: Int = 0,
    val navigationMargin: Int = 0,
) {
    val totalMargin = getWithSpace(includeInsets = false)
    val totalMarginWithNavigation = totalMargin + navigationMargin

    fun getWithSpace(
        moreSpace: Int = 0,
        includeInsets: Boolean = true,
        includeNavigation: Boolean = false,
    ) = margin + additionalSpace + moreSpace +
        (if (includeInsets) bottomInsets else 0) +
        (if (includeNavigation) navigationMargin else 0)
}
