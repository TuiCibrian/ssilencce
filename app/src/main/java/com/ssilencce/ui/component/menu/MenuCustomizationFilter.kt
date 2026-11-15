package com.ssilencce.ui.component.menu

import android.view.Menu
import android.view.MenuItem
import androidx.annotation.MenuRes
import com.ssilencce.core.model.menu.MenuCustomizationGroup
import com.ssilencce.util.Preferences

/**
 * Filter layer for entity option menus.
 *
 * It only touches the actions listed in [MenuCustomizationGroup.items]: hidden actions are
 * removed from the inflated menu and the remaining catalogued actions are re-inserted in the
 * saved order. Actions outside the catalog (for example sort entries or the search icon added
 * by a screen) keep their current position.
 */
object MenuCustomizationFilter {
    fun apply(
        menu: Menu,
        @MenuRes menuRes: Int,
    ) {
        val group = MenuCustomizationGroup.fromMenuRes(menuRes) ?: return
        apply(menu, group)
    }

    fun apply(
        menu: Menu,
        group: MenuCustomizationGroup,
    ) {
        val state = Preferences.menuCustomization(group)
        val catalog = group.items.associateBy { it.itemId }
        if (menu.toList().none { it.itemId in catalog }) return

        val enabledIds = state.order.mapNotNull { key -> group.itemByKey(key)?.itemId }.toSet()
        menu
            .toList()
            .filter { it.itemId in catalog && it.itemId !in enabledIds }
            .forEach { menu.removeItem(it.itemId) }

        val remaining = menu.toList().filter { it.itemId in catalog }
        val orderedItems = orderedItems(state.order, group, remaining)
        if (remaining.map { it.itemId } == orderedItems.map { it.itemId }) return

        orderedItems.forEach { item -> menu.removeItem(item.itemId) }
        orderedItems.forEach { item -> addItem(menu, item) }
    }

    private fun orderedItems(
        order: List<String>,
        group: MenuCustomizationGroup,
        present: List<MenuItem>,
    ): List<MenuItem> {
        val presentById = present.associateBy { it.itemId }
        val ordered = order.mapNotNull { key -> group.itemByKey(key)?.itemId }.mapNotNull { presentById[it] }
        // Safety net: never drop a catalogued action that the saved order does not know about.
        return ordered + present.filter { item -> ordered.none { it.itemId == item.itemId } }
    }

    private fun addItem(
        menu: Menu,
        source: MenuItem,
    ) {
        if (source.hasSubMenu()) {
            val subMenu = menu.addSubMenu(source.groupId, source.itemId, source.order, source.title)
            menu.findItem(source.itemId)?.let { target -> copyProperties(target, source) }
            source.subMenu?.toList()?.forEach { child -> addItem(subMenu, child) }
        } else {
            val target = menu.add(source.groupId, source.itemId, source.order, source.title)
            copyProperties(target, source)
        }
    }

    private fun copyProperties(
        target: MenuItem,
        source: MenuItem,
    ) {
        target.titleCondensed = source.titleCondensed
        target.icon = source.icon
        target.isCheckable = source.isCheckable
        target.isChecked = source.isChecked
        target.isVisible = source.isVisible
        target.isEnabled = source.isEnabled
        target.intent = source.intent
        target.alphabeticShortcut = source.alphabeticShortcut
        target.numericShortcut = source.numericShortcut
    }

    private fun Menu.toList(): List<MenuItem> = (0 until size()).map { getItem(it) }
}
