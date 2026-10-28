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
import com.codeonyx.bcvaqui.domain.SyncPolicy
import java.time.ZonedDateTime
import com.codeonyx.bcvaqui.data.local.PreferencesRepository
import com.codeonyx.bcvaqui.widget.Widgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Obsoleto: se mantiene sólo para que trabajos ya encolados por versiones anteriores
 * no fallen al instanciarse. Delega en el sync unificado.
 */
class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = RateSync.run(applicationContext)
}

/**
 * Schedules background work for widget refresh and optional rate notifications.
 *
 * Adjust intervals in [ApiConfig.WIDGET_UPDATE_INTERVAL_MINUTES] and user settings.
 */
object WorkScheduler {
    // Nombres antiguos: se cancelan al reprogramar (había dos periódicos haciendo lo mismo).
    private const val LEGACY_RATE_CHECK_WORK = "rate_check_work"
    private const val LEGACY_WIDGET_REFRESH_WORK = "widget_refresh_work"

    private const val RATE_SYNC_WORK = "rate_sync_work"
    private const val BCV_PUBLISH_WORK = "bcv_publish_check"
    private const val IMMEDIATE_REFRESH_WORK = "immediate_widget_refresh"
    private const val UPDATE_CHECK_WORK = "update_check_work"
    private val schedulerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val network = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun scheduleAllBackgroundWork(context: Context) {
        val appContext = context.applicationContext
        rescheduleRateSync(appContext)
        scheduleUpdateCheck(appContext)
    }

    /** Refresco inmediato (botón del widget, arranque del equipo). No hace nada si no hay widgets. */
    fun enqueueImmediateRefresh(context: Context) {
        val appContext = context.applicationContext
        if (!Widgets.hasAnyWidget(appContext)) return
        enqueueImmediate(appContext)
    }

    fun enqueueImmediateRateCheck(context: Context) = enqueueImmediate(context.applicationContext)

    private fun enqueueImmediate(context: Context) {
        val request = OneTimeWorkRequestBuilder<RateCheckWorker>()
            .setConstraints(network)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_REFRESH_WORK,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun onWidgetAdded(context: Context) {
        rescheduleRateSync(context.applicationContext)
        enqueueImmediate(context.applicationContext)
    }

    /** Llamar cuando se quita el último widget de un tipo. */
    fun onWidgetRemoved(context: Context) {
        rescheduleRateSync(context.applicationContext)
    }

    /** Compatibilidad con Ajustes: reprograma según notificaciones y widgets. */
    fun schedulePeriodicRateCheck(context: Context) = rescheduleRateSync(context.applicationContext)

    /**
     * Un único trabajo periódico (tasas + notificación + widgets) y un chequeo diario
     * a la hora de publicación del BCV. Si no hay widgets ni notificaciones, no se programa nada.
     */
    fun rescheduleRateSync(context: Context) {
        val appContext = context.applicationContext
        schedulerScope.launch {
            val hours = PreferencesRepository(appContext).preferences.first().notificationIntervalHours
            val hasWidgets = Widgets.hasAnyWidget(appContext)
            val wm = WorkManager.getInstance(appContext)
            wm.cancelUniqueWork(LEGACY_RATE_CHECK_WORK)
            wm.cancelUniqueWork(LEGACY_WIDGET_REFRESH_WORK)

            val interval = SyncPolicy.periodicIntervalMinutes(hasWidgets, hours)
            if (interval == null) {
                wm.cancelUniqueWork(RATE_SYNC_WORK)
                wm.cancelUniqueWork(BCV_PUBLISH_WORK)
                return@launch
            }
            val periodic = PeriodicWorkRequestBuilder<RateCheckWorker>(interval, TimeUnit.MINUTES)
                .setConstraints(network)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()
            wm.enqueueUniquePeriodicWork(RATE_SYNC_WORK, ExistingPeriodicWorkPolicy.UPDATE, periodic)
            scheduleNextPublishCheck(appContext)
        }
    }

    /** Encadena un trabajo único para el próximo día hábil a las 17:30 (Caracas). */
    fun scheduleNextPublishCheck(context: Context) {
        val delay = SyncPolicy.delayToNextBcvPublishMs(ZonedDateTime.now())
        val request = OneTimeWorkRequestBuilder<BcvPublishWorker>()
            .setConstraints(network)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 20, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            BCV_PUBLISH_WORK,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun scheduleUpdateCheck(context: Context) {
        val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(12, TimeUnit.HOURS)
            .setConstraints(network)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UPDATE_CHECK_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}

/** Chequeo a la hora de publicación del BCV; luego se reprograma para el siguiente día hábil. */
class BcvPublishWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val result = RateSync.run(applicationContext)
        if (result !is Result.Retry) WorkScheduler.scheduleNextPublishCheck(applicationContext)
        return result
    }
}
