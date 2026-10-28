package com.codeonyx.bcvaqui.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Textos de los botones de copiar en Inicio. */
object CopyTexts {
    private val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    /** "Tasa BCV 27/10/2026: 888,12 Bs." — la fecha sale de updatedAt (ISO) o de [today]. */
    fun rateOfDay(rateLabel: String, rate: Double, updatedAt: String?, today: LocalDate = LocalDate.now()): String {
        val date = updatedAt?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today
        return "Tasa $rateLabel ${date.format(dateFmt)}: ${FxCalculator.format2(rate)} Bs."
    }

    /** "5,00 $ = 4.440,60 Bs." o "4.440,60 Bs. = 5,00 $" según la dirección. */
    fun conversion(input: Double, result: Double, symbol: String, toVes: Boolean): String =
        if (toVes) "${FxCalculator.format2(input)} $symbol = ${FxCalculator.format2(result)} Bs."
        else "${FxCalculator.format2(input)} Bs. = ${FxCalculator.format2(result)} $symbol"

    fun amountOnly(result: Double): String = FxCalculator.format2(result)
}
