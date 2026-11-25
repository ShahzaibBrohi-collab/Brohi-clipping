package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiTestResult
import com.example.data.ai.GeminiAiService
import com.example.data.db.AppSettingsEntity
import com.example.data.db.BrohiDatabase
import com.example.data.db.BrohiRepository
import com.example.data.db.ShortRecordEntity
import com.example.data.media.AssSubtitleGenerator
import com.example.data.media.VideoProcessingEngine
import com.example.data.model.AiVoiceOption
import com.example.data.model.CaptionStyle
import com.example.data.model.DetectedMoment
import com.example.data.model.EditingConfig
import com.example.data.model.EngineStatus
import com.example.data.model.PipelineProgress
import com.example.data.model.SourceMediaMetadata
import com.example.data.model.StorageStats
import com.example.data.model.StreamScanConfig
import com.example.data.model.StudioFont
import com.example.data.model.TargetLanguage
import com.example.data.model.TwitchStreamInfo
import com.example.data.model.VoiceMode
import com.example.data.network.TwitchHelixService
import com.example.data.network.TwitchResult
import com.example.data.network.UrlMetadataExtractor
import com.example.data.network.UrlValidationResult
import com.example.data.security.KeystoreCryptoManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

sealed interface DiscoveryUiState {
    data object Idle : DiscoveryUiState
    data object Loading : DiscoveryUiState
    data class MissingCredentials(val message: String) : DiscoveryUiState
    data class Loaded(val streams: List<TwitchStreamInfo>) : DiscoveryUiState
    data class Error(val message: String) : DiscoveryUiState
}

data class ScanningUiState(
    val isScanning: Boolean = false,
    val progressPercent: Int = 0,
    val scannedSec: Int = 0,
    val totalSec: Int = 0,
    val statusLog: String = "Ready to scan",
    val detectedCount: Int = 0,
    val blipAngles: List<Float> = emptyList(),
    val errorMessage: String? = null,
    val completedSuccess: Boolean = false
)

class BrohiViewModel(application: Application) : AndroidViewModel(application) {

    private val db = BrohiDatabase.getInstance(application)
    val repository = BrohiRepository(db.brohiDao())
    val cryptoManager = KeystoreCryptoManager(application)
    val twitchService = TwitchHelixService(cryptoManager)
    val urlExtractor = UrlMetadataExtractor(application, twitchService)
    val geminiService = GeminiAiService(application)
    val videoEngine = VideoProcessingEngine(application)

    val settingsFlow: StateFlow<AppSettingsEntity> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettingsEntity()
    )

    val vaultShortsFlow: StateFlow<List<ShortRecordEntity>> = repository.shortsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _isValidatingUrl = MutableStateFlow(false)
    val isValidatingUrl: StateFlow<Boolean> = _isValidatingUrl.asStateFlow()

    private val _urlError = MutableStateFlow<String?>(null)
    val urlError: StateFlow<String?> = _urlError.asStateFlow()

    private val _activeMetadata = MutableStateFlow<SourceMediaMetadata?>(null)
    val activeMetadata: StateFlow<SourceMediaMetadata?> = _activeMetadata.asStateFlow()

    private val _scanConfig = MutableStateFlow<StreamScanConfig?>(null)
    val scanConfig: StateFlow<StreamScanConfig?> = _scanConfig.asStateFlow()

    private val _scanningState = MutableStateFlow(ScanningUiState())
    val scanningState: StateFlow<ScanningUiState> = _scanningState.asStateFlow()

    private val _detectedMoments = MutableStateFlow<List<DetectedMoment>>(emptyList())
    val detectedMoments: StateFlow<List<DetectedMoment>> = _detectedMoments.asStateFlow()

    private val _generationProgress = MutableStateFlow<PipelineProgress?>(null)
    val generationProgress: StateFlow<PipelineProgress?> = _generationProgress.asStateFlow()

    private val _discoveryState = MutableStateFlow<DiscoveryUiState>(DiscoveryUiState.Idle)
    val discoveryState: StateFlow<DiscoveryUiState> = _discoveryState.asStateFlow()

    private val _selectedCategory = MutableStateFlow("GTA V")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _engineStatus = MutableStateFlow(videoEngine.initializeEngine())
    val engineStatus: StateFlow<EngineStatus> = _engineStatus.asStateFlow()

    private val _storageStats = MutableStateFlow(StorageStats(0L, 0L, 0L, 0))
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    private val _studioExportProgress = MutableStateFlow<PipelineProgress?>(null)
    val studioExportProgress: StateFlow<PipelineProgress?> = _studioExportProgress.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private var scanJob: Job? = null
    private var generationJob: Job? = null
    private var studioExportJob: Job? = null

    init {
        viewModelScope.launch {
            repository.getSettings()
            refreshStorageStats()
        }
    }

    fun consumeToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun updateUrlInput(newUrl: String) {
        _urlInput.value = newUrl
        _urlError.value = null
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.saveSettings(current.copy(onboardingCompleted = true))
        }
    }

    fun inspectAndProceedUrl(url: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isValidatingUrl.value = true
            _urlError.value = null
            when (val res = urlExtractor.inspectSourceUrl(url)) {
                is UrlValidationResult.Valid -> {
                    _activeMetadata.value = res.metadata
                    initDefaultScanConfig(res.metadata)
                    _isValidatingUrl.value = false
                    onSuccess()
                }
                is UrlValidationResult.Invalid -> {
                    _urlError.value = res.error
                    _isValidatingUrl.value = false
                }
            }
        }
    }

    fun importLocalVideoUri(uri: Uri, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isValidatingUrl.value = true
            _urlError.value = null
            when (val res = urlExtractor.inspectLocalVideoUri(uri)) {
                is UrlValidationResult.Valid -> {
                    _activeMetadata.value = res.metadata
                    _urlInput.value = res.metadata.title
                    initDefaultScanConfig(res.metadata)
                    _isValidatingUrl.value = false
                    onSuccess()
                }
                is UrlValidationResult.Invalid -> {
                    _urlError.value = res.error
                    _isValidatingUrl.value = false
                }
            }
        }
    }

    fun attachLocalVideoToCurrentSession(uri: Uri) {
        viewModelScope.launch {
            when (val res = urlExtractor.inspectLocalVideoUri(uri)) {
                is UrlValidationResult.Valid -> {
                    val existing = _activeMetadata.value
                    val merged = if (existing != null) {
                        existing.copy(
                            localVideoUri = res.metadata.localVideoUri,
                            durationMs = res.metadata.durationMs,
                            width = res.metadata.width,
                            height = res.metadata.height,
                            isDirectPlayable = true,
                            streamNotice = null
                        )
                    } else {
                        res.metadata
                    }
                    _activeMetadata.value = merged
                    _scanConfig.value = _scanConfig.value?.copy(metadata = merged)
                    showToast("Local video attached (${merged.durationMs / 1000}s) — ready for real Media3 clipping.")
                }
                is UrlValidationResult.Invalid -> showToast(res.error)
            }
        }
    }

    fun selectTwitchStream(stream: TwitchStreamInfo, onReady: () -> Unit) {
        val metadata = SourceMediaMetadata(
            sourceUrl = stream.streamUrl,
            localVideoUri = null,
            title = stream.title,
            streamer = stream.userName,
            platform = "Twitch Live • ${stream.gameName}",
            durationMs = 300_000L,
            thumbnailUrl = stream.thumbnailUrl,
            isDirectPlayable = false,
            streamNotice = "Selected live Twitch stream (${stream.viewerCount} viewers). Gemini will analyze metadata & script viral hooks; attach an authorized recording if you want full 1080×1920 MP4 rendering."
        )
        _activeMetadata.value = metadata
        _urlInput.value = stream.streamUrl
        viewModelScope.launch {
            initDefaultScanConfig(metadata)
            onReady()
        }
    }

    private suspend fun initDefaultScanConfig(metadata: SourceMediaMetadata) {
        val settings = repository.getSettings()
        val lang = runCatching { TargetLanguage.valueOf(settings.defaultLanguage) }.getOrDefault(TargetLanguage.ENGLISH)
        val voice = runCatching { AiVoiceOption.valueOf(settings.defaultVoice) }.getOrDefault(AiVoiceOption.PUCK)
        val mode = runCatching { VoiceMode.valueOf(settings.defaultVoiceMode) }.getOrDefault(VoiceMode.INTRO)
        _scanConfig.value = StreamScanConfig(
            metadata = metadata,
            language = lang,
            voiceMode = mode,
            voiceOption = voice,
            clipDurationSec = settings.defaultClipDurationSec.coerceIn(20, 60),
            clipCount = settings.defaultClipCount,
            geminiModelId = settings.geminiModelId
        )
    }

    fun updateScanConfig(transform: (StreamScanConfig) -> StreamScanConfig) {
        _scanConfig.value?.let { current ->
            _scanConfig.value = transform(current)
        }
    }

    fun startAiScan(onCompleted: () -> Unit) {
        val config = _scanConfig.value ?: return
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            val totalSec = (config.metadata.durationMs / 1000L).toInt().coerceAtLeast(30)
            _detectedMoments.value = emptyList()
            _scanningState.value = ScanningUiState(
                isScanning = true,
                progressPercent = 12,
                scannedSec = 0,
                totalSec = totalSec,
                statusLog = "Validating source & preparing extraction...",
                detectedCount = 0
            )

            try {
                // Stage 1: Extract real audio segment if a local or direct playable URI is present
                var extractedAudio: File? = null
                val playableUriStr = config.metadata.localVideoUri
                if (!playableUriStr.isNullOrBlank()) {
                    _scanningState.update {
                        it.copy(
                            progressPercent = 28,
                            scannedSec = (totalSec * 0.25f).toInt(),
                            statusLog = "Extracting audio track via MediaExtractor..."
                        )
                    }
                    val audioOut = File(videoEngine.tempDir, "scan_audio_${System.currentTimeMillis()}.m4a")
                    extractedAudio = videoEngine.extractAudioSegment(
                        sourceUri = Uri.parse(playableUriStr),
                        startMs = 0L,
                        maxDurationMs = 90_000L,
                        outputFile = audioOut
                    )
                }

                _scanningState.update {
                    it.copy(
                        progressPercent = 52,
                        scannedSec = (totalSec * 0.55f).toInt(),
                        statusLog = "Connecting to Gemini (${config.geminiModelId}) via Firebase AI Logic..."
                    )
                }

                val result = geminiService.detectViralMoments(
                    config = config,
                    audioChunkFile = extractedAudio,
                    onStageUpdate = { msg ->
                        _scanningState.update { it.copy(statusLog = msg, progressPercent = 72) }
                    }
                )

                result.fold(
                    onSuccess = { moments ->
                        val angles = moments.mapIndexed { idx, _ -> (35f + idx * 65f) % 360f }
                        _detectedMoments.value = moments
                        _scanningState.value = ScanningUiState(
                            isScanning = false,
                            progressPercent = 100,
                            scannedSec = totalSec,
                            totalSec = totalSec,
                            statusLog = "Detected ${moments.size} viral moments!",
                            detectedCount = moments.size,
                            blipAngles = angles,
                            completedSuccess = true
                        )
                        delay(450L)
                        onCompleted()
                    },
                    onFailure = { err ->
                        _scanningState.update {
                            it.copy(
                                isScanning = false,
                                errorMessage = err.localizedMessage ?: "AI scanning failed."
                            )
                        }
                    }
                )
            } catch (e: CancellationException) {
                _scanningState.update {
                    it.copy(isScanning = false, statusLog = "Scan cancelled by user.")
                }
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Scan failed", e)
                _scanningState.update {
                    it.copy(isScanning = false, errorMessage = e.localizedMessage ?: "Scan error.")
                }
            }
        }
    }

    fun cancelAiScan() {
        scanJob?.cancel()
        _scanningState.update { it.copy(isScanning = false, statusLog = "Scan cancelled.") }
    }

    fun toggleMomentSelection(id: String) {
        _detectedMoments.update { list ->
            list.map { if (it.id == id) it.copy(isSelected = !it.isSelected) else it }
        }
    }

    fun selectAllMoments(selected: Boolean) {
        _detectedMoments.update { list -> list.map { it.copy(isSelected = selected) } }
    }

    fun sortMomentsByViralScore() {
        _detectedMoments.update { it.sortedByDescending { m -> m.viralScore } }
    }

    /**
     * Runs the multi-stage pipeline for all selected moments:
     * 1) AI Narration WAV generation
     * 2) ASS karaoke subtitle file generation
     * 3) Real Media3 Transformer 1080x1920 MP4 rendering (when playable source exists)
     * 4) Thumbnail extraction
     * 5) Saving ShortRecordEntity to Room Vault
     */
    fun generateSelectedShorts(onDone: (firstCreatedId: String?) -> Unit) {
        val config = _scanConfig.value ?: return
        val selected = _detectedMoments.value.filter { it.isSelected }
        if (selected.isEmpty()) {
            showToast("Select at least one viral moment.")
            return
        }

        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            val settings = repository.getSettings()
            val captionStyle = runCatching { CaptionStyle.valueOf(settings.defaultCaptionStyle) }.getOrDefault(CaptionStyle.BOLD)
            val font = runCatching { StudioFont.valueOf(settings.defaultFont) }.getOrDefault(StudioFont.BEBAS_NEUE)
            var firstId: String? = null

            try {
                selected.forEachIndexed { idx, moment ->
                    val shortId = UUID.randomUUID().toString()
                    if (firstId == null) firstId = shortId
                    val clipLabel = "Clip ${idx + 1}/${selected.size}: ${moment.title}"
                    val startMs = moment.startSec * 1000L
                    val endMs = (moment.endSec * 1000L).coerceAtLeast(startMs + 5_000L)

                    val editConfig = EditingConfig(
                        startMs = startMs,
                        endMs = endMs,
                        captionStyle = captionStyle,
                        fontFamily = font,
                        wordCues = moment.wordCues
                    )

                    // Stage 1: Narration TTS WAV
                    var voiceFile: File? = null
                    if (config.voiceMode != VoiceMode.OFF && moment.narrationScript.isNotBlank()) {
                        _generationProgress.value = PipelineProgress(
                            stageName = "AI Narration & WAV Synthesis",
                            stageIndex = 1,
                            totalStages = 4,
                            percent = 15,
                            detail = "$clipLabel — Synthesizing ${config.voiceOption.displayName} voice"
                        )
                        val wav = File(videoEngine.shortsDir, "${shortId}_voice.wav")
                        val ok = geminiService.synthesizeNarrationWav(
                            script = moment.narrationScript,
                            language = config.language,
                            voiceOption = config.voiceOption,
                            outputWavFile = wav
                        )
                        if (ok) voiceFile = wav
                    }

                    // Stage 2: ASS Karaoke Subtitles
                    _generationProgress.value = PipelineProgress(
                        stageName = "ASS Karaoke Subtitles",
                        stageIndex = 2,
                        totalStages = 4,
                        percent = 32,
                        detail = "$clipLabel — Generating .ass karaoke file"
                    )
                    val assFile = AssSubtitleGenerator.generateAssFile(
                        config = editConfig,
                        title = moment.title,
                        outputFile = File(videoEngine.shortsDir, "${shortId}_subs.ass")
                    )

                    // Stage 3: Media3 1080x1920 Video Render
                    var finalMp4: File? = null
                    var thumbFile: File? = null
                    val playableUriStr = config.metadata.localVideoUri
                    if (!playableUriStr.isNullOrBlank()) {
                        val mp4Out = File(videoEngine.shortsDir, "${shortId}_short.mp4")
                        val renderResult = videoEngine.renderVerticalShort(
                            sourceUri = Uri.parse(playableUriStr),
                            voiceWavFile = voiceFile,
                            config = editConfig,
                            outputMp4File = mp4Out,
                            preferHardwareEncoder = settings.preferHardwareEncoder,
                            onProgress = { pct ->
                                _generationProgress.value = PipelineProgress(
                                    stageName = "1080×1920 Media3 Rendering",
                                    stageIndex = 3,
                                    totalStages = 4,
                                    percent = 35 + (pct * 0.55f).toInt(),
                                    detail = "$clipLabel — Encoding ($pct%)"
                                )
                            }
                        )
                        if (renderResult.isSuccess) {
                            finalMp4 = renderResult.getOrNull()
                            thumbFile = finalMp4?.let {
                                videoEngine.generateThumbnail(
                                    videoUriOrPath = it.absolutePath,
                                    atMs = 1_000L,
                                    outputJpg = File(videoEngine.shortsDir, "${shortId}_thumb.jpg")
                                )
                            }
                        } else {
                            Log.w(TAG, "Media3 render skipped/failed: ${renderResult.exceptionOrNull()?.message}")
                        }
                    }

                    // Stage 4: Persist to Room Vault
                    _generationProgress.value = PipelineProgress(
                        stageName = "Saving to Shorts Vault",
                        stageIndex = 4,
                        totalStages = 4,
                        percent = 96,
                        detail = "$clipLabel — Saving record & metadata"
                    )
                    val now = System.currentTimeMillis()
                    val record = ShortRecordEntity(
                        id = shortId,
                        sourceUrl = config.metadata.sourceUrl,
                        sourceUri = playableUriStr,
                        streamer = config.metadata.streamer,
                        title = moment.title,
                        caption = moment.captionText,
                        startTimeMs = startMs,
                        endTimeMs = endMs,
                        rawVideoPath = playableUriStr,
                        finalMp4Path = finalMp4?.absolutePath,
                        voicePath = voiceFile?.absolutePath,
                        assSubtitlePath = assFile.absolutePath,
                        thumbnailPath = thumbFile?.absolutePath,
                        viralScore = moment.viralScore,
                        languageName = config.language.displayName,
                        voiceName = config.voiceOption.displayName,
                        voiceModeName = config.voiceMode.displayName,
                        createdTimestamp = now,
                        updatedTimestamp = now,
                        editingConfigJson = repository.encodeConfig(editConfig)
                    )
                    repository.saveShort(record)
                }

                refreshStorageStats()
                _generationProgress.value = PipelineProgress(
                    stageName = "Completed",
                    stageIndex = 4,
                    totalStages = 4,
                    percent = 100,
                    detail = "Saved ${selected.size} short(s) to Vault!",
                    isCompleted = true
                )
                delay(450L)
                _generationProgress.value = null
                onDone(firstId)
            } catch (e: CancellationException) {
                videoEngine.cancelActiveRender()
                _generationProgress.value = null
                showToast("Clip generation cancelled.")
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Clip generation failed", e)
                _generationProgress.value = PipelineProgress(
                    stageName = "Error",
                    stageIndex = 0,
                    totalStages = 4,
                    percent = 0,
                    detail = e.localizedMessage ?: "Generation failed.",
                    errorMessage = e.localizedMessage ?: "Generation failed."
                )
            }
        }
    }

    fun cancelGeneration() {
        videoEngine.cancelActiveRender()
        generationJob?.cancel()
        _generationProgress.value = null
    }

    fun clearGenerationError() {
        _generationProgress.value = null
    }

    // --- Studio Editor Export & Draft Saving ---

    fun saveStudioDraft(shortId: String, config: EditingConfig, title: String, caption: String) {
        viewModelScope.launch {
            val cues = geminiService.buildWordCues(caption, (config.endMs - config.startMs).coerceAtLeast(3_000L))
            val updatedConfig = config.copy(wordCues = cues)
            repository.updateEditingConfig(shortId, updatedConfig, title, caption)
            val record = repository.getShortById(shortId)
            if (record != null) {
                AssSubtitleGenerator.generateAssFile(
                    config = updatedConfig,
                    title = title,
                    outputFile = File(videoEngine.shortsDir, "${shortId}_subs.ass")
                )
            }
            showToast("Studio draft & ASS subtitles saved.")
        }
    }

    fun exportStudioShort(shortId: String, config: EditingConfig, title: String, caption: String) {
        studioExportJob?.cancel()
        studioExportJob = viewModelScope.launch {
            val record = repository.getShortById(shortId)
            if (record == null) {
                showToast("Short record not found.")
                return@launch
            }
            val playableSource = record.sourceUri ?: record.finalMp4Path
            if (playableSource.isNullOrBlank()) {
                _studioExportProgress.value = PipelineProgress(
                    stageName = "Source Video Needed",
                    stageIndex = 0,
                    totalStages = 3,
                    percent = 0,
                    detail = "Attach a local video file to render a 1080×1920 MP4.",
                    errorMessage = "No playable video file attached to this draft. Import a video from Home or attach one to render."
                )
                return@launch
            }

            val cues = geminiService.buildWordCues(caption, (config.endMs - config.startMs).coerceAtLeast(3_000L))
            val finalConfig = config.copy(wordCues = cues)
            val settings = repository.getSettings()

            _studioExportProgress.value = PipelineProgress(
                stageName = "Preparing Studio Timeline",
                stageIndex = 1,
                totalStages = 3,
                percent = 8,
                detail = "Updating ASS karaoke & overlay layers..."
            )

            val assFile = AssSubtitleGenerator.generateAssFile(
                config = finalConfig,
                title = title,
                outputFile = File(videoEngine.shortsDir, "${shortId}_subs.ass")
            )

            val outMp4 = File(videoEngine.exportsDir, "${shortId}_${System.currentTimeMillis()}.mp4")
            val voiceFile = record.voicePath?.let { File(it) }?.takeIf { it.exists() }

            val renderRes = videoEngine.renderVerticalShort(
                sourceUri = Uri.parse(playableSource),
                voiceWavFile = voiceFile,
                config = finalConfig,
                outputMp4File = outMp4,
                preferHardwareEncoder = settings.preferHardwareEncoder,
                onProgress = { pct ->
                    _studioExportProgress.value = PipelineProgress(
                        stageName = "Rendering 1080×1920 MP4",
                        stageIndex = 2,
                        totalStages = 3,
                        percent = 12 + (pct * 0.85f).toInt(),
                        detail = "Burning framing, audio mix & captions ($pct%)"
                    )
                }
            )

            renderRes.fold(
                onSuccess = { file ->
                    val thumb = videoEngine.generateThumbnail(
                        videoUriOrPath = file.absolutePath,
                        atMs = 1_000L,
                        outputJpg = File(videoEngine.shortsDir, "${shortId}_thumb.jpg")
                    )
                    repository.saveShort(
                        record.copy(
                            title = title,
                            caption = caption,
                            startTimeMs = finalConfig.startMs,
                            endTimeMs = finalConfig.endMs,
                            finalMp4Path = file.absolutePath,
                            assSubtitlePath = assFile.absolutePath,
                            thumbnailPath = thumb?.absolutePath ?: record.thumbnailPath,
                            updatedTimestamp = System.currentTimeMillis(),
                            editingConfigJson = repository.encodeConfig(finalConfig)
                        )
                    )
                    refreshStorageStats()
                    _studioExportProgress.value = PipelineProgress(
                        stageName = "Export Ready",
                        stageIndex = 3,
                        totalStages = 3,
                        percent = 100,
                        detail = "Saved MP4 (${file.length() / 1024} KB)",
                        isCompleted = true
                    )
                },
                onFailure = { err ->
                    _studioExportProgress.value = PipelineProgress(
                        stageName = "Export Failed",
                        stageIndex = 0,
                        totalStages = 3,
                        percent = 0,
                        detail = err.localizedMessage ?: "Render error",
                        errorMessage = err.localizedMessage ?: "Render failed."
                    )
                }
            )
        }
    }

    fun cancelStudioExport() {
        videoEngine.cancelActiveRender()
        studioExportJob?.cancel()
        _studioExportProgress.value = null
    }

    fun clearStudioExportProgress() {
        _studioExportProgress.value = null
    }

    fun attachVideoToShortDraft(shortId: String, uri: Uri) {
        viewModelScope.launch {
            val res = urlExtractor.inspectLocalVideoUri(uri)
            if (res is UrlValidationResult.Valid) {
                val existing = repository.getShortById(shortId) ?: return@launch
                val updatedEnd = res.metadata.durationMs.coerceAtMost(60_000L)
                val cfg = repository.decodeConfig(existing.editingConfigJson).copy(startMs = 0L, endMs = updatedEnd)
                val thumb = videoEngine.generateThumbnail(
                    videoUriOrPath = uri.toString(),
                    atMs = 1_000L,
                    outputJpg = File(videoEngine.shortsDir, "${shortId}_thumb.jpg")
                )
                repository.saveShort(
                    existing.copy(
                        sourceUri = uri.toString(),
                        rawVideoPath = uri.toString(),
                        startTimeMs = 0L,
                        endTimeMs = updatedEnd,
                        thumbnailPath = thumb?.absolutePath ?: existing.thumbnailPath,
                        updatedTimestamp = System.currentTimeMillis(),
                        editingConfigJson = repository.encodeConfig(cfg)
                    )
                )
                showToast("Video attached to Studio draft!")
            } else if (res is UrlValidationResult.Invalid) {
                showToast(res.error)
            }
        }
    }

    // --- Vault Actions ---

    fun saveShortToGallery(record: ShortRecordEntity) {
        val path = record.finalMp4Path
        if (path.isNullOrBlank() || !File(path).exists()) {
            showToast("Render the MP4 in Studio before saving to Gallery.")
            return
        }
        viewModelScope.launch {
            videoEngine.saveToGallery(File(path), record.title).fold(
                onSuccess = { showToast("Saved to Movies/BrohiClipping in Gallery!") },
                onFailure = { showToast(it.localizedMessage ?: "Failed saving to Gallery.") }
            )
        }
    }

    fun deleteShort(id: String) {
        viewModelScope.launch {
            repository.deleteShortAndMedia(id)
            refreshStorageStats()
            showToast("Short and media files deleted.")
        }
    }

    // --- Twitch Discovery ---

    fun loadTwitchCategory(category: String) {
        _selectedCategory.value = category
        viewModelScope.launch {
            _discoveryState.value = DiscoveryUiState.Loading
            when (val res = twitchService.fetchLiveStreamsByCategory(category)) {
                is TwitchResult.Success -> _discoveryState.value = DiscoveryUiState.Loaded(res.data)
                is TwitchResult.MissingCredentials -> _discoveryState.value = DiscoveryUiState.MissingCredentials(res.message)
                is TwitchResult.Error -> _discoveryState.value = DiscoveryUiState.Error(res.message)
            }
        }
    }

    // --- Settings & Storage ---

    fun updateSettings(transform: (AppSettingsEntity) -> AppSettingsEntity) {
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.saveSettings(transform(current))
        }
    }

    fun saveTwitchCredentials(clientId: String, clientSecret: String) {
        cryptoManager.putEncrypted(KeystoreCryptoManager.KEY_TWITCH_CLIENT_ID, clientId.trim())
        cryptoManager.putEncrypted(KeystoreCryptoManager.KEY_TWITCH_CLIENT_SECRET, clientSecret.trim())
        showToast("Twitch credentials encrypted with Android Keystore.")
    }

    fun getTwitchClientId(): String = cryptoManager.getDecrypted(KeystoreCryptoManager.KEY_TWITCH_CLIENT_ID)
    fun getTwitchClientSecret(): String = cryptoManager.getDecrypted(KeystoreCryptoManager.KEY_TWITCH_CLIENT_SECRET)

    suspend fun testGeminiAi(modelId: String): AiTestResult = geminiService.testConnection(modelId)

    suspend fun testTwitchConnection(): TwitchResult<String> = twitchService.testConnection()

    fun refreshStorageStats() {
        viewModelScope.launch {
            val count = vaultShortsFlow.value.size
            _storageStats.value = videoEngine.getStorageUsage(count)
            _engineStatus.value = videoEngine.checkEngineStatus()
        }
    }

    fun clearTempCache() {
        viewModelScope.launch {
            val freed = videoEngine.cleanupTemporaryFiles()
            refreshStorageStats()
            showToast("Cleared ${freed / 1024} KB of temporary cache.")
        }
    }

    companion object {
        private const val TAG = "BrohiViewModel"
    }
}
