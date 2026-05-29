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

@SuppressLint("ViewConstructor")
class FloatingButtonView(context: Context) : FrameLayout(context) {

    enum class EventType {
        SINGLE_TAP, LONG_PRESS, DOUBLE_TAP
    }

    private val BUTTON_SIZE = 56 // dp
    private val buttonSizePx: Int

    private var progressBar: ProgressBar? = null
    private val handler = Handler(Looper.getMainLooper())

    // Touch tracking
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private val CLICK_THRESHOLD = 10 // pixels

    // Long press
    private var longPressRunnable: Runnable? = null
    private val LONG_PRESS_TIMEOUT = 500L
    private var isLongPressTriggered = false

    init {
        val density = context.resources.displayMetrics.density
        buttonSizePx = (BUTTON_SIZE * density).toInt()

        // Create circular button background
        val background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0xFF6366F1.toInt()) // PrimaryGradientStart
            setStroke((2 * density).toInt(), 0xFF8B5CF6.toInt())
        }

        setBackground(background)
        elevation = 8 * density

        // Create a simple progress bar for loading state
        progressBar = ProgressBar(context).apply {
            layoutParams = LayoutParams(
                (24 * density).toInt(),
                (24 * density).toInt()
            ).apply {
                gravity = android.view.Gravity.CENTER
            }
            visibility = View.GONE
            isIndeterminate = true
        }
        addView(progressBar)

        layoutParams = LayoutParams(buttonSizePx, buttonSizePx)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (progressBar?.visibility == View.VISIBLE) return

        // Draw translate icon (simple "T" text)
        val paint = Paint().apply {
            color = 0xFFFFFFFF.toInt()
            textSize = buttonSizePx * 0.4f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }

        val xPos = width / 2f
        val yPos = (height / 2f) - ((paint.descent() + paint.ascent()) / 2f)
        canvas.drawText("ت", xPos, yPos, paint)
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

                    // Start long press timer
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
                        windowManager.updateViewLayout(this, params)
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
