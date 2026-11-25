package com.example.data.ai

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.model.AiVoiceOption
import com.example.data.model.CaptionWordCue
import com.example.data.model.DetectedMoment
import com.example.data.model.GeminiModelOption
import com.example.data.model.StreamScanConfig
import com.example.data.model.TargetLanguage
import com.example.data.model.VoiceMode
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

sealed interface AiTestResult {
    data class Success(val message: String) : AiTestResult
    data class Failure(val error: String) : AiTestResult
}

class GeminiAiService(private val context: Context) {

    private val momentListSchema = Schema.obj(
        properties = mapOf(
            "moments" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "startSec" to Schema.integer("Start timestamp in seconds within total video duration"),
                        "endSec" to Schema.integer("End timestamp in seconds"),
                        "viralScore" to Schema.integer("Virality score from 1 to 100"),
                        "title" to Schema.string("Punchy short title under 60 characters"),
                        "hook" to Schema.string("3-second opening hook line"),
                        "explanation" to Schema.string("Why this moment has high viral potential"),
                        "narrationScript" to Schema.string("Spoken narration script matching requested voice mode and language"),
                        "captionText" to Schema.string("Short crisp karaoke subtitle script for the clip")
                    )
                ),
                description = "List of detected viral short moments"
            )
        )
    )

    suspend fun testConnection(modelId: String): AiTestResult = withContext(Dispatchers.IO) {
        val safeModel = validateSupportedModel(modelId)
        try {
            val model = Firebase.ai.generativeModel(safeModel)
            val response = withTimeout(30_000L) {
                model.generateContent("Reply with exactly: BROHI CLIPPING AI ONLINE")
            }
            val text = response.text?.trim().orEmpty()
            if (text.isNotBlank()) {
                AiTestResult.Success("Firebase AI Logic ($safeModel) verified: $text")
            } else {
                AiTestResult.Failure("Model returned an empty response.")
            }
        } catch (e: TimeoutCancellationException) {
            AiTestResult.Failure("Connection timed out after 30 seconds.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini connection test failed", e)
            AiTestResult.Failure(e.localizedMessage ?: "Firebase AI Logic verification failed.")
        }
    }

    suspend fun detectViralMoments(
        config: StreamScanConfig,
        audioChunkFile: File?,
        onStageUpdate: (String) -> Unit
    ): Result<List<DetectedMoment>> = withContext(Dispatchers.IO) {
        val safeModel = validateSupportedModel(config.geminiModelId)
        val totalSec = (config.metadata.durationMs / 1000L).toInt().coerceAtLeast(config.clipDurationSec)

        val genConfig = generationConfig {
            responseMimeType = "application/json"
            responseSchema = momentListSchema
        }

        val model = Firebase.ai.generativeModel(
            modelName = safeModel,
            generationConfig = genConfig
        )

        val audioBytes: ByteArray? = audioChunkFile?.takeIf { it.exists() && it.length() in 1..MAX_INLINE_AUDIO_BYTES }
            ?.readBytes()

        val prompt = buildString {
            appendLine("You are an elite viral short-form video producer for BROHI CLIPPING.")
            appendLine("Analyze this stream/video and identify exactly ${config.clipCount} high-retention viral moments.")
            appendLine("Source Title: ${config.metadata.title}")
            appendLine("Creator / Streamer: ${config.metadata.streamer}")
            appendLine("Platform: ${config.metadata.platform}")
            appendLine("Total Available Duration: $totalSec seconds.")
            appendLine("Target Clip Duration: ~${config.clipDurationSec} seconds (between 20 and 60 seconds, staying strictly within 0..$totalSec).")
            appendLine("Target Language: ${config.language.displayName}")
            appendLine("Voice Mode: ${config.voiceMode.name} (${config.voiceMode.description})")
            appendLine("Voice Persona: ${config.voiceOption.displayName} (${config.voiceOption.toneHint})")
            if (audioBytes != null) {
                appendLine("An extracted audio track from the source video is attached. Listen closely for hype reactions, speech peaks, clutch gameplay callouts, laughter, and dramatic shifts.")
            } else {
                appendLine("No raw audio stream was attached; plan optimal highlight timestamps across 0..$totalSec based on the title, creator style, and pacing, with complete scripts and captions.")
            }
            when (config.voiceMode) {
                VoiceMode.INTRO -> appendLine("For narrationScript: write a punchy 3-5 second intro hook (10-16 words) in ${config.language.displayName}.")
                VoiceMode.FULL -> appendLine("For narrationScript: write an engaging full-clip narration script (~45-70 words) in ${config.language.displayName}.")
                VoiceMode.OFF -> appendLine("For narrationScript: return an empty string since commentary is OFF.")
            }
            appendLine("For captionText: write crisp punchy spoken captions (12-30 words) in ${config.language.displayName}.")
        }

        var attempt = 0
        var lastError: Exception? = null
        while (attempt < 3) {
            attempt++
            try {
                onStageUpdate(
                    if (audioBytes != null) "Sending ${audioBytes.size / 1024} KB audio + metadata to $safeModel (attempt $attempt)..."
                    else "Querying $safeModel for viral moment extraction (attempt $attempt)..."
                )

                val response = withTimeout(60_000L) {
                    if (audioBytes != null) {
                        val mime = if (audioChunkFile?.extension?.lowercase() == "wav") "audio/wav" else "audio/mp4"
                        model.generateContent(
                            content {
                                inlineData(audioBytes, mime)
                                text(prompt)
                            }
                        )
                    } else {
                        model.generateContent(prompt)
                    }
                }

                val jsonString = response.text
                    ?: throw IllegalStateException("Gemini returned an empty response.")

                val parsed = parseMomentsJson(jsonString, totalSec, config.clipDurationSec)
                if (parsed.isEmpty()) {
                    throw IllegalStateException("Gemini returned zero valid clip moments.")
                }
                return@withContext Result.success(parsed)
            } catch (e: CancellationException) {
                throw e
            } catch (e: TimeoutCancellationException) {
                lastError = IllegalStateException("Gemini inference timed out after 60s.", e)
                Log.e(TAG, "Timeout on attempt $attempt", e)
            } catch (e: Exception) {
                lastError = e
                Log.e(TAG, "Gemini moment detection error on attempt $attempt", e)
            }
            if (attempt < 3) {
                delay(1200L * attempt)
            }
        }

        Result.failure(lastError ?: IllegalStateException("AI moment detection failed."))
    }

    /**
     * Synthesizes spoken narration audio into a real PCM/WAV file on disk using Android's
     * local TextToSpeech synthesizer with locale, pitch, and rate matched to the selected
     * Gemini Voice & Target Language.
     */
    suspend fun synthesizeNarrationWav(
        script: String,
        language: TargetLanguage,
        voiceOption: AiVoiceOption,
        outputWavFile: File
    ): Boolean = withContext(Dispatchers.Main) {
        if (script.isBlank()) return@withContext false
        outputWavFile.parentFile?.mkdirs()

        suspendCancellableCoroutine { cont ->
            var ttsRef: TextToSpeech? = null
            val utteranceId = UUID.randomUUID().toString()
            ttsRef = TextToSpeech(context.applicationContext) { status ->
                val tts = ttsRef
                if (status != TextToSpeech.SUCCESS || tts == null) {
                    runCatching { tts?.shutdown() }
                    if (cont.isActive) cont.resume(false)
                    return@TextToSpeech
                }
                val locale = Locale.forLanguageTag(language.localeTag)
                val langSupport = tts.isLanguageAvailable(locale)
                if (langSupport >= TextToSpeech.LANG_AVAILABLE) {
                    tts.language = locale
                } else {
                    tts.language = Locale.US
                }
                tts.setPitch(voiceOption.pitch)
                tts.setSpeechRate(voiceOption.rate)
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) = Unit
                    override fun onDone(id: String?) {
                        runCatching { tts.shutdown() }
                        if (cont.isActive) {
                            cont.resume(outputWavFile.exists() && outputWavFile.length() > 44L)
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(id: String?) {
                        runCatching { tts.shutdown() }
                        if (cont.isActive) cont.resume(false)
                    }

                    override fun onError(id: String?, errorCode: Int) {
                        runCatching { tts.shutdown() }
                        if (cont.isActive) cont.resume(false)
                    }
                })
                val params = Bundle()
                val result = tts.synthesizeToFile(script, params, outputWavFile, utteranceId)
                if (result != TextToSpeech.SUCCESS) {
                    runCatching { tts.shutdown() }
                    if (cont.isActive) cont.resume(false)
                }
            }
            cont.invokeOnCancellation {
                runCatching {
                    ttsRef?.stop()
                    ttsRef?.shutdown()
                }
            }
        }
    }

    private fun parseMomentsJson(
        jsonString: String,
        totalSec: Int,
        targetDurationSec: Int
    ): List<DetectedMoment> {
        val root = JSONObject(jsonString)
        val array = root.getJSONArray("moments")
        val list = mutableListOf<DetectedMoment>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val rawStart = obj.optInt("startSec", i * 30).coerceIn(0, (totalSec - 10).coerceAtLeast(0))
            val rawEnd = obj.optInt("endSec", rawStart + targetDurationSec)
                .coerceIn(rawStart + 10, totalSec.coerceAtLeast(rawStart + 15))
            val clampedEnd = if (rawEnd - rawStart > 60) rawStart + 60 else rawEnd
            val score = obj.optInt("viralScore", 82).coerceIn(1, 100)
            val title = obj.optString("title", "Viral Highlight #${i + 1}").trim()
            val hook = obj.optString("hook", "Watch what happens next!").trim()
            val explanation = obj.optString("explanation", "High-energy moment with strong viewer retention.").trim()
            val narration = obj.optString("narrationScript", "").trim()
            val caption = obj.optString("captionText", hook).trim()

            val durationMs = ((clampedEnd - rawStart) * 1000L).coerceAtLeast(5_000L)
            val cues = buildWordCues(caption, durationMs)

            list.add(
                DetectedMoment(
                    id = UUID.randomUUID().toString(),
                    startSec = rawStart,
                    endSec = clampedEnd,
                    viralScore = score,
                    title = title,
                    hook = hook,
                    explanation = explanation,
                    narrationScript = narration,
                    captionText = caption,
                    wordCues = cues,
                    isSelected = true
                )
            )
        }
        return list.sortedByDescending { it.viralScore }
    }

    fun buildWordCues(captionText: String, clipDurationMs: Long): List<CaptionWordCue> {
        val words = captionText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()
        val usableMs = (clipDurationMs - 400L).coerceAtLeast(2_000L)
        val stepMs = (usableMs / words.size).coerceAtLeast(180L)
        return words.mapIndexed { idx, w ->
            val start = 200L + idx * stepMs
            val end = (start + stepMs).coerceAtMost(clipDurationMs)
            CaptionWordCue(word = w, startMs = start, endMs = end)
        }
    }

    private fun validateSupportedModel(modelId: String): String {
        return GeminiModelOption.entries.firstOrNull { it.modelId == modelId }?.modelId
            ?: GeminiModelOption.FLASH_38.modelId
    }

    companion object {
        private const val TAG = "GeminiAiService"
        private const val MAX_INLINE_AUDIO_BYTES = 18 * 1024 * 1024
    }
}
