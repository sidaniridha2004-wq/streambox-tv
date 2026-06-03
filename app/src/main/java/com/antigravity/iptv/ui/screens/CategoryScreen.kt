package com.antigravity.iptv.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.antigravity.iptv.data.local.entity.ChannelEntity
import com.antigravity.iptv.ui.MainViewModel
import com.antigravity.iptv.ui.theme.AuraPurple
import com.antigravity.iptv.ui.theme.AuraCyan
import com.antigravity.iptv.ui.theme.bounceClick
import androidx.compose.ui.res.stringResource
import com.antigravity.iptv.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategoryScreen(
    viewModel: MainViewModel,
    categoryFilter: String,
    title: String,
    onChannelClick: (String, String) -> Unit
) {
    val activePlaylist by viewModel.activePlaylist.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }

    val vodChannels by remember(activePlaylist, categoryFilter) {
        if (activePlaylist == null) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            when (categoryFilter) {
                "movies" -> viewModel.getMovies(activePlaylist!!.id)
                "series" -> viewModel.getSeries(activePlaylist!!.id)
                "sports" -> viewModel.getSportsChannels(activePlaylist!!.id)
                else -> kotlinx.coroutines.flow.flowOf(emptyList())
            }
        }
    }.collectAsState(initial = emptyList())

    val recentChannels by viewModel.activeRecentChannels.collectAsState(initial = emptyList())
    val continueWatching = remember(recentChannels, categoryFilter) {
        recentChannels.filter { 
            (it.channel.sourceType == categoryFilter) && 
            (it.progressMs > 0 && it.progressMs < it.durationMs - 10000L)
        }
    }

    val strAll = stringResource(R.string.all)
    val strFavorites = stringResource(R.string.favorites)

    val filters = remember(vodChannels, strAll, strFavorites) {
        val groups = vodChannels.map { it.groupName }.filter { it.isNotBlank() }.distinct().sorted()
        listOf(strAll, strFavorites) + groups
    }
    var selectedFilter by rememberSaveable(strAll) { mutableStateOf(strAll) }
    var showAllGrid by rememberSaveable { mutableStateOf(false) }
    
    var selectedSeries by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSeason by rememberSaveable { mutableStateOf<String?>(null) }

    val filteredChannels = remember(vodChannels, selectedFilter, strAll, strFavorites) {
        when (selectedFilter) {
            strAll -> vodChannels
            strFavorites -> vodChannels.filter { it.isFavorite }
            else -> vodChannels.filter { it.groupName == selectedFilter }
        }
    }

    // Compute series grouping on background thread to avoid UI freeze
    val seriesData by produceState<Map<String, Map<String, List<ChannelEntity>>>?>(
        initialValue = null,
        key1 = filteredChannels,
        key2 = categoryFilter
    ) {
        if (categoryFilter == "series" && filteredChannels.isNotEmpty()) {
            value = null // Show loading
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                val patterns = listOf(
                    Regex("""^(.*?)\s*[Ss](\d+)\s*[Ee](\d+)"""),
                    Regex("""^(.*?)\s*[Ss]eason\s*(\d+)\s*[Ee]pisode\s*(\d+)""", RegexOption.IGNORE_CASE),
                    Regex("""^(.*?)\s*[Ss]aison\s*(\d+)\s*[Ee]pisode\s*(\d+)""", RegexOption.IGNORE_CASE),
                    Regex("""^(.*?)\s*[Ss](\d+)\s*[Ee]p?\s*(\d+)"""),
                    Regex("""^(.*?)\s*-\s*[Ss](\d+)[Ee](\d+)"""),
                )
                val map = mutableMapOf<String, MutableMap<String, MutableList<ChannelEntity>>>()
                val prefixCleanRegex = Regex("""^[\w]{2}\s*[\|│]\s*""")
                val seasonSortRegex = Regex("""(\d+)""")
                val episodeSortRegex = Regex("""[Ee](?:p(?:isode)?)?\s*(\d+)""")
                
                filteredChannels.forEach { channel ->
                    var matched = false
                    for (regex in patterns) {
                        val match = regex.find(channel.name)
                        if (match != null) {
                            val seriesName = match.groupValues[1].trim()
                                .removeSuffix("-").removeSuffix("–").trim()
                            val seasonNum = match.groupValues[2].toIntOrNull() ?: 1
                            val season = "Season $seasonNum"
                            map.getOrPut(seriesName) { mutableMapOf() }
                                .getOrPut(season) { mutableListOf() }
                                .add(channel)
                            matched = true
                            break
                        }
                    }
                    if (!matched) {
                        val seriesName = channel.groupName
                            .replace(prefixCleanRegex, "")
                            .trim()
                            .ifBlank { "Other Series" }
                        map.getOrPut(seriesName) { mutableMapOf() }
                            .getOrPut("Episodes") { mutableListOf() }
                            .add(channel)
                    }
                }
                
                // Sort seasons numerically and episodes by name
                map.mapValues { (_, seasons) ->
                    seasons.toSortedMap(compareBy { 
                        seasonSortRegex.find(it)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    }).mapValues { (_, episodes) ->
                        episodes.sortedBy { ep ->
                            episodeSortRegex.find(ep.name)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                        }
                    }
                }
            }
        } else if (categoryFilter != "series") {
            value = null
        }
    }
    
    val isSeriesLoading = categoryFilter == "series" && seriesData == null && filteredChannels.isNotEmpty()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(R.string.search_hint), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                cursorColor = AuraCyan,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Aura ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            Text("TV", color = AuraPurple, fontSize = 12.sp, modifier = Modifier.background(Color.White, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                },
                navigationIcon = {
                    if (isSearchActive) {
                        IconButton(onClick = { isSearchActive = false; searchQuery = "" }) {
                            Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, "Search", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (activePlaylist == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AuraCyan)
            }
        } else if (vodChannels.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_content_found, title), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (isSearchActive && searchQuery.isNotBlank()) {
            val results = vodChannels.filter { it.name.contains(searchQuery, ignoreCase = true) }
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(results, key = { it.id }) { channel ->
                    Text(
                        text = channel.name,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick {
                                viewModel.getStreamUrl(
                                    channel = channel,
                                    playlistId = activePlaylist!!.id,
                                    onResolved = { url -> onChannelClick(url, channel.name) },
                                    onError = { errorMsg = it }
                                )
                            }
                            .padding(vertical = 12.dp)
                            .animateItemPlacement()
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
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
                            modifier = Modifier.bounceClick { selectedFilter = filter }
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

                if (isSeriesLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            CircularProgressIndicator(color = AuraCyan, modifier = Modifier.size(40.dp), strokeWidth = 3.dp)
                            Text(stringResource(R.string.loading_series), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        }
                    }
                } else if (seriesData != null) {
                    val series = seriesData!!
                    if (selectedSeries == null) {
                        // Display Series List (Grid of Series)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${stringResource(R.string.all_series)} (${series.keys.size})", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(130.dp),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(series.keys.toList().sortedBy { it.lowercase() }) { sName ->
                                val seasons = series[sName] ?: emptyMap()
                                val totalEpisodes = seasons.values.sumOf { it.size }
                                val firstEpi = seasons.values.firstOrNull()?.firstOrNull()
                                Card(
                                    modifier = Modifier.width(130.dp).aspectRatio(2f/3f).bounceClick {
                                        selectedSeries = sName
                                        selectedSeason = seasons.keys.firstOrNull()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        if (firstEpi?.logoUrl != null) {
                                            coil.compose.AsyncImage(model = firstEpi.logoUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        }
                                        // Gradient overlay
                                        Box(modifier = Modifier.fillMaxSize().background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color(0xFF06091A).copy(alpha = 0.85f)),
                                                startY = 80f
                                            )
                                        ))
                                        // Episode count badge
                                        Surface(
                                            color = AuraCyan.copy(alpha = 0.9f),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                                        ) {
                                            Text(
                                                text = "${seasons.size}S · ${totalEpisodes}E",
                                                color = Color.Black,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                        // Series name at bottom
                                        Text(
                                            text = sName,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 2,
                                            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                                            textAlign = TextAlign.Start
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Display Seasons and Episodes
                        val seasons = series[selectedSeries!!] ?: emptyMap()
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { selectedSeries = null }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground) }
                                Text(selectedSeries!!, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                            }
                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(seasons.keys.toList()) { seasonName ->
                                    val isSel = seasonName == selectedSeason
                                    Surface(
                                        color = if (isSel) com.antigravity.iptv.ui.theme.AuraCyan else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.bounceClick { selectedSeason = seasonName }
                                    ) {
                                        Text(seasonName, color = if (isSel) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            val episodes = seasons[selectedSeason] ?: emptyList()
                            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                                items(episodes, key = { it.id }) { epi ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().bounceClick {
                                            viewModel.getStreamUrl(epi, activePlaylist!!.id, { url -> onChannelClick(url, epi.name) }, { errorMsg = it })
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            if (!epi.logoUrl.isNullOrBlank()) {
                                                coil.compose.AsyncImage(model = epi.logoUrl, contentDescription = null, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                                Spacer(modifier = Modifier.width(16.dp))
                                            }
                                            Text(epi.name, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (showAllGrid) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${stringResource(R.string.all_content, title)} (${filteredChannels.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Text(stringResource(R.string.back), color = AuraPurple, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { showAllGrid = false })
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(130.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredChannels, key = { it.id }) { channel ->
                            MovieCard(
                                channel = channel,
                                modifier = Modifier.animateItemPlacement(),
                                onClick = {
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
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                    item {
                        val heroChannel = filteredChannels.firstOrNull()
                        if (heroChannel != null) {
                            CategoryHeroBanner(
                                channel = heroChannel,
                                onPlayClick = {
                                    viewModel.getStreamUrl(
                                        channel = heroChannel,
                                        playlistId = activePlaylist!!.id,
                                        onResolved = { url -> onChannelClick(url, heroChannel.name) },
                                        onError = { errorMsg = it }
                                    )
                                }
                            )
                        }
                    }

                    if (continueWatching.isNotEmpty() && selectedFilter == strAll) {
                        item {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.continue_watching),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(continueWatching, key = { it.channel.id }) { item ->
                                        MovieCardWithProgress(
                                            channel = item.channel,
                                            progressMs = item.progressMs,
                                            durationMs = item.durationMs,
                                            onClick = {
                                                viewModel.getStreamUrl(
                                                    channel = item.channel,
                                                    playlistId = activePlaylist!!.id,
                                                    onResolved = { url -> onChannelClick(url, item.channel.name) },
                                                    onError = { errorMsg = it }
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${stringResource(R.string.all_content, title)} (${filteredChannels.size})",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(stringResource(R.string.see_all), color = AuraPurple, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { showAllGrid = true })
                            }
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredChannels.take(20), key = { it.id }) { channel ->
                                    MovieCard(
                                        channel = channel,
                                        onClick = {
                                            viewModel.getStreamUrl(
                                                channel = channel,
                                                playlistId = activePlaylist!!.id,
                                                onResolved = { url -> onChannelClick(url, channel.name) },
                                                onError = { errorMsg = it }
                                            )
                                        },
                                        modifier = Modifier.animateItemPlacement()
                                    )
                                }
                            }
                        }
                    }
                    }
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
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
fun MovieCardWithProgress(
    channel: ChannelEntity,
    progressMs: Long,
    durationMs: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (durationMs > 0) (progressMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    
    Card(
        modifier = modifier
            .width(130.dp)
            .aspectRatio(2f / 3f)
            .bounceClick(onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!channel.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(AuraPurple.copy(alpha = 0.5f), Color.Black)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SortByAlpha, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
                }
            }

            // Gradient at bottom for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 150f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
            ) {
                Text(
                    text = channel.name,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
                
                // Progress Bar
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = AuraCyan,
                    trackColor = Color.White.copy(alpha = 0.3f),
                )
            }
        }
    }
}

@Composable
fun CategoryHeroBanner(
    channel: ChannelEntity,
    onPlayClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val bannerHeight = configuration.screenHeightDp.dp * 0.45f

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("TM", color = AuraCyan, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Text("DB", color = AuraCyan, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.width(40.dp).height(12.dp).clip(RoundedCornerShape(6.dp)).background(AuraCyan))
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.Close, contentDescription = null, tint = AuraPurple, modifier = Modifier.background(AuraPurple.copy(alpha=0.2f), RoundedCornerShape(50)).padding(4.dp))
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(bannerHeight)
                .bounceClick(onPlayClick),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!channel.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                        )
                )

                // Large Number Overlay
                Text(
                    text = "1",
                    color = Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Black,
                    fontSize = 180.sp,
                    modifier = Modifier.align(Alignment.BottomStart).offset(y = 40.dp, x = (-10).dp)
                )
                
                // Title
                Text(
                    text = channel.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp).padding(horizontal = 40.dp)
                )

                // Dots
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(10) { index ->
                        Box(
                            modifier = Modifier
                                .size(if (index == 0) 8.dp else 6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (index == 0) Color.White else Color.White.copy(alpha = 0.4f))
                        )
                    }
                }
            }
        }
    }
}
