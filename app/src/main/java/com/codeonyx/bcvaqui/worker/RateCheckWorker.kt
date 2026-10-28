package com.codeonyx.bcvaqui.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.codeonyx.bcvaqui.data.local.PreferencesRepository
import com.codeonyx.bcvaqui.data.repository.FxRepository
import com.codeonyx.bcvaqui.notification.NotificationHelper
import com.codeonyx.bcvaqui.widget.Widgets
import kotlinx.coroutines.flow.first

/** Trabajo periódico unificado: refresca tasas, avisa si cambió y actualiza widgets. */
class RateCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = RateSync.run(applicationContext)
}

object RateSync {
    /** Recordatorios de deudas: máx. una notificación al día, reutiliza este worker. */
    private suspend fun remindDebts(context: Context) = runCatching {
        val repo = com.codeonyx.bcvaqui.data.local.FinanceRepository(context)
        val today = java.time.LocalDate.now()
        val due = com.codeonyx.bcvaqui.domain.FinancePlanner.debtsToRemind(repo.debts.first(), today)
        if (due.isEmpty() || !repo.claimReminderDay(today.toEpochDay())) return@runCatching
        val overdue = due.count { it.second.overdue }
        val title = if (overdue > 0) "Tienes $overdue pago(s) vencido(s)" else "Pagos próximos"
        val body = due.take(4).joinToString("\n") { (d, s) ->
            val who = if (d.iOwe) "Pagar a ${d.person}" else "Cobrar a ${d.person}"
            val cur = if (d.currency == com.codeonyx.bcvaqui.domain.Money.USD) "$" else "Bs"
            val whenTxt = when {
                s.daysToNext!! < 0 -> "vencido hace ${-s.daysToNext} d"
                s.daysToNext == 0L -> "hoy"
                else -> "en ${s.daysToNext} d"
            }
            "$who: $cur ${com.codeonyx.bcvaqui.domain.FxCalculator.format2(s.nextAmount)} ($whenTxt)"
        }
        NotificationHelper.showDebtReminder(context, title, body)
    }

    suspend fun run(context: Context): Result {
        if (PreferencesRepository(context).preferences.first().debtReminders) remindDebts(context)
        val preferencesRepository = PreferencesRepository(context)
        val repository = FxRepository(preferencesRepository)
        val prefs = preferencesRepository.preferences.first()

        return repository.refreshDashboard(forceNetwork = true).fold(
            onSuccess = { dashboard ->
                if (dashboard.isFromCache) return@fold Result.retry()
                if (prefs.notificationIntervalHours > 0) {
                    val current = mapOf(
                        com.codeonyx.bcvaqui.domain.NotifySource.BCV_USD to dashboard.bcvRates["USD"]?.rateToVes,
                        com.codeonyx.bcvaqui.domain.NotifySource.BCV_EUR to dashboard.bcvRates["EUR"]?.rateToVes,
                        com.codeonyx.bcvaqui.domain.NotifySource.USDT to dashboard.binanceUsdtVes
                    )
                    val enabled = buildSet {
                        if (prefs.notifyBcvUsd) add(com.codeonyx.bcvaqui.domain.NotifySource.BCV_USD)
                        if (prefs.notifyBcvEur) add(com.codeonyx.bcvaqui.domain.NotifySource.BCV_EUR)
                        if (prefs.notifyUsdt) add(com.codeonyx.bcvaqui.domain.NotifySource.USDT)
                    }
                    val quiet = com.codeonyx.bcvaqui.domain.RateNotifier.isQuietHour(
                        java.time.LocalTime.now().hour, prefs.quietHoursEnabled, prefs.quietStartHour, prefs.quietEndHour
                    )
                    val decision = com.codeonyx.bcvaqui.domain.RateNotifier.decide(
                        current, preferencesRepository.notifyBaselines(), enabled, prefs.alertThresholdPercent, quiet
                    )
                    preferencesRepository.setNotifyBaselines(decision.baselines)
                    NotificationHelper.showRateNotifications(context, decision.notifications)
                }
                Widgets.updateAll(context)
                Result.success()
            },
            onFailure = { Result.retry() }
        )
    }
}
