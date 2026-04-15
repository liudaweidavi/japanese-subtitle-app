package com.subtitle.japanese.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import com.subtitle.japanese.util.Constants
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Manages system audio capture via MediaProjection + AudioPlaybackCapture.
 * Captures app playback audio (e.g. Douyin video sound) as PCM 16-bit.
 */
class AudioCaptureManager(private val context: Context) {

    private var mediaProjection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null
    private var isCapturing = false

    private val _audioData = MutableSharedFlow<ShortArray>(extraBufferCapacity = 64)
    val audioData: SharedFlow<ShortArray> = _audioData.asSharedFlow()

    fun startCapture(resultCode: Int, data: Intent): Boolean {
        val projectionManager =
            context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        mediaProjection = projectionManager.getMediaProjection(resultCode, data) ?: return false

        val sampleRate = Constants.SAMPLE_RATE
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2

        // Capture system/app playback audio (not microphone)
        val captureConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .build()

        audioRecord = AudioRecord.Builder()
            .setAudioPlaybackCaptureConfig(captureConfig)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .build()

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            return false
        }

        audioRecord?.startRecording()
        isCapturing = true

        // Start capture thread
        Thread {
            val captureBuffer = ShortArray(bufferSize / 2)
            while (isCapturing) {
                val read = audioRecord?.read(captureBuffer, 0, captureBuffer.size) ?: 0
                if (read > 0) {
                    val data = captureBuffer.copyOf(read)
                    _audioData.tryEmit(data)
                }
            }
        }.start()

        return true
    }

    fun stopCapture() {
        isCapturing = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {
        }
        audioRecord = null

        try {
            mediaProjection?.stop()
        } catch (_: Exception) {
        }
        mediaProjection = null
    }

    fun isCapturing(): Boolean = isCapturing
}
