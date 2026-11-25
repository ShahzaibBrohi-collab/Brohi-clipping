package com.example.data.network

import android.util.Log
import com.example.data.model.TwitchStreamInfo
import com.example.data.security.KeystoreCryptoManager
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class TwitchTokenResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "expires_in") val expiresIn: Long
)

@JsonClass(generateAdapter = true)
data class TwitchGameItem(
    val id: String,
    val name: String
)

@JsonClass(generateAdapter = true)
data class TwitchGamesResponse(
    val data: List<TwitchGameItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TwitchStreamItem(
    val id: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "user_login") val userLogin: String,
    @Json(name = "user_name") val userName: String,
    @Json(name = "game_name") val gameName: String,
    val title: String,
    @Json(name = "viewer_count") val viewerCount: Int,
    val language: String,
    @Json(name = "thumbnail_url") val thumbnailUrl: String
)

@JsonClass(generateAdapter = true)
data class TwitchStreamsResponse(
    val data: List<TwitchStreamItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TwitchUserItem(
    val id: String,
    @Json(name = "profile_image_url") val profileImageUrl: String
)

@JsonClass(generateAdapter = true)
data class TwitchUsersResponse(
    val data: List<TwitchUserItem> = emptyList()
)

sealed interface TwitchResult<out T> {
    data class Success<T>(val data: T) : TwitchResult<T>
    data class MissingCredentials(val message: String) : TwitchResult<Nothing>
    data class Error(val message: String) : TwitchResult<Nothing>
}

class TwitchHelixService(
    private val cryptoManager: KeystoreCryptoManager
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val tokenAdapter = moshi.adapter(TwitchTokenResponse::class.java)
    private val gamesAdapter = moshi.adapter(TwitchGamesResponse::class.java)
    private val streamsAdapter = moshi.adapter(TwitchStreamsResponse::class.java)
    private val usersAdapter = moshi.adapter(TwitchUsersResponse::class.java)

    private var cachedToken: String? = null
    private var tokenExpiresAtMs: Long = 0L

    fun hasCredentials(): Boolean {
        val id = cryptoManager.getDecrypted(KeystoreCryptoManager.KEY_TWITCH_CLIENT_ID)
        val secret = cryptoManager.getDecrypted(KeystoreCryptoManager.KEY_TWITCH_CLIENT_SECRET)
        return id.isNotBlank() && secret.isNotBlank()
    }

    suspend fun testConnection(): TwitchResult<String> = withContext(Dispatchers.IO) {
        val clientId = cryptoManager.getDecrypted(KeystoreCryptoManager.KEY_TWITCH_CLIENT_ID).trim()
        val clientSecret = cryptoManager.getDecrypted(KeystoreCryptoManager.KEY_TWITCH_CLIENT_SECRET).trim()
        if (clientId.isBlank() || clientSecret.isBlank()) {
            return@withContext TwitchResult.MissingCredentials(
                "Enter your Twitch Client ID and Client Secret in Settings first."
            )
        }
        cachedToken = null
        when (val tokenRes = fetchAccessToken(clientId, clientSecret)) {
            is TwitchResult.Success -> TwitchResult.Success("Connected to Twitch Helix API successfully.")
            is TwitchResult.MissingCredentials -> tokenRes
            is TwitchResult.Error -> tokenRes
        }
    }

    suspend fun fetchLiveStreamsByCategory(categoryDisplayName: String): TwitchResult<List<TwitchStreamInfo>> =
        withContext(Dispatchers.IO) {
            val clientId = cryptoManager.getDecrypted(KeystoreCryptoManager.KEY_TWITCH_CLIENT_ID).trim()
            val clientSecret = cryptoManager.getDecrypted(KeystoreCryptoManager.KEY_TWITCH_CLIENT_SECRET).trim()
            if (clientId.isBlank() || clientSecret.isBlank()) {
                return@withContext TwitchResult.MissingCredentials(
                    "Twitch Developer credentials required. Add your Client ID and Secret in Settings to discover real live streams."
                )
            }

            val token = when (val tokenRes = fetchAccessToken(clientId, clientSecret)) {
                is TwitchResult.Success -> tokenRes.data
                is TwitchResult.MissingCredentials -> return@withContext tokenRes
                is TwitchResult.Error -> return@withContext tokenRes
            }

            val twitchGameQuery = when (categoryDisplayName.uppercase()) {
                "GTA V" -> "Grand Theft Auto V"
                "VALORANT" -> "VALORANT"
                "MINECRAFT" -> "Minecraft"
                else -> "Just Chatting"
            }

            try {
                val encodedGame = java.net.URLEncoder.encode(twitchGameQuery, "UTF-8")
                val gameReq = Request.Builder()
                    .url("https://api.twitch.tv/helix/games?name=$encodedGame")
                    .header("Client-Id", clientId)
                    .header("Authorization", "Bearer $token")
                    .build()

                val gameBody = client.newCall(gameReq).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        return@withContext TwitchResult.Error("Twitch Games API error (${resp.code}).")
                    }
                    resp.body?.string().orEmpty()
                }

                val gameId = gamesAdapter.fromJson(gameBody)?.data?.firstOrNull()?.id
                    ?: return@withContext TwitchResult.Error("Category '$twitchGameQuery' not found on Twitch.")

                val streamsReq = Request.Builder()
                    .url("https://api.twitch.tv/helix/streams?game_id=$gameId&first=15")
                    .header("Client-Id", clientId)
                    .header("Authorization", "Bearer $token")
                    .build()

                val streamsBody = client.newCall(streamsReq).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        return@withContext TwitchResult.Error("Twitch Streams API error (${resp.code}).")
                    }
                    resp.body?.string().orEmpty()
                }

                val streamItems = streamsAdapter.fromJson(streamsBody)?.data.orEmpty()
                if (streamItems.isEmpty()) {
                    return@withContext TwitchResult.Success(emptyList())
                }

                val avatarMap = fetchUserAvatars(clientId, token, streamItems.map { it.userId })

                val result = streamItems.map { item ->
                    val thumb = item.thumbnailUrl
                        .replace("{width}", "640")
                        .replace("{height}", "360")
                    TwitchStreamInfo(
                        id = item.id,
                        userId = item.userId,
                        userName = item.userName,
                        title = item.title,
                        gameName = item.gameName,
                        viewerCount = item.viewerCount,
                        language = item.language.uppercase(),
                        thumbnailUrl = thumb,
                        avatarUrl = avatarMap[item.userId].orEmpty(),
                        streamUrl = "https://www.twitch.tv/${item.userLogin}"
                    )
                }
                TwitchResult.Success(result)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch live Twitch streams", e)
                TwitchResult.Error(e.localizedMessage ?: "Network error contacting Twitch Helix API.")
            }
        }

    suspend fun fetchStreamByLogin(userLogin: String): TwitchStreamInfo? = withContext(Dispatchers.IO) {
        val clientId = cryptoManager.getDecrypted(KeystoreCryptoManager.KEY_TWITCH_CLIENT_ID).trim()
        val clientSecret = cryptoManager.getDecrypted(KeystoreCryptoManager.KEY_TWITCH_CLIENT_SECRET).trim()
        if (clientId.isBlank() || clientSecret.isBlank()) return@withContext null
        val token = (fetchAccessToken(clientId, clientSecret) as? TwitchResult.Success)?.data ?: return@withContext null

        try {
            val encoded = java.net.URLEncoder.encode(userLogin.trim(), "UTF-8")
            val req = Request.Builder()
                .url("https://api.twitch.tv/helix/streams?user_login=$encoded")
                .header("Client-Id", clientId)
                .header("Authorization", "Bearer $token")
                .build()
            val body = client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                resp.body?.string().orEmpty()
            }
            val item = streamsAdapter.fromJson(body)?.data?.firstOrNull() ?: return@withContext null
            val avatarMap = fetchUserAvatars(clientId, token, listOf(item.userId))
            TwitchStreamInfo(
                id = item.id,
                userId = item.userId,
                userName = item.userName,
                title = item.title,
                gameName = item.gameName,
                viewerCount = item.viewerCount,
                language = item.language.uppercase(),
                thumbnailUrl = item.thumbnailUrl.replace("{width}", "640").replace("{height}", "360"),
                avatarUrl = avatarMap[item.userId].orEmpty(),
                streamUrl = "https://www.twitch.tv/${item.userLogin}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inspect Twitch stream by login", e)
            null
        }
    }

    private fun fetchAccessToken(clientId: String, clientSecret: String): TwitchResult<String> {
        val now = System.currentTimeMillis()
        val existing = cachedToken
        if (!existing.isNullOrBlank() && now < tokenExpiresAtMs - 60_000L) {
            return TwitchResult.Success(existing)
        }
        return try {
            val formBody = FormBody.Builder()
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("grant_type", "client_credentials")
                .build()
            val req = Request.Builder()
                .url("https://id.twitch.tv/oauth2/token")
                .post(formBody)
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return TwitchResult.Error("Twitch OAuth rejected credentials (HTTP ${resp.code}).")
                }
                val body = resp.body?.string().orEmpty()
                val parsed = tokenAdapter.fromJson(body)
                    ?: return TwitchResult.Error("Invalid Twitch OAuth response.")
                cachedToken = parsed.accessToken
                tokenExpiresAtMs = System.currentTimeMillis() + (parsed.expiresIn * 1000L)
                TwitchResult.Success(parsed.accessToken)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Twitch OAuth token request failed", e)
            TwitchResult.Error(e.localizedMessage ?: "Failed to authenticate with Twitch.")
        }
    }

    private fun fetchUserAvatars(clientId: String, token: String, userIds: List<String>): Map<String, String> {
        if (userIds.isEmpty()) return emptyMap()
        return try {
            val query = userIds.distinct().take(20).joinToString("&") {
                "id=${java.net.URLEncoder.encode(it, "UTF-8")}"
            }
            val req = Request.Builder()
                .url("https://api.twitch.tv/helix/users?$query")
                .header("Client-Id", clientId)
                .header("Authorization", "Bearer $token")
                .build()
            val body = client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyMap()
                resp.body?.string().orEmpty()
            }
            usersAdapter.fromJson(body)?.data?.associate { it.id to it.profileImageUrl } ?: emptyMap()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch Twitch user avatars", e)
            emptyMap()
        }
    }

    companion object {
        private const val TAG = "TwitchHelixService"
    }
}
