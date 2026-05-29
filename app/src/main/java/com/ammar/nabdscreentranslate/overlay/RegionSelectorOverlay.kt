package com.ammar.nabdscreentranslate.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Overlay that allows the user to select a region of the screen for translation.
 */
class RegionSelectorOverlay(
    private val context: Context,
    private val windowManager: WindowManager,
    private val onRegionSelected: (Rect?) -> Unit
) {

    private var overlayView: View? = null

    fun show() {
        val view = RegionSelectorView(context) { rect ->
            onRegionSelected(rect)
        }
        overlayView = view

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        windowManager.addView(view, params)
    }

    fun remove() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
        }
        overlayView = null
    }
}

@SuppressLint("ViewConstructor")
private class RegionSelectorView(
    context: Context,
    private val onRegionSelected: (Rect?) -> Unit
) : FrameLayout(context) {

    private var startX = 0f
    private var startY = 0f
    private var endX = 0f
    private var endY = 0f
    private var isDrawing = false
    private var hasSelection = false

    private val selectionPaint = Paint().apply {
        color = Color.argb(60, 88, 166, 255) // PrimaryBlue with alpha
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint().apply {
        color = Color.argb(200, 88, 166, 255)
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val dimPaint = Paint().apply {
        color = Color.argb(120, 0, 0, 0)
        style = Paint.Style.FILL
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    init {
        setWillNotDraw(false)

        // Add instruction text and buttons at the top
        addInstructionBar()
    }

    private fun addInstructionBar() {
        val density = context.resources.displayMetrics.density

        val topBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(
                (16 * density).toInt(),
                (40 * density).toInt(),
                (16 * density).toInt(),
                (8 * density).toInt()
            )
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP
            }
        }

        val instructionText = TextView(context).apply {
            text = "اسحب لتحديد منطقة الترجمة"
            setTextColor(Color.WHITE)
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            gravity = android.view.Gravity.CENTER
        }
        topBar.addView(instructionText)

        val cancelBtn = TextView(context).apply {
            text = "إلغاء"
            setTextColor(Color.argb(255, 248, 81, 73)) // AccentRed
            textSize = 14f
            setPadding(
                (12 * density).toInt(),
                (8 * density).toInt(),
                (12 * density).toInt(),
                (8 * density).toInt()
            )
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.argb(40, 248, 81, 73))
                cornerRadius = 8 * density
            }
            background = bg
            setOnClickListener {
                onRegionSelected(null)
            }
        }
        topBar.addView(cancelBtn)

        addView(topBar)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                endX = event.x
                endY = event.y
                isDrawing = true
                hasSelection = false
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDrawing) {
                    endX = event.x
                    endY = event.y
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (isDrawing) {
                    endX = event.x
                    endY = event.y
                    isDrawing = false
                    hasSelection = true
                    invalidate()

                    // Calculate the selected region
                    val left = minOf(startX, endX).toInt()
                    val top = minOf(startY, endY).toInt()
                    val right = maxOf(startX, endX).toInt()
                    val bottom = maxOf(startY, endY).toInt()

                    // Minimum selection size
                    if (right - left > 50 && bottom - top > 50) {
                        val rect = Rect(left, top, right, bottom)
                        // Small delay to show selection before processing
                        postDelayed({ onRegionSelected(rect) }, 200)
                    } else {
                        hasSelection = false
                        invalidate()
                    }
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw dim overlay
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)

        if (isDrawing || hasSelection) {
            val left = minOf(startX, endX)
            val top = minOf(startY, endY)
            val right = maxOf(startX, endX)
            val bottom = maxOf(startY, endY)

            // Clear the selected region (make it brighter)
            canvas.drawRect(left, top, right, bottom, selectionPaint)
            canvas.drawRect(left, top, right, bottom, borderPaint)
        }

        if (!isDrawing && !hasSelection) {
            // Draw instruction
            canvas.drawText(
                "↕ اسحب لتحديد المنطقة",
                width / 2f,
                height / 2f,
                textPaint
            )
        }
    }
}
