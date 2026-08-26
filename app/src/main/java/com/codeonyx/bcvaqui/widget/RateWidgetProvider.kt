package com.codeonyx.bcvaqui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.codeonyx.bcvaqui.MainActivity
import com.codeonyx.bcvaqui.R
import com.codeonyx.bcvaqui.data.local.PreferencesRepository
import com.codeonyx.bcvaqui.domain.FxCalculator
import com.codeonyx.bcvaqui.worker.WidgetRefreshWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class RateWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            updateWidget(context, appWidgetManager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueue(request)
            // Optimistic UI refresh from cache while network fetch runs
            updateAllWidgets(context)
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.codeonyx.bcvaqui.widget.ACTION_REFRESH"

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, RateWidgetProvider::class.java))
            ids.forEach { id ->
                updateWidget(context, manager, id)
            }
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = runBlocking {
                PreferencesRepository(context).preferences.first()
            }
            val cachedRates = PreferencesRepository(context).decodeCachedRates(prefs.lastKnownRatesJson)
            val currency = prefs.selectedCurrency
            val rate = cachedRates[currency]?.rateToVes
                ?: if (currency == "USD") prefs.lastKnownUsdRate else null

            val views = RemoteViews(context.packageName, R.layout.widget_rate)
            views.setTextViewText(
                R.id.widget_currency_label,
                "${FxCalculator.currencyLabel(currency)} · $currency"
            )
            if (rate != null) {
                views.setTextViewText(R.id.widget_rate_text, FxCalculator.format2(rate))
                views.setTextViewText(R.id.widget_rate_suffix, "Bs. por 1 $currency")
            } else {
                views.setTextViewText(R.id.widget_rate_text, "—")
                views.setTextViewText(R.id.widget_rate_suffix, "Sin datos aún")
            }
            views.setTextViewText(
                R.id.widget_updated_text,
                "Actualizado: ${FxCalculator.formatUpdatedAt(prefs.lastKnownUpdatedAt)}"
            )

            val openIntent = Intent(context, MainActivity::class.java)
            val openPending = PendingIntent.getActivity(
                context,
                appWidgetId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openPending)

            val refreshIntent = Intent(context, RateWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPending = PendingIntent.getBroadcast(
                context,
                appWidgetId + 1000,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh, refreshPending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
