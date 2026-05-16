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
 * - On creation, loads persisted providers from [ProviderStore] and re-fetches
 *   the active one's playlist so AuraTV remembers your IPTV across restarts.
 * - Every provider mutation is persisted asynchronously.
 * - The [channels] flow yields the active provider's real fetched channels
 *   when available, otherwise sample data so the UI is always populated.
 */
@Singleton
class IptvRepository @Inject constructor(
    private val playlistFetcher: PlaylistFetcher,
    private val store: ProviderStore,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _providers = MutableStateFlow(SampleData.providers)
    val providers: StateFlow<List<Provider>> = _providers.asStateFlow()

    private val _activeProviderId = MutableStateFlow(SampleData.providers.firstOrNull()?.id)
    val activeProviderId: StateFlow<String?> = _activeProviderId.asStateFlow()

    /** Per-provider credentials kept in memory for re-fetching. Populated either
     *  from the store on launch, or from the form when a provider is added. */
    private val credentials = mutableMapOf<String, StoredProvider>()

    /** Fetched channels per provider id (only populated for providers that successfully synced). */
    private val _fetchedChannels = MutableStateFlow<Map<String, List<Channel>>>(emptyMap())

    val channels: StateFlow<List<Channel>> = combine(
        _activeProviderId,
        _fetchedChannels,
    ) { activeId, fetchedMap ->
        val real = activeId?.let { fetchedMap[it] }
        real ?: SampleData.channels
    }.stateIn(scope, SharingStarted.Eagerly, SampleData.channels)

    val isUsingRealChannels: StateFlow<Boolean> = combine(
        _activeProviderId,
        _fetchedChannels,
    ) { activeId, fetchedMap ->
        activeId != null && fetchedMap[activeId] != null
    }.stateIn(scope, SharingStarted.Eagerly, false)

    val groups: List<String> = SampleData.groups
    val epg: List<EpgProgram> = SampleData.epg

    private val _movies = MutableStateFlow(SampleData.movies)
    val movies: StateFlow<List<Movie>> = _movies.asStateFlow()

    private val _series = MutableStateFlow(SampleData.series)
    val series: StateFlow<List<Series>> = _series.asStateFlow()

    val continueWatching: List<ContinueWatching> = SampleData.continueWatching

    init {
        // Restore from disk
        scope.launch {
            val (storedProviders, storedActiveId) = store.load()
            if (storedProviders.isNotEmpty()) {
                val live = storedProviders.map { it.toProvider(status = ProviderStatus.SYNCING) }
                _providers.value = live
                storedProviders.forEach { credentials[it.id] = it }
                _activeProviderId.value = storedActiveId ?: live.first().id
                // Re-fetch each persisted provider in the background. Active one
                // first so the UI populates fastest.
                val ordered = live.sortedByDescending { it.id == _activeProviderId.value }
                ordered.forEach { p ->
                    val cred = credentials[p.id] ?: return@forEach
                    when (cred.type) {
                        ProviderType.M3U.name -> startSync(p, cred.endpoint, cred.username, cred.password)
                        ProviderType.XTREAM.name -> startSync(p, cred.endpoint, null, null)
                        ProviderType.STALKER.name -> {
                            updateProviderStatus(p.id, ProviderStatus.FAILED, "Stalker login is not yet supported")
                        }
                    }
                }
            }
        }
    }

    fun setActiveProvider(id: String) {
        _activeProviderId.value = id
        persist()
    }

    // ---------- Provider creation ----------

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
        credentials[provider.id] = StoredProvider(
            id = provider.id,
            name = provider.name,
            type = ProviderType.M3U.name,
            endpoint = url,
            username = username,
            password = password,
            epgUrl = epgUrl,
        )
        _activeProviderId.value = provider.id
        persist()
        startSync(provider, url, username, password)
        return provider.id
    }

    fun addStalkerProvider(portal: String, mac: String, deviceId: String?, serial: String?): String {
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
        credentials[provider.id] = StoredProvider(
            id = provider.id,
            name = provider.name,
            type = ProviderType.STALKER.name,
            endpoint = portal,
            username = mac,
        )
        _activeProviderId.value = provider.id
        persist()
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
        credentials[provider.id] = StoredProvider(
            id = provider.id,
            name = provider.name,
            type = ProviderType.XTREAM.name,
            endpoint = playlistUrl,
            username = user,
            password = pass,
            xtreamHost = host,
            xtreamOutput = output,
            epgUrl = XtreamClient.epgUrl(host, user, pass),
        )
        _activeProviderId.value = provider.id
        persist()
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

    /** Re-trigger sync for a provider (Pull-to-refresh / manual retry). */
    fun refreshProvider(id: String) {
        val cred = credentials[id] ?: return
        val p = _providers.value.firstOrNull { it.id == id } ?: return
        _providers.update { list ->
            list.map { if (it.id == id) it.copy(status = ProviderStatus.SYNCING, lastSync = "syncing…") else it }
        }
        when (cred.type) {
            ProviderType.STALKER.name ->
                updateProviderStatus(id, ProviderStatus.FAILED, "Stalker login is not yet supported")
            else -> startSync(p, cred.endpoint, cred.username, cred.password)
        }
    }

    private fun updateProviderStatus(id: String, status: ProviderStatus, message: String) {
        _providers.update { list ->
            list.map { if (it.id == id) it.copy(status = status, lastSync = message) else it }
        }
    }

    // ---------- Favorites ----------

    fun toggleChannelFavorite(id: String) {
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
        credentials.remove(id)
        if (_activeProviderId.value == id) _activeProviderId.value = _providers.value.firstOrNull()?.id
        persist()
    }

    // ---------- Internal ----------

    private fun persist() {
        // Persist only "real" providers (skip the bundled SampleData entries).
        val toStore = _providers.value
            .mapNotNull { credentials[it.id] }
        scope.launch { store.save(toStore, _activeProviderId.value) }
    }
}

private fun StoredProvider.toProvider(status: ProviderStatus) = Provider(
    id = id,
    name = name,
    type = ProviderType.valueOf(type),
    endpoint = endpoint,
    status = status,
    lastSync = "syncing…",
    channelCount = 0, movieCount = 0, seriesCount = 0,
)
