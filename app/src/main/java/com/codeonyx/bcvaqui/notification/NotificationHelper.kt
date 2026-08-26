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

    private fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String
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
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
