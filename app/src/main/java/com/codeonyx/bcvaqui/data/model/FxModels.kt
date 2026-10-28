package com.codeonyx.bcvaqui.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BcvRateResponse(
    val USD: Double? = null,
    val EUR: Double? = null,
    val CNY: Double? = null,
    val TRY: Double? = null,
    val RUB: Double? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("effective_date") val effectiveDate: String? = null,
    val date: String? = null,
    val source: String? = null
)

@Serializable
data class DolarApiQuote(
    val moneda: String? = null,
    val fuente: String? = null,
    val promedio: Double? = null,
    val fechaActualizacion: String? = null
)

@Serializable
data class DolarApiHistoryEntry(
    val fuente: String? = null,
    val promedio: Double? = null,
    val fecha: String? = null
)

/** Entrada del histórico diario BCV (misma forma que rate.json). */
typealias BcvHistoryEntry = BcvRateResponse

@Serializable
data class WorldRatesResponse(
    val result: String? = null,
    @SerialName("base_code") val baseCode: String? = null,
    val rates: Map<String, Double>? = null,
    @SerialName("time_last_update_utc") val updatedAt: String? = null
)

@Serializable
data class CurrencyRate(
    val code: String,
    val rateToVes: Double,
    val updatedAt: String? = null
)

data class FxDashboard(
    val bcvRates: Map<String, CurrencyRate>,
    val bcvUpdatedAt: String?,
    val worldRatesUsd: Map<String, Double> = emptyMap(),
    val worldRatesUpdatedAt: String? = null,
    /** Mediana aproximada USDT/VES en Binance P2P (compra). */
    val binanceUsdtVes: Double? = null,
    val binanceError: String? = null,
    val isFromCache: Boolean,
    val errorMessage: String? = null,
    /** Tasa BCV USD del próximo día hábil, si ya fue publicada. */
    val nextBcvUsd: com.codeonyx.bcvaqui.domain.NextRate? = null,
    /** USDT/VES compra, venta y promedio (Binance P2P). */
    val usdtQuote: com.codeonyx.bcvaqui.domain.UsdtQuote? = null,
    /** Momento (epoch ms) de la última carga exitosa mostrada. */
    val loadedAtMs: Long? = null
)

@Serializable
data class BinanceSearchRequest(
    val asset: String = "USDT",
    val fiat: String = "VES",
    val tradeType: String = "BUY",
    val page: Int = 1,
    val rows: Int = 10,
    val payTypes: List<String> = emptyList()
)

@Serializable
data class BinanceP2pResponse(
    val code: String? = null,
    val message: String? = null,
    val data: List<BinanceAdvRow>? = null
)

@Serializable
data class BinanceAdvRow(
    val adv: BinanceAdv? = null
)

@Serializable
data class BinanceAdv(
    val price: String? = null
)

/** Destinos / monedas disponibles en la pestaña "Dónde". */
data class WhereDestination(
    val code: String,
    val country: String,
    val adjective: String,
    val flag: String,
    /** Etiqueta corta en chips (ej. Bs, COP). */
    val chipLabel: String = code
) {
    companion object {
        val ALL = listOf(
            WhereDestination("VES", "Venezuela", "bolívares", "🇻🇪", "Bs"),
            WhereDestination("USD", "Estados Unidos", "dólares", "🇺🇸", "USD"),
            WhereDestination("COP", "Colombia", "pesos colombianos", "🇨🇴", "COP"),
            WhereDestination("MXN", "México", "pesos mexicanos", "🇲🇽", "MXN"),
            WhereDestination("BRL", "Brasil", "reales", "🇧🇷", "BRL"),
            WhereDestination("ARS", "Argentina", "pesos argentinos", "🇦🇷", "ARS"),
            WhereDestination("PEN", "Perú", "soles", "🇵🇪", "PEN"),
            WhereDestination("CLP", "Chile", "pesos chilenos", "🇨🇱", "CLP"),
            WhereDestination("DOP", "Rep. Dominicana", "pesos dominicanos", "🇩🇴", "DOP"),
            WhereDestination("EUR", "Europa", "euros", "🇪🇺", "EUR"),
            WhereDestination("GBP", "Reino Unido", "libras", "🇬🇧", "GBP"),
            WhereDestination("CAD", "Canadá", "dólares canadienses", "🇨🇦", "CAD"),
            WhereDestination("CNY", "China", "yuanes", "🇨🇳", "CNY"),
            WhereDestination("TRY", "Turquía", "liras", "🇹🇷", "TRY"),
            WhereDestination("RUB", "Rusia", "rublos", "🇷🇺", "RUB")
        )

        val DEFAULT_CODES = listOf("VES", "USD", "COP", "MXN")

        fun byCode(code: String): WhereDestination? = ALL.find { it.code == code }
    }
}

