package com.codeonyx.bcvaqui.domain

import kotlin.math.abs

/** Fuentes que pueden notificar, cada una con su propia línea base. */
enum class NotifySource(val key: String, val title: String, val currency: String) {
    BCV_USD("BCV_USD", "Dólar BCV", "USD"),
    BCV_EUR("BCV_EUR", "Euro BCV", "EUR"),
    USDT("USDT", "USDT Binance", RateSources.BINANCE_USDT)
}

data class RateNotification(
    val source: NotifySource,
    val oldRate: Double,
    val newRate: Double,
    val changePercent: Double
) {
    val up: Boolean get() = newRate > oldRate
    /** "Dólar BCV subió 0,85 %" */
    val title: String get() = "${source.title} ${if (up) "subió" else "bajó"} ${FxCalculator.format2(abs(changePercent))} %"
    /** "888,12 Bs. · antes 880,64 Bs." */
    val body: String get() = "${FxCalculator.format2(newRate)} Bs. · antes ${FxCalculator.format2(oldRate)} Bs."
}

data class NotifyDecision(
    val notifications: List<RateNotification>,
    /** Nuevas líneas base por fuente (se persisten siempre). */
    val baselines: Map<String, Double>
)

/**
 * Detección de cambios por fuente: cada fuente se compara SOLO contra su propio
 * último valor. Cambiar la moneda seleccionada no afecta nada aquí.
 */
object RateNotifier {
    fun isQuietHour(hour: Int, enabled: Boolean, start: Int, end: Int): Boolean {
        if (!enabled || start == end) return false
        return if (start < end) hour in start until end else hour >= start || hour < end
    }

    fun decide(
        current: Map<NotifySource, Double?>,
        baselines: Map<String, Double>,
        enabled: Set<NotifySource>,
        thresholdPercent: Double,
        quiet: Boolean
    ): NotifyDecision {
        val next = baselines.toMutableMap()
        val out = mutableListOf<RateNotification>()
        for ((source, rate) in current) {
            if (rate == null || rate <= 0.0) continue
            val old = baselines[source.key]
            if (old == null || source !in enabled) {
                // Primera vez o fuente apagada: solo fija la base, sin avisar.
                next[source.key] = rate
                continue
            }
            if (!RateAlertPolicy.shouldNotify(source.currency, old, rate, thresholdPercent)) continue
            if (quiet) continue // se mantiene la base: avisará al terminar las horas de silencio
            out += RateNotification(source, old, rate, RateAlertPolicy.changePercent(old, rate))
            next[source.key] = rate
        }
        return NotifyDecision(out, next)
    }
}
