package com.example.data.media

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Typeface
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.MatrixTransformation
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.BuildConfig
import com.example.R
import com.example.data.model.EditingConfig
import com.example.data.model.EngineStatus
import com.example.data.model.LayoutMode
import com.example.data.model.StorageStats
import com.example.data.model.StudioFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.coroutines.resume

class VideoProcessingEngine(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var activeTransformer: Transformer? = null

    val shortsDir: File
        get() = File(context.filesDir, "shorts").apply { mkdirs() }

    val exportsDir: File
        get() = File(context.filesDir, "exports").apply { mkdirs() }

    val tempDir: File
        get() = File(context.cacheDir, "brohi_temp").apply { mkdirs() }

    fun initializeEngine(): EngineStatus {
        shortsDir.mkdirs()
        exportsDir.mkdirs()
        tempDir.mkdirs()
        return checkEngineStatus()
    }

    fun checkEngineStatus(): EngineStatus {
        val codecs = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
        var hwVideo: String? = null
        var swVideo: String? = null
        var aacAudio = false

        for (info in codecs) {
            if (!info.isEncoder) continue
            val types = info.supportedTypes
            if (types.any { it.equals(MediaFormat.MIMETYPE_VIDEO_AVC, true) || it.equals(MediaFormat.MIMETYPE_VIDEO_HEVC, true) }) {
                val isSw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    info.isSoftwareOnly
                } else {
                    info.name.startsWith("OMX.google.", true) || info.name.startsWith("c2.android.", true)
                }
                if (isSw && swVideo == null) swVideo = info.name
                if (!isSw && hwVideo == null) hwVideo = info.name
            }
            if (types.any { it.equals(MediaFormat.MIMETYPE_AUDIO_AAC, true) }) {
                aacAudio = true
            }
        }

        return EngineStatus(
            media3Ready = true,
            hardwareVideoEncoderName = hwVideo,
            softwareVideoEncoderName = swVideo,
            audioAacEncoderAvailable = aacAudio,
            supportsAudioDucking = true,
            supportsAssAndOverlayBurnIn = true
        )
    }

    /**
     * Extracts up to 90 seconds of compressed audio directly from the source video track
     * using MediaExtractor + MediaMuxer into an MP4/M4A file under 18MB for Gemini analysis.
     */
    suspend fun extractAudioSegment(
        sourceUri: Uri,
        startMs: Long = 0L,
        maxDurationMs: Long = 90_000L,
        outputFile: File
    ): File? = withContext(Dispatchers.IO) {
        outputFile.parentFile?.mkdirs()
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            extractor.setDataSource(context, sourceUri, null)
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }
            if (audioTrackIndex == -1 || audioFormat == null) return@withContext null

            extractor.selectTrack(audioTrackIndex)
            if (startMs > 0L) {
                extractor.seekTo(startMs * 1000L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            }

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerTrackIndex = muxer.addTrack(audioFormat)
            muxer.start()

            val bufferSize = 256 * 1024
            val buffer = ByteBuffer.allocate(bufferSize)
            val bufferInfo = android.media.MediaCodec.BufferInfo()
            val endUs = (startMs + maxDurationMs) * 1000L

            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs > endUs) break

                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = sampleTimeUs
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                extractor.advance()
            }

            muxer.stop()
            if (outputFile.exists() && outputFile.length() > 1024L) outputFile else null
        } catch (e: Exception) {
            Log.e(TAG, "Audio extraction failed", e)
            null
        } finally {
            runCatching { muxer?.release() }
            runCatching { extractor.release() }
        }
    }

    suspend fun generateThumbnail(
        videoUriOrPath: String,
        atMs: Long,
        outputJpg: File
    ): File? = withContext(Dispatchers.IO) {
        outputJpg.parentFile?.mkdirs()
        val retriever = MediaMetadataRetriever()
        try {
            val uri = Uri.parse(videoUriOrPath)
            if (uri.scheme == "content" || uri.scheme == "http" || uri.scheme == "https") {
                retriever.setDataSource(context, uri)
            } else {
                retriever.setDataSource(videoUriOrPath)
            }
            val frame = retriever.getFrameAtTime(atMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
                ?: return@withContext null

            FileOutputStream(outputJpg).use { out ->
                frame.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
            frame.recycle()
            if (outputJpg.exists()) outputJpg else null
        } catch (e: Exception) {
            Log.e(TAG, "Thumbnail generation failed", e)
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    /**
     * Real 1080x1920 9:16 vertical short rendering using Jetpack Media3 Transformer.
     * Applies trim range, zoom/pan/framing mode, burned-in karaoke captions & masks/shapes,
     * and multi-track audio mixing with automatic ducking.
     */
    @OptIn(UnstableApi::class)
    suspend fun renderVerticalShort(
        sourceUri: Uri,
        voiceWavFile: File?,
        config: EditingConfig,
        outputMp4File: File,
        preferHardwareEncoder: Boolean,
        onProgress: (Int) -> Unit
    ): Result<File> = withContext(Dispatchers.Main) {
        outputMp4File.parentFile?.mkdirs()
        if (outputMp4File.exists()) outputMp4File.delete()

        val clipDurationMs = (config.endMs - config.startMs).coerceAtLeast(3_000L)
        val mainMediaItem = MediaItem.Builder()
            .setUri(sourceUri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(config.startMs.coerceAtLeast(0L))
                    .setEndPositionMs(config.endMs.coerceAtLeast(config.startMs + 3_000L))
                    .build()
            )
            .build()

        val videoEffects = mutableListOf<Effect>()

        // Zoom + Pan transformation
        if (config.zoom != 1.0f || config.panX != 0f || config.panY != 0f) {
            videoEffects.add(
                ScaleAndRotateTransformation.Builder()
                    .setScale(config.zoom.coerceIn(0.6f, 3.0f), config.zoom.coerceIn(0.6f, 3.0f))
                    .build()
            )
            videoEffects.add(MatrixTransformation { _ ->
                Matrix().apply {
                    postTranslate(config.panX.coerceIn(-1f, 1f) * 0.45f, -config.panY.coerceIn(-1f, 1f) * 0.45f)
                }
            })
        }

        // 1080x1920 Vertical Framing (FILL vs BLUR/FIT)
        val layoutStrategy = if (config.layoutMode == LayoutMode.FILL) {
            Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
        } else {
            Presentation.LAYOUT_SCALE_TO_FIT
        }
        videoEffects.add(Presentation.createForWidthAndHeight(1080, 1920, layoutStrategy))

        // Burned-in Karaoke Subtitles, Blur Masks & Solid Shapes
        val typeface = loadTypeface(config.fontFamily)
        val overlayBitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
        val overlay = object : BitmapOverlay() {
            override fun getBitmap(presentationTimeUs: Long): Bitmap {
                val timeMs = presentationTimeUs / 1000L
                AssSubtitleGenerator.renderOverlayFrame(overlayBitmap, timeMs, config, typeface)
                return overlayBitmap
            }
        }
        videoEffects.add(OverlayEffect(listOf(overlay)))

        // Audio ducking windows when AI narration file is present
        val hasVoice = voiceWavFile != null && voiceWavFile.exists() && voiceWavFile.length() > 44L
        val duckWindows = if (hasVoice) {
            val voiceDurationMs = probeAudioDurationMs(voiceWavFile!!).coerceAtMost(clipDurationMs)
            listOf(0L to voiceDurationMs)
        } else {
            emptyList()
        }

        val mainAudioProcessors = listOf<AudioProcessor>(
            VolumeDuckingAudioProcessor(
                baseGain = config.originalAudioVolume,
                duckEnabled = config.duckingEnabled && hasVoice,
                duckWindowsMs = duckWindows
            )
        )

        val mainEditedItem = EditedMediaItem.Builder(mainMediaItem)
            .setEffects(Effects(mainAudioProcessors, videoEffects))
            .build()

        val sequences = mutableListOf(
            EditedMediaItemSequence(listOf(mainEditedItem))
        )

        // Add AI Narration track if available
        if (hasVoice && config.voiceVolume > 0.01f) {
            val voiceItem = MediaItem.Builder()
                .setUri(Uri.fromFile(voiceWavFile))
                .build()
            val voiceAudioProc = listOf<AudioProcessor>(
                VolumeDuckingAudioProcessor(
                    baseGain = config.voiceVolume,
                    duckEnabled = false
                )
            )
            val editedVoice = EditedMediaItem.Builder(voiceItem)
                .setRemoveVideo(true)
                .setEffects(Effects(voiceAudioProc, emptyList()))
                .build()
            sequences.add(EditedMediaItemSequence(listOf(editedVoice)))
        }

        // Add optional Background Music track
        val musicUriStr = config.musicUri
        if (!musicUriStr.isNullOrBlank() && config.musicVolume > 0.01f) {
            val musicItem = MediaItem.Builder()
                .setUri(Uri.parse(musicUriStr))
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(0L)
                        .setEndPositionMs(clipDurationMs)
                        .build()
                )
                .build()
            val musicProc = listOf<AudioProcessor>(
                VolumeDuckingAudioProcessor(
                    baseGain = config.musicVolume,
                    duckEnabled = config.duckingEnabled && hasVoice,
                    duckWindowsMs = duckWindows
                )
            )
            val editedMusic = EditedMediaItem.Builder(musicItem)
                .setRemoveVideo(true)
                .setEffects(Effects(musicProc, emptyList()))
                .build()
            sequences.add(EditedMediaItemSequence(listOf(editedMusic)))
        }

        val composition = Composition.Builder(sequences).build()

        suspendCancellableCoroutine { cont ->
            var polling = true
            val progressHolder = ProgressHolder()
            lateinit var transformer: Transformer

            val listener = object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    polling = false
                    activeTransformer = null
                    runCatching { overlayBitmap.recycle() }
                    onProgress(100)
                    if (cont.isActive) {
                        cont.resume(Result.success(outputMp4File))
                    }
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    polling = false
                    activeTransformer = null
                    runCatching { overlayBitmap.recycle() }
                    Log.e(TAG, "Media3 Transformer export failed", exportException)
                    if (cont.isActive) {
                        cont.resume(Result.failure(exportException))
                    }
                }
            }

            val encoderFactory = DefaultEncoderFactory.Builder(context)
                .setEnableFallback(true)
                .build()

            transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .setEncoderFactory(encoderFactory)
                .addListener(listener)
                .build()

            activeTransformer = transformer

            val progressRunnable = object : Runnable {
                override fun run() {
                    if (!polling) return
                    val state = transformer.getProgress(progressHolder)
                    if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                        onProgress(progressHolder.progress.coerceIn(0, 99))
                    }
                    if (state != Transformer.PROGRESS_STATE_NOT_STARTED && polling) {
                        mainHandler.postDelayed(this, 300L)
                    }
                }
            }

            cont.invokeOnCancellation {
                polling = false
                mainHandler.post {
                    runCatching { transformer.cancel() }
                    activeTransformer = null
                    runCatching { overlayBitmap.recycle() }
                }
            }

            try {
                transformer.start(composition, outputMp4File.absolutePath)
                mainHandler.postDelayed(progressRunnable, 300L)
            } catch (e: Exception) {
                polling = false
                activeTransformer = null
                runCatching { overlayBitmap.recycle() }
                if (cont.isActive) cont.resume(Result.failure(e))
            }
        }
    }

    fun cancelActiveRender() {
        mainHandler.post {
            runCatching { activeTransformer?.cancel() }
            activeTransformer = null
        }
    }

    suspend fun saveToGallery(mp4File: File, displayTitle: String): Result<Uri> = withContext(Dispatchers.IO) {
        if (!mp4File.exists()) {
            return@withContext Result.failure(IllegalStateException("Rendered MP4 file not found."))
        }
        try {
            val safeName = displayTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(40)
            val fileName = "Brohi_${safeName}_${System.currentTimeMillis()}.mp4"

            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/BrohiClipping")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext Result.failure(IllegalStateException("MediaStore failed to create Gallery entry."))

            resolver.openOutputStream(uri)?.use { out ->
                mp4File.inputStream().use { input ->
                    input.copyTo(out)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            Result.success(uri)
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving MP4 to Gallery", e)
            Result.failure(e)
        }
    }

    fun createShareIntent(mp4File: File, title: String): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            mp4File
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    suspend fun getStorageUsage(totalShortsCount: Int): StorageStats = withContext(Dispatchers.IO) {
        StorageStats(
            shortsBytes = folderSize(shortsDir),
            exportsBytes = folderSize(exportsDir),
            tempCacheBytes = folderSize(tempDir) + folderSize(context.cacheDir),
            totalShortsCount = totalShortsCount
        )
    }

    suspend fun cleanupTemporaryFiles(): Long = withContext(Dispatchers.IO) {
        val freed = folderSize(tempDir)
        runCatching { tempDir.deleteRecursively() }
        tempDir.mkdirs()
        freed
    }

    private fun folderSize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private fun probeAudioDurationMs(wavFile: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(wavFile.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 4_000L
        } catch (e: Exception) {
            4_000L
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun loadTypeface(font: StudioFont): Typeface? {
        val resId = when (font) {
            StudioFont.OUTFIT -> R.font.outfit
            StudioFont.BEBAS_NEUE -> R.font.bebas_neue
            StudioFont.PLUS_JAKARTA_SANS -> R.font.plus_jakarta_sans
            StudioFont.JETBRAINS_MONO -> R.font.jetbrains_mono
        }
        return runCatching { ResourcesCompat.getFont(context, resId) }.getOrNull()
    }

    companion object {
        private const val TAG = "VideoProcessingEngine"
    }
}
