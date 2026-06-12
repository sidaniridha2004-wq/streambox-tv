package com.streambox.tv.ui.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streambox.tv.data.IptvRepository
import com.streambox.tv.data.Movie
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class MoviesUiState(
    val genre: String = "All",
    val sort: String = "Recently added",
    val query: String = "",
    val filtered: List<Movie> = emptyList(),
)

@HiltViewModel
class MoviesViewModel @Inject constructor(
    private val repo: IptvRepository,
) : ViewModel() {
    private val genre = MutableStateFlow("All")
    private val sort = MutableStateFlow("Recently added")
    private val query = MutableStateFlow("")

    val state = combine(repo.movies, genre, sort, query) { movies, g, s, q ->
        var list = if (g == "All") movies else movies.filter { it.genre.equals(g, ignoreCase = true) }
        if (q.isNotBlank()) list = list.filter { it.title.contains(q, ignoreCase = true) }
        list = when (s) {
            "Highest rated" -> list.sortedByDescending { it.rating }
            "A–Z" -> list.sortedBy { it.title }
            else -> list
        }
        MoviesUiState(g, s, q, list)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MoviesUiState())

    fun setGenre(v: String) { genre.value = v }
    fun setSort(v: String) { sort.value = v }
    fun setQuery(v: String) { query.value = v }
    fun toggleFavorite(id: String) { repo.toggleMovieFavorite(id) }
    fun get(id: String) = repo.movies.value.firstOrNull { it.id == id }
    fun related(id: String): List<Movie> {
        val m = get(id) ?: return emptyList()
        return repo.movies.value.filter { it.id != id && (it.genre == m.genre || kotlin.math.abs(it.rating - m.rating) < 1) }.take(8)
    }
}
