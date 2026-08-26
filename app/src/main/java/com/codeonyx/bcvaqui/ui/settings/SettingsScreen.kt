package com.codeonyx.bcvaqui.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.codeonyx.bcvaqui.BuildConfig
import com.codeonyx.bcvaqui.domain.FxCalculator
import com.codeonyx.bcvaqui.ui.MainViewModel
import com.codeonyx.bcvaqui.ui.appViewModel
import com.codeonyx.bcvaqui.ui.theme.BcvBlue
import com.codeonyx.bcvaqui.ui.theme.currencyAccent
import com.codeonyx.bcvaqui.update.AppUpdateManager
import kotlinx.coroutines.launch

private val notificationOptions = listOf(
    0 to "Desactivado",
    1 to "Cada 1 hora",
    2 to "Cada 2 horas",
    4 to "Cada 4 horas",
    6 to "Cada 6 horas",
    12 to "Cada 12 horas",
    24 to "Cada 24 horas"
)

private val alertThresholdOptions = listOf(
    0.0 to "Desactivado",
    0.5 to "±0.5%",
    1.0 to "±1%",
    2.0 to "±2%",
    5.0 to "±5%"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel = appViewModel()) {
    val preferences by viewModel.preferences.collectAsState()
    val dashboard by viewModel.dashboard.collectAsState()
    val currencies = dashboard?.bcvRates?.keys?.sorted().orEmpty()

    var currencyExpanded by remember { mutableStateOf(false) }
    var intervalExpanded by remember { mutableStateOf(false) }
    var alertExpanded by remember { mutableStateOf(false) }
    var updateStatus by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Personaliza tu experiencia",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SettingsCard(title = "Moneda principal", icon = Icons.Default.Paid, iconTint = currencyAccent(preferences.selectedCurrency)) {
            Text(
                text = "Moneda que se muestra en el convertidor y en el widget.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            ExposedDropdownMenuBox(
                expanded = currencyExpanded,
                onExpandedChange = { currencyExpanded = it }
            ) {
                OutlinedTextField(
                    value = "${FxCalculator.currencyLabel(preferences.selectedCurrency)}  ·  ${preferences.selectedCurrency}",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(currencyExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BcvBlue,
                        focusedLabelColor = BcvBlue
                    )
                )
                ExposedDropdownMenu(
                    expanded = currencyExpanded,
                    onDismissRequest = { currencyExpanded = false }
                ) {
                    currencies.forEach { code ->
                        DropdownMenuItem(
                            text = { Text("${FxCalculator.currencyLabel(code)} ($code)") },
                            onClick = {
                                viewModel.setSelectedCurrency(code)
                                currencyExpanded = false
                            }
                        )
                    }
                }
            }
        }

        SettingsCard(title = "Notificaciones", icon = Icons.Default.Notifications, iconTint = BcvBlue) {
            Text(
                text = "Recibe avisos cuando cambie la tasa BCV de tu moneda.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            val intervalLabel = notificationOptions
                .firstOrNull { it.first == preferences.notificationIntervalHours }?.second
                ?: "Desactivado"
            ExposedDropdownMenuBox(
                expanded = intervalExpanded,
                onExpandedChange = { intervalExpanded = it }
            ) {
                OutlinedTextField(
                    value = intervalLabel,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(intervalExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BcvBlue,
                        focusedLabelColor = BcvBlue
                    )
                )
                ExposedDropdownMenu(
                    expanded = intervalExpanded,
                    onDismissRequest = { intervalExpanded = false }
                ) {
                    notificationOptions.forEach { (hours, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                if (hours > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                viewModel.setNotificationInterval(hours)
                                intervalExpanded = false
                            }
                        )
                    }
                }
            }
        }

        SettingsCard(
            title = "Binance P2P",
            icon = Icons.Default.CurrencyExchange,
            iconTint = Color(0xFFF0B90B)
        ) {
            Text(
                text = "Muestra la referencia USDT/VES de Binance P2P en Inicio. No es tasa oficial del BCV.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (preferences.showBinanceRate) "Activado" else "Desactivado",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Switch(
                    checked = preferences.showBinanceRate,
                    onCheckedChange = { viewModel.setShowBinanceRate(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BcvBlue
                    )
                )
            }
            if (preferences.showBinanceRate) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when {
                        dashboard?.binanceUsdtVes != null ->
                            "Ahora: ${FxCalculator.format2(dashboard?.binanceUsdtVes ?: 0.0)} Bs./USDT"
                        !dashboard?.binanceError.isNullOrBlank() ->
                            dashboard?.binanceError ?: "Error"
                        else -> "Consultando Binance…"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (dashboard?.binanceError != null) MaterialTheme.colorScheme.error else BcvBlue,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        SettingsCard(
            title = "Alerta visual (widgets)",
            icon = Icons.Default.Notifications,
            iconTint = Color(0xFF059669)
        ) {
            Text(
                text = "Si la tasa del día se mueve más que el umbral vs ayer, los widgets muestran borde verde o rojo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            val alertLabel = alertThresholdOptions
                .firstOrNull { kotlin.math.abs(it.first - preferences.alertThresholdPercent) < 0.01 }
                ?.second
                ?: "±1%"
            ExposedDropdownMenuBox(
                expanded = alertExpanded,
                onExpandedChange = { alertExpanded = it }
            ) {
                OutlinedTextField(
                    value = alertLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Umbral") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(alertExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BcvBlue,
                        focusedLabelColor = BcvBlue
                    )
                )
                ExposedDropdownMenu(
                    expanded = alertExpanded,
                    onDismissRequest = { alertExpanded = false }
                ) {
                    alertThresholdOptions.forEach { (pct, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                viewModel.setAlertThresholdPercent(pct)
                                alertExpanded = false
                            }
                        )
                    }
                }
            }
        }

        SettingsCard(title = "Información", icon = Icons.Default.Info, iconTint = MaterialTheme.colorScheme.onSurfaceVariant) {
            InfoRow("Última actualización", FxCalculator.formatUpdatedAt(dashboard?.bcvUpdatedAt))
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            InfoRow("Fuente de datos", "bcv.today")
            if (preferences.showBinanceRate) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                InfoRow("Referencia P2P", "Binance USDT/VES")
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            InfoRow("Versión", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            InfoRow("Widget", "Actualización cada 30 min")
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            InfoRow("Hecho por", "Codeonyx")
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                onClick = {
                    scope.launch {
                        updateStatus = "Buscando…"
                        val manifest = AppUpdateManager.checkForUpdate(context, notifyIfAvailable = true)
                        updateStatus = when {
                            manifest != null -> "Actualización ${manifest.versionName} disponible"
                            else -> "Ya tienes la última versión"
                        }
                        Toast.makeText(context, updateStatus, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Buscar actualizaciones", fontWeight = FontWeight.SemiBold)
            }
            updateStatus?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, MaterialTheme.shapes.large),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = iconTint.copy(alpha = 0.12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
