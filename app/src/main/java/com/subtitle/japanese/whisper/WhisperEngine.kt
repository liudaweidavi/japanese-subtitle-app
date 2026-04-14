package com.subtitle.japanese.whisper

import android.content.Context
import android.util.Log
import com.subtitle.japanese.util.Constants
import java.io.File
import java.io.FileOutputStream

/**
 * High-level wrapper for whisper.cpp speech recognition.
 * Manages model loading, context lifecycle, and transcription.
 */
class WhisperEngine(private val context: Context) {

    private var contextPtr: Long = 0
    private var isInitialized = false
    private var config = WhisperConfig()

    companion object {
        private const val TAG = "WhisperEngine"
    }

    /**
     * Load model from assets to internal storage and initialize context.
     */
    fun initialize(config: WhisperConfig): Boolean {
        if (isInitialized) {
            destroy()
        }

        this.config = config

        // Copy model from assets if not already copied
        val modelFile = File(context.filesDir, config.modelPath.substringAfterLast("/"))
        if (!modelFile.exists()) {
            try {
                val assetPath = "models/${config.modelPath.substringAfterLast("/")}"
                context.assets.open(assetPath).use { input ->
                    FileOutputStream(modelFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy model from assets: ${e.message}")
                return false
            }
        }

        // Initialize whisper context
        contextPtr = WhisperJni.nativeInitContext(modelFile.absolutePath)
        if (contextPtr == 0L) {
            Log.e(TAG, "Failed to initialize whisper context")
            return false
        }

        isInitialized = true
        Log.i(TAG, "Whisper engine initialized with model: ${modelFile.name}")
        return true
    }

    /**
     * Transcribe PCM float audio data.
     * @param samples 32-bit float PCM at 16kHz mono
     * @return WhisperResult with transcribed text
     */
    fun transcribe(samples: FloatArray): WhisperResult? {
        if (!isInitialized || contextPtr == 0L) {
            Log.w(TAG, "Engine not initialized")
            return null
        }

        return try {
            val text = WhisperJni.nativeTranscribe(
                contextPtr,
                samples,
                samples.size,
                config.language,
                config.nThreads
            ).trim()

            WhisperResult(
                text = text,
                language = config.language,
                confidence = 1.0f
            )
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed: ${e.message}")
            null
        }
    }

    /**
     * Release whisper context and free native memory.
     */
    fun destroy() {
        if (contextPtr != 0L) {
            WhisperJni.nativeFreeContext(contextPtr)
            contextPtr = 0
        }
        isInitialized = false
    }

    fun isInitialized(): Boolean = isInitialized
}
