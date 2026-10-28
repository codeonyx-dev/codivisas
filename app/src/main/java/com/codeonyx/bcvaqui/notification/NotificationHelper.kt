package com.codeonyx.bcvaqui.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.codeonyx.bcvaqui.BcvAquiApp
import com.codeonyx.bcvaqui.MainActivity
import com.codeonyx.bcvaqui.R
import com.codeonyx.bcvaqui.domain.FxCalculator

object NotificationHelper {
    private const val RATE_NOTIFICATION_ID = 1001

    fun showRateChangeNotification(
        context: Context,
        currency: String,
        oldRate: Double,
        newRate: Double
    ) {
        val increased = newRate > oldRate
        val title = if (increased) "${FxCalculator.currencyLabel(currency)} subió" else "${FxCalculator.currencyLabel(currency)} bajó"
        val body = context.getString(
            if (increased) R.string.notification_rate_up else R.string.notification_rate_down,
            FxCalculator.currencyLabel(currency),
            FxCalculator.format2(newRate),
            FxCalculator.format2(oldRate)
        )
        showNotification(context, RATE_NOTIFICATION_ID, title, body)
    }

    private const val DEBT_NOTIFICATION_ID = 1002

    /** Recordatorio de deudas próximas o vencidas (mismo canal de notificaciones). */
    fun showDebtReminder(context: Context, title: String, body: String) {
        runCatching { showNotification(context, DEBT_NOTIFICATION_ID, title, body) }
    }

    private const val GROUP_RATES = "spreadve_rates"
    private const val SUMMARY_ID = 1000

    /** Una notificación por fuente (id estable = reemplaza, no duplica) y un resumen agrupado. */
    fun showRateNotifications(context: Context, items: List<com.codeonyx.bcvaqui.domain.RateNotification>) {
        if (items.isEmpty()) return
        runCatching {
            items.forEach { n ->
                showNotification(context, RATE_NOTIFICATION_ID + 10 + n.source.ordinal, n.title, n.body, GROUP_RATES)
            }
            if (items.size > 1) {
                val inbox = NotificationCompat.InboxStyle().also { st -> items.forEach { st.addLine(it.title) } }
                val summary = NotificationCompat.Builder(context, BcvAquiApp.CHANNEL_RATE_CHANGES)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("Cambios de tasa")
                    .setContentText(items.joinToString(" · ") { it.source.title })
                    .setStyle(inbox)
                    .setGroup(GROUP_RATES)
                    .setGroupSummary(true)
                    .setContentIntent(openApp(context, SUMMARY_ID))
                    .setAutoCancel(true)
                    .build()
                NotificationManagerCompat.from(context).notify(SUMMARY_ID, summary)
            }
        }
    }

    private fun openApp(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String,
        group: String? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, BcvAquiApp.CHANNEL_RATE_CHANGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .apply { group?.let { setGroup(it) } }
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
