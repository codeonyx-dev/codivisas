package com.codeonyx.bcvaqui.domain

import com.codeonyx.bcvaqui.data.model.CurrencyRate

object RateSources {
    const val BINANCE_USDT = "USDT"

    fun effectiveRate(
        currency: String,
        bcvRates: Map<String, CurrencyRate>,
        binanceUsdtVes: Double?
    ): Double? = when (currency) {
        BINANCE_USDT -> binanceUsdtVes?.takeIf { it > 0 }
        else -> bcvRates[currency]?.rateToVes
    }

    fun rateSourceLabel(currency: String): String = when (currency) {
        BINANCE_USDT -> "Binance P2P"
        else -> "Tasa BCV"
    }
}
