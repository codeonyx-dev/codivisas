package com.codeonyx.bcvaqui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.codeonyx.bcvaqui.ui.BcvAquiNavHost
import com.codeonyx.bcvaqui.ui.theme.BcvAquiTheme
import com.codeonyx.bcvaqui.update.AppUpdateManager
import com.codeonyx.bcvaqui.worker.WorkScheduler
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WorkScheduler.scheduleWidgetRefresh(this)
        AppUpdateManager.resumePendingInstall(this)
        lifecycleScope.launch {
            AppUpdateManager.checkForUpdate(this@MainActivity, notifyIfAvailable = true)
        }
        setContent {
            BcvAquiTheme {
                BcvAquiNavHost()
            }
        }
    }
}
