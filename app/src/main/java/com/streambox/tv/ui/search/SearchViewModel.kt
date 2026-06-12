package com.streambox.tv.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streambox.tv.data.Channel
import com.streambox.tv.data.IptvRepository
import com.streambox.tv.data.Movie
import com.streambox.tv.data.Series
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val channels: List<Channel> = emptyList(),
    val movies: List<Movie> = emptyList(),
    val series: List<Series> = emptyList(),
    val recents: List<String> = listOf("BeIN Sports", "Real Madrid", "Dune", "House of the Dragon"),
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: IptvRepository,
) : ViewModel() {
    private val q = MutableStateFlow("")
    val state = combine(q, repo.channels, repo.movies, repo.series) { query, channels, movies, series ->
        if (query.isBlank()) SearchUiState(query = "")
        else SearchUiState(
            query = query,
            channels = channels.filter { it.name.contains(query, ignoreCase = true) },
            movies = movies.filter { it.title.contains(query, ignoreCase = true) },
            series = series.filter { it.title.contains(query, ignoreCase = true) },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SearchUiState())

    fun setQuery(value: String) { q.value = value }
}
