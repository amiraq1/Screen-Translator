package com.ammar.nabdscreentranslate.ocr

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MlKitOcrEngine : OcrEngine {

    private val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognizeText(bitmap: Bitmap): List<TextBlockResult> {
        return suspendCancellableCoroutine { continuation ->
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val results = visionText.textBlocks
                        .filter { it.text.isNotBlank() }
                        .map { block ->
                            TextBlockResult(
                                text = block.text.trim(),
                                boundingBox = block.boundingBox,
                                confidence = block.lines.firstOrNull()?.confidence
                            )
                        }
                    Log.d("NabdScreenTranslate", "OCR found ${results.size} text blocks")
                    continuation.resume(results)
                }
                .addOnFailureListener { exception ->
                    Log.e("NabdScreenTranslate", "OCR failed: ${exception.message}", exception)
                    continuation.resumeWithException(exception)
                }
        }
    }

    override fun close() {
        recognizer.close()
    }
}
