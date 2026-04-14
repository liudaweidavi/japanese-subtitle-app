package com.subtitle.japanese.audio

import com.subtitle.japanese.util.Constants
import java.util.concurrent.atomic.AtomicInteger

/**
 * Ring buffer with sliding window for audio data.
 * Whisper requires 16kHz mono PCM 16-bit input.
 * We accumulate audio in a circular buffer and extract overlapping chunks.
 */
class AudioBuffer(
    private val sampleRate: Int = Constants.SAMPLE_RATE,
    private val windowSizeMs: Long = Constants.WINDOW_SIZE_MS,
    private val overlapSizeMs: Long = Constants.OVERLAP_SIZE_MS
) {
    private val windowSamples = (sampleRate * windowSizeMs / 1000).toInt()
    private val overlapSamples = (sampleRate * overlapSizeMs / 1000).toInt()
    private val stepSamples = windowSamples - overlapSamples

    private val buffer = ShortArray(windowSamples * 2)
    private val writePos = AtomicInteger(0)
    private var totalSamplesWritten = 0
    private var lastExtractedEnd = 0

    val windowSize: Int get() = windowSamples
    val stepSize: Int get() = stepSamples

    @Synchronized
    fun write(samples: ShortArray, offset: Int, length: Int) {
        for (i in 0 until length) {
            val pos = writePos.get() % buffer.size
            buffer[pos] = samples[offset + i]
            writePos.incrementAndGet()
        }
        totalSamplesWritten += length
    }

    @Synchronized
    fun hasEnoughData(): Boolean {
        return totalSamplesWritten - lastExtractedEnd >= stepSamples
    }

    @Synchronized
    fun extractWindow(): FloatArray? {
        if (!hasEnoughData()) return null

        val result = FloatArray(windowSamples)
        val startIdx = totalSamplesWritten - windowSamples

        for (i in 0 until windowSamples) {
            val bufIdx = (startIdx + i) % buffer.size
            result[i] = buffer[bufIdx] / 32768.0f
        }

        lastExtractedEnd = totalSamplesWritten - overlapSamples
        return result
    }

    @Synchronized
    fun reset() {
        writePos.set(0)
        totalSamplesWritten = 0
        lastExtractedEnd = 0
        buffer.fill(0)
    }
}
