package com.streambox.tv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streambox.tv.data.IptvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val activeProvider: com.streambox.tv.data.Provider? = null,
    val providers: List<com.streambox.tv.data.Provider> = emptyList(),
    val featuredChannel: com.streambox.tv.data.Channel? = null,
    val featuredEpg: com.streambox.tv.data.EpgProgram? = null,
    val recentlyAdded: List<com.streambox.tv.data.Channel> = emptyList(),
    val favoriteChannels: List<com.streambox.tv.data.Channel> = emptyList(),
    val movies: List<com.streambox.tv.data.Movie> = emptyList(),
    val series: List<com.streambox.tv.data.Series> = emptyList(),
    val continueWatching: List<com.streambox.tv.data.ContinueWatching> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: IptvRepository,
) : ViewModel() {
    val state = combine(
        repo.providers,
        repo.activeProviderId,
        repo.channels,
        repo.movies,
        repo.series,
    ) { providers, activeId, channels, movies, series ->
        val active = providers.firstOrNull { it.id == activeId }
        val featured = channels.firstOrNull()
        val featuredEpg = featured?.let { c -> repo.epg.firstOrNull { it.channelId == c.id && it.startMinute <= 0 && it.startMinute + it.durationMinutes > 0 } }
        HomeUiState(
            activeProvider = active,
            providers = providers,
            featuredChannel = featured,
            featuredEpg = featuredEpg,
            recentlyAdded = channels.take(8),
            favoriteChannels = channels.filter { it.isFavorite }.ifEmpty { channels.take(6) },
            movies = movies,
            series = series,
            continueWatching = repo.continueWatching,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())

    fun setActiveProvider(id: String) = repo.setActiveProvider(id)
}
