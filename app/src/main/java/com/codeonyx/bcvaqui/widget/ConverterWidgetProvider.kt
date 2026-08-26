package com.codeonyx.bcvaqui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.codeonyx.bcvaqui.R
import com.codeonyx.bcvaqui.data.local.PreferencesRepository
import com.codeonyx.bcvaqui.domain.FxCalculator
import com.codeonyx.bcvaqui.domain.RateSources

class ConverterWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        Widgets.runAsync(pendingResult) {
            val snapshot = Widgets.loadSnapshot(context)
            appWidgetIds.forEach { updateWidget(context, appWidgetManager, it, snapshot) }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_CYCLE_AMOUNT) {
            val pendingResult = goAsync()
            Widgets.runAsync(pendingResult) {
                PreferencesRepository(context).cycleWidgetConvertAmount()
                updateAllWidgets(context, Widgets.loadSnapshot(context))
            }
        }
    }

    companion object {
        const val ACTION_CYCLE_AMOUNT = "com.codeonyx.bcvaqui.widget.ACTION_CYCLE_AMOUNT"

        fun updateAllWidgets(context: Context, snapshot: WidgetSnapshot) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ConverterWidgetProvider::class.java))
            ids.forEach { updateWidget(context, manager, it, snapshot) }
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            snap: WidgetSnapshot
        ) {
            val amount = snap.prefs.widgetConvertAmount
            val rate = snap.rate
            val ves = if (rate != null) FxCalculator.round2(amount * rate) else null

            val views = RemoteViews(context.packageName, R.layout.widget_converter)
            Widgets.applyBrandAndAlert(views, R.id.widget_converter_root, snap)
            views.setTextViewText(
                R.id.widget_converter_from,
                "${FxCalculator.formatAmount(amount)} ${snap.currency}"
            )
            views.setTextViewText(
                R.id.widget_converter_to,
                ves?.let { "${FxCalculator.formatAmount(it)} Bs." } ?: "—"
            )
            views.setTextViewText(
                R.id.widget_converter_rate,
                rate?.let {
                    "${RateSources.rateSourceLabel(snap.currency)} ${FxCalculator.format2(it)}"
                } ?: "Sin tasa"
            )
            views.setOnClickPendingIntent(
                R.id.widget_converter_root,
                Widgets.openAppPending(context, 4000 + appWidgetId)
            )

            val cycleIntent = Intent(context, ConverterWidgetProvider::class.java).apply {
                action = ACTION_CYCLE_AMOUNT
            }
            val cyclePending = PendingIntent.getBroadcast(
                context,
                4100 + appWidgetId,
                cycleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_converter_cycle, cyclePending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
