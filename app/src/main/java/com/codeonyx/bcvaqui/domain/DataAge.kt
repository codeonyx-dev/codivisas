package com.codeonyx.bcvaqui.domain

/** Texto corto de antigüedad de los datos: "hace 5 min", "hace 3 h", "hace 2 días". */
object DataAge {
    fun describe(epochMs: Long, nowMs: Long = System.currentTimeMillis()): String? {
        if (epochMs <= 0L) return null
        val minutes = ((nowMs - epochMs).coerceAtLeast(0L)) / 60_000L
        return when {
            minutes < 1 -> "hace instantes"
            minutes < 60 -> "hace $minutes min"
            minutes < 60 * 24 -> "hace ${minutes / 60} h"
            minutes < 60 * 48 -> "hace 1 día"
            else -> "hace ${minutes / (60 * 24)} días"
        }
    }

    /** true si los datos tienen más de [hours] horas. */
    fun isStale(epochMs: Long, hours: Long = 12, nowMs: Long = System.currentTimeMillis()): Boolean =
        epochMs > 0L && nowMs - epochMs > hours * 3_600_000L
}
