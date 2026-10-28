package com.codeonyx.bcvaqui.ui.tools

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codeonyx.bcvaqui.R
import com.codeonyx.bcvaqui.domain.FxCalculator
import com.codeonyx.bcvaqui.domain.PriceTools
import com.codeonyx.bcvaqui.domain.VeNumber
import com.codeonyx.bcvaqui.ui.MainViewModel
import com.codeonyx.bcvaqui.ui.appViewModel
import com.codeonyx.bcvaqui.ui.theme.RateDown
import com.codeonyx.bcvaqui.ui.theme.RateUp

@Composable
fun IvaIgtfScreen(viewModel: MainViewModel = appViewModel()) {
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val usdRate = dashboard?.bcvRates?.get("USD")?.rateToVes ?: 0.0

    var amount by remember { mutableStateOf("100") }
    var includesIva by remember { mutableStateOf(false) }
    var applyIgtf by remember { mutableStateOf(true) }

    val r = PriceTools.taxes(
        amount = parseToolAmount(amount),
        amountIncludesIva = includesIva,
        applyIgtf = applyIgtf
    )

    ToolScaffold {
        ToolIntro(stringResource(R.string.tool_iva_intro))
        ToolPanel {
            ToolNumberField(
                amount, { amount = it },
                stringResource(R.string.tool_iva_amount), "$",
                stringResource(R.string.tool_iva_amount_helper)
            )
            Text(stringResource(R.string.tool_iva_question), fontWeight = FontWeight.SemiBold)
            ToolChoiceFlow(maxItemsInEachRow = 2) {
                FilterChip(
                    selected = !includesIva, onClick = { includesIva = false },
                    label = { Text(stringResource(R.string.tool_iva_add)) }, colors = toolChipColors()
                )
                FilterChip(
                    selected = includesIva, onClick = { includesIva = true },
                    label = { Text(stringResource(R.string.tool_iva_extract)) }, colors = toolChipColors()
                )
            }
            ToolChoiceFlow(maxItemsInEachRow = 1) {
                FilterChip(
                    selected = applyIgtf, onClick = { applyIgtf = !applyIgtf },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.tool_iva_igtf)) }, colors = toolChipColors()
                )
            }
        }
        if (r.total > 0.0) {
            val detail = buildString {
                append("Base: ${FxCalculator.format2(r.base)} · IVA 16 %: ${FxCalculator.format2(r.iva)}")
                if (applyIgtf) append(" · IGTF 3 %: ${FxCalculator.format2(r.igtf)}")
                if (usdRate > 0) append("\n≈ ${FxCalculator.format2(r.total * usdRate)} Bs. a tasa BCV")
            }
            ToolResultCard(
                title = stringResource(R.string.tool_iva_total),
                value = "$ ${FxCalculator.format2(r.total)}",
                detail = detail
            )
        } else {
            ToolEmptyResult(stringResource(R.string.tool_empty_title), stringResource(R.string.tool_empty_amount))
        }
    }
}

private enum class GapCompare { USDT, EUR, MANUAL }

@Composable
fun BrechaScreen(viewModel: MainViewModel = appViewModel()) {
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val official = dashboard?.bcvRates?.get("USD")?.rateToVes ?: 0.0
    val usdt = dashboard?.binanceUsdtVes ?: preferences.lastKnownBinanceUsdtVes ?: 0.0
    val eur = dashboard?.bcvRates?.get("EUR")?.rateToVes ?: 0.0

    var compare by rememberSaveable { mutableStateOf(GapCompare.USDT) }
    var customLabel by rememberSaveable { mutableStateOf("") }
    var officialInput by rememberSaveable(official) { mutableStateOf(VeNumber.toInput(official)) }
    val prefill = when (compare) {
        GapCompare.USDT -> usdt
        GapCompare.EUR -> eur
        GapCompare.MANUAL -> 0.0
    }
    var marketInput by rememberSaveable(compare, prefill) { mutableStateOf(VeNumber.toInput(prefill)) }

    val o = parseToolAmount(officialInput)
    val m = parseToolAmount(marketInput)
    val gap = PriceTools.gapPercent(o, m)
    val compareLabel = when (compare) {
        GapCompare.USDT -> stringResource(R.string.gap_compare_usdt)
        GapCompare.EUR -> stringResource(R.string.gap_compare_eur)
        GapCompare.MANUAL -> customLabel.ifBlank { stringResource(R.string.gap_compare_manual) }
    }

    ToolScaffold {
        ToolIntro(stringResource(R.string.tool_gap_intro))
        ToolPanel {
            Text(stringResource(R.string.gap_compare_with), style = MaterialTheme.typography.labelLarge)
            val gapLabels = GapCompare.entries.associateWith {
                stringResource(when (it) {
                    GapCompare.USDT -> R.string.gap_compare_usdt
                    GapCompare.EUR -> R.string.gap_compare_eur
                    GapCompare.MANUAL -> R.string.gap_compare_manual
                })
            }
            SegmentedChoice(GapCompare.entries, compare, { gapLabels.getValue(it) }, { compare = it })
            if (compare == GapCompare.MANUAL) {
                OutlinedTextField(
                    value = customLabel,
                    onValueChange = { customLabel = it.take(30) },
                    label = { Text(stringResource(R.string.gap_custom_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            ToolNumberField(officialInput, { officialInput = it }, stringResource(R.string.tool_gap_official), "Bs.")
            ToolNumberField(
                marketInput, { marketInput = it },
                stringResource(R.string.gap_compare_field, compareLabel), "Bs.",
                helper = if (prefill <= 0.0 && compare != GapCompare.MANUAL) stringResource(R.string.gap_no_rate) else null
            )
        }
        if (gap != null) {
            ToolResultCard(
                title = stringResource(R.string.gap_result_vs, compareLabel),
                value = "${FxCalculator.format2(gap)} %",
                detail = stringResource(
                    R.string.tool_gap_detail,
                    FxCalculator.format2(m - o),
                    FxCalculator.format2(100.0 * o / m)
                ),
                accent = if (gap >= 0) RateDown else RateUp
            )
        } else {
            ToolEmptyResult(stringResource(R.string.tool_empty_title), stringResource(R.string.tool_gap_empty))
        }
    }
}
