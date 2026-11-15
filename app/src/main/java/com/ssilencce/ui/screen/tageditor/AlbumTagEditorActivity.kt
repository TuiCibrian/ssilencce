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

package com.ssilencce.ui.screen.tageditor

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.ssilencce.R
import com.ssilencce.coil.DEFAULT_ALBUM_IMAGE
import com.ssilencce.core.model.tag.TagEditField
import com.ssilencce.data.local.toBitmap
import com.ssilencce.databinding.TagEditorAlbumFieldBinding
import com.ssilencce.extensions.resources.getResized
import com.ssilencce.extensions.showToast
import com.ssilencce.extensions.webSearch
import com.ssilencce.ui.component.base.AbsTagEditorActivity
import com.ssilencce.ui.component.views.getPlaceholderDrawable
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

/**
 * @author Christians M. A. (mardous)
 */
class AlbumTagEditorActivity : AbsTagEditorActivity() {
    override val viewModel by viewModel<TagEditorViewModel> {
        parametersOf(getEditTarget())
    }

    override val placeholderDrawable: Drawable by lazy {
        getPlaceholderDrawable(DEFAULT_ALBUM_IMAGE)
    }

    private lateinit var albumBinding: TagEditorAlbumFieldBinding
    private var initialValues: Map<TagEditField, String?> = emptyMap()
    private var destructiveConfirmed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.artworkResult.observe(this) { artwork ->
            setImageBitmap(artwork?.toBitmap()?.getResized(1080))
        }
        loadAggregatedContent()
        viewModel.loadArtwork()
    }

    private fun loadAggregatedContent() {
        lifecycleScope.launch {
            val result = aggregateTagValues(viewModel.uris, ALBUM_FIELDS)
            viewModel.restrictWriteTarget(result.readableUris)
            initialValues = result.common
            albumBinding.album.setText(result.value(TagEditField.ALBUM).orEmpty())
            albumBinding.albumArtist.setText(result.value(TagEditField.ALBUM_ARTIST).orEmpty())
            albumBinding.conductor.setText(result.value(TagEditField.CONDUCTOR).orEmpty())
            albumBinding.publisher.setText(result.value(TagEditField.PUBLISHER).orEmpty())
            albumBinding.genre.setText(result.value(TagEditField.GENRE).orEmpty())
            albumBinding.year.setText(result.value(TagEditField.YEAR).orEmpty())
            albumBinding.trackTotal.setText(result.value(TagEditField.TRACK_TOTAL).orEmpty())
            albumBinding.discNumber.setText(result.value(TagEditField.DISC_NUMBER).orEmpty())
            albumBinding.discTotal.setText(result.value(TagEditField.DISC_TOTAL).orEmpty())
            setVariesHint(albumBinding.albumTextInputLayout, result.isVaries(TagEditField.ALBUM))
            setVariesHint(albumBinding.albumArtistTextInputLayout, result.isVaries(TagEditField.ALBUM_ARTIST))
            setVariesHint(albumBinding.conductorTextInputLayout, result.isVaries(TagEditField.CONDUCTOR))
            setVariesHint(albumBinding.publisherTextInputLayout, result.isVaries(TagEditField.PUBLISHER))
            setVariesHint(albumBinding.genreTextInputLayout, result.isVaries(TagEditField.GENRE))
            setVariesHint(albumBinding.yearTextInputLayout, result.isVaries(TagEditField.YEAR))
            setVariesHint(albumBinding.trackTotalTextInputLayout, result.isVaries(TagEditField.TRACK_TOTAL))
            setVariesHint(albumBinding.discNumberTextInputLayout, result.isVaries(TagEditField.DISC_NUMBER))
            setVariesHint(albumBinding.discTotalTextInputLayout, result.isVaries(TagEditField.DISC_TOTAL))
        }
    }

    private fun setVariesHint(
        layout: TextInputLayout,
        varies: Boolean,
    ) {
        layout.helperText = if (varies) getString(R.string.tag_editor_varies) else null
        layout.isHelperTextEnabled = varies
    }

    override fun onWrapFieldViews(
        inflater: LayoutInflater,
        parent: ViewGroup,
    ) {
        albumBinding =
            TagEditorAlbumFieldBinding.inflate(inflater, parent, true).apply {
                genreTextInputLayout.setEndIconOnClickListener {
                    selectDefaultGenre()
                }
            }
    }

    override fun onDefaultGenreSelection(genre: String) {
        val genreContent = albumBinding.genre.text?.toString()
        if (!genreContent.isNullOrBlank()) {
            albumBinding.genre.setText(String.format("%s;%s", genreContent, genre))
        } else {
            albumBinding.genre.setText(genre)
        }
    }

    override fun downloadOnlineImage() {
        val albumTitleStr = albumBinding.album.text?.toString()
        val albumArtistNameStr = albumBinding.albumArtist.text?.toString()
        if (albumArtistNameStr.isNullOrBlank() || albumTitleStr.isNullOrBlank()) {
            showToast(resources.getString(R.string.album_or_artist_empty))
            return
        }
        viewModel.getAlbumInfo(albumArtistNameStr, albumTitleStr).observe(this) { deezerAlbum ->
            if (deezerAlbum != null) {
                val url = deezerAlbum.imageUrl
                if (!url.isNullOrEmpty()) {
                    loadImageFromUrl(url)
                } else {
                    showToast(R.string.could_not_download_album_cover)
                }
            } else {
                showToast(R.string.could_not_download_album_cover)
            }
        }
    }

    override fun searchOnlineImage() {
        webSearch(albumBinding.album.text.toString(), albumBinding.albumArtist.text.toString())
    }

    override fun restoreImage() {
        viewModel.loadArtwork()
    }

    override fun save() {
        val destructiveChanged =
            dirtyPropertyMap().keys.any { key ->
                DESTRUCTIVE_FIELDS.any { it.propertyKey == key }
            }
        if (destructiveChanged && !destructiveConfirmed && viewModel.writableUris.size > 1) {
            destructiveConfirmed = true
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.tag_editor_destructive_title)
                .setMessage(getString(R.string.tag_editor_destructive_message, viewModel.writableUris.size))
                .setPositiveButton(R.string.tag_editor_destructive_confirm) { _, _ -> save() }
                .setNegativeButton(android.R.string.cancel) { _, _ -> destructiveConfirmed = false }
                .setOnCancelListener { destructiveConfirmed = false }
                .show()
            return
        }
        destructiveConfirmed = false
        super.save()
    }

    private fun dirtyPropertyMap(): Map<String, String?> =
        buildMap {
            addIfChanged(TagEditField.ALBUM, albumBinding.album.text?.toString())
            addIfChanged(TagEditField.ALBUM_ARTIST, albumBinding.albumArtist.text?.toString())
            addIfChanged(TagEditField.CONDUCTOR, albumBinding.conductor.text?.toString())
            addIfChanged(TagEditField.PUBLISHER, albumBinding.publisher.text?.toString())
            addIfChanged(TagEditField.GENRE, albumBinding.genre.text?.toString())
            addIfChanged(TagEditField.YEAR, albumBinding.year.text?.toString())
            addIfChanged(TagEditField.TRACK_TOTAL, albumBinding.trackTotal.text?.toString())
            addIfChanged(TagEditField.DISC_NUMBER, albumBinding.discNumber.text?.toString())
            addIfChanged(TagEditField.DISC_TOTAL, albumBinding.discTotal.text?.toString())
        }

    private fun MutableMap<String, String?>.addIfChanged(
        field: TagEditField,
        value: String?,
    ) {
        val current = value?.trim()?.takeIf { it.isNotEmpty() }
        val initial = initialValues[field]?.trim()?.takeIf { it.isNotEmpty() }
        if (current != initial) {
            put(field.propertyKey, current)
        }
    }

    override val propertyMap: Map<String, String?>
        get() = dirtyPropertyMap()

    private companion object {
        val ALBUM_FIELDS =
            listOf(
                TagEditField.ALBUM,
                TagEditField.ALBUM_ARTIST,
                TagEditField.CONDUCTOR,
                TagEditField.PUBLISHER,
                TagEditField.GENRE,
                TagEditField.YEAR,
                TagEditField.TRACK_TOTAL,
                TagEditField.DISC_NUMBER,
                TagEditField.DISC_TOTAL,
            )

        val DESTRUCTIVE_FIELDS =
            setOf(
                TagEditField.TRACK_TOTAL,
                TagEditField.DISC_NUMBER,
                TagEditField.DISC_TOTAL,
            )
    }
}
