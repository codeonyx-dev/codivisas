package com.codeonyx.bcvaqui.domain

import com.codeonyx.bcvaqui.data.local.HistoryDiskCache
import com.codeonyx.bcvaqui.data.model.BcvRateResponse
import com.codeonyx.bcvaqui.data.model.DolarApiQuote
import com.codeonyx.bcvaqui.data.repository.BcvFallback
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class BcvFallbackTest {
    @Test fun mapsDolarApi() {
        val r = BcvFallback.toBcvResponse(
            DolarApiQuote("USD", "oficial", 887.302, "2026-10-27T00:00:00-04:00"),
            DolarApiQuote("EUR", "oficial", 994.03, "2026-10-27T00:00:00-04:00")
        )!!
        assertEquals(887.302, r.USD!!, 1e-9)
        assertEquals(994.03, r.EUR!!, 1e-9)
        assertEquals("2026-10-27", r.effectiveDate)
    }

    @Test fun missingUsdIsNull() {
        assertNull(BcvFallback.toBcvResponse(DolarApiQuote(promedio = null), null))
        assertNull(BcvFallback.toBcvResponse(null, null))
    }

    @Test fun eurOptional() {
        assertNull(BcvFallback.toBcvResponse(DolarApiQuote(promedio = 10.0), null)!!.EUR)
    }
}

class HistoryDiskCacheTest {
    @Test fun roundTripAndLimit() = runBlocking<Unit> {
        val f = File.createTempFile("hist", ".json").apply { delete() }
        val cache = HistoryDiskCache(f)
        assertEquals(emptyList<BcvRateResponse>(), cache.read())
        val entries = (1..450).map { BcvRateResponse(USD = it.toDouble(), date = "d$it") }
        cache.write(entries)
        val read = cache.read()
        assertEquals(HistoryDiskCache.MAX_ENTRIES, read.size)
        assertEquals(450.0, read.last().USD!!, 1e-9)
        f.delete()
    }

    @Test fun corruptFileReadsEmpty() = runBlocking<Unit> {
        val f = File.createTempFile("hist", ".json").apply { writeText("{no json") }
        assertEquals(0, HistoryDiskCache(f).read().size)
        f.delete()
    }
}
