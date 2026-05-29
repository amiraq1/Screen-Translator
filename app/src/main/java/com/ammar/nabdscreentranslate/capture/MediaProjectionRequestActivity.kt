package com.ammar.nabdscreentranslate.capture

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log

/**
 * Transparent activity to request MediaProjection permission.
 * Launches the system consent dialog and stores the result.
 * Uses FLAG_ACTIVITY_NEW_TASK to launch from a Service context.
 */
class MediaProjectionRequestActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "Requesting MediaProjection consent from user")

        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = projectionManager.createScreenCaptureIntent()

        @Suppress("DEPRECATION")
        startActivityForResult(captureIntent, REQUEST_CODE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                Log.d(TAG, "MediaProjection consent GRANTED")
                // Clone the intent to ensure it survives activity destruction
                MediaProjectionHolder.store(resultCode, data.clone() as Intent)
                onPermissionGranted?.invoke()
            } else {
                Log.w(TAG, "MediaProjection consent DENIED by user")
                onPermissionDenied?.invoke()
            }
        }

        // Clear static callbacks to prevent memory leaks
        onPermissionGranted = null
        onPermissionDenied = null
        finish()
    }

    companion object {
        private const val TAG = "NabdScreenTranslate"
        private const val REQUEST_CODE = 1000

        // Static callbacks - cleared after use to prevent leaks
        var onPermissionGranted: (() -> Unit)? = null
        var onPermissionDenied: (() -> Unit)? = null

        fun launch(context: Context, onGranted: () -> Unit = {}, onDenied: () -> Unit = {}) {
            onPermissionGranted = onGranted
            onPermissionDenied = onDenied
            val intent = Intent(context, MediaProjectionRequestActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
