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

package com.ssilencce.data.local

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ssilencce.util.Preferences

/**
 * Schedules a debounced library rescan when a storage volume becomes available.
 *
 * The user opts in through the setup wizard or Library settings; the default keeps the previous
 * behavior (MediaStore refreshes on its own).
 */
class MediaMountRescanReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_MEDIA_MOUNTED) return
        if (!Preferences.autoRescanOnMediaMount) return
        if (!LibraryRescan.hasScanPermission(context)) return
        LibraryRescan.scheduleDebounced(context.applicationContext)
    }
}
