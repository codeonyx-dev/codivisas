package com.codeonyx.bcvaqui.data.repository

import com.codeonyx.bcvaqui.data.api.NetworkModule
import com.codeonyx.bcvaqui.data.local.HistoryDiskCache
import com.codeonyx.bcvaqui.data.local.PreferencesRepository
import com.codeonyx.bcvaqui.data.local.UserPreferences
import com.codeonyx.bcvaqui.data.model.BcvRateResponse
import com.codeonyx.bcvaqui.data.model.BinanceSearchRequest
import com.codeonyx.bcvaqui.data.model.CurrencyRate
import com.codeonyx.bcvaqui.data.model.FxDashboard
import com.codeonyx.bcvaqui.data.model.WhereDestination
import com.codeonyx.bcvaqui.domain.FxCalculator
import com.codeonyx.bcvaqui.domain.DayPoint
import com.codeonyx.bcvaqui.domain.MarketMath
import com.codeonyx.bcvaqui.domain.NextRate
import com.codeonyx.bcvaqui.domain.UsdtQuote
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

class FxRepository(
    private val preferencesRepository: PreferencesRepository,
    private val historyDisk: HistoryDiskCache? = preferencesRepository.historyDiskCache()
) {
    val preferences = preferencesRepository.preferences
    private var historyCache: List<BcvRateResponse> = emptyList()
    private var historyCacheAtMs: Long = 0L

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
                val binanceDeferred = async { fetchBinanceUsdtVes() }
                val sellDeferred = async { fetchBinanceSide("SELL") }
                val nextDeferred = async { fetchNextRateSafe() }

                val bcvResult = bcvDeferred.await()
                val world = worldDeferred.await()
                val binance = binanceDeferred.await()
                val sell = sellDeferred.await()
                val next = nextDeferred.await()
                val bcvRates = bcvResult.rates
                val bcvUpdatedAt = bcvResult.updatedAt

                val now = System.currentTimeMillis()
                val merged = com.codeonyx.bcvaqui.domain.OfflineRates.merge(
                    com.codeonyx.bcvaqui.domain.OfflineRates.decode(prefs.ratesSnapshotJson),
                    com.codeonyx.bcvaqui.domain.RatesSnapshot(
                        bcvRates = bcvRates.values.toList(),
                        bcvUpdatedAt = bcvUpdatedAt,
                        worldRatesUsd = world.rates,
                        worldUpdatedAt = world.updatedAt,
                        usdtBuy = binance.rate,
                        usdtSell = sell.rate,
                        loadedAtMs = now
                    )
                )
                preferencesRepository.saveRatesSnapshot(com.codeonyx.bcvaqui.domain.OfflineRates.encode(merged))
                preferencesRepository.cacheRates(
                    rates = bcvRates,
                    updatedAt = bcvUpdatedAt,
                    usdRate = bcvRates["USD"]?.rateToVes,
                    selectedCurrency = prefs.selectedCurrency,
                    binanceUsdtVes = binance.rate
                )

                FxDashboard(
                    bcvRates = bcvRates,
                    bcvUpdatedAt = bcvUpdatedAt,
                    worldRatesUsd = merged.worldRatesUsd,
                    worldRatesUpdatedAt = merged.worldUpdatedAt,
                    binanceUsdtVes = binance.rate ?: merged.usdtBuy,
                    binanceError = binance.error,
                    isFromCache = false,
                    nextBcvUsd = next,
                    usdtQuote = if (binance.rate != null || sell.rate != null) {
                        UsdtQuote(binance.rate, sell.rate, System.currentTimeMillis())
                    } else null,
                    loadedAtMs = now
                )
            }
        }.recoverCatching { error ->
            preferencesRepository.setRefreshFailed(true)
            if (cachedRates.isNotEmpty() || com.codeonyx.bcvaqui.domain.OfflineRates.decode(prefs.ratesSnapshotJson) != null) {
                buildDashboardFromCache(prefs, cachedRates, error.message)
            } else {
                throw error
            }
        }
    }

    private suspend fun fetchNextRateSafe(): NextRate? = runCatching {
        val today = java.time.LocalDate.now(java.time.ZoneId.of("America/Caracas")).toString()
        MarketMath.nextRate(officialSeries(force = true), today)
    }.getOrNull()

    private var officialCache: List<DayPoint> = emptyList()
    private var parallelCache: List<DayPoint> = emptyList()

    private suspend fun officialSeries(force: Boolean = false): List<DayPoint> {
        if (!force && officialCache.isNotEmpty()) return officialCache
        return runCatching {
            NetworkModule.dolarApi.getOfficialHistory().toPoints().also { officialCache = it }
        }.getOrDefault(officialCache)
    }

    private fun List<com.codeonyx.bcvaqui.data.model.DolarApiHistoryEntry>.toPoints(): List<DayPoint> =
        mapNotNull { e ->
            val d = e.fecha?.take(10) ?: return@mapNotNull null
            val v = e.promedio ?: return@mapNotNull null
            DayPoint(d, v)
        }.sortedBy { it.date }

    /**
     * Series para gráficos: BCV (histórico en disco + ve.dolarapi.com como relleno) y paralelo/USDT.
     * Las fechas futuras (tasa ya publicada del próximo día) se excluyen.
     */
    suspend fun fetchMarketSeries(): MarketSeries = coroutineScope {
        val today = java.time.LocalDate.now(java.time.ZoneId.of("America/Caracas")).toString()
        val localDeferred = async {
            runCatching { loadHistoryEntries(forceNetwork = false) }.getOrDefault(emptyList())
                .mapNotNull { r ->
                    val d = (r.effectiveDate ?: r.date ?: r.updatedAt)?.take(10) ?: return@mapNotNull null
                    r.USD?.let { DayPoint(d, it) }
                }
        }
        val officialDeferred = async { officialSeries() }
        val parallelDeferred = async {
            if (parallelCache.isNotEmpty()) parallelCache else runCatching {
                NetworkModule.dolarApi.getParallelHistory().toPoints().also { parallelCache = it }
            }.getOrDefault(emptyList())
        }
        val bcv = MarketMath.merge(localDeferred.await(), officialDeferred.await())
            .filter { it.date <= today }
        val usdt = parallelDeferred.await().filter { it.date <= today }
        MarketSeries(bcv = bcv, usdt = usdt, gap = MarketMath.gapSeries(bcv, usdt))
    }

    /** Últimos [days] días del histórico BCV (más reciente al final). */
    suspend fun fetchHistory(days: Int = 30): Result<List<BcvRateResponse>> {
        return runCatching {
            loadHistoryEntries(forceNetwork = false).takeLast(days.coerceIn(7, 365))
        }
    }

    private suspend fun fetchBcvRatesWithRetry(maxAttempts: Int = 3): BcvParsedResult {
        var lastError: Throwable? = null
        repeat(maxAttempts) { attempt ->
            runCatching {
                return coroutineScope {
                    val rateDeferred = async { NetworkModule.bcvTodayApi.getRates() }
                    val historyDeferred = async { fetchLatestHistoryEntry(forceNetwork = true) }
                    val bestResponse = pickLatestBcvSnapshot(rateDeferred.await(), historyDeferred.await())
                    parseBcvResponse(bestResponse)
                }
            }.onFailure { lastError = it }
            if (attempt < maxAttempts - 1) {
                kotlinx.coroutines.delay(1_000L * (attempt + 1))
            }
        }
        // Respaldo: ve.dolarapi.com (tasa oficial USD/EUR).
        runCatching {
            coroutineScope {
                val usd = async { NetworkModule.dolarApi.getUsdOfficial() }
                val eur = async { runCatching { NetworkModule.dolarApi.getEurOfficial() }.getOrNull() }
                BcvFallback.toBcvResponse(usd.await(), eur.await())
            }
        }.getOrNull()?.let { return parseBcvResponse(it) }
        throw lastError ?: IllegalStateException("Failed to fetch BCV rates")
    }

    private suspend fun loadHistoryEntries(forceNetwork: Boolean): List<BcvRateResponse> {
        val cacheIsFresh = !forceNetwork &&
            historyCache.isNotEmpty() &&
            System.currentTimeMillis() - historyCacheAtMs < HISTORY_CACHE_TTL_MS
        if (cacheIsFresh) return historyCache

        return runCatching {
            NetworkModule.bcvTodayApi.getHistory().also {
                historyCache = it
                historyCacheAtMs = System.currentTimeMillis()
                historyDisk?.write(it)
            }
        }.getOrElse {
            if (historyCache.isEmpty()) historyCache = historyDisk?.read().orEmpty()
            historyCache
        }
    }

    /** bcv.today/rate.json a veces queda atrasado; el histórico suele tener la tasa del día. */
    private suspend fun fetchLatestHistoryEntry(forceNetwork: Boolean): BcvRateResponse? =
        loadHistoryEntries(forceNetwork).lastOrNull()

    private fun pickLatestBcvSnapshot(
        current: BcvRateResponse,
        fromHistory: BcvRateResponse?
    ): BcvRateResponse {
        if (fromHistory == null) return current
        return if (bcvSortKey(fromHistory) >= bcvSortKey(current)) fromHistory else current
    }

    private fun bcvSortKey(response: BcvRateResponse): String =
        response.effectiveDate ?: response.date ?: response.updatedAt?.take(10).orEmpty()

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

    private suspend fun fetchBinanceUsdtVes(): BinanceFetchResult = fetchBinanceSide("BUY")

    private suspend fun fetchBinanceSide(tradeType: String): BinanceFetchResult {
        return runCatching {
            val response = NetworkModule.binanceP2pApi.searchAds(
                BinanceSearchRequest(
                    asset = "USDT",
                    fiat = "VES",
                    tradeType = tradeType,
                    page = 1,
                    rows = 10,
                    payTypes = emptyList()
                )
            )
            if (response.code != null && response.code != "000000") {
                return BinanceFetchResult(
                    rate = null,
                    error = response.message?.takeIf { it.isNotBlank() } ?: "Binance no disponible"
                )
            }
            val prices = response.data.orEmpty()
                .mapNotNull { row -> row.adv?.price?.toDoubleOrNull() }
                .filter { it > 0 }
                .sorted()
            if (prices.isEmpty()) {
                BinanceFetchResult(null, "Sin anuncios P2P ahora")
            } else {
                val mid = prices[prices.size / 2]
                BinanceFetchResult(FxCalculator.round2(mid), null)
            }
        }.getOrElse { error ->
            BinanceFetchResult(
                rate = null,
                error = error.message?.take(80) ?: "Error al consultar Binance"
            )
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
        return BcvParsedResult(rates, bcvDisplayTimestamp(response))
    }

    private fun bcvDisplayTimestamp(response: BcvRateResponse): String? =
        response.effectiveDate ?: response.date ?: response.updatedAt

    private fun buildDashboardFromCache(
        prefs: UserPreferences,
        cachedRates: Map<String, CurrencyRate>,
        errorMessage: String? = null
    ): FxDashboard {
        val snap = com.codeonyx.bcvaqui.domain.OfflineRates.decode(prefs.ratesSnapshotJson)
        val rates = cachedRates.ifEmpty { com.codeonyx.bcvaqui.domain.OfflineRates.ratesMap(snap) }
        val buy = prefs.lastKnownBinanceUsdtVes ?: snap?.usdtBuy
        return FxDashboard(
            bcvRates = rates,
            bcvUpdatedAt = prefs.lastKnownUpdatedAt ?: snap?.bcvUpdatedAt,
            worldRatesUsd = snap?.worldRatesUsd.orEmpty(),
            worldRatesUpdatedAt = snap?.worldUpdatedAt,
            binanceUsdtVes = buy,
            isFromCache = true,
            errorMessage = errorMessage ?: "Sin conexión — mostrando última tasa guardada",
            usdtQuote = if (buy != null || snap?.usdtSell != null) {
                UsdtQuote(buy, snap?.usdtSell, snap?.loadedAtMs ?: prefs.lastFetchEpochMs)
            } else null,
            loadedAtMs = (snap?.loadedAtMs ?: prefs.lastFetchEpochMs).takeIf { it > 0L }
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

    private data class BinanceFetchResult(
        val rate: Double?,
        val error: String?
    )

    private companion object {
        const val HISTORY_CACHE_TTL_MS = 15 * 60 * 1000L
    }
}

data class MarketSeries(
    val bcv: List<DayPoint>,
    val usdt: List<DayPoint>,
    val gap: List<DayPoint>
)
