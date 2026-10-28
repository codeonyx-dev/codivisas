package com.codeonyx.bcvaqui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.codeonyx.bcvaqui.R
import com.codeonyx.bcvaqui.domain.FxCalculator
import com.codeonyx.bcvaqui.worker.WorkScheduler

class SpreadWidgetProvider : AppWidgetProvider() {
    override fun onEnabled(context: Context) {
        WorkScheduler.onWidgetAdded(context)
    }

    override fun onDisabled(context: Context) {
        WorkScheduler.onWidgetRemoved(context)
    }

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

    companion object {
        fun updateAllWidgets(context: Context, snapshot: WidgetSnapshot) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, SpreadWidgetProvider::class.java))
            ids.forEach { updateWidget(context, manager, it, snapshot) }
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            snap: WidgetSnapshot
        ) {
            val prefs = snap.prefs
            val bcv = prefs.lastKnownUsdRate
                ?: snap.rate.takeIf { snap.currency == "USD" }
            val p2p = prefs.lastKnownBinanceUsdtVes

            val spreadPct = if (bcv != null && p2p != null && bcv > 0) {
                FxCalculator.round2((p2p - bcv) / bcv * 100.0)
            } else null

            val views = RemoteViews(context.packageName, R.layout.widget_spread)
            Widgets.applyBrandAndAlert(views, R.id.widget_spread_root, snap)
            views.setTextViewText(
                R.id.widget_spread_bcv,
                bcv?.let { FxCalculator.format2(it) } ?: "—"
            )
            views.setTextViewText(
                R.id.widget_spread_p2p,
                p2p?.let { FxCalculator.format2(it) } ?: "—"
            )
            views.setTextViewText(
                R.id.widget_spread_pct,
                spreadPct?.let {
                    val sign = if (it > 0) "+" else ""
                    "$sign${FxCalculator.format2(it)}%"
                } ?: "—"
            )

            val footer = Widgets.offlineMarker(snap) ?: when {
                p2p == null -> "P2P no disponible · abre la app para actualizar"
                spreadPct != null -> "P2P USDT/VES · Binance"
                else -> "USD · referencia de mercado"
            }
            views.setTextViewText(R.id.widget_spread_delta, footer)

            if (spreadPct != null) {
                val color = when {
                    spreadPct >= 0 -> context.getColor(R.color.positive_green)
                    else -> context.getColor(R.color.negative_red)
                }
                views.setTextColor(R.id.widget_spread_pct, color)
            } else {
                views.setTextColor(R.id.widget_spread_pct, context.getColor(R.color.widget_text))
            }

            views.setOnClickPendingIntent(
                R.id.widget_spread_root,
                Widgets.openAppPending(context, 3000 + appWidgetId)
            )
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
