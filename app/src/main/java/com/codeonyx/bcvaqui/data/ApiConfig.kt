package com.codeonyx.bcvaqui.data

/**
 * Central configuration for API endpoints and refresh intervals.
 */
object ApiConfig {
    const val BCV_RATE_URL = "https://bcv.today/api/v1/"
    /** Respaldo de la tasa oficial BCV si bcv.today falla. */
    const val DOLAR_API_URL = "https://ve.dolarapi.com/"
    const val WORLD_FX_URL = "https://open.er-api.com/"
    const val BINANCE_P2P_URL = "https://p2p.binance.com/"
    const val WIDGET_UPDATE_INTERVAL_MINUTES = 30L
    const val DEFAULT_BACKGROUND_INTERVAL_HOURS = 4L
    const val HTTP_TIMEOUT_SECONDS = 30L
    /** Montos rápidos del widget convertidor (USD/EUR → Bs.). */
    val WIDGET_CONVERT_PRESETS = listOf(50.0, 100.0, 200.0, 500.0)
    /** Manifiesto de actualización, servido desde codeonyx.online. Actualizar en cada release. */
    const val UPDATE_MANIFEST_URL = "https://codeonyx.online/codivisas/update.json"
    /** Respaldo en GitHub mientras el repositorio siga público. */
    const val UPDATE_MANIFEST_FALLBACK_URL =
        "https://raw.githubusercontent.com/codeonyx-dev/spread-ve/main/update.json"
    val UPDATE_MANIFEST_URLS = listOf(UPDATE_MANIFEST_URL, UPDATE_MANIFEST_FALLBACK_URL)
}
