package com.codeonyx.bcvaqui.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.codeonyx.bcvaqui.data.ApiConfig
import com.codeonyx.bcvaqui.data.local.PreferencesRepository
import com.codeonyx.bcvaqui.data.repository.FxRepository
import com.codeonyx.bcvaqui.widget.Widgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
                Widgets.updateAll(applicationContext)
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
    private const val IMMEDIATE_REFRESH_WORK = "immediate_widget_refresh"
    private const val UPDATE_CHECK_WORK = "update_check_work"
    private val schedulerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun scheduleAllBackgroundWork(context: Context) {
        val appContext = context.applicationContext
        scheduleWidgetRefresh(appContext)
        schedulePeriodicRateCheck(appContext)
        scheduleUpdateCheck(appContext)
    }

    fun enqueueImmediateRefresh(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            IMMEDIATE_REFRESH_WORK,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun enqueueImmediateRateCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<RateCheckWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "immediate_rate_check",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun onWidgetAdded(context: Context) {
        scheduleWidgetRefresh(context.applicationContext)
        enqueueImmediateRefresh(context.applicationContext)
    }

    fun schedulePeriodicRateCheck(context: Context) {
        val appContext = context.applicationContext
        schedulerScope.launch {
            val intervalHours = PreferencesRepository(appContext)
                .preferences.first().notificationIntervalHours
            schedulePeriodicRateCheck(appContext, intervalHours)
        }
    }

    private fun schedulePeriodicRateCheck(context: Context, intervalHours: Int) {
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
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleUpdateCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UPDATE_CHECK_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
