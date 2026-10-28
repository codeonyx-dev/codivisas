package com.codeonyx.bcvaqui.data.api

import com.codeonyx.bcvaqui.data.model.BcvRateResponse
import com.codeonyx.bcvaqui.data.model.BinanceP2pResponse
import com.codeonyx.bcvaqui.data.model.BinanceSearchRequest
import com.codeonyx.bcvaqui.data.model.WorldRatesResponse
import com.codeonyx.bcvaqui.data.model.DolarApiQuote
import com.codeonyx.bcvaqui.data.model.DolarApiHistoryEntry
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

/** ve.dolarapi.com: respaldo de la tasa oficial (USD y EUR). */
interface DolarApi {
    @GET("v1/dolares/oficial")
    suspend fun getUsdOfficial(): DolarApiQuote

    @GET("v1/euros/oficial")
    suspend fun getEurOfficial(): DolarApiQuote

    /** Histórico diario oficial; suele incluir la tasa del próximo día hábil ya publicada. */
    @GET("v1/historicos/dolares/oficial")
    suspend fun getOfficialHistory(): List<DolarApiHistoryEntry>

    /** Histórico diario del promedio paralelo (referencia de mercado). */
    @GET("v1/historicos/dolares/paralelo")
    suspend fun getParallelHistory(): List<DolarApiHistoryEntry>
}
