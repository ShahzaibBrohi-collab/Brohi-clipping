package com.example.data.model

import com.squareup.moshi.JsonClass

enum class TargetLanguage(val displayName: String, val localeTag: String) {
    ENGLISH("English", "en-US"),
    HINDI("Hindi", "hi-IN"),
    URDU("Urdu", "ur-PK"),
    ROMAN_URDU("Roman Urdu", "en-PK"),
    ARABIC("Arabic", "ar-SA"),
    BANGLA("Bangla", "bn-BD"),
    SPANISH("Spanish", "es-ES"),
    PORTUGUESE("Portuguese", "pt-BR")
}

enum class VoiceMode(val displayName: String, val description: String) {
    INTRO("Intro Hook", "3-5s AI intro hook, then original stream audio"),
    FULL("Full Narration", "AI commentary covers the entire short"),
    OFF("Original Only", "No AI voice; pure stream audio")
}

enum class AiVoiceOption(val displayName: String, val toneHint: String, val pitch: Float, val rate: Float) {
    PUCK("Puck", "Energetic & playful gaming hype", 1.12f, 1.08f),
    CHARON("Charon", "Deep & authoritative narrator", 0.85f, 0.96f),
    FENRIR("Fenrir", "Intense cinematic trailer voice", 0.88f, 1.02f),
    KORE("Kore", "Crisp & expressive storyteller", 1.06f, 1.03f),
    AOEDE("Aoede", "Warm & melodic broadcast tone", 1.04f, 0.98f),
    LEDA("Leda", "Sharp & dynamic commentary", 1.09f, 1.06f)
}

enum class CaptionStyle(val displayName: String) {
    BOLD("Bold"),
    NEON("Neon"),
    BOX("Box"),
    CLEAN("Clean")
}

enum class LayoutMode(val displayName: String) {
    FILL("Fill 9:16"),
    BLUR("Blur Canvas")
}

enum class StudioFont(val displayName: String, val assFontName: String) {
    OUTFIT("Outfit", "Outfit"),
    BEBAS_NEUE("Bebas Neue", "Bebas Neue"),
    PLUS_JAKARTA_SANS("Jakarta Sans", "Plus Jakarta Sans"),
    JETBRAINS_MONO("JetBrains Mono", "JetBrains Mono")
}

enum class GeminiModelOption(val modelId: String, val displayName: String, val subtitle: String) {
    FLASH_38("gemini-3.8-flash", "Gemini 3.8 Flash", "Default • Fast multimodal & audio analysis"),
    FLASH_LITE_35("gemini-3.5-flash-lite", "Gemini 3.5 Flash-Lite", "Ultra-fast • High-volume captioning"),
    ARGON_4("gemini-4-argon", "Gemini 4 Argon", "Peak reasoning • Complex narrative hooks")
}

@JsonClass(generateAdapter = true)
data class BlurMaskRect(
    val id: String,
    val xFraction: Float = 0.25f,
    val yFraction: Float = 0.2f,
    val widthFraction: Float = 0.45f,
    val heightFraction: Float = 0.12f
)

@JsonClass(generateAdapter = true)
data class SolidShapeRect(
    val id: String,
    val xFraction: Float = 0.2f,
    val yFraction: Float = 0.72f,
    val widthFraction: Float = 0.5f,
    val heightFraction: Float = 0.1f,
    val colorHex: String = "#12100C"
)

@JsonClass(generateAdapter = true)
data class CaptionWordCue(
    val word: String,
    val startMs: Long,
    val endMs: Long
)

@JsonClass(generateAdapter = true)
data class EditingConfig(
    val startMs: Long = 0L,
    val endMs: Long = 30_000L,
    val layoutMode: LayoutMode = LayoutMode.BLUR,
    val zoom: Float = 1.0f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val originalAudioVolume: Float = 0.85f,
    val voiceVolume: Float = 1.0f,
    val musicVolume: Float = 0.25f,
    val duckingEnabled: Boolean = true,
    val musicUri: String? = null,
    val blurMasks: List<BlurMaskRect> = emptyList(),
    val solidShapes: List<SolidShapeRect> = emptyList(),
    val captionStyle: CaptionStyle = CaptionStyle.BOLD,
    val fontFamily: StudioFont = StudioFont.BEBAS_NEUE,
    val fontSizeSp: Float = 32f,
    val primaryColorHex: String = "#F8F2E4",
    val highlightColorHex: String = "#FFC81F",
    val captionPositionY: Float = 0.78f,
    val wordCues: List<CaptionWordCue> = emptyList()
)

data class SourceMediaMetadata(
    val sourceUrl: String,
    val localVideoUri: String? = null,
    val title: String,
    val streamer: String,
    val platform: String,
    val durationMs: Long,
    val width: Int = 1920,
    val height: Int = 1080,
    val thumbnailUrl: String? = null,
    val isDirectPlayable: Boolean = false,
    val streamNotice: String? = null
)

data class StreamScanConfig(
    val metadata: SourceMediaMetadata,
    val language: TargetLanguage = TargetLanguage.ENGLISH,
    val voiceMode: VoiceMode = VoiceMode.INTRO,
    val voiceOption: AiVoiceOption = AiVoiceOption.PUCK,
    val clipDurationSec: Int = 30,
    val clipCount: Int = 3,
    val geminiModelId: String = GeminiModelOption.FLASH_38.modelId
)

data class DetectedMoment(
    val id: String,
    val startSec: Int,
    val endSec: Int,
    val viralScore: Int,
    val title: String,
    val hook: String,
    val explanation: String,
    val narrationScript: String,
    val captionText: String,
    val wordCues: List<CaptionWordCue> = emptyList(),
    val isSelected: Boolean = true
) {
    val durationSec: Int get() = (endSec - startSec).coerceAtLeast(5)
}

data class TwitchStreamInfo(
    val id: String,
    val userId: String,
    val userName: String,
    val title: String,
    val gameName: String,
    val viewerCount: Int,
    val language: String,
    val thumbnailUrl: String,
    val avatarUrl: String,
    val streamUrl: String
)

data class PipelineProgress(
    val stageName: String,
    val stageIndex: Int,
    val totalStages: Int,
    val percent: Int,
    val detail: String,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null
)

data class EngineStatus(
    val media3Ready: Boolean,
    val hardwareVideoEncoderName: String?,
    val softwareVideoEncoderName: String?,
    val audioAacEncoderAvailable: Boolean,
    val supportsAudioDucking: Boolean,
    val supportsAssAndOverlayBurnIn: Boolean
)

data class StorageStats(
    val shortsBytes: Long,
    val exportsBytes: Long,
    val tempCacheBytes: Long,
    val totalShortsCount: Int
)
