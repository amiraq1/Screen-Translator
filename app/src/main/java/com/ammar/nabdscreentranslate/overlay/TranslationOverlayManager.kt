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

/**
 * Manages the floating translation result overlay.
 * Uses View-based UI since it's rendered via WindowManager outside of Compose.
 * Design: Dark Liquid Lens - glass card with cyan accents.
 */
class TranslationOverlayManager(
    private val context: Context,
    private val windowManager: WindowManager
) {

    private var overlayView: View? = null
    private var isVisible = false

    // Colors matching Dark Liquid Lens theme
    private val colorBg = 0xF00F1115.toInt()        // Ink800 with high alpha
    private val colorBorder = 0xFF2E3442.toInt()     // GlassBorder
    private val colorCyan = 0xFF22D3EE.toInt()       // Cyan400
    private val colorSuccess = 0xFF4ADE80.toInt()    // Success400
    private val colorError = 0xFFF87171.toInt()      // Error400
    private val colorTextWhite = 0xFFF1F5F9.toInt()  // TextWhite
    private val colorTextMuted = 0xFF94A3B8.toInt()  // TextMuted
    private val colorTextDim = 0xFF64748B.toInt()    // TextDim
    private val colorGlass = 0xFF1A1E28.toInt()      // Glass800

    @SuppressLint("InflateParams")
    fun showTranslation(
        originalText: String,
        translatedText: String,
        onCopy: () -> Unit,
        onSave: () -> Unit,
        onClose: () -> Unit
    ) {
        Log.d("NabdScreenTranslate", "Showing translation overlay")
        hide()

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
            y = 80
        }

        try {
            windowManager.addView(view, params)
            isVisible = true
        } catch (e: Exception) {
            Log.e("NabdScreenTranslate", "Failed to show overlay: ${e.message}")
        }
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
            y = 80
        }

        try {
            windowManager.addView(view, params)
            isVisible = true
            view.postDelayed({ hide() }, 4000)
        } catch (e: Exception) {
            Log.e("NabdScreenTranslate", "Failed to show error overlay: ${e.message}")
        }
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
        val d = context.resources.displayMetrics.density
        val pad = (16 * d).toInt()
        val padSm = (10 * d).toInt()
        val radius = 16 * d

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(colorBg)
                setCornerRadii(floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f))
                setStroke((1 * d).toInt(), colorBorder)
            }
            background = bg
            elevation = 12 * d
        }

        // Header
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        header.addView(TextView(context).apply {
            text = "تمت الترجمة ✓"
            setTextColor(colorSuccess)
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        header.addView(TextView(context).apply {
            text = "✕"
            setTextColor(colorTextDim)
            textSize = 18f
            setPadding(padSm, 0, padSm, 0)
            setOnClickListener { onClose() }
        })
        container.addView(header)

        // Divider
        container.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (1 * d).toInt()).apply {
                topMargin = padSm; bottomMargin = padSm
            }
            setBackgroundColor(colorBorder)
        })

        // Content scroll
        val scroll = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (4 * d).toInt()
            }
            minimumHeight = 0
        }

        val content = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

        // Original text (show if short)
        if (originalText.length <= 150) {
            content.addView(TextView(context).apply {
                text = "النص الأصلي"
                setTextColor(colorTextDim)
                textSize = 10f
            })
            content.addView(TextView(context).apply {
                text = originalText
                setTextColor(colorTextMuted)
                textSize = 12f
                maxLines = 2
                setPadding(0, (3 * d).toInt(), 0, padSm)
            })
        }

        // Translation
        content.addView(TextView(context).apply {
            text = "الترجمة"
            setTextColor(colorCyan)
            textSize = 10f
        })
        content.addView(TextView(context).apply {
            text = translatedText
            setTextColor(colorTextWhite)
            textSize = 15f
            setPadding(0, (4 * d).toInt(), 0, 0)
            setLineSpacing(4 * d, 1f)
        })

        scroll.addView(content)
        container.addView(scroll)

        // Buttons
        val buttons = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (12 * d).toInt()
            }
        }

        buttons.addView(makeBtn("نسخ", colorCyan, d) {
            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cb.setPrimaryClip(ClipData.newPlainText("translation", translatedText))
            Toast.makeText(context, "تم النسخ", Toast.LENGTH_SHORT).show()
            onCopy()
        })

        buttons.addView(makeBtn("حفظ", colorSuccess, d) {
            onSave()
            Toast.makeText(context, "تم الحفظ", Toast.LENGTH_SHORT).show()
        })

        container.addView(buttons)
        return container
    }

    private fun createErrorView(message: String): View {
        val d = context.resources.displayMetrics.density
        val pad = (14 * d).toInt()

        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(pad, pad, pad, pad)
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(colorBg)
                cornerRadius = 12 * d
                setStroke((1 * d).toInt(), colorError)
            }
            background = bg
            elevation = 12 * d

            addView(TextView(context).apply {
                text = "⚠"
                textSize = 16f
                setPadding(0, 0, (8 * d).toInt(), 0)
            })

            addView(TextView(context).apply {
                text = message
                setTextColor(colorTextWhite)
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })

            addView(TextView(context).apply {
                text = "✕"
                setTextColor(colorTextDim)
                textSize = 16f
                setPadding((8 * d).toInt(), 0, 0, 0)
                setOnClickListener { hide() }
            })
        }
    }

    private fun makeBtn(text: String, color: Int, d: Float, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(color)
            textSize = 12f
            setPadding((14 * d).toInt(), (8 * d).toInt(), (14 * d).toInt(), (8 * d).toInt())
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(0x18FFFFFF)
                cornerRadius = 8 * d
                setStroke((1 * d).toInt(), (color and 0x00FFFFFF) or 0x30000000)
            }
            background = bg
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = (8 * d).toInt()
            }
            setOnClickListener { onClick() }
        }
    }
}
