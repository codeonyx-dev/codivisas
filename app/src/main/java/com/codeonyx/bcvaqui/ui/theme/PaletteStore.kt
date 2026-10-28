package com.codeonyx.bcvaqui.ui.theme

import android.content.Context
import com.codeonyx.bcvaqui.domain.AppPalette

/**
 * Copia síncrona de la paleta en SharedPreferences: se lee en onCreate antes de setContent,
 * así la app arranca directamente con la paleta elegida (sin destello azul).
 * DataStore sigue siendo la fuente para respaldos; ambos se escriben juntos.
 */
object PaletteStore {
    private const val FILE = "codivisas_palette"
    private const val KEY = "palette"

    fun read(context: Context): AppPalette =
        AppPalette.from(context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null))

    fun write(context: Context, palette: AppPalette) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY, palette.name).apply()
        ActivePalette.current = palette
    }
}
