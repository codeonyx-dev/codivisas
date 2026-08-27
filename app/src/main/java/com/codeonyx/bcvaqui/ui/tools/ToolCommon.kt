package com.codeonyx.bcvaqui.ui.tools

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.codeonyx.bcvaqui.domain.FxCalculator
import com.codeonyx.bcvaqui.ui.theme.BcvBlue
import com.codeonyx.bcvaqui.ui.theme.RateDown
import com.codeonyx.bcvaqui.ui.theme.RateUp
import com.codeonyx.bcvaqui.ui.theme.SurfaceLight

@Composable
fun ToolScaffold(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
fun ToolIntro(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = BcvBlue.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, BcvBlue.copy(alpha = 0.14f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = BcvBlue.copy(alpha = 0.14f)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = BcvBlue,
                    modifier = Modifier.padding(7.dp).size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "¿Qué hace esta herramienta?",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = BcvBlue
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ToolPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
fun ToolResultCard(
    title: String,
    value: String,
    detail: String? = null,
    accent: Color = BcvBlue,
    changePercent: Double? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        tonalElevation = 0.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(accent, accent.copy(alpha = 0.35f))
                        )
                    )
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (changePercent != null) {
                        ChangeBadge(changePercent)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                if (!detail.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ToolEmptyResult(
    title: String,
    hint: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun toolChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = BcvBlue.copy(alpha = 0.14f),
    selectedLabelColor = BcvBlue
)

@Composable
fun ChangeBadge(changePercent: Double) {
    val up = changePercent > 0.005
    val down = changePercent < -0.005
    val color = when {
        up -> RateUp
        down -> RateDown
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val icon: ImageVector = when {
        up -> Icons.AutoMirrored.Filled.TrendingUp
        down -> Icons.AutoMirrored.Filled.TrendingDown
        else -> Icons.AutoMirrored.Filled.TrendingUp
    }
    val sign = if (changePercent > 0) "+" else ""
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (up || down) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = "$sign${FxCalculator.format2(changePercent)}%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun ToolNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suffix: String? = null,
    helper: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            onValueChange(
                raw.replace(',', '.').filter { it.isDigit() || it == '.' }.let { cleaned ->
                    val parts = cleaned.split('.')
                    if (parts.size <= 1) cleaned
                    else parts.first() + "." + parts.drop(1).joinToString("").take(4)
                }
            )
        },
        label = { Text(label) },
        suffix = suffix?.let { { Text(it) } },
        supportingText = helper?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BcvBlue,
            focusedLabelColor = BcvBlue,
            unfocusedContainerColor = SurfaceLight,
            focusedContainerColor = Color.White
        )
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolChoiceFlow(
    maxItemsInEachRow: Int = 3,
    content: @Composable () -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = maxItemsInEachRow
    ) {
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CurrencyChips(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    val orderedOptions = listOf(selected) + options.filterNot { it == selected }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 3
    ) {
        orderedOptions.distinct().forEach { code ->
            FilterChip(
                selected = code == selected,
                onClick = { onSelect(code) },
                modifier = Modifier.widthIn(min = 96.dp),
                label = {
                    Text(
                        if (code == "VES") "Bs."
                        else FxCalculator.currencySymbol(code) + " $code",
                        fontWeight = if (code == selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = toolChipColors()
            )
        }
    }
}

fun parseToolAmount(raw: String): Double =
    raw.replace(',', '.').toDoubleOrNull() ?: 0.0
