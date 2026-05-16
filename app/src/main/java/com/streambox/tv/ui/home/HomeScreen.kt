package com.streambox.tv.ui.home

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.streambox.tv.data.Provider
import com.streambox.tv.data.ProviderStatus
import com.streambox.tv.data.ProviderType
import com.streambox.tv.nav.Routes
import com.streambox.tv.ui.components.ChannelLogo
import com.streambox.tv.ui.components.GhostButton
import com.streambox.tv.ui.components.PosterCard
import com.streambox.tv.ui.components.PrimaryButton
import com.streambox.tv.ui.components.QuickActionCard
import com.streambox.tv.ui.components.SecondaryButton
import com.streambox.tv.ui.components.SectionHeader
import com.streambox.tv.ui.components.StatusPill
import com.streambox.tv.ui.theme.Bg700
import com.streambox.tv.ui.theme.Bg900
import com.streambox.tv.ui.theme.FocusRing
import com.streambox.tv.ui.theme.GlassStroke
import com.streambox.tv.ui.theme.Green500
import com.streambox.tv.ui.theme.Amber500
import com.streambox.tv.ui.theme.Red500
import com.streambox.tv.ui.theme.Teal400
import com.streambox.tv.ui.theme.TextMuted
import com.streambox.tv.ui.theme.TextPrimary
import com.streambox.tv.ui.theme.TextSecondary

@Composable
fun HomeScreen(nav: NavHostController, vm: HomeViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg900)
            .verticalScroll(scroll),
    ) {
        TopBar(
            providers = state.providers,
            active = state.activeProvider,
            onProviderSelect = vm::setActiveProvider,
            onAddProvider = { nav.navigate(Routes.LoginChooser) },
            onSearch = { nav.navigate(Routes.Search) },
        )
        HeroSection(
            channelName = state.featuredChannel?.name ?: "No active channel",
            channelGroup = state.featuredChannel?.group ?: "—",
            nowTitle = state.featuredEpg?.title ?: "—",
            nowProgress = state.featuredEpg?.let { e ->
                val elapsed = (-e.startMinute).coerceAtLeast(0)
                (elapsed.toFloat() / e.durationMinutes).coerceIn(0f, 1f)
            } ?: 0f,
            onPlay = {
                state.featuredChannel?.streamUrl?.let { nav.navigate(Routes.player(it)) }
            },
            onGuide = { nav.navigate(Routes.Epg) },
        )
        Spacer(Modifier.height(8.dp))
        QuickActions(nav)
        Spacer(Modifier.height(8.dp))

        SectionHeader("Continue watching")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp),
        ) {
            items(state.continueWatching) { cw -> ContinueCard(cw.title, cw.subtitle, cw.progress) {} }
        }
        Spacer(Modifier.height(16.dp))

        SectionHeader("Recently added channels", actionText = "Live TV") { nav.navigate(Routes.LiveTv) }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp),
        ) {
            items(state.recentlyAdded) { ch ->
                ChannelChipCard(ch.name, ch.group, ch.quality) {
                    nav.navigate(Routes.player(ch.streamUrl))
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        SectionHeader("Movies for you", actionText = "All movies") { nav.navigate(Routes.Movies) }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp),
        ) {
            items(state.movies.take(10)) { m ->
                Box(modifier = Modifier.width(140.dp)) {
                    PosterCard(title = m.title, subtitle = "${m.year} · ${m.genre}", imageUrl = m.posterUrl) {
                        nav.navigate(Routes.movieDetails(m.id))
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        SectionHeader("Series", actionText = "All series") { nav.navigate(Routes.Series) }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp),
        ) {
            items(state.series) { s ->
                Box(modifier = Modifier.width(140.dp)) {
                    PosterCard(title = s.title, subtitle = "${s.year} · ${s.genre}", imageUrl = s.posterUrl) {
                        nav.navigate(Routes.seriesDetails(s.id))
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        SectionHeader("Favorite channels", actionText = "All favorites") { nav.navigate(Routes.Favorites) }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp),
        ) {
            items(state.favoriteChannels) { ch ->
                ChannelChipCard(ch.name, ch.group, ch.quality) {
                    nav.navigate(Routes.player(ch.streamUrl))
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun TopBar(
    providers: List<Provider>,
    active: Provider?,
    onProviderSelect: (String) -> Unit,
    onAddProvider: () -> Unit,
    onSearch: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Welcome back", color = TextMuted, style = MaterialTheme.typography.labelMedium)
            Text("StreamBox TV", color = TextPrimary, style = MaterialTheme.typography.headlineMedium)
        }
        ProviderSwitcher(providers, active, onProviderSelect, onAddProvider)
        Spacer(Modifier.width(12.dp))
        SecondaryButton(text = "Search", leadingIcon = Icons.Default.Search, onClick = onSearch)
    }
}

@Composable
private fun ProviderSwitcher(
    providers: List<Provider>,
    active: Provider?,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Bg700, shape)
            .border(1.dp, if (focused) FocusRing else GlassStroke, shape)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .clickable(interactionSource = src, indication = null) {
                // simple cycle through providers — a bottom sheet would replace this in a full impl
                val idx = providers.indexOfFirst { it.id == active?.id }.coerceAtLeast(0)
                val next = providers.getOrNull((idx + 1) % providers.size.coerceAtLeast(1))
                if (next != null) onSelect(next.id) else onAdd()
            },
    ) {
        Box(
            modifier = Modifier.size(32.dp).background(Color(0x3314B8A6), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                (active?.name ?: "+").take(1).uppercase(),
                color = Teal400,
                style = MaterialTheme.typography.titleSmall,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(active?.name ?: "Add provider", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
            Text(
                "${active?.type?.name ?: "—"} · ${providerStatusLabel(active)}",
                color = TextMuted,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Spacer(Modifier.width(10.dp))
        StatusPill(text = active?.status?.name ?: "NEW", color = providerStatusColor(active?.status))
    }
}

private fun providerStatusLabel(p: Provider?): String = when (p?.status) {
    ProviderStatus.OK -> "${p.channelCount} channels · ${p.lastSync}"
    ProviderStatus.SYNCING -> "syncing…"
    ProviderStatus.EXPIRED -> "expired · refresh required"
    ProviderStatus.FAILED -> "failed to connect"
    null -> "no provider"
}

private fun providerStatusColor(s: ProviderStatus?) = when (s) {
    ProviderStatus.OK -> Green500
    ProviderStatus.SYNCING -> Teal400
    ProviderStatus.EXPIRED -> Amber500
    ProviderStatus.FAILED -> Red500
    null -> TextMuted
}

@Composable
private fun HeroSection(
    channelName: String,
    channelGroup: String,
    nowTitle: String,
    nowProgress: Float,
    onPlay: () -> Unit,
    onGuide: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(260.dp)
            .background(
                Brush.horizontalGradient(listOf(Color(0xFF0E2A2D), Color(0xFF0A1726), Color(0xFF11304A))),
                shape,
            )
            .border(1.dp, GlassStroke, shape)
            .padding(28.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(text = "● LIVE", color = Red500)
                Spacer(Modifier.width(10.dp))
                Text(channelGroup, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            }
            Spacer(Modifier.height(10.dp))
            Text(channelName, style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
            Spacer(Modifier.height(6.dp))
            Text("Now playing · $nowTitle", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(2.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(nowProgress)
                        .height(4.dp)
                        .background(Teal400, RoundedCornerShape(2.dp)),
                )
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PrimaryButton(text = "Watch live", leadingIcon = Icons.Default.PlayArrow, onClick = onPlay)
                Spacer(Modifier.width(12.dp))
                SecondaryButton(text = "Open Guide", leadingIcon = Icons.Default.CalendarMonth, onClick = onGuide)
                Spacer(Modifier.width(12.dp))
                GhostButton(text = "Channel info", onClick = {})
            }
        }
    }
}

@Composable
private fun QuickActions(nav: NavHostController) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        QuickActionCard(Icons.Default.LiveTv, "Live TV", modifier = Modifier.weight(1f)) { nav.navigate(Routes.LiveTv) }
        QuickActionCard(Icons.Default.Movie, "Movies", modifier = Modifier.weight(1f)) { nav.navigate(Routes.Movies) }
        QuickActionCard(Icons.Default.Tv, "Series", modifier = Modifier.weight(1f)) { nav.navigate(Routes.Series) }
        QuickActionCard(Icons.Default.CalendarMonth, "Guide", modifier = Modifier.weight(1f)) { nav.navigate(Routes.Epg) }
        QuickActionCard(Icons.Default.Favorite, "Favorites", modifier = Modifier.weight(1f)) { nav.navigate(Routes.Favorites) }
        QuickActionCard(Icons.Default.Settings, "Settings", modifier = Modifier.weight(1f)) { nav.navigate(Routes.Settings) }
    }
}

@Composable
private fun ContinueCard(title: String, subtitle: String, progress: Float, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .width(280.dp)
            .height(120.dp)
            .background(Bg700, shape)
            .border(1.dp, if (focused) FocusRing else GlassStroke, shape)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).background(Color(0x3314B8A6), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Icon(Icons.Default.PlayArrow, null, tint = Teal400)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, color = TextMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier.fillMaxWidth().height(4.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(2.dp)),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(progress).height(4.dp).background(Teal400, RoundedCornerShape(2.dp)),
            )
        }
    }
}

@Composable
private fun ChannelChipCard(name: String, group: String, quality: String, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .width(240.dp)
            .height(72.dp)
            .background(Bg700, shape)
            .border(1.dp, if (focused) FocusRing else GlassStroke, shape)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(10.dp),
    ) {
        ChannelLogo(logoUrl = null, fallbackLetter = name, modifier = Modifier.size(48.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = TextPrimary, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(group, color = TextMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
        StatusPill(quality, color = Teal400)
    }
}
