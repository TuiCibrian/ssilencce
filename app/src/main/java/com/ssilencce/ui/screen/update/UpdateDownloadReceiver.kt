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

package com.ssilencce.ui.screen.update

import android.Manifest
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.ssilencce.R
import com.ssilencce.util.Preferences
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

class UpdateDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        if (downloadId != Preferences.lastUpdateId) return

        // APK parsing must not run on the main thread
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        Thread {
            try {
                handleDownloadComplete(appContext, downloadId)
            } finally {
                pendingResult.finish()
            }
        }.start()
    }

    private fun handleDownloadComplete(
        context: Context,
        downloadId: Long,
    ) {
        val downloadManager = context.getSystemService<DownloadManager>() ?: return
        var apkFile: File? = null
        var fileName: String? = null
        downloadManager.query(DownloadManager.Query().setFilterById(downloadId)).use { cursor ->
            if (!cursor.moveToFirst()) return
            if (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) != DownloadManager.STATUS_SUCCESSFUL) {
                return
            }
            fileName = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE))
            apkFile = resolveApkFile(context, downloadId, cursor)
        }
        if (apkFile != null && isSignatureValid(context, apkFile)) {
            runCatching { apkFile.delete() }
            Preferences.lastUpdateId = -1
            return
        }
        // Signature mismatch: remove the download so DownloadManager's "Open" notification is
        // dismissed, then delete the artifact and notify the user
        runCatching { downloadManager.remove(downloadId) }
        apkFile?.let { runCatching { it.delete() } }
        Preferences.lastUpdateId = -1
        showSignatureMismatchNotification(context, fileName)
    }

    /**
     * API 29+ no longer exposes [DownloadManager.COLUMN_LOCAL_FILENAME] to apps, so the APK is
     * copied from [DownloadManager.COLUMN_LOCAL_URI] into a cache file that [getPackageArchiveInfo]
     * can parse. The cache copy is always deleted after verification.
     */
    private fun resolveApkFile(
        context: Context,
        downloadId: Long,
        cursor: Cursor,
    ): File? {
        val source =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val uriString = runCatching { cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)) }.getOrNull()
                uriString
                    ?.let { runCatching { Uri.parse(it) }.getOrNull() }
                    ?.let { runCatching { context.contentResolver.openInputStream(it) }.getOrNull() }
            } else {
                val path = runCatching { cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_FILENAME)) }.getOrNull()
                if (path.isNullOrEmpty()) {
                    null
                } else {
                    runCatching { FileInputStream(File(path)) }.getOrNull()
                }
            }
        val tempFile = File(context.cacheDir, "update_verify_$downloadId.apk")
        val copied =
            runCatching {
                val input = source ?: return@runCatching false
                input.use { stream ->
                    tempFile.outputStream().use { output -> stream.copyTo(output) }
                }
                true
            }.getOrDefault(false)
        if (!copied) {
            runCatching { tempFile.delete() }
            return null
        }
        return tempFile
    }

    @Suppress("DEPRECATION")
    private fun isSignatureValid(
        context: Context,
        apkFile: File,
    ): Boolean {
        val installedInfo =
            try {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            } catch (e: PackageManager.NameNotFoundException) {
                return false
            }
        val archiveInfo =
            try {
                context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNATURES) ?: return false
            } catch (e: Exception) {
                return false
            }
        val installedDigest = signerDigest(installedInfo)
        val archiveDigest = signerDigest(archiveInfo)
        return installedDigest != null && archiveDigest != null && installedDigest.contentEquals(archiveDigest)
    }

    private fun signerDigest(info: PackageInfo): ByteArray? {
        val signer =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.signingInfo?.apkContentsSigners?.firstOrNull()
            } else {
                info.signatures?.firstOrNull()
            }
        return signer?.toByteArray()?.let { MessageDigest.getInstance("SHA-256").digest(it) }
    }

    private fun showSignatureMismatchNotification(
        context: Context,
        fileName: String?,
    ) {
        val notificationManager = context.getSystemService<NotificationManager>() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.update_notification_channel),
                    NotificationManager.IMPORTANCE_HIGH,
                ),
            )
        }
        val message = context.getString(R.string.update_signature_mismatch_message, fileName ?: context.getString(R.string.app_name))
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_error_24dp)
                .setContentTitle(context.getString(R.string.update_signature_mismatch_title))
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .build()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val CHANNEL_ID = "update_verification"
        private const val NOTIFICATION_ID = 9001
    }
}
