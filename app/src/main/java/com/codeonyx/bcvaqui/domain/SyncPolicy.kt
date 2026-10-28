package com.codeonyx.bcvaqui.domain

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Reglas puras de sincronización en segundo plano (testeables sin Android). */
object SyncPolicy {
    val CARACAS: ZoneId = ZoneId.of("America/Caracas")

    /** El BCV suele publicar la tasa del día hábil siguiente entre 15:00 y 18:00 (hora de Caracas). */
    val BCV_PUBLISH_CHECK: LocalTime = LocalTime.of(17, 30)

    const val WIDGET_INTERVAL_MINUTES = 60L

    /**
     * Intervalo del sync periódico único, o null si no hace falta ninguno
     * (sin widgets y sin notificaciones).
     */
    fun periodicIntervalMinutes(hasWidgets: Boolean, notificationHours: Int): Long? {
        val notif = if (notificationHours > 0) notificationHours * 60L else null
        val widget = if (hasWidgets) WIDGET_INTERVAL_MINUTES else null
        return listOfNotNull(notif, widget).minOrNull()
    }

    /** Milisegundos hasta el próximo chequeo de publicación BCV (lunes a viernes 17:30 Caracas). */
    fun delayToNextBcvPublishMs(now: ZonedDateTime): Long {
        val local = now.withZoneSameInstant(CARACAS)
        var target = local.with(BCV_PUBLISH_CHECK).withSecond(0).withNano(0)
        if (!target.isAfter(local)) target = target.plusDays(1)
        while (target.dayOfWeek == DayOfWeek.SATURDAY || target.dayOfWeek == DayOfWeek.SUNDAY) {
            target = target.plusDays(1)
        }
        return Duration.between(local, target).toMillis()
    }
}
