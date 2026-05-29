package com.ammar.nabdscreentranslate.capture

import android.content.Intent
import android.media.projection.MediaProjection
import android.util.Log

/**
 * Singleton holder for MediaProjection consent data.
 * On Android 14+, each MediaProjection token can only be used once.
 * We store the consent result and create a new projection for each capture.
 */
object MediaProjectionHolder {

    private const val TAG = "NabdScreenTranslate"

    var resultCode: Int = 0
        private set
    var resultData: Intent? = null
        private set
    var mediaProjection: MediaProjection? = null
        private set

    fun store(resultCode: Int, data: Intent?) {
        Log.d(TAG, "MediaProjectionHolder: storing consent (resultCode=$resultCode)")
        this.resultCode = resultCode
        this.resultData = data
    }

    fun setProjection(projection: MediaProjection?) {
        this.mediaProjection = projection
    }

    fun stopProjection() {
        try {
            mediaProjection?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping MediaProjection: ${e.message}")
        }
        mediaProjection = null
    }

    fun clear() {
        stopProjection()
        resultCode = 0
        resultData = null
    }

    /**
     * On Android 14+, consent is single-use. After one capture we must
     * invalidate and re-request. For now we keep the consent and handle
     * SecurityException by re-requesting.
     */
    fun invalidateAfterUse() {
        // On Android 14+, the token is consumed after getMediaProjection()
        // We clear it so next tap will re-request permission
        stopProjection()
        resultCode = 0
        resultData = null
    }

    fun hasPermission(): Boolean = resultData != null && resultCode != 0
}
