package com.codeonyx.bcvaqui.ui.home

import androidx.compose.ui.res.stringResource
import com.codeonyx.bcvaqui.R
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codeonyx.bcvaqui.domain.CalcEngine
import com.codeonyx.bcvaqui.domain.CalcInput
import com.codeonyx.bcvaqui.ui.MainViewModel
import com.codeonyx.bcvaqui.ui.appViewModel
import com.codeonyx.bcvaqui.ui.tools.copyText

/** Calculadora en hoja inferior (desde Inicio). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniCalculatorSheet(
    usdRate: Double,
    onDismiss: () -> Unit,
    viewModel: MainViewModel = appViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        CalculatorPanel(
            viewModel = viewModel,
            fallbackUsdRate = usdRate,
            modifier = Modifier.navigationBarsPadding()
        )
    }
}

private data class RateOption(val code: String, val label: String, val rate: Double)

/** Calculadora completa: teclado, resultado en vivo, conversión a/desde Bs. e historial. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalculatorPanel(
    viewModel: MainViewModel,
    fallbackUsdRate: Double = 0.0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()

    val rates = dashboard?.bcvRates.orEmpty()
    val options = buildList {
        val usd = rates["USD"]?.rateToVes ?: fallbackUsdRate.takeIf { it > 0 }
        usd?.let { add(RateOption("USD", "$ BCV", it)) }
        rates["EUR"]?.rateToVes?.let { add(RateOption("EUR", "€ BCV", it)) }
        (dashboard?.binanceUsdtVes ?: prefs.lastKnownBinanceUsdtVes)?.let {
            add(RateOption("USDT", "USDT", it))
        }
    }

    var expression by rememberSaveable { mutableStateOf("") }
    var rateCode by rememberSaveable { mutableStateOf("USD") }
    var toBs by rememberSaveable { mutableStateOf(true) }
    val input = CalcInput(expression)
    val result = input.result
    val option = options.firstOrNull { it.code == rateCode } ?: options.firstOrNull()
    val converted = if (result != null && option != null && option.rate > 0) {
        if (toBs) result * option.rate else result / option.rate
    } else null
    val fromUnit = if (toBs) option?.code ?: "" else "Bs."
    val toUnit = if (toBs) "Bs." else option?.code ?: ""

    fun update(next: CalcInput) { expression = next.expression }

    fun commit() {
        val r = result ?: return
        if (expression.any { it in CalcEngine.OPERATORS || it == '%' }) {
            viewModel.addCalculatorHistory("$expression = ${CalcEngine.format(r)}")
        }
        update(input.withValue(r))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Pantalla
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.End) {
                Text(
                    text = expression.ifEmpty { "0" },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                val resultText = result?.let { CalcEngine.format(it) } ?: if (expression.isEmpty()) "0" else "—"
                Text(
                    text = "= $resultText",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClickLabel = stringResource(R.string.calc_copy_result),
                            onClick = { result?.let { copyText(context, CalcEngine.format(it, 2)) } },
                            onLongClick = { result?.let { copyText(context, CalcEngine.format(it, 2)) } }
                        )
                )
            }
        }

        // Conversión
        if (options.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                options.forEach { o ->
                    FilterChip(
                        selected = o.code == option?.code,
                        onClick = { rateCode = o.code },
                        label = { Text("${o.label} ${CalcEngine.format(o.rate, 2)}") }
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "$fromUnit → $toUnit",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = converted?.let { "${CalcEngine.format(it, 2)} $toUnit" } ?: "—",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { toBs = !toBs }) {
                        Icon(Icons.Default.SwapVert, contentDescription = stringResource(R.string.calc_swap),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    IconButton(
                        onClick = { converted?.let { copyText(context, CalcEngine.format(it, 2)) } },
                        enabled = converted != null
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.calc_copy_conversion),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    TextButton(
                        onClick = {
                            converted?.let {
                                if (result != null) {
                                    viewModel.addCalculatorHistory(
                                        "${CalcEngine.format(result, 2)} $fromUnit = ${CalcEngine.format(it, 2)} $toUnit"
                                    )
                                }
                                update(CalcInput().withValue(Math.round(it * 100) / 100.0))
                                toBs = !toBs
                            }
                        },
                        enabled = converted != null
                    ) { Text(stringResource(R.string.calc_use)) }
                }
            }
        }

        // Teclado
        val keys = listOf(
            listOf("AC", "⌫", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("00", "0", ",", "=")
        )
        keys.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    val isOp = key in listOf("÷", "×", "−", "+", "%")
                    val isEq = key == "="
                    val isFn = key == "AC" || key == "⌫"
                    val bg = when {
                        isEq -> MaterialTheme.colorScheme.primary
                        isOp -> MaterialTheme.colorScheme.secondaryContainer
                        isFn -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceContainerHighest
                    }
                    val fg = when {
                        isEq -> MaterialTheme.colorScheme.onPrimary
                        isOp -> MaterialTheme.colorScheme.onSecondaryContainer
                        isFn -> MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = bg,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 60.dp)
                            .combinedClickable(
                                onClickLabel = key,
                                onLongClick = if (key == "⌫") ({ update(input.clear()) }) else null,
                                onClick = {
                                    when (key) {
                                        "AC" -> update(input.clear())
                                        "⌫" -> update(input.backspace())
                                        "%" -> update(input.percent())
                                        "," -> update(input.comma())
                                        "=" -> commit()
                                        "÷" -> update(input.operator(CalcEngine.DIVIDE))
                                        "×" -> update(input.operator(CalcEngine.TIMES))
                                        "−" -> update(input.operator(CalcEngine.MINUS))
                                        "+" -> update(input.operator(CalcEngine.PLUS))
                                        else -> update(input.digit(key))
                                    }
                                }
                            )
                    ) {
                        Row(
                            modifier = Modifier.heightIn(min = 60.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (key == "⌫") {
                                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = stringResource(R.string.calc_backspace), tint = fg)
                            } else {
                                Text(key, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = fg)
                            }
                        }
                    }
                }
            }
        }

        // Historial
        if (prefs.calculatorHistory.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.calc_history), style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { viewModel.clearCalculatorHistory() }) { Text(stringResource(R.string.calc_clear_history)) }
            }
            prefs.calculatorHistory.take(6).forEach { entry ->
                val value = entry.substringAfterLast("= ").substringBefore(" ")
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClickLabel = stringResource(R.string.calc_use_result),
                            onClick = { CalcEngine.evaluate(value)?.let { update(CalcInput().withValue(it)) } },
                            onLongClick = { copyText(context, value) }
                        )
                ) {
                    Text(
                        entry,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(stringResource(R.string.calc_history_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(1.dp).height(8.dp))
    }
}

