package com.streambox.tv.ui.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streambox.tv.data.IptvRepository
import com.streambox.tv.data.Series
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SeriesUiState(
    val genre: String = "All",
    val query: String = "",
    val filtered: List<Series> = emptyList(),
)

@HiltViewModel
class SeriesViewModel @Inject constructor(
    private val repo: IptvRepository,
) : ViewModel() {
    private val genre = MutableStateFlow("All")
    private val query = MutableStateFlow("")

    val state = combine(repo.series, genre, query) { list, g, q ->
        var l = if (g == "All") list else list.filter { it.genre.equals(g, ignoreCase = true) }
        if (q.isNotBlank()) l = l.filter { it.title.contains(q, ignoreCase = true) }
        SeriesUiState(g, q, l)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SeriesUiState())

    fun setGenre(v: String) { genre.value = v }
    fun setQuery(v: String) { query.value = v }
    fun get(id: String) = repo.series.value.firstOrNull { it.id == id }
    fun toggleFavorite(id: String) = repo.toggleSeriesFavorite(id)
}
