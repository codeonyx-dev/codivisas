package com.codeonyx.bcvaqui.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FxCalculatorTest {
    @Test fun format2_usesVenezuelanSeparators() {
        assertEquals("1.234,57", FxCalculator.format2(1234.567))
        assertEquals("0,00", FxCalculator.format2(0.0))
    }

    @Test fun convert_handlesZeroRates() {
        assertEquals(0.0, FxCalculator.convert(10.0, 0.0, 5.0), 0.0)
        assertEquals(20.0, FxCalculator.convert(10.0, 10.0, 5.0), 0.0)
    }

    @Test fun round2_halfUp() {
        assertEquals(1.01, FxCalculator.round2(1.005), 0.0)
    }
}

class VeNumberTest {
    @Test fun sanitize_typingDotBecomesComma() {
        assertEquals("12,5", VeNumber.sanitize("12.5"))
        assertEquals("1500,", VeNumber.sanitize("1500."))
    }

    @Test fun sanitize_pastedFormats() {
        assertEquals("1234,56", VeNumber.sanitize("Bs. 1.234,56"))
        assertEquals("1234,56", VeNumber.sanitize("1,234.56"))
        assertEquals("1500", VeNumber.sanitize("1.500"))
        assertEquals("1234567", VeNumber.sanitize("1.234.567"))
    }

    @Test fun parse_values() {
        assertEquals(1234.56, VeNumber.parse("1.234,56"), 1e-9)
        assertEquals(0.5, VeNumber.parse(",5"), 1e-9)
        assertEquals(0.0, VeNumber.parse("abc"), 0.0)
    }

    @Test fun toInput_roundTrips() {
        assertEquals("1234,5", VeNumber.toInput(1234.5))
        assertEquals(1234.5, VeNumber.parse(VeNumber.toInput(1234.5)), 1e-9)
    }
}

class PriceToolsTest {
    @Test fun taxes_addIvaAndIgtf() {
        val r = PriceTools.taxes(100.0, amountIncludesIva = false, applyIgtf = true)
        assertEquals(16.0, r.iva, 0.0)
        assertEquals(3.48, r.igtf, 0.0)
        assertEquals(119.48, r.total, 0.0)
    }

    @Test fun taxes_extractIva() {
        val r = PriceTools.taxes(116.0, amountIncludesIva = true, applyIgtf = false)
        assertEquals(100.0, r.base, 0.0)
        assertEquals(116.0, r.total, 0.0)
    }

    @Test fun gap() {
        assertEquals(10.0, PriceTools.gapPercent(100.0, 110.0)!!, 0.0)
        assertNull(PriceTools.gapPercent(null, 110.0))
    }
}

class RateAlertPolicyTest {
    @Test fun bcv_respectsThreshold() {
        assertFalse(RateAlertPolicy.shouldNotify("USD", 100.0, 100.5, 1.0))
        assertTrue(RateAlertPolicy.shouldNotify("USD", 100.0, 101.5, 1.0))
        assertTrue(RateAlertPolicy.shouldNotify("USD", 100.0, 100.2, 0.0))
    }

    @Test fun bcv_ignoresRoundingNoise() {
        assertFalse(RateAlertPolicy.shouldNotify("USD", 100.0, 100.001, 0.0))
    }

    @Test fun usdt_hasMinimumThreshold() {
        assertFalse(RateAlertPolicy.shouldNotify("USDT", 100.0, 100.5, 0.0))
        assertTrue(RateAlertPolicy.shouldNotify("USDT", 100.0, 102.0, 0.0))
    }
}

class DataAgeTest {
    @Test fun describe() {
        val now = 10_000_000_000L
        assertEquals("hace 5 min", DataAge.describe(now - 5 * 60_000L, now))
        assertEquals("hace 3 h", DataAge.describe(now - 3 * 3_600_000L, now))
        assertNull(DataAge.describe(0L, now))
    }
}
