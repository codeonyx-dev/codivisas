package com.codeonyx.bcvaqui.ui.tools.finance

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codeonyx.bcvaqui.domain.*
import com.codeonyx.bcvaqui.ui.MainViewModel
import com.codeonyx.bcvaqui.ui.theme.BcvGoldDark
import com.codeonyx.bcvaqui.ui.tools.ToolNumberField
import com.codeonyx.bcvaqui.ui.tools.ToolScaffold
import com.codeonyx.bcvaqui.ui.tools.toolChipColors
import java.time.LocalDate

@Composable
fun DeudasScreen(viewModel: MainViewModel, finance: FinanceViewModel = viewModel()) {
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val rate = dashboard?.bcvRates?.get("USD")?.rateToVes ?: prefs.lastKnownUsdRate ?: 0.0
    val debts by finance.debts.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    var tab by rememberSaveable { mutableIntStateOf(0) } // 0 Debo, 1 Me deben, 2 Archivadas
    var editing by remember { mutableStateOf<Debt?>(null) }
    var creating by remember { mutableStateOf(false) }

    val active = debts.filter { !it.archived }
    fun pendingUsd(iOwe: Boolean) = active.filter { it.iOwe == iOwe }.sumOf {
        FinancePlanner.toUsd(FinancePlanner.debtStatus(it, today).remaining, it.currency, rate)
    }
    val debo = pendingUsd(true)
    val meDeben = pendingUsd(false)
    val net = meDeben - debo
    val overdueCount = active.count { FinancePlanner.debtStatus(it, today).overdue }

    ToolScaffold {
        FinanceHero(
            title = "CONTROL DE DEUDAS",
            lines = listOf("Debo" to money(debo, Money.USD), "Me deben" to money(meDeben, Money.USD), "Neto" to money(net, Money.USD)),
            footer = buildString {
                if (rate > 0) append("Neto en Bs: ${money(net * rate, Money.VES)} · BCV ${FxCalculator.format2(rate)}")
                if (overdueCount > 0) append("\n⚠ $overdueCount pago(s) vencido(s)")
            }
        )
        com.codeonyx.bcvaqui.ui.tools.SegmentedChoice(listOf(0, 1, 2), tab, { listOf("Debo", "Me deben", "Archivadas")[it] }, { tab = it })
        val shown = when (tab) {
            0 -> active.filter { it.iOwe }
            1 -> active.filter { !it.iOwe }
            else -> debts.filter { it.archived }
        }.sortedWith(compareBy({ FinancePlanner.debtStatus(it, today).settled }, { FinancePlanner.debtStatus(it, today).nextDue ?: LocalDate.MAX }))
        if (shown.isEmpty()) {
            EmptyIllustration(
                piggy = false,
                title = if (tab == 2) "Nada archivado" else if (tab == 0) "No debes nada 🎉" else "Nadie te debe",
                body = if (tab == 2) "Las deudas archivadas aparecerán aquí." else "Registra un préstamo, cuotas o una cuenta pendiente y lleva el control de cada abono."
            )
        }
        shown.forEach { d ->
            DebtCard(d, FinancePlanner.debtStatus(d, today), rate, finance, onEdit = { editing = d })
        }
        if (tab != 2) {
            Button(onClick = { creating = true }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp))
                Text(if (tab == 0) "Agregar algo que debo" else "Agregar algo que me deben")
            }
        }
        Text("Mantén presionada una deuda para editarla o eliminarla.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (creating || editing != null) {
        DebtEditor(
            initial = editing, iOweDefault = tab != 1, newId = finance::newId,
            onSave = { finance.saveDebt(it) }, onDismiss = { creating = false; editing = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DebtCard(d: Debt, s: DebtStatus, rate: Double, finance: FinanceViewModel, onEdit: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    var pay by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val accent = when { s.settled -> FinGood; s.overdue -> FinBad; d.iOwe -> BcvGoldDark; else -> MaterialTheme.colorScheme.primary }
    Surface(
        shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .combinedClickable(onClick = { expanded = !expanded }, onLongClick = { menu = true }, onLongClickLabel = "Editar o eliminar")
            .animateContentSize()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(d.person, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(if (d.iOwe) "Le debo" else "Me debe", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusPill(
                    when {
                        s.settled -> "Pagada"
                        s.overdue -> "Vencida"
                        s.daysToNext != null && s.daysToNext <= 3 -> if (s.daysToNext == 0L) "Vence hoy" else "En ${s.daysToNext} días"
                        else -> "Al día"
                    }, accent
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(money(s.remaining, d.currency), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = accent)
                Spacer(Modifier.width(8.dp))
                Text("de ${money(d.amount, d.currency)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (rate > 0) {
                val other = if (d.currency == Money.USD) money(s.remaining * rate, Money.VES) else money(s.remaining / rate, Money.USD)
                Text("≈ $other a tasa BCV", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ProgressBar(s.progress, accent)
            if (!s.settled) {
                val next = s.nextDue?.let { "Próximo pago: ${money(s.nextAmount, d.currency)} · ${it.es()}" } ?: "Sin fecha de vencimiento"
                Text(next, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = if (s.overdue) FinBad else MaterialTheme.colorScheme.onSurface)
            }
            if (s.installmentAmount != null) {
                val done = ((s.paid + 0.01) / s.installmentAmount).toInt().coerceAtMost(d.installments)
                Text("Cuotas: $done de ${d.installments} · ${money(s.installmentAmount, d.currency)} ${d.frequency.label.lowercase()}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (expanded) {
                if (d.notes.isNotBlank()) Text("📝 ${d.notes}", style = MaterialTheme.typography.bodySmall)
                Text("Historial de abonos", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                if (d.payments.isEmpty()) Text("Sin abonos todavía.", style = MaterialTheme.typography.bodySmall)
                d.payments.sortedByDescending { it.epochDay }.forEach { p ->
                    Row { Text(LocalDate.ofEpochDay(p.epochDay).es() + (if (p.note.isNotBlank()) " · ${p.note}" else ""), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Text(money(p.amount, d.currency), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!s.settled && !d.archived) {
                    FilledTonalButton(onClick = { pay = true }) { Text(if (d.iOwe) "Abonar" else "Registrar cobro") }
                    TextButton(onClick = { finance.setDebtFlags(d.id, paid = true) }) { Text("Marcar pagada") }
                } else {
                    TextButton(onClick = { finance.setDebtFlags(d.id, archived = !d.archived) }) { Text(if (d.archived) "Restaurar" else "Archivar") }
                    if (d.markedPaid && !d.archived) TextButton(onClick = { finance.setDebtFlags(d.id, paid = false) }) { Text("Reabrir") }
                }
            }
        }
    }
    if (menu) {
        AlertDialog(onDismissRequest = { menu = false }, title = { Text(d.person) },
            text = { Text("¿Qué deseas hacer con esta deuda?") },
            confirmButton = { TextButton(onClick = { menu = false; onEdit() }) { Text("Editar") } },
            dismissButton = {
                Row {
                    TextButton(onClick = { menu = false; finance.setDebtFlags(d.id, archived = !d.archived) }) { Text(if (d.archived) "Restaurar" else "Archivar") }
                    TextButton(onClick = { menu = false; confirm = true }) { Text("Eliminar", color = FinBad) }
                }
            })
    }
    if (confirm) ConfirmDelete("la deuda con ${d.person}", { finance.deleteDebt(d.id) }, { confirm = false })
    if (pay) AmountDialog(if (d.iOwe) "Abono a ${d.person}" else "Cobro de ${d.person}", d.currency, s.nextAmount,
        { a, n -> finance.addPayment(d.id, a, n) }, { pay = false })
}

@Composable
private fun DebtEditor(initial: Debt?, iOweDefault: Boolean, newId: () -> String, onSave: (Debt) -> Unit, onDismiss: () -> Unit) {
    val today = LocalDate.now().toEpochDay()
    var iOwe by remember { mutableStateOf(initial?.iOwe ?: iOweDefault) }
    var person by remember { mutableStateOf(initial?.person ?: "") }
    var amount by remember { mutableStateOf(initial?.amount?.let { VeNumber.toInput(it) } ?: "") }
    var currency by remember { mutableStateOf(initial?.currency ?: Money.USD) }
    var start by remember { mutableStateOf<Long?>(initial?.startEpochDay ?: today) }
    var due by remember { mutableStateOf(initial?.dueEpochDay) }
    var useCuotas by remember { mutableStateOf((initial?.installments ?: 0) > 0) }
    var cuotas by remember { mutableStateOf(initial?.installments?.takeIf { it > 0 }?.toString() ?: "3") }
    var freq by remember { mutableStateOf(initial?.frequency ?: Frequency.MENSUAL) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var remind by remember { mutableStateOf((initial?.remindDaysBefore ?: 1) >= 0) }
    val total = VeNumber.parse(amount)
    val n = cuotas.toIntOrNull() ?: 0
    val valid = person.isNotBlank() && total > 0 && (!useCuotas || n in 1..120)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nueva deuda" else "Editar deuda") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                com.codeonyx.bcvaqui.ui.tools.SegmentedChoice(listOf(true, false), iOwe, { if (it) "Debo" else "Me deben" }, { iOwe = it })
                OutlinedTextField(person, { person = it }, label = { Text("Persona") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                ToolNumberField(amount, { amount = it }, "Monto total", suffix = if (currency == Money.USD) "$" else "Bs")
                CurrencyToggle(currency) { currency = it }
                DateField("Inicio", start, { start = it ?: start })
                if (!useCuotas) DateField("Vence", due, { due = it }, clearable = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pagar en cuotas", Modifier.weight(1f)); Switch(useCuotas, { useCuotas = it })
                }
                if (useCuotas) {
                    OutlinedTextField(cuotas, { cuotas = it.filter(Char::isDigit).take(3) }, label = { Text("Número de cuotas") }, singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth())
                    FrequencyChips(freq) { freq = it }
                    if (total > 0 && n > 0) {
                        val last = FinancePlanner.schedule(LocalDate.ofEpochDay(start ?: today), n, freq).last()
                        Text("Cuota: ${money(FinancePlanner.installmentAmount(total, n), currency)} · última el ${last.es()}",
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Recordarme un día antes", Modifier.weight(1f)); Switch(remind, { remind = it })
                }
                OutlinedTextField(notes, { notes = it }, label = { Text("Notas") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                val s = start ?: today
                onSave(
                    (initial ?: Debt(newId(), iOwe, "", 0.0, startEpochDay = s)).copy(
                        iOwe = iOwe, person = person.trim(), amount = total, currency = currency, startEpochDay = s,
                        dueEpochDay = if (useCuotas) FinancePlanner.schedule(LocalDate.ofEpochDay(s), n, freq).last().toEpochDay() else due,
                        installments = if (useCuotas) n else 0, frequency = freq, notes = notes.trim(),
                        remindDaysBefore = if (remind) 1 else -1
                    )
                )
                onDismiss()
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
