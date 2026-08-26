package com.codeonyx.bcvaqui.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object FxCalculator {
    private val twoDecimalFormat = DecimalFormat("0.00", DecimalFormatSymbols(Locale.US))

    fun round2(value: Double): Double =
        BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toDouble()

    fun round4(value: Double): Double =
        BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).toDouble()

    fun format2(value: Double): String = twoDecimalFormat.format(round2(value))

    fun formatAmount(value: Double): String {
        val symbols = DecimalFormatSymbols(Locale.US)
        return if (value >= 1000) {
            DecimalFormat("#,##0.00", symbols).format(round2(value))
        } else {
            format2(value)
        }
    }

    fun convert(
        amount: Double,
        fromRateToVes: Double,
        toRateToVes: Double
    ): Double {
        if (fromRateToVes <= 0.0 || toRateToVes <= 0.0) return 0.0
        return round2(amount * fromRateToVes / toRateToVes)
    }

    /** Convierte un monto a USD usando tasas BCV y/o internacionales. */
    fun toUsd(
        amount: Double,
        currency: String,
        bcvRates: Map<String, com.codeonyx.bcvaqui.data.model.CurrencyRate>,
        worldRatesUsd: Map<String, Double> = emptyMap()
    ): Double {
        if (amount <= 0.0) return 0.0
        if (currency == "USD") return round2(amount)
        if (currency == "VES") {
            val usd = bcvRates["USD"]?.rateToVes ?: return 0.0
            if (usd <= 0.0) return 0.0
            return round2(amount / usd)
        }
        bcvRates[currency]?.rateToVes?.let { from ->
            val usd = bcvRates["USD"]?.rateToVes ?: return 0.0
            if (usd <= 0.0) return 0.0
            return round2(amount * from / usd)
        }
        worldRatesUsd[currency]?.takeIf { it > 0 }?.let { usdToCurrency ->
            return round2(amount / usdToCurrency)
        }
        return 0.0
    }

    /** Convierte USD a otra moneda. */
    fun fromUsd(
        amountUsd: Double,
        currency: String,
        bcvRates: Map<String, com.codeonyx.bcvaqui.data.model.CurrencyRate>,
        worldRatesUsd: Map<String, Double> = emptyMap()
    ): Double {
        if (amountUsd <= 0.0) return 0.0
        if (currency == "USD") return round2(amountUsd)
        if (currency == "VES") {
            val usd = bcvRates["USD"]?.rateToVes ?: return 0.0
            return round2(amountUsd * usd)
        }
        bcvRates[currency]?.rateToVes?.let { toRate ->
            val usd = bcvRates["USD"]?.rateToVes ?: return 0.0
            if (usd <= 0.0 || toRate <= 0.0) return 0.0
            return round2(amountUsd * usd / toRate)
        }
        worldRatesUsd[currency]?.takeIf { it > 0 }?.let { usdToCurrency ->
            return round2(amountUsd * usdToCurrency)
        }
        return 0.0
    }

    fun formatRateLine(currency: String, rate: Double): String =
        "1 $currency = ${format2(rate)} VES"

    fun currencyLabel(code: String): String = when (code) {
        "USD" -> "Dólar"
        "EUR" -> "Euro"
        "CNY" -> "Yuan chino"
        "TRY" -> "Lira turca"
        "RUB" -> "Rublo"
        "VES" -> "Bolívar"
        "COP" -> "Peso colombiano"
        "MXN" -> "Peso mexicano"
        "BRL" -> "Real"
        "ARS" -> "Peso argentino"
        "PEN" -> "Sol"
        "CLP" -> "Peso chileno"
        "DOP" -> "Peso dominicano"
        "GBP" -> "Libra"
        "CAD" -> "Dólar canadiense"
        else -> code
    }

    fun currencySymbol(code: String): String = when (code) {
        "USD" -> "$"
        "EUR" -> "€"
        "CNY" -> "¥"
        "TRY" -> "₺"
        "RUB" -> "₽"
        "VES" -> "Bs."
        "COP" -> "$"
        "MXN" -> "$"
        "BRL" -> "R$"
        "ARS" -> "$"
        "PEN" -> "S/"
        "CLP" -> "$"
        "DOP" -> "RD$"
        "GBP" -> "£"
        "CAD" -> "C$"
        else -> code
    }

    fun formatUpdatedAt(isoOrDate: String?): String {
        if (isoOrDate.isNullOrBlank()) return "—"
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd"
        )
        for (pattern in patterns) {
            runCatching {
                val parser = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = parser.parse(isoOrDate) ?: return@runCatching
                val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                return formatter.format(date)
            }
        }
        return isoOrDate
    }
}
