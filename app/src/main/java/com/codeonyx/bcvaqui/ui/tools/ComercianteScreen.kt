package com.codeonyx.bcvaqui.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codeonyx.bcvaqui.domain.FxCalculator
import com.codeonyx.bcvaqui.domain.MarketMath
import com.codeonyx.bcvaqui.domain.VeNumber
import com.codeonyx.bcvaqui.ui.MainViewModel
import com.codeonyx.bcvaqui.ui.appViewModel

/** Modo comerciante: precio en $ → Bs. con un % de ganancia o descuento guardado. */
@Composable
fun ComercianteScreen(viewModel: MainViewModel = appViewModel()) {
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val bcv = dashboard?.bcvRates?.get("USD")?.rateToVes ?: prefs.lastKnownUsdRate ?: 0.0
    val next = dashboard?.nextBcvUsd

    var marginInput by remember { mutableStateOf("") }
    LaunchedEffect(prefs.merchantMarginPercent) {
        if (marginInput.isEmpty() && prefs.merchantMarginPercent != 0.0) {
            marginInput = FxCalculator.format2(prefs.merchantMarginPercent)
        }
    }
    var amountInput by remember { mutableStateOf("1") }
    var discount by remember(prefs.merchantMarginPercent) { mutableStateOf(prefs.merchantMarginPercent < 0) }
    var useNext by remember { mutableStateOf(false) }

    val marginAbs = kotlin.math.abs(VeNumber.parse(marginInput))
    val margin = if (discount) -marginAbs else marginAbs
    val rate = if (useNext && next != null) next.rate else bcv
    val amount = VeNumber.parse(amountInput)
    val base = FxCalculator.round2(amount * rate)
    val withMargin = MarketMath.applyMargin(base, margin)

    ToolScaffold {
        ToolIntro("Aplica tu % de ganancia o descuento a las conversiones. El porcentaje queda guardado para la próxima vez.")
        ToolPanel {
            SegmentedChoice(listOf(false, true), discount, { if (it) "Descuento −" else "Ganancia +" }, {
                discount = it; viewModel.setMerchantMargin(if (it) -marginAbs else marginAbs)
            })
            ToolNumberField(
                value = marginInput,
                onValueChange = {
                    marginInput = it
                    val v = kotlin.math.abs(VeNumber.parse(it))
                    viewModel.setMerchantMargin(if (discount) -v else v)
                },
                label = "Porcentaje",
                suffix = "%"
            )
            ToolNumberField(value = amountInput, onValueChange = { amountInput = it }, label = "Precio en dólares", suffix = "$")
            if (next != null) {
                SegmentedChoice(listOf(false, true), useNext, { if (it) "Próxima (${MarketMath.shortDate(next.date)})" else "Tasa de hoy" }, { useNext = it })
            }
        }
        ToolResultCard(
            title = "Precio a cobrar",
            value = "${FxCalculator.format2(withMargin)} Bs.",
            detail = "Base ${FxCalculator.format2(base)} Bs. · ${if (margin >= 0) "+" else ""}${FxCalculator.format2(margin)}% · tasa ${FxCalculator.format2(rate)}"
        )
        ToolResultCard(
            title = "Tasa con margen",
            value = "${FxCalculator.format2(MarketMath.marginRate(rate, margin))} Bs./$",
            detail = "Útil para fijar precios en bolívares"
        )
        Text("Referencial: verifica siempre la tasa vigente.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Normal)
    }
}

private fun Int.dp() = androidx.compose.ui.unit.Dp(this.toFloat())
