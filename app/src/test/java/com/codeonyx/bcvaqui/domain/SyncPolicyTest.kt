package com.codeonyx.bcvaqui.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZonedDateTime

class SyncPolicyTest {
    private fun at(s: String) = ZonedDateTime.parse(s)
    private val hour = 3_600_000L

    @Test fun noWorkWhenNothingNeedsIt() {
        assertNull(SyncPolicy.periodicIntervalMinutes(hasWidgets = false, notificationHours = 0))
    }

    @Test fun widgetsUseHourlyInterval() {
        assertEquals(60L, SyncPolicy.periodicIntervalMinutes(true, 0))
        assertEquals(60L, SyncPolicy.periodicIntervalMinutes(true, 4))
    }

    @Test fun notificationsOnlyUseUserInterval() {
        assertEquals(240L, SyncPolicy.periodicIntervalMinutes(false, 4))
    }

    @Test fun publishLaterToday() {
        // martes 10:30 Caracas -> 7 h
        assertEquals(7 * hour, SyncPolicy.delayToNextBcvPublishMs(at("2026-10-27T10:30-04:00[America/Caracas]")))
    }

    @Test fun publishAfterTimeGoesToNextDay() {
        assertEquals(24 * hour, SyncPolicy.delayToNextBcvPublishMs(at("2026-10-27T17:30-04:00[America/Caracas]")))
    }

    @Test fun skipsWeekend() {
        // viernes 18:30 -> lunes 17:30 = 71 h
        assertEquals(71 * hour, SyncPolicy.delayToNextBcvPublishMs(at("2026-10-30T18:30-04:00[America/Caracas]")))
    }

    @Test fun convertsFromUtc() {
        // 14:30Z = 10:30 Caracas
        assertEquals(7 * hour, SyncPolicy.delayToNextBcvPublishMs(at("2026-10-27T14:30Z")))
    }
}
