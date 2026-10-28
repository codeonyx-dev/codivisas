package com.codeonyx.bcvaqui.ui.donde

import androidx.compose.material.icons.filled.Close
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeonyx.bcvaqui.ui.appViewModel
import com.codeonyx.bcvaqui.data.model.WhereDestination
import com.codeonyx.bcvaqui.domain.FxCalculator
import com.codeonyx.bcvaqui.ui.MainViewModel
import com.codeonyx.bcvaqui.ui.tools.ToolChoiceFlow
import com.codeonyx.bcvaqui.ui.theme.BcvBlue
import com.codeonyx.bcvaqui.ui.theme.SurfaceLight
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DondeScreen(viewModel: MainViewModel = appViewModel()) {
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val rates = dashboard?.bcvRates.orEmpty()
    val worldRates = dashboard?.worldRatesUsd.orEmpty()

    var amountInput by remember { mutableStateOf("1") }
    var sourceCurrency by remember { mutableStateOf("USD") }
    var showCurrencyPicker by remember { mutableStateOf(false) }

    val amount = parseAmountInput(amountInput)
    val selectedCodes = preferences.dondeCurrencies
    val amountUsd = FxCalculator.toUsd(amount, sourceCurrency, rates, worldRates)

    val sourceDest = WhereDestination.byCode(sourceCurrency)
    val sourceSymbol = FxCalculator.currencySymbol(sourceCurrency)

    val selectedDestinations = remember(selectedCodes) {
        WhereDestination.ALL.filter { it.code in selectedCodes }
    }

    fun switchBaseTo(code: String) {
        if (code == sourceCurrency) return
        val converted = FxCalculator.fromUsd(amountUsd, code, rates, worldRates)
        if (converted > 0) {
            amountInput = formatInputAmount(converted)
        }
        sourceCurrency = code
        if (code !in selectedCodes) {
            viewModel.addDondeCurrency(code)
        }
    }

    val resultCodes = remember(selectedCodes, sourceCurrency) {
        val ordered = WhereDestination.ALL.map { it.code }
            .filter { it in selectedCodes || it == sourceCurrency }
        if (sourceCurrency !in ordered) listOf(sourceCurrency) + ordered else ordered
    }

    val baseOptions = selectedDestinations.ifEmpty { WhereDestination.ALL.take(4) }

    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = { viewModel.refresh(forceNetwork = true) },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Convierte a varias monedas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${resultCodes.size} monedas · toca un resultado para usarlo de base",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { showCurrencyPicker = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Elegir monedas", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                    onClick = { viewModel.refresh(forceNetwork = true) },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Escribes en",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ToolChoiceFlow(maxItemsInEachRow = 2) {
                        val orderedBaseOptions = listOfNotNull(
                            baseOptions.firstOrNull { it.code == sourceCurrency }
                        ) + baseOptions.filterNot { it.code == sourceCurrency }
                        orderedBaseOptions.distinctBy { it.code }.forEach { dest ->
                            CompactBaseChip(
                                dest = dest,
                                selected = dest.code == sourceCurrency,
                                modifier = Modifier.widthIn(min = 132.dp),
                                onClick = { switchBaseTo(dest.code) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FlagBadge(flag = sourceDest?.flag ?: "💱", size = 40.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            OutlinedTextField(
                                value = amountInput,
                                onValueChange = { raw ->
                                    amountInput = com.codeonyx.bcvaqui.domain.VeNumber.sanitize(raw)
                                        .let { cleaned ->
                                            val parts = cleaned.split('.')
                                            if (parts.size <= 1) cleaned
                                            else parts.first() + "." + parts.drop(1).joinToString("").take(2)
                                        }
                                },
                                label = { Text(sourceDest?.chipLabel ?: sourceCurrency) },
                                suffix = { Text(sourceSymbol, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                trailingIcon = if (amountInput.isNotEmpty()) { {
                                    androidx.compose.material3.IconButton(onClick = { amountInput = "" }) {
                                        androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.Close, "Borrar monto")
                                    }
                                } } else null,
                                textStyle = MaterialTheme.typography.titleMedium,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Equivalencias",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        itemsIndexed(resultCodes, key = { _, code -> code }) { index, code ->
            val dest = WhereDestination.byCode(code)
            val converted = FxCalculator.fromUsd(amountUsd, code, rates, worldRates)
            val displayAmount = if (code == sourceCurrency) amount else converted
            val isBase = code == sourceCurrency

            ResultRow(
                flag = dest?.flag ?: "💱",
                codeLabel = if (code == "VES") "Bs." else code,
                country = dest?.country ?: FxCalculator.currencyLabel(code),
                amountText = FxCalculator.formatAmount(displayAmount),
                isBase = isBase,
                onClick = { switchBaseTo(code) }
            )
        }

        item {
            Text(
                text = "Bs. con tasa BCV · otras monedas con referencia internacional",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        }

        if (!isLoading && rates.isEmpty() && worldRates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "No hay tasas disponibles",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Conéctate a internet para cargar las equivalencias.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Surface(
                            onClick = { viewModel.refresh(forceNetwork = true) },
                            shape = RoundedCornerShape(12.dp),
                            color = BcvBlue
                        ) {
                            Text(
                                "Reintentar",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCurrencyPicker) {
        CurrencyPickerSheet(
            selectedCodes = selectedCodes,
            onToggle = { viewModel.toggleDondeCurrency(it) },
            onDismiss = { showCurrencyPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyPickerSheet(
    selectedCodes: List<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Elegir monedas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Marca las que quieres ver en los resultados",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WhereDestination.ALL.forEach { dest ->
                    CurrencyCheckRow(
                        dest = dest,
                        selected = dest.code in selectedCodes,
                        onClick = { onToggle(dest.code) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Listo")
            }
        }
    }
}

@Composable
private fun FlagBadge(flag: String, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = flag,
            fontSize = (size.value * 0.45f).sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
private fun CompactBaseChip(
    dest: WhereDestination,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = if (selected) BcvBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.5.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .semantics {
                this.selected = selected
                role = Role.RadioButton
                contentDescription = "Base ${dest.chipLabel}"
            }
            .padding(horizontal = 8.dp)
    ) {
        FlagBadge(flag = dest.flag, size = 26.dp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = dest.chipLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) BcvBlue else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CurrencyCheckRow(
    dest: WhereDestination,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) BcvBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .semantics {
                this.selected = selected
                role = Role.Checkbox
                contentDescription = "${dest.country}, ${dest.chipLabel}"
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FlagBadge(flag = dest.flag, size = 40.dp)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dest.country,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = dest.chipLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (selected) BcvBlue else Color.Transparent)
                .border(
                    width = 2.dp,
                    color = if (selected) BcvBlue else MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ResultRow(
    flag: String,
    codeLabel: String,
    country: String,
    amountText: String,
    isBase: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = !isBase,
        shape = RoundedCornerShape(16.dp),
        color = if (isBase) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        shadowElevation = if (isBase) 0.dp else 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlagBadge(flag = flag, size = 42.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = country,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = codeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isBase) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Base",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BcvBlue
                            )
                        }
                    }
                }
            }
            Text(
                text = amountText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isBase) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

private fun parseAmountInput(raw: String): Double =
    com.codeonyx.bcvaqui.domain.VeNumber.parse(raw)

private fun formatInputAmount(value: Double): String {
    val rounded = FxCalculator.round2(value)
    return if (rounded % 1.0 == 0.0) {
        rounded.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", rounded)
    }
}
