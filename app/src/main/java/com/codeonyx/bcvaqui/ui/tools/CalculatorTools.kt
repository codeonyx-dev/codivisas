package com.codeonyx.bcvaqui.ui.tools

import androidx.compose.material3.MaterialTheme

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codeonyx.bcvaqui.ui.appViewModel
import com.codeonyx.bcvaqui.domain.FxCalculator
import com.codeonyx.bcvaqui.domain.VeNumber
import com.codeonyx.bcvaqui.domain.RateSources
import com.codeonyx.bcvaqui.ui.MainViewModel
import com.codeonyx.bcvaqui.ui.share.ReceiptData
import com.codeonyx.bcvaqui.ui.share.ReceiptShareHelper
import com.codeonyx.bcvaqui.ui.theme.BcvBlue
import com.codeonyx.bcvaqui.ui.theme.RateDown
import com.codeonyx.bcvaqui.ui.theme.RateUp

@Composable
fun ComisionScreen() {
    var amount by remember { mutableStateOf("100") }
    var feePct by remember { mutableStateOf("3") }
    var feeOnSender by remember { mutableStateOf(true) }

    val base = parseToolAmount(amount)
    val pct = parseToolAmount(feePct)
    val fee = FxCalculator.round2(base * pct / 100.0)
    val net = if (feeOnSender) FxCalculator.round2(base - fee) else base
    val totalPaid = if (feeOnSender) base else FxCalculator.round2(base + fee)

    ToolScaffold {
        ToolIntro("Calcula el neto y el costo de una comisión o remesa.")
        ToolPanel {
            Text("Datos de la operación", fontWeight = FontWeight.Bold)
            ToolNumberField(
                amount,
                { amount = it },
                "Monto original",
                "$",
                "Cantidad antes de aplicar la comisión."
            )
            ToolNumberField(
                feePct,
                { feePct = it },
                "Porcentaje de comisión",
                "%",
                "Ejemplo: escribe 3 si cobran 3%."
            )
            Text(
                "¿Cómo cobran la comisión?",
                fontWeight = FontWeight.SemiBold
            )
            ToolChoiceFlow(maxItemsInEachRow = 1) {
                FilterChip(
                    selected = feeOnSender,
                    onClick = { feeOnSender = true },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("La persona recibe menos") },
                    colors = toolChipColors()
                )
                FilterChip(
                    selected = !feeOnSender,
                    onClick = { feeOnSender = false },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Yo pago la comisión aparte") },
                    colors = toolChipColors()
                )
            }
        }
        ToolResultCard(
            title = if (feeOnSender) "Recibe neto" else "Monto a enviar",
            value = "$" + FxCalculator.format2(net),
            detail = "Comisión $" + FxCalculator.format2(fee) +
                " · Total pagado $" + FxCalculator.format2(totalPaid),
            accent = Color(0xFFD97706)
        )
    }
}

@Composable
fun DividirScreen(viewModel: MainViewModel = appViewModel()) {
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val usdRate = dashboard?.bcvRates?.get("USD")?.rateToVes ?: 0.0

    var total by remember { mutableStateOf("100") }
    var people by remember { mutableStateOf("2") }
    var inVes by remember { mutableStateOf(true) }

    val amount = parseToolAmount(total)
    val n = people.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val each = FxCalculator.round2(amount / n)
    val eachOther = when {
        inVes && usdRate > 0 -> FxCalculator.round2(each / usdRate)
        !inVes && usdRate > 0 -> FxCalculator.round2(each * usdRate)
        else -> 0.0
    }

    ToolScaffold {
        ToolIntro("Divide una cuenta en partes iguales y mira el equivalente en bolívares o dólares.")
        ToolPanel {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = inVes,
                    onClick = { inVes = true },
                    modifier = Modifier.weight(1f),
                    label = { Text("En Bs.") },
                    colors = toolChipColors()
                )
                FilterChip(
                    selected = !inVes,
                    onClick = { inVes = false },
                    modifier = Modifier.weight(1f),
                    label = { Text("En USD") },
                    colors = toolChipColors()
                )
            }
            ToolNumberField(
                total,
                { total = it },
                "Total de la cuenta",
                if (inVes) "Bs." else "$"
            )
            ToolNumberField(
                people,
                { people = it.filter { c -> c.isDigit() } },
                "Cantidad de personas",
                helper = "El total se divide en partes iguales."
            )
        }
        ToolResultCard(
            title = "Cada uno paga",
            value = if (inVes) {
                "${FxCalculator.formatAmount(each)} Bs."
            } else {
                "${FxCalculator.formatAmount(each)} $"
            },
            detail = if (inVes) {
                "≈ ${FxCalculator.formatAmount(eachOther)} USD"
            } else {
                "≈ ${FxCalculator.formatAmount(eachOther)} Bs."
            },
            accent = Color(0xFFEA580C)
        )
    }
}

