package com.subtitle.japanese.translation

data class TranslationResult(
    val sourceText: String,
    val translatedText: String,
    val from: String,
    val to: String,
    val timestampMs: Long = System.currentTimeMillis()
)
