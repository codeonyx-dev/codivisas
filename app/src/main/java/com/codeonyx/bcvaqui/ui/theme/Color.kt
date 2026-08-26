package com.codeonyx.bcvaqui.ui.theme

import androidx.compose.ui.graphics.Color

val BcvBlue = Color(0xFF1E3A8A)
val BcvBlueLight = Color(0xFF3B82F6)
val BcvBlueDark = Color(0xFF172554)
val BcvIndigo = Color(0xFF6366F1)
val BcvGold = Color(0xFFFBBF24)
val BcvGoldDark = Color(0xFFD97706)
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
