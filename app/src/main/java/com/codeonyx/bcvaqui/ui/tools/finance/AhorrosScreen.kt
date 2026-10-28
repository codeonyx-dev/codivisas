package com.codeonyx.bcvaqui.ui.tools.finance

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codeonyx.bcvaqui.domain.*
import com.codeonyx.bcvaqui.ui.MainViewModel
import com.codeonyx.bcvaqui.ui.theme.BcvBlue
import com.codeonyx.bcvaqui.ui.theme.BcvGold
import com.codeonyx.bcvaqui.ui.theme.BcvIndigo
import com.codeonyx.bcvaqui.ui.theme.LocalDarkTheme
import com.codeonyx.bcvaqui.ui.tools.ToolNumberField
import com.codeonyx.bcvaqui.ui.tools.ToolScaffold
import java.time.LocalDate
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AhorrosScreen(viewModel: MainViewModel, finance: FinanceViewModel = viewModel()) {
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val rate = dashboard?.bcvRates?.get("USD")?.rateToVes ?: prefs.lastKnownUsdRate ?: 0.0
    val goals by finance.goals.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    var editing by remember { mutableStateOf<SavingsGoal?>(null) }
    var creating by remember { mutableStateOf(false) }
    var celebrate by remember { mutableStateOf<String?>(null) }

    val savedUsd = goals.sumOf { FinancePlanner.toUsd(FinancePlanner.goalStatus(it, today).saved, it.currency, rate) }
    val targetUsd = goals.sumOf { FinancePlanner.toUsd(it.target, it.currency, rate) }

    ToolScaffold {
        FinanceHero(
            title = "MIS AHORROS",
            lines = listOf("Ahorrado" to money(savedUsd, Money.USD), "En Bs" to money(savedUsd * rate, Money.VES), "Metas" to "${goals.size}"),
            footer = if (targetUsd > 0) "Llevas ${(savedUsd / targetUsd * 100).toInt()}% del total de tus metas (${money(targetUsd, Money.USD)})" else null
        )
        if (goals.isEmpty()) {
            EmptyIllustration(true, "Empieza tu primera meta", "Define cuánto quieres ahorrar y para cuándo. Te diremos cuánto apartar por semana, quincena o mes.")
        }
        goals.sortedBy { FinancePlanner.goalStatus(it, today).complete }.forEach { g ->
            GoalCard(g, FinancePlanner.goalStatus(g, today), rate, finance, onEdit = { editing = g }, onCompleted = { celebrate = g.name })
        }
        Button(onClick = { creating = true }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
            Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Nueva meta de ahorro")
        }
        Text("Mantén presionada una meta para editarla o eliminarla.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (creating || editing != null) {
        GoalEditor(editing, rate, finance::newId, { finance.saveGoal(it) }, { creating = false; editing = null })
    }
    celebrate?.let { name -> Celebration(name) { celebrate = null } }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GoalCard(g: SavingsGoal, s: GoalStatus, rate: Double, finance: FinanceViewModel, onEdit: () -> Unit, onCompleted: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    var deposit by remember { mutableStateOf<Boolean?>(null) }
    var expanded by remember { mutableStateOf(false) }
    val dark = LocalDarkTheme.current
    val accent = if (s.complete) FinGood else if (dark) BcvIndigo else BcvBlue
    Surface(
        shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .combinedClickable(onClick = { expanded = !expanded }, onLongClick = { menu = true }, onLongClickLabel = "Editar o eliminar")
            .animateContentSize()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(s.progress, accent, 72.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("${g.emoji} ${g.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${money(s.saved, g.currency)} de ${money(g.target, g.currency)}", style = MaterialTheme.typography.bodyMedium)
                    Text("Meta: ${LocalDate.ofEpochDay(g.targetEpochDay).es()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (s.complete) {
                StatusPill("🎉 ¡Meta cumplida!", FinGood)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Aparta por ${g.frequency.plural}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(money(s.requiredPerPeriod, g.currency), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = accent)
                    }
                    if (s.behindBy > 0.5) StatusPill("Atrasado ${money(s.behindBy, g.currency)}", FinBad) else StatusPill("Vas al día ✓", FinGood)
                }
                val proj = s.projectedDate
                Text(
                    if (proj == null) "Haz tu primer depósito para estimar cuándo llegarás."
                    else "A tu ritmo actual llegarías el ${proj.es()}" + if (proj.isAfter(LocalDate.ofEpochDay(g.targetEpochDay))) " (después de la meta)" else " 🚀",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (g.currency == Money.VES && rate > 0) {
                val usdNow = s.saved / rate
                val erosion = FinancePlanner.erosionPercent(g.rateAtCreation, rate)
                Text("Hoy equivale a ${money(usdNow, Money.USD)}" + (erosion?.takeIf { it > 0 }?.let { " · la meta perdió $it% de su valor en $ desde que la creaste" } ?: ""),
                    style = MaterialTheme.typography.bodySmall, color = if ((erosion ?: 0.0) > 0) FinBad else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (expanded) {
                Text("Historial", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                if (g.movements.isEmpty()) Text("Sin movimientos todavía.", style = MaterialTheme.typography.bodySmall)
                g.movements.sortedByDescending { it.epochDay }.forEach { m ->
                    Row { Text(LocalDate.ofEpochDay(m.epochDay).es() + (if (m.note.isNotBlank()) " · ${m.note}" else ""), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Text((if (m.amount >= 0) "+" else "−") + money(kotlin.math.abs(m.amount), g.currency), style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold, color = if (m.amount >= 0) FinGood else FinBad) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { deposit = true }) { Text("Depositar") }
                TextButton(onClick = { deposit = false }, enabled = s.saved > 0) { Text("Retirar") }
            }
        }
    }
    if (menu) {
        AlertDialog(onDismissRequest = { menu = false }, title = { Text("${g.emoji} ${g.name}") },
            text = { Text("¿Qué deseas hacer con esta meta?") },
            confirmButton = { TextButton(onClick = { menu = false; onEdit() }) { Text("Editar") } },
            dismissButton = { TextButton(onClick = { menu = false; confirm = true }) { Text("Eliminar", color = FinBad) } })
    }
    if (confirm) ConfirmDelete("la meta ${g.name}", { finance.deleteGoal(g.id) }, { confirm = false })
    deposit?.let { isDeposit ->
        AmountDialog(if (isDeposit) "Depositar en ${g.name}" else "Retirar de ${g.name}", g.currency,
            if (isDeposit) s.requiredPerPeriod.takeIf { it > 0 } else null,
            { a, n ->
                val signed = if (isDeposit) a else -minOf(a, s.saved)
                finance.addMovement(g.id, signed, n)
                if (isDeposit && !s.complete && s.saved + a >= g.target - 0.005) onCompleted()
            }, { deposit = null })
    }
}

private val emojis = listOf("🎯", "🏠", "🚗", "✈️", "🎓", "🎁")

@Composable
private fun GoalEditor(initial: SavingsGoal?, rate: Double, newId: () -> String, onSave: (SavingsGoal) -> Unit, onDismiss: () -> Unit) {
    val today = LocalDate.now()
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var emoji by remember { mutableStateOf(initial?.emoji ?: "🎯") }
    var target by remember { mutableStateOf(initial?.target?.let { VeNumber.toInput(it) } ?: "") }
    var currency by remember { mutableStateOf(initial?.currency ?: Money.USD) }
    var date by remember { mutableStateOf<Long?>(initial?.targetEpochDay ?: today.plusMonths(6).toEpochDay()) }
    var freq by remember { mutableStateOf(initial?.frequency ?: Frequency.MENSUAL) }
    val amount = VeNumber.parse(target)
    val valid = name.isNotBlank() && amount > 0 && date != null && date!! > today.toEpochDay()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nueva meta" else "Editar meta") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    emojis.forEach { e ->
                        val sel = emoji == e
                        Box(
                            Modifier.size(38.dp).clip(androidx.compose.foundation.shape.CircleShape)
                                .background(if (sel) BcvGold.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { emoji = e },
                            contentAlignment = Alignment.Center
                        ) { Text(e, style = MaterialTheme.typography.titleMedium) }
                    }
                }
                OutlinedTextField(name, { name = it }, label = { Text("Nombre de la meta") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                ToolNumberField(target, { target = it }, "Monto objetivo", suffix = if (currency == Money.USD) "$" else "Bs")
                CurrencyToggle(currency) { currency = it }
                DateField("Fecha meta", date, { date = it ?: date })
                Text("Quiero apartar dinero cada:", style = MaterialTheme.typography.labelLarge)
                FrequencyChips(freq) { freq = it }
                if (valid) {
                    val saved = initial?.movements?.sumOf { it.amount } ?: 0.0
                    val periods = freq.periodsBetween(today, LocalDate.ofEpochDay(date!!))
                    Text("Necesitas ${money(FxCalculator.round2((amount - saved).coerceAtLeast(0.0) / periods), currency)} por ${freq.plural} ($periods ${freq.plural}s)",
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onSave((initial ?: SavingsGoal(newId(), "", target = 0.0, createdEpochDay = today.toEpochDay(), targetEpochDay = 0,
                    rateAtCreation = rate.takeIf { it > 0 })).copy(
                    name = name.trim(), emoji = emoji, target = amount, currency = currency, targetEpochDay = date!!, frequency = freq
                ))
                onDismiss()
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/** Celebración: confeti dibujado con Canvas. */
@Composable
private fun Celebration(name: String, onDismiss: () -> Unit) {
    val t = rememberInfiniteTransition(label = "confetti")
    val phase by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "p")
    val colors = listOf(BcvGold, BcvBlue, BcvIndigo, FinGood, Color(0xFFF472B6))
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Canvas(Modifier.size(140.dp, 90.dp)) {
                repeat(28) { i ->
                    val a = i * 0.7f
                    val y = ((phase + i / 28f) % 1f) * size.height
                    val x = size.width / 2 + cos(a) * size.width * 0.45f * (0.4f + (i % 5) / 5f) + sin(phase * 6.28f + i) * 6f
                    drawCircle(colors[i % colors.size], radius = 3f + (i % 3) * 2f, center = Offset(x, y))
                }
            }
        },
        title = { Text("¡Meta cumplida! 🎉") },
        text = { Text("Completaste \"$name\". La constancia paga: ¿qué tal una nueva meta?") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("¡Genial!") } }
    )
}
