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

package com.ssilencce.ui.screen.setup

import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ssilencce.R
import com.ssilencce.core.model.filesystem.StorageDevice
import com.ssilencce.core.sort.sortedByName
import com.ssilencce.util.StorageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Compose folder browser over the mounted storage volumes. It mirrors the legacy
 * `FolderChooserDialog` behavior: start at the volume list, navigate into directories, and select
 * the current directory.
 */
@Composable
fun SetupFolderPickerDialog(
    title: String,
    startPath: String?,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    var storageDevices by remember { mutableStateOf<List<StorageDevice>>(emptyList()) }
    var currentPath by rememberSaveable { mutableStateOf(startPath) }
    var directories by remember { mutableStateOf<List<File>>(emptyList()) }
    var loadingDirectories by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val loaded =
            withContext(Dispatchers.IO) {
                StorageUtil.refreshStorageVolumes()
            }.ifEmpty {
                val fallback = Environment.getExternalStorageDirectory()
                listOf(
                    StorageDevice(
                        filePath = fallback.path,
                        fileName = fallback.name.ifEmpty { fallback.path },
                        iconRes = R.drawable.ic_phone_android_24dp,
                    ),
                )
            }
        storageDevices = loaded.distinctBy { it.filePath }
    }

    LaunchedEffect(currentPath) {
        val path = currentPath
        if (path == null) {
            directories = emptyList()
            loadingDirectories = false
            return@LaunchedEffect
        }
        loadingDirectories = true
        directories =
            withContext(Dispatchers.IO) {
                runCatching {
                    File(path)
                        .listFiles { file ->
                            file.isDirectory && file.canRead() && !file.name.startsWith(".")
                        }?.toList()
                        ?.sortedByName { it.name }
                        .orEmpty()
                }.getOrDefault(emptyList())
            }
        loadingDirectories = false
    }

    val isAtDeviceRoot = currentPath != null && storageDevices.any { it.filePath == currentPath }
    val canGoUp = currentPath != null
    val currentName = currentPath?.let { File(it).name.ifEmpty { it } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth(0.94f)
                    .heightIn(min = 320.dp, max = 560.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = currentName ?: stringResource(R.string.setup_wizard_storage_title),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close_24dp),
                            contentDescription = stringResource(R.string.close_action),
                        )
                    }
                }

                if (currentPath != null) {
                    Text(
                        text = currentPath.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                    )
                } else {
                    Spacer(Modifier.padding(top = 8.dp))
                }

                HorizontalDivider()

                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp)
                            .padding(vertical = 4.dp),
                ) {
                    if (currentPath == null) {
                        items(storageDevices, key = { it.filePath }) { device ->
                            FolderPickerRow(
                                iconRes = device.iconRes,
                                title = device.fileName,
                                subtitle = device.filePath,
                                onClick = { currentPath = device.filePath },
                            )
                        }
                    } else {
                        if (canGoUp) {
                            item(key = "up") {
                                FolderPickerRow(
                                    iconRes = R.drawable.ic_keyboard_arrow_up_24dp,
                                    title = stringResource(R.string.setup_wizard_up),
                                    subtitle = null,
                                    onClick = {
                                        currentPath =
                                            if (isAtDeviceRoot) {
                                                null
                                            } else {
                                                File(currentPath.orEmpty()).parentFile?.absolutePath
                                            }
                                    },
                                )
                            }
                        }
                        if (loadingDirectories) {
                            item(key = "loading") {
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                }
                            }
                        } else if (directories.isEmpty()) {
                            item(key = "empty") {
                                Text(
                                    text = stringResource(R.string.setup_wizard_no_subfolders),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        } else {
                            items(directories, key = { it.absolutePath }) { directory ->
                                FolderPickerRow(
                                    iconRes = R.drawable.ic_folder_24dp,
                                    title = directory.name,
                                    subtitle = directory.absolutePath,
                                    onClick = { currentPath = directory.absolutePath },
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = { currentPath?.let(onSelect) },
                        enabled = currentPath != null,
                    ) {
                        Text(stringResource(R.string.setup_wizard_use_this_folder))
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderPickerRow(
    iconRes: Int,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
