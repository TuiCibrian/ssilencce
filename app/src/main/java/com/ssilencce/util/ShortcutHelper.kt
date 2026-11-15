/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ssilencce.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.ssilencce.R

enum class ShortcutEntityType {
    Album,
    Artist,
    Folder,
    Playlist,
}

object ShortcutHelper {
    // Only the compatibility alias is exported; shortcuts must target it so launchers can deliver
    // the entity intent and extras instead of starting an unexported activity.
    private const val LAUNCHER_ACTIVITY_ALIAS = "com.ssilencce.activities.MainActivity"

    const val ACTION_ENTITY = "com.ssilencce.shortcut.ENTITY"
    const val EXTRA_TYPE = "shortcut_type"
    const val EXTRA_ID = "shortcut_id"
    const val EXTRA_PATH = "shortcut_path"
    const val EXTRA_NAME = "shortcut_name"

    fun add(
        context: Context,
        type: ShortcutEntityType,
        id: String,
        name: String,
        path: String? = null,
    ): Boolean {
        val intent =
            Intent()
                .setComponent(ComponentName(context.packageName, LAUNCHER_ACTIVITY_ALIAS))
                .setAction(ACTION_ENTITY)
                .putExtra(EXTRA_TYPE, type.name)
                .putExtra(EXTRA_ID, id)
                .putExtra(EXTRA_PATH, path)
                .putExtra(EXTRA_NAME, name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val shortcut =
            ShortcutInfoCompat
                .Builder(context, shortcutId(type, id))
                .setShortLabel(name)
                .setLongLabel(name)
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                .setIntent(intent)
                .build()
        return ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
    }

    fun remove(
        context: Context,
        type: ShortcutEntityType,
        id: String,
    ) {
        ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(shortcutId(type, id)))
    }

    fun shortcutId(
        type: ShortcutEntityType,
        id: String,
    ): String = "${type.name.lowercase()}-$id"
}
