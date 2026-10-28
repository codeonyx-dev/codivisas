package com.codeonyx.bcvaqui.ui.theme

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.codeonyx.bcvaqui.R
import com.codeonyx.bcvaqui.domain.AppPalette

/** Aplica la paleta fuera de Compose: ícono del launcher (activity-alias) y tema del splash. */
object PaletteManager {

    fun aliasComponent(context: Context, p: AppPalette) =
        ComponentName(context.packageName, "com.codeonyx.bcvaqui.${p.launcherAlias}")

    /** Habilita solo el alias de la paleta elegida (DONT_KILL_APP). Habilita primero el nuevo para no quedar sin ícono. */
    fun applyLauncherIcon(context: Context, palette: AppPalette) {
        val pm = context.packageManager
        runCatching {
            pm.setComponentEnabledSetting(
                aliasComponent(context, palette),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            AppPalette.entries.filter { it != palette }.forEach {
                pm.setComponentEnabledSetting(
                    aliasComponent(context, it),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
    }

    /** Alias habilitado actualmente según PackageManager (o null si no se puede leer). */
    fun currentLauncherPalette(context: Context): AppPalette? = runCatching {
        val pm = context.packageManager
        AppPalette.entries.firstOrNull { p ->
            when (pm.getComponentEnabledSetting(aliasComponent(context, p))) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> p == AppPalette.DEFAULT
                else -> false
            }
        }
    }.getOrNull()

    fun splashTheme(p: AppPalette): Int = when (p) {
        AppPalette.AZUL -> R.style.Theme_BcvAqui_Splash_Azul
        AppPalette.ONIX -> R.style.Theme_BcvAqui_Splash_Onix
        AppPalette.MORADO -> R.style.Theme_BcvAqui_Splash_Morado
        AppPalette.ESMERALDA -> R.style.Theme_BcvAqui_Splash_Esmeralda
    }

    fun logoForeground(p: AppPalette): Int = when (p) {
        AppPalette.AZUL -> R.drawable.ic_launcher_foreground_azul
        AppPalette.ONIX -> R.drawable.ic_launcher_foreground_onix
        AppPalette.MORADO -> R.drawable.ic_launcher_foreground_morado
        AppPalette.ESMERALDA -> R.drawable.ic_launcher_foreground_esmeralda
    }

    /** Android 13+: el sistema recuerda el tema del splash para los próximos arranques. */
    fun applySplash(activity: Activity, palette: AppPalette) {
        if (Build.VERSION.SDK_INT >= 33) {
            runCatching { activity.splashScreen.setSplashScreenTheme(splashTheme(palette)) }
        }
    }

    /** Sincroniza ícono y splash si no coinciden con la paleta guardada (p. ej. tras importar un respaldo). */
    fun sync(activity: Activity, palette: AppPalette) {
        if (currentLauncherPalette(activity) != palette) applyLauncherIcon(activity, palette)
        applySplash(activity, palette)
    }
}
