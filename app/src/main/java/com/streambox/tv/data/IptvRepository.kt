package com.streambox.tv.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for the running app. In a real build this would
 * delegate to Room + remote sources (M3U parser + Stalker portal client).
 * We expose flows so the UI is reactive when favorites/providers change.
 */
@Singleton
class IptvRepository @Inject constructor() {

    private val _providers = MutableStateFlow(SampleData.providers)
    val providers: StateFlow<List<Provider>> = _providers.asStateFlow()

    private val _activeProviderId = MutableStateFlow(SampleData.providers.firstOrNull()?.id)
    val activeProviderId: StateFlow<String?> = _activeProviderId.asStateFlow()

    private val _channels = MutableStateFlow(SampleData.channels)
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    val groups: List<String> = SampleData.groups

    val epg: List<EpgProgram> = SampleData.epg

    private val _movies = MutableStateFlow(SampleData.movies)
    val movies: StateFlow<List<Movie>> = _movies.asStateFlow()

    private val _series = MutableStateFlow(SampleData.series)
    val series: StateFlow<List<Series>> = _series.asStateFlow()

    val continueWatching: List<ContinueWatching> = SampleData.continueWatching

    fun setActiveProvider(id: String) { _activeProviderId.value = id }

    fun addM3uProvider(name: String, url: String, epgUrl: String?, username: String?, password: String?) {
        val provider = Provider(
            id = "m3u-${System.currentTimeMillis()}",
            name = name.ifBlank { "M3U Playlist" },
            type = ProviderType.M3U,
            endpoint = url,
            status = ProviderStatus.SYNCING,
            lastSync = "syncing…",
            channelCount = 0, movieCount = 0, seriesCount = 0,
        )
        _providers.update { it + provider }
        _activeProviderId.value = provider.id
    }

    fun addStalkerProvider(portal: String, mac: String, deviceId: String?, serial: String?) {
        val provider = Provider(
            id = "stk-${System.currentTimeMillis()}",
            name = "Stalker Portal",
            type = ProviderType.STALKER,
            endpoint = portal,
            status = ProviderStatus.SYNCING,
            lastSync = "syncing…",
            channelCount = 0, movieCount = 0, seriesCount = 0,
        )
        _providers.update { it + provider }
        _activeProviderId.value = provider.id
    }

    fun addXtreamProvider(name: String, host: String, user: String, pass: String, output: String) {
        val playlistUrl = XtreamClient.playlistUrl(host, user, pass, output)
        val provider = Provider(
            id = "xc-${System.currentTimeMillis()}",
            name = name.ifBlank { "Xtream Provider" },
            type = ProviderType.XTREAM,
            endpoint = playlistUrl,
            status = ProviderStatus.SYNCING,
            lastSync = "syncing…",
            channelCount = 0, movieCount = 0, seriesCount = 0,
        )
        _providers.update { it + provider }
        _activeProviderId.value = provider.id
    }

    fun toggleChannelFavorite(id: String) {
        _channels.update { list -> list.map { if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it } }
    }

    fun toggleMovieFavorite(id: String) {
        _movies.update { list -> list.map { if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it } }
    }

    fun toggleSeriesFavorite(id: String) {
        _series.update { list -> list.map { if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it } }
    }

    fun deleteProvider(id: String) {
        _providers.update { it.filterNot { p -> p.id == id } }
        if (_activeProviderId.value == id) _activeProviderId.value = _providers.value.firstOrNull()?.id
    }
}
