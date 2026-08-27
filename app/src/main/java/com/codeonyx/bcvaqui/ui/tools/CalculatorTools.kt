package com.codeonyx.bcvaqui.ui.tools

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
fun VariacionScreen(viewModel: MainViewModel = appViewModel()) {
    val dashboard by viewModel.dashboard.collectAsState()
    val current = dashboard?.bcvRates?.get("USD")?.rateToVes ?: 0.0

    var fromRate by remember { mutableStateOf("") }
    var toRate by remember(current) {
        mutableStateOf(if (current > 0) FxCalculator.format2(current) else "")
    }

    val a = parseToolAmount(fromRate)
    val b = parseToolAmount(toRate)
    val diff = FxCalculator.round2(b - a)
    val pct = if (a > 0) FxCalculator.round2((b - a) / a * 100.0) else 0.0
    val canShowResult = a > 0 && b > 0

    ToolScaffold {
        ToolIntro("Mide cuánto subió o bajó una tasa entre dos momentos, por ejemplo ayer y hoy.")
        ToolPanel {
            Text("Escribe las dos tasas", fontWeight = FontWeight.Bold)
            ToolNumberField(
                fromRate,
                { fromRate = it },
                "Antes",
                "Bs.",
                "La tasa inicial o más antigua."
            )
            ToolNumberField(
                toRate,
                { toRate = it },
                "Ahora",
                "Bs.",
                "La tasa nueva que quieres comparar."
            )
            if (current > 0) {
                FilterChip(
                    selected = false,
                    onClick = {
                        fromRate = toRate
                        toRate = FxCalculator.format2(current)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Usar tasa BCV actual (${FxCalculator.format2(current)})") },
                    colors = toolChipColors()
                )
            }
        }
        if (!canShowResult) {
            ToolEmptyResult(
                title = "Escribe la tasa anterior",
                hint = "Cuando completes ambas tasas verás la diferencia en bolívares y en porcentaje."
            )
        } else {
            ToolResultCard(
                title = "Variación",
                value = "${if (pct > 0) "+" else ""}${FxCalculator.format2(pct)}%",
                detail = "${FxCalculator.format2(diff)} Bs. entre ambas tasas",
                accent = when {
                    pct > 0.005 -> RateUp
                    pct < -0.005 -> RateDown
                    else -> BcvBlue
                },
                changePercent = pct
            )
        }
    }
}

@Composable
fun DividirScreen(viewModel: MainViewModel = appViewModel()) {
    val dashboard by viewModel.dashboard.collectAsState()
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

@Composable
fun CompartirToolScreen(viewModel: MainViewModel = appViewModel()) {
    val dashboard by viewModel.dashboard.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val rates = dashboard?.bcvRates.orEmpty()
    val currencies = buildList {
        addAll(rates.keys.sorted().ifEmpty { listOf("USD") })
        add(RateSources.BINANCE_USDT)
    }.distinct()
    val context = LocalContext.current

    var amount by remember { mutableStateOf("1") }
    var currency by remember {
        mutableStateOf(
            preferences.selectedCurrency.takeIf { it in currencies } ?: currencies.first()
        )
    }
    var convertToVes by remember { mutableStateOf(true) }
    var selectedPaymentId by remember { mutableStateOf<String?>(null) }

    val rate = RateSources.effectiveRate(
        currency = currency,
        bcvRates = rates,
        binanceUsdtVes = dashboard?.binanceUsdtVes ?: preferences.lastKnownBinanceUsdtVes
    ) ?: 0.0
    val amt = parseToolAmount(amount)
    val result = if (convertToVes) FxCalculator.round2(amt * rate)
    else if (rate > 0) FxCalculator.round2(amt / rate) else 0.0
    val foreign = if (convertToVes) amt else result
    val ves = if (convertToVes) result else amt
    val contacts = preferences.paymentContacts

    ToolScaffold {
        ToolIntro("Arma una conversión y compártela (opcional: datos de pago).")
        ToolPanel {
            CurrencyChips(currencies, currency) { currency = it }
            ToolNumberField(
                amount,
                { amount = it },
                if (convertToVes) "Monto en $currency" else "Monto en Bs."
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = convertToVes,
                    onClick = { convertToVes = true },
                    modifier = Modifier.weight(1f),
                    label = { Text("A bolívares") },
                    colors = toolChipColors()
                )
                FilterChip(
                    selected = !convertToVes,
                    onClick = { convertToVes = false },
                    modifier = Modifier.weight(1f),
                    label = { Text("A $currency") },
                    colors = toolChipColors()
                )
            }
        }
        ToolResultCard(
            title = "Resultado",
            value = if (convertToVes) "${FxCalculator.formatAmount(result)} Bs."
            else "${FxCalculator.formatAmount(result)} $currency",
            accent = Color(0xFF0F766E)
        )

        if (contacts.isNotEmpty()) {
            ToolPanel {
                Text("Incluir datos de pago", fontWeight = FontWeight.SemiBold)
                FilterChip(
                    selected = selectedPaymentId == null,
                    onClick = { selectedPaymentId = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Solo conversión") },
                    colors = toolChipColors()
                )
                contacts.forEach { c ->
                    FilterChip(
                        selected = selectedPaymentId == c.id,
                        onClick = { selectedPaymentId = c.id },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("${c.displayName} · ${c.typeLabel()}") },
                        colors = toolChipColors()
                    )
                }
            }
        }

        Button(
            onClick = {
                val receipt = ReceiptData(
                    currencyCode = currency,
                    rateToVes = rate,
                    foreignAmount = foreign,
                    vesAmount = ves,
                    updatedAt = dashboard?.bcvUpdatedAt
                        .takeUnless { currency == RateSources.BINANCE_USDT }
                )
                val payment = contacts.firstOrNull { it.id == selectedPaymentId }
                ReceiptShareHelper.share(
                    context = context,
                    data = receipt,
                    paymentText = payment?.formatForCopy()
                )
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = BcvBlue)
        ) {
            Text("Compartir", fontWeight = FontWeight.SemiBold)
        }
    }
}
