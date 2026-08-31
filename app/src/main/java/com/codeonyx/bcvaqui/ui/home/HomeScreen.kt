package com.codeonyx.bcvaqui.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeonyx.bcvaqui.data.model.PaymentContact
import com.codeonyx.bcvaqui.domain.FxCalculator
import com.codeonyx.bcvaqui.domain.RateSources
import com.codeonyx.bcvaqui.ui.MainViewModel
import com.codeonyx.bcvaqui.ui.ResultSpring
import com.codeonyx.bcvaqui.ui.appViewModel
import com.codeonyx.bcvaqui.ui.share.ReceiptData
import com.codeonyx.bcvaqui.ui.share.ReceiptShareHelper
import com.codeonyx.bcvaqui.ui.theme.BcvBlue
import com.codeonyx.bcvaqui.ui.theme.BcvBlueDark
import com.codeonyx.bcvaqui.ui.theme.BcvGold
import com.codeonyx.bcvaqui.ui.theme.BcvIndigo
import com.codeonyx.bcvaqui.ui.theme.currencyAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel = appViewModel()) {
    val dashboard by viewModel.dashboard.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    val rates = dashboard?.bcvRates.orEmpty()
    val currencies = rates.keys.sorted()
    val binanceRate = dashboard?.binanceUsdtVes ?: preferences.lastKnownBinanceUsdtVes
    val selectedCurrency = when (val code = preferences.selectedCurrency) {
        RateSources.BINANCE_USDT -> RateSources.BINANCE_USDT
        else -> code.takeIf { it in currencies } ?: currencies.firstOrNull() ?: "USD"
    }
    val selectedRate = RateSources.effectiveRate(selectedCurrency, rates, binanceRate) ?: 0.0
    val rateSourceLabel = RateSources.rateSourceLabel(selectedCurrency)

    var amountInput by remember { mutableStateOf("1") }
    var convertToVes by remember { mutableStateOf(true) }
    var currencyMenuExpanded by remember { mutableStateOf(false) }
    var copyMenuExpanded by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var showCalculator by remember { mutableStateOf(false) }

    val amount = amountInput.toDoubleOrNull() ?: 0.0
    val result = if (convertToVes) {
        FxCalculator.round2(amount * selectedRate)
    } else if (selectedRate > 0) {
        FxCalculator.round2(amount / selectedRate)
    } else {
        0.0
    }
    val animatedResult by animateFloatAsState(
        targetValue = result.toFloat(),
        animationSpec = ResultSpring,
        label = "homeResult"
    )
    val swapRotation by animateFloatAsState(
        targetValue = if (convertToVes) 0f else 180f,
        animationSpec = ResultSpring,
        label = "swapIcon"
    )
    val accent = currencyAccent(selectedCurrency)

    val (foreignAmount, vesAmount) = if (convertToVes) {
        amount to result
    } else {
        result to amount
    }
    val receiptData = ReceiptData(
        currencyCode = selectedCurrency,
        rateToVes = selectedRate,
        foreignAmount = foreignAmount,
        vesAmount = vesAmount,
        updatedAt = dashboard?.bcvUpdatedAt
            .takeUnless { selectedCurrency == RateSources.BINANCE_USDT }
    )
    val copyText = ReceiptShareHelper.formatConversionLine(receiptData)
    val copyRateText =
        "1 $selectedCurrency = ${FxCalculator.format2(selectedRate)} Bs. · $rateSourceLabel"
    val paymentContacts = preferences.paymentContacts

    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = { viewModel.refresh(forceNetwork = true) },
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BcvBlueDark, BcvBlue, BcvIndigo)
                )
            )
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 8.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        )
        Box(
            modifier = Modifier
                .size(140.dp)
                .align(Alignment.BottomStart)
                .padding(bottom = 60.dp, start = 8.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.04f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            if (dashboard?.isFromCache == true) {
                OfflineBanner(
                    message = dashboard?.errorMessage
                        ?: "Sin conexión — mostrando última tasa guardada",
                    onRetry = { viewModel.refresh(forceNetwork = true) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "CONVERTIDOR",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showCalculator = true }) {
                        Icon(Icons.Default.Calculate, contentDescription = "Calculadora", tint = Color.White)
                    }
                    IconButton(onClick = { viewModel.refresh(forceNetwork = true) }, enabled = !isLoading) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { currencyMenuExpanded = true },
                    shape = MaterialTheme.shapes.medium,
                    color = Color.White.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurrencyBadge(code = selectedCurrency, size = 48.dp, accent = accent)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = FxCalculator.currencyLabel(selectedCurrency),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "1 $selectedCurrency = ${FxCalculator.format2(selectedRate)} Bs.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = rateSourceLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedCurrency == RateSources.BINANCE_USDT) {
                                    Color(0xFFF0B90B)
                                } else {
                                    Color.White.copy(alpha = 0.5f)
                                }
                            )
                        }
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Cambiar moneda",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                DropdownMenu(
                    expanded = currencyMenuExpanded,
                    onDismissRequest = { currencyMenuExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    if (binanceRate != null) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CurrencyBadge(
                                        code = RateSources.BINANCE_USDT,
                                        size = 32.dp,
                                        accent = currencyAccent(RateSources.BINANCE_USDT)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "USDT Binance P2P · USDT",
                                            fontWeight = if (selectedCurrency == RateSources.BINANCE_USDT) {
                                                FontWeight.Bold
                                            } else {
                                                FontWeight.Normal
                                            }
                                        )
                                        Text(
                                            text = "${FxCalculator.format2(binanceRate)} Bs.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            onClick = {
                                viewModel.setSelectedCurrency(RateSources.BINANCE_USDT)
                                currencyMenuExpanded = false
                            }
                        )
                    }
                    currencies.forEach { code ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CurrencyBadge(code = code, size = 32.dp, accent = currencyAccent(code))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "${FxCalculator.currencyLabel(code)} · $code",
                                            fontWeight = if (code == selectedCurrency) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = "${FxCalculator.format2(rates[code]?.rateToVes ?: 0.0)} Bs.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            onClick = {
                                viewModel.setSelectedCurrency(code)
                                currencyMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = Color.White,
                shadowElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it.filter { c -> c.isDigit() || c == '.' } },
                        label = {
                            Text(
                                if (convertToVes) "Monto en $selectedCurrency"
                                else "Monto en bolívares"
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BcvBlue,
                            focusedLabelColor = BcvBlue,
                            cursorColor = BcvBlue
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = { convertToVes = !convertToVes },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Icon(
                            Icons.Default.SwapVert,
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(swapRotation)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (convertToVes) "Convertir a bolívares" else "Convertir a $selectedCurrency",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (convertToVes) "RECIBES" else "EQUIVALE A",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (convertToVes) {
                        "${FxCalculator.format2(animatedResult.toDouble())} Bs."
                    } else {
                        "${FxCalculator.format2(animatedResult.toDouble())} $selectedCurrency"
                    },
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 44.sp),
                    color = BcvGold,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (convertToVes) {
                        "${FxCalculator.format2(amount)} ${FxCalculator.currencySymbol(selectedCurrency)} en bolívares"
                    } else {
                        "${FxCalculator.format2(amount)} Bs. en ${FxCalculator.currencyLabel(selectedCurrency)}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { copyMenuExpanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = MaterialTheme.shapes.medium,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copiar", fontWeight = FontWeight.SemiBold)
                        }
                        DropdownMenu(
                            expanded = copyMenuExpanded,
                            onDismissRequest = { copyMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Copiar tasa") },
                                onClick = {
                                    copyMenuExpanded = false
                                    copyToClipboard(context, copyRateText)
                                    Toast.makeText(context, "Tasa copiada", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Copiar conversión") },
                                onClick = {
                                    copyMenuExpanded = false
                                    copyToClipboard(context, copyText)
                                    Toast.makeText(context, "Conversión copiada", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (paymentContacts.isEmpty()) {
                                ReceiptShareHelper.share(context, receiptData)
                            } else {
                                showShareSheet = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = BcvBlueDark
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compartir", fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = FxCalculator.formatUpdatedAt(dashboard?.bcvUpdatedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                    if (dashboard?.isFromCache == true) {
                        Surface(shape = RoundedCornerShape(12.dp), color = BcvGold.copy(alpha = 0.2f)) {
                            Text(
                                text = "Caché",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = BcvGold
                            )
                        }
                    }
                }

                if (dashboard?.isFromCache != true) {
                    dashboard?.errorMessage?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = it, style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFCDD2))
                    }
                }
            }
        }

        if (!isLoading && rates.isEmpty()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(BcvBlueDark, BcvBlue, BcvIndigo)
                        )
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = Color.White,
                    shadowElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            tint = BcvBlue,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No pudimos cargar las tasas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Revisa tu conexión e inténtalo nuevamente.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.refresh(forceNetwork = true) },
                            colors = ButtonDefaults.buttonColors(containerColor = BcvBlue)
                        ) {
                            Text("Reintentar")
                        }
                    }
                }
            }
        }
    }

    if (showShareSheet) {
        ShareWithPaymentSheet(
            contacts = paymentContacts,
            onDismiss = { showShareSheet = false },
            onShare = { contact ->
                showShareSheet = false
                ReceiptShareHelper.share(
                    context = context,
                    data = receiptData,
                    paymentText = contact?.formatForCopy()
                )
            }
        )
    }

    if (showCalculator) {
        val usdBcvRate = rates["USD"]?.rateToVes ?: selectedRate
        MiniCalculatorSheet(
            usdRate = usdBcvRate,
            onDismiss = { showCalculator = false }
        )
    }

    if (!preferences.hasSeenOnboarding) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Bienvenido a SpreadVE") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("• BCV: tasa oficial para USD, EUR y otras monedas.")
                    Text("• Binance P2P: referencia de mercado para USDT/VES.")
                    Text("• Dónde: compara cuánto representa un monto en varios países.")
                    Text(
                        "Puedes cambiar tu moneda principal y administrar tus datos de pago desde Ajustes.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.completeOnboarding() },
                    colors = ButtonDefaults.buttonColors(containerColor = BcvBlue)
                ) {
                    Text("Entendido")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareWithPaymentSheet(
    contacts: List<PaymentContact>,
    onDismiss: () -> Unit,
    onShare: (PaymentContact?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Compartir conversión",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "¿Quieres incluir datos de pago móvil o cuenta?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = { onShare(null) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Solo conversión")
            }

            contacts.forEach { contact ->
                Surface(
                    onClick = { onShare(contact) },
                    shape = RoundedCornerShape(14.dp),
                    color = BcvBlue.copy(alpha = 0.06f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(contact.displayName, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${contact.typeLabel()} · ${contact.subtitle()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineBanner(
    message: String,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = Color(0xFFFBBF24)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sin conexión",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF78350F)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF92400E)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF78350F)
            ) {
                Text(
                    text = "Reintentar",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun CurrencyBadge(
    code: String,
    size: androidx.compose.ui.unit.Dp,
    accent: Color
) {
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = accent.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, accent.copy(alpha = 0.4f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = FxCalculator.currencySymbol(code),
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.35f).sp,
                color = accent
            )
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("BCV monto", text))
}
