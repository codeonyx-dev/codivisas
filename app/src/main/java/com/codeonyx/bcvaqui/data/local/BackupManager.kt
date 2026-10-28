package com.codeonyx.bcvaqui.data.local

import android.content.Context
import com.codeonyx.bcvaqui.data.model.PaymentContact
import com.codeonyx.bcvaqui.domain.Debt
import com.codeonyx.bcvaqui.domain.SavingsGoal
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SpreadBackup(
    val version: Int = 1,
    val createdAt: Long = 0,
    val debts: List<Debt> = emptyList(),
    val goals: List<SavingsGoal> = emptyList(),
    val paymentContacts: List<PaymentContact> = emptyList(),
    /** Paleta de colores (AppPalette.name). Null en respaldos antiguos: no se cambia. */
    val palette: String? = null
)

/** Respaldo local en JSON (deudas, ahorros, métodos de pago y paleta) vía selector de archivos. */
object BackupManager {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }

    suspend fun export(context: Context): String {
        val fin = FinanceRepository(context)
        val prefs = PreferencesRepository(context).preferences.first()
        return json.encodeToString(
            SpreadBackup.serializer(),
            SpreadBackup(2, System.currentTimeMillis(), fin.debts.first(), fin.goals.first(), prefs.paymentContacts, prefs.palette)
        )
    }

    /** Devuelve un resumen legible o lanza si el archivo no es válido. */
    suspend fun import(context: Context, raw: String): String {
        val b = json.decodeFromString(SpreadBackup.serializer(), raw)
        val fin = FinanceRepository(context)
        fin.updateDebts { b.debts }
        fin.updateGoals { b.goals }
        PreferencesRepository(context).setPaymentContacts(b.paymentContacts)
        val palette = b.palette?.let { com.codeonyx.bcvaqui.domain.AppPalette.entries.firstOrNull { p -> p.name == it } }
        if (palette != null) {
            PreferencesRepository(context).setPalette(palette.name)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                com.codeonyx.bcvaqui.ui.theme.PaletteStore.write(context, palette)
            }
            com.codeonyx.bcvaqui.ui.theme.PaletteManager.applyLauncherIcon(context, palette)
            com.codeonyx.bcvaqui.widget.Widgets.updateAll(context)
        }
        val extra = palette?.let { " · paleta ${it.label}" } ?: ""
        return "${b.debts.size} deudas, ${b.goals.size} metas y ${b.paymentContacts.size} métodos de pago$extra"
    }
}
