package com.codeonyx.bcvaqui.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CopyTextsTest {
    @Test fun rateOfDay() {
        assertEquals("Tasa BCV 27/10/2026: 888,12 Bs.", CopyTexts.rateOfDay("BCV", 888.12, "2026-10-27T00:00:00-04:00"))
        assertEquals("Tasa Usdt 01/02/2026: 1.000,00 Bs.", CopyTexts.rateOfDay("Usdt", 1000.0, null, LocalDate.of(2026, 2, 1)))
        assertEquals("Tasa Euro 01/02/2026: 5,00 Bs.", CopyTexts.rateOfDay("Euro", 5.0, "basura", LocalDate.of(2026, 2, 1)))
    }

    @Test fun conversionBothWays() {
        assertEquals("5,00 $ = 4.440,60 Bs.", CopyTexts.conversion(5.0, 4440.6, "$", true))
        assertEquals("4.440,60 Bs. = 5,00 $", CopyTexts.conversion(4440.6, 5.0, "$", false))
    }

    @Test fun amountOnly() = assertEquals("4.440,60", CopyTexts.amountOnly(4440.6))
}
