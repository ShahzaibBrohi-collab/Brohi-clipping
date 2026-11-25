package com.example

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import com.example.data.media.AssSubtitleGenerator
import com.example.data.media.VolumeDuckingAudioProcessor
import com.example.data.model.BlurMaskRect
import com.example.data.model.CaptionStyle
import com.example.data.model.CaptionWordCue
import com.example.data.model.DetectedMoment
import com.example.data.model.EditingConfig
import com.example.data.model.LayoutMode
import com.example.data.model.SolidShapeRect
import com.example.data.model.TargetLanguage
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BrohiClippingUnitTest {

    @Test
    fun `ass subtitle generator creates valid 1080x1920 karaoke script`() {
        val tempFile = File.createTempFile("brohi_test", ".ass")
        try {
            val config = EditingConfig(
                captionStyle = CaptionStyle.BOX,
                primaryColorHex = "#F8F2E4",
                highlightColorHex = "#FFC81F",
                wordCues = listOf(
                    CaptionWordCue("CLUTCH", 200L, 700L),
                    CaptionWordCue("PLAY", 700L, 1200L),
                    CaptionWordCue("NOW", 1200L, 1800L)
                )
            )

            AssSubtitleGenerator.generateAssFile(config, "Clutch Highlight", tempFile)
            val text = tempFile.readText()

            assertTrue(text.contains("PlayResX: 1080"))
            assertTrue(text.contains("PlayResY: 1920"))
            assertTrue(text.contains("{\\kf50}CLUTCH"))
            assertTrue(text.contains("{\\kf50}PLAY"))
            assertTrue(text.contains("{\\kf60}NOW"))
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `editing config moshi serialization roundtrips accurately`() {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(EditingConfig::class.java)

        val original = EditingConfig(
            startMs = 5_000L,
            endMs = 35_000L,
            layoutMode = LayoutMode.FILL,
            zoom = 1.45f,
            panX = -0.25f,
            panY = 0.15f,
            duckingEnabled = true,
            blurMasks = listOf(BlurMaskRect(id = "b1", xFraction = 0.1f)),
            solidShapes = listOf(SolidShapeRect(id = "s1", colorHex = "#FFC81F"))
        )

        val json = adapter.toJson(original)
        val restored = adapter.fromJson(json)

        assertEquals(original, restored)
    }

    @Test
    fun `audio processor scales gain and ducks during narration window`() {
        val processor = VolumeDuckingAudioProcessor(
            baseGain = 1.0f,
            duckEnabled = true,
            duckWindowsMs = listOf(0L to 1000L),
            duckFactor = 0.25f
        )
        processor.configure(AudioProcessor.AudioFormat(1000, 1, C.ENCODING_PCM_16BIT))
        processor.flush()

        val inBuf = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
        inBuf.putShort(2000)
        inBuf.putShort(2000)
        inBuf.flip()

        processor.queueInput(inBuf)
        val outBuf = processor.output.order(ByteOrder.nativeOrder())
        val firstSample = outBuf.short
        assertEquals(500.toShort(), firstSample)
    }

    @Test
    fun `detected moment computes clamped duration and all 8 target languages exist`() {
        val moment = DetectedMoment(
            id = "m1",
            startSec = 10,
            endSec = 42,
            viralScore = 94,
            title = "Final Zone Victory",
            hook = "Wait for the final shot!",
            explanation = "High crowd & streamer reaction spike.",
            narrationScript = "Watch this clutch play unfold.",
            captionText = "Watch this clutch play unfold"
        )
        assertEquals(32, moment.durationSec)
        assertEquals(8, TargetLanguage.entries.size)
    }
}
