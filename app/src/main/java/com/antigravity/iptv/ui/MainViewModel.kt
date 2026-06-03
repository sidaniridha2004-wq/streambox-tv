package com.antigravity.iptv.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.iptv.data.local.entity.ChannelEntity
import com.antigravity.iptv.data.local.entity.ChannelWithProgress
import com.antigravity.iptv.data.local.entity.PlaylistEntity
import com.antigravity.iptv.data.remote.RemoteConfigManager
import com.antigravity.iptv.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.antigravity.iptv.domain.model.PlaylistType

import com.antigravity.iptv.data.local.AppPreferences

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: PlaylistRepository,
    private val appPreferences: AppPreferences,
    private val remoteConfigManager: RemoteConfigManager
) : ViewModel() {

    // --- State ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMsg = MutableStateFlow<String?>(null)
    val errorMsg: StateFlow<String?> = _errorMsg.asStateFlow()
    
    // --- Activation ---
    val isActivated: StateFlow<Boolean> = appPreferences.isActivated
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun activate() {
        viewModelScope.launch {
            appPreferences.setActivated(true)
        }
    }

    // --- Remote Config ---
    private val _remoteConfigStatus = MutableStateFlow<String?>(null)
    val remoteConfigStatus: StateFlow<String?> = _remoteConfigStatus.asStateFlow()

    private val _isRemoteSyncing = MutableStateFlow(false)
    val isRemoteSyncing: StateFlow<Boolean> = _isRemoteSyncing.asStateFlow()

    private var hasCheckedRemote = false

    fun checkRemoteConfig() {
        if (hasCheckedRemote) return
        hasCheckedRemote = true
        viewModelScope.launch {
            _isRemoteSyncing.value = true
            _remoteConfigStatus.value = "Checking for updates..."
            val result = remoteConfigManager.checkAndSync { progressMsg ->
                _remoteConfigStatus.value = progressMsg
            }
            if (result.isSuccess) {
                val didSync = result.getOrDefault(false)
                if (didSync) {
                    _remoteConfigStatus.value = "Finalizing..."
                    kotlinx.coroutines.delay(1500)
                    _remoteConfigStatus.value = "Ready!"
                    kotlinx.coroutines.delay(800)
                }
                _isRemoteSyncing.value = false
                _remoteConfigStatus.value = null
            } else {
                _remoteConfigStatus.value = "Offline mode"
                kotlinx.coroutines.delay(2000)
                _isRemoteSyncing.value = false
                _remoteConfigStatus.value = null
            }
        }
    }

    // --- Theme ---
    val isDarkMode: StateFlow<Boolean> = appPreferences.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
        
    fun toggleTheme() {
        viewModelScope.launch {
            appPreferences.setDarkMode(!isDarkMode.value)
        }
    }

    // --- Active Playlist ---
    val activePlaylistId: StateFlow<Int?> = repository.activePlaylistId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setActivePlaylist(id: Int) {
        viewModelScope.launch { repository.setActivePlaylist(id) }
    }

    // --- Playlists ---
    val playlists = repository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val remotePlaylistId: StateFlow<Int?> = appPreferences.remotePlaylistId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activePlaylist: StateFlow<PlaylistEntity?> = combine(
        playlists,
        activePlaylistId
    ) { allPlaylists, activeId ->
        if (activeId == null) null
        else allPlaylists.find { it.id == activeId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Active Content Streams ---
    
    val activeChannels: Flow<List<ChannelEntity>> = flow {
        activePlaylistId.collect { id ->
            if (id != null) {
                emitAll(repository.getLiveTv(id, 1000))
            } else {
                emit(emptyList())
            }
        }
    }

    val activeRecentChannels: Flow<List<ChannelWithProgress>> = flow {
        activePlaylistId.collect { id ->
            if (id != null) {
                emitAll(repository.getRecentChannels(id))
            } else {
                emit(emptyList())
            }
        }
    }

    val activeCategories: Flow<List<String>> = flow {
        activePlaylistId.collect { id ->
            if (id != null) {
                emitAll(repository.getCategoriesForPlaylist(id))
            } else {
                emit(emptyList())
            }
        }
    }

    // --- General Fetchers ---
    fun getChannelsForPlaylist(playlistId: Int): Flow<List<ChannelEntity>> = repository.getChannelsByPlaylist(playlistId)
    fun getMovies(playlistId: Int): Flow<List<ChannelEntity>> = repository.getMovies(playlistId, 50000)
    fun getSeries(playlistId: Int): Flow<List<ChannelEntity>> = repository.getSeries(playlistId, 50000)
    fun getSportsChannels(playlistId: Int): Flow<List<ChannelEntity>> = repository.getSportsChannels(playlistId)
    fun getChannelsByBaseName(playlistId: Int, baseName: String): Flow<List<ChannelEntity>> = repository.getChannelsByBaseName(playlistId, baseName)
    fun getLiveTv(playlistId: Int): Flow<List<ChannelEntity>> = repository.getLiveTv(playlistId, limit = 50000)

    fun getCategoriesForPlaylist(playlistId: Int): Flow<List<String>> = repository.getCategoriesForPlaylist(playlistId)
    fun getCategoriesBySourceType(playlistId: Int, sourceType: String): Flow<List<String>> = repository.getCategoriesBySourceType(playlistId, sourceType)
    fun getChannelsByCategory(playlistId: Int, category: String): Flow<List<ChannelEntity>> = repository.getChannelsByCategory(playlistId, category)
    fun getChannelsByCategoryAndType(playlistId: Int, category: String, sourceType: String): Flow<List<ChannelEntity>> = repository.getChannelsByCategoryAndType(playlistId, category, sourceType)

    suspend fun getAlternativeQualities(playlistId: Int, channelName: String): List<ChannelEntity> {
        // Strip common tags to find the base name
        var baseName = channelName.replace(Regex("(?i)\\b(HD|FHD|SD|UHD|4K|8K|HEVC|H265|H264|1080p|720p)\\b"), "")
        baseName = baseName.replace(Regex("[^a-zA-Z0-9 ]"), " ").trim()
        baseName = baseName.replace(Regex("\\s+"), " ") // normalize spaces
        if (baseName.length < 3) return emptyList()

        // Fetch all matching channels from DB
        val matches = repository.getChannelsByBaseName(playlistId, baseName).firstOrNull() ?: emptyList()
        
        // Filter out channels that don't share the same base name to avoid false positives (e.g. "Bein Sports 1" vs "Bein Sports 10")
        return matches.filter {
            val otherBase = it.name.replace(Regex("(?i)\\b(HD|FHD|SD|UHD|4K|8K|HEVC|H265|H264|1080p|720p)\\b"), "")
                .replace(Regex("[^a-zA-Z0-9 ]"), " ").trim()
                .replace(Regex("\\s+"), " ")
            otherBase.equals(baseName, ignoreCase = true)
        }.distinctBy { it.name }
    }

    fun searchChannels(playlistId: Int, query: String): Flow<List<ChannelEntity>> = repository.searchChannels(playlistId, query)
    fun getChannelCount(playlistId: Int): Flow<Int> = repository.getChannelCount(playlistId)

    // --- Actions ---
    fun getStreamUrl(channel: ChannelEntity, playlistId: Int, onResolved: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val playlist = repository.getPlaylist(playlistId) ?: throw Exception("Playlist not found")
                
                // Record history
                repository.recordWatchHistory(playlistId, channel.id)
                
                val streamUrl = if (playlist.type == PlaylistType.STALKER) {
                    repository.resolveStalkerLink(playlist, channel.streamUrl)
                } else {
                    channel.streamUrl
                }
                
                _isLoading.value = false
                onResolved(streamUrl)
            } catch (e: Exception) {
                _isLoading.value = false
                onError(e.message ?: "Failed to resolve link")
            }
        }
    }

    fun toggleFavorite(channel: ChannelEntity) {
        viewModelScope.launch { repository.toggleFavorite(channel) }
    }

    suspend fun getWatchProgress(channel: ChannelEntity): Long {
        val playlistId = activePlaylistId.value ?: return 0L
        return repository.getWatchProgress(playlistId, channel.id)
    }

    fun updateWatchProgress(channel: ChannelEntity, progressMs: Long, durationMs: Long) {
        val playlistId = activePlaylistId.value ?: return
        viewModelScope.launch {
            repository.updateWatchProgress(playlistId, channel.id, progressMs, durationMs)
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch { repository.deletePlaylist(playlist) }
    }

    private val _progressLogs = MutableStateFlow<List<String>>(emptyList())
    val progressLogs: StateFlow<List<String>> = _progressLogs.asStateFlow()

    fun updateM3uPlaylist(id: Int, name: String, url: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMsg.value = null
            _progressLogs.value = emptyList()
            
            val result = repository.updateM3uPlaylist(id, name, url) { log ->
                _progressLogs.value = _progressLogs.value + log
            }
            
            _isLoading.value = false
            if (result.isSuccess) {
                onSuccess()
            } else {
                _errorMsg.value = result.exceptionOrNull()?.message ?: "Unknown error"
            }
        }
    }

    fun updateStalkerPlaylist(id: Int, name: String, portalUrl: String, macAddress: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMsg.value = null
            
            val result = repository.updateStalkerPlaylist(id, name, portalUrl, macAddress)
            
            _isLoading.value = false
            if (result.isSuccess) {
                onSuccess()
            } else {
                _errorMsg.value = result.exceptionOrNull()?.message ?: "Unknown error"
            }
        }
    }

    fun addM3uPlaylist(name: String, url: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMsg.value = null
            _progressLogs.value = emptyList()
            
            val result = repository.addM3uPlaylist(name, url) { log ->
                _progressLogs.value = _progressLogs.value + log
            }
            
            _isLoading.value = false
            if (result.isSuccess) {
                val id = result.getOrNull()
                if (id != null) setActivePlaylist(id)
                onSuccess()
            } else {
                _errorMsg.value = result.exceptionOrNull()?.message ?: "Unknown error"
            }
        }
    }

    fun addStalkerPlaylist(name: String, portalUrl: String, macAddress: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMsg.value = null
            
            val result = repository.addStalkerPlaylist(name, portalUrl, macAddress)
            
            _isLoading.value = false
            if (result.isSuccess) {
                val id = result.getOrNull()
                if (id != null) setActivePlaylist(id)
                onSuccess()
            } else {
                _errorMsg.value = result.exceptionOrNull()?.message ?: "Unknown error"
            }
        }
    }

    fun clearError() { _errorMsg.value = null }

    // --- Sync Logic ---
    fun checkAndSyncPlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            val lastSyncTime = appPreferences.getLastSyncTime(playlist.id).first()
            val currentTime = System.currentTimeMillis()
            val twentyFourHoursMs = 24 * 60 * 60 * 1000L
            
            if (currentTime - lastSyncTime > twentyFourHoursMs) {
                syncPlaylist(playlist)
            }
        }
    }

    fun syncPlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                if (playlist.type == PlaylistType.M3U && !playlist.url.isNullOrBlank()) {
                    repository.updateM3uPlaylist(playlist.id, playlist.name, playlist.url!!)
                } else if (playlist.type == PlaylistType.STALKER && !playlist.url.isNullOrBlank() && !playlist.macAddress.isNullOrBlank()) {
                    repository.updateStalkerPlaylist(playlist.id, playlist.name, playlist.url!!, playlist.macAddress!!)
                }
                
                appPreferences.setLastSyncTime(playlist.id, System.currentTimeMillis())
            } catch (e: Exception) {
                _errorMsg.value = "Erreur de synchronisation: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
