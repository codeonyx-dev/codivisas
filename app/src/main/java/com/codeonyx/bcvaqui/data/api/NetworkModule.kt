package com.codeonyx.bcvaqui.data.api

import com.codeonyx.bcvaqui.BuildConfig
import com.codeonyx.bcvaqui.data.ApiConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object NetworkModule {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private fun baseClientBuilder(): OkHttpClient.Builder =
        OkHttpClient.Builder()
            .connectTimeout(ApiConfig.HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(ApiConfig.HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(ApiConfig.HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                        }
                    )
                }
            }

    private val okHttpClient: OkHttpClient by lazy {
        baseClientBuilder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "BCV-Aqui/1.0 Android")
                        .header("Accept", "application/json")
                        .build()
                )
            }
            .build()
    }

    /** Binance suele rechazar clientes sin headers de navegador. */
    private val binanceOkHttpClient: OkHttpClient by lazy {
        baseClientBuilder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header(
                            "User-Agent",
                            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                        )
                        .header("Accept", "application/json")
                        .header("Content-Type", "application/json")
                        .header("clienttype", "android")
                        .header("lang", "es")
                        .build()
                )
            }
            .build()
    }

    val bcvTodayApi: BcvTodayApi by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BCV_RATE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BcvTodayApi::class.java)
    }

    val openErApi: OpenErApi by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.WORLD_FX_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenErApi::class.java)
    }

    val binanceP2pApi: BinanceP2pApi by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BINANCE_P2P_URL)
            .client(binanceOkHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BinanceP2pApi::class.java)
    }
}
