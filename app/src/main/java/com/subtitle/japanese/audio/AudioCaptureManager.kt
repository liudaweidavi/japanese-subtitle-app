package com.subtitle.japanese.audio

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import com.subtitle.japanese.util.Constants
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Manages system audio capture via MediaProjection.
 * Captures PCM 16-bit audio and converts to 16kHz mono for Whisper.
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

        // Configure AudioRecord for system audio capture
        val sampleRate = Constants.SAMPLE_RATE
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.DEFAULT,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

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
