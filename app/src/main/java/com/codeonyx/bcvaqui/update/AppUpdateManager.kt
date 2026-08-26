package com.codeonyx.bcvaqui.update

import android.app.DownloadManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import com.codeonyx.bcvaqui.BcvAquiApp
import com.codeonyx.bcvaqui.BuildConfig
import com.codeonyx.bcvaqui.MainActivity
import com.codeonyx.bcvaqui.R
import com.codeonyx.bcvaqui.data.ApiConfig
import com.codeonyx.bcvaqui.data.model.UpdateManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

object AppUpdateManager {
    private val json = Json { ignoreUnknownKeys = true }
    private const val UPDATE_NOTIFICATION_ID = 2001
    const val ACTION_INSTALL_UPDATE = "com.codeonyx.bcvaqui.update.ACTION_INSTALL_UPDATE"
    const val EXTRA_DOWNLOAD_ID = "download_id"

    suspend fun checkForUpdate(context: Context, notifyIfAvailable: Boolean = true): UpdateManifest? {
        return withContext(Dispatchers.IO) {
            runCatching {
                val connection = java.net.URL(ApiConfig.UPDATE_MANIFEST_URL).openConnection().apply {
                    connectTimeout = 15_000
                    readTimeout = 15_000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "SpreadVE/${BuildConfig.VERSION_NAME}")
                }
                val body = connection.getInputStream().bufferedReader().use { it.readText() }
                val manifest = json.decodeFromString<UpdateManifest>(body)
                if (manifest.versionCode > BuildConfig.VERSION_CODE) {
                    if (notifyIfAvailable) {
                        showUpdateNotification(context, manifest)
                    }
                    manifest
                } else {
                    null
                }
            }.getOrNull()
        }
    }

    fun showUpdateNotification(context: Context, manifest: UpdateManifest) {
        val notes = manifest.releaseNotes?.takeIf { it.isNotBlank() }
            ?: "Versión ${manifest.versionName} disponible"
        val downloadIntent = Intent(context, UpdateDownloadReceiver::class.java).apply {
            action = ACTION_INSTALL_UPDATE
            putExtra("apk_url", manifest.apkUrl)
            putExtra("version_name", manifest.versionName)
        }
        val downloadPending = PendingIntent.getBroadcast(
            context,
            UPDATE_NOTIFICATION_ID,
            downloadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openIntent = Intent(context, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            context,
            UPDATE_NOTIFICATION_ID + 1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, BcvAquiApp.CHANNEL_APP_UPDATES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Actualización de SpreadVE")
            .setContentText("Nueva versión ${manifest.versionName}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(notes))
            .setContentIntent(openPending)
            .addAction(0, "Descargar e instalar", downloadPending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(UPDATE_NOTIFICATION_ID, notification)
    }

    fun startDownload(context: Context, apkUrl: String, versionName: String): Long {
        val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
            setTitle("SpreadVE $versionName")
            setDescription("Descargando actualización…")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "SpreadVE-$versionName.apk")
            setAllowedOverMetered(true)
            setAllowedOverRoaming(false)
        }
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = manager.enqueue(request)

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id != downloadId) return
                runCatching { ctx.unregisterReceiver(this) }
                promptInstall(ctx, downloadId)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
        return downloadId
    }

    fun promptInstall(context: Context, downloadId: Long) {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = manager.getUriForDownloadedFile(downloadId) ?: return
        val install = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(install)
    }

    fun promptInstallFromFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val install = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(install)
    }
}

class UpdateDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AppUpdateManager.ACTION_INSTALL_UPDATE) return
        val url = intent.getStringExtra("apk_url") ?: return
        val version = intent.getStringExtra("version_name") ?: "nueva"
        AppUpdateManager.startDownload(context.applicationContext, url, version)
    }
}
