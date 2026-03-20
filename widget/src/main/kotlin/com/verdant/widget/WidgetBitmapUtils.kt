package com.verdant.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import java.io.File
import java.io.FileOutputStream

/**
 * Off-screen Canvas rendering for widget types that need graphics Glance can't produce:
 * circular progress rings, concentric arcs, gradient backgrounds.
 *
 * All bitmaps are saved as PNG files in [context.filesDir]/widget_bitmaps/ so they
 * survive Glance recompositions without re-generation. [loadBitmap] decodes them back.
 */
internal object WidgetBitmapUtils {

    private fun bitmapDir(context: Context): File =
        File(context.filesDir, "widget_bitmaps").also { it.mkdirs() }

    fun saveBitmap(context: Context, bitmap: Bitmap, filename: String): String {
        val file = File(bitmapDir(context), filename)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 90, it) }
        return file.absolutePath
    }

    fun loadBitmap(path: String): Bitmap? = runCatching { BitmapFactory.decodeFile(path) }.getOrNull()

    // ── Summary Ring ──────────────────────────────────────────────────────────

    /**
     * Dark card + circular progress track + filled arc + "done/total" centred text.
     *
     * @param done         number of completed habits
     * @param total        total scheduled today
     * @param ringColorArgb ARGB int (from the habit's color or a brand green)
     * @param size         side length in px (square bitmap)
     */
    fun generateSummaryRing(done: Int, total: Int, ringColorArgb: Int, size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Background
        canvas.drawColor(Color.parseColor("#1A1D21"))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val cx = size / 2f
        val cy = size / 2f
        val sw = size * 0.10f
        val r  = cx - sw - size * 0.04f
        val oval = RectF(cx - r, cy - r, cx + r, cy + r)

        // Track ring
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = sw
        paint.color = Color.argb(45, 255, 255, 255)
        canvas.drawCircle(cx, cy, r, paint)

        // Progress arc
        if (total > 0 && done > 0) {
            paint.color = ringColorArgb
            paint.strokeCap = Paint.Cap.ROUND
            val sweep = (done.toFloat() / total * 360f).coerceAtMost(360f)
            canvas.drawArc(oval, -90f, sweep, false, paint)
        }

        // Centre: big number
        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.BUTT
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = size * 0.26f
        paint.isFakeBoldText = true
        canvas.drawText("$done", cx, cy - size * 0.03f, paint)

        // Sub-label: "/ total"
        paint.textSize = size * 0.13f
        paint.isFakeBoldText = false
        paint.color = Color.argb(170, 255, 255, 255)
        canvas.drawText("/ $total", cx, cy + size * 0.17f, paint)

        // Bottom hint
        paint.textSize = size * 0.09f
        paint.color = Color.argb(100, 255, 255, 255)
        val remaining = (total - done).coerceAtLeast(0)
        canvas.drawText(
            if (remaining == 0) "All done! 🎉" else "$remaining remaining",
            cx, size - size * 0.09f, paint,
        )

        return bmp
    }

    // ── Radial Concentric Rings ───────────────────────────────────────────────

    /**
     * Concentric arcs — one ring per habit (outermost first).
     *
     * @param habits list of (name, ARGB color, progress 0..1), max 5 rendered
     * @param done   completed habit count for centre label
     * @param total  total scheduled count
     */
    fun generateRadialRings(
        habits: List<Triple<String, Int, Float>>,
        done: Int,
        total: Int,
        size: Int,
    ): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.parseColor("#1A1D21"))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val cx = size / 2f
        val cy = size / 2f
        val maxR  = cx * 0.88f
        val count = habits.size.coerceIn(1, 5)
        val gap   = (maxR - cx * 0.20f) / count
        val sw    = (gap * 0.55f).coerceAtMost(size * 0.075f)

        habits.take(5).forEachIndexed { i, (_, colorArgb, progress) ->
            val r    = maxR - i * gap
            val oval = RectF(cx - r, cy - r, cx + r, cy + r)

            // Track
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = sw
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = Color.argb(35, 255, 255, 255)
            canvas.drawCircle(cx, cy, r, paint)

            // Arc
            if (progress > 0f) {
                paint.color = colorArgb
                val sweep = (progress * 360f).coerceAtMost(360f)
                canvas.drawArc(oval, -90f, sweep, false, paint)
            }
        }

        // Centre count
        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.BUTT
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = size * 0.20f
        paint.isFakeBoldText = true
        canvas.drawText("$done/$total", cx, cy + size * 0.07f, paint)

        // Dot legend at bottom
        val dotR = size * 0.035f
        val startX = cx - (habits.size - 1) * dotR * 1.6f
        habits.take(5).forEachIndexed { i, (_, colorArgb, _) ->
            paint.color = colorArgb
            canvas.drawCircle(startX + i * dotR * 3.2f, size * 0.88f, dotR, paint)
        }

        return bmp
    }

    // ── Motivational Quote with Gradient ─────────────────────────────────────

    private val GRADIENT_PALETTES = listOf(
        intArrayOf(0xFF1B5E20.toInt(), 0xFF2E7D32.toInt(), 0xFF388E3C.toInt()),  // verdant green
        intArrayOf(0xFF0D47A1.toInt(), 0xFF1565C0.toInt(), 0xFF1976D2.toInt()),  // deep blue
        intArrayOf(0xFF4A148C.toInt(), 0xFF6A1B9A.toInt(), 0xFF7B1FA2.toInt()),  // violet
        intArrayOf(0xFFBF360C.toInt(), 0xFFD84315.toInt(), 0xFFE64A19.toInt()),  // burnt orange
        intArrayOf(0xFF006064.toInt(), 0xFF00838F.toInt(), 0xFF0097A7.toInt()),  // teal
    )

    /**
     * Generates a gradient-background quote card bitmap.
     * The palette is chosen from [paletteIndex] (e.g., derived from day of year).
     */
    fun generateQuoteCard(
        quote: String,
        author: String,
        paletteIndex: Int,
        width: Int,
        height: Int,
    ): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Gradient background
        val palette = GRADIENT_PALETTES[paletteIndex % GRADIENT_PALETTES.size]
        val grad = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            palette,
            null,
            Shader.TileMode.CLAMP,
        )
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).also { it.shader = grad }
        canvas.drawRoundRect(
            RectF(0f, 0f, width.toFloat(), height.toFloat()),
            24f, 24f, bgPaint,
        )

        // Subtle texture dots
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).also {
            it.color = Color.argb(15, 255, 255, 255)
            it.style = Paint.Style.FILL
        }
        for (x in 0..width step 20) for (y in 0..height step 20) {
            canvas.drawCircle(x.toFloat(), y.toFloat(), 1.5f, dotPaint)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER

        // Quote mark
        paint.textSize = height * 0.20f
        paint.alpha = 60
        canvas.drawText("\u201C", width * 0.15f, height * 0.28f, paint)

        // Quote text — wrap to two lines if needed
        paint.textSize = height * 0.115f
        paint.alpha = 255
        paint.isFakeBoldText = false
        drawWrappedText(canvas, paint, quote, width * 0.12f, width * 0.88f, height * 0.40f, height * 0.145f)

        // Author
        if (author.isNotBlank()) {
            paint.textSize = height * 0.088f
            paint.alpha = 180
            paint.isFakeBoldText = false
            canvas.drawText("— $author", width / 2f, height * 0.82f, paint)
        }

        // Verdant label
        paint.textSize = height * 0.07f
        paint.alpha = 100
        canvas.drawText("🌱 Verdant", width / 2f, height * 0.93f, paint)

        return bmp
    }

    /** Naive text wrapping for the quote card — breaks at space near max width. */
    private fun drawWrappedText(
        canvas: Canvas,
        paint: Paint,
        text: String,
        leftX: Float,
        rightX: Float,
        startY: Float,
        lineH: Float,
    ) {
        val maxW = rightX - leftX
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = ""
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxW) {
                current = candidate
            } else {
                if (current.isNotEmpty()) lines.add(current)
                current = word
            }
        }
        if (current.isNotEmpty()) lines.add(current)

        val cx = (leftX + rightX) / 2f
        val totalH = lines.size * lineH
        var y = startY - totalH / 2f + lineH * 0.75f
        for (line in lines) {
            canvas.drawText(line, cx, y, paint)
            y += lineH
        }
    }
}
