package com.codeonyx.bcvaqui.domain

import com.codeonyx.bcvaqui.data.model.CurrencyRate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineRatesTest {
    // 2026-10-27 22:45 en Caracas (UTC-4) = 2026-10-28T02:45:00Z
    private val loadedAt = 1793155500000L

    private val full = RatesSnapshot(
        bcvRates = listOf(CurrencyRate("USD", 220.5, "2026-10-27"), CurrencyRate("EUR", 255.1)),
        bcvUpdatedAt = "2026-10-27",
        worldRatesUsd = mapOf("COP" to 4100.0, "EUR" to 0.92),
        worldUpdatedAt = "w1",
        usdtBuy = 300.0,
        usdtSell = 305.0,
        loadedAtMs = loadedAt
    )

    @Test fun encodeDecodeRoundTrip() {
        assertEquals(full, OfflineRates.decode(OfflineRates.encode(full)))
    }

    @Test fun decodeInvalidReturnsNull() {
        assertNull(OfflineRates.decode(null))
        assertNull(OfflineRates.decode(""))
        assertNull(OfflineRates.decode("{not json"))
    }

    @Test fun decodeIgnoresUnknownKeys() {
        val raw = """{"bcvRates":[],"loadedAtMs":5,"futureField":1}"""
        assertEquals(5L, OfflineRates.decode(raw)?.loadedAtMs)
    }

    @Test fun formatIsCaracasTime() {
        assertEquals("27/10/2026 10:45 PM", OfflineRates.formatLoadedAt(loadedAt))
        assertEquals("—", OfflineRates.formatLoadedAt(0L))
        assertEquals("—", OfflineRates.formatLoadedAt(null))
    }

    @Test fun bannerText() {
        assertEquals(
            "Modo sin conexión · Última tasa cargada: 27/10/2026 10:45 PM",
            OfflineRates.bannerText(loadedAt)
        )
        assertEquals("⚠ Sin conexión · 27/10/2026 10:45 PM", OfflineRates.widgetMarker(loadedAt))
    }

    @Test fun mergeKeepsLastGoodSourceWhenOneFails() {
        val fresh = RatesSnapshot(
            bcvRates = listOf(CurrencyRate("USD", 221.0)),
            bcvUpdatedAt = "2026-10-28",
            worldRatesUsd = emptyMap(),
            usdtBuy = null,
            usdtSell = 310.0,
            loadedAtMs = loadedAt + 1000
        )
        val m = OfflineRates.merge(full, fresh)
        assertEquals(221.0, m.bcvRates.single().rateToVes, 0.0)
        assertEquals(full.worldRatesUsd, m.worldRatesUsd)
        assertEquals("w1", m.worldUpdatedAt)
        assertEquals(300.0, m.usdtBuy!!, 0.0)
        assertEquals(310.0, m.usdtSell!!, 0.0)
        assertEquals(loadedAt + 1000, m.loadedAtMs)
    }

    @Test fun mergeWithoutPreviousIsFresh() {
        assertEquals(full, OfflineRates.merge(null, full))
    }

    @Test fun offlineState() {
        assertTrue(OfflineRates.isOffline(true, full))
        assertFalse(OfflineRates.isOffline(false, full))
        assertFalse(OfflineRates.isOffline(true, null))
        assertFalse(OfflineRates.isOffline(true, full.copy(loadedAtMs = 0)))
    }

    @Test fun ratesMapByCode() {
        assertEquals(setOf("USD", "EUR"), OfflineRates.ratesMap(full).keys)
        assertTrue(OfflineRates.ratesMap(null).isEmpty())
    }
}
