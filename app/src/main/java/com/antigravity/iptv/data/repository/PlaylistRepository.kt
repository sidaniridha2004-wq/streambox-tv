package com.antigravity.iptv.data.repository

import com.antigravity.iptv.data.local.AppPreferences
import com.antigravity.iptv.data.local.dao.ChannelDao
import com.antigravity.iptv.data.local.dao.PlaylistDao
import com.antigravity.iptv.data.local.dao.WatchHistoryDao
import com.antigravity.iptv.data.local.entity.PlaylistEntity
import com.antigravity.iptv.data.local.entity.ChannelEntity
import com.antigravity.iptv.data.local.entity.WatchHistoryEntity
import com.antigravity.iptv.data.local.entity.ChannelWithProgress
import com.antigravity.iptv.data.parser.M3uParser
import com.antigravity.iptv.domain.model.PlaylistType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton
import com.antigravity.iptv.data.remote.StalkerApiService

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val channelDao: ChannelDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val appPreferences: AppPreferences,
    private val okHttpClient: OkHttpClient,
    private val stalkerApiService: StalkerApiService
) {
    // --- Preferences ---
    val activePlaylistId: Flow<Int?> = appPreferences.activePlaylistId

    suspend fun setActivePlaylist(id: Int) = appPreferences.setActivePlaylistId(id)
    suspend fun clearActivePlaylist() = appPreferences.clearActivePlaylistId()

    // --- History ---
    fun getRecentChannels(playlistId: Int): Flow<List<ChannelWithProgress>> = watchHistoryDao.getRecentChannels(playlistId)

    suspend fun getWatchProgress(playlistId: Int, channelId: Int): Long = withContext(Dispatchers.IO) {
        val existing = watchHistoryDao.getHistoryForChannel(playlistId, channelId)
        return@withContext existing?.progressMs ?: 0L
    }

    suspend fun recordWatchHistory(playlistId: Int, channelId: Int) = withContext(Dispatchers.IO) {
        val existing = watchHistoryDao.getHistoryForChannel(playlistId, channelId)
        val now = System.currentTimeMillis()
        if (existing != null) {
            watchHistoryDao.updateHistoryTimestamp(existing.id, now)
        } else {
            watchHistoryDao.insertHistory(WatchHistoryEntity(playlistId = playlistId, channelId = channelId, timestamp = now))
        }
    }

    suspend fun updateWatchProgress(playlistId: Int, channelId: Int, progressMs: Long, durationMs: Long) = withContext(Dispatchers.IO) {
        val existing = watchHistoryDao.getHistoryForChannel(playlistId, channelId)
        val now = System.currentTimeMillis()
        if (existing != null) {
            watchHistoryDao.updateHistoryProgress(existing.id, progressMs, durationMs, now)
        } else {
            watchHistoryDao.insertHistory(WatchHistoryEntity(playlistId = playlistId, channelId = channelId, timestamp = now, progressMs = progressMs, durationMs = durationMs))
        }
    }

    // --- Playlists & Channels ---
    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    suspend fun getPlaylist(id: Int): PlaylistEntity? = playlistDao.getPlaylistById(id)

    fun getChannelsByPlaylist(playlistId: Int): Flow<List<ChannelEntity>> = channelDao.getChannelsByPlaylist(playlistId)
    fun getMovies(playlistId: Int, limit: Int = 100): Flow<List<ChannelEntity>> = channelDao.getMovies(playlistId, limit)
    fun getSeries(playlistId: Int, limit: Int = 100): Flow<List<ChannelEntity>> = channelDao.getSeries(playlistId, limit)
    fun getLiveTv(playlistId: Int, limit: Int = 100): Flow<List<ChannelEntity>> = channelDao.getLiveTv(playlistId, limit)
    fun getSportsChannels(playlistId: Int): Flow<List<ChannelEntity>> = channelDao.getSportsChannels(playlistId)
    fun getChannelsByBaseName(playlistId: Int, baseName: String): Flow<List<ChannelEntity>> = channelDao.getChannelsByBaseName(playlistId, baseName)

    fun getCategoriesForPlaylist(playlistId: Int): Flow<List<String>> = channelDao.getCategoriesByPlaylist(playlistId)

    fun getCategoriesBySourceType(playlistId: Int, sourceType: String): Flow<List<String>> = channelDao.getCategoriesBySourceType(playlistId, sourceType)

    fun getChannelsByCategory(playlistId: Int, groupName: String): Flow<List<ChannelEntity>> = channelDao.getChannelsByCategory(playlistId, groupName)

    fun getChannelsByCategoryAndType(playlistId: Int, groupName: String, sourceType: String): Flow<List<ChannelEntity>> = channelDao.getChannelsByCategoryAndType(playlistId, groupName, sourceType)

    fun searchChannels(playlistId: Int, query: String): Flow<List<ChannelEntity>> = channelDao.searchChannels(playlistId, query)

    fun getChannelCount(playlistId: Int): Flow<Int> = channelDao.getChannelCountByPlaylist(playlistId)

    // --- Add/Remove Playlists ---
    suspend fun addM3uPlaylist(name: String, url: String, sourceType: String = "live", onProgress: (String) -> Unit = {}): Result<Int> = withContext(Dispatchers.IO) {
        try {
            onProgress("Starting playlist download...")
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to download playlist: ${response.code}"))
            }
            
            val inputStream = response.body?.byteStream()
                ?: return@withContext Result.failure(Exception("Empty response body"))

            onProgress("Parsing channels...")
            kotlinx.coroutines.delay(500)
            val playlist = PlaylistEntity(name = name, type = PlaylistType.M3U, url = url)
            val playlistId = playlistDao.insertPlaylist(playlist).toInt()

            val channels = M3uParser.parse(inputStream, playlistId, sourceType)
            onProgress("Found ${channels.size} channels")
            kotlinx.coroutines.delay(500)
            
            val moviesCount = channels.count { it.sourceType == "movie" }
            val seriesCount = channels.count { it.sourceType == "series" }
            val liveCount = channels.count { it.sourceType == "live" }
            if (liveCount > 0) onProgress("Found $liveCount live channels")
            if (moviesCount > 0) onProgress("Found $moviesCount movies")
            if (seriesCount > 0) onProgress("Found $seriesCount TV shows")
            kotlinx.coroutines.delay(500)
            
            onProgress("Saving channels...")
            channelDao.insertChannels(channels)
            
            appPreferences.setLastSyncTime(playlistId, System.currentTimeMillis())

            onProgress("Playlist was added successfully!")
            Result.success(playlistId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Download a VOD M3U and insert channels into an existing playlist.
     * Channels are auto-categorized as movie or series based on group names.
     * Only replaces VOD channels (sourceType != 'live'), keeping live channels intact.
     */
    suspend fun addVodChannels(playlistId: Int, vodUrl: String, onProgress: (String) -> Unit = {}): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            onProgress("Downloading Movies & Series...")
            val request = Request.Builder().url(vodUrl).build()
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to download VOD: ${response.code}"))
            }
            
            val inputStream = response.body?.byteStream()
                ?: return@withContext Result.failure(Exception("Empty response body"))

            onProgress("Parsing VOD content...")
            // Parse with "vod" sourceType — parser auto-detects movies vs series vs live
            val allChannels = M3uParser.parse(inputStream, playlistId, "vod")
            
            // Filter OUT channels detected as "live" — they're already in the TV M3U
            val vodChannels = allChannels.filter { it.sourceType == "movie" || it.sourceType == "series" }
            
            val moviesCount = vodChannels.count { it.sourceType == "movie" }
            val seriesCount = vodChannels.count { it.sourceType == "series" }
            val skippedCount = allChannels.size - vodChannels.size
            onProgress("Found $moviesCount movies, $seriesCount TV shows")
            if (skippedCount > 0) onProgress("Skipped $skippedCount live channels")
            kotlinx.coroutines.delay(500)

            // Wipe old VOD channels only (keep live channels)
            channelDao.deleteChannelsBySourceType(playlistId, "movie")
            channelDao.deleteChannelsBySourceType(playlistId, "series")
            
            onProgress("Saving $moviesCount movies, $seriesCount series...")
            channelDao.insertChannels(vodChannels)
            
            onProgress("Movies & Series updated!")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addStalkerPlaylist(name: String, portalUrl: String, macAddress: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val loadPhpUrl = if (portalUrl.endsWith("/")) "${portalUrl}server/load.php" else "$portalUrl/server/load.php"
            val macCookie = "mac=$macAddress"
            
            val handshakeResponse = stalkerApiService.handshake(loadPhpUrl, macCookie = macCookie)
            if (!handshakeResponse.isSuccessful) throw Exception("Handshake failed")
            
            val token = handshakeResponse.body()?.js?.token ?: throw Exception("No token received")
            val authHeader = "Bearer $token"

            val channelsResponse = stalkerApiService.getAllChannels(loadPhpUrl, macCookie = macCookie, token = authHeader)
            if (!channelsResponse.isSuccessful) throw Exception("Failed to fetch channels")
            
            val stalkerChannels = channelsResponse.body()?.js?.data ?: emptyList()

            val playlist = PlaylistEntity(
                name = name, 
                type = PlaylistType.STALKER, 
                url = portalUrl, 
                macAddress = macAddress
            )
            val playlistId = playlistDao.insertPlaylist(playlist).toInt()
            
            val entities = stalkerChannels.map {
                val channelName = it.name ?: "Unknown"
                val orderIndex = if (channelName.lowercase().contains("bein")) 0 else 1
                ChannelEntity(
                    playlistId = playlistId,
                    name = channelName,
                    streamUrl = it.cmd ?: "", 
                    logoUrl = it.logo,
                    groupName = it.tv_genre_id ?: "Uncategorized",
                    orderIndex = orderIndex
                )
            }
            channelDao.insertChannels(entities)
            appPreferences.setLastSyncTime(playlistId, System.currentTimeMillis())
            
            Result.success(playlistId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resolveStalkerLink(playlist: PlaylistEntity, cmd: String): String = withContext(Dispatchers.IO) {
        val portalUrl = playlist.url ?: throw Exception("Portal URL missing")
        val loadPhpUrl = if (portalUrl.endsWith("/")) "${portalUrl}server/load.php" else "$portalUrl/server/load.php"
        val macCookie = "mac=${playlist.macAddress}"
        
        val handshakeResponse = stalkerApiService.handshake(loadPhpUrl, macCookie = macCookie)
        val token = handshakeResponse.body()?.js?.token ?: throw Exception("Failed to get token")
        val authHeader = "Bearer $token"
        
        val linkResponse = stalkerApiService.createLink(loadPhpUrl, cmd = cmd, macCookie = macCookie, token = authHeader)
        val streamUrl = linkResponse.body()?.js?.cmd ?: throw Exception("Failed to resolve stream link")
        
        if (streamUrl.startsWith("ffmpeg ")) {
            return@withContext streamUrl.substringAfter("ffmpeg ").trim()
        }
        return@withContext streamUrl
    }

    suspend fun toggleFavorite(channel: ChannelEntity) = withContext(Dispatchers.IO) {
        channelDao.updateFavoriteStatus(channel.id, !channel.isFavorite)
    }

    suspend fun deletePlaylist(playlist: PlaylistEntity) = withContext(Dispatchers.IO) {
        watchHistoryDao.deleteHistoryForPlaylist(playlist.id)
        channelDao.deleteChannelsByPlaylist(playlist.id)
        playlistDao.deletePlaylist(playlist)
        
        // If this was the active playlist, clear it
        appPreferences.clearActivePlaylistId()
    }

    suspend fun updateM3uPlaylist(id: Int, name: String, url: String, sourceType: String = "live", onProgress: (String) -> Unit = {}): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            onProgress("Starting playlist download...")
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) return@withContext Result.failure(Exception("Failed to download playlist: ${response.code}"))
            val inputStream = response.body?.byteStream() ?: return@withContext Result.failure(Exception("Empty response body"))

            // Only wipe channels of the same sourceType
            if (sourceType == "live") {
                channelDao.deleteChannelsBySourceType(id, "live")
            } else {
                channelDao.deleteChannelsBySourceType(id, "movie")
                channelDao.deleteChannelsBySourceType(id, "series")
            }
            
            // Update Playlist DB
            playlistDao.updatePlaylist(id, name, url, null)

            // Re-parse and insert
            onProgress("Parsing channels...")
            val channels = M3uParser.parse(inputStream, id, sourceType)
            onProgress("Found ${channels.size} channels")
            kotlinx.coroutines.delay(500)
            
            onProgress("Saving channels...")
            channelDao.insertChannels(channels)
            
            appPreferences.setLastSyncTime(id, System.currentTimeMillis())

            onProgress("Playlist was updated successfully!")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateStalkerPlaylist(id: Int, name: String, portalUrl: String, macAddress: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val loadPhpUrl = if (portalUrl.endsWith("/")) "${portalUrl}server/load.php" else "$portalUrl/server/load.php"
            val macCookie = "mac=$macAddress"
            
            val handshakeResponse = stalkerApiService.handshake(loadPhpUrl, macCookie = macCookie)
            if (!handshakeResponse.isSuccessful) throw Exception("Handshake failed")
            val token = handshakeResponse.body()?.js?.token ?: throw Exception("No token received")
            val authHeader = "Bearer $token"

            val channelsResponse = stalkerApiService.getAllChannels(loadPhpUrl, macCookie = macCookie, token = authHeader)
            if (!channelsResponse.isSuccessful) throw Exception("Failed to fetch channels")
            val stalkerChannels = channelsResponse.body()?.js?.data ?: emptyList()

            // Wipe existing channels and history
            watchHistoryDao.deleteHistoryForPlaylist(id)
            channelDao.deleteChannelsByPlaylist(id)

            // Update Playlist DB
            playlistDao.updatePlaylist(id, name, portalUrl, macAddress)

            val entities = stalkerChannels.map {
                val channelName = it.name ?: "Unknown"
                val orderIndex = if (channelName.lowercase().contains("bein")) 0 else 1
                ChannelEntity(
                    playlistId = id,
                    name = channelName,
                    streamUrl = it.cmd ?: "", 
                    logoUrl = it.logo,
                    groupName = it.tv_genre_id ?: "Uncategorized",
                    orderIndex = orderIndex
                )
            }
            channelDao.insertChannels(entities)
            appPreferences.setLastSyncTime(id, System.currentTimeMillis())

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
