package com.ammar.nabdscreentranslate.overlay

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.ammar.nabdscreentranslate.NabdApp
import com.ammar.nabdscreentranslate.capture.MediaProjectionHolder
import com.ammar.nabdscreentranslate.capture.MediaProjectionRequestActivity
import com.ammar.nabdscreentranslate.capture.ScreenCaptureManager
import com.ammar.nabdscreentranslate.data.AppDatabase
import com.ammar.nabdscreentranslate.data.SettingsDataStore
import com.ammar.nabdscreentranslate.domain.SaveTranslationUseCase
import com.ammar.nabdscreentranslate.domain.TranslateScreenUseCase
import com.ammar.nabdscreentranslate.ocr.MlKitOcrEngine
import com.ammar.nabdscreentranslate.translate.MlKitTranslationEngine
import com.ammar.nabdscreentranslate.ui.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class FloatingButtonService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: FloatingButtonView? = null
    private var translationOverlayManager: TranslationOverlayManager? = null
    private var regionSelectorOverlay: RegionSelectorOverlay? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var screenCaptureManager: ScreenCaptureManager
    private lateinit var translateScreenUseCase: TranslateScreenUseCase
    private lateinit var saveTranslationUseCase: SaveTranslationUseCase
    private lateinit var settingsDataStore: SettingsDataStore

    private val ocrEngine = MlKitOcrEngine()
    private val translationEngine = MlKitTranslationEngine()

    // Debounce & state
    private var lastClickTime = 0L
    private val DEBOUNCE_MS = 1500L
    private var isProcessing = false
    private var lastTranslationResult: Pair<String, String>? = null

    // Long press detection
    private var pendingRegion: android.graphics.Rect? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FloatingButtonService created")
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        screenCaptureManager = ScreenCaptureManager(this)
        settingsDataStore = SettingsDataStore(this)

        val dao = AppDatabase.getInstance(this).translationHistoryDao()
        translateScreenUseCase = TranslateScreenUseCase(ocrEngine, translationEngine)
        saveTranslationUseCase = SaveTranslationUseCase(dao)
        translationOverlayManager = TranslationOverlayManager(this, windowManager!!)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                Log.d(TAG, "Starting floating button service")
                startForegroundWithNotification()
                showFloatingButton()
            }
            ACTION_STOP -> {
                Log.d(TAG, "Stopping floating button service")
                removeFloatingButton()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ requires foreground service type
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, FloatingButtonService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NabdApp.CAPTURE_CHANNEL_ID)
            .setContentTitle("ترجمة الشاشة نشطة")
            .setContentText("اضغط على الزر العائم لترجمة الشاشة")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إيقاف", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun showFloatingButton() {
        if (floatingView != null) return

        floatingView = FloatingButtonView(this)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 300
        }

        floatingView?.setupTouchListener(params, windowManager!!) { eventType ->
            when (eventType) {
                FloatingButtonView.EventType.SINGLE_TAP -> onSingleTap()
                FloatingButtonView.EventType.LONG_PRESS -> onLongPress()
                FloatingButtonView.EventType.DOUBLE_TAP -> onDoubleTap()
            }
        }

        try {
            windowManager?.addView(floatingView, params)
            Log.d(TAG, "Floating button shown")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show floating button: ${e.message}", e)
        }
    }

    private fun onSingleTap() {
        val now = System.currentTimeMillis()
        if (now - lastClickTime < DEBOUNCE_MS || isProcessing) {
            Log.d(TAG, "Tap ignored (debounce or processing)")
            return
        }
        lastClickTime = now
        Log.d(TAG, "Floating button tapped - starting capture flow")
        startCaptureFlow(region = null)
    }

    private fun onLongPress() {
        if (isProcessing) return
        Log.d(TAG, "Long press - showing region selector")
        showRegionSelector()
    }

    private fun onDoubleTap() {
        Log.d(TAG, "Double tap - toggling last translation")
        lastTranslationResult?.let {
            translationOverlayManager?.toggleVisibility()
        }
    }

    /**
     * Main capture flow entry point.
     * Always requests MediaProjection permission since tokens are single-use on Android 14+.
     */
    private fun startCaptureFlow(region: android.graphics.Rect?) {
        pendingRegion = region
        // Always request fresh permission - tokens are single-use on Android 14+
        Log.d(TAG, "Requesting MediaProjection permission")
        MediaProjectionRequestActivity.launch(
            context = this,
            onGranted = {
                Log.d(TAG, "MediaProjection granted - performing capture")
                performCapture(pendingRegion)
                pendingRegion = null
            },
            onDenied = {
                Log.w(TAG, "MediaProjection denied by user")
                pendingRegion = null
                handler.post {
                    translationOverlayManager?.showError("لم يتم منح صلاحية التقاط الشاشة")
                }
            }
        )
    }

    private fun performCapture(region: android.graphics.Rect? = null) {
        if (isProcessing) {
            Log.d(TAG, "Already processing, ignoring")
            return
        }
        isProcessing = true
        floatingView?.setLoading(true)

        // Hide floating button to avoid capturing it
        handler.post { floatingView?.visibility = View.INVISIBLE }

        serviceScope.launch {
            delay(250) // Wait for button to hide from screen

            try {
                // Step 1: Capture screenshot
                Log.d(TAG, "Step 1: Capturing screen...")
                val bitmap = screenCaptureManager.captureScreen()
                Log.d(TAG, "Screen captured: ${bitmap.width}x${bitmap.height}")

                // Step 2: Read settings
                val sourceLang = settingsDataStore.sourceLang.first()
                val targetLang = settingsDataStore.targetLang.first()
                Log.d(TAG, "Step 2: Languages - source=$sourceLang, target=$targetLang")

                // Step 3: OCR + Translation via UseCase
                Log.d(TAG, "Step 3: Running OCR + Translation...")
                val result = translateScreenUseCase.executeWithBlocks(
                    bitmap = bitmap,
                    sourceLang = sourceLang,
                    targetLang = targetLang,
                    region = region
                )

                // Step 4: Show result
                withContext(Dispatchers.Main) {
                    result.fold(
                        onSuccess = { screenResult ->
                            Log.d(TAG, "Step 4: Translation SUCCESS - showing overlay")
                            lastTranslationResult = screenResult.originalText to screenResult.translatedText

                            translationOverlayManager?.showTranslation(
                                originalText = screenResult.originalText,
                                translatedText = screenResult.translatedText,
                                onCopy = { /* copy handled internally in overlay */ },
                                onSave = {
                                    serviceScope.launch {
                                        saveToHistory(
                                            screenResult.originalText,
                                            screenResult.translatedText,
                                            sourceLang,
                                            targetLang
                                        )
                                    }
                                },
                                onClose = {
                                    translationOverlayManager?.hide()
                                }
                            )

                            // Auto-save to history
                            saveToHistory(
                                screenResult.originalText,
                                screenResult.translatedText,
                                sourceLang,
                                targetLang
                            )

                            vibrateIfEnabled()
                        },
                        onFailure = { error ->
                            Log.e(TAG, "Step 4: Translation FAILED - ${error.message}")
                            translationOverlayManager?.showError(
                                error.message ?: "تعذرت الترجمة. حاول مرة أخرى."
                            )
                        }
                    )
                }

                // Recycle bitmap after use
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Capture/translate error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    val msg = when {
                        e.message?.contains("صلاحية") == true -> e.message!!
                        e.message?.contains("تعذر") == true -> e.message!!
                        else -> "تعذر التقاط الشاشة. حاول مرة أخرى."
                    }
                    translationOverlayManager?.showError(msg)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    floatingView?.visibility = View.VISIBLE
                    floatingView?.setLoading(false)
                    isProcessing = false
                }
            }
        }
    }

    private suspend fun saveToHistory(
        sourceText: String,
        translatedText: String,
        sourceLang: String,
        targetLang: String
    ) {
        try {
            val saveHistory = settingsDataStore.saveHistory.first()
            if (saveHistory && sourceText.isNotBlank() && translatedText.isNotBlank()) {
                saveTranslationUseCase.execute(
                    sourceText = sourceText,
                    translatedText = translatedText,
                    sourceLang = sourceLang,
                    targetLang = targetLang
                )
                Log.d(TAG, "Translation saved to history")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save to history: ${e.message}")
        }
    }

    private fun showRegionSelector() {
        regionSelectorOverlay = RegionSelectorOverlay(this, windowManager!!) { region ->
            regionSelectorOverlay?.remove()
            regionSelectorOverlay = null
            if (region != null) {
                startCaptureFlow(region)
            }
        }
        regionSelectorOverlay?.show()
    }

    private fun vibrateIfEnabled() {
        serviceScope.launch {
            try {
                val shouldVibrate = settingsDataStore.vibrateOnTranslate.first()
                if (shouldVibrate) {
                    withContext(Dispatchers.Main) {
                        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                            vm.defaultVibrator
                        } else {
                            @Suppress("DEPRECATION")
                            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                        }
                        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Vibration error: ${e.message}")
            }
        }
    }

    private fun removeFloatingButton() {
        try {
            floatingView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Error removing floating button: ${e.message}")
        }
        floatingView = null

        try {
            translationOverlayManager?.hide()
        } catch (e: Exception) {
            Log.w(TAG, "Error hiding translation overlay: ${e.message}")
        }

        try {
            regionSelectorOverlay?.remove()
        } catch (e: Exception) {
            Log.w(TAG, "Error removing region selector: ${e.message}")
        }
        regionSelectorOverlay = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "FloatingButtonService destroyed")
        removeFloatingButton()
        serviceScope.cancel()
        ocrEngine.close()
        translationEngine.close()
    }

    companion object {
        private const val TAG = "NabdScreenTranslate"
        const val ACTION_START = "com.ammar.nabdscreentranslate.START_FLOATING"
        const val ACTION_STOP = "com.ammar.nabdscreentranslate.STOP_FLOATING"
        private const val NOTIFICATION_ID = 2001
    }
}
