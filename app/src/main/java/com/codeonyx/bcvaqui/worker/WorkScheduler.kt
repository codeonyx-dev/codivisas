package com.codeonyx.bcvaqui.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.codeonyx.bcvaqui.data.ApiConfig
import com.codeonyx.bcvaqui.data.local.PreferencesRepository
import com.codeonyx.bcvaqui.data.repository.FxRepository
import com.codeonyx.bcvaqui.widget.RateWidgetProvider
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/** Refreshes cached rates and updates the home screen widget (no notifications). */
class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repository = FxRepository(PreferencesRepository(applicationContext))
        return repository.refreshDashboard(forceNetwork = true).fold(
            onSuccess = {
                RateWidgetProvider.updateAllWidgets(applicationContext)
                Result.success()
            },
            onFailure = { Result.retry() }
        )
    }
}

/**
 * Schedules background work for widget refresh and optional rate notifications.
 *
 * Adjust intervals in [ApiConfig.WIDGET_UPDATE_INTERVAL_MINUTES] and user settings.
 */
object WorkScheduler {
    private const val RATE_CHECK_WORK = "rate_check_work"
    private const val WIDGET_REFRESH_WORK = "widget_refresh_work"

    fun schedulePeriodicRateCheck(context: Context) {
        val preferencesRepository = PreferencesRepository(context)
        val intervalHours = kotlinx.coroutines.runBlocking {
            preferencesRepository.preferences.first().notificationIntervalHours
        }

        WorkManager.getInstance(context).cancelUniqueWork(RATE_CHECK_WORK)
        if (intervalHours <= 0) return

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<RateCheckWorker>(
            intervalHours.toLong(),
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            RATE_CHECK_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleWidgetRefresh(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(
            ApiConfig.WIDGET_UPDATE_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WIDGET_REFRESH_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
