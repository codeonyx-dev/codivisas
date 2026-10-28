package com.codeonyx.bcvaqui.ui.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codeonyx.bcvaqui.domain.DayPoint
import com.codeonyx.bcvaqui.domain.FxCalculator
import com.codeonyx.bcvaqui.domain.MarketMath
import com.codeonyx.bcvaqui.ui.MainViewModel
import com.codeonyx.bcvaqui.ui.appViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MercadoScreen(viewModel: MainViewModel = appViewModel()) {
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val market by viewModel.market.collectAsStateWithLifecycle()
    val loading by viewModel.marketLoading.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadMarket() }

    val ranges = listOf(7, 30, 90)
    var rangeIdx by remember { mutableIntStateOf(1) }
    val days = ranges[rangeIdx]

    ToolScaffold {
        ToolIntro("USDT compra/venta en Binance P2P, la próxima tasa BCV si ya fue publicada y gráficos de 7, 30 y 90 días.")

        val q = dashboard?.usdtQuote
        ToolPanel {
            Text("USDT · Binance P2P", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (q == null) {
                Text(
                    dashboard?.binanceError?.let { "No disponible ahora: $it" } ?: "No disponible ahora. Desliza en Inicio para reintentar.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Stat("Compra", q.buy)
                    Stat("Venta", q.sell)
                    Stat("Promedio", q.average)
                }
                Text(
                    "Consultado: " + SimpleDateFormat("dd/MM/yyyy h:mm a", Locale("es", "VE")).format(Date(q.atEpochMs)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        dashboard?.nextBcvUsd?.let { next ->
            ToolResultCard(
                title = "Próxima tasa BCV (${MarketMath.shortDate(next.date)})",
                value = "${FxCalculator.format2(next.rate)} Bs.",
                detail = "Ya publicada por el BCV · aplica el próximo día hábil",
                changePercent = dashboard?.bcvRates?.get("USD")?.rateToVes?.takeIf { it > 0 }?.let {
                    FxCalculator.round2((next.rate - it) / it * 100.0)
                }
            )
        }

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            ranges.forEachIndexed { i, d ->
                SegmentedButton(
                    selected = i == rangeIdx,
                    onClick = { rangeIdx = i },
                    shape = SegmentedButtonDefaults.itemShape(i, ranges.size)
                ) { Text("$d días") }
            }
        }

        if (loading && market == null) {
            CircularProgressIndicator(Modifier.padding(16.dp))
        }
        market?.let { m ->
            ChartPanel("Dólar BCV (Bs.)", MarketMath.window(m.bcv, days), Color(0xFF2563EB), suffix = " Bs.")
            ChartPanel("Paralelo / USDT (Bs.)", MarketMath.window(m.usdt, days), Color(0xFFF0B90B), suffix = " Bs.")
            ChartPanel("Brecha (%)", MarketMath.window(m.gap, days), Color(0xFFDC2626), suffix = "%")
            Text(
                "Fuentes: bcv.today y ve.dolarapi.com. Tasas solo referenciales.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: Double?) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value?.let { FxCalculator.format2(it) } ?: "—", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ChartPanel(title: String, points: List<DayPoint>, color: Color, suffix: String) {
    ToolPanel {
        val change = MarketMath.changePercent(points)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                points.lastOrNull()?.let { FxCalculator.format2(it.value) + suffix } ?: "—",
                style = MaterialTheme.typography.titleSmall
            )
        }
        if (points.size < 2) {
            Text("Sin datos suficientes para este período.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LineChart(points.map { it.value }, color)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(MarketMath.shortDate(points.first().date), style = MaterialTheme.typography.labelSmall)
                change?.let {
                    Text(
                        "${MarketMath.trendArrow(it)} ${if (it > 0) "+" else ""}${FxCalculator.format2(it)}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(MarketMath.shortDate(points.last().date), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun LineChart(values: List<Double>, color: Color) {
    val grid = MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.fillMaxWidth().height(140.dp)) {
        val min = values.min()
        val max = values.max()
        val span = (max - min).takeIf { it > 0 } ?: 1.0
        val stepX = size.width / (values.size - 1)
        for (i in 0..3) {
            val y = size.height * i / 3f
            drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            val y = (size.height * (1 - (v - min) / span)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 3.dp.toPx()))
        val fill = Path().apply {
            addPath(path)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(fill, color.copy(alpha = 0.12f))
    }
}
