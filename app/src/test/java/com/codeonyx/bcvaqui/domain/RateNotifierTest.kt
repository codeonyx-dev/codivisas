package com.codeonyx.bcvaqui.domain

import org.junit.Assert.*
import org.junit.Test

class RateNotifierTest {
    private val all = NotifySource.entries.toSet()

    @Test fun firstRun_setsBaselineWithoutNotifying() {
        val d = RateNotifier.decide(mapOf(NotifySource.BCV_USD to 100.0), emptyMap(), all, 0.0, false)
        assertTrue(d.notifications.isEmpty())
        assertEquals(100.0, d.baselines["BCV_USD"]!!, 0.0)
    }

    @Test fun eachSourceComparedAgainstItsOwnBaseline() {
        // Simula el bug: antes se comparaba USDT contra la tasa BCV al cambiar de moneda.
        val base = mapOf("BCV_USD" to 100.0, "USDT" to 150.0)
        val d = RateNotifier.decide(mapOf(NotifySource.BCV_USD to 100.0, NotifySource.USDT to 150.0), base, all, 0.0, false)
        assertTrue(d.notifications.isEmpty())
    }

    @Test fun realChange_notifiesOnce_noDuplicates() {
        val base = mapOf("BCV_USD" to 100.0)
        val d1 = RateNotifier.decide(mapOf(NotifySource.BCV_USD to 101.0), base, all, 0.0, false)
        assertEquals(1, d1.notifications.size)
        assertEquals("Dólar BCV subió 1,00 %", d1.notifications[0].title)
        val d2 = RateNotifier.decide(mapOf(NotifySource.BCV_USD to 101.0), d1.baselines, all, 0.0, false)
        assertTrue(d2.notifications.isEmpty())
    }

    @Test fun disabledSource_updatesBaselineSilently() {
        val d = RateNotifier.decide(mapOf(NotifySource.BCV_EUR to 120.0), mapOf("BCV_EUR" to 110.0), setOf(NotifySource.BCV_USD), 0.0, false)
        assertTrue(d.notifications.isEmpty())
        assertEquals(120.0, d.baselines["BCV_EUR"]!!, 0.0)
    }

    @Test fun quietHours_holdBaselineUntilLater() {
        val base = mapOf("BCV_USD" to 100.0)
        val q = RateNotifier.decide(mapOf(NotifySource.BCV_USD to 102.0), base, all, 0.0, true)
        assertTrue(q.notifications.isEmpty())
        assertEquals(100.0, q.baselines["BCV_USD"]!!, 0.0)
        assertEquals(1, RateNotifier.decide(mapOf(NotifySource.BCV_USD to 102.0), q.baselines, all, 0.0, false).notifications.size)
    }

    @Test fun usdtNeedsMinimumThreshold() {
        val d = RateNotifier.decide(mapOf(NotifySource.USDT to 100.5), mapOf("USDT" to 100.0), all, 0.0, false)
        assertTrue(d.notifications.isEmpty())
    }

    @Test fun quietHourWindow() {
        assertTrue(RateNotifier.isQuietHour(23, true, 22, 7))
        assertTrue(RateNotifier.isQuietHour(3, true, 22, 7))
        assertFalse(RateNotifier.isQuietHour(12, true, 22, 7))
        assertFalse(RateNotifier.isQuietHour(23, false, 22, 7))
        assertTrue(RateNotifier.isQuietHour(14, true, 13, 15))
    }
}
