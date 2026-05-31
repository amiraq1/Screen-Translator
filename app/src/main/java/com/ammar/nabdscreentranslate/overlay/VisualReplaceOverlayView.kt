package com.ammar.nabdscreentranslate.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.PixelFormat
import androidx.core.content.res.ResourcesCompat
import com.ammar.nabdscreentranslate.R
import com.ammar.nabdscreentranslate.domain.InPlaceBlock

/**
 * Visual Replace overlay: covers original text with a background-matched rectangle
 * and renders the translated text in-place, simulating text replacement like Google Lens.
 *
 * Pipeline:
 * 1. Sample background color around each bounding box from the screenshot bitmap
 * 2. Draw a rounded cover rectangle with the sampled color
 * 3. Render translated text inside the cover with auto-fit sizing
 * 4. Tap a block → show popup with original + translation + copy
 * 5. Tap empty area → toggle overlay visibility (peek original)
 */
@SuppressLint("ViewConstructor")
class VisualReplaceOverlayView(
    context: Context,
    private val blocks: List<InPlaceBlock>,
    private val screenshotBitmap: Bitmap,
    private val windowManager: WindowManager,
    private val onCloseRequested: () -> Unit
) : View(context) {

    companion object {
        private const val TAG = "NabdScreenTranslate"
        private const val LIGHT_FALLBACK = 0xFFF7F7F7.toInt()
        private const val DARK_FALLBACK = 0xFF151517.toInt()
    }

    private val density = context.resources.displayMetrics.density
    @Suppress("DEPRECATION")
    private val scaledDensity = context.resources.displayMetrics.scaledDensity

    private val tajawal: Typeface? = runCatching {
        ResourcesCompat.getFont(context, R.font.tajawal_medium)
    }.getOrNull()

    // Padding around bounding box for the cover
    private val coverPadH = 6f * density
    private val coverPadV = 4f * density
    private val coverCorner = 6f * density

    // Text sizes to try (auto-fit)
    private val textSizes = floatArrayOf(18f, 16f, 14f, 12f, 10f)

    // Pre-computed cover data
    private data class CoverBlock(
        val index: Int,
        val coverRect: RectF,
        val bgColor: Int,
        val textColor: Int,
        val translatedText: String,
        val originalText: String,
        val textSize: Float,
        val textLayout: StaticLayout
    )

    private var coverBlocks: List<CoverBlock> = emptyList()
    private var overlayVisible = true
    private var popupView: View? = null

    // Background sampling results cached at construction
    private val sampledColors: Map<Int, Int>

    init {
        isClickable = true

        // Sample background colors from bitmap before it might be recycled
        val colors = mutableMapOf<Int, Int>()
        blocks.forEachIndexed { index, block ->
            block.boundingBox?.let { box ->
                colors[index] = sampleBackgroundColor(box)
            }
        }
        sampledColors = colors
        Log.d(TAG, "VisualReplace: sampled colors for ${colors.size} groups")
    }

    /**
     * Must be called after the view is measured (has width/height).
     * Builds the cover block layouts.
     */
    private fun buildCoverBlocks(): List<CoverBlock> {
        val w = width
        val h = height
        if (w == 0 || h == 0) return emptyList()

        val result = mutableListOf<CoverBlock>()

        blocks.forEachIndexed { index, block ->
            val box = block.boundingBox ?: return@forEachIndexed
            if (block.translatedText.isBlank()) return@forEachIndexed

            // Expand bounding box with padding
            val coverRect = RectF(
                (box.left - coverPadH).coerceAtLeast(0f),
                (box.top - coverPadV).coerceAtLeast(0f),
                (box.right + coverPadH).coerceAtMost(w.toFloat()),
                (box.bottom + coverPadV).coerceAtMost(h.toFloat())
            )

            // Get sampled background color
            val bgColor = sampledColors[index] ?: DARK_FALLBACK

            // Determine text color based on luminance
            val luminance = computeLuminance(bgColor)
            val textColor = if (luminance > 0.5f) 0xFF111111.toInt() else 0xFFFFFFFF.toInt()
            Log.d(TAG, "VisualReplace: group $index bg=${Integer.toHexString(bgColor)} lum=${"%.2f".format(luminance)} textColor=${if (luminance > 0.5f) "dark" else "light"}")

            // Auto-fit text size
            val innerWidth = (coverRect.width() - 2 * coverPadH).coerceAtLeast(20f)
            val innerHeight = (coverRect.height() - 2 * coverPadV).coerceAtLeast(14f)

            var bestSize = textSizes.last() * scaledDensity
            var bestLayout: StaticLayout? = null

            for (sp in textSizes) {
                val px = sp * scaledDensity
                val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = px
                    typeface = tajawal ?: Typeface.DEFAULT
                    color = textColor
                }

                val layout = StaticLayout.Builder
                    .obtain(block.translatedText, 0, block.translatedText.length, tp, innerWidth.toInt())
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1.15f)
                    .setMaxLines(getMaxLines(innerHeight, px))
                    .setEllipsize(android.text.TextUtils.TruncateAt.END)
                    .build()

                if (layout.height <= innerHeight || sp == textSizes.last()) {
                    bestSize = px
                    bestLayout = layout
                    Log.d(TAG, "VisualReplace: group $index auto-fit ${sp}sp, lines=${layout.lineCount}")
                    break
                }
            }

            if (bestLayout != null) {
                result.add(
                    CoverBlock(
                        index = index,
                        coverRect = coverRect,
                        bgColor = bgColor,
                        textColor = textColor,
                        translatedText = block.translatedText,
                        originalText = block.originalText,
                        textSize = bestSize,
                        textLayout = bestLayout
                    )
                )
            }
        }

        Log.d(TAG, "VisualReplace: ${result.size} cover blocks built")
        return result
    }

    private fun getMaxLines(availableHeight: Float, textSize: Float): Int {
        val lineHeight = textSize * 1.15f
        return (availableHeight / lineHeight).toInt().coerceIn(1, 5)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        coverBlocks = buildCoverBlocks()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!overlayVisible) return

        val coverPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        for (cb in coverBlocks) {
            // Draw cover rectangle
            coverPaint.color = cb.bgColor
            canvas.drawRoundRect(cb.coverRect, coverCorner, coverCorner, coverPaint)

            // Draw text inside cover
            canvas.save()
            // Position text inside the cover with padding, RTL aligned to right
            val textX = cb.coverRect.right - coverPadH - cb.textLayout.width
            val textY = cb.coverRect.top + coverPadV +
                ((cb.coverRect.height() - 2 * coverPadV - cb.textLayout.height) / 2f)
                    .coerceAtLeast(0f)
            canvas.translate(textX, textY)
            cb.textLayout.draw(canvas)
            canvas.restore()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) return true
        val x = event.x
        val y = event.y

        // Check if tap is on a cover block
        for (cb in coverBlocks) {
            if (cb.coverRect.contains(x, y)) {
                showBlockPopup(cb)
                return true
            }
        }

        // Tap empty area → toggle visibility (peek original)
        overlayVisible = !overlayVisible
        invalidate()
        return true
    }

    /**
     * Shows a small popup with original text, translation, and copy button.
     */
    private fun showBlockPopup(cb: CoverBlock) {
        dismissPopup()

        val d = density
        val popup = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((12 * d).toInt(), (10 * d).toInt(), (12 * d).toInt(), (10 * d).toInt())
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xF2151517.toInt())
                cornerRadius = 12 * d
                setStroke((1 * d).toInt(), 0xFF3A322C.toInt())
            }
            background = bg
            elevation = 16 * d
        }

        // Original text label
        popup.addView(TextView(context).apply {
            text = "النص الأصلي:"
            setTextColor(0xFFA89B90.toInt())
            textSize = 10f
        })
        popup.addView(TextView(context).apply {
            text = cb.originalText
            setTextColor(0xFFFBF7F4.toInt())
            textSize = 13f
            maxLines = 3
            setPadding(0, (2 * d).toInt(), 0, (8 * d).toInt())
        })

        // Translation label
        popup.addView(TextView(context).apply {
            text = "الترجمة:"
            setTextColor(0xFFFF7000.toInt())
            textSize = 10f
        })
        popup.addView(TextView(context).apply {
            text = cb.translatedText
            setTextColor(0xFFFBF7F4.toInt())
            textSize = 14f
            typeface = tajawal
            maxLines = 4
            setPadding(0, (2 * d).toInt(), 0, (8 * d).toInt())
        })

        // Copy button
        popup.addView(TextView(context).apply {
            text = "نسخ"
            setTextColor(0xFFFF7000.toInt())
            textSize = 12f
            setPadding((14 * d).toInt(), (6 * d).toInt(), (14 * d).toInt(), (6 * d).toInt())
            val btnBg = android.graphics.drawable.GradientDrawable().apply {
                setColor(0x18FFFFFF)
                cornerRadius = 8 * d
                setStroke((1 * d).toInt(), 0x30FF7000)
            }
            background = btnBg
            setOnClickListener {
                val cb2 = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cb2.setPrimaryClip(ClipData.newPlainText("translation", cb.translatedText))
                Toast.makeText(context, "تم نسخ الترجمة", Toast.LENGTH_SHORT).show()
                dismissPopup()
            }
        })

        // Position popup near the tapped block
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = (width - cb.coverRect.right + coverPadH).toInt().coerceAtLeast(16)
            y = cb.coverRect.bottom.toInt() + (8 * d).toInt()
        }

        try {
            windowManager.addView(popup, params)
            popupView = popup

            // Auto-dismiss after 5 seconds
            popup.postDelayed({ dismissPopup() }, 5000)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show popup: ${e.message}")
        }
    }

    fun dismissPopup() {
        popupView?.let { v ->
            try {
                if (v.isAttachedToWindow) windowManager.removeView(v)
            } catch (_: Exception) {}
        }
        popupView = null
    }

    /**
     * Samples background color around a bounding box from the screenshot bitmap.
     * Takes 8 sample points around the edges and computes average.
     */
    private fun sampleBackgroundColor(box: Rect): Int {
        if (screenshotBitmap.isRecycled) return DARK_FALLBACK

        val bw = screenshotBitmap.width
        val bh = screenshotBitmap.height

        // Sample points around the box (just outside the edges)
        val margin = (4 * density).toInt()
        val samplePoints = listOf(
            // Top-left
            Pair((box.left - margin).coerceIn(0, bw - 1), (box.top - margin).coerceIn(0, bh - 1)),
            // Top-right
            Pair((box.right + margin).coerceIn(0, bw - 1), (box.top - margin).coerceIn(0, bh - 1)),
            // Bottom-left
            Pair((box.left - margin).coerceIn(0, bw - 1), (box.bottom + margin).coerceIn(0, bh - 1)),
            // Bottom-right
            Pair((box.right + margin).coerceIn(0, bw - 1), (box.bottom + margin).coerceIn(0, bh - 1)),
            // Center-top
            Pair(((box.left + box.right) / 2).coerceIn(0, bw - 1), (box.top - margin).coerceIn(0, bh - 1)),
            // Center-bottom
            Pair(((box.left + box.right) / 2).coerceIn(0, bw - 1), (box.bottom + margin).coerceIn(0, bh - 1)),
            // Center-left
            Pair((box.left - margin).coerceIn(0, bw - 1), ((box.top + box.bottom) / 2).coerceIn(0, bh - 1)),
            // Center-right
            Pair((box.right + margin).coerceIn(0, bw - 1), ((box.top + box.bottom) / 2).coerceIn(0, bh - 1))
        )

        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var count = 0

        for ((px, py) in samplePoints) {
            try {
                val pixel = screenshotBitmap.getPixel(px, py)
                rSum += Color.red(pixel)
                gSum += Color.green(pixel)
                bSum += Color.blue(pixel)
                count++
            } catch (_: Exception) {
                // Skip invalid pixels
            }
        }

        if (count == 0) return DARK_FALLBACK

        val avgR = (rSum / count).toInt()
        val avgG = (gSum / count).toInt()
        val avgB = (bSum / count).toInt()

        // Determine alpha based on luminance
        val luminance = (0.299f * avgR + 0.587f * avgG + 0.114f * avgB) / 255f
        val alpha = if (luminance > 0.5f) 0xF0 else 0xF5 // 0.94 for light, 0.96 for dark

        Log.d(TAG, "VisualReplace: background sampled rgb($avgR,$avgG,$avgB) lum=${"%.2f".format(luminance)}")
        return Color.argb(alpha, avgR, avgG, avgB)
    }

    /**
     * Computes relative luminance of a color (0=dark, 1=light).
     */
    private fun computeLuminance(color: Int): Float {
        val r = Color.red(color) / 255f
        val g = Color.green(color) / 255f
        val b = Color.blue(color) / 255f
        return 0.299f * r + 0.587f * g + 0.114f * b
    }
}
