package com.streambox.tv.ui.livetv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.streambox.tv.data.Channel
import com.streambox.tv.data.EpgProgram
import com.streambox.tv.nav.Routes
import com.streambox.tv.ui.components.ChannelListItem
import com.streambox.tv.ui.components.FilterChipRow
import com.streambox.tv.ui.components.SearchBar
import com.streambox.tv.ui.theme.Bg700
import com.streambox.tv.ui.theme.Bg800
import com.streambox.tv.ui.theme.Bg900
import com.streambox.tv.ui.theme.FocusRing
import com.streambox.tv.ui.theme.GlassStroke
import com.streambox.tv.ui.theme.Teal400
import com.streambox.tv.ui.theme.TealGlow
import com.streambox.tv.ui.theme.TextMuted
import com.streambox.tv.ui.theme.TextPrimary
import com.streambox.tv.ui.theme.TextSecondary

@Composable
fun LiveTvScreen(nav: NavHostController, vm: LiveTvViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    Row(modifier = Modifier.fillMaxSize().background(Bg900)) {
        // Category sidebar
        CategorySidebar(
            categories = state.categories,
            selected = state.selectedCategory,
            onSelect = vm::selectCategory,
        )

        Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(end = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Live TV", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    Text(
                        "${state.filteredChannels.size} channels in ${state.selectedCategory}",
                        color = TextMuted,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Box(modifier = Modifier.width(360.dp)) {
                    SearchBar(value = state.query, onValueChange = vm::setQuery, placeholder = "Search channels…")
                }
            }
            FilterChipRow(
                items = listOf("All", "Sports", "News", "Kids", "Movies", "Arabic", "French", "Favorites"),
                selected = state.selectedQuickFilter,
                onSelected = vm::setQuickFilter,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 12.dp),
            )

            Row(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.weight(2f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.filteredChannels) { ch ->
                        val now = state.epgByChannel[ch.id]?.first
                        val next = state.epgByChannel[ch.id]?.second
                        ChannelListItem(
                            number = ch.number,
                            name = ch.name,
                            group = ch.group,
                            nowTitle = now?.title ?: "—",
                            nextTitle = next?.title ?: "—",
                            logoUrl = ch.logoUrl,
                            quality = ch.quality,
                            isPlaying = ch.id == state.currentlyPlayingId,
                            onClick = {
                                vm.markPlaying(ch.id)
                                nav.navigate(Routes.player(ch.streamUrl))
                            },
                        )
                    }
                }
                EpgPreviewPanel(
                    selectedChannel = state.filteredChannels.firstOrNull(),
                    epg = state.epgByChannel[state.filteredChannels.firstOrNull()?.id ?: ""]?.let {
                        listOfNotNull(it.first, it.second)
                    } ?: emptyList(),
                )
            }
        }
    }
}

@Composable
private fun CategorySidebar(categories: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(240.dp)
            .background(Bg800)
            .padding(vertical = 16.dp, horizontal = 8.dp),
    ) {
        Text(
            "Categories",
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(categories) { cat ->
                CategoryRow(cat, cat == selected, count = null) { onSelect(cat) }
            }
        }
    }
}

@Composable
private fun CategoryRow(label: String, selected: Boolean, count: Int?, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)
    val bg = when {
        selected -> TealGlow
        focused -> androidx.compose.ui.graphics.Color.White.copy(alpha = 0.06f)
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    val stroke = when {
        focused -> FocusRing
        selected -> Teal400.copy(alpha = 0.4f)
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(bg, shape)
            .border(1.dp, stroke, shape)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp),
    ) {
        Text(label, color = if (selected) Teal400 else TextPrimary, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        if (count != null) Text(count.toString(), color = TextMuted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun EpgPreviewPanel(selectedChannel: Channel?, epg: List<EpgProgram>) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(320.dp)
            .padding(start = 16.dp, top = 4.dp)
            .background(Bg800, RoundedCornerShape(16.dp))
            .border(1.dp, GlassStroke, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text("EPG preview", color = TextMuted, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(6.dp))
        Text(selectedChannel?.name ?: "No selection", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        epg.forEachIndexed { idx, program ->
            val isNow = idx == 0
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isNow) TealGlow else Bg700, RoundedCornerShape(10.dp))
                    .border(1.dp, if (isNow) Teal400 else GlassStroke, RoundedCornerShape(10.dp))
                    .padding(12.dp),
            ) {
                Text(if (isNow) "NOW" else "NEXT", color = if (isNow) Teal400 else TextMuted, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(2.dp))
                Text(program.title, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                Text("${program.durationMinutes} min", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
