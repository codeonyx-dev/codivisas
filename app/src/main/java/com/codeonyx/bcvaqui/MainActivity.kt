package com.codeonyx.bcvaqui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codeonyx.bcvaqui.data.local.ThemeModes
import com.codeonyx.bcvaqui.ui.appViewModel
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.codeonyx.bcvaqui.ui.BcvAquiNavHost
import com.codeonyx.bcvaqui.ui.theme.BcvAquiTheme
import com.codeonyx.bcvaqui.update.AppUpdateManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Paleta leída de forma síncrona: tema del splash y colores listos antes del primer frame.
        val startPalette = com.codeonyx.bcvaqui.ui.theme.PaletteStore.read(applicationContext)
        com.codeonyx.bcvaqui.ui.theme.ActivePalette.current = startPalette
        setTheme(com.codeonyx.bcvaqui.ui.theme.PaletteManager.splashTheme(startPalette))
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppUpdateManager.resumePendingInstall(this)
        lifecycleScope.launch {
            // Ícono/splash acordes a la paleta guardada (p. ej. tras importar un respaldo).
            val saved = com.codeonyx.bcvaqui.domain.AppPalette.from(
                com.codeonyx.bcvaqui.data.local.PreferencesRepository(applicationContext).preferences.first().palette
            )
            if (saved != com.codeonyx.bcvaqui.ui.theme.ActivePalette.current) {
                com.codeonyx.bcvaqui.ui.theme.PaletteStore.write(applicationContext, saved)
            }
            com.codeonyx.bcvaqui.ui.theme.PaletteManager.sync(this@MainActivity, saved)
        }
        lifecycleScope.launch {
            AppUpdateManager.checkForUpdate(this@MainActivity, notifyIfAvailable = true)
        }
        setContent {
            val viewModel = appViewModel()
            val prefs by viewModel.preferences.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val dark = when (prefs.themeMode) {
                ThemeModes.LIGHT -> false
                ThemeModes.DARK -> true
                else -> systemDark
            }
            DisposableEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                }
                // La barra superior es azul en ambos temas: iconos de estado claros.
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                    navigationBarStyle = style
                )
                onDispose { }
            }
            // Fuente inmediata (SharedPreferences), no el valor por defecto de DataStore al arrancar.
            val palette = com.codeonyx.bcvaqui.ui.theme.ActivePalette.current
            BcvAquiTheme(darkTheme = dark, palette = palette) {
                BcvAquiNavHost()
            }
        }
    }
}
