package com.streambox.tv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Downloads and parses an M3U / M3U8 playlist into a list of [Channel].
 *
 * Works for:
 *  • Plain M3U URLs
 *  • Xtream Codes URLs built by [XtreamClient.playlistUrl] (which themselves
 *    return an M3U body)
 *  • URLs that require HTTP basic auth (rare but supported via username/password)
 */
@Singleton
class PlaylistFetcher @Inject constructor() {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    /**
     * Fetches the playlist body and returns parsed [Channel]s.
     *
     * @throws java.io.IOException on network errors
     * @throws PlaylistException for HTTP errors or empty/invalid playlists
     */
    suspend fun fetch(
        url: String,
        username: String? = null,
        password: String? = null,
    ): List<Channel> = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(url.trim())
            .header("User-Agent", "AuraTV/1.0 (Android)")
            .apply {
                if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
                    header("Authorization", Credentials.basic(username, password))
                }
            }
            .get()
            .build()

        client.newCall(req).execute().use { response ->
            if (!response.isSuccessful) {
                throw PlaylistException(
                    "HTTP ${response.code} ${response.message.ifBlank { "from playlist server" }}",
                )
            }
            val body = response.body?.string()
                ?: throw PlaylistException("Empty response from playlist server.")
            if (!body.trimStart().startsWith("#EXTM3U") && !body.contains("#EXTINF")) {
                throw PlaylistException(
                    "The URL did not return an M3U playlist. Double-check the URL and credentials.",
                )
            }
            val channels = M3uParser.parse(body)
            if (channels.isEmpty()) {
                throw PlaylistException("Playlist parsed but contained no channels.")
            }
            channels
        }
    }
}

class PlaylistException(message: String) : RuntimeException(message)
