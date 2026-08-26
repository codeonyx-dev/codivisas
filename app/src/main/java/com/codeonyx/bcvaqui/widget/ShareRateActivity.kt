package com.codeonyx.bcvaqui.widget

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.codeonyx.bcvaqui.domain.FxCalculator
import com.codeonyx.bcvaqui.domain.RateSources
import com.codeonyx.bcvaqui.ui.share.ReceiptData
import com.codeonyx.bcvaqui.ui.share.ReceiptShareHelper
import kotlinx.coroutines.launch

/**
 * Activity transparente lanzada desde el widget "Compartir tasa".
 */
class ShareRateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val snap = Widgets.loadSnapshot(this@ShareRateActivity)
            val rate = snap.rate
            if (rate == null) {
                Toast.makeText(this@ShareRateActivity, "Sin tasa para compartir", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            val data = ReceiptData(
                currencyCode = snap.currency,
                rateToVes = rate,
                foreignAmount = 1.0,
                vesAmount = rate,
                updatedAt = snap.prefs.lastKnownUpdatedAt
                    .takeUnless { snap.currency == RateSources.BINANCE_USDT }
            )

            val source = RateSources.rateSourceLabel(snap.currency)
            runCatching {
                ReceiptShareHelper.share(this@ShareRateActivity, data)
            }.onFailure {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "SpreadVE · ${snap.currency}: ${FxCalculator.format2(rate)} Bs. ($source)"
                    )
                }
                startActivity(Intent.createChooser(send, "Compartir tasa"))
            }

            finish()
        }
    }
}
