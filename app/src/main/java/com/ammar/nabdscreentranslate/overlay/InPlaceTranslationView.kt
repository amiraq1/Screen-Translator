package com.ammar.nabdscreentranslate.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import android.view.MotionEvent
import android.view.View
import com.ammar.nabdscreentranslate.R
import com.ammar.nabdscreentranslate.domain.InPlaceBlock

/**
 * Inline overlay mode: draws each translated block inside an opaque, rounded
 * "graphite" bubble positioned near the original text — NOT directly on top of
 * it without a background. This prevents the original/translation overlap.
 *
 * Features:
 * - Opaque dark graphite bubble (#F2151517) with 12dp corners
 * - Slim ember (#FF7000) accent bar on top of each bubble
 * - White medium-weight Arabic text, RTL aligned, comfortable line height
 * - Bubbles clamped inside screen bounds + vertically de-overlapped
 * - Tap a bubble → toggle original/translation; tap empty area → peek screen
 */
@SuppressLint("ViewConstructor")
class InPlaceTranslationView(
    context: Context,
    private val blocks: List<InPlaceBlock>,
    private val onCloseRequested: () -> Unit
) : View(context) {

    private val density = context.resources.displayMetrics.density
    @Suppress("DEPRECATION")
    private val scaledDensity = context.resources.displayMetrics.scaledDensity

    private val showTranslated = BooleanArray(blocks.size) { true }
    private var textVisible = true

    private val tajawal: Typeface? = runCatching {
        ResourcesCompat.getFont(context, R.font.tajawal_medium)
    }.getOrNull()

    // ─── Paints ──────────────────────────────────────────────────────────────
    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xF2151517.toInt() // opaque dark graphite (~95%)
        style = Paint.Style.FILL
    }
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF7000.toInt() // Ember500 — highlights only
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.RIGHT // RTL: align to right edge
        typeface = tajawal ?: Typeface.DEFAULT_BOLD
    }

    // ─── Metrics ─────────────────────────────────────────────────────────────
    private val padH = 8f * density        // horizontal padding
    private val padV = 6f * density         // vertical padding
    private val corner = 12f * density
    private val accentBar = 2.5f * density  // ember top bar height
    private val safeMargin = 10f * density  // screen edge safe margin
    private val bubbleGap = 6f * density    // min gap between stacked bubbles
    private val lineSpacingMult = 1.3f      // comfortable Arabic line height

    private val sizeShort = 19f * scaledDensity
    private val sizeMedium = 15f * scaledDensity
    private val sizeLong = 12.5f * scaledDensity
    private val sizeMin = 11f * scaledDensity

    private data class Bubble(
        val srcIndex: Int,
        val rect: RectF,
        val textRightX: Float,
        val firstBaseline: Float,
        val lineHeight: Float,
        val textSize: Float,
        val lines: List<String>
    )

    private var layoutCache: List<Bubble>? = null
    private var cachedW = 0
    private var cachedH = 0

    init {
        isClickable = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!textVisible) return

        val bubbles = ensureLayout()
        for (b in bubbles) {
            val translated = showTranslated[b.srcIndex]
            // Bubble background
            canvas.drawRoundRect(b.rect, corner, corner, bubblePaint)
            // Ember top accent bar (rounded, only top portion)
            drawTopAccent(canvas, b.rect)
            // Text
            textPaint.textSize = b.textSize
            textPaint.color = if (translated) Color.WHITE else 0xFFB9ABA0.toInt()
            var y = b.firstBaseline
            for (line in b.lines) {
                canvas.drawText(line, b.textRightX, y, textPaint)
                y += b.lineHeight
            }
        }
    }

    private fun drawTopAccent(canvas: Canvas, rect: RectF) {
        canvas.save()
        canvas.clipRect(rect.left, rect.top, rect.right, rect.top + accentBar)
        canvas.drawRoundRect(
            RectF(rect.left, rect.top, rect.right, rect.top + corner * 2),
            corner, corner, accentPaint
        )
        canvas.restore()
    }

    /** Build (and cache) bubble layouts: measure, clamp to screen, de-overlap vertically. */
    private fun ensureLayout(): List<Bubble> {
        val w = width
        val h = height
        layoutCache?.let { if (w == cachedW && h == cachedH) return it }
        if (w == 0 || h == 0) return emptyList()

        val maxInnerWidth = w - 2 * safeMargin - 2 * padH

        // Build initial bubbles
        val raw = ArrayList<Bubble>()
        blocks.forEachIndexed { index, block ->
            val box = block.boundingBox ?: return@forEachIndexed
            val text = block.translatedText
            if (text.isBlank()) return@forEachIndexed

            // Preferred text width: track original block but cap to screen-safe width
            val preferred = box.width().toFloat().coerceIn(90f * density, maxInnerWidth)

            // Choose size tier by length, auto-fit down so it never gets absurdly tall
            var size = when {
                text.length <= 22 -> sizeShort
                text.length <= 75 -> sizeMedium
                else -> sizeLong
            }
            var lines: List<String>
            while (true) {
                textPaint.textSize = size
                lines = wrapText(text, preferred)
                // cap to 6 lines; if more, shrink
                if (lines.size <= 6 || size <= sizeMin) break
                size -= 1f * density
            }
            textPaint.textSize = size
            lines = wrapText(text, preferred)

            val actualTextW = (lines.maxOfOrNull { textPaint.measureText(it) } ?: preferred)
                .coerceAtMost(maxInnerWidth)
            val lineHeight = size * lineSpacingMult
            val bubbleW = actualTextW + 2 * padH
            val bubbleH = lines.size * lineHeight + 2 * padV + accentBar

            // Position: anchor at original block, clamp inside screen
            var left = box.left.toFloat()
            if (left + bubbleW > w - safeMargin) left = w - safeMargin - bubbleW
            if (left < safeMargin) left = safeMargin
            var top = box.top.toFloat()
            if (top < safeMargin) top = safeMargin

            val rect = RectF(left, top, left + bubbleW, top + bubbleH)
            val textRightX = rect.right - padH
            val firstBaseline = rect.top + accentBar + padV + size * 0.85f

            raw.add(Bubble(index, rect, textRightX, firstBaseline, lineHeight, size, lines))
        }

        // De-overlap: process top→bottom, push down bubbles that collide
        raw.sortBy { it.rect.top }
        var lastBottom = 0f
        val placed = ArrayList<Bubble>(raw.size)
        for (b in raw) {
            var rect = b.rect
            if (rect.top < lastBottom + bubbleGap) {
                val shift = (lastBottom + bubbleGap) - rect.top
                rect = RectF(rect.left, rect.top + shift, rect.right, rect.bottom + shift)
            }
            // Clamp bottom inside screen (if it spills, move up but keep within bounds)
            if (rect.bottom > h - safeMargin) {
                val up = rect.bottom - (h - safeMargin)
                val newTop = (rect.top - up).coerceAtLeast(safeMargin)
                rect = RectF(rect.left, newTop, rect.right, newTop + (b.rect.bottom - b.rect.top))
            }
            val textRightX = rect.right - padH
            val firstBaseline = rect.top + accentBar + padV + b.textSize * 0.85f
            placed.add(b.copy(rect = rect, textRightX = textRightX, firstBaseline = firstBaseline))
            lastBottom = rect.bottom
        }

        layoutCache = placed
        cachedW = w
        cachedH = h
        return placed
    }

    /** Greedy word-wrap to fit width (no hard line cap; sizing handles overflow). */
    private fun wrapText(text: String, maxWidth: Float): List<String> {
        if (maxWidth <= 0) return listOf(text)
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (textPaint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) lines.add(current.toString())
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return if (lines.isEmpty()) listOf(text) else lines
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) return true
        val x = event.x
        val y = event.y

        val bubbles = ensureLayout()
        for (b in bubbles) {
            if (b.rect.contains(x, y)) {
                showTranslated[b.srcIndex] = !showTranslated[b.srcIndex]
                // recompute layout because text length changes bubble size
                layoutCache = null
                invalidate()
                return true
            }
        }

        // Tap empty area → peek the underlying screen
        textVisible = !textVisible
        invalidate()
        return true
    }

    fun blockCount(): Int = blocks.count { it.boundingBox != null }
}
