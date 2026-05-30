package com.ammar.nabdscreentranslate.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import android.view.MotionEvent
import android.view.View
import com.ammar.nabdscreentranslate.R
import com.ammar.nabdscreentranslate.domain.InPlaceBlock

/**
 * Cardless, subtitle-style in-place translation overlay.
 *
 * Draws ONLY the translated text directly over each original text region,
 * using a strong black outline + drop shadow for readability instead of a
 * large opaque box. No cyan borders, no big black cards.
 *
 * Interactions:
 * - Tap a block → briefly reveal the original text for that block
 * - Tap empty area → toggle the whole overlay's text visibility (peek the screen)
 */
@SuppressLint("ViewConstructor")
class InPlaceTranslationView(
    context: Context,
    private val blocks: List<InPlaceBlock>,
    /** When true, draw a very faint translucent pill behind each line. Default off (pure cardless). */
    private val lightBackground: Boolean = false,
    private val onCloseRequested: () -> Unit
) : View(context) {

    private val density = context.resources.displayMetrics.density
    private val scaledDensity = context.resources.displayMetrics.scaledDensity

    // Per-block state: true = show translated, false = momentarily show original
    private val showTranslated = BooleanArray(blocks.size) { true }

    // Global peek toggle (tap empty area to hide all text and see the screen)
    private var textVisible = true

    private val tajawal: Typeface? = runCatching {
        ResourcesCompat.getFont(context, R.font.tajawal_medium)
    }.getOrNull()

    // Fill text paint (white)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.RIGHT // RTL
        typeface = tajawal ?: Typeface.DEFAULT_BOLD
        // Strong drop shadow for dark backgrounds
        setShadowLayer(5f * density, 0f, 1.5f * density, 0xCC000000.toInt())
    }

    // Outline paint (black stroke around glyphs) for light backgrounds / contrast
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xE6000000.toInt() // near-black
        style = Paint.Style.STROKE
        textAlign = Paint.Align.RIGHT
        typeface = tajawal ?: Typeface.DEFAULT_BOLD
    }

    // Optional faint background pill (only when enabled)
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33000000 // ~20% black, very light
        style = Paint.Style.FILL
    }

    // Tiny ember underline accent under each translated block
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF7000.toInt() // Ember500
        style = Paint.Style.FILL
    }

    // Font-size tiers (sp → px)
    private val sizeShort = 22f * scaledDensity   // short text
    private val sizeMedium = 16f * scaledDensity  // medium
    private val sizeLong = 13f * scaledDensity    // long
    private val sizeMin = 11f * scaledDensity

    private val lineSpacingMult = 1.2f
    private val cornerRadius = 5f * density

    init {
        isClickable = true
        // setShadowLayer requires software rendering on a custom View
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!textVisible) return

        blocks.forEachIndexed { index, block ->
            val box = block.boundingBox ?: return@forEachIndexed
            val text = if (showTranslated[index]) block.translatedText else block.originalText
            if (text.isBlank()) return@forEachIndexed
            drawBlock(canvas, box, text, showTranslated[index])
        }
    }

    private fun drawBlock(canvas: Canvas, box: Rect, text: String, translated: Boolean) {
        val maxWidth = box.width().toFloat().coerceAtLeast(40f * density)

        // Pick a starting size tier based on text length, then auto-fit down.
        val startSize = when {
            text.length <= 25 -> sizeShort
            text.length <= 80 -> sizeMedium
            else -> sizeLong
        }.coerceAtMost(box.height() * 1.1f)

        var size = startSize
        var lines: List<String>
        // Max lines that reasonably fit the block height (allow some growth, capped)
        val maxLines = ((box.height() / (startSize * lineSpacingMult)).toInt() + 2).coerceIn(1, 6)

        while (true) {
            applyTextSize(size)
            lines = wrapText(text, maxWidth, maxLines)
            val totalHeight = lines.size * size * lineSpacingMult
            // Fit within ~1.8x the original block height before shrinking further
            if (totalHeight <= box.height() * 1.8f || size <= sizeMin) break
            size -= 1f * density
        }
        applyTextSize(size)
        lines = wrapText(text, maxWidth, maxLines)

        val lineHeight = size * lineSpacingMult
        val xRight = box.right.toFloat()
        // Anchor baseline near the original top, nudged for cap height
        var y = box.top.toFloat() + size * 0.95f

        // Optional faint background pill behind the text block
        if (lightBackground) {
            val contentH = lines.size * lineHeight
            val padH = 6f * density
            val padV = 3f * density
            val rect = RectF(
                box.left.toFloat() - padH,
                box.top.toFloat() - padV,
                box.right.toFloat() + padH,
                box.top.toFloat() + contentH + padV
            )
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)
        }

        // Outline thickness scales slightly with text size
        strokePaint.strokeWidth = (size * 0.10f).coerceIn(2f * density, 4.5f * density)

        for (line in lines) {
            // 1) black outline (no shadow) for crisp edges on any background
            canvas.drawText(line, xRight, y, strokePaint)
            // 2) white fill with soft shadow on top
            fillPaint.color = Color.WHITE
            canvas.drawText(line, xRight, y, fillPaint)
            y += lineHeight
        }

        // Subtle ember accent: a short underline at the bottom-right of the block
        val accentW = (box.width() * 0.18f).coerceIn(14f * density, 40f * density)
        val accentY = (box.top + (lines.size * lineHeight) + 2f * density)
        canvas.drawRoundRect(
            RectF(xRight - accentW, accentY, xRight, accentY + 2f * density),
            1f * density, 1f * density, accentPaint
        )
    }

    private fun applyTextSize(size: Float) {
        fillPaint.textSize = size
        strokePaint.textSize = size
    }

    /** Greedy word-wrap to fit width, capped at maxLines (last line ellipsized if needed). */
    private fun wrapText(text: String, maxWidth: Float, maxLines: Int): List<String> {
        if (maxWidth <= 0) return listOf(text)
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()

        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (fillPaint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) lines.add(current.toString())
                current = StringBuilder(word)
                if (lines.size >= maxLines) break
            }
        }
        if (current.isNotEmpty() && lines.size < maxLines) lines.add(current.toString())

        // If we overflowed, ellipsize the last visible line
        if (lines.size >= maxLines) {
            val trimmed = lines.take(maxLines).toMutableList()
            var last = trimmed.last()
            if (fillPaint.measureText(last) > maxWidth || words.size > trimmed.sumOf { it.split(" ").size }) {
                while (last.isNotEmpty() && fillPaint.measureText("$last…") > maxWidth) {
                    last = last.dropLast(1)
                }
                trimmed[trimmed.size - 1] = "$last…"
            }
            return trimmed
        }
        return if (lines.isEmpty()) listOf(text) else lines
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) return true
        val x = event.x
        val y = event.y

        // Tap a block → toggle original/translated for that block
        blocks.forEachIndexed { index, block ->
            val box = block.boundingBox ?: return@forEachIndexed
            val pad = 10 * density
            if (x >= box.left - pad && x <= box.right + pad &&
                y >= box.top - pad && y <= box.bottom + pad
            ) {
                showTranslated[index] = !showTranslated[index]
                invalidate()
                return true
            }
        }

        // Tap empty area → peek the underlying screen (hide/show all translated text)
        textVisible = !textVisible
        invalidate()
        return true
    }

    fun blockCount(): Int = blocks.count { it.boundingBox != null }
}
