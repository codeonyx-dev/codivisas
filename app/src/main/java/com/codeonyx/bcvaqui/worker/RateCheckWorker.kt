package com.codeonyx.bcvaqui.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.codeonyx.bcvaqui.data.local.PreferencesRepository
import com.codeonyx.bcvaqui.data.repository.FxRepository
import com.codeonyx.bcvaqui.notification.NotificationHelper
import com.codeonyx.bcvaqui.widget.Widgets
import kotlinx.coroutines.flow.first

class RateCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val preferencesRepository = PreferencesRepository(applicationContext)
        val repository = FxRepository(preferencesRepository)
        val prefs = preferencesRepository.preferences.first()
        val currency = prefs.selectedCurrency
        val previousRate = prefs.lastKnownSelectedCurrencyRate

        return repository.refreshDashboard(forceNetwork = true).fold(
            onSuccess = { dashboard ->
                val newRate = dashboard.bcvRates[currency]?.rateToVes

                if (newRate != null && previousRate != null && newRate != previousRate) {
                    NotificationHelper.showRateChangeNotification(
                        applicationContext,
                        currency,
                        previousRate,
                        newRate
                    )
                }

                Widgets.updateAll(applicationContext)
                Result.success()
            },
            onFailure = {
                Result.retry()
            }
        )
    }
}
