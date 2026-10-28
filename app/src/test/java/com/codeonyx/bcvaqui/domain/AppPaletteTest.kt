package com.codeonyx.bcvaqui.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPaletteTest {
    @Test fun requestedHexValues() {
        assertEquals(0xFF1E3A8A, AppPalette.AZUL.primary); assertEquals(0xFFFBBF24, AppPalette.AZUL.accent)
        assertEquals(0xFF1A1A1A, AppPalette.ONIX.primary); assertEquals(0xFFB91C1C, AppPalette.ONIX.accent)
        assertEquals(0xFF4C1D95, AppPalette.MORADO.primary)
        assertEquals(0xFF065F46, AppPalette.ESMERALDA.primary); assertEquals(0xFFFBBF24, AppPalette.ESMERALDA.accent)
    }

    @Test fun defaultAndParsing() {
        assertEquals(AppPalette.AZUL, AppPalette.from(null))
        assertEquals(AppPalette.AZUL, AppPalette.from("desconocida"))
        assertEquals(AppPalette.MORADO, AppPalette.from("MORADO"))
    }

    @Test fun uniqueAliases() {
        assertEquals(4, AppPalette.entries.map { it.launcherAlias }.toSet().size)
        assertEquals("LauncherAzul", AppPalette.DEFAULT.launcherAlias)
    }

    @Test fun whiteOnPrimaryIsAA() {
        AppPalette.entries.forEach { assertTrue(it.name, AppPalette.contrast(0xFFFFFFFF, it.primary) >= 4.5) }
    }

    @Test fun moradoAccentIsAA() {
        assertTrue(AppPalette.contrast(AppPalette.MORADO.accent, AppPalette.MORADO.primary) >= 4.5)
    }

    @Test fun gemNames() {
        assertEquals(listOf("Zafiro", "Ónix", "Amatista", "Esmeralda"), AppPalette.entries.map { it.label })
    }

    @Test fun onixRedsAreAA() {
        val p = AppPalette.ONIX
        assertTrue(AppPalette.contrast(0xFFFFFFFF, p.accent) >= 4.5)          // texto blanco sobre rojo (claro)
        assertTrue(AppPalette.contrast(p.darkAccent, p.darkBackground) >= 4.5) // rojo sobre fondo oscuro
        assertTrue(AppPalette.contrast(p.darkAccent, p.darkSurface) >= 4.5)
        assertTrue(AppPalette.contrast(p.darkPrimary, p.darkSurface) >= 4.5)
        assertTrue(AppPalette.contrast(p.accent, 0xFFFFFFFF) >= 4.5)           // rojo sobre fondo claro
    }

    @Test fun onSecondaryAlwaysAA() {
        AppPalette.entries.forEach {
            assertTrue(it.name, AppPalette.contrast(AppPalette.onColor(it.accent), it.accent) >= 4.5)
            assertTrue(it.name, AppPalette.contrast(AppPalette.onColor(it.darkAccent), it.darkAccent) >= 4.5)
        }
    }
}
