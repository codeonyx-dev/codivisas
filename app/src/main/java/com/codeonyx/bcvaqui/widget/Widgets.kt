package com.codeonyx.bcvaqui.widget

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.RemoteViews
import com.codeonyx.bcvaqui.MainActivity
import com.codeonyx.bcvaqui.R
import com.codeonyx.bcvaqui.data.local.PreferencesRepository
import com.codeonyx.bcvaqui.data.local.UserPreferences
import com.codeonyx.bcvaqui.domain.FxCalculator
import com.codeonyx.bcvaqui.domain.RateSources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class WidgetSnapshot(
    val prefs: UserPreferences,
    val currency: String,
    val rate: Double?,
    val dayChangePct: Double?,
    val alertActive: Boolean,
    val alertUp: Boolean
)

object Widgets {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun loadSnapshot(context: Context): WidgetSnapshot {
        val repo = PreferencesRepository(context)
        val prefs = repo.preferences.first()
        refreshNight(context)
        val cached = repo.decodeCachedRates(prefs.lastKnownRatesJson)
        val currency = prefs.selectedCurrency
        val rate = RateSources.effectiveRate(
            currency = currency,
            bcvRates = cached,
            binanceUsdtVes = prefs.lastKnownBinanceUsdtVes
        ) ?: prefs.variationTodayRate
            ?: prefs.lastKnownSelectedCurrencyRate
            ?: if (currency == "USD") prefs.lastKnownUsdRate else null

        val previous = prefs.previousDaySelectedRate
        val dayChangePct = if (rate != null && previous != null && previous > 0) {
            FxCalculator.round2((rate - previous) / previous * 100.0)
        } else null

        val threshold = prefs.alertThresholdPercent
        val alertActive = threshold > 0 && dayChangePct != null && kotlin.math.abs(dayChangePct) >= threshold
        val alertUp = (dayChangePct ?: 0.0) >= 0

        return WidgetSnapshot(prefs, currency, rate, dayChangePct, alertActive, alertUp)
    }

    /** Marca de sin conexión para widgets, o null si la última actualización fue exitosa. */
    fun offlineMarker(snapshot: WidgetSnapshot): String? {
        val snap = com.codeonyx.bcvaqui.domain.OfflineRates.decode(snapshot.prefs.ratesSnapshotJson)
        if (!com.codeonyx.bcvaqui.domain.OfflineRates.isOffline(snapshot.prefs.lastRefreshFailed, snap)) return null
        return com.codeonyx.bcvaqui.domain.OfflineRates.widgetMarker(snap?.loadedAtMs)
    }

    fun alertBackground(snapshot: WidgetSnapshot): Int = when {
        !snapshot.alertActive -> R.drawable.widget_background
        snapshot.alertUp -> R.drawable.widget_background_alert_up
        else -> R.drawable.widget_background_alert_down
    }

    fun openAppPending(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun applyBrandAndAlert(views: RemoteViews, rootId: Int, snapshot: WidgetSnapshot) {
        val palette = com.codeonyx.bcvaqui.domain.AppPalette.from(snapshot.prefs.palette)
        val bg = if (snapshot.alertActive) alertBackground(snapshot) else paletteBackground(palette)
        views.setInt(rootId, "setBackgroundResource", bg)
        accentViewsFor(rootId).forEach { id -> views.setTextColor(id, paletteAccent(palette)) }
    }

    /** Fondo del widget según la paleta (claro: blanco con tinte; oscuro: color de la paleta). */
    fun paletteBackground(p: com.codeonyx.bcvaqui.domain.AppPalette): Int = when (p) {
        com.codeonyx.bcvaqui.domain.AppPalette.AZUL -> R.drawable.widget_background
        com.codeonyx.bcvaqui.domain.AppPalette.ONIX -> R.drawable.widget_background_onix
        com.codeonyx.bcvaqui.domain.AppPalette.MORADO -> R.drawable.widget_background_morado
        com.codeonyx.bcvaqui.domain.AppPalette.ESMERALDA -> R.drawable.widget_background_esmeralda
    }

    private var nightCache: Boolean = false

    /** Color de acento: color principal en claro, acento de la paleta en oscuro. */
    private fun paletteAccent(p: com.codeonyx.bcvaqui.domain.AppPalette): Int =
        (if (nightCache) p.darkAccent
        else if (com.codeonyx.bcvaqui.domain.AppPalette.contrast(p.accent, 0xFFFFFFFF) >= 4.5) p.accent
        else p.primary).toInt()

    fun refreshNight(context: Context) {
        nightCache = (context.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    /** Vista de acento (etiqueta de moneda) que existe en cada layout, para no tocar ids ausentes. */
    private fun accentViewsFor(rootId: Int): List<Int> = when (rootId) {
        R.id.widget_root -> listOf(R.id.widget_currency_label)
        R.id.widget_converter_root -> listOf(R.id.widget_converter_from)
        R.id.widget_spread_root -> listOf(R.id.widget_spread_lbl_bcv, R.id.widget_spread_lbl_p2p, R.id.widget_spread_lbl_pct)
        else -> emptyList()
    }

    private val providers = listOf(
        RateWidgetProvider::class.java,
        SpreadWidgetProvider::class.java,
        ConverterWidgetProvider::class.java,
    )

    /** true si el usuario tiene al menos un widget de Codivisas en la pantalla de inicio. */
    fun hasAnyWidget(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context) ?: return false
        return providers.any { cls ->
            runCatching { manager.getAppWidgetIds(ComponentName(context, cls)).isNotEmpty() }
                .getOrDefault(false)
        }
    }

    /** Actualiza los widgets colocados; si no hay ninguno no lee nada. */
    fun updateAll(context: Context) {
        val appContext = context.applicationContext
        if (!hasAnyWidget(appContext)) return
        runAsync {
            val snapshot = loadSnapshot(appContext)
            RateWidgetProvider.updateAllWidgets(appContext, snapshot)
            SpreadWidgetProvider.updateAllWidgets(appContext, snapshot)
            ConverterWidgetProvider.updateAllWidgets(appContext, snapshot)
        }
    }

    fun runAsync(
        pendingResult: BroadcastReceiver.PendingResult? = null,
        block: suspend () -> Unit
    ) {
        scope.launch {
            try {
                block()
            } finally {
                pendingResult?.finish()
            }
        }
    }
}
