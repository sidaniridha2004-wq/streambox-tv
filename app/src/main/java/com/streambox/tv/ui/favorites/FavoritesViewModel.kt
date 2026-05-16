package com.streambox.tv.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streambox.tv.data.Channel
import com.streambox.tv.data.IptvRepository
import com.streambox.tv.data.Movie
import com.streambox.tv.data.Series
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class FavoritesUiState(
    val channels: List<Channel> = emptyList(),
    val movies: List<Movie> = emptyList(),
    val series: List<Series> = emptyList(),
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(repo: IptvRepository) : ViewModel() {
    val state = combine(repo.channels, repo.movies, repo.series) { c, m, s ->
        FavoritesUiState(
            channels = c.filter { it.isFavorite },
            movies = m.filter { it.isFavorite },
            series = s.filter { it.isFavorite },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, FavoritesUiState())
}
