package com.codeonyx.bcvaqui.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.codeonyx.bcvaqui.data.ApiConfig
import com.codeonyx.bcvaqui.data.model.CurrencyRate
import com.codeonyx.bcvaqui.data.model.PaymentContact
import com.codeonyx.bcvaqui.data.model.WhereDestination
import com.codeonyx.bcvaqui.domain.RateSources
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bcv_aqui_prefs")

data class UserPreferences(
    val selectedCurrency: String = "USD",
    val notificationIntervalHours: Int = 0,
    val dondeCurrencies: List<String> = WhereDestination.DEFAULT_CODES,
    val paymentContacts: List<PaymentContact> = emptyList(),
    val lastKnownRatesJson: String? = null,
    val lastKnownUpdatedAt: String? = null,
    val lastKnownUsdRate: Double? = null,
    val lastKnownSelectedCurrencyRate: Double? = null,
    val lastFetchEpochMs: Long = 0L,
    val lastKnownBinanceUsdtVes: Double? = null,
    /** Tasa de la moneda seleccionada al cierre del día anterior (para variación). */
    val previousDaySelectedRate: Double? = null,
    val variationSnapshotDate: String? = null,
    val variationTodayRate: Double? = null,
    /** Umbral % para borde de alerta en widgets (0 = desactivado). */
    val alertThresholdPercent: Double = 1.0,
    val widgetConvertAmount: Double = 100.0,
    /** Empieza en true para evitar un parpadeo antes de que DataStore cargue. */
    val hasSeenOnboarding: Boolean = true,
    val merchantMarginPercent: Double = 0.0,
    /** "system", "light" o "dark". */
    val themeMode: String = ThemeModes.SYSTEM,
    /** Operaciones recientes de la calculadora (más reciente primero). */
    val calculatorHistory: List<String> = emptyList(),
    val notifyBcvUsd: Boolean = true,
    val notifyBcvEur: Boolean = false,
    val notifyUsdt: Boolean = false,
    val quietHoursEnabled: Boolean = false,
    val quietStartHour: Int = 22,
    val quietEndHour: Int = 7,
    /** Avisar deudas próximas o vencidas. */
    val debtReminders: Boolean = true,
    val autoUpdateCheck: Boolean = true,
    val liteMode: Boolean = false,
    /** Última carga exitosa de todas las fuentes (JSON de RatesSnapshot). */
    val ratesSnapshotJson: String? = null,
    /** true si el último intento de actualizar por red falló (modo sin conexión). */
    val lastRefreshFailed: Boolean = false,
    /** Paleta de colores elegida (ver AppPalette). */
    val palette: String = "AZUL"
)

object ThemeModes {
    const val SYSTEM = "system"
    const val LIGHT = "light"
    const val DARK = "dark"
}

private const val HISTORY_SEPARATOR = "\u001F"
private const val MAX_CALC_HISTORY = 10

class PreferencesRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Caché en disco del histórico BCV (filesDir, excluido del respaldo como el resto de datos). */
    fun historyDiskCache(): HistoryDiskCache =
        HistoryDiskCache(java.io.File(context.filesDir, "bcv_history.json"))

    private object Keys {
        val SELECTED_CURRENCY = stringPreferencesKey("selected_currency")
        val NOTIFICATION_INTERVAL = intPreferencesKey("notification_interval_hours")
        val DONDE_CURRENCIES = stringPreferencesKey("donde_currencies")
        val PAYMENT_CONTACTS = stringPreferencesKey("payment_contacts_json")
        val LAST_RATES_JSON = stringPreferencesKey("last_rates_json")
        val LAST_UPDATED_AT = stringPreferencesKey("last_updated_at")
        val LAST_USD_RATE = doublePreferencesKey("last_usd_rate")
        val LAST_SELECTED_CURRENCY_RATE = doublePreferencesKey("last_selected_currency_rate")
        val LAST_FETCH_EPOCH = longPreferencesKey("last_fetch_epoch_ms")
        val LAST_BINANCE = doublePreferencesKey("last_binance_usdt_ves")
        val PREVIOUS_DAY_RATE = doublePreferencesKey("previous_day_selected_rate")
        val VARIATION_DATE = stringPreferencesKey("variation_snapshot_date")
        val VARIATION_TODAY = doublePreferencesKey("variation_today_rate")
        val ALERT_THRESHOLD = doublePreferencesKey("alert_threshold_percent")
        val WIDGET_CONVERT_AMOUNT = doublePreferencesKey("widget_convert_amount")
        val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CALC_HISTORY = stringPreferencesKey("calculator_history")
        val MERCHANT_MARGIN = doublePreferencesKey("merchant_margin_percent")
        val NOTIFY_USD = booleanPreferencesKey("notify_bcv_usd")
        val NOTIFY_EUR = booleanPreferencesKey("notify_bcv_eur")
        val NOTIFY_USDT = booleanPreferencesKey("notify_usdt")
        val QUIET_ON = booleanPreferencesKey("quiet_hours_enabled")
        val QUIET_START = intPreferencesKey("quiet_start_hour")
        val QUIET_END = intPreferencesKey("quiet_end_hour")
        val NOTIFY_BASELINES = stringPreferencesKey("notify_baselines_json")
        val DEBT_REMINDERS = booleanPreferencesKey("debt_reminders")
        val AUTO_UPDATE = booleanPreferencesKey("auto_update_check")
        val LITE_MODE = booleanPreferencesKey("lite_mode")
        val RATES_SNAPSHOT = stringPreferencesKey("rates_snapshot_json")
        val REFRESH_FAILED = booleanPreferencesKey("last_refresh_failed")
        val PALETTE = stringPreferencesKey("app_palette")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val donde = prefs[Keys.DONDE_CURRENCIES]
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.ifEmpty { null }
            ?: WhereDestination.DEFAULT_CODES

        UserPreferences(
            selectedCurrency = prefs[Keys.SELECTED_CURRENCY] ?: "USD",
            notificationIntervalHours = prefs[Keys.NOTIFICATION_INTERVAL] ?: 0,
            dondeCurrencies = donde,
            paymentContacts = decodePaymentContacts(prefs[Keys.PAYMENT_CONTACTS]),
            lastKnownRatesJson = prefs[Keys.LAST_RATES_JSON],
            lastKnownUpdatedAt = prefs[Keys.LAST_UPDATED_AT],
            lastKnownUsdRate = prefs[Keys.LAST_USD_RATE],
            lastKnownSelectedCurrencyRate = prefs[Keys.LAST_SELECTED_CURRENCY_RATE],
            lastFetchEpochMs = prefs[Keys.LAST_FETCH_EPOCH] ?: 0L,
            lastKnownBinanceUsdtVes = prefs[Keys.LAST_BINANCE],
            previousDaySelectedRate = prefs[Keys.PREVIOUS_DAY_RATE],
            variationSnapshotDate = prefs[Keys.VARIATION_DATE],
            variationTodayRate = prefs[Keys.VARIATION_TODAY],
            alertThresholdPercent = prefs[Keys.ALERT_THRESHOLD] ?: 1.0,
            widgetConvertAmount = prefs[Keys.WIDGET_CONVERT_AMOUNT] ?: 100.0,
            hasSeenOnboarding = prefs[Keys.HAS_SEEN_ONBOARDING] ?: false,
            merchantMarginPercent = prefs[Keys.MERCHANT_MARGIN] ?: 0.0,
            themeMode = prefs[Keys.THEME_MODE] ?: ThemeModes.SYSTEM,
            calculatorHistory = prefs[Keys.CALC_HISTORY]
                ?.split(HISTORY_SEPARATOR)
                ?.filter { it.isNotBlank() }
                .orEmpty(),
            notifyBcvUsd = prefs[Keys.NOTIFY_USD] ?: true,
            notifyBcvEur = prefs[Keys.NOTIFY_EUR] ?: false,
            notifyUsdt = prefs[Keys.NOTIFY_USDT] ?: false,
            quietHoursEnabled = prefs[Keys.QUIET_ON] ?: false,
            quietStartHour = prefs[Keys.QUIET_START] ?: 22,
            quietEndHour = prefs[Keys.QUIET_END] ?: 7,
            debtReminders = prefs[Keys.DEBT_REMINDERS] ?: true,
            autoUpdateCheck = prefs[Keys.AUTO_UPDATE] ?: true,
            liteMode = prefs[Keys.LITE_MODE] ?: false,
            ratesSnapshotJson = prefs[Keys.RATES_SNAPSHOT],
            lastRefreshFailed = prefs[Keys.REFRESH_FAILED] ?: false,
            palette = prefs[Keys.PALETTE] ?: "AZUL"
        )
    }

    suspend fun setNotifySource(key: String, on: Boolean) {
        context.dataStore.edit {
            when (key) {
                "BCV_USD" -> it[Keys.NOTIFY_USD] = on
                "BCV_EUR" -> it[Keys.NOTIFY_EUR] = on
                "USDT" -> it[Keys.NOTIFY_USDT] = on
            }
        }
    }

    suspend fun setQuietHours(enabled: Boolean, start: Int, end: Int) {
        context.dataStore.edit { it[Keys.QUIET_ON] = enabled; it[Keys.QUIET_START] = start; it[Keys.QUIET_END] = end }
    }

    suspend fun setDebtReminders(on: Boolean) { context.dataStore.edit { it[Keys.DEBT_REMINDERS] = on } }
    suspend fun setPalette(id: String) { context.dataStore.edit { it[Keys.PALETTE] = id } }
    suspend fun saveRatesSnapshot(jsonString: String) {
        context.dataStore.edit { it[Keys.RATES_SNAPSHOT] = jsonString; it[Keys.REFRESH_FAILED] = false }
    }
    suspend fun setRefreshFailed(failed: Boolean) { context.dataStore.edit { it[Keys.REFRESH_FAILED] = failed } }
    suspend fun setLiteMode(on: Boolean) { context.dataStore.edit { it[Keys.LITE_MODE] = on } }
    suspend fun setAutoUpdateCheck(on: Boolean) { context.dataStore.edit { it[Keys.AUTO_UPDATE] = on } }

    suspend fun notifyBaselines(): Map<String, Double> =
        context.dataStore.data.first()[Keys.NOTIFY_BASELINES]
            ?.let { runCatching { json.decodeFromString<Map<String, Double>>(it) }.getOrNull() }.orEmpty()

    suspend fun setNotifyBaselines(map: Map<String, Double>) {
        context.dataStore.edit { it[Keys.NOTIFY_BASELINES] = json.encodeToString(map) }
    }

    suspend fun setMerchantMarginPercent(percent: Double) {
        context.dataStore.edit { it[Keys.MERCHANT_MARGIN] = percent.coerceIn(-90.0, 500.0) }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun addCalculatorHistory(entry: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.CALC_HISTORY]
                ?.split(HISTORY_SEPARATOR)
                ?.filter { it.isNotBlank() }
                .orEmpty()
            val updated = (listOf(entry) + current.filter { it != entry }).take(MAX_CALC_HISTORY)
            prefs[Keys.CALC_HISTORY] = updated.joinToString(HISTORY_SEPARATOR)
        }
    }

    suspend fun clearCalculatorHistory() {
        context.dataStore.edit { it.remove(Keys.CALC_HISTORY) }
    }

    suspend fun setSelectedCurrency(code: String) {
        context.dataStore.edit { it[Keys.SELECTED_CURRENCY] = code }
    }

    suspend fun setNotificationIntervalHours(hours: Int) {
        context.dataStore.edit { it[Keys.NOTIFICATION_INTERVAL] = hours }
    }

    suspend fun setHasSeenOnboarding(seen: Boolean) {
        context.dataStore.edit { it[Keys.HAS_SEEN_ONBOARDING] = seen }
    }

    suspend fun setAlertThresholdPercent(percent: Double) {
        context.dataStore.edit { it[Keys.ALERT_THRESHOLD] = percent }
    }

    suspend fun setWidgetConvertAmount(amount: Double) {
        context.dataStore.edit { it[Keys.WIDGET_CONVERT_AMOUNT] = amount }
    }

    suspend fun cycleWidgetConvertAmount(): Double {
        val current = preferences.first().widgetConvertAmount
        val presets = ApiConfig.WIDGET_CONVERT_PRESETS
        val idx = presets.indexOfFirst { kotlin.math.abs(it - current) < 0.01 }
        val next = presets[(idx + 1).coerceAtLeast(0) % presets.size]
        setWidgetConvertAmount(next)
        return next
    }

    suspend fun setDondeCurrencies(codes: List<String>) {
        context.dataStore.edit {
            it[Keys.DONDE_CURRENCIES] = codes.joinToString(",")
        }
    }

    suspend fun setPaymentContacts(contacts: List<PaymentContact>) {
        context.dataStore.edit {
            it[Keys.PAYMENT_CONTACTS] = json.encodeToString(contacts)
        }
    }

    suspend fun upsertPaymentContact(contact: PaymentContact) {
        val current = preferences.first().paymentContacts
        var next = current.filterNot { it.id == contact.id } + contact
        if (contact.isDefault) next = com.codeonyx.bcvaqui.data.model.PaymentMigration.setDefault(next, contact.id)
        setPaymentContacts(com.codeonyx.bcvaqui.data.model.PaymentMigration.normalize(next.sortedBy { it.displayName.lowercase() }))
    }

    suspend fun deletePaymentContact(id: String) {
        val next = preferences.first().paymentContacts.filterNot { it.id == id }
        setPaymentContacts(com.codeonyx.bcvaqui.data.model.PaymentMigration.normalize(next))
    }

    suspend fun setDefaultPaymentContact(id: String) {
        setPaymentContacts(com.codeonyx.bcvaqui.data.model.PaymentMigration.setDefault(preferences.first().paymentContacts, id))
    }

    suspend fun cacheRates(
        rates: Map<String, CurrencyRate>,
        updatedAt: String?,
        usdRate: Double?,
        selectedCurrency: String,
        binanceUsdtVes: Double? = null
    ) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val selectedRate = RateSources.effectiveRate(selectedCurrency, rates, binanceUsdtVes)
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_RATES_JSON] = json.encodeToString(rates.values.toList())
            updatedAt?.let { prefs[Keys.LAST_UPDATED_AT] = it }
            usdRate?.let { prefs[Keys.LAST_USD_RATE] = it }
            selectedRate?.let { prefs[Keys.LAST_SELECTED_CURRENCY_RATE] = it }
            prefs[Keys.LAST_FETCH_EPOCH] = System.currentTimeMillis()
            binanceUsdtVes?.let { prefs[Keys.LAST_BINANCE] = it }

            if (selectedRate != null) {
                val storedDate = prefs[Keys.VARIATION_DATE]
                val storedToday = prefs[Keys.VARIATION_TODAY]
                if (storedDate != null && storedDate != today && storedToday != null) {
                    prefs[Keys.PREVIOUS_DAY_RATE] = storedToday
                }
                prefs[Keys.VARIATION_DATE] = today
                prefs[Keys.VARIATION_TODAY] = selectedRate
            }
        }
    }

    fun decodeCachedRates(jsonString: String?): Map<String, CurrencyRate> {
        if (jsonString.isNullOrBlank()) return emptyMap()
        return runCatching {
            json.decodeFromString<List<CurrencyRate>>(jsonString)
                .associateBy { it.code }
        }.getOrDefault(emptyMap())
    }

    private fun decodePaymentContacts(jsonString: String?): List<PaymentContact> {
        if (jsonString.isNullOrBlank()) return emptyList()
        return com.codeonyx.bcvaqui.data.model.PaymentMigration.decode(jsonString)
    }
}
