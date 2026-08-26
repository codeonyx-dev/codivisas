package com.codeonyx.bcvaqui.data.repository

import com.codeonyx.bcvaqui.data.api.NetworkModule
import com.codeonyx.bcvaqui.data.local.PreferencesRepository
import com.codeonyx.bcvaqui.data.local.UserPreferences
import com.codeonyx.bcvaqui.data.model.BcvRateResponse
import com.codeonyx.bcvaqui.data.model.CurrencyRate
import com.codeonyx.bcvaqui.data.model.FxDashboard
import com.codeonyx.bcvaqui.data.model.WhereDestination
import com.codeonyx.bcvaqui.domain.FxCalculator
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

class FxRepository(
    private val preferencesRepository: PreferencesRepository
) {
    val preferences = preferencesRepository.preferences

    suspend fun refreshDashboard(forceNetwork: Boolean = true): Result<FxDashboard> {
        val prefs = preferences.first()
        val cachedRates = preferencesRepository.decodeCachedRates(prefs.lastKnownRatesJson)

        if (!forceNetwork) {
            return Result.success(buildDashboardFromCache(prefs, cachedRates))
        }

        return runCatching {
            coroutineScope {
                val bcvDeferred = async { fetchBcvRatesWithRetry() }
                val worldDeferred = async { fetchWorldRatesSafe() }

                val bcvResult = bcvDeferred.await()
                val world = worldDeferred.await()
                val bcvRates = bcvResult.rates
                val bcvUpdatedAt = bcvResult.updatedAt

                preferencesRepository.cacheRates(
                    rates = bcvRates,
                    updatedAt = bcvUpdatedAt,
                    usdRate = bcvRates["USD"]?.rateToVes,
                    selectedCurrency = prefs.selectedCurrency
                )

                FxDashboard(
                    bcvRates = bcvRates,
                    bcvUpdatedAt = bcvUpdatedAt,
                    worldRatesUsd = world.rates,
                    worldRatesUpdatedAt = world.updatedAt,
                    isFromCache = false
                )
            }
        }.recoverCatching { error ->
            if (cachedRates.isNotEmpty()) {
                buildDashboardFromCache(prefs, cachedRates, error.message)
            } else {
                throw error
            }
        }
    }

    /** Últimos [days] días del histórico BCV (más reciente al final). */
    suspend fun fetchHistory(days: Int = 30): Result<List<BcvRateResponse>> {
        return runCatching {
            val all = NetworkModule.bcvTodayApi.getHistory()
            all.takeLast(days.coerceIn(7, 365))
        }
    }

    private suspend fun fetchBcvRatesWithRetry(maxAttempts: Int = 3): BcvParsedResult {
        var lastError: Throwable? = null
        repeat(maxAttempts) { attempt ->
            runCatching {
                val response = NetworkModule.bcvTodayApi.getRates()
                return parseBcvResponse(response)
            }.onFailure { lastError = it }
            if (attempt < maxAttempts - 1) {
                kotlinx.coroutines.delay(1_000L * (attempt + 1))
            }
        }
        throw lastError ?: IllegalStateException("Failed to fetch BCV rates")
    }

    private suspend fun fetchWorldRatesSafe(): WorldParsedResult {
        return runCatching {
            val response = NetworkModule.openErApi.getUsdRates()
            val wanted = WhereDestination.ALL.map { it.code }.toSet() + setOf("USD")
            val filtered = response.rates.orEmpty()
                .filterKeys { it in wanted }
                .mapValues { FxCalculator.round4(it.value) }
            WorldParsedResult(filtered, response.updatedAt)
        }.getOrElse {
            WorldParsedResult(emptyMap(), null)
        }
    }

    private fun parseBcvResponse(response: BcvRateResponse): BcvParsedResult {
        val rates = buildMap {
            response.USD?.let { put("USD", CurrencyRate("USD", FxCalculator.round2(it), response.updatedAt)) }
            response.EUR?.let { put("EUR", CurrencyRate("EUR", FxCalculator.round2(it), response.updatedAt)) }
            response.CNY?.let { put("CNY", CurrencyRate("CNY", FxCalculator.round2(it), response.updatedAt)) }
            response.TRY?.let { put("TRY", CurrencyRate("TRY", FxCalculator.round2(it), response.updatedAt)) }
            response.RUB?.let { put("RUB", CurrencyRate("RUB", FxCalculator.round2(it), response.updatedAt)) }
        }
        if (rates.isEmpty()) throw IllegalStateException("BCV API returned no rates")
        return BcvParsedResult(rates, response.updatedAt)
    }

    private fun buildDashboardFromCache(
        prefs: UserPreferences,
        cachedRates: Map<String, CurrencyRate>,
        errorMessage: String? = null
    ): FxDashboard {
        return FxDashboard(
            bcvRates = cachedRates,
            bcvUpdatedAt = prefs.lastKnownUpdatedAt,
            isFromCache = true,
            errorMessage = errorMessage ?: "Sin conexión — mostrando última tasa guardada"
        )
    }

    private data class BcvParsedResult(
        val rates: Map<String, CurrencyRate>,
        val updatedAt: String?
    )

    private data class WorldParsedResult(
        val rates: Map<String, Double>,
        val updatedAt: String?
    )
}
