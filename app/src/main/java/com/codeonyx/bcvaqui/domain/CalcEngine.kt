package com.codeonyx.bcvaqui.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Motor de la calculadora. Expresiones con coma decimal venezolana y los operadores
 * + − × ÷ y % (postfijo):
 *  - "200 + 10%" = 220 (porcentaje del valor anterior)
 *  - "200 × 10%" = 20
 *  - "15%" = 0,15
 */
object CalcEngine {
    const val PLUS = '+'
    const val MINUS = '−'
    const val TIMES = '×'
    const val DIVIDE = '÷'
    val OPERATORS = setOf(PLUS, MINUS, TIMES, DIVIDE)

    /** Evalúa la expresión. Ignora un operador o coma final (resultado en vivo). null si no es válida. */
    fun evaluate(expression: String): Double? {
        var expr = expression.replace(" ", "").replace('-', MINUS).replace('*', TIMES).replace('/', DIVIDE)
        while (expr.isNotEmpty() && (expr.last() in OPERATORS || expr.last() == ',')) expr = expr.dropLast(1)
        if (expr.isEmpty()) return null
        return try {
            val parser = Parser(expr)
            val value = parser.parseExpression()
            if (!parser.atEnd() || value.isNaN() || value.isInfinite()) null else value
        } catch (e: IllegalArgumentException) {
            null
        } catch (e: ArithmeticException) {
            null
        }
    }

    /** Número con miles "." y hasta 4 decimales ",": 1234.5 -> "1.234,5". */
    fun format(value: Double, maxDecimals: Int = 4): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        val pattern = "#,##0" + if (maxDecimals > 0) "." + "#".repeat(maxDecimals) else ""
        val df = DecimalFormat(pattern, symbols)
        df.roundingMode = RoundingMode.HALF_UP
        val rounded = BigDecimal.valueOf(value).setScale(maxDecimals, RoundingMode.HALF_UP)
        val text = df.format(rounded)
        return if (text == "-0") "0" else text
    }

    private class Parser(private val s: String) {
        private var i = 0
        fun atEnd() = i >= s.length

        private data class Term(val value: Double, val percentOnly: Boolean)

        fun parseExpression(): Double {
            var left = parseTerm().let { if (it.percentOnly) it.value / 100.0 else it.value }
            while (!atEnd() && (s[i] == PLUS || s[i] == MINUS)) {
                val op = s[i++]
                val right = parseTerm()
                val r = if (right.percentOnly) left * right.value / 100.0 else right.value
                left = if (op == PLUS) left + r else left - r
            }
            return left
        }

        private fun parseTerm(): Term {
            var (value, percent) = parseFactor()
            var single = true
            while (!atEnd() && (s[i] == TIMES || s[i] == DIVIDE)) {
                if (percent) value /= 100.0
                percent = false
                single = false
                val op = s[i++]
                val (rv, rp) = parseFactor()
                val r = if (rp) rv / 100.0 else rv
                value = if (op == TIMES) {
                    value * r
                } else {
                    if (r == 0.0) throw ArithmeticException("División entre cero")
                    value / r
                }
            }
            return Term(value, single && percent)
        }

        private fun parseFactor(): Pair<Double, Boolean> {
            var sign = 1.0
            while (!atEnd() && s[i] == MINUS) { sign = -sign; i++ }
            val start = i
            while (!atEnd() && (s[i].isDigit() || s[i] == ',' || s[i] == '.')) i++
            require(i > start) { "Número esperado" }
            val raw = s.substring(start, i).replace(".", "").replace(',', '.')
            val number = raw.toDoubleOrNull() ?: throw IllegalArgumentException("Número inválido")
            var percent = false
            if (!atEnd() && s[i] == '%') { percent = true; i++ }
            return sign * number to percent
        }
    }
}

/** Estado editable de la calculadora (sin Android, fácil de probar). */
data class CalcInput(val expression: String = "") {
    private val lastNumber: String
        get() = expression.takeLastWhile { it.isDigit() || it == ',' }

    fun digit(d: String): CalcInput {
        if (expression.endsWith("%")) return CalcInput(expression + CalcEngine.TIMES + d)
        val last = lastNumber
        if (last == "0" && d.all { it == '0' }) return this
        if (last == "0") return CalcInput(expression.dropLast(1) + d)
        if (last.isEmpty() && d == "00") return CalcInput(expression + "0")
        return CalcInput(expression + d)
    }

    fun comma(): CalcInput {
        if (lastNumber.contains(',')) return this
        if (expression.endsWith("%")) return CalcInput(expression + CalcEngine.TIMES + "0,")
        return if (lastNumber.isEmpty()) CalcInput(expression + "0,") else CalcInput(expression + ",")
    }

    fun operator(op: Char): CalcInput {
        if (expression.isEmpty()) return if (op == CalcEngine.MINUS) CalcInput("−") else this
        val trimmed = if (expression.last() in CalcEngine.OPERATORS || expression.last() == ',') {
            if (expression.length == 1) return this
            expression.dropLast(1)
        } else {
            expression
        }
        return CalcInput(trimmed + op)
    }

    fun percent(): CalcInput {
        if (lastNumber.isEmpty() || expression.endsWith("%")) return this
        return CalcInput(expression + "%")
    }

    fun backspace(): CalcInput = CalcInput(expression.dropLast(1))

    fun clear(): CalcInput = CalcInput("")

    /** Reemplaza todo por un valor (resultado anterior, conversión, historial). */
    fun withValue(value: Double): CalcInput =
        CalcInput(CalcEngine.format(value).replace(".", "").let { if (value < 0) "−" + it.removePrefix("-") else it })

    val result: Double? get() = CalcEngine.evaluate(expression)
}
