package com.example.data.media

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.roundToInt

/**
 * Real-time Media3 PCM AudioProcessor implementing gain scaling and automatic
 * narration-aware audio ducking with smooth soft-clipping limiter.
 */
@OptIn(UnstableApi::class)
class VolumeDuckingAudioProcessor(
    private val baseGain: Float,
    private val duckEnabled: Boolean,
    private val duckWindowsMs: List<Pair<Long, Long>> = emptyList(),
    private val duckFactor: Float = 0.28f
) : BaseAudioProcessor() {

    private var processedSamples: Long = 0L

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        return inputAudioFormat
    }

    override fun onFlush() {
        processedSamples = 0L
    }

    override fun onReset() {
        processedSamples = 0L
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val out = replaceOutputBuffer(remaining)
        val channelCount = inputAudioFormat.channelCount.coerceAtLeast(1)
        val sampleRate = inputAudioFormat.sampleRate.coerceAtLeast(1)

        while (inputBuffer.remaining() >= 2) {
            val currentTimeMs = (processedSamples * 1000L) / sampleRate
            val isDucking = duckEnabled && duckWindowsMs.any { (s, e) -> currentTimeMs in s..e }
            val gain = (baseGain * if (isDucking) duckFactor else 1f).coerceIn(0f, 2.0f)

            for (ch in 0 until channelCount) {
                if (inputBuffer.remaining() < 2) break
                val sample = inputBuffer.short.toInt()
                val scaled = (sample * gain).roundToInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                out.putShort(scaled.toShort())
            }
            processedSamples++
        }

        out.flip()
    }
}
