package com.streambox.tv.ui.livetv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streambox.tv.data.Channel
import com.streambox.tv.data.EpgProgram
import com.streambox.tv.data.IptvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class LiveTvUiState(
    val categories: List<String> = emptyList(),
    val selectedCategory: String = "All",
    val selectedQuickFilter: String = "All",
    val query: String = "",
    val filteredChannels: List<Channel> = emptyList(),
    /** Channels grouped into Netflix-style rows ("Sports", "News" …). */
    val channelsByGroup: Map<String, List<Channel>> = emptyMap(),
    /** Featured carousel (top of screen, auto-rotates). */
    val featured: List<Channel> = emptyList(),
    val recentlyAdded: List<Channel> = emptyList(),
    val favorites: List<Channel> = emptyList(),
    val epgByChannel: Map<String, Pair<EpgProgram?, EpgProgram?>> = emptyMap(),
    val currentlyPlayingId: String? = null,
)

@HiltViewModel
class LiveTvViewModel @Inject constructor(
    private val repo: IptvRepository,
) : ViewModel() {

    private val selectedCategory = MutableStateFlow("All")
    private val quickFilter = MutableStateFlow("All")
    private val query = MutableStateFlow("")
    private val playingId = MutableStateFlow<String?>(null)

    val state = combine(
        repo.channels,
        selectedCategory,
        quickFilter,
        query,
        playingId,
    ) { channels, cat, quick, q, playing ->
        val byCategory = if (cat == "All") channels
            else channels.filter { it.group.equals(cat, ignoreCase = true) }
        val afterQuick = when (quick) {
            "Favorites" -> byCategory.filter { it.isFavorite }
            "All" -> byCategory
            else -> byCategory.filter { it.group.equals(quick, ignoreCase = true) }
        }
        val filtered = if (q.isBlank()) afterQuick
            else afterQuick.filter { it.name.contains(q, ignoreCase = true) }

        // Build EPG map for the FILTERED list (faster than building for all).
        val epgMap = filtered.associate { ch ->
            val byCh = repo.epg.filter { it.channelId == ch.id }.sortedBy { it.startMinute }
            val now = byCh.firstOrNull { it.startMinute <= 0 && it.startMinute + it.durationMinutes > 0 }
            val next = byCh.firstOrNull { it.startMinute > 0 }
            ch.id to (now to next)
        }

        // Group rows are computed against the full channel list so they stay
        // stable when you scrub the filter chips. Each row keeps its order.
        val grouped: Map<String, List<Channel>> = channels.groupBy { it.group }
            .filterValues { it.isNotEmpty() }
            .toSortedMap()

        LiveTvUiState(
            categories = listOf("All") + grouped.keys.toList(),
            selectedCategory = cat,
            selectedQuickFilter = quick,
            query = q,
            filteredChannels = filtered,
            channelsByGroup = grouped,
            featured = channels.take(5),
            recentlyAdded = channels.takeLast(8),
            favorites = channels.filter { it.isFavorite },
            epgByChannel = epgMap,
            currentlyPlayingId = playing,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LiveTvUiState())

    fun selectCategory(c: String) { selectedCategory.value = c }
    fun setQuickFilter(c: String) { quickFilter.value = c }
    fun setQuery(q: String) { query.value = q }
    fun markPlaying(id: String) { playingId.value = id }
    fun toggleFavorite(id: String) = repo.toggleChannelFavorite(id)
}
