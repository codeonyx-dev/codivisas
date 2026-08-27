package com.codeonyx.bcvaqui.ui.tools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object ToolRoutes {
    const val GRAPH = "tools"
    const val HUB = "hub"
    const val DONDE = "donde"
    const val COMISION = "comision"
    const val VARIACION = "variacion"
    const val HISTORIAL = "historial"
    const val DIVIDIR = "dividir"
    const val COMPARTIR = "compartir"
    const val AGENDA = "agenda"
}

enum class ToolCategory(val label: String) {
    CONVERTIR("Convertir y calcular"),
    MERCADO("Mercado y tasas"),
    PLANIFICAR("Planificar y compartir")
}

data class ToolItem(
    val route: String,
    val title: String,
    val subtitle: String,
    val category: ToolCategory,
    val icon: ImageVector,
    val tint: Color
)

val toolCatalog = listOf(
    ToolItem(
        route = ToolRoutes.DONDE,
        title = "Equivalencias",
        subtitle = "Compara un monto en varias monedas",
        category = ToolCategory.CONVERTIR,
        icon = Icons.Default.Public,
        tint = Color(0xFF2563EB)
    ),
    ToolItem(
        route = ToolRoutes.COMISION,
        title = "Comisión / remesa",
        subtitle = "Calcula cuánto llega y cuánto pagas",
        category = ToolCategory.PLANIFICAR,
        icon = Icons.Default.Percent,
        tint = Color(0xFFD97706)
    ),
    ToolItem(
        route = ToolRoutes.VARIACION,
        title = "Variación de tasa",
        subtitle = "Mide cuánto subió o bajó una tasa",
        category = ToolCategory.MERCADO,
        icon = Icons.Default.Calculate,
        tint = Color(0xFF7C3AED)
    ),
    ToolItem(
        route = ToolRoutes.HISTORIAL,
        title = "Historial BCV",
        subtitle = "Consulta el gráfico y las tasas recientes",
        category = ToolCategory.MERCADO,
        icon = Icons.AutoMirrored.Filled.ShowChart,
        tint = Color(0xFFDC2626)
    ),
    ToolItem(
        route = ToolRoutes.DIVIDIR,
        title = "Dividir gasto",
        subtitle = "Reparte una cuenta entre varias personas",
        category = ToolCategory.CONVERTIR,
        icon = Icons.AutoMirrored.Filled.CallSplit,
        tint = Color(0xFFEA580C)
    ),
    ToolItem(
        route = ToolRoutes.COMPARTIR,
        title = "Compartir conversión",
        subtitle = "Genera una imagen con monto y datos de pago",
        category = ToolCategory.PLANIFICAR,
        icon = Icons.Default.Share,
        tint = Color(0xFF0F766E)
    ),
    ToolItem(
        route = ToolRoutes.AGENDA,
        title = "Agenda de pagos",
        subtitle = "Guarda pago móvil y cuentas para compartir",
        category = ToolCategory.PLANIFICAR,
        icon = Icons.Default.AccountBalance,
        tint = Color(0xFF1E3A8A)
    )
)

fun toolTitle(route: String?): String =
    toolCatalog.firstOrNull { it.route == route }?.title
        ?: if (route == ToolRoutes.HUB || route == null) "Herramientas" else "Herramientas"

fun toolSubtitle(route: String?): String =
    toolCatalog.firstOrNull { it.route == route }?.subtitle
        ?: "Utilidades con tasa BCV"
