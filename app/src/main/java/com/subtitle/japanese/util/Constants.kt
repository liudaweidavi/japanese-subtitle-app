package com.subtitle.japanese.util

object Constants {
    const val NOTIFICATION_CHANNEL_ID = "subtitle_service_channel"
    const val NOTIFICATION_ID = 1001
    const val MEDIA_PROJECTION_REQUEST_CODE = 1001
    const val OVERLAY_PERMISSION_REQUEST_CODE = 1002

    // Audio
    const val SAMPLE_RATE = 16000
    const val AUDIO_CHANNEL_COUNT = 1
    const val AUDIO_ENCODING = android.media.AudioFormat.ENCODING_PCM_16BIT
    const val WINDOW_SIZE_MS = 5000L
    const val OVERLAP_SIZE_MS = 1000L
    const val CHUNK_INTERVAL_MS = 3000L

    // Whisper
    const val WHISPER_LANGUAGE = "ja"
    const val WHISPER_N_THREADS = 4
    const val MODEL_FILE_TINY = "ggml-tiny.bin"
    const val MODEL_FILE_BASE = "ggml-base.ja.bin"

    // Baidu Translation
    const val BAIDU_API_URL = "https://fanyi-api.baidu.com/api/trans/vip/translate"
    const val BAIDU_TRANSLATE_FROM = "jp"
    const val BAIDU_TRANSLATE_TO = "zh"

    // Intent extras
    const val EXTRA_MEDIA_PROJECTION_RESULT_CODE = "media_projection_result_code"
    const val EXTRA_MEDIA_PROJECTION_DATA = "media_projection_data"
}
