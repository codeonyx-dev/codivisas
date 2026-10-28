package com.codeonyx.bcvaqui.ui.tools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.CompareArrows
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.codeonyx.bcvaqui.R
import androidx.compose.ui.graphics.vector.ImageVector

object ToolRoutes {
    const val GRAPH = "tools"
    const val HUB = "hub"
    const val DONDE = "donde"
    const val COMISION = "comision"
    const val CALCULADORA = "calculadora"
    const val HISTORIAL = "historial"
    const val DIVIDIR = "dividir"
    const val AGENDA = "agenda"
    const val IVA_IGTF = "iva_igtf"
    const val BRECHA = "brecha"
    const val MERCADO = "mercado"
    const val COMERCIANTE = "comerciante"
    const val DEUDAS = "deudas"
    const val AHORROS = "ahorros"
}

enum class ToolCategory(@StringRes val label: Int) {
    CONVERTIR(R.string.tool_cat_convertir),
    MERCADO(R.string.tool_cat_mercado),
    PLANIFICAR(R.string.tool_cat_planificar)
}

@Immutable
data class ToolItem(
    val route: String,
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    val category: ToolCategory,
    val icon: ImageVector,
    val tint: Color
)

/** Herramientas destacadas (tarjetas grandes arriba del grid). */
val featuredTools = listOf(
    ToolItem(ToolRoutes.DEUDAS, R.string.tool_deudas_title, R.string.tool_deudas_desc, ToolCategory.PLANIFICAR, Icons.Default.Handshake, Color(0xFFFBBF24)),
    ToolItem(ToolRoutes.AHORROS, R.string.tool_ahorros_title, R.string.tool_ahorros_desc, ToolCategory.PLANIFICAR, Icons.Default.Savings, Color(0xFFFBBF24))
)

val toolCatalog = listOf(
    ToolItem(
        route = ToolRoutes.CALCULADORA,
        title = R.string.tool_calculadora_title,
        subtitle = R.string.tool_calculadora_desc,
        category = ToolCategory.CONVERTIR,
        icon = Icons.Default.Calculate,
        tint = Color(0xFF4F46E5)
    ),
    ToolItem(
        route = ToolRoutes.DONDE,
        title = R.string.tool_donde_title,
        subtitle = R.string.tool_donde_desc,
        category = ToolCategory.CONVERTIR,
        icon = Icons.Default.Public,
        tint = Color(0xFF2563EB)
    ),
    ToolItem(
        route = ToolRoutes.COMISION,
        title = R.string.tool_comision_title,
        subtitle = R.string.tool_comision_desc,
        category = ToolCategory.PLANIFICAR,
        icon = Icons.Default.Percent,
        tint = Color(0xFFD97706)
    ),
    ToolItem(
        route = ToolRoutes.IVA_IGTF,
        title = R.string.tool_iva_igtf_title,
        subtitle = R.string.tool_iva_igtf_desc,
        category = ToolCategory.CONVERTIR,
        icon = Icons.Default.ReceiptLong,
        tint = Color(0xFF059669)
    ),
    ToolItem(
        route = ToolRoutes.BRECHA,
        title = R.string.tool_brecha_title,
        subtitle = R.string.tool_brecha_desc,
        category = ToolCategory.MERCADO,
        icon = Icons.Default.CompareArrows,
        tint = Color(0xFFF0B90B)
    ),
    ToolItem(
        route = ToolRoutes.MERCADO,
        title = R.string.tool_mercado_title,
        subtitle = R.string.tool_mercado_desc,
        category = ToolCategory.MERCADO,
        icon = Icons.Default.Insights,
        tint = Color(0xFF0D9488)
    ),
    ToolItem(
        route = ToolRoutes.COMERCIANTE,
        title = R.string.tool_comerciante_title,
        subtitle = R.string.tool_comerciante_desc,
        category = ToolCategory.PLANIFICAR,
        icon = Icons.Default.Storefront,
        tint = Color(0xFF7C3AED)
    ),
    ToolItem(
        route = ToolRoutes.HISTORIAL,
        title = R.string.tool_historial_title,
        subtitle = R.string.tool_historial_desc,
        category = ToolCategory.MERCADO,
        icon = Icons.AutoMirrored.Filled.ShowChart,
        tint = Color(0xFFDC2626)
    ),
    ToolItem(
        route = ToolRoutes.DIVIDIR,
        title = R.string.tool_dividir_title,
        subtitle = R.string.tool_dividir_desc,
        category = ToolCategory.CONVERTIR,
        icon = Icons.AutoMirrored.Filled.CallSplit,
        tint = Color(0xFFEA580C)
    ),
    ToolItem(
        route = ToolRoutes.AGENDA,
        title = R.string.tool_agenda_title,
        subtitle = R.string.tool_agenda_desc,
        category = ToolCategory.PLANIFICAR,
        icon = Icons.Default.AccountBalance,
        tint = com.codeonyx.bcvaqui.ui.theme.BcvBlue
    )
)

@StringRes
fun toolTitle(route: String?): Int =
    toolCatalog.firstOrNull { it.route == route }?.title ?: R.string.tools_title

@StringRes
fun toolSubtitle(route: String?): Int =
    toolCatalog.firstOrNull { it.route == route }?.subtitle ?: R.string.tools_subtitle
