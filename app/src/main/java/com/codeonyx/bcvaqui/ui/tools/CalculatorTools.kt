package com.codeonyx.bcvaqui.ui.tools

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codeonyx.bcvaqui.domain.FxCalculator
import com.codeonyx.bcvaqui.ui.MainViewModel
import com.codeonyx.bcvaqui.ui.share.ReceiptData
import com.codeonyx.bcvaqui.ui.share.ReceiptShareHelper

@Composable
fun AlcanzaScreen(viewModel: MainViewModel = viewModel()) {
    val dashboard by viewModel.dashboard.collectAsState()
    val rates = dashboard?.bcvRates.orEmpty()
    val currencies = rates.keys.sorted().ifEmpty { listOf("USD") }

    var amountBs by remember { mutableStateOf("1000") }
    var currency by remember { mutableStateOf(currencies.firstOrNull() ?: "USD") }
    val rate = rates[currency]?.rateToVes ?: 0.0
    val bs = parseToolAmount(amountBs)
    val foreign = if (rate > 0) FxCalculator.round2(bs / rate) else 0.0

    ToolScaffold {
        Text(
            text = "Tienes bolívares y quieres saber a cuánto equivalen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ToolNumberField(amountBs, { amountBs = it }, "Monto en bolívares", "Bs.")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("100", "500", "1000", "5000", "10000").forEach { chip ->
                FilterChip(
                    selected = amountBs == chip,
                    onClick = { amountBs = chip },
                    label = { Text(chip) }
                )
            }
        }
        CurrencyChips(currencies, currency) { currency = it }
        ToolResultCard(
            title = "Te alcanzan",
            value = "${FxCalculator.formatAmount(foreign)} ${FxCalculator.currencySymbol(currency)}",
            detail = "a tasa BCV ${FxCalculator.format2(rate)} Bs./$currency"
        )
    }
}

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
        Text(
            text = "Calcula el neto y el costo de una comisión o remesa.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ToolNumberField(amount, { amount = it }, "Monto", "$")
        ToolNumberField(feePct, { feePct = it }, "Comisión", "%")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = feeOnSender,
                onClick = { feeOnSender = true },
                label = { Text("Se descuenta del monto") }
            )
            FilterChip(
                selected = !feeOnSender,
                onClick = { feeOnSender = false },
                label = { Text("Se suma al pagar") }
            )
        }
        ToolResultCard("Comisión", FxCalculator.format2(fee))
        ToolResultCard(
            title = if (feeOnSender) "Recibe neto" else "Monto a enviar",
            value = FxCalculator.format2(net)
        )
        ToolResultCard(
            title = "Total que pagas",
            value = FxCalculator.format2(totalPaid),
            detail = "Comisión $pct%"
        )
    }
}

@Composable
fun VariacionScreen(viewModel: MainViewModel = viewModel()) {
    val dashboard by viewModel.dashboard.collectAsState()
    val current = dashboard?.bcvRates?.get("USD")?.rateToVes ?: 0.0

    var fromRate by remember(current) {
        mutableStateOf(if (current > 0) FxCalculator.format2(current) else "")
    }
    var toRate by remember(current) {
        mutableStateOf(if (current > 0) FxCalculator.format2(current) else "")
    }

    val a = parseToolAmount(fromRate)
    val b = parseToolAmount(toRate)
    val diff = FxCalculator.round2(b - a)
    val pct = if (a > 0) FxCalculator.round2((b - a) / a * 100.0) else 0.0
    val sign = if (pct >= 0) "+" else ""

    ToolScaffold {
        Text(
            text = "Compara dos tasas (ej. ayer vs hoy).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ToolNumberField(fromRate, { fromRate = it }, "Tasa anterior", "Bs.")
        ToolNumberField(toRate, { toRate = it }, "Tasa nueva", "Bs.")
        if (current > 0) {
            FilterChip(
                selected = false,
                onClick = {
                    fromRate = toRate
                    toRate = FxCalculator.format2(current)
                },
                label = { Text("Usar tasa BCV actual (${FxCalculator.format2(current)})") }
            )
        }
        ToolResultCard(
            title = "Variación",
            value = "$sign${FxCalculator.format2(pct)}%",
            detail = "Diferencia: $sign${FxCalculator.format2(diff)} Bs."
        )
    }
}

@Composable
fun MetaScreen(viewModel: MainViewModel = viewModel()) {
    val dashboard by viewModel.dashboard.collectAsState()
    val rates = dashboard?.bcvRates.orEmpty()
    val currencies = rates.keys.sorted().ifEmpty { listOf("USD") }

    var target by remember { mutableStateOf("100") }
    var have by remember { mutableStateOf("0") }
    var currency by remember { mutableStateOf(currencies.firstOrNull() ?: "USD") }
    val rate = rates[currency]?.rateToVes ?: 0.0
    val goal = parseToolAmount(target)
    val owned = parseToolAmount(have)
    val remaining = (goal - owned).coerceAtLeast(0.0)
    val neededBs = FxCalculator.round2(remaining * rate)
    val progress = if (goal > 0) ((owned / goal) * 100.0).coerceIn(0.0, 100.0) else 0.0

    ToolScaffold {
        Text(
            text = "Define una meta en moneda extranjera y calcula cuántos Bs. faltan.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        CurrencyChips(currencies, currency) { currency = it }
        ToolNumberField(target, { target = it }, "Meta", FxCalculator.currencySymbol(currency))
        ToolNumberField(have, { have = it }, "Ya tienes", FxCalculator.currencySymbol(currency))
        ToolResultCard(
            title = "Te faltan",
            value = "${FxCalculator.formatAmount(remaining)} ${FxCalculator.currencySymbol(currency)}",
            detail = "≈ ${FxCalculator.formatAmount(neededBs)} Bs. · avance ${FxCalculator.format2(progress)}%"
        )
    }
}

@Composable
fun DividirScreen(viewModel: MainViewModel = viewModel()) {
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
        Text(
            text = "Reparte un total entre varias personas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = inVes, onClick = { inVes = true }, label = { Text("En Bs.") })
            FilterChip(selected = !inVes, onClick = { inVes = false }, label = { Text("En USD") })
        }
        ToolNumberField(total, { total = it }, "Total", if (inVes) "Bs." else "$")
        ToolNumberField(people, { people = it.filter { c -> c.isDigit() } }, "Personas")
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
            }
        )
    }
}

@Composable
fun CompartirToolScreen(viewModel: MainViewModel = viewModel()) {
    val dashboard by viewModel.dashboard.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val rates = dashboard?.bcvRates.orEmpty()
    val currencies = rates.keys.sorted().ifEmpty { listOf("USD") }
    val context = LocalContext.current

    var amount by remember { mutableStateOf("1") }
    var currency by remember {
        mutableStateOf(
            preferences.selectedCurrency.takeIf { it in currencies } ?: currencies.first()
        )
    }
    var convertToVes by remember { mutableStateOf(true) }
    var selectedPaymentId by remember { mutableStateOf<String?>(null) }

    val rate = rates[currency]?.rateToVes ?: 0.0
    val amt = parseToolAmount(amount)
    val result = if (convertToVes) FxCalculator.round2(amt * rate)
    else if (rate > 0) FxCalculator.round2(amt / rate) else 0.0
    val foreign = if (convertToVes) amt else result
    val ves = if (convertToVes) result else amt
    val contacts = preferences.paymentContacts

    ToolScaffold {
        Text(
            text = "Arma una conversión y compártela (opcional: datos de pago).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        CurrencyChips(currencies, currency) { currency = it }
        ToolNumberField(
            amount,
            { amount = it },
            if (convertToVes) "Monto en $currency" else "Monto en Bs."
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = convertToVes,
                onClick = { convertToVes = true },
                label = { Text("A bolívares") }
            )
            FilterChip(
                selected = !convertToVes,
                onClick = { convertToVes = false },
                label = { Text("A $currency") }
            )
        }
        ToolResultCard(
            title = "Resultado",
            value = if (convertToVes) "${FxCalculator.formatAmount(result)} Bs."
            else "${FxCalculator.formatAmount(result)} $currency"
        )

        if (contacts.isNotEmpty()) {
            Text("Incluir datos de pago", fontWeight = FontWeight.SemiBold)
            FilterChip(
                selected = selectedPaymentId == null,
                onClick = { selectedPaymentId = null },
                label = { Text("Solo conversión") }
            )
            contacts.forEach { c ->
                FilterChip(
                    selected = selectedPaymentId == c.id,
                    onClick = { selectedPaymentId = c.id },
                    label = { Text("${c.displayName} · ${c.typeLabel()}") }
                )
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
                )
                val payment = contacts.firstOrNull { it.id == selectedPaymentId }
                ReceiptShareHelper.share(
                    context = context,
                    data = receipt,
                    paymentText = payment?.formatForCopy()
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Compartir")
        }
    }
}
