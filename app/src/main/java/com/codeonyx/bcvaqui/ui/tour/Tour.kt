package com.codeonyx.bcvaqui.ui.tour

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codeonyx.bcvaqui.ui.theme.BcvGold

object TourKeys {
    const val RATE = "rate"; const val AMOUNT = "amount"; const val RESULT = "result"
    const val COPY = "copy"; const val SHARE = "share"; const val STATUS = "status"
    const val NAV_TOOLS = "nav_tools"; const val NAV_SETTINGS = "nav_settings"
    const val TOOLS_CONTENT = "tools_content"; const val SETTINGS_CONTENT = "settings_content"
}

/** route: "home" | "tools" | "settings" — la guía navega sola a la pantalla del paso. */
data class TourStep(val key: String?, val route: String, val emoji: String, val title: String, val body: String)

val tourSteps = listOf(
    TourStep(null, "home", "👋", "Bienvenido a Codivisas", "Te mostramos la app en menos de un minuto. Puedes saltar la guía cuando quieras."),
    TourStep(TourKeys.RATE, "home", "💱", "Elige la tasa", "Toca aquí para cambiar entre Dólar BCV, Euro BCV o USDT Binance P2P."),
    TourStep(TourKeys.AMOUNT, "home", "⌨️", "Escribe el monto", "Ingresa la cantidad y usa el botón para invertir la conversión."),
    TourStep(TourKeys.RESULT, "home", "✨", "Resultado al instante", "Tócalo para copiar el monto; mantén presionado para copiarlo con la moneda."),
    TourStep(TourKeys.COPY, "home", "📋", "Copia rápida", "Copia la tasa del día, la conversión completa o solo el monto."),
    TourStep(TourKeys.SHARE, "home", "📤", "Comparte", "Envía un comprobante con tus datos de pago por WhatsApp u otra app."),
    TourStep(TourKeys.STATUS, "home", "🕒", "Estado de la tasa", "Aquí ves cuándo se actualizó y la brecha entre BCV y USDT."),
    TourStep(TourKeys.NAV_TOOLS, "tools", "🧰", "Herramientas", "Aquí están todas las utilidades. Te mostramos cada una."),
    TourStep("tool_deudas", "tools", "🤝", "Deudas", "Lleva lo que debes y lo que te deben, con abonos, fechas y recordatorios."),
    TourStep("tool_ahorros", "tools", "🐷", "Ahorros", "Crea metas, planifica cuánto apartar y mira tu progreso."),
    TourStep("tool_calculadora", "tools", "🧮", "Calculadora", "Calcula en $ y Bs. a la vez, con historial de operaciones."),
    TourStep("tool_donde", "tools", "🌎", "Equivalencias", "Convierte bolívares a otras monedas del mundo (pesos, soles, reales…)."),
    TourStep("tool_iva_igtf", "tools", "🧾", "IVA + IGTF", "Suma el IVA y el IGTF de un pago en divisas en un toque."),
    TourStep("tool_dividir", "tools", "✂️", "Dividir gasto", "Reparte un gasto entre varias personas en $ o Bs."),
    TourStep("tool_brecha", "tools", "📊", "Brecha cambiaria", "Compara la tasa BCV con USDT o el euro y mira la diferencia en %."),
    TourStep("tool_mercado", "tools", "📈", "Mercado y gráficos", "Sigue la evolución de las tasas con gráficos."),
    TourStep("tool_historial", "tools", "🗓️", "Historial BCV", "Consulta tasas BCV de días anteriores."),
    TourStep("tool_comision", "tools", "💸", "Comisión / remesa", "Descubre cuánto llega realmente tras comisiones de envío o pago."),
    TourStep("tool_comerciante", "tools", "🏪", "Modo comerciante", "Calcula precios con ganancia o descuento a la tasa de hoy."),
    TourStep("tool_agenda", "tools", "🏦", "Agenda de pagos", "Guarda tus datos de pago móvil y cuentas para compartirlos."),
    TourStep(TourKeys.SETTINGS_CONTENT, "settings", "⚙️", "Ajustes", "Tema, moneda, notificaciones por fuente, respaldo de datos y actualizaciones."),
    TourStep(null, "home", "📱", "Widgets", "Mantén presionada la pantalla de inicio › Widgets › Codivisas para ver la tasa sin abrir la app."),
    TourStep(null, "home", "🚀", "¡Listo!", "¿Prefieres algo más sencillo? Activa el Modo lite: solo la tasa del dólar y un convertidor grande. Puedes cambiarlo en Ajustes y ver esta guía de nuevo en Ajustes › Ayuda.")
)

/** Clave del paso actual, para que las pantallas hagan scroll hasta el elemento. */
val LocalTourFocus = compositionLocalOf<String?> { null }

val LocalTourTargets = staticCompositionLocalOf<SnapshotStateMap<String, Rect>?> { null }

fun Modifier.tourTarget(key: String): Modifier = composed {
    val map = LocalTourTargets.current
    if (map == null) this else onGloballyPositioned { map[key] = it.boundsInRoot() }
}

@Composable
fun TourOverlay(
    targets: Map<String, Rect>,
    onNavigate: (String) -> Unit,
    onFinish: () -> Unit,
    onFocus: (String?) -> Unit = {},
    onLite: (() -> Unit)? = null
) {
    var index by remember { mutableIntStateOf(0) }
    val step = tourSteps[index]
    LaunchedEffect(index) { onNavigate(step.route); onFocus(step.key) }
    val rect = step.key?.let { targets[it] }
    val density = LocalDensity.current
    val pad = with(density) { 8.dp.toPx() }
    val l by animateFloatAsState(rect?.left?.minus(pad) ?: 0f, tween(350), label = "l")
    val t by animateFloatAsState(rect?.top?.minus(pad) ?: 0f, tween(350), label = "t")
    val r by animateFloatAsState(rect?.right?.plus(pad) ?: 0f, tween(350), label = "r")
    val b by animateFloatAsState(rect?.bottom?.plus(pad) ?: 0f, tween(350), label = "b")
    val alpha by animateFloatAsState(if (rect != null) 1f else 0f, tween(250), label = "a")

    BoxWithConstraints(
        Modifier.fillMaxSize()
            .clickable(remember { MutableInteractionSource() }, null) { }
    ) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            drawRect(Color(0xE6050B1A))
            if (alpha > 0f) {
                val cr = CornerRadius(18.dp.toPx())
                drawRoundRect(Color.Black, Offset(l, t), Size(r - l, b - t), cr, alpha = alpha, blendMode = BlendMode.Clear)
                drawRoundRect(BcvGold, Offset(l, t), Size(r - l, b - t), cr, alpha = alpha, style = Stroke(2.dp.toPx()))
            }
        }
        val heightPx = with(density) { maxHeight.toPx() }
        val cardOnTop = rect != null && rect.center.y > heightPx * 0.55f
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(16.dp),
            verticalArrangement = when {
                rect == null -> Arrangement.Center
                cardOnTop -> Arrangement.Top
                else -> Arrangement.Bottom
            }
        ) {
            if (cardOnTop) Spacer(Modifier.height(56.dp))
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface, shadowElevation = 12.dp) {
                Column(Modifier.padding(20.dp)) {
                    AnimatedContent(index, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) }, label = "tour") { i ->
                        val s = tourSteps[i]
                        Column {
                            Text(s.emoji, style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.height(6.dp))
                            Text(s.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(s.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        tourSteps.indices.forEach { i ->
                            Box(
                                Modifier.height(6.dp).width(if (i == index) 18.dp else 6.dp)
                                    .background(if (i <= index) BcvGold else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Text("${index + 1}/${tourSteps.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(10.dp))
                    if (index == tourSteps.lastIndex && onLite != null) {
                        OutlinedButton(onClick = onLite, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text("Usar Modo lite (más sencillo)")
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (index < tourSteps.lastIndex) TextButton(onClick = onFinish) { Text("Saltar") }
                        Spacer(Modifier.weight(1f))
                        if (index > 0) TextButton(onClick = { index-- }) { Text("Atrás") }
                        Button(onClick = { if (index == tourSteps.lastIndex) onFinish() else index++ }) {
                            Text(if (index == tourSteps.lastIndex) "Empezar" else "Siguiente")
                        }
                    }
                }
            }
        }
    }
}
