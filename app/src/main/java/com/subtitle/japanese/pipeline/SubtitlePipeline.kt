package com.subtitle.japanese.pipeline

import android.content.Context
import android.util.Log
import com.subtitle.japanese.audio.AudioBuffer
import com.subtitle.japanese.audio.AudioCaptureManager
import com.subtitle.japanese.data.AppDatabase
import com.subtitle.japanese.data.SubtitleEntry
import com.subtitle.japanese.data.SubtitleRepository
import com.subtitle.japanese.overlay.OverlayManager
import com.subtitle.japanese.translation.BaiduTranslator
import com.subtitle.japanese.util.Constants
import com.subtitle.japanese.whisper.WhisperConfig
import com.subtitle.japanese.whisper.WhisperEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Core orchestrator: audio capture → ASR → translation → overlay display.
 */
class SubtitlePipeline(private val context: Context) {

    private val TAG = "SubtitlePipeline"

    private val _state = MutableStateFlow(PipelineState.IDLE)
    val state: StateFlow<PipelineState> = _state.asStateFlow()

    private val _subtitleText = MutableStateFlow("")
    val subtitleText: StateFlow<String> = _subtitleText.asStateFlow()

    private var audioCapture: AudioCaptureManager? = null
    private var audioBuffer = AudioBuffer()
    private var whisperEngine: WhisperEngine? = null
    private var translator: BaiduTranslator? = null
    private var overlayManager: OverlayManager? = null
    private var repository: SubtitleRepository? = null
    private var sessionId: String = ""

    private var pipelineJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun configure(
        baiduAppId: String,
        baiduSecretKey: String,
        overlayManager: OverlayManager
    ) {
        this.translator = BaiduTranslator(baiduAppId, baiduSecretKey)
        this.overlayManager = overlayManager
    }

    fun start(resultCode: Int, data: android.content.Intent): Boolean {
        if (_state.value != PipelineState.IDLE) return false

        // Initialize repository and session
        val dao = AppDatabase.getInstance(context).subtitleDao()
        repository = SubtitleRepository(dao)
        sessionId = UUID.randomUUID().toString()

        // Initialize whisper
        val engine = WhisperEngine(context)
        val config = WhisperConfig(
            modelPath = Constants.MODEL_FILE_TINY,
            language = Constants.WHISPER_LANGUAGE,
            nThreads = Constants.WHISPER_N_THREADS
        )
        if (!engine.initialize(config)) {
            Log.e(TAG, "Failed to initialize Whisper engine")
            _state.value = PipelineState.ERROR
            return false
        }
        whisperEngine = engine

        // Start audio capture
        audioCapture = AudioCaptureManager(context)
        if (!audioCapture!!.startCapture(resultCode, data)) {
            Log.e(TAG, "Failed to start audio capture")
            engine.destroy()
            _state.value = PipelineState.ERROR
            return false
        }

        // Start pipeline coroutine
        _state.value = PipelineState.CAPTURING
        pipelineJob = scope.launch {
            audioCapture!!.audioData.collect { pcmData ->
                audioBuffer.write(pcmData, 0, pcmData.size)

                if (audioBuffer.hasEnoughData()) {
                    val samples = audioBuffer.extractWindow() ?: return@collect

                    // Step 1: ASR (Japanese speech to text)
                    _state.value = PipelineState.PROCESSING_ASR
                    val whisperResult = whisperEngine?.transcribe(samples)
                    val japaneseText = whisperResult?.text?.trim()

                    if (japaneseText.isNullOrBlank()) {
                        _state.value = PipelineState.CAPTURING
                        return@collect
                    }

                    Log.d(TAG, "ASR: $japaneseText")

                    // Step 2: Translation (Japanese to Chinese)
                    _state.value = PipelineState.PROCESSING_TRANSLATION
                    val translationResult = translator?.translate(japaneseText)
                    val displayText = translationResult?.translatedText ?: japaneseText

                    Log.d(TAG, "Translation: $displayText")

                    // Save to database
                    repository?.insert(
                        SubtitleEntry(
                            sourceText = japaneseText,
                            translatedText = displayText,
                            sessionId = sessionId
                        )
                    )

                    // Step 3: Display on overlay
                    _state.value = PipelineState.DISPLAYING
                    _subtitleText.value = displayText
                    withContext(Dispatchers.Main) {
                        overlayManager?.updateText(displayText)
                    }

                    _state.value = PipelineState.CAPTURING
                }
            }
        }

        return true
    }

    fun stop() {
        pipelineJob?.cancel()
        pipelineJob = null

        audioCapture?.stopCapture()
        audioCapture = null

        whisperEngine?.destroy()
        whisperEngine = null

        audioBuffer.reset()
        _state.value = PipelineState.IDLE
        _subtitleText.value = ""
    }

    fun destroy() {
        stop()
        overlayManager?.hide()
        scope.cancel()
    }
}
