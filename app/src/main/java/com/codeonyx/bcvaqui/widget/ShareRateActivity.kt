package com.codeonyx.bcvaqui.widget

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.codeonyx.bcvaqui.domain.FxCalculator
import com.codeonyx.bcvaqui.ui.share.ReceiptData
import com.codeonyx.bcvaqui.ui.share.ReceiptShareHelper

/**
 * Activity transparente lanzada desde el widget "Compartir tasa".
 */
class ShareRateActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val snap = Widgets.loadSnapshot(this)
        val rate = snap.rate
        if (rate == null) {
            Toast.makeText(this, "Sin tasa para compartir", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val data = ReceiptData(
            currencyCode = snap.currency,
            rateToVes = rate,
            foreignAmount = 1.0,
            vesAmount = rate,
            updatedAt = snap.prefs.lastKnownUpdatedAt
        )

        runCatching {
            val text = buildString {
                append("SpreadVE · Tasa BCV\n")
                append("${FxCalculator.currencyLabel(snap.currency)} (${snap.currency}): ")
                append("${FxCalculator.format2(rate)} Bs.\n")
                append("Actualizado: ${FxCalculator.formatUpdatedAt(snap.prefs.lastKnownUpdatedAt)}")
            }
            // Prefer image receipt; also include text via helper
            ReceiptShareHelper.share(this, data)
            // Extra plain-text share as fallback path if needed — ReceiptShareHelper already opens chooser
            // Keep activity short-lived
        }.onFailure {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    "SpreadVE · ${snap.currency}: ${FxCalculator.format2(rate)} Bs. (BCV)"
                )
            }
            startActivity(Intent.createChooser(send, "Compartir tasa"))
        }

        finish()
    }
}
