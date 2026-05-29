package com.ammar.nabdscreentranslate.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ProgressBar
import kotlin.math.abs

/**
 * Floating action button rendered as a View (not Compose) for WindowManager overlay.
 * Design: Dark Liquid Lens - dark circle with cyan border and translate icon.
 */
@SuppressLint("ViewConstructor")
class FloatingButtonView(context: Context) : FrameLayout(context) {

    enum class EventType {
        SINGLE_TAP, LONG_PRESS, DOUBLE_TAP
    }

    private val BUTTON_SIZE_DP = 52
    private val buttonSizePx: Int
    private val density: Float = context.resources.displayMetrics.density

    private var progressBar: ProgressBar? = null
    private val handler = Handler(Looper.getMainLooper())

    // Touch tracking
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private val CLICK_THRESHOLD = 10

    // Long press
    private var longPressRunnable: Runnable? = null
    private val LONG_PRESS_TIMEOUT = 500L
    private var isLongPressTriggered = false

    // Colors - Dark Liquid Lens
    private val colorBg = 0xFF0F1115.toInt()       // Ink800
    private val colorBorder = 0xFF22D3EE.toInt()   // Cyan400
    private val colorIcon = 0xFF22D3EE.toInt()     // Cyan400

    init {
        buttonSizePx = (BUTTON_SIZE_DP * density).toInt()

        val bg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(colorBg)
            setStroke((2 * density).toInt(), colorBorder)
        }
        setBackground(bg)
        elevation = 10 * density

        // Progress bar for loading state
        progressBar = ProgressBar(context).apply {
            layoutParams = LayoutParams(
                (20 * density).toInt(),
                (20 * density).toInt()
            ).apply {
                gravity = android.view.Gravity.CENTER
            }
            visibility = View.GONE
            isIndeterminate = true
        }
        addView(progressBar)

        layoutParams = LayoutParams(buttonSizePx, buttonSizePx)
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (progressBar?.visibility == View.VISIBLE) return

        // Draw translate icon "ت" centered
        val paint = Paint().apply {
            color = colorIcon
            textSize = buttonSizePx * 0.38f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
        val x = width / 2f
        val y = (height / 2f) - ((paint.descent() + paint.ascent()) / 2f)
        canvas.drawText("ت", x, y, paint)
    }

    fun setLoading(loading: Boolean) {
        progressBar?.visibility = if (loading) View.VISIBLE else View.GONE
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setupTouchListener(
        params: WindowManager.LayoutParams,
        windowManager: WindowManager,
        onEvent: (EventType) -> Unit
    ) {
        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = false
                    isLongPressTriggered = false
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY

                    longPressRunnable = Runnable {
                        if (!isDragging) {
                            isLongPressTriggered = true
                            onEvent(EventType.LONG_PRESS)
                        }
                    }
                    handler.postDelayed(longPressRunnable!!, LONG_PRESS_TIMEOUT)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY

                    if (abs(dx) > CLICK_THRESHOLD || abs(dy) > CLICK_THRESHOLD) {
                        isDragging = true
                        longPressRunnable?.let { handler.removeCallbacks(it) }
                    }

                    if (isDragging) {
                        params.x = initialX + dx.toInt()
                        params.y = initialY + dy.toInt()
                        try {
                            windowManager.updateViewLayout(this, params)
                        } catch (_: Exception) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    longPressRunnable?.let { handler.removeCallbacks(it) }
                    if (!isDragging && !isLongPressTriggered) {
                        onEvent(EventType.SINGLE_TAP)
                    }
                    true
                }
                else -> false
            }
        }
    }
}
