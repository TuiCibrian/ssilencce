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

package com.ssilencce.ui.screen.library.folders

import android.content.Context
import android.provider.MediaStore

/**
 * Blacklist changes are applied at query time, so poking the MediaStore observable is the
 * cheapest way to refresh every open library screen.
 */
internal fun notifyMediaStoreChanged(context: Context) {
    runCatching {
        context.contentResolver.notifyChange(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            null,
        )
    }
}
