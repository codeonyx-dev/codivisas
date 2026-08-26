package com.codeonyx.bcvaqui.data.api

import com.codeonyx.bcvaqui.data.model.BcvRateResponse
import com.codeonyx.bcvaqui.data.model.WorldRatesResponse
import retrofit2.http.GET

interface BcvTodayApi {
    @GET("rate.json")
    suspend fun getRates(): BcvRateResponse

    @GET("history.json")
    suspend fun getHistory(): List<BcvRateResponse>
}

interface OpenErApi {
    @GET("v6/latest/USD")
    suspend fun getUsdRates(): WorldRatesResponse
}
