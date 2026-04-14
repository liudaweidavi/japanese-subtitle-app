package com.subtitle.japanese.whisper

data class WhisperResult(
    val text: String,
    val language: String,
    val confidence: Float,
    val timestampMs: Long = System.currentTimeMillis()
)
