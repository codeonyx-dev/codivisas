package com.codeonyx.bcvaqui.ui.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.widget.Toast
import androidx.core.content.FileProvider
import com.codeonyx.bcvaqui.domain.FxCalculator
import com.codeonyx.bcvaqui.domain.RateSources
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReceiptData(
    val currencyCode: String,
    val rateToVes: Double,
    val foreignAmount: Double,
    val vesAmount: Double,
    val updatedAt: String?
)

object ReceiptShareHelper {

    fun share(context: Context, data: ReceiptData, paymentText: String? = null) {
        runCatching {
            val bitmap = drawReceiptBitmap(context, data)
            val dir = File(context.cacheDir, "share").apply { mkdirs() }
            val file = File(dir, "comprobante_bcv_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val text = buildShareText(data, paymentText)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir comprobante"))
        }.onFailure { error ->
            Toast.makeText(
                context,
                "No se pudo generar el comprobante: ${error.message ?: "error"}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun formatConversionLine(data: ReceiptData): String {
        val symbol = FxCalculator.currencySymbol(data.currencyCode)
        val name = FxCalculator.currencyLabel(data.currencyCode)
        return "Monto: ${FxCalculator.format2(data.foreignAmount)} $symbol ($name) = ${FxCalculator.format2(data.vesAmount)} Bs."
    }

    fun buildShareText(data: ReceiptData, paymentText: String? = null): String {
        return buildString {
            append(formatConversionLine(data))
            if (!paymentText.isNullOrBlank()) {
                appendLine()
                appendLine()
                append(paymentText.trim())
            }
        }
    }

    private fun drawReceiptBitmap(context: Context, data: ReceiptData): Bitmap {
        val density = context.resources.displayMetrics.density
        fun dp(value: Float) = value * density

        val width = dp(360f).toInt()
        val height = dp(420f).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF121214.toInt() }
        val cardRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(cardRect, dp(28f), dp(28f), bgPaint)

        // Logo disc with exchange + spark mark
        val logoCx = dp(50f)
        val logoCy = dp(52f)
        val logoR = dp(26f)
        val discPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1E3A8A.toInt() }
        canvas.drawCircle(logoCx, logoCy, logoR, discPaint)

        val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = dp(3.2f)
            strokeCap = Paint.Cap.BUTT
        }
        val r = dp(12f)
        canvas.drawArc(
            logoCx - r, logoCy - r, logoCx + r, logoCy + r,
            200f, 140f, false, arrowPaint
        )
        canvas.drawArc(
            logoCx - r, logoCy - r, logoCx + r, logoCy + r,
            20f, 140f, false, arrowPaint
        )
        val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
        val head1 = Path().apply {
            moveTo(logoCx + r * 0.72f, logoCy - r * 0.7f)
            lineTo(logoCx + r * 0.35f, logoCy - r * 1.05f)
            lineTo(logoCx + r * 1.05f, logoCy - r * 0.95f)
            close()
        }
        val head2 = Path().apply {
            moveTo(logoCx - r * 0.72f, logoCy + r * 0.7f)
            lineTo(logoCx - r * 0.35f, logoCy + r * 1.05f)
            lineTo(logoCx - r * 1.05f, logoCy + r * 0.95f)
            close()
        }
        canvas.drawPath(head1, headPaint)
        canvas.drawPath(head2, headPaint)

        val sparkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFBBF24.toInt()
            style = Paint.Style.STROKE
            strokeWidth = dp(2f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val spark = Path().apply {
            moveTo(logoCx - dp(12f), logoCy + dp(10f))
            cubicTo(
                logoCx - dp(4f), logoCy + dp(6f),
                logoCx, logoCy,
                logoCx + dp(12f), logoCy - dp(12f)
            )
        }
        canvas.drawPath(spark, sparkPaint)
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFBBF24.toInt() }
        canvas.drawCircle(logoCx + dp(12f), logoCy - dp(12f), dp(2.4f), dotPaint)

        // Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = dp(22f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFA1A1AA.toInt()
            textSize = dp(13f)
        }
        canvas.drawText("SpreadVE", dp(86f), dp(48f), titlePaint)
        canvas.drawText("Comprobante de Cálculo", dp(86f), dp(68f), subtitlePaint)

        // White rate card
        val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
        val rateRect = RectF(dp(24f), dp(100f), width - dp(24f), dp(210f))
        canvas.drawRoundRect(rateRect, dp(18f), dp(18f), whitePaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF71717A.toInt()
            textSize = dp(11f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.08f
        }
        val ratePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF18181B.toInt()
            textSize = dp(34f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val sourcePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF71717A.toInt()
            textSize = dp(13f)
            textAlign = Paint.Align.CENTER
        }
        val cx = width / 2f
        canvas.drawText("TASA UTILIZADA", cx, dp(132f), labelPaint)
        canvas.drawText("Bs. ${FxCalculator.format2(data.rateToVes)}", cx, dp(172f), ratePaint)
        canvas.drawText("(${RateSources.rateSourceLabel(data.currencyCode)})", cx, dp(194f), sourcePaint)

        // Amounts row
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x40FFFFFF
            strokeWidth = dp(1f)
        }
        canvas.drawLine(cx, dp(250f), cx, dp(298f), dividerPaint)

        val amountLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFA1A1AA.toInt()
            textSize = dp(10f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.05f
        }
        val amountValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = dp(18f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val leftX = width * 0.27f
        val rightX = width * 0.73f
        val currencyName = FxCalculator.currencyLabel(data.currencyCode).uppercase(Locale.getDefault())
        val foreignText =
            "${FxCalculator.currencySymbol(data.currencyCode)} ${FxCalculator.format2(data.foreignAmount)}"
        val vesText = "Bs. ${FxCalculator.format2(data.vesAmount)}"

        canvas.drawText("MONTO $currencyName", leftX, dp(258f), amountLabelPaint)
        canvas.drawText(foreignText, leftX, dp(286f), amountValuePaint)
        canvas.drawText("MONTO BOLÍVARES", rightX, dp(258f), amountLabelPaint)
        canvas.drawText(vesText, rightX, dp(286f), amountValuePaint)

        // Date
        val dateText = data.updatedAt?.let { FxCalculator.formatUpdatedAt(it).substringBefore(" ") }
            ?: SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFA1A1AA.toInt()
            textSize = dp(13f)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(dateText, cx, dp(350f), datePaint)

        return bitmap
    }
}
