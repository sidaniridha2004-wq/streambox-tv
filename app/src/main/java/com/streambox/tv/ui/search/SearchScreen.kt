package com.streambox.tv.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.streambox.tv.nav.Routes
import com.streambox.tv.ui.components.ChannelListItem
import com.streambox.tv.ui.components.EmptyState
import com.streambox.tv.ui.components.FilterChip
import com.streambox.tv.ui.components.PosterCard
import com.streambox.tv.ui.components.SearchBar
import com.streambox.tv.ui.components.SegmentedTabs
import com.streambox.tv.ui.theme.Bg900
import com.streambox.tv.ui.theme.TextMuted
import com.streambox.tv.ui.theme.TextPrimary

@Composable
fun SearchScreen(nav: NavHostController, vm: SearchViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf("Channels") }

    Column(modifier = Modifier.fillMaxSize().background(Bg900)) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp)) {
            Text("Search", color = TextPrimary, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            SearchBar(
                value = state.query,
                onValueChange = vm::setQuery,
                placeholder = "Search across providers, channels, movies, series…",
            )
        }

        if (state.query.isBlank()) {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text("Recent searches", color = TextMuted, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.recents.forEach { r ->
                        FilterChip(label = r, selected = false, onClick = { vm.setQuery(r) })
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            EmptyState("Type to search", "We’ll search channels, movies and series at the same time.")
            return@Column
        }

        SegmentedTabs(
            items = listOf("Channels", "Movies", "Series"),
            selected = tab,
            onSelected = { tab = it },
        )
        when (tab) {
            "Channels" -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.channels) { ch ->
                    ChannelListItem(
                        number = ch.number,
                        name = ch.name,
                        group = ch.group,
                        nowTitle = "—",
                        nextTitle = "—",
                        logoUrl = ch.logoUrl,
                        quality = ch.quality,
                        onClick = { nav.navigate(Routes.player(ch.streamUrl)) },
                    )
                }
            }
            "Movies" -> LazyVerticalGrid(
                columns = GridCells.Adaptive(140.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(state.movies, key = { it.id }) { m ->
                    PosterCard(m.title, "${m.year} · ${m.genre}", m.posterUrl) { nav.navigate(Routes.movieDetails(m.id)) }
                }
            }
            "Series" -> LazyVerticalGrid(
                columns = GridCells.Adaptive(140.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(state.series, key = { it.id }) { s ->
                    PosterCard(s.title, "${s.year} · ${s.genre}", s.posterUrl) { nav.navigate(Routes.seriesDetails(s.id)) }
                }
            }
        }
    }
}
