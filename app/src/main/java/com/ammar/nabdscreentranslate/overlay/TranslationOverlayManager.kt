package com.ammar.nabdscreentranslate.overlay

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class TranslationOverlayManager(
    private val context: Context,
    private val windowManager: WindowManager
) {

    private var overlayView: View? = null
    private var isVisible = false

    @SuppressLint("InflateParams")
    fun showTranslation(
        originalText: String,
        translatedText: String,
        onCopy: () -> Unit,
        onSave: () -> Unit,
        onClose: () -> Unit
    ) {
        Log.d("NabdScreenTranslate", "Showing translation overlay")
        hide() // Remove existing overlay

        val view = createTranslationView(originalText, translatedText, onCopy, onSave, onClose)
        overlayView = view

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 100
        }

        windowManager.addView(view, params)
        isVisible = true
    }

    fun showError(message: String) {
        hide()

        val view = createErrorView(message)
        overlayView = view

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 100
        }

        windowManager.addView(view, params)
        isVisible = true

        // Auto-dismiss error after 3 seconds
        view.postDelayed({ hide() }, 3000)
    }

    fun hide() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
        }
        overlayView = null
        isVisible = false
    }

    fun toggleVisibility() {
        overlayView?.let {
            if (it.visibility == View.VISIBLE) {
                it.visibility = View.GONE
                isVisible = false
            } else {
                it.visibility = View.VISIBLE
                isVisible = true
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun createTranslationView(
        originalText: String,
        translatedText: String,
        onCopy: () -> Unit,
        onSave: () -> Unit,
        onClose: () -> Unit
    ): View {
        val density = context.resources.displayMetrics.density
        val padding = (16 * density).toInt()
        val smallPadding = (8 * density).toInt()
        val cornerRadius = 16 * density

        // Main container
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
            layoutDirection = View.LAYOUT_DIRECTION_RTL

            // Background with rounded corners
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xF0161B22.toInt()) // Dark surface with high opacity
                setCornerRadii(floatArrayOf(
                    cornerRadius, cornerRadius, cornerRadius, cornerRadius,
                    0f, 0f, 0f, 0f
                ))
                setStroke((1 * density).toInt(), 0xFF30363D.toInt())
            }
            background = bg
            elevation = 8 * density
        }

        // Header with close button
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val titleText = TextView(context).apply {
            text = "تمت الترجمة بنجاح ✓"
            setTextColor(0xFF3FB950.toInt()) // AccentGreen
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        header.addView(titleText)

        val closeBtn = TextView(context).apply {
            text = "✕"
            setTextColor(0xFF8B949E.toInt())
            textSize = 18f
            setPadding(smallPadding, 0, smallPadding, 0)
            setOnClickListener {
                onClose()
            }
        }
        header.addView(closeBtn)
        container.addView(header)

        // Divider
        val divider = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (1 * density).toInt()
            ).apply {
                topMargin = smallPadding
                bottomMargin = smallPadding
            }
            setBackgroundColor(0xFF30363D.toInt())
        }
        container.addView(divider)

        // Scrollable content
        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = smallPadding
            }
            minimumHeight = 0
        }

        val contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Original text (collapsed)
        if (originalText.length <= 200) {
            val originalLabel = TextView(context).apply {
                text = "النص الأصلي:"
                setTextColor(0xFF8B949E.toInt())
                textSize = 11f
            }
            contentLayout.addView(originalLabel)

            val originalTextView = TextView(context).apply {
                text = originalText
                setTextColor(0xFFB0B8C4.toInt())
                textSize = 12f
                maxLines = 3
                setPadding(0, (4 * density).toInt(), 0, smallPadding)
            }
            contentLayout.addView(originalTextView)
        }

        // Translated text
        val translatedLabel = TextView(context).apply {
            text = "الترجمة:"
            setTextColor(0xFF58A6FF.toInt()) // PrimaryBlue
            textSize = 11f
        }
        contentLayout.addView(translatedLabel)

        val translatedTextView = TextView(context).apply {
            text = translatedText
            setTextColor(0xFFF0F6FC.toInt()) // TextPrimary
            textSize = 15f
            setPadding(0, (4 * density).toInt(), 0, 0)
            setLineSpacing(4 * density, 1f)
        }
        contentLayout.addView(translatedTextView)

        scrollView.addView(contentLayout)
        container.addView(scrollView)

        // Action buttons
        val buttonsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (12 * density).toInt()
            }
        }

        val copyBtn = createActionButton("نسخ", 0xFF58A6FF.toInt()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("translation", translatedText)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "تم النسخ", Toast.LENGTH_SHORT).show()
            onCopy()
        }
        buttonsLayout.addView(copyBtn)

        val saveBtn = createActionButton("حفظ", 0xFF3FB950.toInt()) {
            onSave()
            Toast.makeText(context, "تم الحفظ", Toast.LENGTH_SHORT).show()
        }
        buttonsLayout.addView(saveBtn)

        container.addView(buttonsLayout)

        return container
    }

    private fun createErrorView(message: String): View {
        val density = context.resources.displayMetrics.density
        val padding = (16 * density).toInt()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(padding, padding, padding, padding)
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL

            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xF0161B22.toInt())
                cornerRadius = 12 * density
                setStroke((1 * density).toInt(), 0xFFF85149.toInt())
            }
            background = bg
            elevation = 8 * density
        }

        val errorIcon = TextView(context).apply {
            text = "⚠️"
            textSize = 18f
            setPadding(0, 0, (8 * density).toInt(), 0)
        }
        container.addView(errorIcon)

        val errorText = TextView(context).apply {
            text = message
            setTextColor(0xFFF0F6FC.toInt())
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        container.addView(errorText)

        val closeBtn = TextView(context).apply {
            text = "✕"
            setTextColor(0xFF8B949E.toInt())
            textSize = 16f
            setPadding((8 * density).toInt(), 0, 0, 0)
            setOnClickListener { hide() }
        }
        container.addView(closeBtn)

        return container
    }

    private fun createActionButton(text: String, color: Int, onClick: () -> Unit): TextView {
        val density = context.resources.displayMetrics.density
        return TextView(context).apply {
            this.text = text
            setTextColor(color)
            textSize = 13f
            setPadding(
                (12 * density).toInt(),
                (8 * density).toInt(),
                (12 * density).toInt(),
                (8 * density).toInt()
            )
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(0x20FFFFFF)
                cornerRadius = 8 * density
            }
            background = bg
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = (8 * density).toInt()
            }
            setOnClickListener { onClick() }
        }
    }
}
