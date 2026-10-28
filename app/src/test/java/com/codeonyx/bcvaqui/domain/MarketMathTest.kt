package com.codeonyx.bcvaqui.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MarketMathTest {
    private val hist = listOf(
        DayPoint("2026-10-26", 880.0), DayPoint("2026-10-27", 887.3), DayPoint("2026-10-28", 888.12)
    )

    @Test fun nextRate_picksFirstFutureDate() {
        assertEquals(NextRate("2026-10-28", 888.12), MarketMath.nextRate(hist, "2026-10-27"))
    }

    @Test fun nextRate_nullWhenNotPublished() {
        assertNull(MarketMath.nextRate(hist, "2026-10-28"))
    }

    @Test fun window_excludesFutureAndLimits() {
        val w = MarketMath.window(hist, 7, today = "2026-10-27")
        assertEquals(listOf("2026-10-26", "2026-10-27"), w.map { it.date })
        assertEquals(1, MarketMath.window(hist, 1).size)
    }

    @Test fun gapSeries_matchesDates() {
        val gap = MarketMath.gapSeries(
            listOf(DayPoint("a", 100.0), DayPoint("b", 200.0)),
            listOf(DayPoint("b", 250.0), DayPoint("c", 300.0))
        )
        assertEquals(listOf(DayPoint("b", 25.0)), gap)
    }

    @Test fun merge_primaryWins() {
        val m = MarketMath.merge(listOf(DayPoint("b", 2.0)), listOf(DayPoint("a", 1.0), DayPoint("b", 9.0)))
        assertEquals(listOf(DayPoint("a", 1.0), DayPoint("b", 2.0)), m)
    }

    @Test fun changeAndTrend() {
        assertEquals(10.0, MarketMath.changePercent(listOf(DayPoint("a", 100.0), DayPoint("b", 110.0)))!!, 0.0)
        assertNull(MarketMath.changePercent(listOf(DayPoint("a", 1.0))))
        assertEquals(Trend.UP, MarketMath.trend(0.5))
        assertEquals(Trend.DOWN, MarketMath.trend(-0.5))
        assertEquals(Trend.FLAT, MarketMath.trend(null))
        assertEquals("▲", MarketMath.trendArrow(1.0))
    }

    @Test fun margin() {
        assertEquals(110.0, MarketMath.applyMargin(100.0, 10.0), 0.0)
        assertEquals(95.0, MarketMath.applyMargin(100.0, -5.0), 0.0)
        assertEquals(887.3, MarketMath.marginRate(887.3, 0.0), 0.0)
    }

    @Test fun usdtAverage() {
        assertEquals(1001.0, UsdtQuote(1000.0, 1002.0, 0).average!!, 0.0)
        assertEquals(1000.0, UsdtQuote(1000.0, null, 0).average!!, 0.0)
        assertNull(UsdtQuote(null, null, 0).average)
    }

    @Test fun shortDate() {
        assertEquals("28/10/2026", MarketMath.shortDate("2026-10-28"))
    }
}
