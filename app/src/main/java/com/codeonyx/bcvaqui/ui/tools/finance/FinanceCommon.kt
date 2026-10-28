package com.codeonyx.bcvaqui.ui.tools.finance

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.codeonyx.bcvaqui.domain.FxCalculator
import com.codeonyx.bcvaqui.domain.Frequency
import com.codeonyx.bcvaqui.domain.Money
import com.codeonyx.bcvaqui.ui.theme.BcvBlue
import com.codeonyx.bcvaqui.ui.theme.BcvBlueDark
import com.codeonyx.bcvaqui.ui.theme.BcvGold
import com.codeonyx.bcvaqui.ui.theme.BcvIndigo
import com.codeonyx.bcvaqui.ui.theme.LocalDarkTheme
import com.codeonyx.bcvaqui.ui.tools.toolChipColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

val FinGood = Color(0xFF16A34A)
val FinBad = Color(0xFFDC2626)

private val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es", "VE"))
fun LocalDate.es(): String = format(dateFmt)
fun money(amount: Double, c: Money): String =
    if (c == Money.USD) "$ ${FxCalculator.format2(amount)}" else "Bs ${FxCalculator.format2(amount)}"

/** Encabezado navy/dorado con cifras resumen. */
@Composable
fun FinanceHero(title: String, lines: List<Pair<String, String>>, footer: String? = null) {
    Box(
        Modifier.fillMaxWidth()
            .background(Brush.linearGradient(listOf(BcvBlueDark, BcvBlue, BcvIndigo)), RoundedCornerShape(24.dp))
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(BcvGold.copy(alpha = 0.16f), radius = size.height * 0.75f, center = Offset(size.width * 0.95f, 0f))
            drawCircle(Color.White.copy(alpha = 0.06f), radius = size.height * 0.5f, center = Offset(size.width * 0.75f, size.height * 1.1f))
        }
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = BcvGold, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                lines.forEach { (label, value) ->
                    Column(Modifier.weight(1f)) {
                        Text(label, color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelSmall)
                        Text(value, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
            footer?.let { Text(it, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
fun ProgressRing(progress: Float, color: Color, size: Dp = 64.dp, stroke: Dp = 7.dp, label: String? = null) {
    val anim by animateFloatAsState(progress, tween(700), label = "ring")
    val track = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = Stroke(stroke.toPx(), cap = StrokeCap.Round)
            drawArc(track, 0f, 360f, false, style = s)
            drawArc(Brush.sweepGradient(listOf(color, BcvGold, color)), -90f, 360f * anim, false, style = s)
        }
        Text(label ?: "${(anim * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ProgressBar(progress: Float, color: Color) {
    val anim by animateFloatAsState(progress, tween(700), label = "bar")
    val track = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    Canvas(Modifier.fillMaxWidth().height(10.dp)) {
        val r = CornerRadius(size.height / 2)
        drawRoundRect(track, cornerRadius = r)
        drawRoundRect(Brush.horizontalGradient(listOf(color, BcvGold)), size = Size(size.width * anim, size.height), cornerRadius = r)
    }
}

/** Ilustración de estado vacío dibujada con formas: billetera o alcancía. */
@Composable
fun EmptyIllustration(piggy: Boolean, title: String, body: String) {
    val dark = LocalDarkTheme.current
    val navy = if (dark) BcvIndigo else BcvBlue
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(150.dp)) {
            val w = size.width
            drawCircle(navy.copy(alpha = 0.10f), radius = w * 0.48f)
            if (piggy) {
                drawOval(navy, topLeft = Offset(w * 0.18f, w * 0.32f), size = Size(w * 0.62f, w * 0.44f))
                drawCircle(navy, radius = w * 0.07f, center = Offset(w * 0.80f, w * 0.52f))
                drawRect(navy, topLeft = Offset(w * 0.28f, w * 0.70f), size = Size(w * 0.08f, w * 0.12f))
                drawRect(navy, topLeft = Offset(w * 0.58f, w * 0.70f), size = Size(w * 0.08f, w * 0.12f))
                drawRoundRect(BcvGold, topLeft = Offset(w * 0.42f, w * 0.30f), size = Size(w * 0.14f, w * 0.03f), cornerRadius = CornerRadius(4f))
                drawCircle(Color.White, radius = w * 0.025f, center = Offset(w * 0.66f, w * 0.46f))
                drawCircle(BcvGold, radius = w * 0.09f, center = Offset(w * 0.49f, w * 0.15f))
                drawCircle(Color(0xFFD97706), radius = w * 0.09f, center = Offset(w * 0.49f, w * 0.15f), style = Stroke(w * 0.015f))
            } else {
                drawRoundRect(BcvGold.copy(alpha = 0.9f), topLeft = Offset(w * 0.26f, w * 0.22f), size = Size(w * 0.42f, w * 0.26f), cornerRadius = CornerRadius(10f))
                drawRoundRect(navy, topLeft = Offset(w * 0.16f, w * 0.34f), size = Size(w * 0.68f, w * 0.42f), cornerRadius = CornerRadius(18f))
                drawRoundRect(BcvGold, topLeft = Offset(w * 0.62f, w * 0.48f), size = Size(w * 0.24f, w * 0.14f), cornerRadius = CornerRadius(10f))
                drawCircle(navy, radius = w * 0.025f, center = Offset(w * 0.70f, w * 0.55f))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
    }
}

@Composable
fun CurrencyToggle(value: Money, onChange: (Money) -> Unit) =
    com.codeonyx.bcvaqui.ui.tools.SegmentedChoice(Money.entries, value, { if (it == Money.USD) "Dólares ($)" else "Bolívares (Bs)" }, onChange)

@Composable
fun FrequencyChips(value: Frequency, onChange: (Frequency) -> Unit) =
    com.codeonyx.bcvaqui.ui.tools.SegmentedChoice(Frequency.entries, value, { it.label }, onChange)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(label: String, epochDay: Long?, onChange: (Long?) -> Unit, clearable: Boolean = false) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.CalendarMonth, null, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("$label: " + (epochDay?.let { LocalDate.ofEpochDay(it).es() } ?: "sin fecha"))
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = (epochDay ?: LocalDate.now().toEpochDay()) * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = { onChange(state.selectedDateMillis?.let { it / 86_400_000L }); open = false }) { Text("Listo") } },
            dismissButton = {
                TextButton(onClick = { if (clearable) onChange(null); open = false }) { Text(if (clearable) "Quitar" else "Cancelar") }
            }
        ) { DatePicker(state) }
    }
}

@Composable
fun ConfirmDelete(what: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Eliminar $what?") },
        text = { Text("Se borrará junto con su historial. Esta acción no se puede deshacer.") },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text("Eliminar", color = FinBad) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/** Diálogo de abono / depósito / retiro. */
@Composable
fun AmountDialog(title: String, currency: Money, suggested: Double?, onSave: (Double, String) -> Unit, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf(suggested?.let { com.codeonyx.bcvaqui.domain.VeNumber.toInput(it) } ?: "") }
    var note by remember { mutableStateOf("") }
    val parsed = com.codeonyx.bcvaqui.domain.VeNumber.parse(amount)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                com.codeonyx.bcvaqui.ui.tools.ToolNumberField(amount, { amount = it }, "Monto", suffix = if (currency == Money.USD) "$" else "Bs")
                OutlinedTextField(note, { note = it }, label = { Text("Nota (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(enabled = parsed > 0, onClick = { onSave(parsed, note.trim()); onDismiss() }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun StatusPill(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(50)) {
        Text(text, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}
