package com.subtitle.japanese.pipeline

enum class PipelineState {
    IDLE,
    CAPTURING,
    PROCESSING_ASR,
    PROCESSING_TRANSLATION,
    DISPLAYING,
    ERROR
}
