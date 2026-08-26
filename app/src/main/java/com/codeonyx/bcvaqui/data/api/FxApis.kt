package com.codeonyx.bcvaqui.data.api

import com.codeonyx.bcvaqui.data.model.BcvRateResponse
import com.codeonyx.bcvaqui.data.model.BinanceP2pResponse
import com.codeonyx.bcvaqui.data.model.BinanceSearchRequest
import com.codeonyx.bcvaqui.data.model.WorldRatesResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

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

interface BinanceP2pApi {
    @POST("bapi/c2c/v2/friendly/c2c/adv/search")
    suspend fun searchAds(@Body body: BinanceSearchRequest): BinanceP2pResponse
}
