package com.codeonyx.bcvaqui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.codeonyx.bcvaqui.R
import com.codeonyx.bcvaqui.domain.FxCalculator

class VariationWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, VariationWidgetProvider::class.java))
            ids.forEach { updateWidget(context, manager, it) }
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val snap = Widgets.loadSnapshot(context)
            val views = RemoteViews(context.packageName, R.layout.widget_variation)
            Widgets.applyBrandAndAlert(views, R.id.widget_variation_root, snap)
            views.setTextViewText(
                R.id.widget_variation_currency,
                "${FxCalculator.currencyLabel(snap.currency)} · ${snap.currency}"
            )
            views.setTextViewText(
                R.id.widget_variation_rate,
                snap.rate?.let { FxCalculator.format2(it) } ?: "—"
            )

            val pct = snap.dayChangePct
            val pctText = when {
                pct == null -> "Sin dato de ayer aún"
                else -> {
                    val sign = if (pct > 0) "+" else ""
                    "$sign${FxCalculator.format2(pct)}% vs ayer"
                }
            }
            views.setTextViewText(R.id.widget_variation_pct, pctText)
            val color = when {
                pct == null -> context.getColor(R.color.widget_text_muted)
                pct >= 0 -> context.getColor(R.color.positive_green)
                else -> context.getColor(R.color.negative_red)
            }
            views.setTextColor(R.id.widget_variation_pct, color)

            views.setOnClickPendingIntent(
                R.id.widget_variation_root,
                Widgets.openAppPending(context, 5000 + appWidgetId)
            )
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
