package com.streambox.tv.ui.series

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.streambox.tv.ui.components.FilterChipRow
import com.streambox.tv.ui.components.PosterCard
import com.streambox.tv.ui.components.SearchBar
import com.streambox.tv.ui.theme.Bg900
import com.streambox.tv.ui.theme.TextMuted
import com.streambox.tv.ui.theme.TextPrimary

@Composable
fun SeriesScreen(nav: NavHostController, vm: SeriesViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().background(Bg900)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Series", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Text("${state.filtered.size} titles", style = MaterialTheme.typography.labelMedium, color = TextMuted)
            }
            Box(modifier = Modifier.width(360.dp)) {
                SearchBar(value = query, onValueChange = { q -> query = q; vm.setQuery(q) }, placeholder = "Search series…")
            }
        }
        FilterChipRow(
            items = listOf("All", "Drama", "Fantasy", "Mystery", "Sci-Fi"),
            selected = state.genre,
            onSelected = vm::setGenre,
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(state.filtered, key = { it.id }) { s ->
                PosterCard(
                    title = s.title,
                    subtitle = "${s.year} · ${s.genre} · ★${s.rating}",
                    imageUrl = s.posterUrl,
                    onClick = { nav.navigate(Routes.seriesDetails(s.id)) },
                )
            }
        }
    }
}
