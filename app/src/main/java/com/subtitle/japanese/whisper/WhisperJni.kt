package com.subtitle.japanese.whisper

/**
 * JNI bridge to whisper.cpp native code.
 * All native methods are declared here.
 */
object WhisperJni {

    init {
        System.loadLibrary("whisper-jni")
    }

    /**
     * Initialize whisper context with model file.
     * @param modelPath Absolute path to ggml model file
     * @return Context pointer (0 on failure)
     */
    external fun nativeInitContext(modelPath: String): Long

    /**
     * Free whisper context.
     * @param context Context pointer from nativeInitContext
     */
    external fun nativeFreeContext(context: Long)

    /**
     * Transcribe audio data.
     * @param context Whisper context pointer
     * @param samples PCM float array (32-bit float, 16kHz mono)
     * @param numSamples Number of samples in the array
     * @param language Language code (e.g. "ja")
     * @param nThreads Number of threads for inference
     * @return Transcribed text
     */
    external fun nativeTranscribe(
        context: Long,
        samples: FloatArray,
        numSamples: Int,
        language: String,
        nThreads: Int
    ): String

    /**
     * Get whisper.cpp version string.
     */
    external fun nativeGetVersion(): String
}
