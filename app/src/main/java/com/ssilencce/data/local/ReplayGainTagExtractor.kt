/*
 * Copyright (c) 2024 Christians Martínez Alvarado
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

import android.net.Uri
import android.util.LruCache
import com.ssilencce.data.local.replaygain.ReplayGainTagResolver
import com.ssilencce.data.model.Song
import com.ssilencce.data.model.replaygain.ReplayGain

object ReplayGainTagExtractor {
    private val cache = LruCache<Uri, ReplayGain>(128)

    fun getReplayGain(song: Song): ReplayGain {
        if (song == Song.emptySong) {
            return ReplayGain.Empty
        }
        var gainValues = cache.get(song.uri)
        if (gainValues == null) {
            val metadataReader = MetadataReader(song.uri)
            val rawTags = metadataReader.all()
            gainValues = ReplayGainTagResolver.resolve(rawTags)
            cache.put(song.uri, gainValues)
        }
        return gainValues
    }

    fun removeFromCache(uri: Uri) {
        cache.remove(uri)
    }

    fun clearCache() {
        cache.evictAll()
    }
}
