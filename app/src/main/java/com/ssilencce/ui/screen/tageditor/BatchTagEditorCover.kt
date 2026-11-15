package com.ssilencce.ui.screen.tageditor

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ssilencce.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BatchCoverCard(
    cover: BatchCoverState,
    onPickCover: () -> Unit,
    onDeleteCover: () -> Unit,
    onKeepCover: () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.batch_tag_editor_cover),
                style = MaterialTheme.typography.titleSmall,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BatchCoverThumbnail(
                    bitmap = cover.preview,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)),
                )
                Text(
                    text = coverDescription(cover),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = cover.action == BatchCoverAction.KEEP,
                    onClick = onKeepCover,
                    label = { Text(stringResource(R.string.batch_tag_editor_keep)) },
                )
                FilterChip(
                    selected = cover.action == BatchCoverAction.REPLACE,
                    onClick = onPickCover,
                    label = { Text(stringResource(R.string.batch_tag_editor_cover_replace)) },
                )
                FilterChip(
                    selected = cover.action == BatchCoverAction.DELETE,
                    onClick = onDeleteCover,
                    label = { Text(stringResource(R.string.batch_tag_editor_cover_remove)) },
                    colors =
                        if (cover.action == BatchCoverAction.DELETE) {
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        } else {
                            FilterChipDefaults.filterChipColors()
                        },
                )
            }
            when {
                cover.loadFailed -> {
                    Text(
                        text = stringResource(R.string.batch_tag_editor_cover_load_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                cover.action == BatchCoverAction.DELETE -> {
                    Text(
                        text = stringResource(R.string.batch_tag_editor_cover_delete_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun coverDescription(cover: BatchCoverState): String =
    when (cover.action) {
        BatchCoverAction.KEEP -> {
            if (cover.preview != null) {
                stringResource(R.string.batch_tag_editor_cover_keep_note)
            } else {
                stringResource(R.string.batch_tag_editor_cover_none)
            }
        }

        BatchCoverAction.REPLACE -> {
            stringResource(R.string.batch_tag_editor_cover_replace_note)
        }

        BatchCoverAction.DELETE -> {
            stringResource(R.string.batch_tag_editor_cover_delete_note)
        }
    }

@Composable
private fun BatchCoverThumbnail(
    bitmap: Bitmap?,
    modifier: Modifier,
) {
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.album_art_preview_content_description),
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_album_24dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxSize(0.5f),
            )
        }
    }
}
