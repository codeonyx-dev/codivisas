package com.codeonyx.bcvaqui.domain

import com.codeonyx.bcvaqui.data.model.CurrencyRate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Última carga exitosa de todas las fuentes (BCV, tasas mundiales, USDT P2P) con su hora de carga. */
@Serializable
data class RatesSnapshot(
    val bcvRates: List<CurrencyRate> = emptyList(),
    val bcvUpdatedAt: String? = null,
    val worldRatesUsd: Map<String, Double> = emptyMap(),
    val worldUpdatedAt: String? = null,
    val usdtBuy: Double? = null,
    val usdtSell: Double? = null,
    val loadedAtMs: Long = 0L
)

/** Lógica pura del modo sin conexión (sin Android, probada en unit tests). */
object OfflineRates {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    val CARACAS: TimeZone = TimeZone.getTimeZone("America/Caracas")

    fun encode(s: RatesSnapshot): String = json.encodeToString(RatesSnapshot.serializer(), s)

    fun decode(raw: String?): RatesSnapshot? {
        if (raw.isNullOrBlank()) return null
        return runCatching { json.decodeFromString(RatesSnapshot.serializer(), raw) }.getOrNull()
    }

    /**
     * Combina una carga nueva con la anterior: si una fuente falló (vacía/null) se conserva
     * su último valor bueno, así nunca se pierde una tasa ya cargada.
     */
    fun merge(previous: RatesSnapshot?, fresh: RatesSnapshot): RatesSnapshot {
        if (previous == null) return fresh
        return fresh.copy(
            bcvRates = fresh.bcvRates.ifEmpty { previous.bcvRates },
            bcvUpdatedAt = if (fresh.bcvRates.isEmpty()) previous.bcvUpdatedAt else fresh.bcvUpdatedAt ?: previous.bcvUpdatedAt,
            worldRatesUsd = fresh.worldRatesUsd.ifEmpty { previous.worldRatesUsd },
            worldUpdatedAt = if (fresh.worldRatesUsd.isEmpty()) previous.worldUpdatedAt else fresh.worldUpdatedAt,
            usdtBuy = fresh.usdtBuy ?: previous.usdtBuy,
            usdtSell = fresh.usdtSell ?: previous.usdtSell
        )
    }

    /** dd/MM/yyyy hh:mm a en hora de Caracas. */
    fun formatLoadedAt(epochMs: Long?): String {
        if (epochMs == null || epochMs <= 0L) return "—"
        return SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.US).apply { timeZone = CARACAS }.format(Date(epochMs))
    }

    const val BANNER_TITLE = "Modo sin conexión"

    fun bannerText(epochMs: Long?): String = "$BANNER_TITLE · Última tasa cargada: ${formatLoadedAt(epochMs)}"

    fun lastLoadedLine(epochMs: Long?): String = "Última tasa cargada: ${formatLoadedAt(epochMs)}"

    /** Marca corta para widgets. */
    fun widgetMarker(epochMs: Long?): String = "⚠ Sin conexión · ${formatLoadedAt(epochMs)}"

    /** Sin conexión = la última actualización falló pero hay datos guardados que mostrar. */
    fun isOffline(lastRefreshFailed: Boolean, snapshot: RatesSnapshot?): Boolean =
        lastRefreshFailed && snapshot != null && snapshot.loadedAtMs > 0L

    fun ratesMap(s: RatesSnapshot?): Map<String, CurrencyRate> = s?.bcvRates.orEmpty().associateBy { it.code }
}
