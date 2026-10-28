package com.codeonyx.bcvaqui.domain

import kotlin.math.abs

/**
 * Decide si un cambio de tasa merece notificación.
 *  - BCV: la tasa oficial cambia una vez por publicación; se notifica cuando
 *    realmente cambia y supera el umbral del usuario (0 = cualquier cambio real).
 *  - USDT (Binance P2P): es volátil; se exige siempre un umbral mínimo para
 *    no notificar ruido minuto a minuto.
 */
object RateAlertPolicy {
    /** Umbral mínimo para USDT aunque el usuario tenga 0 %. */
    const val MIN_USDT_THRESHOLD_PERCENT = 1.0

    fun changePercent(oldRate: Double, newRate: Double): Double {
        if (oldRate <= 0.0) return 0.0
        return (newRate - oldRate) / oldRate * 100.0
    }

    fun shouldNotify(
        currency: String,
        oldRate: Double,
        newRate: Double,
        thresholdPercent: Double
    ): Boolean {
        if (oldRate <= 0.0 || newRate <= 0.0) return false
        // Ignora diferencias de redondeo (< 0,01 Bs.).
        if (abs(FxCalculator.round2(newRate) - FxCalculator.round2(oldRate)) < 0.01) return false
        val change = abs(changePercent(oldRate, newRate))
        val threshold = if (currency == RateSources.BINANCE_USDT) {
            maxOf(thresholdPercent, MIN_USDT_THRESHOLD_PERCENT)
        } else {
            thresholdPercent.coerceAtLeast(0.0)
        }
        return change >= threshold
    }
}
