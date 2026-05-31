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
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.ammar.nabdscreentranslate.domain.InPlaceBlock

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
    private var inPlaceView: View? = null
    private var inPlaceCloseButton: View? = null
    private var bottomSheetView: View? = null
    private var visualReplaceView: VisualReplaceOverlayView? = null
    private var isVisible = false

    // Colors matching Ember on Graphite theme
    private val colorBg = 0xF2151517.toInt()         // opaque dark graphite (~95%)
    private val colorBorder = 0xFF3A322C.toInt()     // GlassBorder
    private val colorAccent = 0xFFFF7000.toInt()     // Ember500 (primary accent — highlights only)
    private val colorSuccess = 0xFF4ADE80.toInt()    // Success400
    private val colorError = 0xFFF87171.toInt()      // Error400
    private val colorTextWhite = 0xFFFBF7F4.toInt()  // TextWhite (warm)
    private val colorTextMuted = 0xFFA89B90.toInt()  // TextMuted (warm)
    private val colorTextDim = 0xFF6F645B.toInt()    // TextDim (warm)
    private val colorGlass = 0xFF1C1815.toInt()      // Glass800 (warm)

    @SuppressLint("InflateParams")
    fun showInPlaceTranslation(
        blocks: List<InPlaceBlock>,
        onCopy: () -> Unit,
        onSave: () -> Unit,
        onClose: () -> Unit
    ) {
        Log.d("NabdScreenTranslate", "Showing inline overlay translation (${blocks.size} blocks)")
        hide()

        val d = context.resources.displayMetrics.density

        // Full-screen view that draws opaque graphite bubbles near each block
        val inPlace = InPlaceTranslationView(context, blocks) { hide(); onClose() }
        inPlaceView = inPlace

        val inPlaceParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        // Small, subtle control pill (copy / save / hide / overflow indicator)
        val actionBar = buildControlPill(blocks, onCopy, onSave, onClose, d, hasOverflow = false)

        val actionParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = (32 * d).toInt()
        }
        inPlaceCloseButton = actionBar

        try {
            windowManager.addView(inPlace, inPlaceParams)
            windowManager.addView(actionBar, actionParams)
            isVisible = true
            Log.d("NabdScreenTranslate", "Inline overlay shown with ${blocks.count { it.boundingBox != null }} bubbles")
        } catch (e: Exception) {
            Log.e("NabdScreenTranslate", "Failed to show inline overlay: ${e.message}")
        }
    }

    /**
     * Show inline translation with overflow indicator when there are more
     * items available in the bottom sheet.
     */
    @SuppressLint("InflateParams")
    fun showInPlaceTranslationWithOverflow(
        blocks: List<InPlaceBlock>,
        overflowCount: Int,
        onCopy: () -> Unit,
        onSave: () -> Unit,
        onClose: () -> Unit
    ) {
        Log.d("NabdScreenTranslate", "Showing inline overlay (${blocks.size} bubbles, $overflowCount overflow)")
        hide()

        val d = context.resources.displayMetrics.density

        val inPlace = InPlaceTranslationView(context, blocks) { hide(); onClose() }
        inPlaceView = inPlace

        val inPlaceParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        val actionBar = buildControlPill(blocks, onCopy, onSave, onClose, d, hasOverflow = overflowCount > 0)
        val actionParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = (32 * d).toInt()
        }
        inPlaceCloseButton = actionBar

        try {
            windowManager.addView(inPlace, inPlaceParams)
            windowManager.addView(actionBar, actionParams)
            isVisible = true
            Log.d("NabdScreenTranslate", "Inline overlay shown: ${blocks.count { it.boundingBox != null }} bubbles, $overflowCount in sheet")
        } catch (e: Exception) {
            Log.e("NabdScreenTranslate", "Failed to show inline overlay: ${e.message}")
        }
    }

    /**
     * Bottom Sheet mode: shows ALL detected translations in a single clean,
     * scrollable panel anchored to the bottom of the screen.
     */
    @SuppressLint("InflateParams")
    fun showBottomSheetTranslation(
        blocks: List<InPlaceBlock>,
        onCopy: () -> Unit,
        onSave: () -> Unit,
        onClose: () -> Unit
    ) {
        Log.d("NabdScreenTranslate", "Showing bottom-sheet translation (${blocks.size} blocks)")
        // Keep any inline overlay (for "both" mode); only replace an existing sheet/card.
        removeBottomSheet()

        val view = buildBottomSheet(blocks, onCopy, onSave) {
            removeBottomSheet(); onClose()
        }
        bottomSheetView = view

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        }

        try {
            windowManager.addView(view, params)
            isVisible = true
        } catch (e: Exception) {
            Log.e("NabdScreenTranslate", "Failed to show bottom sheet: ${e.message}")
        }
    }

    /**
     * Visual Replace mode: covers original text with background-matched rectangles
     * and renders translated text in-place, simulating Google Lens style replacement.
     *
     * @param screenshotBitmap The captured screenshot for background color sampling.
     *                         This bitmap will NOT be recycled by this method.
     */
    @SuppressLint("InflateParams")
    fun showVisualReplaceTranslation(
        blocks: List<InPlaceBlock>,
        screenshotBitmap: android.graphics.Bitmap,
        onCopy: () -> Unit,
        onSave: () -> Unit,
        onClose: () -> Unit
    ) {
        Log.d("NabdScreenTranslate", "VisualReplace mode enabled, groups count = ${blocks.size}")
        hide()

        val d = context.resources.displayMetrics.density

        val vrView = VisualReplaceOverlayView(context, blocks, screenshotBitmap, windowManager) {
            hide(); onClose()
        }
        visualReplaceView = vrView

        val vrParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        // Control pill for copy/save/hide
        val actionBar = buildControlPill(blocks, onCopy, onSave, onClose, d, hasOverflow = false)
        val actionParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = (32 * d).toInt()
        }
        inPlaceCloseButton = actionBar

        try {
            windowManager.addView(vrView, vrParams)
            windowManager.addView(actionBar, actionParams)
            isVisible = true
            Log.d("NabdScreenTranslate", "VisualReplace: visual replace shown with ${blocks.count { it.boundingBox != null }} groups")
        } catch (e: Exception) {
            Log.e("NabdScreenTranslate", "Failed to show visual replace overlay: ${e.message}")
        }
    }

    /** Small compact control pill used by inline overlay mode. */
    private fun buildControlPill(
        blocks: List<InPlaceBlock>,
        onCopy: () -> Unit,
        onSave: () -> Unit,
        onClose: () -> Unit,
        d: Float,
        hasOverflow: Boolean = false
    ): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((6 * d).toInt(), (4 * d).toInt(), (6 * d).toInt(), (4 * d).toInt())
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xCC131110.toInt())
                cornerRadius = 22 * d
                setStroke((1 * d).toInt(), 0x33FFFFFF)
            }
            background = bg
            elevation = 12 * d
            alpha = 0.92f

            if (hasOverflow) {
                addView(TextView(context).apply {
                    text = "↓ المزيد"
                    setTextColor(0xFFFFC107.toInt())
                    textSize = 11f
                    setPadding((10 * d).toInt(), (6 * d).toInt(), (10 * d).toInt(), (6 * d).toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginStart = (3 * d).toInt()
                        marginEnd = (3 * d).toInt()
                    }
                })
            }

            addView(makeIconAction("نسخ", colorAccent, d) {
                copyAll(blocks); onCopy()
            })
            addView(makeIconAction("حفظ", colorSuccess, d) {
                onSave()
                Toast.makeText(context, "تم الحفظ", Toast.LENGTH_SHORT).show()
            })
            addView(makeIconAction("إخفاء", colorTextMuted, d) {
                hide(); onClose()
            })
        }
    }

    private fun copyAll(blocks: List<InPlaceBlock>) {
        val allText = blocks.joinToString("\n") { it.translatedText }
        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText("translation", allText))
        Toast.makeText(context, "تم نسخ الترجمة", Toast.LENGTH_SHORT).show()
    }

    /** Builds the clean bottom-sheet panel listing all translated blocks. */
    @SuppressLint("SetTextI18n")
    private fun buildBottomSheet(
        blocks: List<InPlaceBlock>,
        onCopy: () -> Unit,
        onSave: () -> Unit,
        onClose: () -> Unit
    ): View {
        val d = context.resources.displayMetrics.density
        val pad = (16 * d).toInt()
        val padSm = (10 * d).toInt()
        val radius = 20 * d
        val tajawal = runCatching {
            androidx.core.content.res.ResourcesCompat.getFont(context, com.ammar.nabdscreentranslate.R.font.tajawal_medium)
        }.getOrNull()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, padSm, pad, pad)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(colorBg)
                setCornerRadii(floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f))
                setStroke((1 * d).toInt(), colorBorder)
            }
            background = bg
            elevation = 16 * d
        }

        // Grab handle
        container.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams((40 * d).toInt(), (4 * d).toInt()).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = padSm
            }
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(colorTextDim)
                cornerRadius = 2 * d
            }
        })

        // Header row
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        header.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams((3 * d).toInt(), (16 * d).toInt()).apply { marginEnd = (8 * d).toInt() }
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(colorAccent); cornerRadius = 2 * d
            }
        })
        header.addView(TextView(context).apply {
            text = "الترجمة"
            setTextColor(colorTextWhite)
            textSize = 15f
            typeface = tajawal
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

        // Scrollable list of translations (cap height ~45% screen)
        val maxH = (context.resources.displayMetrics.heightPixels * 0.45f).toInt()
        val scroll = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val list = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

        val translatedBlocks = blocks.filter { it.translatedText.isNotBlank() }
        if (translatedBlocks.isEmpty()) {
            list.addView(TextView(context).apply {
                text = "لم يتم العثور على نص واضح"
                setTextColor(colorTextMuted)
                textSize = 14f
                typeface = tajawal
            })
        } else {
            translatedBlocks.forEachIndexed { i, block ->
                list.addView(TextView(context).apply {
                    text = block.translatedText
                    setTextColor(colorTextWhite)
                    textSize = 15f
                    typeface = tajawal
                    textDirection = View.TEXT_DIRECTION_RTL
                    textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                    setLineSpacing(5 * d, 1f)
                    val bg = android.graphics.drawable.GradientDrawable().apply {
                        setColor(colorGlass)
                        cornerRadius = 12 * d
                    }
                    background = bg
                    setPadding((12 * d).toInt(), (10 * d).toInt(), (12 * d).toInt(), (10 * d).toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { if (i > 0) topMargin = (8 * d).toInt() }
                })
            }
        }
        scroll.addView(list)
        scroll.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        // enforce max height
        container.addView(scroll)
        scroll.viewTreeObserver.addOnPreDrawListener(object : android.view.ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (scroll.height > maxH) {
                    scroll.layoutParams = scroll.layoutParams.apply { height = maxH }
                    scroll.requestLayout()
                }
                scroll.viewTreeObserver.removeOnPreDrawListener(this)
                return true
            }
        })

        // Buttons
        val buttons = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (14 * d).toInt()
            }
        }
        buttons.addView(makeBtn("نسخ الكل", colorAccent, d) {
            copyAll(blocks); onCopy()
        })
        buttons.addView(makeBtn("حفظ", colorSuccess, d) {
            onSave()
            Toast.makeText(context, "تم الحفظ", Toast.LENGTH_SHORT).show()
        })
        container.addView(buttons)

        return container
    }

    /** Small compact control chip for the in-place control pill. */
    private fun makeIconAction(text: String, color: Int, d: Float, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(color)
            textSize = 12.5f
            setPadding((13 * d).toInt(), (6 * d).toInt(), (13 * d).toInt(), (6 * d).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = (3 * d).toInt()
                marginEnd = (3 * d).toInt()
            }
            setOnClickListener { onClick() }
        }
    }

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
        val view = overlayView
        if (view != null) {
            try {
                if (view.isAttachedToWindow) {
                    windowManager.removeView(view)
                }
            } catch (_: Exception) {
                // Ignore removal errors - view may already be detached
            }
        }
        overlayView = null

        // Clean up in-place overlay views
        inPlaceView?.let { v ->
            try {
                if (v.isAttachedToWindow) windowManager.removeView(v)
            } catch (_: Exception) {}
        }
        inPlaceView = null

        inPlaceCloseButton?.let { v ->
            try {
                if (v.isAttachedToWindow) windowManager.removeView(v)
            } catch (_: Exception) {}
        }
        inPlaceCloseButton = null

        // Clean up visual replace overlay
        visualReplaceView?.let { v ->
            v.dismissPopup()
            try {
                if (v.isAttachedToWindow) windowManager.removeView(v)
            } catch (_: Exception) {}
        }
        visualReplaceView = null

        removeBottomSheet()

        isVisible = false
    }

    private fun removeBottomSheet() {
        bottomSheetView?.let { v ->
            try {
                if (v.isAttachedToWindow) windowManager.removeView(v)
            } catch (_: Exception) {}
        }
        bottomSheetView = null
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
            setTextColor(colorAccent)
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

        buttons.addView(makeBtn("نسخ", colorAccent, d) {
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
