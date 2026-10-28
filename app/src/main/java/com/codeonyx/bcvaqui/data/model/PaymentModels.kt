package com.codeonyx.bcvaqui.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class PaymentType {
    PAGO_MOVIL,
    CUENTA_VE,
    CUENTA_EXTRANJERA,
    BINANCE,
    WALLY,
    ZINLI,
    CUENTA_CO,
    ZELLE,
    PAYPAL;

    /** Métodos que se cobran en dólares: al compartir se destaca el monto en USD. */
    val isUsd: Boolean get() = this == BINANCE || this == WALLY || this == ZINLI || this == ZELLE || this == PAYPAL
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
    val notes: String = "",
    /** Correo (Zelle, PayPal, Binance, Zinli, Wally). */
    val email: String = "",
    /** Usuario / alias (Binance user, paypal.me, Wally). */
    val userHandle: String = "",
    /** Tipo de cuenta (Ahorros / Corriente / Nequi / Daviplata). */
    val accountType: String = "",
    val isDefault: Boolean = false
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
            else -> append(com.codeonyx.bcvaqui.domain.ShareFormatter.methodBlock(this@PaymentContact, null, null))
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
        PaymentType.BINANCE -> "Binance"
        PaymentType.WALLY -> "Wally"
        PaymentType.ZINLI -> "Zinli"
        PaymentType.CUENTA_CO -> "Cuenta Colombia"
        PaymentType.ZELLE -> "Zelle"
        PaymentType.PAYPAL -> "PayPal"
    }

    fun subtitle(): String = when (type) {
        PaymentType.PAGO_MOVIL -> phone.ifBlank { documentId }
        PaymentType.CUENTA_VE -> accountNumber.ifBlank { bankLabel() }
        PaymentType.CUENTA_EXTRANJERA -> listOf(bankName, country, currency)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
            .ifBlank { accountNumber }
        PaymentType.BINANCE -> listOf(accountNumber, email, userHandle).firstOrNull { it.isNotBlank() }.orEmpty()
        PaymentType.WALLY, PaymentType.ZINLI -> listOf(email, phone, userHandle).firstOrNull { it.isNotBlank() }.orEmpty()
        PaymentType.ZELLE -> email.ifBlank { phone }
        PaymentType.PAYPAL -> email.ifBlank { userHandle }
        PaymentType.CUENTA_CO -> listOf(bankName, accountType, accountNumber.ifBlank { phone })
            .filter { it.isNotBlank() }.joinToString(" · ")
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

/** Bancos y billeteras comunes en Colombia. */
object ColombianBanks {
    val ALL = listOf(
        "Bancolombia", "Banco de Bogotá", "Davivienda", "BBVA Colombia", "Banco de Occidente",
        "Banco Popular", "Banco AV Villas", "Banco Caja Social", "Scotiabank Colpatria",
        "Banco Agrario", "Itaú", "Banco Falabella", "Nequi", "Daviplata"
    )
    val ACCOUNT_TYPES = listOf("Ahorros", "Corriente")
    fun isWallet(bank: String) = bank.equals("Nequi", true) || bank.equals("Daviplata", true)
}

/** Lectura tolerante de los métodos guardados (migra datos viejos sin perderlos). */
object PaymentMigration {
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; coerceInputValues = true }

    fun decode(raw: String?): List<PaymentContact> {
        if (raw.isNullOrBlank()) return emptyList()
        val array = runCatching { json.parseToJsonElement(raw) as? kotlinx.serialization.json.JsonArray }
            .getOrNull() ?: return emptyList()
        val contacts = array.mapNotNull { element ->
            runCatching { json.decodeFromJsonElement(PaymentContact.serializer(), element) }.getOrNull()
        }
        return normalize(contacts)
    }

    /** Garantiza un único predeterminado: el marcado, o el primer pago móvil, o el primero. */
    fun normalize(contacts: List<PaymentContact>): List<PaymentContact> {
        if (contacts.isEmpty()) return contacts
        val defaultId = contacts.firstOrNull { it.isDefault }?.id
            ?: contacts.firstOrNull { it.type == PaymentType.PAGO_MOVIL }?.id
            ?: contacts.first().id
        return contacts.map { it.copy(isDefault = it.id == defaultId) }
    }

    fun setDefault(contacts: List<PaymentContact>, id: String): List<PaymentContact> =
        contacts.map { it.copy(isDefault = it.id == id) }
}
