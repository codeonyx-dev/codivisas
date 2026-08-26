package com.codeonyx.bcvaqui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.codeonyx.bcvaqui.ui.BcvAquiNavHost
import com.codeonyx.bcvaqui.ui.theme.BcvAquiTheme
import com.codeonyx.bcvaqui.worker.WorkScheduler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WorkScheduler.scheduleWidgetRefresh(this)
        setContent {
            BcvAquiTheme {
                BcvAquiNavHost()
            }
        }
    }
}
