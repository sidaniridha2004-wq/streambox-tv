package com.streambox.tv.ui.series

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.streambox.tv.data.Episode
import com.streambox.tv.nav.Routes
import com.streambox.tv.ui.components.GhostButton
import com.streambox.tv.ui.components.PrimaryButton
import com.streambox.tv.ui.components.SecondaryButton
import com.streambox.tv.ui.components.StatusPill
import com.streambox.tv.ui.theme.Amber500
import com.streambox.tv.ui.theme.Bg700
import com.streambox.tv.ui.theme.Bg900
import com.streambox.tv.ui.theme.FocusRing
import com.streambox.tv.ui.theme.GlassStroke
import com.streambox.tv.ui.theme.Teal400
import com.streambox.tv.ui.theme.TealGlow
import com.streambox.tv.ui.theme.TextMuted
import com.streambox.tv.ui.theme.TextPrimary
import com.streambox.tv.ui.theme.TextSecondary

@Composable
fun SeriesDetailsScreen(nav: NavHostController, id: String, vm: SeriesViewModel = hiltViewModel()) {
    val series = vm.get(id) ?: return
    var selectedSeason by remember { mutableStateOf(series.seasons.firstOrNull()?.number ?: 1) }
    val season = series.seasons.firstOrNull { it.number == selectedSeason } ?: series.seasons.first()

    Column(modifier = Modifier.fillMaxSize().background(Bg900).verticalScroll(rememberScrollState())) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFF1B1130), Bg900))),
        ) {
            Row(modifier = Modifier.fillMaxSize().padding(32.dp), verticalAlignment = Alignment.Bottom) {
                Box(
                    modifier = Modifier.width(160.dp).aspectRatio(2f / 3f).background(Bg700, RoundedCornerShape(14.dp)).border(1.dp, GlassStroke, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) { Text(series.title.take(1).uppercase(), color = Teal400, style = MaterialTheme.typography.displayMedium) }
                Spacer(Modifier.width(24.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill("SERIES", color = Teal400)
                        StatusPill("★ ${series.rating}", color = Amber500)
                        Text("${series.year} · ${series.genre} · ${series.seasons.size} seasons", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(series.title, style = MaterialTheme.typography.displaySmall, color = TextPrimary)
                    Spacer(Modifier.height(10.dp))
                    Text(series.description, color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PrimaryButton(
                            text = "Play next episode",
                            leadingIcon = Icons.Default.PlayArrow,
                            onClick = {
                                val next = series.seasons.flatMap { it.episodes }.firstOrNull { it.watchedRatio < 1f }
                                if (next != null) nav.navigate(Routes.player(next.streamUrl))
                            },
                        )
                        Spacer(Modifier.width(12.dp))
                        SecondaryButton(
                            text = if (series.isFavorite) "In favorites" else "Add to favorites",
                            leadingIcon = if (series.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            onClick = { vm.toggleFavorite(id) },
                        )
                    }
                }
            }
        }

        // Season selector
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Seasons", color = TextMuted, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(8.dp))
            series.seasons.forEach { s ->
                SeasonChip("Season ${s.number}", selected = s.number == selectedSeason) { selectedSeason = s.number }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            season.episodes.forEach { ep ->
                EpisodeRow(ep) { nav.navigate(Routes.player(ep.streamUrl)) }
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SeasonChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = Modifier
            .height(34.dp)
            .background(if (selected) TealGlow else Bg700, shape)
            .border(1.dp, if (focused) FocusRing else if (selected) Teal400 else GlassStroke, shape)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = if (selected) Teal400 else TextPrimary, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun EpisodeRow(ep: Episode, onPlay: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .background(Bg700, shape)
            .border(1.dp, if (focused) FocusRing else GlassStroke, shape)
            .clickable(interactionSource = src, indication = null, onClick = onPlay)
            .padding(horizontal = 16.dp),
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(TealGlow, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) { Text("E${ep.number}", color = Teal400, style = MaterialTheme.typography.titleSmall) }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Episode ${ep.number} · ${ep.title}", color = TextPrimary, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text("${ep.durationMin} min", color = TextMuted, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier.fillMaxWidth(0.5f).height(3.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(2.dp)),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(ep.watchedRatio).height(3.dp).background(Teal400, RoundedCornerShape(2.dp)),
                )
            }
        }
        Icon(Icons.Default.PlayArrow, null, tint = TextMuted)
    }
}
