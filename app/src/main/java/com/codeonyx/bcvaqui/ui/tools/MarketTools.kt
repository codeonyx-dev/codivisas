package com.codeonyx.bcvaqui.ui.tools

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.codeonyx.bcvaqui.domain.FxCalculator
import com.codeonyx.bcvaqui.ui.MainViewModel
import com.codeonyx.bcvaqui.ui.appViewModel

@Composable
fun ParallelComparatorScreen(viewModel: MainViewModel = appViewModel()) {
    val dashboard by viewModel.dashboard.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val bcv = dashboard?.bcvRates?.get("USD")?.rateToVes
    val p2p = dashboard?.binanceUsdtVes ?: preferences.lastKnownBinanceUsdtVes
    var parallelInput by remember { mutableStateOf("") }
    val parallel = parallelInput.toDoubleOrNull()?.takeIf { it > 0 }

    ToolScaffold {
        ToolIntro(
            "Compara la tasa oficial y Binance P2P con una tasa paralela ingresada por ti."
        )
        ToolPanel {
            Text("Referencias automáticas", fontWeight = FontWeight.SemiBold)
            Text(
                "BCV USD: ${bcv?.let { "${FxCalculator.format2(it)} Bs." } ?: "Sin datos"}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                "Binance P2P: ${p2p?.let { "${FxCalculator.format2(it)} Bs." } ?: "Desactivado o sin datos"}",
                style = MaterialTheme.typography.bodyLarge
            )
            ToolNumberField(
                value = parallelInput,
                onValueChange = { parallelInput = it },
                label = "Tasa paralela manual",
                suffix = "Bs."
            )
        }

        val base = bcv
        val spread = if (parallel != null && base != null && base > 0) {
            FxCalculator.round2((parallel - base) / base * 100.0)
        } else null
        ToolResultCard(
            title = "Diferencia paralelo vs BCV",
            value = spread?.let {
                "${if (it > 0) "+" else ""}${FxCalculator.format2(it)}%"
            } ?: "Ingresa una tasa",
            detail = when {
                parallel == null -> "La tasa paralela es manual; SpreadVE no la publica ni la valida."
                base == null -> "No hay tasa BCV disponible para comparar."
                else -> "Paralelo ${FxCalculator.format2(parallel)} Bs. · BCV ${FxCalculator.format2(base)} Bs."
            },
            accent = Color(0xFF9333EA),
            changePercent = spread
        )
    }
}

@Composable
fun ArbitrageCalculatorScreen() {
    var amountInput by remember { mutableStateOf("100") }
    var buyRateInput by remember { mutableStateOf("") }
    var sellRateInput by remember { mutableStateOf("") }
    var feeInput by remember { mutableStateOf("0") }

    val amount = amountInput.toDoubleOrNull() ?: 0.0
    val buyRate = buyRateInput.toDoubleOrNull() ?: 0.0
    val sellRate = sellRateInput.toDoubleOrNull() ?: 0.0
    val feePercent = feeInput.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0
    val cost = amount * buyRate
    val grossSale = amount * sellRate
    val fees = grossSale * feePercent / 100.0
    val netSale = grossSale - fees
    val profit = netSale - cost
    val roi = if (cost > 0) FxCalculator.round2(profit / cost * 100.0) else 0.0

    ToolScaffold {
        ToolIntro(
            "Calcula el resultado estimado de comprar en un mercado y vender en otro."
        )
        ToolPanel {
            ToolNumberField(amountInput, { amountInput = it }, "Cantidad", "USDT")
            ToolNumberField(buyRateInput, { buyRateInput = it }, "Tasa de compra", "Bs.")
            ToolNumberField(sellRateInput, { sellRateInput = it }, "Tasa de venta", "Bs.")
            ToolNumberField(feeInput, { feeInput = it }, "Comisiones totales", "%")
        }
        ToolResultCard(
            title = if (profit >= 0) "Ganancia estimada" else "Pérdida estimada",
            value = "${FxCalculator.formatAmount(profit)} Bs.",
            detail = "Costo ${FxCalculator.formatAmount(cost)} Bs. · Venta neta ${
                FxCalculator.formatAmount(netSale)
            } Bs. · ROI ${if (roi > 0) "+" else ""}${FxCalculator.format2(roi)}%",
            accent = if (profit >= 0) Color(0xFF059669) else Color(0xFFDC2626),
            changePercent = roi
        )
        Text(
            "Cálculo informativo. No incluye límites, liquidez, tiempos ni variaciones durante la operación.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
