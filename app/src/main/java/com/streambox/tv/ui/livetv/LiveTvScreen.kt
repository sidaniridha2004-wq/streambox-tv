package com.streambox.tv.ui.livetv

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.streambox.tv.data.Channel
import com.streambox.tv.data.EpgProgram
import com.streambox.tv.nav.Routes
import com.streambox.tv.ui.components.ChannelLogo
import com.streambox.tv.ui.components.GhostButton
import com.streambox.tv.ui.components.PrimaryButton
import com.streambox.tv.ui.components.SearchBar
import com.streambox.tv.ui.components.SecondaryButton
import com.streambox.tv.ui.components.StatusPill
import com.streambox.tv.ui.theme.Bg700
import com.streambox.tv.ui.theme.Bg800
import com.streambox.tv.ui.theme.Bg900
import com.streambox.tv.ui.theme.FocusRing
import com.streambox.tv.ui.theme.GlassStroke
import com.streambox.tv.ui.theme.Red500
import com.streambox.tv.ui.theme.Teal400
import com.streambox.tv.ui.theme.TealGlow
import com.streambox.tv.ui.theme.TextMuted
import com.streambox.tv.ui.theme.TextPrimary
import com.streambox.tv.ui.theme.TextSecondary
import com.streambox.tv.ui.theme.scaleOnFocus
import kotlinx.coroutines.delay

/**
 * Live TV — Netflix-style:
 *  • Top: full-bleed featured hero that auto-rotates between top channels.
 *  • Below: horizontal carousels grouped by category, plus Recently Added
 *    and Favorites rows.
 *  • Left edge: 3-dots handle that opens an animated category drawer (your
 *    earlier request); category selection collapses the screen to a single
 *    long list filtered to that category.
 *  • Search bar is contextual, on top right.
 */
@Composable
fun LiveTvScreen(nav: NavHostController, vm: LiveTvViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var categoriesOpen by remember { mutableStateOf(false) }
    var searchActive by remember { mutableStateOf(false) }
    val isFiltered = state.selectedCategory != "All" || state.query.isNotBlank() || state.selectedQuickFilter != "All"

    Box(modifier = Modifier.fillMaxSize().background(Bg900)) {
        // Reserve the left edge gutter for the dots handle so content never
        // sits flush against the screen edge.
        Row(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.width(56.dp))

            Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                // ---- Top bar: title + search ----
                LiveTopBar(
                    activeCategory = state.selectedCategory,
                    query = state.query,
                    searchActive = searchActive,
                    onSearchToggle = { searchActive = !searchActive },
                    onQueryChange = vm::setQuery,
                    onClearCategory = { vm.selectCategory("All") },
                )

                if (isFiltered) {
                    // ---- FILTERED VIEW: single long list (Netflix's "browse all" mode)
                    FilteredChannelList(
                        channels = state.filteredChannels,
                        epg = state.epgByChannel,
                        currentId = state.currentlyPlayingId,
                        onPlay = { ch ->
                            vm.markPlaying(ch.id)
                            nav.navigate(Routes.player(ch.streamUrl))
                        },
                        onFavorite = vm::toggleFavorite,
                    )
                } else {
                    // ---- DEFAULT VIEW: hero + carousels
                    BrowseLayout(
                        state = state,
                        onPlay = { ch ->
                            vm.markPlaying(ch.id)
                            nav.navigate(Routes.player(ch.streamUrl))
                        },
                        onOpenGuide = { nav.navigate(Routes.Epg) },
                    )
                }
            }
        }

        // Three-dots handle — always visible
        CategoryHandle(
            open = categoriesOpen,
            onToggle = { categoriesOpen = !categoriesOpen },
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp),
        )

        // Drawer + scrim
        AnimatedVisibility(
            visible = categoriesOpen,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { categoriesOpen = false },
            )
        }
        AnimatedVisibility(
            visible = categoriesOpen,
            enter = slideInHorizontally(tween(220)) { -it } + fadeIn(tween(220)),
            exit = slideOutHorizontally(tween(180)) { -it } + fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            CategoryDrawer(
                categories = state.categories,
                selected = state.selectedCategory,
                onSelect = { vm.selectCategory(it); categoriesOpen = false },
                onClose = { categoriesOpen = false },
            )
        }
    }
}

// ---------- Top bar ----------

@Composable
private fun LiveTopBar(
    activeCategory: String,
    query: String,
    searchActive: Boolean,
    onSearchToggle: () -> Unit,
    onQueryChange: (String) -> Unit,
    onClearCategory: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Live TV", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
            if (activeCategory != "All") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(activeCategory, color = Teal400)
                    Spacer(Modifier.width(8.dp))
                    GhostButton(text = "Clear", onClick = onClearCategory)
                }
            }
        }
        AnimatedContent(
            targetState = searchActive,
            transitionSpec = {
                (fadeIn(tween(160)) + slideInHorizontally(tween(180)) { it / 4 }) togetherWith
                    (fadeOut(tween(120)) + slideOutHorizontally(tween(120)) { it / 4 })
            },
            label = "search",
        ) { active ->
            if (active) {
                Box(modifier = Modifier.width(360.dp)) {
                    SearchBar(value = query, onValueChange = onQueryChange, placeholder = "Search channels…")
                }
            } else {
                SecondaryButton(text = "Search", leadingIcon = Icons.Default.Info, onClick = onSearchToggle)
            }
        }
    }
}

// ---------- Browse (hero + carousels) ----------

@Composable
private fun BrowseLayout(
    state: LiveTvUiState,
    onPlay: (Channel) -> Unit,
    onOpenGuide: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item("hero") {
            FeaturedHero(
                featured = state.featured,
                epgByChannel = state.epgByChannel,
                onPlay = onPlay,
                onOpenGuide = onOpenGuide,
            )
        }

        if (state.recentlyAdded.isNotEmpty()) {
            item("recent") {
                CarouselRow(
                    title = "Recently added",
                    channels = state.recentlyAdded,
                    epgByChannel = state.epgByChannel,
                    currentId = state.currentlyPlayingId,
                    onPlay = onPlay,
                )
            }
        }

        if (state.favorites.isNotEmpty()) {
            item("fav") {
                CarouselRow(
                    title = "Your favorites",
                    channels = state.favorites,
                    epgByChannel = state.epgByChannel,
                    currentId = state.currentlyPlayingId,
                    onPlay = onPlay,
                )
            }
        }

        // One row per category (Sports / News / Kids / …)
        items(state.channelsByGroup.entries.toList(), key = { it.key }) { (group, list) ->
            CarouselRow(
                title = group,
                channels = list,
                epgByChannel = state.epgByChannel,
                currentId = state.currentlyPlayingId,
                onPlay = onPlay,
            )
        }
    }
}

@Composable
private fun FeaturedHero(
    featured: List<Channel>,
    epgByChannel: Map<String, Pair<EpgProgram?, EpgProgram?>>,
    onPlay: (Channel) -> Unit,
    onOpenGuide: () -> Unit,
) {
    if (featured.isEmpty()) return
    var idx by remember { mutableIntStateOf(0) }
    LaunchedEffect(featured) {
        while (true) {
            delay(6500)
            idx = (idx + 1) % featured.size
        }
    }
    val channel = featured[idx.coerceIn(featured.indices)]
    val now = epgByChannel[channel.id]?.first

    AnimatedContent(
        targetState = channel.id,
        transitionSpec = { fadeIn(tween(450)) togetherWith fadeOut(tween(450)) },
        label = "hero-rotate",
        modifier = Modifier.fillMaxWidth().height(380.dp),
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0B2030),
                            Color(0xFF11304A),
                            Color(0xFF0A1726),
                        ),
                    ),
                ),
        ) {
            // Bottom-to-top fade so the text rail at the bottom is always legible.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.55f to Bg900.copy(alpha = 0.4f),
                            1f to Bg900,
                        ),
                    ),
            )

            // Glow blob, top-right
            Box(
                modifier = Modifier
                    .size(360.dp)
                    .padding(40.dp)
                    .background(TealGlow, RoundedCornerShape(999.dp))
                    .align(Alignment.TopEnd),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill("● LIVE", color = Red500)
                    StatusPill("FEATURED", color = Teal400)
                    Text(channel.group.uppercase(), color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                }
                Column {
                    Text(
                        channel.name,
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (now != null) "Now playing · ${now.title}" else "Live now",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PrimaryButton(text = "Watch live", leadingIcon = Icons.Default.PlayArrow, onClick = { onPlay(channel) })
                        Spacer(Modifier.width(12.dp))
                        SecondaryButton(text = "Open Guide", leadingIcon = Icons.Default.CalendarMonth, onClick = onOpenGuide)
                        Spacer(Modifier.width(20.dp))
                        // Pagination dots
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            featured.forEachIndexed { i, _ ->
                                Box(
                                    modifier = Modifier
                                        .size(if (i == idx) 22.dp else 6.dp, 6.dp)
                                        .background(
                                            if (i == idx) Teal400 else Color.White.copy(alpha = 0.25f),
                                            RoundedCornerShape(3.dp),
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CarouselRow(
    title: String,
    channels: List<Channel>,
    epgByChannel: Map<String, Pair<EpgProgram?, EpgProgram?>>,
    currentId: String?,
    onPlay: (Channel) -> Unit,
) {
    Column {
        Text(
            title,
            color = TextPrimary,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(channels, key = { it.id }) { ch ->
                val now = epgByChannel[ch.id]?.first
                ChannelTileLarge(
                    channel = ch,
                    nowTitle = now?.title,
                    isCurrent = ch.id == currentId,
                    onClick = { onPlay(ch) },
                )
            }
        }
    }
}

/**
 * 16:9 channel tile — the Netflix card metaphor adapted for live channels.
 * Shows the logo prominently with a rich gradient and now-playing label.
 */
@Composable
private fun ChannelTileLarge(
    channel: Channel,
    nowTitle: String?,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .scaleOnFocus(interactionSource = src)
            .width(260.dp)
            .height(146.dp)
            .background(
                Brush.linearGradient(listOf(Color(0xFF152233), Color(0xFF0E1A2A))),
                shape,
            )
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) FocusRing else if (isCurrent) Teal400 else GlassStroke,
                shape,
            )
            .clickable(interactionSource = src, indication = null, onClick = onClick),
    ) {
        // Top-right quality + LIVE pill
        Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (isCurrent) StatusPill("ON NOW", color = Teal400)
            StatusPill(channel.quality, color = Teal400)
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp),
        ) {
            ChannelLogo(channel.logoUrl, channel.name, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                channel.name,
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (nowTitle != null) {
                Text(
                    "Now · $nowTitle",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ---------- Filtered list (when the user picked a category or typed in search) ----------

@Composable
private fun FilteredChannelList(
    channels: List<Channel>,
    epg: Map<String, Pair<EpgProgram?, EpgProgram?>>,
    currentId: String?,
    onPlay: (Channel) -> Unit,
    onFavorite: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp, ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(channels, key = { it.id }) { ch ->
            val now = epg[ch.id]?.first
            val next = epg[ch.id]?.second
            Box(modifier = Modifier.scaleOnFocus()) {
                com.streambox.tv.ui.components.ChannelListItem(
                    number = ch.number,
                    name = ch.name,
                    group = ch.group,
                    nowTitle = now?.title ?: "—",
                    nextTitle = next?.title ?: "—",
                    logoUrl = ch.logoUrl,
                    quality = ch.quality,
                    isPlaying = ch.id == currentId,
                    onClick = { onPlay(ch) },
                )
            }
        }
    }
}

// ---------- Left-edge handle + drawer ----------

@Composable
private fun CategoryHandle(open: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val shape = RoundedCornerShape(999.dp)
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .scaleOnFocus(interactionSource = src)
            .width(40.dp)
            .height(96.dp)
            .background(if (focused || open) TealGlow else Bg700, shape)
            .border(1.dp, if (focused || open) FocusRing else GlassStroke, shape)
            .clickable(interactionSource = src, indication = null, onClick = onToggle),
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(if (focused || open) Teal400 else TextMuted, RoundedCornerShape(3.dp)),
            )
            if (it < 2) Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun CategoryDrawer(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(Bg800)
            .padding(start = 16.dp, end = 12.dp, top = 24.dp, bottom = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Categories",
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            val src = remember { MutableInteractionSource() }
            val focused by src.collectIsFocusedAsState()
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Bg900, RoundedCornerShape(10.dp))
                    .border(1.dp, if (focused) FocusRing else GlassStroke, RoundedCornerShape(10.dp))
                    .clickable(interactionSource = src, indication = null, onClick = onClose),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.Close, null, tint = TextMuted) }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Pick a category to filter the channel list.",
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(categories) { cat -> CategoryRow(cat, cat == selected) { onSelect(cat) } }
        }
    }
}

@Composable
private fun CategoryRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    val bg = when {
        selected -> TealGlow
        focused -> Color.White.copy(alpha = 0.06f)
        else -> Color.Transparent
    }
    val stroke = when {
        focused -> FocusRing
        selected -> Teal400.copy(alpha = 0.4f)
        else -> Color.Transparent
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(bg, shape)
            .border(1.dp, stroke, shape)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(if (selected) Teal400 else TextMuted.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Text(label, color = if (selected) Teal400 else TextPrimary, style = MaterialTheme.typography.titleSmall)
    }
}
