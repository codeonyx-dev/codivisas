package com.codeonyx.bcvaqui.ui.tools

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codeonyx.bcvaqui.ui.appViewModel
import com.codeonyx.bcvaqui.domain.FxCalculator
import com.codeonyx.bcvaqui.ui.MainViewModel
import com.codeonyx.bcvaqui.ui.theme.BcvBlue
import com.codeonyx.bcvaqui.ui.theme.RateDown
import com.codeonyx.bcvaqui.ui.theme.RateUp
import com.codeonyx.bcvaqui.ui.theme.SurfaceLight

@Composable
fun HistorialScreen(viewModel: MainViewModel = appViewModel()) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    val loading by viewModel.historyLoading.collectAsStateWithLifecycle()
    val error by viewModel.historyError.collectAsStateWithLifecycle()

    var days by remember { mutableIntStateOf(30) }
    var currency by remember { mutableStateOf("USD") }

    LaunchedEffect(days) {
        viewModel.loadHistory(days)
    }

    ToolScaffold {
        ToolIntro("Evolución de la tasa oficial BCV. Verde = subió, rojo = bajó.")

        ToolPanel {
            Text("Período", fontWeight = FontWeight.SemiBold)
            ToolChoiceFlow(maxItemsInEachRow = 3) {
                listOf(7, 30, 90).forEach { d ->
                    FilterChip(
                        selected = days == d,
                        onClick = { days = d },
                        modifier = Modifier.widthIn(min = 88.dp),
                        label = { Text("$d días", fontWeight = if (days == d) FontWeight.Bold else FontWeight.Medium) },
                        colors = toolChipColors()
                    )
                }
            }
            Text("Moneda", fontWeight = FontWeight.SemiBold)
            CurrencyChips(listOf("USD", "EUR", "CNY", "TRY", "RUB"), currency) { currency = it }
        }

        when {
            loading && history.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            error != null && history.isEmpty() -> {
                ToolErrorState(error ?: "Revisa tu conexión.", onRetry = { viewModel.loadHistory(days) })
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
                    ToolPanel { Text("Sin datos para $currency en este período.") }
                } else {
                    val first = points.first().second
                    val last = points.last().second
                    val change = FxCalculator.round2(last - first)
                    val pct = if (first > 0) FxCalculator.round2((last - first) / first * 100.0) else 0.0
                    val periodColor = when {
                        pct > 0.005 -> RateUp
                        pct < -0.005 -> RateDown
                        else -> MaterialTheme.colorScheme.primary
                    }

                    ToolResultCard(
                        title = "Última tasa $currency",
                        value = "${FxCalculator.format2(last)} Bs.",
                        detail = "${FxCalculator.format2(change)} Bs. en $days días",
                        accent = periodColor,
                        changePercent = pct
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp
                    ) {
                        RateChart(
                            values = points.map { it.second },
                            lineColor = periodColor
                        )
                    }

                    Text(
                        text = "${points.first().first} — ${points.last().first}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    ToolPanel {
                        Text(
                            text = "Últimos días",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        points.takeLast(10).asReversed().forEachIndexed { index, (date, rate) ->
                            val older = points.takeLast(10).asReversed().getOrNull(index + 1)?.second
                            val dayPct = if (older != null && older > 0) {
                                FxCalculator.round2((rate - older) / older * 100.0)
                            } else {
                                null
                            }
                            HistoryDayRow(date = date, rate = rate, dayChangePct = dayPct)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryDayRow(date: String, rate: Double, dayChangePct: Double?) {
    val up = (dayChangePct ?: 0.0) > 0.005
    val down = (dayChangePct ?: 0.0) < -0.005
    val color = when {
        up -> RateUp
        down -> RateDown
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            when {
                up -> Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = color, modifier = Modifier.size(18.dp))
                down -> Icon(Icons.AutoMirrored.Filled.TrendingDown, null, tint = color, modifier = Modifier.size(18.dp))
                else -> Text("·", color = color, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(date.ifBlank { "—" }, fontWeight = FontWeight.SemiBold)
            if (dayChangePct != null) {
                val sign = if (dayChangePct > 0) "+" else ""
                Text(
                    text = "$sign${FxCalculator.format2(dayChangePct)}% vs día anterior",
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
            }
        }
        Text(
            text = "${FxCalculator.format2(rate)} Bs.",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
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
            val fill = Path()
            values.forEachIndexed { index, value ->
                val x = index * stepX
                val y = size.height - (((value - min) / range).toFloat() * size.height)
                if (index == 0) {
                    path.moveTo(x, y)
                    fill.moveTo(x, size.height)
                    fill.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fill.lineTo(x, y)
                }
            }
            fill.lineTo((values.lastIndex) * stepX, size.height)
            fill.close()
            drawPath(
                fill,
                brush = Brush.verticalGradient(
                    listOf(lineColor.copy(alpha = 0.22f), Color.Transparent)
                )
            )
            drawPath(path, color = lineColor, style = Stroke(width = 5f))
            val lastX = (values.lastIndex) * stepX
            val lastY = size.height - (((values.last() - min) / range).toFloat() * size.height)
            drawCircle(color = lineColor, radius = 9f, center = Offset(lastX, lastY))
            drawCircle(color = Color.White, radius = 4f, center = Offset(lastX, lastY))
        }
    }
}
