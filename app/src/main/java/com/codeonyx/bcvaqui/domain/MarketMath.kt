package com.codeonyx.bcvaqui.domain

import kotlin.math.abs

/** Punto diario de una serie (fecha ISO yyyy-MM-dd). */
data class DayPoint(val date: String, val value: Double)

/** Tasa BCV ya publicada para un día hábil posterior a hoy. */
data class NextRate(val date: String, val rate: Double)

data class UsdtQuote(val buy: Double?, val sell: Double?, val atEpochMs: Long) {
    val average: Double?
        get() = when {
            buy != null && sell != null -> FxCalculator.round2((buy + sell) / 2.0)
            else -> buy ?: sell
        }
}

enum class Trend { UP, DOWN, FLAT }

object MarketMath {
    /** Primera tasa con fecha posterior a [today] (el BCV publica la del próximo día hábil por la tarde). */
    fun nextRate(history: List<DayPoint>, today: String): NextRate? =
        history.filter { it.date > today && it.value > 0 }
            .minByOrNull { it.date }
            ?.let { NextRate(it.date, it.value) }

    /** Últimos [days] puntos hasta [today] inclusive, ordenados por fecha. */
    fun window(points: List<DayPoint>, days: Int, today: String? = null): List<DayPoint> =
        points.asSequence()
            .filter { it.value > 0 && (today == null || it.date <= today) }
            .distinctBy { it.date }
            .sortedBy { it.date }
            .toList()
            .takeLast(days)

    /** Brecha % entre paralelo/USDT y BCV para las fechas que coinciden. */
    fun gapSeries(bcv: List<DayPoint>, market: List<DayPoint>): List<DayPoint> {
        val byDate = bcv.associate { it.date to it.value }
        return market.mapNotNull { m ->
            val o = byDate[m.date] ?: return@mapNotNull null
            if (o <= 0) null else DayPoint(m.date, FxCalculator.round2((m.value - o) / o * 100.0))
        }.sortedBy { it.date }
    }

    /** Combina dos fuentes por fecha; [primary] gana si ambas tienen el día. */
    fun merge(primary: List<DayPoint>, secondary: List<DayPoint>): List<DayPoint> =
        (secondary.associateBy { it.date } + primary.associateBy { it.date }).values.sortedBy { it.date }

    fun changePercent(points: List<DayPoint>): Double? {
        if (points.size < 2) return null
        val first = points.first().value
        val last = points.last().value
        return if (first <= 0) null else FxCalculator.round2((last - first) / first * 100.0)
    }

    fun trend(changePct: Double?, epsilon: Double = 0.01): Trend = when {
        changePct == null || abs(changePct) < epsilon -> Trend.FLAT
        changePct > 0 -> Trend.UP
        else -> Trend.DOWN
    }

    fun trendArrow(changePct: Double?): String = when (trend(changePct)) {
        Trend.UP -> "▲"
        Trend.DOWN -> "▼"
        Trend.FLAT -> "■"
    }

    /** Modo comerciante: aplica ganancia (+) o descuento (−) en % a un monto. */
    fun applyMargin(amount: Double, marginPercent: Double): Double =
        FxCalculator.round2(amount * (1.0 + marginPercent / 100.0))

    /** Tasa efectiva con margen, útil para cotizar en Bs. */
    fun marginRate(rate: Double, marginPercent: Double): Double =
        FxCalculator.round2(rate * (1.0 + marginPercent / 100.0))

    /** "2026-10-28" -> "28/10/2026". */
    fun shortDate(iso: String): String {
        val p = iso.take(10).split("-")
        return if (p.size == 3) "${p[2]}/${p[1]}/${p[0]}" else iso
    }
}
