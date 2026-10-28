package com.codeonyx.bcvaqui.ui.home

import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.res.stringResource
import com.codeonyx.bcvaqui.R
import com.codeonyx.bcvaqui.ui.theme.BrandColors
import com.codeonyx.bcvaqui.domain.DataAge
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShareWithPaymentSheet(
    contacts: List<PaymentContact>,
    defaultCopRate: Double?,
    onDismiss: () -> Unit,
    onShare: (List<PaymentContact>, Double?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selected = remember(contacts) {
        androidx.compose.runtime.mutableStateListOf<String>().apply {
            contacts.filter { it.isDefault }.forEach { add(it.id) }
        }
    }
    val hasCo = contacts.any { it.id in selected && it.type == com.codeonyx.bcvaqui.data.model.PaymentType.CUENTA_CO }
    var copInput by remember { mutableStateOf(com.codeonyx.bcvaqui.domain.VeNumber.toInput(defaultCopRate ?: 0.0)) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = stringResource(R.string.share_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.share_sheet_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            contacts.forEach { contact ->
                val checked = contact.id in selected
                Surface(
                    onClick = { if (checked) selected.remove(contact.id) else selected.add(contact.id) },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = if (checked) 0.16f else 0.06f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Checkbox(checked = checked, onCheckedChange = null)
                        Column(modifier = Modifier.weight(1f).padding(start = 6.dp)) {
                            Text(contact.displayName.ifBlank { contact.typeLabel() }, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${contact.typeLabel()} · ${contact.subtitle()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (contact.isDefault) {
                            Text(
                                stringResource(R.string.payment_default_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            if (hasCo) {
                Spacer(modifier = Modifier.height(8.dp))
                com.codeonyx.bcvaqui.ui.tools.ToolNumberField(
                    value = copInput,
                    onValueChange = { copInput = it },
                    label = stringResource(R.string.share_cop_rate),
                    suffix = "COP",
                    helper = stringResource(R.string.share_cop_rate_help)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.material3.Button(
                onClick = {
                    val cop = com.codeonyx.bcvaqui.domain.VeNumber.parse(copInput).takeIf { it > 0 }
                    onShare(contacts.filter { it.id in selected }, cop)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (selected.isEmpty()) stringResource(R.string.share_only_conversion)
                    else stringResource(R.string.share_with_methods, selected.size)
                )
            }
        }
    }
}

@Composable
internal fun OfflineBanner(
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
                    text = com.codeonyx.bcvaqui.domain.OfflineRates.BANNER_TITLE,
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
internal fun CurrencyBadge(
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

internal fun copyHomeText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("BCV monto", text))
}


/** Estado de error a pantalla completa cuando no hay tasas. */
@Composable
internal fun HomeErrorState(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = BrandColors.heroGradient
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
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
                    onClick = { onRetry() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Reintentar")
                }
            }
        }
    }
}

private data class OnboardingPage(val title: String, val lines: List<String>)

private val onboardingPages = listOf(
    OnboardingPage(
        "Bienvenido a Codivisas",
        listOf(
            "Tasa oficial BCV para USD, EUR y otras monedas.",
            "Referencia USDT de Binance P2P y la brecha con el BCV.",
            "Desliza hacia abajo en Inicio para actualizar."
        )
    ),
    OnboardingPage(
        "Sin anuncios, sin registro, sin rastreo",
        listOf(
            "No mostramos publicidad ni pedimos cuenta.",
            "No usamos analíticas ni rastreadores: tus datos de pago se quedan en tu teléfono.",
            "Herramientas extra (gráficos, modo comerciante, IVA…) están en la pestaña Herramientas."
        )
    ),
    OnboardingPage(
        "Tasas solo referenciales",
        listOf(
            "Las tasas provienen de fuentes públicas (BCV, ve.dolarapi.com, Binance P2P) y pueden tener retraso.",
            "Verifica siempre la tasa vigente antes de pagar o cobrar.",
            "Tip: mantén pulsado el inicio de tu teléfono → Widgets → Codivisas para agregar un widget."
        )
    )
)

@Composable
internal fun OnboardingDialog(onDone: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val current = onboardingPages[page]
    val last = page == onboardingPages.lastIndex
    AlertDialog(
        onDismissRequest = {},
        title = { Text(current.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                current.lines.forEach { Text("• $it") }
                Text(
                    "${page + 1} de ${onboardingPages.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        dismissButton = {
            if (page > 0) TextButton(onClick = { page-- }) { Text("Atrás") }
            else TextButton(onClick = onDone) { Text("Saltar") }
        },
        confirmButton = {
            Button(
                onClick = { if (last) onDone() else page++ },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(if (last) "Entendido" else "Siguiente")
            }
        }
    )
}

/** Chips de "actualizado hace X" y brecha oficial vs. USDT sobre el fondo del hero. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HomeStatusRow(lastFetchEpochMs: Long, gapPercent: Double?) {
    val age = DataAge.describe(lastFetchEpochMs)
    val stale = DataAge.isStale(lastFetchEpochMs)
    val parts = buildList {
        if (age != null) add(stringResource(R.string.home_updated_ago, age))
        if (gapPercent != null) add("Brecha ${FxCalculator.format2(gapPercent)} %")
    }
    if (parts.isEmpty()) return
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (stale) BcvGold.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = parts.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HeroChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, highlight: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (highlight) BcvGold.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = text, style = MaterialTheme.typography.labelSmall, color = Color.White)
        }
    }
}
