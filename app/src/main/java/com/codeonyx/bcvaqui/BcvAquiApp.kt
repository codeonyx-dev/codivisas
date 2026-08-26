package com.codeonyx.bcvaqui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.codeonyx.bcvaqui.worker.WorkScheduler

class BcvAquiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        WorkScheduler.schedulePeriodicRateCheck(this)
        WorkScheduler.scheduleUpdateCheck(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_RATE_CHANGES,
                getString(R.string.channel_rate_changes),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_APP_UPDATES,
                getString(R.string.channel_app_updates),
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    companion object {
        const val CHANNEL_RATE_CHANGES = "rate_changes"
        const val CHANNEL_APP_UPDATES = "app_updates"
    }
}
