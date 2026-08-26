package com.codeonyx.bcvaqui.ui.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codeonyx.bcvaqui.domain.FxCalculator
import com.codeonyx.bcvaqui.ui.MainViewModel
import com.codeonyx.bcvaqui.ui.theme.BcvBlue
import com.codeonyx.bcvaqui.ui.theme.SurfaceLight

@Composable
fun HistorialScreen(viewModel: MainViewModel = viewModel()) {
    val history by viewModel.history.collectAsState()
    val loading by viewModel.historyLoading.collectAsState()
    val error by viewModel.historyError.collectAsState()

    var days by remember { mutableIntStateOf(30) }
    var currency by remember { mutableStateOf("USD") }

    LaunchedEffect(days) {
        viewModel.loadHistory(days)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Evolución de la tasa oficial BCV.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(7, 30, 90).forEach { d ->
                FilterChip(
                    selected = days == d,
                    onClick = { days = d },
                    label = { Text("$d días") }
                )
            }
        }

        CurrencyChips(listOf("USD", "EUR", "CNY", "TRY", "RUB"), currency) { currency = it }

        when {
            loading && history.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null && history.isEmpty() -> {
                Column {
                    Text(error ?: "", color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { viewModel.loadHistory(days) }) {
                        Text("Reintentar")
                    }
                }
            }
            else -> {
                val points = history.mapNotNull { entry ->
                    val rate = when (currency) {
                        "EUR" -> entry.EUR
                        "CNY" -> entry.CNY
                        "TRY" -> entry.TRY
                        "RUB" -> entry.RUB
                        else -> entry.USD
                    } ?: return@mapNotNull null
                    (entry.date ?: entry.effectiveDate.orEmpty()) to rate
                }

                if (points.isEmpty()) {
                    Text("Sin datos para $currency en este período.")
                } else {
                    val first = points.first().second
                    val last = points.last().second
                    val change = FxCalculator.round2(last - first)
                    val pct = if (first > 0) FxCalculator.round2((last - first) / first * 100.0) else 0.0
                    val sign = if (pct >= 0) "+" else ""

                    ToolResultCard(
                        title = "Última tasa $currency",
                        value = "${FxCalculator.format2(last)} Bs.",
                        detail = "$sign${FxCalculator.format2(pct)}% en $days días (${sign}${FxCalculator.format2(change)} Bs.)"
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 1.dp
                    ) {
                        RateChart(values = points.map { it.second }, lineColor = BcvBlue)
                    }

                    Text(
                        text = "${points.first().first} — ${points.last().first}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    points.takeLast(7).asReversed().forEach { (date, rate) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(date.ifBlank { "—" }, fontWeight = FontWeight.Medium)
                            Text("${FxCalculator.format2(rate)} Bs.")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RateChart(values: List<Double>, lineColor: Color) {
    if (values.size < 2) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Pocos datos para graficar")
        }
    } else {
        val min = values.minOrNull() ?: 0.0
        val max = values.maxOrNull() ?: 1.0
        val range = (max - min).takeIf { it > 0 } ?: 1.0

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val stepX = size.width / (values.size - 1).coerceAtLeast(1)
            val path = Path()
            values.forEachIndexed { index, value ->
                val x = index * stepX
                val y = size.height - (((value - min) / range).toFloat() * size.height)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = lineColor, style = Stroke(width = 4f))
            val lastX = (values.lastIndex) * stepX
            val lastY = size.height - (((values.last() - min) / range).toFloat() * size.height)
            drawCircle(color = lineColor, radius = 8f, center = Offset(lastX, lastY))
        }
    }
}
