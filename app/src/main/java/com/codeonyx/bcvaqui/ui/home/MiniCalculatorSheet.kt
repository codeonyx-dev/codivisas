package com.codeonyx.bcvaqui.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeonyx.bcvaqui.domain.FxCalculator
import com.codeonyx.bcvaqui.ui.theme.BcvBlue
import com.codeonyx.bcvaqui.ui.theme.BcvGold
import com.codeonyx.bcvaqui.ui.tools.parseToolAmount
import kotlin.math.abs

private enum class CalcMode { CUENTAS, DOLARES }

private enum class CalcOp(val symbol: String) {
    Add("+"),
    Subtract("−"),
    Multiply("×"),
    Divide("÷")
}

private val usdPresets = listOf(1.0, 5.0, 10.0, 20.0, 50.0, 100.0, 200.0)
private val feePresets = listOf(3.0, 5.0, 10.0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniCalculatorSheet(
    usdRate: Double,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var mode by remember { mutableStateOf(CalcMode.DOLARES) }
    var inputInUsd by remember { mutableStateOf(true) }
    var display by remember { mutableStateOf("0") }
    var accumulator by remember { mutableStateOf<Double?>(null) }
    var pendingOp by remember { mutableStateOf<CalcOp?>(null) }
    var freshEntry by remember { mutableStateOf(true) }

    fun formatValue(value: Double, useGrouping: Boolean = true): String {
        if (value.isNaN() || value.isInfinite()) return "Error"
        val rounded = FxCalculator.round2(value)
        if (!useGrouping) {
            return if (abs(rounded - rounded.toLong()) < 0.001) {
                rounded.toLong().toString()
            } else {
                rounded.toString()
            }
        }
        return FxCalculator.format2(rounded)
    }

    fun currentValue(): Double = parseToolAmount(display)

    fun resetCalc() {
        display = "0"
        accumulator = null
        pendingOp = null
        freshEntry = true
    }

    fun applyPendingOperation(nextValue: Double): Double? {
        val left = accumulator ?: return nextValue
        val op = pendingOp ?: return nextValue
        return when (op) {
            CalcOp.Add -> left + nextValue
            CalcOp.Subtract -> left - nextValue
            CalcOp.Multiply -> left * nextValue
            CalcOp.Divide -> if (nextValue == 0.0) Double.NaN else left / nextValue
        }
    }

    fun setError() {
        display = "Error"
        accumulator = null
        pendingOp = null
        freshEntry = true
    }

    fun onDigit(digit: String) {
        if (display == "Error") resetCalc()
        display = when {
            freshEntry -> if (digit == ".") "0." else digit
            display == "0" && digit != "." -> digit
            digit == "." && display.contains('.') -> display
            else -> display + digit
        }
        freshEntry = false
    }

    fun onOperator(op: CalcOp) {
        if (display == "Error") return
        val value = currentValue()
        val result = applyPendingOperation(value)
        if (result == null || result.isNaN() || result.isInfinite()) {
            setError()
            return
        }
        accumulator = result
        pendingOp = op
        display = formatValue(result, useGrouping = false)
        freshEntry = true
    }

    fun onEquals() {
        if (display == "Error") return
        val value = currentValue()
        val result = applyPendingOperation(value) ?: return
        if (result.isNaN() || result.isInfinite()) {
            setError()
            return
        }
        display = formatValue(result)
        accumulator = null
        pendingOp = null
        freshEntry = true
    }

    fun onBackspace() {
        if (freshEntry || display == "Error") return
        display = when {
            display.length <= 1 || (display.length == 2 && display.startsWith("-")) -> "0"
            else -> display.dropLast(1)
        }
        if (display == "-") display = "0"
    }

    fun setPreset(value: Double) {
        display = formatValue(value, useGrouping = false)
        freshEntry = true
    }

    fun applyFeePercent(percent: Double, addToTotal: Boolean) {
        if (display == "Error") return
        val base = currentValue()
        val next = if (addToTotal) {
            base * (1.0 + percent / 100.0)
        } else {
            base * (1.0 - percent / 100.0)
        }
        display = formatValue(next)
        accumulator = null
        pendingOp = null
        freshEntry = true
    }

    fun primaryAmount(): Double {
        if (display == "Error") return 0.0
        return currentValue()
    }

    fun usdAmount(): Double {
        val value = primaryAmount()
        return if (inputInUsd) value else if (usdRate > 0) value / usdRate else 0.0
    }

    fun bsAmount(): Double {
        val value = primaryAmount()
        return if (inputInUsd) FxCalculator.round2(value * usdRate) else value
    }

    fun copyText(text: String, label: String) {
        copyToClipboard(context, text)
        Toast.makeText(context, "$label copiado", Toast.LENGTH_SHORT).show()
    }

    fun buildExpressionText(): String? {
        val op = pendingOp ?: return null
        val left = accumulator ?: return null
        return if (freshEntry) {
            "${formatValue(left)} ${op.symbol}"
        } else {
            "${formatValue(left)} ${op.symbol} $display"
        }
    }

    fun previewResult(): Double? {
        if (display == "Error" || pendingOp == null || freshEntry) return null
        val result = applyPendingOperation(currentValue()) ?: return null
        return if (result.isNaN() || result.isInfinite()) null else result
    }

    val expressionText = buildExpressionText()
    val livePreview = previewResult()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Calculadora rápida",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = mode == CalcMode.DOLARES,
                    onClick = { mode = CalcMode.DOLARES },
                    label = { Text("Dólares") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BcvBlue.copy(alpha = 0.15f),
                        selectedLabelColor = BcvBlue
                    )
                )
                FilterChip(
                    selected = mode == CalcMode.CUENTAS,
                    onClick = { mode = CalcMode.CUENTAS },
                    label = { Text("Cuentas") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BcvBlue.copy(alpha = 0.15f),
                        selectedLabelColor = BcvBlue
                    )
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (mode == CalcMode.DOLARES && usdRate > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "1 USD = ${FxCalculator.format2(usdRate)} Bs.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = { inputInUsd = !inputInUsd }) {
                                Icon(
                                    Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (inputInUsd) "Ingreso USD" else "Ingreso Bs.",
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }

                    expressionText?.let { expr ->
                        Text(
                            text = expr,
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.End,
                            maxLines = 1
                        )
                    }

                    livePreview?.let { preview ->
                        Text(
                            text = "= ${formatValue(preview)}",
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = BcvBlue.copy(alpha = 0.75f),
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.End
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val mainText = when {
                                display == "Error" -> "Error"
                                pendingOp != null && !freshEntry -> display
                                mode == CalcMode.DOLARES && inputInUsd ->
                                    "${formatValue(usdAmount())} USD"
                                mode == CalcMode.DOLARES ->
                                    "${formatValue(bsAmount())} Bs."
                                else -> display
                            }
                            Text(
                                text = mainText,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (mode == CalcMode.DOLARES && display != "Error" && usdRate > 0) {
                                val conversionText = when {
                                    livePreview != null && inputInUsd ->
                                        "≈ ${formatValue(FxCalculator.round2(livePreview * usdRate))} Bs."
                                    livePreview != null && !inputInUsd && usdRate > 0 ->
                                        "≈ ${formatValue(livePreview / usdRate)} USD"
                                    inputInUsd ->
                                        "≈ ${formatValue(bsAmount())} Bs."
                                    else ->
                                        "≈ ${formatValue(usdAmount())} USD"
                                }
                                Text(
                                    text = conversionText,
                                    modifier = Modifier.fillMaxWidth(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = BcvGold,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.End
                                )
                            }
                        }

                        if (display != "Error") {
                            IconButton(
                                onClick = {
                                    val text = when {
                                        livePreview != null && pendingOp != null ->
                                            "${expressionText} = ${formatValue(livePreview)}"
                                        mode == CalcMode.DOLARES -> {
                                            if (inputInUsd) {
                                                "${formatValue(usdAmount())} USD = ${formatValue(bsAmount())} Bs."
                                            } else {
                                                "${formatValue(bsAmount())} Bs. = ${formatValue(usdAmount())} USD"
                                            }
                                        }
                                        else -> formatValue(currentValue())
                                    }
                                    copyText(text, "Resultado")
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar")
                            }
                        }
                    }
                }
            }

            if (mode == CalcMode.DOLARES) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    usdPresets.forEach { preset ->
                        OutlinedButton(
                            onClick = { setPreset(preset) },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = ButtonDefaults.ContentPadding
                        ) {
                            Text(
                                text = if (preset >= 1) "${preset.toInt()}" else formatValue(preset, false),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    feePresets.forEach { pct ->
                        OutlinedButton(
                            onClick = { applyFeePercent(pct, addToTotal = false) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("−$pct%", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            if (display != "Error") {
                                display = formatValue(currentValue() / 2.0)
                                freshEntry = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("÷2", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            CalcKeypad(
                onClear = { resetCalc() },
                onBackspace = { onBackspace() },
                onDigit = { onDigit(it) },
                onOperator = { onOperator(it) },
                onEquals = { onEquals() }
            )
        }
    }
}

@Composable
private fun CalcKeypad(
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onDigit: (String) -> Unit,
    onOperator: (CalcOp) -> Unit,
    onEquals: () -> Unit
) {
    val rows = listOf(
        listOf("C", "⌫", "÷", "×"),
        listOf("7", "8", "9", "−"),
        listOf("4", "5", "6", "+"),
        listOf("1", "2", "3", "=")
    )

    rows.forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            row.forEach { key ->
                CalcKeyButton(
                    label = key,
                    modifier = Modifier.weight(1f),
                    emphasized = key == "=",
                    onClick = {
                        when (key) {
                            "C" -> onClear()
                            "⌫" -> onBackspace()
                            "+" -> onOperator(CalcOp.Add)
                            "−" -> onOperator(CalcOp.Subtract)
                            "×" -> onOperator(CalcOp.Multiply)
                            "÷" -> onOperator(CalcOp.Divide)
                            "=" -> onEquals()
                            "." -> onDigit(".")
                            else -> onDigit(key)
                        }
                    }
                )
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CalcKeyButton(
            label = "0",
            modifier = Modifier.weight(2f),
            onClick = { onDigit("0") }
        )
        CalcKeyButton(
            label = ".",
            modifier = Modifier.weight(1f),
            onClick = { onDigit(".") }
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CalcKeyButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false
) {
    val isOperator = label in listOf("+", "−", "×", "÷", "=")
    if (emphasized || isOperator) {
        Button(
            onClick = onClick,
            modifier = modifier.height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (emphasized) BcvBlue else MaterialTheme.colorScheme.primaryContainer,
                contentColor = if (emphasized) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }
            )
        ) {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(label, fontWeight = FontWeight.Medium, fontSize = 18.sp)
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Calculadora", text))
}
