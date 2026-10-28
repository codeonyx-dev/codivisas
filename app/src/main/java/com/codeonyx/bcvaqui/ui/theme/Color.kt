package com.codeonyx.bcvaqui.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.codeonyx.bcvaqui.domain.AppPalette



/** Paleta activa (estado de Compose: al cambiarla se recomponen todas las pantallas). */
object ActivePalette {
    var current: AppPalette by mutableStateOf(AppPalette.DEFAULT)
}

// Los nombres históricos (BcvBlue/BcvGold…) ahora siguen la paleta elegida.
val BcvBlue: Color get() = Color(ActivePalette.current.primary)
val BcvBlueLight: Color get() = Color(ActivePalette.current.primaryLight)
val BcvBlueDark: Color get() = Color(ActivePalette.current.primaryDark)
val BcvIndigo: Color get() = Color(ActivePalette.current.tertiary)
val BcvGold: Color get() = Color(ActivePalette.current.accent)
val BcvGoldDark: Color get() = Color(ActivePalette.current.accentDark)
val SurfaceLight = Color(0xFFF8FAFC)
val CardElevated = Color(0xFFFFFFFF)
val OnSurfaceMuted = Color(0xFF64748B)
val DividerLight = Color(0xFFE2E8F0)
val SuccessGreen = Color(0xFF059669)
val DangerRed = Color(0xFFDC2626)
val RateUp = SuccessGreen
val RateDown = DangerRed

fun currencyAccent(code: String): Color = when (code) {
        "USD" -> Color(0xFF059669)
        "USDT" -> Color(0xFFF0B90B)
        "EUR" -> Color(0xFF2563EB)
    "CNY" -> Color(0xFFDC2626)
    "TRY" -> Color(0xFFD97706)
    "RUB" -> Color(0xFF7C3AED)
    "COP" -> Color(0xFFCE1126)
    "MXN" -> Color(0xFF006847)
    "BRL" -> Color(0xFF009C3B)
    "ARS" -> Color(0xFF74ACDF)
    "PEN" -> Color(0xFFD91023)
    "CLP" -> Color(0xFF0039A6)
    "DOP" -> Color(0xFF002D62)
    "GBP" -> Color(0xFFC8102E)
    "CAD" -> Color(0xFFFF0000)
    else -> BcvBlueLight
}
