package com.subtitle.japanese.whisper

import android.content.Context
import android.util.Log
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
     * Check if model file is already copied to internal storage.
     */
    fun isModelReady(): Boolean {
        val modelFile = File(context.filesDir, config.modelPath.substringAfterLast("/"))
        return modelFile.exists()
    }

    /**
     * Copy model from assets to internal storage with progress callback.
     * @param onProgress callback with percentage (0-100)
     */
    fun prepareModel(config: WhisperConfig, onProgress: (Int) -> Unit = {}): Boolean {
        this.config = config
        val fileName = config.modelPath.substringAfterLast("/")
        val modelFile = File(context.filesDir, fileName)

        if (modelFile.exists()) {
            onProgress(100)
            return true
        }

        return try {
            val assetPath = "models/$fileName"
            context.assets.openFd(assetPath).use {afd ->
                val totalSize = afd.length
                afd.createInputStream().use { input ->
                    FileOutputStream(modelFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (totalSize > 0) {
                                onProgress((totalRead * 100 / totalSize).toInt())
                            }
                        }
                    }
                }
            }
            onProgress(100)
            Log.i(TAG, "Model copied: $fileName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy model: ${e.message}")
            false
        }
    }

    /**
     * Load model into memory (call after prepareModel).
     */
    fun loadModel(config: WhisperConfig): Boolean {
        if (isInitialized) {
            destroy()
        }
        this.config = config

        val modelFile = File(context.filesDir, config.modelPath.substringAfterLast("/"))
        if (!modelFile.exists()) return false

        contextPtr = WhisperJni.nativeInitContext(modelFile.absolutePath)
        if (contextPtr == 0L) {
            Log.e(TAG, "Failed to initialize whisper context")
            return false
        }

        isInitialized = true
        Log.i(TAG, "Whisper engine initialized: ${modelFile.name}")
        return true
    }

    /**
     * Load model from assets to internal storage and initialize context.
     */
    fun initialize(config: WhisperConfig): Boolean {
        if (!prepareModel(config)) return false
        return loadModel(config)
    }

    /**
     * Transcribe PCM float audio data.
     */
    fun transcribe(samples: FloatArray): WhisperResult? {
        if (!isInitialized || contextPtr == 0L) {
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

            if (text.isBlank()) null
            else WhisperResult(text = text, language = config.language, confidence = 1.0f)
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed: ${e.message}")
            null
        }
    }

    fun destroy() {
        if (contextPtr != 0L) {
            WhisperJni.nativeFreeContext(contextPtr)
            contextPtr = 0
        }
        isInitialized = false
    }

    fun isInitialized(): Boolean = isInitialized
}
