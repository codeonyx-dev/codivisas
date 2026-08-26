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
import com.codeonyx.bcvaqui.R
import com.codeonyx.bcvaqui.domain.FxCalculator
import com.codeonyx.bcvaqui.worker.WidgetRefreshWorker

class RateWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        Widgets.runAsync(pendingResult) {
            val snapshot = Widgets.loadSnapshot(context)
            appWidgetIds.forEach { id ->
                updateWidget(context, appWidgetManager, id, snapshot)
            }
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
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.codeonyx.bcvaqui.widget.ACTION_REFRESH"

        fun updateAllWidgets(context: Context, snapshot: WidgetSnapshot) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, RateWidgetProvider::class.java))
            ids.forEach { id ->
                updateWidget(context, manager, id, snapshot)
            }
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            snap: WidgetSnapshot
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_rate)
            Widgets.applyBrandAndAlert(views, R.id.widget_root, snap)
            views.setTextViewText(
                R.id.widget_currency_label,
                "${FxCalculator.currencyLabel(snap.currency)} · ${snap.currency}"
            )
            if (snap.rate != null) {
                views.setTextViewText(R.id.widget_rate_text, FxCalculator.format2(snap.rate))
                views.setTextViewText(R.id.widget_rate_suffix, "Bs. por 1 ${snap.currency}")
            } else {
                views.setTextViewText(R.id.widget_rate_text, "—")
                views.setTextViewText(R.id.widget_rate_suffix, "Sin datos aún")
            }
            val updated = FxCalculator.formatUpdatedAt(snap.prefs.lastKnownUpdatedAt)
            val alertNote = when {
                !snap.alertActive || snap.dayChangePct == null -> ""
                else -> {
                    val sign = if (snap.dayChangePct > 0) "+" else ""
                    " · $sign${FxCalculator.format2(snap.dayChangePct)}%"
                }
            }
            views.setTextViewText(R.id.widget_updated_text, "Actualizado: $updated$alertNote")

            views.setOnClickPendingIntent(
                R.id.widget_root,
                Widgets.openAppPending(context, appWidgetId)
            )

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
