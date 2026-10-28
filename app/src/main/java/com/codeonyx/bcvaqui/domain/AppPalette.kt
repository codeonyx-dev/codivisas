package com.codeonyx.bcvaqui.domain

/**
 * Paletas de color de Codivisas. Valores ARGB puros (sin Android) para poder probarlos.
 * [launcherAlias] es el nombre de la activity-alias del manifest que muestra el ícono de la paleta.
 */
enum class AppPalette(
    val label: String,
    val primary: Long,
    val primaryDark: Long,
    val primaryLight: Long,
    val tertiary: Long,
    val accent: Long,
    val accentDark: Long,
    val container: Long,
    val darkPrimary: Long,
    val darkTopBar: Long,
    val darkBackground: Long,
    val darkSurface: Long,
    val launcherAlias: String,
    /** Acento en modo oscuro (más brillante); por defecto igual a [accent]. */
    val darkAccent: Long = accent
) {
    AZUL("Zafiro", 0xFF1E3A8A, 0xFF172554, 0xFF3B82F6, 0xFF6366F1, 0xFFFBBF24, 0xFFD97706, 0xFFDBEAFE,
        0xFF93C5FD, 0xFF0F1B3D, 0xFF0B1120, 0xFF111827, "LauncherAzul"),
    ONIX("Ónix", 0xFF1A1A1A, 0xFF0D0D0D, 0xFFDC2626, 0xFF404040, 0xFFB91C1C, 0xFF8A0F1F, 0xFFFEE2E2,
        0xFFFF5A6A, 0xFF161618, 0xFF0E0E10, 0xFF1A1A1E, "LauncherOnix", darkAccent = 0xFFFF5A6A),
    MORADO("Amatista", 0xFF4C1D95, 0xFF2E1065, 0xFF7C3AED, 0xFF8B5CF6, 0xFFC4B5FD, 0xFF6D28D9, 0xFFEDE9FE,
        0xFFC4B5FD, 0xFF1E0B3F, 0xFF0F0720, 0xFF1A1030, "LauncherMorado"),
    ESMERALDA("Esmeralda", 0xFF065F46, 0xFF064E3B, 0xFF10B981, 0xFF059669, 0xFFFBBF24, 0xFFD97706, 0xFFD1FAE5,
        0xFF6EE7B7, 0xFF052E24, 0xFF03140F, 0xFF0B1F19, "LauncherEsmeralda");

    companion object {
        val DEFAULT = AZUL
        /** Color de texto (blanco o casi negro) con mejor contraste sobre [bg]. */
        fun onColor(bg: Long): Long = if (contrast(0xFFFFFFFF, bg) >= contrast(0xFF0D0D0D, bg)) 0xFFFFFFFF else 0xFF0D0D0D

        fun from(id: String?): AppPalette = entries.firstOrNull { it.name == id } ?: DEFAULT

        /** Contraste WCAG entre dos colores ARGB. */
        fun contrast(a: Long, b: Long): Double {
            val la = luminance(a); val lb = luminance(b)
            return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
        }

        fun luminance(c: Long): Double {
            fun ch(v: Long): Double {
                val s = v / 255.0
                return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
            }
            return 0.2126 * ch((c shr 16) and 0xFF) + 0.7152 * ch((c shr 8) and 0xFF) + 0.0722 * ch(c and 0xFF)
        }
    }
}
