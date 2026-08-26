package com.codeonyx.bcvaqui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.codeonyx.bcvaqui.R
import com.codeonyx.bcvaqui.domain.FxCalculator
import com.codeonyx.bcvaqui.domain.RateSources

class ShareRateWidgetProvider : AppWidgetProvider() {
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
            val ids = manager.getAppWidgetIds(ComponentName(context, ShareRateWidgetProvider::class.java))
            ids.forEach { updateWidget(context, manager, it) }
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val snap = Widgets.loadSnapshot(context)
            val views = RemoteViews(context.packageName, R.layout.widget_share)
            Widgets.applyBrandAndAlert(views, R.id.widget_share_root, snap)
            views.setTextViewText(
                R.id.widget_share_rate,
                snap.rate?.let { FxCalculator.format2(it) } ?: "—"
            )
            views.setTextViewText(
                R.id.widget_share_label,
                "${snap.currency} · ${RateSources.rateSourceLabel(snap.currency)}"
            )

            val shareIntent = Intent(context, ShareRateActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val sharePending = PendingIntent.getActivity(
                context,
                6000 + appWidgetId,
                shareIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_share_button, sharePending)
            views.setOnClickPendingIntent(
                R.id.widget_share_root,
                Widgets.openAppPending(context, 6100 + appWidgetId)
            )
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
