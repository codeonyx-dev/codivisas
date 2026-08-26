package com.codeonyx.bcvaqui.widget

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.codeonyx.bcvaqui.MainActivity
import com.codeonyx.bcvaqui.R
import com.codeonyx.bcvaqui.data.local.PreferencesRepository
import com.codeonyx.bcvaqui.data.local.UserPreferences
import com.codeonyx.bcvaqui.domain.FxCalculator
import com.codeonyx.bcvaqui.domain.RateSources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class WidgetSnapshot(
    val prefs: UserPreferences,
    val currency: String,
    val rate: Double?,
    val dayChangePct: Double?,
    val alertActive: Boolean,
    val alertUp: Boolean
)

object Widgets {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun loadSnapshot(context: Context): WidgetSnapshot {
        val repo = PreferencesRepository(context)
        val prefs = repo.preferences.first()
        val cached = repo.decodeCachedRates(prefs.lastKnownRatesJson)
        val currency = prefs.selectedCurrency
        val rate = RateSources.effectiveRate(
            currency = currency,
            bcvRates = cached,
            binanceUsdtVes = prefs.lastKnownBinanceUsdtVes
        ) ?: prefs.variationTodayRate
            ?: prefs.lastKnownSelectedCurrencyRate
            ?: if (currency == "USD") prefs.lastKnownUsdRate else null

        val previous = prefs.previousDaySelectedRate
        val dayChangePct = if (rate != null && previous != null && previous > 0) {
            FxCalculator.round2((rate - previous) / previous * 100.0)
        } else null

        val threshold = prefs.alertThresholdPercent
        val alertActive = threshold > 0 && dayChangePct != null && kotlin.math.abs(dayChangePct) >= threshold
        val alertUp = (dayChangePct ?: 0.0) >= 0

        return WidgetSnapshot(prefs, currency, rate, dayChangePct, alertActive, alertUp)
    }

    fun alertBackground(snapshot: WidgetSnapshot): Int = when {
        !snapshot.alertActive -> R.drawable.widget_background
        snapshot.alertUp -> R.drawable.widget_background_alert_up
        else -> R.drawable.widget_background_alert_down
    }

    fun openAppPending(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun applyBrandAndAlert(views: RemoteViews, rootId: Int, snapshot: WidgetSnapshot) {
        views.setInt(rootId, "setBackgroundResource", alertBackground(snapshot))
    }

    fun updateAll(context: Context) {
        runAsync {
            val appContext = context.applicationContext
            val snapshot = loadSnapshot(appContext)
            RateWidgetProvider.updateAllWidgets(appContext, snapshot)
            MiniRateWidgetProvider.updateAllWidgets(appContext, snapshot)
            SpreadWidgetProvider.updateAllWidgets(appContext, snapshot)
            ConverterWidgetProvider.updateAllWidgets(appContext, snapshot)
            VariationWidgetProvider.updateAllWidgets(appContext, snapshot)
            ShareRateWidgetProvider.updateAllWidgets(appContext, snapshot)
        }
    }

    fun runAsync(
        pendingResult: BroadcastReceiver.PendingResult? = null,
        block: suspend () -> Unit
    ) {
        scope.launch {
            try {
                block()
            } finally {
                pendingResult?.finish()
            }
        }
    }
}
