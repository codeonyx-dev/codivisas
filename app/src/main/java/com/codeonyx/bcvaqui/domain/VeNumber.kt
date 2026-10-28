package com.codeonyx.bcvaqui.domain

/**
 * Entrada numérica en formato venezolano.
 *
 * Forma canónica del campo: sin separador de miles y con coma decimal (ej. "1234,56").
 * Acepta texto pegado como "Bs. 1.234,56", "1,234.56", "$ 50" o "1.500".
 */
object VeNumber {
    private const val MAX_DECIMALS = 4

    /** Convierte cualquier texto (tecleado o pegado) a la forma canónica del campo. */
    fun sanitize(raw: String, maxDecimals: Int = MAX_DECIMALS): String {
        var cleaned = raw.filter { it.isDigit() || it == ',' || it == '.' }
        // Punto tecleado después de una coma ya existente: se ignora.
        if (cleaned.contains(',') && cleaned.endsWith('.')) cleaned = cleaned.dropLast(1)
        if (cleaned.isEmpty()) return ""
        val decimalIndex = decimalSeparatorIndex(cleaned)
        return if (decimalIndex < 0) {
            cleaned.filter(Char::isDigit).trimLeadingZeros()
        } else {
            val intPart = cleaned.substring(0, decimalIndex).filter(Char::isDigit).trimLeadingZeros()
            val decPart = cleaned.substring(decimalIndex + 1).filter(Char::isDigit).take(maxDecimals)
            (intPart.ifEmpty { "0" }) + "," + decPart
        }
    }

    /** Valor numérico del texto (canónico o pegado). Devuelve 0.0 si no es válido. */
    fun parse(raw: String): Double {
        val canonical = sanitize(raw, maxDecimals = 8)
        if (canonical.isEmpty()) return 0.0
        return canonical.replace(',', '.').toDoubleOrNull() ?: 0.0
    }

    /** Valor -> texto canónico editable (sin miles), ej. 1234.5 -> "1234,5". */
    fun toInput(value: Double): String {
        if (value <= 0.0) return ""
        val rounded = FxCalculator.round2(value)
        val text = java.math.BigDecimal.valueOf(rounded).stripTrailingZeros().toPlainString()
        return text.replace('.', ',')
    }

    /**
     * Decide qué carácter es el decimal:
     *  - Si hay coma y punto, el último que aparece es el decimal.
     *  - Solo comas: la última es decimal (formato venezolano).
     *  - Solo puntos: varios = miles; uno seguido de exactamente 3 dígitos en un
     *    texto pegado ("1.500") = miles; en otro caso es decimal.
     */
    private fun decimalSeparatorIndex(text: String): Int {
        val lastComma = text.lastIndexOf(',')
        val lastDot = text.lastIndexOf('.')
        if (lastComma >= 0 && lastDot >= 0) return maxOf(lastComma, lastDot)
        if (lastComma >= 0) return lastComma
        if (lastDot < 0) return -1
        val dots = text.count { it == '.' }
        if (dots > 1) return -1
        val after = text.length - lastDot - 1
        val before = lastDot
        return if (after == 3 && before in 1..3) -1 else lastDot
    }

    private fun String.trimLeadingZeros(): String {
        val trimmed = trimStart('0')
        return if (trimmed.isEmpty() && isNotEmpty()) "0" else trimmed
    }
}
