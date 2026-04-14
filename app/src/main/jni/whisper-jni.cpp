#include <jni.h>
#include <string>
#include <android/log.h>

// Whisper.cpp headers
#include "whisper.h"

#define LOG_TAG "WhisperJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

/**
 * Initialize whisper context with model file.
 */
JNIEXPORT jlong JNICALL
Java_com_subtitle_japanese_whisper_WhisperJni_nativeInitContext(
        JNIEnv *env, jobject thiz, jstring model_path) {
    const char *modelPath = env->GetStringUTFChars(model_path, nullptr);
    if (!modelPath) {
        LOGE("Model path is null");
        return 0;
    }

    LOGI("Loading whisper model: %s", modelPath);

    struct whisper_context_params cparams = whisper_context_default_params();
    cparams.use_gpu = false;  // CPU-only for broader device support

    struct whisper_context *ctx = whisper_init_from_file_with_params(modelPath, cparams);

    env->ReleaseStringUTFChars(model_path, modelPath);

    if (!ctx) {
        LOGE("Failed to initialize whisper context");
        return 0;
    }

    LOGI("Whisper context initialized successfully");
    return reinterpret_cast<jlong>(ctx);
}

/**
 * Free whisper context.
 */
JNIEXPORT void JNICALL
Java_com_subtitle_japanese_whisper_WhisperJni_nativeFreeContext(
        JNIEnv *env, jobject thiz, jlong context_ptr) {
    if (context_ptr == 0) return;

    struct whisper_context *ctx = reinterpret_cast<struct whisper_context *>(context_ptr);
    whisper_free(ctx);
    LOGI("Whisper context freed");
}

/**
 * Transcribe audio data.
 */
JNIEXPORT jstring JNICALL
Java_com_subtitle_japanese_whisper_WhisperJni_nativeTranscribe(
        JNIEnv *env, jobject thiz,
        jlong context_ptr,
        jfloatArray samples,
        jint num_samples,
        jstring language,
        jint n_threads) {
    if (context_ptr == 0) {
        LOGE("Context is null");
        return env->NewStringUTF("");
    }

    struct whisper_context *ctx = reinterpret_cast<struct whisper_context *>(context_ptr);

    // Get audio samples
    jfloat *samplesPtr = env->GetFloatArrayElements(samples, nullptr);
    if (!samplesPtr) {
        LOGE("Failed to get samples array");
        return env->NewStringUTF("");
    }

    const char *lang = env->GetStringUTFChars(language, nullptr);
    if (!lang) {
        env->ReleaseFloatArrayElements(samples, samplesPtr, JNI_ABORT);
        LOGE("Language string is null");
        return env->NewStringUTF("");
    }

    // Configure whisper parameters
    struct whisper_full_params wparams = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);

    wparams.print_realtime = false;
    wparams.print_progress = false;
    wparams.print_timestamps = false;
    wparams.print_special = false;
    wparams.single_segment = true;
    wparams.language = lang;
    wparams.n_threads = n_threads;
    wparams.translate = false;
    wparams.no_timestamps = true;

    // Run inference
    int result = whisper_full(ctx, wparams, samplesPtr, num_samples);

    env->ReleaseFloatArrayElements(samples, samplesPtr, JNI_ABORT);
    env->ReleaseStringUTFChars(language, lang);

    if (result != 0) {
        LOGE("Whisper full failed with code: %d", result);
        return env->NewStringUTF("");
    }

    // Collect transcription results
    std::string text;
    const int n_segments = whisper_full_n_segments(ctx);
    for (int i = 0; i < n_segments; ++i) {
        const char *segment = whisper_full_get_segment_text(ctx, i);
        if (segment) {
            text += segment;
        }
    }

    LOGI("Transcribed (%d segments): %s", n_segments, text.c_str());

    return env->NewStringUTF(text.c_str());
}

/**
 * Get whisper.cpp version.
 */
JNIEXPORT jstring JNICALL
Java_com_subtitle_japanese_whisper_WhisperJni_nativeGetVersion(
        JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(whisper_print_system_info());
}

} // extern "C"
