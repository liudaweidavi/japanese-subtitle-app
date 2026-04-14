package com.subtitle.japanese.whisper

data class WhisperConfig(
    val modelPath: String = "",
    val language: String = "ja",
    val nThreads: Int = 4,
    val singleSegment: Boolean = true,
    val printProgress: Boolean = false,
    val printTimestamps: Boolean = false
)
