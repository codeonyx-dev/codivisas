package com.codeonyx.bcvaqui.data

/**
 * Central configuration for API endpoints and refresh intervals.
 */
object ApiConfig {
    const val BCV_RATE_URL = "https://bcv.today/api/v1/"
    const val WORLD_FX_URL = "https://open.er-api.com/"
    const val BINANCE_P2P_URL = "https://p2p.binance.com/"
    const val WIDGET_UPDATE_INTERVAL_MINUTES = 30L
    const val DEFAULT_BACKGROUND_INTERVAL_HOURS = 4L
    const val HTTP_TIMEOUT_SECONDS = 30L
    /** Montos rápidos del widget convertidor (USD/EUR → Bs.). */
    val WIDGET_CONVERT_PRESETS = listOf(50.0, 100.0, 200.0, 500.0)
    /** Manifiesto de actualización (raw en GitHub). Actualizar en cada release. */
    const val UPDATE_MANIFEST_URL =
        "https://raw.githubusercontent.com/codeonyx-dev/spread-ve/main/update.json"
}
