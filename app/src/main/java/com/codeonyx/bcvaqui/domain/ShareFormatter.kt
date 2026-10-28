package com.codeonyx.bcvaqui.domain

import com.codeonyx.bcvaqui.data.model.ColombianBanks
import com.codeonyx.bcvaqui.data.model.PaymentContact
import com.codeonyx.bcvaqui.data.model.PaymentType

/** Datos de una conversión a compartir. */
data class ShareConversion(
    /** Nombre visible de la tasa: BCV, Euro, Usdt o una etiqueta propia. */
    val rateLabel: String,
    val currencyCode: String,
    val foreignAmount: Double,
    val vesAmount: Double,
    /** Monto equivalente en USD (para Zelle, PayPal, Binance…). null = usar foreignAmount si es USD/USDT. */
    val usdAmount: Double? = null
)

/**
 * Desglose detallado para compartir:
 *   -Tasa: Usdt
 *   -Monto en Dolares: 43,00
 *   -Monto Bs: 41.470,06
 *
 *   -Pago Movil:
 *   -Tlf: ...
 */
object ShareFormatter {

    fun rateLabelFor(currencyCode: String, customLabel: String? = null): String = when {
        !customLabel.isNullOrBlank() -> customLabel.trim()
        currencyCode == "USD" -> "BCV"
        currencyCode == "EUR" -> "Euro"
        currencyCode == RateSources.BINANCE_USDT -> "Usdt"
        else -> currencyCode
    }

    private fun amountLabel(currencyCode: String): String = when (currencyCode) {
        "EUR" -> "Monto en Euros"
        "USD", RateSources.BINANCE_USDT -> "Monto en Dolares"
        else -> "Monto en $currencyCode"
    }

    fun usdFor(conv: ShareConversion): Double? = conv.usdAmount
        ?: conv.foreignAmount.takeIf { conv.currencyCode == "USD" || conv.currencyCode == RateSources.BINANCE_USDT }

    fun build(conv: ShareConversion, methods: List<PaymentContact>, copPerUsd: Double? = null): String {
        val usd = usdFor(conv)
        return buildString {
            appendLine("-Tasa: ${conv.rateLabel}")
            appendLine("-${amountLabel(conv.currencyCode)}: ${FxCalculator.format2(conv.foreignAmount)}")
            append("-Monto Bs: ${FxCalculator.format2(conv.vesAmount)}")
            methods.forEach { method ->
                val block = methodBlock(method, usd, copPerUsd)
                if (block.isNotBlank()) {
                    appendLine()
                    appendLine()
                    append(block)
                }
            }
        }
    }

    fun methodBlock(m: PaymentContact, usdAmount: Double?, copPerUsd: Double?): String {
        val lines = mutableListOf<String>()
        fun add(label: String, value: String) { if (value.isNotBlank()) lines += "-$label: ${value.trim()}" }
        val bank = when {
            m.bankCode.isNotBlank() && m.bankName.isNotBlank() -> "${m.bankCode} - ${m.bankName}"
            else -> m.bankName.ifBlank { m.bankCode }
        }
        when (m.type) {
            PaymentType.PAGO_MOVIL -> {
                lines += "-Pago Movil:"
                add("Tlf", m.phone); add("CI", m.documentId); add("Banco", bank)
            }
            PaymentType.CUENTA_VE -> {
                lines += "-Transferencia:"
                add("Titular", m.displayName); add("Cuenta", m.accountNumber)
                add("CI", m.documentId); add("Banco", bank)
            }
            PaymentType.CUENTA_EXTRANJERA -> {
                lines += "-Cuenta extranjera:"
                add("Titular", m.displayName); add("Cuenta/IBAN", m.accountNumber); add("Banco", m.bankName)
                add("País", m.country); add("Moneda", m.currency); add("Documento", m.documentId); add("Datos", m.extraInfo)
            }
            PaymentType.ZELLE -> {
                lines += "-Zelle:"
                add("Nombre", m.displayName); add("Correo", m.email); add("Tlf", m.phone)
            }
            PaymentType.PAYPAL -> {
                lines += "-PayPal:"
                add("Nombre", m.displayName); add("Correo", m.email)
                if (m.userHandle.isNotBlank()) add("Link", paypalMe(m.userHandle))
            }
            PaymentType.BINANCE -> {
                lines += "-Binance Pay:"
                add("Pay ID", m.accountNumber); add("Correo", m.email); add("Usuario", m.userHandle); add("Nombre", m.displayName)
            }
            PaymentType.ZINLI -> {
                lines += "-Zinli:"
                add("Nombre", m.displayName); add("Correo", m.email); add("Tlf", m.phone)
            }
            PaymentType.WALLY -> {
                lines += "-Wally:"
                add("Nombre", m.displayName); add("Correo", m.email); add("Tlf", m.phone); add("Usuario", m.userHandle)
            }
            PaymentType.CUENTA_CO -> {
                lines += if (ColombianBanks.isWallet(m.bankName)) "-${m.bankName.trim()}:" else "-Cuenta Colombia:"
                if (!ColombianBanks.isWallet(m.bankName)) add("Banco", m.bankName)
                add("Tipo", m.accountType); add("Número", m.accountNumber); add("Tlf", m.phone)
                add("Titular", m.displayName); add("CC/NIT", m.documentId)
                if (usdAmount != null && copPerUsd != null && copPerUsd > 0) {
                    add("Monto COP", formatCop(usdAmount * copPerUsd))
                }
            }
        }
        if (m.type.isUsd && usdAmount != null) {
            val unit = if (m.type == PaymentType.BINANCE) "USDT" else "USD"
            lines += "-Monto a enviar: ${FxCalculator.format2(usdAmount)} $unit"
        }
        return if (lines.size <= 1 && !(m.type.isUsd && usdAmount != null)) "" else lines.joinToString("\n")
    }

    fun paypalMe(handle: String): String {
        val h = handle.trim().removePrefix("https://").removePrefix("http://").removePrefix("www.")
        return if (h.startsWith("paypal.me/", ignoreCase = true)) h else "paypal.me/${h.removePrefix("@")}"
    }

    /** COP sin decimales con punto de miles: 172.000 */
    fun formatCop(value: Double): String {
        val rounded = Math.round(value)
        return "%,d".format(java.util.Locale.US, rounded).replace(',', '.')
    }
}
