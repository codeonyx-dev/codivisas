package com.codeonyx.bcvaqui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.codeonyx.bcvaqui.R
import com.codeonyx.bcvaqui.domain.FxCalculator
import com.codeonyx.bcvaqui.worker.WorkScheduler

class MiniRateWidgetProvider : AppWidgetProvider() {
    override fun onEnabled(context: Context) {
        WorkScheduler.onWidgetAdded(context)
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
            val ids = manager.getAppWidgetIds(ComponentName(context, MiniRateWidgetProvider::class.java))
            ids.forEach { updateWidget(context, manager, it, snapshot) }
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            snap: WidgetSnapshot
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_mini_rate)
            Widgets.applyBrandAndAlert(views, R.id.widget_mini_root, snap)
            views.setTextViewText(R.id.widget_mini_code, snap.currency)
            views.setTextViewText(
                R.id.widget_mini_rate,
                snap.rate?.let { FxCalculator.format2(it) } ?: "—"
            )
            views.setOnClickPendingIntent(
                R.id.widget_mini_root,
                Widgets.openAppPending(context, 2000 + appWidgetId)
            )
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
