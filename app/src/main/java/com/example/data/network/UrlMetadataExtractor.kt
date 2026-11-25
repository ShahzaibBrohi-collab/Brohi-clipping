package com.example.data.network

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.example.data.model.SourceMediaMetadata
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.net.URI
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class YouTubeOEmbedResponse(
    val title: String? = null,
    @Json(name = "author_name") val authorName: String? = null,
    @Json(name = "thumbnail_url") val thumbnailUrl: String? = null
)

sealed interface UrlValidationResult {
    data class Valid(val metadata: SourceMediaMetadata) : UrlValidationResult
    data class Invalid(val error: String) : UrlValidationResult
}

class UrlMetadataExtractor(
    private val context: Context,
    private val twitchService: TwitchHelixService
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val oEmbedAdapter = moshi.adapter(YouTubeOEmbedResponse::class.java)

    suspend fun inspectSourceUrl(rawUrl: String): UrlValidationResult = withContext(Dispatchers.IO) {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) {
            return@withContext UrlValidationResult.Invalid("Please enter a YouTube, Twitch, or direct MP4 URL.")
        }

        val parsed = runCatching { URI(trimmed) }.getOrNull()
            ?: return@withContext UrlValidationResult.Invalid("Malformed URL format.")

        val scheme = parsed.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            return@withContext UrlValidationResult.Invalid("Only http:// or https:// URLs are supported.")
        }

        val host = parsed.host?.lowercase().orEmpty()
        if (host.isBlank() || isPrivateOrLoopbackHost(host)) {
            return@withContext UrlValidationResult.Invalid("Private or local network addresses are not permitted.")
        }

        return@withContext when {
            isYouTubeHost(host) -> inspectYouTubeUrl(trimmed)
            isTwitchHost(host) -> inspectTwitchUrl(trimmed, parsed)
            isDirectMediaUrl(trimmed) -> inspectDirectMediaUrl(trimmed)
            else -> UrlValidationResult.Invalid(
                "Unsupported platform. Paste a YouTube link, Twitch link, direct MP4/M3U8 URL, or import a local video."
            )
        }
    }

    suspend fun inspectLocalVideoUri(uri: Uri): UrlValidationResult = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 30_000L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 1920
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 1080
            val titleMeta = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: uri.lastPathSegment?.substringAfterLast('/')
                ?: "Imported Studio Video"

            UrlValidationResult.Valid(
                SourceMediaMetadata(
                    sourceUrl = uri.toString(),
                    localVideoUri = uri.toString(),
                    title = titleMeta,
                    streamer = "Local Media",
                    platform = "Local Device",
                    durationMs = durationMs.coerceAtLeast(5_000L),
                    width = width,
                    height = height,
                    thumbnailUrl = null,
                    isDirectPlayable = true,
                    streamNotice = null
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inspect local video URI", e)
            UrlValidationResult.Invalid("Unable to read video metadata from the selected file.")
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun inspectYouTubeUrl(url: String): UrlValidationResult {
        return try {
            val encoded = java.net.URLEncoder.encode(url, "UTF-8")
            val req = Request.Builder()
                .url("https://www.youtube.com/oembed?url=$encoded&format=json")
                .build()
            val oembed = client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) null
                else resp.body?.string()?.let { oEmbedAdapter.fromJson(it) }
            }
            if (oembed == null) {
                return UrlValidationResult.Invalid("Could not resolve public YouTube metadata. Check the URL or permissions.")
            }
            UrlValidationResult.Valid(
                SourceMediaMetadata(
                    sourceUrl = url,
                    localVideoUri = null,
                    title = oembed.title ?: "YouTube Video",
                    streamer = oembed.authorName ?: "YouTube Creator",
                    platform = "YouTube",
                    durationMs = 180_000L,
                    thumbnailUrl = oembed.thumbnailUrl,
                    isDirectPlayable = false,
                    streamNotice = "YouTube streams do not expose raw downloadable MP4 streams directly due to platform policies. Gemini will analyze metadata & generate scripts/timings; attach your authorized local recording in Studio or on the next step for full 1080×1920 video rendering."
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "YouTube metadata fetch failed", e)
            UrlValidationResult.Invalid("Network error retrieving YouTube metadata.")
        }
    }

    private suspend fun inspectTwitchUrl(url: String, parsed: URI): UrlValidationResult {
        val segments = parsed.path?.trim('/')?.split('/')?.filter { it.isNotBlank() }.orEmpty()
        val channelLogin = segments.firstOrNull()?.takeIf { it != "videos" && it != "directory" } ?: "streamer"
        val liveInfo = twitchService.fetchStreamByLogin(channelLogin)
        return if (liveInfo != null) {
            UrlValidationResult.Valid(
                SourceMediaMetadata(
                    sourceUrl = url,
                    localVideoUri = null,
                    title = liveInfo.title,
                    streamer = liveInfo.userName,
                    platform = "Twitch Live",
                    durationMs = 300_000L,
                    thumbnailUrl = liveInfo.thumbnailUrl,
                    isDirectPlayable = false,
                    streamNotice = "Twitch Helix metadata verified (${liveInfo.viewerCount} viewers in ${liveInfo.gameName}). Live encrypted streams require an authorized VOD/local recording for frame-accurate Media3 MP4 export."
                )
            )
        } else {
            UrlValidationResult.Valid(
                SourceMediaMetadata(
                    sourceUrl = url,
                    localVideoUri = null,
                    title = "Twitch Broadcast — $channelLogin",
                    streamer = channelLogin,
                    platform = "Twitch",
                    durationMs = 240_000L,
                    thumbnailUrl = null,
                    isDirectPlayable = false,
                    streamNotice = "Channel URL validated. Connect Twitch API credentials in Settings for live stream titles/avatars, or import your clip/VOD file for direct 9:16 Media3 rendering."
                )
            )
        }
    }

    private fun inspectDirectMediaUrl(url: String): UrlValidationResult {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(url, emptyMap())
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 60_000L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 1920
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 1080
            UrlValidationResult.Valid(
                SourceMediaMetadata(
                    sourceUrl = url,
                    localVideoUri = url,
                    title = url.substringAfterLast('/').substringBefore('?').ifBlank { "Direct Stream" },
                    streamer = URI(url).host ?: "Direct Stream",
                    platform = "Direct Media",
                    durationMs = durationMs.coerceAtLeast(5_000L),
                    width = width,
                    height = height,
                    isDirectPlayable = true,
                    streamNotice = null
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Direct media metadata probe fallback for $url", e)
            UrlValidationResult.Valid(
                SourceMediaMetadata(
                    sourceUrl = url,
                    localVideoUri = url,
                    title = url.substringAfterLast('/').substringBefore('?').ifBlank { "Direct Stream" },
                    streamer = runCatching { URI(url).host }.getOrNull() ?: "Direct Stream",
                    platform = "Direct Media",
                    durationMs = 90_000L,
                    isDirectPlayable = true,
                    streamNotice = null
                )
            )
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun isYouTubeHost(host: String): Boolean {
        return host == "youtube.com" || host.endsWith(".youtube.com") || host == "youtu.be"
    }

    private fun isTwitchHost(host: String): Boolean {
        return host == "twitch.tv" || host.endsWith(".twitch.tv")
    }

    private fun isDirectMediaUrl(url: String): Boolean {
        val lower = url.substringBefore('?').lowercase()
        return lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".webm") || lower.endsWith(".m3u8")
    }

    private fun isPrivateOrLoopbackHost(host: String): Boolean {
        if (host == "localhost" || host == "127.0.0.1" || host == "::1" || host == "0.0.0.0") return true
        if (host.startsWith("10.") || host.startsWith("192.168.") || host.startsWith("169.254.")) return true
        return runCatching {
            val addr = InetAddress.getByName(host)
            addr.isLoopbackAddress || addr.isSiteLocalAddress || addr.isLinkLocalAddress
        }.getOrDefault(false)
    }

    companion object {
        private const val TAG = "UrlMetadataExtractor"
    }
}
