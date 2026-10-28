package com.codeonyx.bcvaqui.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.border
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codeonyx.bcvaqui.BuildConfig
import com.codeonyx.bcvaqui.R
import com.codeonyx.bcvaqui.data.local.BackupManager
import com.codeonyx.bcvaqui.data.local.ThemeModes
import com.codeonyx.bcvaqui.domain.FxCalculator
import com.codeonyx.bcvaqui.domain.RateSources
import com.codeonyx.bcvaqui.ui.MainViewModel
import com.codeonyx.bcvaqui.ui.appViewModel
import com.codeonyx.bcvaqui.ui.theme.BcvGold
import com.codeonyx.bcvaqui.ui.tools.SegmentedChoice
import com.codeonyx.bcvaqui.update.AppUpdateManager
import kotlinx.coroutines.launch

private val notificationOptions = listOf(0 to "Desactivado", 1 to "Cada 1 hora", 2 to "Cada 2 horas",
    4 to "Cada 4 horas", 6 to "Cada 6 horas", 12 to "Cada 12 horas", 24 to "Cada 24 horas")
private val alertThresholdOptions = listOf(0.0 to "Cualquier cambio", 0.5 to "±0,5 %", 1.0 to "±1 %", 2.0 to "±2 %", 5.0 to "±5 %")
private val hours = (0..23).toList()
private fun h(x: Int) = "%02d:00".format(x)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel = appViewModel(),
    onOpenPaymentAgenda: () -> Unit = {}
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currencies = buildList { addAll(dashboard?.bcvRates?.keys?.sorted().orEmpty()); add(RateSources.BINANCE_USDT) }.distinct()
    var updateStatus by remember { mutableStateOf<String?>(null) }
    var showAbout by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val toast: (String) -> Unit = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun askPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val data = BackupManager.export(context)
                context.contentResolver.openOutputStream(uri)?.use { it.write(data.toByteArray()) }
            }.onSuccess { toast("Respaldo guardado") }.onFailure { toast("No se pudo guardar el respaldo") }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: error("vacío")
                BackupManager.import(context, raw)
            }.onSuccess { toast("Restaurado: $it") }.onFailure { toast("Archivo de respaldo no válido") }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (prefs.liteMode) Surface(shape = MaterialTheme.shapes.large, color = BcvGold.copy(alpha = 0.18f)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Estás en Modo lite", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Vista sencilla con el dólar BCV.", style = MaterialTheme.typography.bodyMedium)
                }
                Button(onClick = { viewModel.setLiteMode(false) }) { Text("Modo completo") }
            }
        }
        Section("Apariencia", Icons.Default.Palette) {
            SwitchRow("Modo lite (más sencillo)", prefs.liteMode, true) { viewModel.setLiteMode(it) }
            Hint("Solo el dólar BCV en grande y un convertidor fácil. Oculta herramientas, USDT y gráficos.")
            Label("Tema")
            SegmentedChoice(
                options = listOf(ThemeModes.SYSTEM, ThemeModes.LIGHT, ThemeModes.DARK),
                selected = prefs.themeMode,
                label = { when (it) { ThemeModes.LIGHT -> "Claro"; ThemeModes.DARK -> "Oscuro"; else -> "Sistema" } },
                onSelect = viewModel::setThemeMode
            )
            Label("Paleta de colores")
            PalettePicker(
                selected = com.codeonyx.bcvaqui.domain.AppPalette.from(prefs.palette),
                onSelect = viewModel::setPalette
            )
            Hint("Aplica a la app, widgets e ícono.")
        }

        Section("Moneda y tasas", Icons.Default.Paid) {
            DropdownRow("Moneda principal", Icons.Default.CurrencyExchange,
                "${FxCalculator.currencyLabel(prefs.selectedCurrency)} · ${prefs.selectedCurrency}",
                currencies.map { it to "${FxCalculator.currencyLabel(it)} ($it)" }) { viewModel.setSelectedCurrency(it) }
            Hint("Se usa en el convertidor y el widget. Los montos se muestran con 2 decimales, formato venezolano (1.234,56).")
        }

        Section("Notificaciones", Icons.Default.Notifications) {
            DropdownRow("Revisar tasas", Icons.Default.Schedule,
                notificationOptions.firstOrNull { it.first == prefs.notificationIntervalHours }?.second ?: "Desactivado",
                notificationOptions) { if (it > 0) askPermission(); viewModel.setNotificationInterval(it) }
            val on = prefs.notificationIntervalHours > 0
            Label("Avisarme cuando cambie")
            SwitchRow("Dólar BCV", prefs.notifyBcvUsd, on) { viewModel.setNotifySource("BCV_USD", it) }
            SwitchRow("Euro BCV", prefs.notifyBcvEur, on) { viewModel.setNotifySource("BCV_EUR", it) }
            SwitchRow("USDT Binance P2P", prefs.notifyUsdt, on) { viewModel.setNotifySource("USDT", it) }
            DropdownRow("Cambio mínimo", Icons.Default.Tune,
                alertThresholdOptions.firstOrNull { kotlin.math.abs(it.first - prefs.alertThresholdPercent) < 0.01 }?.second ?: "±1 %",
                alertThresholdOptions) { viewModel.setAlertThresholdPercent(it) }
            Hint("USDT avisa con cambios de 1 % o más. Cambiar de moneda en Inicio no genera avisos.")
            SwitchRow("Horas de silencio", prefs.quietHoursEnabled, on) {
                viewModel.setQuietHours(it, prefs.quietStartHour, prefs.quietEndHour)
            }
            if (prefs.quietHoursEnabled) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) { DropdownRow("Desde", null, h(prefs.quietStartHour), hours.map { it to h(it) }) {
                    viewModel.setQuietHours(true, it, prefs.quietEndHour) } }
                Box(Modifier.weight(1f)) { DropdownRow("Hasta", null, h(prefs.quietEndHour), hours.map { it to h(it) }) {
                    viewModel.setQuietHours(true, prefs.quietStartHour, it) } }
            }
            SwitchRow("Recordatorios de deudas", prefs.debtReminders, true) { if (it) askPermission(); viewModel.setDebtReminders(it) }
        }

        Section("Widgets", Icons.Default.Widgets) {
            InfoRow("Actualización", "Cada 30 min")
            DropdownRow("Alerta de color", Icons.Default.Contrast,
                alertThresholdOptions.firstOrNull { kotlin.math.abs(it.first - prefs.alertThresholdPercent) < 0.01 }?.second ?: "±1 %",
                alertThresholdOptions) { viewModel.setAlertThresholdPercent(it) }
            Hint("Cómo agregar: mantén presionada la pantalla de inicio › Widgets › Codivisas. Tasa (redimensionable), Brecha y Convertidor.")
        }

        Section("Métodos de pago", Icons.Default.AccountBalanceWallet) {
            ActionRow(Icons.Default.Payments, "Agenda de pagos", "${prefs.paymentContacts.size} guardados", onOpenPaymentAgenda)
        }

        Section("Datos", Icons.Default.Storage) {
            ActionRow(Icons.Default.Upload, "Exportar respaldo", "Deudas, ahorros y métodos de pago (JSON)") {
                exportLauncher.launch("codivisas-respaldo-${java.time.LocalDate.now()}.json")
            }
            ActionRow(Icons.Default.Download, "Importar respaldo", "Reemplaza los datos actuales") {
                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            }
            ActionRow(Icons.Default.DeleteSweep, "Borrar historial de calculadora", null) { confirmClear = true }
        }

        Section("Actualizaciones", Icons.Default.SystemUpdate) {
            SwitchRow("Buscar automáticamente", prefs.autoUpdateCheck, true) { viewModel.setAutoUpdateCheck(it) }
            ActionRow(Icons.Default.Refresh, "Buscar ahora", updateStatus ?: "Versión ${BuildConfig.VERSION_NAME}") {
                scope.launch {
                    updateStatus = "Buscando…"
                    val m = AppUpdateManager.checkForUpdate(context, notifyIfAvailable = true)
                    updateStatus = if (m != null) "Actualización ${m.versionName} disponible" else "Ya tienes la última versión"
                }
            }
        }

        Section("Ayuda", Icons.AutoMirrored.Filled.HelpOutline) {
            ActionRow(Icons.Default.Explore, "Ver guía de nuevo", "Recorrido por toda la app") { viewModel.replayTour() }
            ActionRow(Icons.Default.Info, "Acerca de Codivisas", "Privacidad, fuentes y créditos") { showAbout = true }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (showAbout) AboutSheet(onDismiss = { showAbout = false }, onGuide = { showAbout = false; viewModel.replayTour() })
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("Borrar historial") },
        text = { Text("Se eliminarán las operaciones guardadas de la calculadora.") },
        confirmButton = { TextButton({ viewModel.clearCalculatorHistory(); confirmClear = false; toast("Historial borrado") }) { Text("Borrar") } },
        dismissButton = { TextButton({ confirmClear = false }) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutSheet(onDismiss: () -> Unit, onGuide: () -> Unit) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                AppIconBadge()
                Spacer(Modifier.height(8.dp))
                Text("Codivisas", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Versión ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Section("Privacidad", Icons.Default.Lock) {
                Check("Sin anuncios"); Check("Sin registro ni cuenta"); Check("Sin rastreo ni analíticas: tus datos se quedan en tu teléfono")
            }
            Section("Fuentes de datos", Icons.Default.Public) {
                InfoRow("Tasas BCV", "bcv.today · dolarapi")
                InfoRow("USDT", "Binance P2P")
            }
            Section("Aviso", Icons.Default.WarningAmber) {
                Hint("Las tasas son referenciales y provienen de fuentes públicas. Verifica siempre la tasa vigente antes de pagar o cobrar.")
            }
            Section("Ayuda", Icons.AutoMirrored.Filled.HelpOutline) {
                ActionRow(Icons.Default.Explore, "Ver guía", null, onGuide)
                Hint("Widget: mantén presionada la pantalla de inicio › Widgets › Codivisas.")
            }
            Surface(
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://codeonyx.online")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                shape = MaterialTheme.shapes.large, color = BcvGold.copy(alpha = 0.14f)
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Hecho por Codeonyx", fontWeight = FontWeight.Bold)
                        Text("codeonyx.online", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, "Abrir sitio web", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Row(Modifier.padding(start = 4.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
        }
        Card(shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    }
}

@Composable private fun Label(t: String) = Text(t, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
@Composable private fun Hint(t: String) = Text(t, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
@Composable private fun Check(t: String) = Row { Text("✓  ", color = Color(0xFF10B981), fontWeight = FontWeight.Bold); Text(t, style = MaterialTheme.typography.bodyMedium) }

@Composable
private fun SwitchRow(label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(enabled = enabled) { onChange(!checked) }, verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownRow(label: String, icon: ImageVector?, value: String, options: List<Pair<T, String>>, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value, onValueChange = {}, readOnly = true, singleLine = true,
            label = { Text(label) },
            leadingIcon = icon?.let { { Icon(it, null) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(), shape = MaterialTheme.shapes.small
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (v, l) -> DropdownMenuItem(text = { Text(l) }, onClick = { onSelect(v); expanded = false }) }
        }
    }
}

/**
 * Ícono de la app dibujado con el foreground vectorial sobre el fondo de marca.
 * No cargar el mipmap ic_launcher con painterResource: es un adaptive-icon y Compose lanza
 * IllegalArgumentException ("Only VectorDrawables and rasterized asset types are supported").
 */
@Composable
private fun AppIconBadge() {
    Box(
        Modifier.size(76.dp).clip(CircleShape)
            .background(com.codeonyx.bcvaqui.ui.theme.BcvBlue),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painterResource(com.codeonyx.bcvaqui.ui.theme.PaletteManager.logoForeground(com.codeonyx.bcvaqui.ui.theme.ActivePalette.current)),
            null, Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun PalettePicker(
    selected: com.codeonyx.bcvaqui.domain.AppPalette,
    onSelect: (com.codeonyx.bcvaqui.domain.AppPalette) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        com.codeonyx.bcvaqui.domain.AppPalette.entries.forEach { p ->
            val isSel = p == selected
            Column(
                Modifier.clip(MaterialTheme.shapes.small)
                    .clickable { onSelect(p) }
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(56.dp).clip(CircleShape)
                        .background(androidx.compose.ui.graphics.Color(p.primary))
                        .then(
                            if (isSel) Modifier.border(3.dp, MaterialTheme.colorScheme.secondary, CircleShape)
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painterResource(com.codeonyx.bcvaqui.ui.theme.PaletteManager.logoForeground(p)),
                        contentDescription = "Paleta ${p.label}",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Text(
                    p.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
