package com.codeonyx.bcvaqui.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeonyx.bcvaqui.domain.AppPalette

private fun lightColorsFor(p: AppPalette) = lightColorScheme(
    primary = Color(p.primary),
    onPrimary = Color.White,
    primaryContainer = Color(p.container),
    onPrimaryContainer = Color(p.primaryDark),
    secondary = Color(p.accent),
    onSecondary = Color(AppPalette.onColor(p.accent)),
    tertiary = Color(p.tertiary),
    background = SurfaceLight,
    surface = CardElevated,
    surfaceVariant = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = OnSurfaceMuted,
    outline = DividerLight,
    outlineVariant = Color(0xFFCBD5E1)
)

private fun darkColorsFor(p: AppPalette) = darkColorScheme(
    primary = Color(p.darkPrimary),
    onPrimary = Color(p.primaryDark),
    primaryContainer = Color(p.primary),
    onPrimaryContainer = Color(p.container),
    secondary = Color(p.darkAccent),
    onSecondary = Color(AppPalette.onColor(p.darkAccent)),
    tertiary = Color(p.darkPrimary),
    background = Color(p.darkBackground),
    surface = Color(p.darkSurface),
    surfaceVariant = Color(0xFF1F2937),
    onBackground = Color(0xFFE5E7EB),
    onSurface = Color(0xFFE5E7EB),
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = Color(0xFF374151),
    outlineVariant = Color(0xFF4B5563)
)

private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontSize = 40.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 48.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineLarge = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 40.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineSmall = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 14.sp,
        letterSpacing = 0.3.sp
    )
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** true cuando el tema efectivo es oscuro (sistema o elección del usuario). */
val LocalDarkTheme = staticCompositionLocalOf { false }

/** Colores de marca que no son roles de Material 3 pero deben adaptarse al tema. */
object BrandColors {
    val heroGradient: List<Color>
        @Composable get() = if (LocalDarkTheme.current) {
            ActivePalette.current.let { listOf(Color(it.darkBackground), Color(it.darkTopBar), Color(it.primaryDark)) }
        } else {
            listOf(BcvBlueDark, BcvBlue, BcvIndigo)
        }

    /** Barra superior: azul de marca en claro, superficie elevada en oscuro. */
    val topBar: Color
        @Composable @ReadOnlyComposable get() =
            if (LocalDarkTheme.current) Color(ActivePalette.current.darkTopBar) else BcvBlue

    val onTopBar: Color
        @Composable @ReadOnlyComposable get() = Color.White

    val onTopBarMuted: Color
        @Composable @ReadOnlyComposable get() = Color.White.copy(alpha = 0.78f)
}

@Composable
fun BcvAquiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: AppPalette = AppPalette.DEFAULT,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkColorsFor(palette) else lightColorsFor(palette),
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
