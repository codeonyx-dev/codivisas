package com.codeonyx.bcvaqui.ui.tools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object ToolRoutes {
    const val GRAPH = "tools"
    const val HUB = "hub"
    const val DONDE = "donde"
    const val ALCANZA = "alcanza"
    const val COMISION = "comision"
    const val VARIACION = "variacion"
    const val HISTORIAL = "historial"
    const val META = "meta"
    const val DIVIDIR = "dividir"
    const val COMPARTIR = "compartir"
    const val AGENDA = "agenda"
}

data class ToolItem(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tint: Color
)

val toolCatalog = listOf(
    ToolItem(
        route = ToolRoutes.DONDE,
        title = "Dónde",
        subtitle = "Equivalencias en otros países",
        icon = Icons.Default.Public,
        tint = Color(0xFF2563EB)
    ),
    ToolItem(
        route = ToolRoutes.ALCANZA,
        title = "¿Cuánto me alcanza?",
        subtitle = "De Bs. a dólares o euros",
        icon = Icons.Default.Wallet,
        tint = Color(0xFF059669)
    ),
    ToolItem(
        route = ToolRoutes.COMISION,
        title = "Comisión / remesa",
        subtitle = "Monto con % de comisión y neto",
        icon = Icons.Default.Percent,
        tint = Color(0xFFD97706)
    ),
    ToolItem(
        route = ToolRoutes.VARIACION,
        title = "Variación %",
        subtitle = "Cambio entre dos tasas",
        icon = Icons.Default.Calculate,
        tint = Color(0xFF7C3AED)
    ),
    ToolItem(
        route = ToolRoutes.HISTORIAL,
        title = "Historial",
        subtitle = "Gráfico de la tasa BCV",
        icon = Icons.AutoMirrored.Filled.ShowChart,
        tint = Color(0xFFDC2626)
    ),
    ToolItem(
        route = ToolRoutes.META,
        title = "Meta de compra",
        subtitle = "¿Cuántos Bs. necesito?",
        icon = Icons.Default.ShoppingCart,
        tint = Color(0xFF0891B2)
    ),
    ToolItem(
        route = ToolRoutes.DIVIDIR,
        title = "Dividir cuenta",
        subtitle = "Repartir un monto entre N",
        icon = Icons.AutoMirrored.Filled.CallSplit,
        tint = Color(0xFFEA580C)
    ),
    ToolItem(
        route = ToolRoutes.COMPARTIR,
        title = "Compartir resultado",
        subtitle = "Imagen o texto de la conversión",
        icon = Icons.Default.Share,
        tint = Color(0xFF0F766E)
    ),
    ToolItem(
        route = ToolRoutes.AGENDA,
        title = "Agenda de pagos",
        subtitle = "Pago móvil, cuentas y más",
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
