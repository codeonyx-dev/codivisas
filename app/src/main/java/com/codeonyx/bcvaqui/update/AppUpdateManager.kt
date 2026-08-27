package com.codeonyx.bcvaqui.update

import android.app.DownloadManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
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
    private const val PREFS_NAME = "app_update"
    private const val KEY_PENDING_DOWNLOAD_ID = "pending_download_id"
    private const val KEY_PENDING_APK_FILE = "pending_apk_file"
    private const val UPDATE_NOTIFICATION_ID = 2001
    private const val INSTALL_READY_NOTIFICATION_ID = 2002
    const val ACTION_INSTALL_UPDATE = "com.codeonyx.bcvaqui.update.ACTION_INSTALL_UPDATE"
    const val ACTION_INSTALL_DOWNLOADED = "com.codeonyx.bcvaqui.update.ACTION_INSTALL_DOWNLOADED"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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
                        val onWifi = isOnWifi(context)
                        showUpdateNotification(context, manifest, autoDownloading = onWifi)
                        if (onWifi) {
                            startDownload(context, manifest.apkUrl, manifest.versionName, allowMetered = false)
                        }
                    }
                    manifest
                } else {
                    null
                }
            }.getOrNull()
        }
    }

    fun showUpdateNotification(context: Context, manifest: UpdateManifest, autoDownloading: Boolean) {
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

        val contentText = if (autoDownloading) {
            "Descargando versión ${manifest.versionName}…"
        } else {
            "Nueva versión ${manifest.versionName}. Toca para descargar con datos móviles."
        }

        val builder = NotificationCompat.Builder(context, BcvAquiApp.CHANNEL_APP_UPDATES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Actualización de SpreadVE")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notes))
            .setContentIntent(openPending)
            .setAutoCancel(!autoDownloading)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (autoDownloading) {
            builder.setOngoing(true)
        } else {
            builder.addAction(0, "Descargar con datos", downloadPending)
        }

        NotificationManagerCompat.from(context).notify(UPDATE_NOTIFICATION_ID, builder.build())
    }

    fun startDownload(
        context: Context,
        apkUrl: String,
        versionName: String,
        allowMetered: Boolean = true
    ): Long {
        val appContext = context.applicationContext
        val apkFile = apkFileFor(appContext, versionName)
        if (isDownloadInProgress(appContext)) {
            return prefs(appContext).getLong(KEY_PENDING_DOWNLOAD_ID, -1L)
        }

        val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
            setTitle("SpreadVE $versionName")
            setDescription("Descargando actualización…")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(appContext, Environment.DIRECTORY_DOWNLOADS, apkFile.name)
            setMimeType("application/vnd.android.package-archive")
            setAllowedOverMetered(allowMetered)
            setAllowedOverRoaming(false)
        }
        val manager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = manager.enqueue(request)

        prefs(appContext).edit()
            .putLong(KEY_PENDING_DOWNLOAD_ID, downloadId)
            .putString(KEY_PENDING_APK_FILE, apkFile.absolutePath)
            .apply()
        return downloadId
    }

    fun resumePendingInstall(context: Context) {
        val appContext = context.applicationContext
        val downloadId = prefs(appContext).getLong(KEY_PENDING_DOWNLOAD_ID, -1L)
        if (downloadId == -1L) return

        val manager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        manager.query(query).use { cursor ->
            if (!cursor.moveToFirst()) {
                clearPendingDownload(appContext)
                return
            }
            when (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    val apkPath = prefs(appContext).getString(KEY_PENDING_APK_FILE, null)
                    clearPendingDownload(appContext)
                    promptInstall(appContext, downloadId, apkPath)
                }
                DownloadManager.STATUS_FAILED -> {
                    clearPendingDownload(appContext)
                    NotificationManagerCompat.from(appContext).cancel(UPDATE_NOTIFICATION_ID)
                }
            }
        }
    }

    fun onDownloadComplete(context: Context, downloadId: Long) {
        val appContext = context.applicationContext
        val pendingId = prefs(appContext).getLong(KEY_PENDING_DOWNLOAD_ID, -1L)
        if (pendingId != downloadId) return

        val manager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        manager.query(query).use { cursor ->
            if (!cursor.moveToFirst()) {
                clearPendingDownload(appContext)
                return
            }
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            if (status != DownloadManager.STATUS_SUCCESSFUL) {
                clearPendingDownload(appContext)
                NotificationManagerCompat.from(appContext).cancel(UPDATE_NOTIFICATION_ID)
                showDownloadFailedNotification(appContext)
                return
            }
        }

        val apkPath = prefs(appContext).getString(KEY_PENDING_APK_FILE, null)
        clearPendingDownload(appContext)
        NotificationManagerCompat.from(appContext).cancel(UPDATE_NOTIFICATION_ID)
        showInstallReadyNotification(appContext, downloadId)
        promptInstall(appContext, downloadId, apkPath)
    }

    private fun showDownloadFailedNotification(context: Context) {
        val openIntent = Intent(context, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            context,
            INSTALL_READY_NOTIFICATION_ID + 1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, BcvAquiApp.CHANNEL_APP_UPDATES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("No se pudo descargar la actualización")
            .setContentText("Abre SpreadVE e intenta de nuevo desde Ajustes.")
            .setContentIntent(openPending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(INSTALL_READY_NOTIFICATION_ID, notification)
    }

    private fun showInstallReadyNotification(context: Context, downloadId: Long) {
        val installIntent = Intent(context, UpdateDownloadReceiver::class.java).apply {
            action = ACTION_INSTALL_DOWNLOADED
            putExtra("download_id", downloadId)
        }
        val installPending = PendingIntent.getBroadcast(
            context,
            INSTALL_READY_NOTIFICATION_ID,
            installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, BcvAquiApp.CHANNEL_APP_UPDATES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Actualización lista")
            .setContentText("Toca para instalar SpreadVE")
            .setContentIntent(installPending)
            .addAction(0, "Instalar", installPending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(INSTALL_READY_NOTIFICATION_ID, notification)
    }

    fun promptInstall(context: Context, downloadId: Long, savedApkPath: String? = null) {
        val appContext = context.applicationContext
        val manager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.getUriForDownloadedFile(downloadId)?.let { uri ->
            launchInstallIntent(appContext, uri)
            return
        }

        val apkPath = savedApkPath ?: prefs(appContext).getString(KEY_PENDING_APK_FILE, null)
        if (apkPath != null) {
            val file = File(apkPath)
            if (file.exists()) {
                promptInstallFromFile(appContext, file)
                return
            }
        }

        val query = DownloadManager.Query().setFilterById(downloadId)
        manager.query(query).use { cursor ->
            if (!cursor.moveToFirst()) return
            val localUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
            val file = localUri?.let { Uri.parse(it).path?.let(::File) }
            if (file != null && file.exists()) {
                promptInstallFromFile(appContext, file)
            }
        }
    }

    fun promptInstallFromFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        launchInstallIntent(context, uri)
    }

    private fun launchInstallIntent(context: Context, uri: Uri) {
        val install = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(install) }
    }

    private fun apkFileFor(context: Context, versionName: String): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
        return File(dir, "SpreadVE-$versionName.apk")
    }

    private fun isDownloadInProgress(context: Context): Boolean {
        val downloadId = prefs(context).getLong(KEY_PENDING_DOWNLOAD_ID, -1L)
        if (downloadId == -1L) return false
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        manager.query(query).use { cursor ->
            if (!cursor.moveToFirst()) return false
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            return status == DownloadManager.STATUS_PENDING ||
                status == DownloadManager.STATUS_RUNNING ||
                status == DownloadManager.STATUS_PAUSED
        }
    }

    private fun clearPendingDownload(context: Context) {
        prefs(context).edit()
            .remove(KEY_PENDING_DOWNLOAD_ID)
            .remove(KEY_PENDING_APK_FILE)
            .apply()
    }

    private fun isOnWifi(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}

class UpdateDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AppUpdateManager.ACTION_INSTALL_UPDATE -> {
                val url = intent.getStringExtra("apk_url") ?: return
                val version = intent.getStringExtra("version_name") ?: "nueva"
                AppUpdateManager.startDownload(context.applicationContext, url, version, allowMetered = true)
            }
            AppUpdateManager.ACTION_INSTALL_DOWNLOADED -> {
                val downloadId = intent.getLongExtra("download_id", -1L)
                if (downloadId != -1L) {
                    AppUpdateManager.promptInstall(context.applicationContext, downloadId)
                }
            }
        }
    }
}

class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (downloadId != -1L) {
            AppUpdateManager.onDownloadComplete(context.applicationContext, downloadId)
        }
    }
}
