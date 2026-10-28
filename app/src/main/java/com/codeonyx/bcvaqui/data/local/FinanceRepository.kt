package com.codeonyx.bcvaqui.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.codeonyx.bcvaqui.domain.Debt
import com.codeonyx.bcvaqui.domain.SavingsGoal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Deudas y ahorros: DataStore local (en datastore/, excluido del respaldo). */
private val Context.financeStore: DataStore<Preferences> by preferencesDataStore(name = "spreadve_finanzas")

class FinanceRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val debtSer = ListSerializer(Debt.serializer())
    private val goalSer = ListSerializer(SavingsGoal.serializer())
    private val DEBTS = stringPreferencesKey("debts_json")
    private val GOALS = stringPreferencesKey("goals_json")
    private val LAST_REMIND = androidx.datastore.preferences.core.longPreferencesKey("last_reminder_day")

    val debts: Flow<List<Debt>> = context.financeStore.data.map { decode(it[DEBTS], debtSer) }
    val goals: Flow<List<SavingsGoal>> = context.financeStore.data.map { decode(it[GOALS], goalSer) }

    private fun <T> decode(raw: String?, ser: kotlinx.serialization.KSerializer<List<T>>): List<T> =
        raw?.let { runCatching { json.decodeFromString(ser, it) }.getOrNull() }.orEmpty()

    suspend fun updateDebts(transform: (List<Debt>) -> List<Debt>) {
        context.financeStore.edit { it[DEBTS] = json.encodeToString(debtSer, transform(decode(it[DEBTS], debtSer))) }
    }

    suspend fun updateGoals(transform: (List<SavingsGoal>) -> List<SavingsGoal>) {
        context.financeStore.edit { it[GOALS] = json.encodeToString(goalSer, transform(decode(it[GOALS], goalSer))) }
    }

    /** Devuelve true (y marca el día) solo la primera vez que se llama en [epochDay]. */
    suspend fun claimReminderDay(epochDay: Long): Boolean {
        var claimed = false
        context.financeStore.edit {
            if (it[LAST_REMIND] != epochDay) { it[LAST_REMIND] = epochDay; claimed = true }
        }
        return claimed
    }
}
