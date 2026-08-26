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
import com.codeonyx.bcvaqui.data.model.CurrencyRate
import com.codeonyx.bcvaqui.data.model.PaymentContact
import com.codeonyx.bcvaqui.data.model.WhereDestination
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bcv_aqui_prefs")

data class UserPreferences(
    val selectedCurrency: String = "USD",
    val notificationIntervalHours: Int = 0,
    val dondeCurrencies: List<String> = WhereDestination.DEFAULT_CODES,
    val paymentContacts: List<PaymentContact> = emptyList(),
    val showBinanceRate: Boolean = false,
    val lastKnownRatesJson: String? = null,
    val lastKnownUpdatedAt: String? = null,
    val lastKnownUsdRate: Double? = null,
    val lastKnownSelectedCurrencyRate: Double? = null,
    val lastFetchEpochMs: Long = 0L
)

class PreferencesRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val SELECTED_CURRENCY = stringPreferencesKey("selected_currency")
        val NOTIFICATION_INTERVAL = intPreferencesKey("notification_interval_hours")
        val DONDE_CURRENCIES = stringPreferencesKey("donde_currencies")
        val PAYMENT_CONTACTS = stringPreferencesKey("payment_contacts_json")
        val SHOW_BINANCE = booleanPreferencesKey("show_binance_rate")
        val LAST_RATES_JSON = stringPreferencesKey("last_rates_json")
        val LAST_UPDATED_AT = stringPreferencesKey("last_updated_at")
        val LAST_USD_RATE = doublePreferencesKey("last_usd_rate")
        val LAST_SELECTED_CURRENCY_RATE = doublePreferencesKey("last_selected_currency_rate")
        val LAST_FETCH_EPOCH = longPreferencesKey("last_fetch_epoch_ms")
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
            showBinanceRate = prefs[Keys.SHOW_BINANCE] ?: false,
            lastKnownRatesJson = prefs[Keys.LAST_RATES_JSON],
            lastKnownUpdatedAt = prefs[Keys.LAST_UPDATED_AT],
            lastKnownUsdRate = prefs[Keys.LAST_USD_RATE],
            lastKnownSelectedCurrencyRate = prefs[Keys.LAST_SELECTED_CURRENCY_RATE],
            lastFetchEpochMs = prefs[Keys.LAST_FETCH_EPOCH] ?: 0L
        )
    }

    suspend fun setSelectedCurrency(code: String) {
        context.dataStore.edit { it[Keys.SELECTED_CURRENCY] = code }
    }

    suspend fun setNotificationIntervalHours(hours: Int) {
        context.dataStore.edit { it[Keys.NOTIFICATION_INTERVAL] = hours }
    }

    suspend fun setShowBinanceRate(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_BINANCE] = enabled }
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
        val next = current.filterNot { it.id == contact.id } + contact
        setPaymentContacts(next.sortedBy { it.displayName.lowercase() })
    }

    suspend fun deletePaymentContact(id: String) {
        val next = preferences.first().paymentContacts.filterNot { it.id == id }
        setPaymentContacts(next)
    }

    suspend fun cacheRates(
        rates: Map<String, CurrencyRate>,
        updatedAt: String?,
        usdRate: Double?,
        selectedCurrency: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_RATES_JSON] = json.encodeToString(rates.values.toList())
            updatedAt?.let { prefs[Keys.LAST_UPDATED_AT] = it }
            usdRate?.let { prefs[Keys.LAST_USD_RATE] = it }
            rates[selectedCurrency]?.rateToVes?.let {
                prefs[Keys.LAST_SELECTED_CURRENCY_RATE] = it
            }
            prefs[Keys.LAST_FETCH_EPOCH] = System.currentTimeMillis()
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
        return runCatching {
            json.decodeFromString<List<PaymentContact>>(jsonString)
        }.getOrDefault(emptyList())
    }
}
