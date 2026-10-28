package com.codeonyx.bcvaqui.domain

/** Cálculos de precios con impuestos venezolanos y brecha cambiaria. */
object PriceTools {
    const val IVA_PERCENT = 16.0
    const val IGTF_PERCENT = 3.0

    data class TaxBreakdown(
        val base: Double,
        val iva: Double,
        val igtf: Double,
        val total: Double
    )

    /**
     * @param amount monto escrito por el usuario.
     * @param amountIncludesIva true si el monto ya trae IVA (se desglosa).
     * @param applyIgtf true si se paga en divisas (IGTF 3 % sobre el total con IVA).
     */
    fun taxes(
        amount: Double,
        amountIncludesIva: Boolean,
        applyIva: Boolean = true,
        applyIgtf: Boolean,
        ivaPercent: Double = IVA_PERCENT,
        igtfPercent: Double = IGTF_PERCENT
    ): TaxBreakdown {
        if (amount <= 0.0) return TaxBreakdown(0.0, 0.0, 0.0, 0.0)
        val ivaRate = if (applyIva) ivaPercent / 100.0 else 0.0
        val base = if (amountIncludesIva) amount / (1 + ivaRate) else amount
        val iva = base * ivaRate
        val withIva = base + iva
        val igtf = if (applyIgtf) withIva * igtfPercent / 100.0 else 0.0
        return TaxBreakdown(
            base = FxCalculator.round2(base),
            iva = FxCalculator.round2(iva),
            igtf = FxCalculator.round2(igtf),
            total = FxCalculator.round2(withIva + igtf)
        )
    }

    /** Brecha % del mercado (USDT) sobre la tasa oficial. null si falta alguna. */
    fun gapPercent(official: Double?, market: Double?): Double? {
        if (official == null || market == null || official <= 0.0 || market <= 0.0) return null
        return FxCalculator.round2((market - official) / official * 100.0)
    }
}
