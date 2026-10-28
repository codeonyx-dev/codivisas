package com.codeonyx.bcvaqui.ui.home

import com.codeonyx.bcvaqui.domain.CopyTexts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import kotlinx.coroutines.launch
import com.codeonyx.bcvaqui.domain.ShareConversion
import com.codeonyx.bcvaqui.domain.ShareFormatter
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.codeonyx.bcvaqui.R
import com.codeonyx.bcvaqui.ui.theme.BrandColors
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import com.codeonyx.bcvaqui.domain.VeNumber
import com.codeonyx.bcvaqui.domain.PriceTools
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.filled.Close
import com.codeonyx.bcvaqui.ui.theme.BcvBlueDark
import com.codeonyx.bcvaqui.ui.theme.BcvGold
import com.codeonyx.bcvaqui.ui.theme.BcvIndigo
import com.codeonyx.bcvaqui.ui.theme.currencyAccent
import com.codeonyx.bcvaqui.ui.tour.TourKeys
import com.codeonyx.bcvaqui.ui.tour.tourTarget

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(viewModel: MainViewModel = appViewModel()) {
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
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
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val snackScope = androidx.compose.runtime.rememberCoroutineScope()
    val copiedMsg = stringResource(R.string.copied)
    val copyAndNotify: (String) -> Unit = { text ->
        copyHomeText(context, text)
        snackScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar("$copiedMsg: $text", duration = androidx.compose.material3.SnackbarDuration.Short)
        }
    }
    var showShareSheet by remember { mutableStateOf(false) }
    var showCalculator by remember { mutableStateOf(false) }

    val amount = VeNumber.parse(amountInput)
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
    val usdBcv = rates["USD"]?.rateToVes
    val shareConversion = ShareConversion(
        rateLabel = ShareFormatter.rateLabelFor(selectedCurrency),
        currencyCode = selectedCurrency,
        foreignAmount = foreignAmount,
        vesAmount = vesAmount,
        usdAmount = if (selectedCurrency == "EUR" && usdBcv != null && usdBcv > 0) vesAmount / usdBcv else null
    )
    val copyRateText =
        "1 $selectedCurrency = ${FxCalculator.format2(selectedRate)} Bs. · $rateSourceLabel"
    val paymentContacts = preferences.paymentContacts
    val resultPlain = FxCalculator.format2(result)
    val resultWithUnit = if (convertToVes) "$resultPlain Bs." else "$resultPlain $selectedCurrency"

    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = { viewModel.refresh(forceNetwork = true) },
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = BrandColors.heroGradient
                )
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            if (dashboard?.isFromCache == true) {
                OfflineBanner(
                    message = com.codeonyx.bcvaqui.domain.OfflineRates.lastLoadedLine(dashboard?.loadedAtMs ?: preferences.lastFetchEpochMs),
                    onRetry = { viewModel.refresh(forceNetwork = true) }
                )
                Spacer(modifier = Modifier.height(8.dp))
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

            Spacer(modifier = Modifier.height(6.dp))

            Box(modifier = Modifier.tourTarget(TourKeys.RATE)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { currencyMenuExpanded = true },
                    shape = MaterialTheme.shapes.medium,
                    color = Color.White.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurrencyBadge(code = selectedCurrency, size = 42.dp, accent = accent)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = FxCalculator.currencyLabel(selectedCurrency),
                                style = MaterialTheme.typography.titleMedium,
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

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth().tourTarget(TourKeys.AMOUNT),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = VeNumber.sanitize(it) },
                        trailingIcon = if (amountInput.isNotEmpty()) {
                            {
                                IconButton(onClick = { amountInput = "" }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.action_clear_amount)
                                    )
                                }
                            }
                        } else {
                            null
                        },
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
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { convertToVes = !convertToVes },
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
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

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .tourTarget(TourKeys.RESULT)
                    .animateContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (convertToVes) "RECIBES" else "EQUIVALE A",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (convertToVes) {
                        "${FxCalculator.format2(animatedResult.toDouble())} Bs."
                    } else {
                        "${FxCalculator.format2(animatedResult.toDouble())} $selectedCurrency"
                    },
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 38.sp),
                    color = BcvGold,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .combinedClickable(
                            onClickLabel = "Copiar monto",
                            onLongClickLabel = "Copiar monto con moneda",
                            onClick = { copyAndNotify(CopyTexts.amountOnly(result)) },
                            onLongClick = { copyAndNotify(resultWithUnit) }
                        )
                        .padding(horizontal = 8.dp),
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

            Spacer(modifier = Modifier.height(14.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().tourTarget(TourKeys.COPY),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HomeCopyButton(stringResource(R.string.copy_rate_of_day), Modifier.weight(1f)) {
                        copyAndNotify(CopyTexts.rateOfDay(ShareFormatter.rateLabelFor(selectedCurrency), selectedRate, receiptData.updatedAt))
                    }
                    HomeCopyButton(stringResource(R.string.copy_conversion), Modifier.weight(1f)) {
                        copyAndNotify(
                            CopyTexts.conversion(amount, result, FxCalculator.currencySymbol(selectedCurrency), convertToVes)
                        )
                    }
                    HomeCopyButton(stringResource(R.string.copy_amount_only), Modifier.weight(1f)) {
                        copyAndNotify(CopyTexts.amountOnly(result))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().tourTarget(TourKeys.SHARE)) {
                    Button(
                        onClick = {
                            if (paymentContacts.isEmpty()) {
                                ReceiptShareHelper.share(
                                    context, receiptData,
                                    fullText = ShareFormatter.build(shareConversion, emptyList())
                                )
                            } else {
                                showShareSheet = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
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

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().tourTarget(TourKeys.STATUS),
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

                Spacer(modifier = Modifier.height(4.dp))
                HomeStatusRow(
                    lastFetchEpochMs = preferences.lastFetchEpochMs,
                    gapPercent = PriceTools.gapPercent(rates["USD"]?.rateToVes, binanceRate)
                )

                if (dashboard?.isFromCache != true) {
                    dashboard?.errorMessage?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = it, style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFCDD2))
                    }
                }
            }
        }

        if (!isLoading && rates.isEmpty()) {
            HomeErrorState(onRetry = { viewModel.refresh(forceNetwork = true) })
        }
        androidx.compose.material3.SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }

    if (showShareSheet) {
        ShareWithPaymentSheet(
            contacts = paymentContacts,
            defaultCopRate = dashboard?.worldRatesUsd?.get("COP"),
            onDismiss = { showShareSheet = false },
            onShare = { methods, copRate ->
                showShareSheet = false
                ReceiptShareHelper.share(
                    context = context,
                    data = receiptData,
                    fullText = ShareFormatter.build(shareConversion, methods, copRate)
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

}

@Composable
private fun HomeCopyButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 42.dp),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
    ) {
        Text(label, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, maxLines = 2)
    }
}
