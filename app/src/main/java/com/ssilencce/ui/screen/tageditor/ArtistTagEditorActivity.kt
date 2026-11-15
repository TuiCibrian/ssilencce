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

import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.toBitmap
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.ssilencce.R
import com.ssilencce.coil.DEFAULT_ARTIST_IMAGE
import com.ssilencce.core.model.tag.TagEditField
import com.ssilencce.databinding.TagEditorArtistFieldBinding
import com.ssilencce.extensions.webSearch
import com.ssilencce.ui.component.base.AbsTagEditorActivity
import com.ssilencce.ui.component.views.getPlaceholderDrawable
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

/**
 * @author Christians M. A. (mardous)
 */
class ArtistTagEditorActivity : AbsTagEditorActivity() {
    override val viewModel by viewModel<TagEditorViewModel> {
        parametersOf(getEditTarget())
    }

    override val placeholderDrawable: Drawable by lazy {
        getPlaceholderDrawable(DEFAULT_ARTIST_IMAGE)
    }

    private lateinit var artistBinding: TagEditorArtistFieldBinding
    private var artistName: String? = null
    private var initialValues: Map<TagEditField, String?> = emptyMap()
    private var destructiveConfirmed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.image.setImageDrawable(placeholderDrawable)
        loadAggregatedContent()
        loadCurrentArtistImage()
    }

    private fun loadAggregatedContent() {
        lifecycleScope.launch {
            val result = aggregateTagValues(viewModel.uris, ARTIST_FIELDS)
            viewModel.restrictWriteTarget(result.readableUris)
            initialValues = result.common
            artistName = result.value(TagEditField.ARTIST)
            artistBinding.artist.setText(artistName.orEmpty())
            artistBinding.albumArtist.setText(result.value(TagEditField.ALBUM_ARTIST).orEmpty())
            artistBinding.genre.setText(result.value(TagEditField.GENRE).orEmpty())
            artistBinding.discTotal.setText(result.value(TagEditField.DISC_TOTAL).orEmpty())
            setVariesHint(artistBinding.artistTextInputLayout, result.isVaries(TagEditField.ARTIST))
            setVariesHint(artistBinding.albumArtistTextInputLayout, result.isVaries(TagEditField.ALBUM_ARTIST))
            setVariesHint(artistBinding.genreTextInputLayout, result.isVaries(TagEditField.GENRE))
            setVariesHint(artistBinding.discTotalTextInputLayout, result.isVaries(TagEditField.DISC_TOTAL))
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
        artistBinding =
            TagEditorArtistFieldBinding.inflate(inflater, parent, true).apply {
                genreTextInputLayout.setEndIconOnClickListener {
                    selectDefaultGenre()
                }
            }
    }

    override fun onDefaultGenreSelection(genre: String) {
        val genreContent = artistBinding.genre.text?.toString()
        if (!genreContent.isNullOrBlank()) {
            artistBinding.genre.setText(String.format("%s;%s", genreContent, genre))
        } else {
            artistBinding.genre.setText(genre)
        }
    }

    override fun selectImage() {
        val items =
            arrayOf(
                getString(R.string.set_artist_image),
                getString(R.string.web_search),
                getString(R.string.reset_artist_image),
            )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.update_image)
            .setItems(items) { _: DialogInterface, which: Int ->
                when (which) {
                    0 -> startImagePicker()
                    1 -> searchOnlineImage()
                    2 -> deleteImage()
                }
            }.show()
    }

    override fun downloadOnlineImage() {}

    override fun searchOnlineImage() {
        webSearch(artistName)
    }

    override fun restoreImage() {}

    override fun deleteImage() {
        viewModel.resetArtistImage().observe(this) { isSuccess ->
            if (isSuccess) {
                loadCurrentArtistImage()
            }
        }
    }

    private fun loadCurrentArtistImage() {
        viewModel.requestArtist().observe(this) { artist ->
            SingletonImageLoader
                .get(this)
                .enqueue(
                    ImageRequest
                        .Builder(this)
                        .data(artist)
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .listener(
                            onError = { _, _ ->
                                setImageBitmap(null)
                            },
                            onSuccess = { _, result ->
                                setImageBitmap(result.image.toBitmap())
                            },
                        ).build(),
                )
        }
    }

    override fun loadImageFromFile(selectedFileUri: Uri) {
        viewModel.setArtistImage(selectedFileUri).observe(this) {
            loadCurrentArtistImage()
        }
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
            addIfChanged(TagEditField.ARTIST, artistBinding.artist.text?.toString())
            addIfChanged(TagEditField.ALBUM_ARTIST, artistBinding.albumArtist.text?.toString())
            addIfChanged(TagEditField.GENRE, artistBinding.genre.text?.toString())
            addIfChanged(TagEditField.DISC_TOTAL, artistBinding.discTotal.text?.toString())
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
        val ARTIST_FIELDS =
            listOf(
                TagEditField.ARTIST,
                TagEditField.ALBUM_ARTIST,
                TagEditField.GENRE,
                TagEditField.DISC_TOTAL,
            )

        val DESTRUCTIVE_FIELDS = setOf(TagEditField.DISC_TOTAL)
    }
}
