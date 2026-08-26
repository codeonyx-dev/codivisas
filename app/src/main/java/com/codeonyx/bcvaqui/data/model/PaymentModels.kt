package com.codeonyx.bcvaqui.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class PaymentType {
    PAGO_MOVIL,
    CUENTA_VE,
    CUENTA_EXTRANJERA
}

@Serializable
data class PaymentContact(
    val id: String,
    val type: PaymentType = PaymentType.PAGO_MOVIL,
    /** Nombre de la persona / etiqueta */
    val displayName: String = "",
    val phone: String = "",
    val accountNumber: String = "",
    /** Cédula / documento */
    val documentId: String = "",
    /** Código de banco VE, ej. 0134 */
    val bankCode: String = "",
    /** Nombre del banco (VE o extranjero) */
    val bankName: String = "",
    val country: String = "",
    val currency: String = "",
    /** IBAN, SWIFT, routing, etc. */
    val extraInfo: String = "",
    val notes: String = ""
) {
    fun formatForCopy(): String = buildString {
        when (type) {
            PaymentType.PAGO_MOVIL -> {
                appendLine("Pago móvil")
                if (displayName.isNotBlank()) appendLine("- Nombre: $displayName")
                if (phone.isNotBlank()) appendLine("- Teléfono: $phone")
                if (documentId.isNotBlank()) appendLine("- Cédula: $documentId")
                if (bankLabel().isNotBlank()) appendLine("- Banco: ${bankLabel()}")
            }
            PaymentType.CUENTA_VE -> {
                appendLine("Transferencia / cuenta")
                if (displayName.isNotBlank()) appendLine("- Nombre: $displayName")
                if (accountNumber.isNotBlank()) appendLine("- Cuenta: $accountNumber")
                if (documentId.isNotBlank()) appendLine("- Cédula: $documentId")
                if (bankLabel().isNotBlank()) appendLine("- Banco: ${bankLabel()}")
            }
            PaymentType.CUENTA_EXTRANJERA -> {
                appendLine("Cuenta extranjera")
                if (displayName.isNotBlank()) appendLine("- Nombre: $displayName")
                if (accountNumber.isNotBlank()) appendLine("- Cuenta / IBAN: $accountNumber")
                if (bankName.isNotBlank()) appendLine("- Banco: $bankName")
                if (country.isNotBlank()) appendLine("- País: $country")
                if (currency.isNotBlank()) appendLine("- Moneda: $currency")
                if (documentId.isNotBlank()) appendLine("- Documento: $documentId")
                if (extraInfo.isNotBlank()) appendLine("- $extraInfo")
            }
        }
    }.trim()

    fun bankLabel(): String = when {
        bankCode.isNotBlank() && bankName.isNotBlank() -> "$bankName ($bankCode)"
        bankName.isNotBlank() -> bankName
        bankCode.isNotBlank() -> bankCode
        else -> ""
    }

    fun typeLabel(): String = when (type) {
        PaymentType.PAGO_MOVIL -> "Pago móvil"
        PaymentType.CUENTA_VE -> "Cuenta VE"
        PaymentType.CUENTA_EXTRANJERA -> "Cuenta extranjera"
    }

    fun subtitle(): String = when (type) {
        PaymentType.PAGO_MOVIL -> phone.ifBlank { documentId }
        PaymentType.CUENTA_VE -> accountNumber.ifBlank { bankLabel() }
        PaymentType.CUENTA_EXTRANJERA -> listOf(bankName, country, currency)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
            .ifBlank { accountNumber }
    }
}

data class VenezuelanBank(
    val code: String,
    val name: String
) {
    val label: String get() = "$code · $name"
}

object VenezuelanBanks {
    val ALL = listOf(
        VenezuelanBank("0102", "Banco de Venezuela"),
        VenezuelanBank("0104", "Venezolano de Crédito"),
        VenezuelanBank("0105", "Banco Mercantil"),
        VenezuelanBank("0108", "BBVA Provincial"),
        VenezuelanBank("0114", "Bancaribe"),
        VenezuelanBank("0115", "Banco Exterior"),
        VenezuelanBank("0128", "Banco Caroní"),
        VenezuelanBank("0134", "Banesco"),
        VenezuelanBank("0137", "Sofitasa"),
        VenezuelanBank("0138", "Banco Plaza"),
        VenezuelanBank("0151", "Banco Fondo Común"),
        VenezuelanBank("0156", "100% Banco"),
        VenezuelanBank("0157", "DelSur"),
        VenezuelanBank("0163", "Banco del Tesoro"),
        VenezuelanBank("0168", "Bancrecer"),
        VenezuelanBank("0169", "Mi Banco"),
        VenezuelanBank("0171", "Banco Activo"),
        VenezuelanBank("0172", "Bancamiga"),
        VenezuelanBank("0174", "Banplus"),
        VenezuelanBank("0175", "Bicentenario"),
        VenezuelanBank("0191", "BNC"),
        VenezuelanBank("0601", "IMCP")
    )

    fun byCode(code: String): VenezuelanBank? = ALL.find { it.code == code }
}
