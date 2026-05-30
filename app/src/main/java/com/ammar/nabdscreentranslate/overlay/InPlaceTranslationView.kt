package com.ammar.nabdscreentranslate.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import com.ammar.nabdscreentranslate.domain.InPlaceBlock
import kotlin.math.max

/**
 * Google Lens style in-place translation view.
 * Draws each translated text block over its original screen position.
 *
 * Interactions (no leaving the screen):
 * - Tap a block → toggle between translated and original text for that block
 * - Tap empty area → toggle a dim scrim that helps readability
 */
@SuppressLint("ViewConstructor")
class InPlaceTranslationView(
    context: Context,
    private val blocks: List<InPlaceBlock>,
    private val onCloseRequested: () -> Unit
) : View(context) {

    private val density = context.resources.displayMetrics.density

    // Per-block state: true = show translated, false = show original
    private val showTranslated = BooleanArray(blocks.size) { true }

    // Dim scrim behind text boxes for readability
    private var scrimEnabled = true

    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xF0131110.toInt() // Ink800 warm high alpha
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF7000.toInt() // Ember500
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.RIGHT // RTL Arabic
    }

    private val scrimPaint = Paint().apply {
        color = 0x66000000 // 40% black scrim
    }

    private val cornerRadius = 6f * density

    init {
        // Let touches outside boxes pass through is not trivial with WindowManager;
        // here we capture touches to allow block toggling and close button.
        isClickable = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Optional dim scrim across the whole screen for readability
        if (scrimEnabled) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)
        }

        blocks.forEachIndexed { index, block ->
            val box = block.boundingBox ?: return@forEachIndexed
            val text = if (showTranslated[index]) block.translatedText else block.originalText
            if (text.isBlank()) return@forEachIndexed

            drawBlock(canvas, box, text, showTranslated[index])
        }
    }

    private fun drawBlock(canvas: Canvas, box: Rect, text: String, translated: Boolean) {
        val padding = 6f * density
        val maxWidth = box.width().toFloat()
        val maxHeight = box.height().toFloat()

        // Auto-fit: shrink text size until wrapped lines fit within the box height.
        // Start from box-height-based size and reduce until it fits (or hits minimum).
        val minSize = 9f * density
        val maxSize = (box.height() * 0.7f).coerceIn(minSize, 30f * density)
        var chosenSize = maxSize
        var lines: List<String> = emptyList()

        var size = maxSize
        while (size >= minSize) {
            textPaint.textSize = size
            lines = wrapText(text, maxWidth)
            val totalHeight = lines.size * textPaint.fontSpacing
            // Allow text to grow up to ~1.6x the original box height before forcing smaller
            if (totalHeight <= maxHeight * 1.6f) {
                chosenSize = size
                break
            }
            chosenSize = size
            size -= 1f * density
        }
        textPaint.textSize = chosenSize
        lines = wrapText(text, maxWidth)
        textPaint.color = if (translated) Color.WHITE else 0xFF94A3B8.toInt()

        val lineHeight = textPaint.fontSpacing
        val contentHeight = lines.size * lineHeight

        // Background rounded box grows to fit the actual content height
        val rectF = RectF(
            box.left.toFloat() - padding,
            box.top.toFloat() - padding / 2,
            box.right.toFloat() + padding,
            box.top.toFloat() + contentHeight + padding / 2
        )
        boxPaint.color = if (translated) 0xF0131110.toInt() else 0xF01C1815.toInt()
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, boxPaint)
        borderPaint.color = if (translated) 0xFFFF7000.toInt() else 0xFF3A322C.toInt()
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, borderPaint)

        // Draw text (wrapped, RTL aligned to the right edge)
        var y = box.top + lineHeight * 0.82f
        val xRight = box.right.toFloat()
        for (line in lines) {
            canvas.drawText(line, xRight, y, textPaint)
            y += lineHeight
        }
    }

    /** Simple greedy word-wrap to fit the box width. */
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

        // Check if a block was tapped → toggle its language
        blocks.forEachIndexed { index, block ->
            val box = block.boundingBox ?: return@forEachIndexed
            val pad = 8 * density
            if (x >= box.left - pad && x <= box.right + pad &&
                y >= box.top - pad && y <= box.bottom + pad
            ) {
                showTranslated[index] = !showTranslated[index]
                invalidate()
                return true
            }
        }

        // Tapped empty area → toggle scrim
        scrimEnabled = !scrimEnabled
        invalidate()
        return true
    }

    fun blockCount(): Int = blocks.count { it.boundingBox != null }
}
