package com.codeonyx.bcvaqui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.os.Build
import android.util.SizeF
import com.codeonyx.bcvaqui.domain.MarketMath
import com.codeonyx.bcvaqui.R
import com.codeonyx.bcvaqui.domain.FxCalculator
import com.codeonyx.bcvaqui.worker.WorkScheduler

class RateWidgetProvider : AppWidgetProvider() {

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
            appWidgetIds.forEach { id ->
                updateWidget(context, appWidgetManager, id, snapshot)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            WorkScheduler.enqueueImmediateRefresh(context)
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

        private fun buildViews(context: Context, layout: Int, snap: WidgetSnapshot, appWidgetId: Int): RemoteViews {
            val views = RemoteViews(context.packageName, layout)
            Widgets.applyBrandAndAlert(views, R.id.widget_root, snap)
            val small = layout == R.layout.widget_rate_small
            views.setTextViewText(
                R.id.widget_currency_label,
                if (small) snap.currency else "${FxCalculator.currencyLabel(snap.currency)} · ${snap.currency}"
            )
            views.setTextViewText(R.id.widget_rate_text, snap.rate?.let { FxCalculator.format2(it) } ?: "—")
            val pct = snap.dayChangePct
            val trend = "${MarketMath.trendArrow(pct)} " + (pct?.let {
                (if (it > 0) "+" else "") + FxCalculator.format2(it) + "%"
            } ?: "sin cambio") + if (small) "" else " hoy"
            views.setTextViewText(R.id.widget_trend_text, trend)
            if (!small) {
                views.setTextViewText(
                    R.id.widget_rate_suffix,
                    if (snap.rate != null) "Bs. por 1 ${snap.currency}" else "Sin datos aún"
                )
                views.setTextViewText(
                    R.id.widget_updated_text,
                    Widgets.offlineMarker(snap) ?: "Actualizado: ${FxCalculator.formatUpdatedAt(snap.prefs.lastKnownUpdatedAt)}"
                )
                val refreshIntent = Intent(context, RateWidgetProvider::class.java).apply { action = ACTION_REFRESH }
                views.setOnClickPendingIntent(
                    R.id.widget_refresh,
                    PendingIntent.getBroadcast(
                        context, appWidgetId + 1000, refreshIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }
            if (layout == R.layout.widget_rate_large) {
                val usdt = snap.prefs.lastKnownBinanceUsdtVes
                val usd = snap.prefs.lastKnownUsdRate
                views.setTextViewText(R.id.widget_usdt_text, "USDT ${usdt?.let { FxCalculator.format2(it) } ?: "—"}")
                val gap = if (usdt != null && usd != null && usd > 0) FxCalculator.round2((usdt - usd) / usd * 100) else null
                views.setTextViewText(R.id.widget_gap_text, "Brecha ${gap?.let { FxCalculator.format2(it) + "%" } ?: "—"}")
            }
            views.setOnClickPendingIntent(R.id.widget_root, Widgets.openAppPending(context, appWidgetId))
            return views
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            snap: WidgetSnapshot
        ) {
            val views = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                RemoteViews(
                    mapOf(
                        SizeF(60f, 60f) to buildViews(context, R.layout.widget_rate_small, snap, appWidgetId),
                        SizeF(200f, 110f) to buildViews(context, R.layout.widget_rate, snap, appWidgetId),
                        SizeF(250f, 200f) to buildViews(context, R.layout.widget_rate_large, snap, appWidgetId)
                    )
                )
            } else {
                val opts = appWidgetManager.getAppWidgetOptions(appWidgetId)
                val w = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                val h = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
                val layout = when {
                    w in 1 until 200 -> R.layout.widget_rate_small
                    h >= 200 -> R.layout.widget_rate_large
                    else -> R.layout.widget_rate
                }
                buildViews(context, layout, snap, appWidgetId)
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        val pendingResult = goAsync()
        Widgets.runAsync(pendingResult) {
            updateWidget(context, appWidgetManager, appWidgetId, Widgets.loadSnapshot(context))
        }
    }
}
