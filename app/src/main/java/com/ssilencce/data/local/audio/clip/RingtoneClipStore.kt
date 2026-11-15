package com.ssilencce.data.local.audio.clip

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.ssilencce.extensions.hasQ
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

enum class ClipToneKind {
    Ringtone,
    Notification,
    Alarm,
    ;

    internal val directory: String
        get() =
            when (this) {
                Ringtone -> Environment.DIRECTORY_RINGTONES
                Notification -> Environment.DIRECTORY_NOTIFICATIONS
                Alarm -> Environment.DIRECTORY_ALARMS
            }

    internal val ringtoneManagerType: Int
        get() =
            when (this) {
                Ringtone -> RingtoneManager.TYPE_RINGTONE
                Notification -> RingtoneManager.TYPE_NOTIFICATION
                Alarm -> RingtoneManager.TYPE_ALARM
            }

    internal val mediaStoreFlag: String
        get() =
            when (this) {
                Ringtone -> MediaStore.Audio.Media.IS_RINGTONE
                Notification -> MediaStore.Audio.Media.IS_NOTIFICATION
                Alarm -> MediaStore.Audio.Media.IS_ALARM
            }
}

sealed interface RingtoneSetResult {
    data class Success(
        val uri: Uri,
    ) : RingtoneSetResult

    data object WriteSettingsDenied : RingtoneSetResult

    data object StoragePermissionDenied : RingtoneSetResult

    data class Failed(
        val detail: String? = null,
    ) : RingtoneSetResult
}

/**
 * Publishes a trimmed clip where the system can play it and makes it the default ringtone,
 * notification sound or alarm sound.
 */
class RingtoneClipStore(
    private val context: Context,
    private val exporter: AudioClipExporter,
) {
    private val contentResolver: ContentResolver = context.contentResolver

    fun canWriteSettings(): Boolean = Settings.System.canWrite(context)

    fun hasLegacyStoragePermission(): Boolean =
        hasQ() ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED

    suspend fun exportAndSet(
        source: Uri,
        range: AudioClipRange,
        format: AudioClipFormat,
        kind: ClipToneKind,
        displayName: String,
        title: String,
    ): RingtoneSetResult {
        if (!canWriteSettings()) {
            return RingtoneSetResult.WriteSettingsDenied
        }
        if (!hasQ() && !hasLegacyStoragePermission()) {
            return RingtoneSetResult.StoragePermissionDenied
        }
        val published =
            withContext(Dispatchers.IO) {
                if (hasQ()) {
                    publishToMediaStore(source, range, format, kind, displayName, title)
                } else {
                    publishToLegacyFolder(source, range, format, kind, displayName)
                }
            }
        val uri =
            when (published) {
                is PublishResult.Success -> published.uri
                is PublishResult.Failure -> return RingtoneSetResult.Failed(published.detail)
            }
        return try {
            withContext(Dispatchers.IO) {
                RingtoneManager.setActualDefaultRingtoneUri(context, kind.ringtoneManagerType, uri)
            }
            RingtoneSetResult.Success(uri)
        } catch (throwable: Throwable) {
            RingtoneSetResult.Failed(throwable.message)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun publishToMediaStore(
        source: Uri,
        range: AudioClipRange,
        format: AudioClipFormat,
        kind: ClipToneKind,
        displayName: String,
        title: String,
    ): PublishResult {
        val values =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${kind.directory}/$PUBLISHED_FOLDER")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
                put(MediaStore.Audio.Media.TITLE, title)
                put(MediaStore.Audio.Media.IS_MUSIC, 0)
                put(kind.mediaStoreFlag, 1)
            }
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri =
            runCatching { contentResolver.insert(collection, values) }
                .getOrNull()
                ?: return PublishResult.Failure("Could not create the media entry")
        val export =
            runCatching {
                contentResolver.openOutputStream(uri)?.let { output ->
                    exporter.export(source, range, output, format)
                }
            }.getOrNull()
        if (export !is AudioClipExportResult.Success) {
            deleteMedia(uri)
            return PublishResult.Failure((export as? AudioClipExportResult.Failure)?.detail)
        }
        val published =
            runCatching {
                contentResolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                    null,
                    null,
                )
            }.getOrNull()
        if (published == null || published < 1) {
            deleteMedia(uri)
            return PublishResult.Failure("Could not publish the media entry")
        }
        return PublishResult.Success(uri)
    }

    @Suppress("DEPRECATION")
    private suspend fun publishToLegacyFolder(
        source: Uri,
        range: AudioClipRange,
        format: AudioClipFormat,
        kind: ClipToneKind,
        displayName: String,
    ): PublishResult {
        val directory = File(Environment.getExternalStoragePublicDirectory(kind.directory), PUBLISHED_FOLDER)
        if (!directory.exists() && !directory.mkdirs()) {
            return PublishResult.Failure("Could not create the ${kind.directory} folder")
        }
        val file = uniqueFile(directory, displayName)
        val export = exporter.exportToFile(source, range, file, format)
        if (export !is AudioClipExportResult.Success) {
            file.delete()
            return PublishResult.Failure((export as? AudioClipExportResult.Failure)?.detail)
        }
        val uri =
            scanFile(file, format.mimeType)
                ?: run {
                    file.delete()
                    return PublishResult.Failure("Could not add the clip to the media library")
                }
        return PublishResult.Success(uri)
    }

    private suspend fun scanFile(
        file: File,
        mimeType: String,
    ): Uri? =
        suspendCancellableCoroutine { continuation ->
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf(mimeType),
            ) { _, uri ->
                if (continuation.isActive) {
                    continuation.resume(uri)
                }
            }
        }

    private fun uniqueFile(
        directory: File,
        displayName: String,
    ): File {
        val initial = File(directory, displayName)
        if (!initial.exists()) return initial
        val base = displayName.substringBeforeLast('.', displayName)
        val extension = displayName.substringAfterLast('.', "")
        var index = 1
        while (true) {
            val suffix = if (extension.isEmpty()) " ($index)" else " ($index).$extension"
            val candidate = File(directory, base + suffix)
            if (!candidate.exists()) return candidate
            index++
        }
    }

    private fun deleteMedia(uri: Uri) {
        runCatching { contentResolver.delete(uri, null, null) }
    }

    private sealed interface PublishResult {
        data class Success(
            val uri: Uri,
        ) : PublishResult

        data class Failure(
            val detail: String?,
        ) : PublishResult
    }

    private companion object {
        const val PUBLISHED_FOLDER = "ssilencce"
    }
}
