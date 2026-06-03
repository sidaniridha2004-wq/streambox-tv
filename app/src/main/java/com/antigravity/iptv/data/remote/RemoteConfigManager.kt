package com.antigravity.iptv.data.remote

import com.antigravity.iptv.data.local.AppPreferences
import com.antigravity.iptv.data.repository.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class RemoteConfig(
    val appName: String,
    val tvVersion: Int,
    val tvUrl: String?,
    val vodVersion: Int,
    val vodUrl: String?,
    // Legacy support
    val legacyVersion: Int,
    val legacyM3uUrls: List<String>
)

@Singleton
class RemoteConfigManager @Inject constructor(
    private val appPreferences: AppPreferences,
    private val repository: PlaylistRepository
) {
    companion object {
        // Encoded config endpoint
        private val CONFIG_URL: String get() {
            val encoded = "aHR0cHM6Ly9wYXN0ZWJpbi5jb20vcmF3LzR5MjhBZDFV"
            return String(android.util.Base64.decode(encoded, android.util.Base64.DEFAULT))
        }
        private const val REMOTE_PLAYLIST_NAME = "Aura TV"
    }

    private val syncMutex = Mutex()

    /**
     * Fetches the remote JSON config and syncs TV and VOD independently.
     * @param onProgress callback for step-by-step progress updates
     */
    suspend fun checkAndSync(onProgress: (String) -> Unit = {}): Result<Boolean> = syncMutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                // 1. Fetch JSON from remote
                onProgress("Checking for updates...")
                val jsonText = URL(CONFIG_URL).readText()
                val config = parseConfig(jsonText)

                var didSync = false

                // 2. Resolve TV URL (new format or legacy)
                val tvUrl = config.tvUrl ?: config.legacyM3uUrls.firstOrNull()
                val tvVersion = if (config.tvUrl != null) config.tvVersion else config.legacyVersion

                // 3. Sync TV channels if version changed
                if (tvUrl != null && tvVersion != 0) {
                    val storedTvVersion = appPreferences.remoteConfigVersion.first()
                    if (tvVersion != storedTvVersion) {
                        didSync = true
                        onProgress("Downloading TV channels...")
                        val existingPlaylistId = appPreferences.remotePlaylistId.first()

                        if (existingPlaylistId != null) {
                            repository.updateM3uPlaylist(existingPlaylistId, REMOTE_PLAYLIST_NAME, tvUrl, "live") { step ->
                                onProgress(step)
                            }
                        } else {
                            val result = repository.addM3uPlaylist(REMOTE_PLAYLIST_NAME, tvUrl, "live") { step ->
                                onProgress(step)
                            }
                            if (result.isSuccess) {
                                val newId = result.getOrNull()
                                if (newId != null) {
                                    appPreferences.setRemotePlaylistId(newId)
                                    appPreferences.setActivePlaylistId(newId)
                                }
                            } else {
                                return@withContext Result.failure(result.exceptionOrNull() ?: Exception("Failed to add TV playlist"))
                            }
                        }
                        onProgress("TV channels synced!")
                        appPreferences.setRemoteConfigVersion(tvVersion)
                    }
                }

                // 4. Sync VOD channels if version changed
                val vodUrl = config.vodUrl
                if (vodUrl != null && config.vodVersion != 0) {
                    val storedVodVersion = appPreferences.vodConfigVersion.first()
                    if (config.vodVersion != storedVodVersion) {
                        didSync = true
                        onProgress("Downloading Movies & Series...")
                        // Ensure the playlist exists first
                        var playlistId = appPreferences.remotePlaylistId.first()
                        if (playlistId == null && tvUrl != null) {
                            val result = repository.addM3uPlaylist(REMOTE_PLAYLIST_NAME, tvUrl, "live") { step ->
                                onProgress(step)
                            }
                            if (result.isSuccess) {
                                playlistId = result.getOrNull()
                                if (playlistId != null) {
                                    appPreferences.setRemotePlaylistId(playlistId)
                                    appPreferences.setActivePlaylistId(playlistId)
                                }
                            }
                        }

                        if (playlistId != null) {
                            repository.addVodChannels(playlistId, vodUrl) { step ->
                                onProgress(step)
                            }
                            onProgress("Movies & Series synced!")
                            appPreferences.setVodConfigVersion(config.vodVersion)
                        }
                    }
                }

                // 5. Ensure active playlist is set
                if (!didSync) {
                    val existingPlaylistId = appPreferences.remotePlaylistId.first()
                    if (existingPlaylistId != null) {
                        val currentActive = appPreferences.activePlaylistId.first()
                        if (currentActive == null) {
                            appPreferences.setActivePlaylistId(existingPlaylistId)
                        }
                    }
                }

                if (didSync) {
                    onProgress("All done!")
                }
                Result.success(didSync)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun parseConfig(jsonText: String): RemoteConfig {
        val json = JSONObject(jsonText)
        val appName = json.optString("app_name", "Aura TV")

        // New format: separate tv_version/tv_url and vod_version/vod_url
        val tvVersion = json.optInt("tv_version", 0)
        val tvUrl = json.optString("tv_url", "").ifBlank { null }
        val vodVersion = json.optInt("vod_version", 0)
        val vodUrl = json.optString("vod_url", "").ifBlank { null }

        // Legacy format: single "version" and "m3u_urls" array
        val legacyVersion = json.optInt("version", 0)
        val urlsArray = json.optJSONArray("m3u_urls")
        val legacyUrls = mutableListOf<String>()
        if (urlsArray != null) {
            for (i in 0 until urlsArray.length()) {
                val url = urlsArray.optString(i)
                if (url.isNotBlank()) legacyUrls.add(url)
            }
        }

        return RemoteConfig(appName, tvVersion, tvUrl, vodVersion, vodUrl, legacyVersion, legacyUrls)
    }
}
