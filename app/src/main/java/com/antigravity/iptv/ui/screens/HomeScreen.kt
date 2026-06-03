package com.antigravity.iptv.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.antigravity.iptv.data.local.entity.ChannelEntity
import com.antigravity.iptv.ui.MainViewModel
import com.antigravity.iptv.ui.theme.*
import com.antigravity.iptv.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onChannelClick: (String, String) -> Unit,
    onSwitchPlaylist: () -> Unit,
    onSettingsClick: () -> Unit,
    onNavigateToCategory: (String) -> Unit = {}
) {
    val activePlaylistId by viewModel.activePlaylistId.collectAsState()
    val playlists by viewModel.playlists.collectAsState(initial = emptyList())
    val activePlaylist = playlists.find { it.id == activePlaylistId }
    val isLoading by viewModel.isLoading.collectAsState()
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(activePlaylistId) {
        if (activePlaylist != null) {
            viewModel.checkAndSyncPlaylist(activePlaylist!!)
        }
    }

    val movies by remember(activePlaylist) { 
        if (activePlaylist != null) viewModel.getMovies(activePlaylist!!.id) else kotlinx.coroutines.flow.flowOf(emptyList()) 
    }.collectAsState(initial = emptyList())
    
    val liveChannels by remember(activePlaylist) { 
        if (activePlaylist != null) viewModel.getLiveTv(activePlaylist!!.id) else kotlinx.coroutines.flow.flowOf(emptyList()) 
    }.collectAsState(initial = emptyList())
    
    val series by remember(activePlaylist) { 
        if (activePlaylist != null) viewModel.getSeries(activePlaylist!!.id) else kotlinx.coroutines.flow.flowOf(emptyList()) 
    }.collectAsState(initial = emptyList())

    val filterAll = stringResource(R.string.filter_all)
    val filterVod = stringResource(R.string.filter_vod)
    val filterSeries = stringResource(R.string.filter_series)
    val filterLiveTv = stringResource(R.string.filter_live_tv)
    val filters = listOf(filterAll, filterVod, filterSeries, filterLiveTv)
    var selectedFilter by remember { mutableStateOf(filterAll) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Aura ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Text("TV", color = AuraPurple, fontSize = 12.sp, modifier = Modifier.background(Color.White, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                },
                actions = {
                    IconButton(onClick = onSwitchPlaylist) { Icon(Icons.Outlined.Person, contentDescription = "Switch Playlist", tint = MaterialTheme.colorScheme.onBackground) }
                    IconButton(onClick = onSettingsClick) { Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onBackground) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        if (activePlaylist == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AuraCyan)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                // Filter Pills
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filters) { filter ->
                        val isSelected = filter == selectedFilter
                        Surface(
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.bounceClick { 
                                selectedFilter = filter 
                                when(filter) {
                                    filterVod -> onNavigateToCategory("movies")
                                    filterSeries -> onNavigateToCategory("series")
                                    filterLiveTv -> onNavigateToCategory("live_tv")
                                }
                            }
                        ) {
                            Text(
                                text = filter,
                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Auto-Scrolling Hero Carousel
                    item {
                        val heroChannels = remember(movies, liveChannels, series) {
                            (movies.take(5) + series.take(3) + liveChannels.take(2)).distinctBy { it.name }.take(8)
                        }
                        if (heroChannels.isNotEmpty()) {
                            AutoScrollHeroBanner(
                                channels = heroChannels,
                                onPlayClick = { channel ->
                                    viewModel.getStreamUrl(
                                        channel = channel,
                                        playlistId = activePlaylist!!.id,
                                        onResolved = { url -> onChannelClick(url, channel.name) },
                                        onError = { errorMsg = it }
                                    )
                                }
                            )
                        }
                    }

                    // Series recently added
                    if (series.isNotEmpty() || movies.isNotEmpty()) {
                        item {
                            val items = if (series.isNotEmpty()) series.take(10) else movies.take(10)
                            StreamBoxMovieRow(
                                title = stringResource(R.string.recently_added_series, items.size),
                                channels = items,
                                viewModel = viewModel,
                                playlistId = activePlaylist!!.id,
                                onChannelClick = onChannelClick,
                                onError = { errorMsg = it },
                                onShowAllClick = { onNavigateToCategory(if (series.isNotEmpty()) "series" else "movies") }
                            )
                        }
                    }

                    // Live TV recently added
                    if (liveChannels.isNotEmpty()) {
                        item {
                            StreamBoxLiveRow(
                                title = stringResource(R.string.recently_added_live_tv),
                                channels = liveChannels.take(10),
                                viewModel = viewModel,
                                playlistId = activePlaylist!!.id,
                                onChannelClick = onChannelClick,
                                onError = { errorMsg = it },
                                onShowAllClick = { onNavigateToCategory("live_tv") }
                            )
                        }
                    }
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AuraCyan)
            }
        }

        if (errorMsg != null) {
            AlertDialog(
                onDismissRequest = { errorMsg = null },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                title = { Text(stringResource(R.string.error), color = MaterialTheme.colorScheme.onBackground) },
                text = { Text(errorMsg!!, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                confirmButton = { TextButton(onClick = { errorMsg = null }) { Text("OK", color = AuraCyan) } }
            )
        }
    }
}

@Composable
fun AutoScrollHeroBanner(
    channels: List<ChannelEntity>,
    onPlayClick: (ChannelEntity) -> Unit
) {
    val configuration = LocalConfiguration.current
    val bannerHeight = configuration.screenHeightDp.dp * 0.4f
    
    var currentIndex by remember { mutableStateOf(0) }
    val currentChannel = channels.getOrNull(currentIndex) ?: return
    
    // Auto-scroll every 4 seconds
    LaunchedEffect(channels.size) {
        if (channels.size > 1) {
            while (true) {
                kotlinx.coroutines.delay(4000)
                currentIndex = (currentIndex + 1) % channels.size
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.trending_now),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Aura", color = AuraCyan, fontWeight = FontWeight.Bold)
                Text("TV", color = AuraPurple, fontWeight = FontWeight.Bold)
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(bannerHeight)
                .bounceClick { onPlayClick(currentChannel) },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Crossfade between channels
                androidx.compose.animation.Crossfade(
                    targetState = currentIndex,
                    animationSpec = androidx.compose.animation.core.tween(800),
                    label = "hero_crossfade"
                ) { index ->
                    val ch = channels.getOrNull(index)
                    if (ch != null && !ch.logoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ch.logoUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().background(
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(AuraPurple.copy(alpha = 0.3f), AuraCyan.copy(alpha = 0.3f))
                                )
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ch?.name ?: "",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }
                }

                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xFF06091A).copy(alpha = 0.9f))
                            )
                        )
                )

                // Channel name at bottom
                Text(
                    text = currentChannel.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 40.dp, end = 16.dp)
                )

                // Animated dots indicator
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    channels.forEachIndexed { index, _ ->
                        val isActive = index == currentIndex
                        val dotWidth by androidx.compose.animation.core.animateDpAsState(
                            targetValue = if (isActive) 20.dp else 6.dp,
                            animationSpec = androidx.compose.animation.core.tween(300)
                        )
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(dotWidth)
                                .clip(RoundedCornerShape(50))
                                .background(if (isActive) AuraCyan else Color.White.copy(alpha = 0.4f))
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StreamBoxMovieRow(
    title: String,
    channels: List<ChannelEntity>,
    viewModel: MainViewModel,
    playlistId: Int,
    onChannelClick: (String, String) -> Unit,
    onError: (String) -> Unit,
    onShowAllClick: (() -> Unit)? = null
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            if (onShowAllClick != null) {
                Text(stringResource(R.string.see_all), color = AuraPurple, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onShowAllClick))
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(channels, key = { it.id }) { channel ->
                MovieCard(
                    channel = channel,
                    onClick = {
                        viewModel.getStreamUrl(
                            channel = channel,
                            playlistId = playlistId,
                            onResolved = { url -> onChannelClick(url, channel.name) },
                            onError = onError
                        )
                    },
                    modifier = Modifier.animateItemPlacement()
                )
            }
        }
    }
}

@Composable
fun MovieCard(channel: ChannelEntity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // Fake rating for aesthetics (matching iMPlayer screenshots 0,0, 8,0, etc)
    val fakeRating = remember { listOf("8,0", "5,3", "6,6", "0,0", "7,2").random() }

    Card(
        modifier = modifier
            .width(130.dp)
            .aspectRatio(2f / 3f)
            .bounceClick(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!channel.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = channel.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Rating Badge (Top Left)
            Surface(
                color = AuraCyan,
                shape = RoundedCornerShape(bottomEnd = 12.dp),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = fakeRating,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StreamBoxLiveRow(
    title: String,
    channels: List<ChannelEntity>,
    viewModel: MainViewModel,
    playlistId: Int,
    onChannelClick: (String, String) -> Unit,
    onError: (String) -> Unit,
    onShowAllClick: (() -> Unit)? = null
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            if (onShowAllClick != null) {
                Text(stringResource(R.string.see_all), color = AuraPurple, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onShowAllClick))
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(channels, key = { it.id }) { channel ->
                Card(
                    modifier = Modifier
                        .width(160.dp)
                        .height(100.dp)
                        .animateItemPlacement()
                        .bounceClick {
                            viewModel.getStreamUrl(
                                channel = channel,
                                playlistId = playlistId,
                                onResolved = { url -> onChannelClick(url, channel.name) },
                                onError = onError
                            )
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (!channel.logoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = channel.logoUrl,
                                contentDescription = channel.name,
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text(
                                text = channel.name,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(8.dp),
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}
