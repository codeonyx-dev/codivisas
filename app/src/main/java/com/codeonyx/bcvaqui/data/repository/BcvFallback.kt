package com.codeonyx.bcvaqui.data.repository

import com.codeonyx.bcvaqui.data.model.BcvRateResponse
import com.codeonyx.bcvaqui.data.model.DolarApiQuote

/** Convierte la respuesta de ve.dolarapi.com al formato de bcv.today. null si no trae USD. */
object BcvFallback {
    fun toBcvResponse(usd: DolarApiQuote?, eur: DolarApiQuote?): BcvRateResponse? {
        val usdRate = usd?.promedio?.takeIf { it > 0 } ?: return null
        val date = usd.fechaActualizacion?.take(10)?.takeIf { it.length == 10 }
        return BcvRateResponse(
            USD = usdRate,
            EUR = eur?.promedio?.takeIf { it > 0 },
            updatedAt = usd.fechaActualizacion,
            effectiveDate = date,
            date = date,
            source = "ve.dolarapi.com"
        )
    }
}
