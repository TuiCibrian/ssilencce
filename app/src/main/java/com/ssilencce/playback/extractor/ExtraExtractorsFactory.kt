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

package com.ssilencce.playback.extractor

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorsFactory

/**
 * Prepends the extra-format extractors to Media3's defaults.
 *
 * A new extractor instance is created per call because extractors are stateful; the delegate
 * factory already follows the same contract.
 */
@UnstableApi
class ExtraExtractorsFactory(
    private val delegate: ExtractorsFactory,
) : ExtractorsFactory {
    override fun createExtractors(): Array<Extractor> = extraExtractors() + delegate.createExtractors()

    override fun createExtractors(
        uri: Uri,
        responseHeaders: Map<String, List<String>>,
    ): Array<Extractor> = extraExtractors() + delegate.createExtractors(uri, responseHeaders)

    private fun extraExtractors(): Array<Extractor> = arrayOf(AiffExtractor(), DsdExtractor())
}
