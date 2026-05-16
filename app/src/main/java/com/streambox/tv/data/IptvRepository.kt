package com.streambox.tv.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for the running app.
 *
 * Holds:
 *  • The list of providers (their type, endpoint, sync status)
 *  • Per-provider fetched channels (real M3U/Xtream results)
 *  • The active provider id
 *
 * Exposes a single [channels] flow that returns:
 *  • the active provider's real fetched channels if it has finished syncing
 *  • the bundled sample channels otherwise (so the UI is always populated)
 */
@Singleton
class IptvRepository @Inject constructor(
    private val playlistFetcher: PlaylistFetcher,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _providers = MutableStateFlow(SampleData.providers)
    val providers: StateFlow<List<Provider>> = _providers.asStateFlow()

    private val _activeProviderId = MutableStateFlow(SampleData.providers.firstOrNull()?.id)
    val activeProviderId: StateFlow<String?> = _activeProviderId.asStateFlow()

    /** Fetched channels per provider id (only populated for providers that successfully synced). */
    private val _fetchedChannels = MutableStateFlow<Map<String, List<Channel>>>(emptyMap())

    /** The single channel list the rest of the app should observe. */
    val channels: StateFlow<List<Channel>> = combine(
        _activeProviderId,
        _fetchedChannels,
    ) { activeId, fetchedMap ->
        val real = activeId?.let { fetchedMap[it] }
        real ?: SampleData.channels
    }.stateIn(scope, SharingStarted.Eagerly, SampleData.channels)

    /** Whether the active provider's channels are currently real (not sample data). */
    val isUsingRealChannels: StateFlow<Boolean> = combine(
        _activeProviderId,
        _fetchedChannels,
    ) { activeId, fetchedMap ->
        activeId != null && fetchedMap[activeId] != null
    }.stateIn(scope, SharingStarted.Eagerly, false)

    /** Sync status for a specific provider (collected by SyncingScreen). */
    fun syncStatusFor(providerId: String): StateFlow<ProviderStatus> {
        val flow = MutableStateFlow(ProviderStatus.SYNCING)
        // initial value comes from the providers list
        flow.value = _providers.value.firstOrNull { it.id == providerId }?.status ?: ProviderStatus.SYNCING
        return flow
    }

    val groups: List<String> = SampleData.groups
    val epg: List<EpgProgram> = SampleData.epg

    private val _movies = MutableStateFlow(SampleData.movies)
    val movies: StateFlow<List<Movie>> = _movies.asStateFlow()

    private val _series = MutableStateFlow(SampleData.series)
    val series: StateFlow<List<Series>> = _series.asStateFlow()

    val continueWatching: List<ContinueWatching> = SampleData.continueWatching

    fun setActiveProvider(id: String) { _activeProviderId.value = id }

    // ---------- Provider creation (real fetch) ----------

    /**
     * Adds an M3U provider and immediately starts a background fetch.
     * Returns the new provider's id so the UI can subscribe to its status.
     */
    fun addM3uProvider(
        name: String,
        url: String,
        epgUrl: String?,
        username: String?,
        password: String?,
    ): String {
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
        startSync(provider, url, username, password)
        return provider.id
    }

    fun addStalkerProvider(portal: String, mac: String, deviceId: String?, serial: String?): String {
        // Real Stalker portal handshake is a separate, non-trivial protocol
        // (token negotiation, MAC-based auth, AJAX endpoints). Not implemented
        // in this build — we simulate a short delay then mark it as SYNCING
        // so the UI flow remains valid.
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
        // For now, surface a clear "not yet supported" failure so the user knows
        // why no channels appeared. We'll keep the provider record.
        scope.launch {
            kotlinx.coroutines.delay(800)
            updateProviderStatus(provider.id, ProviderStatus.FAILED, "Stalker login is not yet supported")
        }
        return provider.id
    }

    fun addXtreamProvider(name: String, host: String, user: String, pass: String, output: String): String {
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
        // Xtream playlist URL embeds credentials, so we don't need basic auth
        startSync(provider, playlistUrl, null, null)
        return provider.id
    }

    private fun startSync(provider: Provider, url: String, user: String?, pass: String?) {
        scope.launch {
            try {
                val channels = playlistFetcher.fetch(url, user, pass)
                _fetchedChannels.update { it + (provider.id to channels) }
                _providers.update { list ->
                    list.map {
                        if (it.id == provider.id) it.copy(
                            status = ProviderStatus.OK,
                            lastSync = "just now",
                            channelCount = channels.size,
                        ) else it
                    }
                }
            } catch (t: Throwable) {
                val msg = (t.message ?: t::class.simpleName ?: "unknown error").take(120)
                _providers.update { list ->
                    list.map {
                        if (it.id == provider.id) it.copy(
                            status = ProviderStatus.FAILED,
                            lastSync = "failed · $msg",
                        ) else it
                    }
                }
            }
        }
    }

    private fun updateProviderStatus(id: String, status: ProviderStatus, message: String) {
        _providers.update { list ->
            list.map { if (it.id == id) it.copy(status = status, lastSync = message) else it }
        }
    }

    // ---------- Favorites ----------

    fun toggleChannelFavorite(id: String) {
        // Toggle on the active fetched list (or sample data otherwise).
        val activeId = _activeProviderId.value
        val real = activeId?.let { _fetchedChannels.value[it] }
        if (real != null && activeId != null) {
            _fetchedChannels.update { map ->
                map + (activeId to real.map { if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it })
            }
        }
    }

    fun toggleMovieFavorite(id: String) {
        _movies.update { list -> list.map { if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it } }
    }

    fun toggleSeriesFavorite(id: String) {
        _series.update { list -> list.map { if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it } }
    }

    fun deleteProvider(id: String) {
        _providers.update { it.filterNot { p -> p.id == id } }
        _fetchedChannels.update { it - id }
        if (_activeProviderId.value == id) _activeProviderId.value = _providers.value.firstOrNull()?.id
    }
}
