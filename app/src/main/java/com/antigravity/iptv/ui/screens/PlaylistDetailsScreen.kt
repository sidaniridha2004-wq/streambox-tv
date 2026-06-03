package com.antigravity.iptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.foundation.ExperimentalFoundationApi
import com.antigravity.iptv.data.local.entity.ChannelEntity
import com.antigravity.iptv.ui.MainViewModel
import com.antigravity.iptv.ui.theme.*

import kotlinx.coroutines.flow.map
import androidx.compose.ui.res.stringResource
import com.antigravity.iptv.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlaylistDetailsScreen(
    playlistId: Int,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onChannelClick: (String, String) -> Unit
) {
    // Only show categories from live channels (not movies/series from VOD)
    val allCategories by viewModel.getCategoriesBySourceType(playlistId, "live").collectAsState(initial = emptyList())
    val categories = remember(allCategories) {
        allCategories.sorted()
    }
    
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var showOnlyFavorites by rememberSaveable { mutableStateOf(false) }
    var showWorldCup by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // World Cup broadcaster channel name patterns
    val worldCupKeywords = remember {
        listOf("bein", "fifa", "world cup", "coupe du monde", "fox sport", "itv", "bbc sport",
            "trt spor", "rai sport", "zdf", "ard", "telemundo", "univision", "tsn",
            "sportsnet", "supersport", "astro", "sport 1", "sport 2", "sport 3",
            "al kass", "alkass")
    }

    // Filter channels — only live channels (beIN sorting done at SQL level)
    val displayedChannels by remember(selectedCategory, showOnlyFavorites, showWorldCup, searchQuery, playlistId) {
        when {
            searchQuery.isNotBlank() -> viewModel.searchChannels(playlistId, searchQuery).map { list ->
                list.filter { it.sourceType == "live" }
            }
            showOnlyFavorites -> viewModel.getLiveTv(playlistId).map { list -> list.filter { it.isFavorite } }
            showWorldCup -> viewModel.getLiveTv(playlistId).map { list ->
                list.filter { ch ->
                    val nameLower = ch.name.lowercase()
                    val groupLower = ch.groupName.lowercase()
                    worldCupKeywords.any { kw -> nameLower.contains(kw) || groupLower.contains(kw) }
                }
            }
            selectedCategory != null -> viewModel.getChannelsByCategoryAndType(playlistId, selectedCategory!!, "live")
            else -> viewModel.getLiveTv(playlistId)
        }
    }.collectAsState(initial = emptyList())

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(R.string.search_channels), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Column {
                            Text(stringResource(R.string.live_tv_title), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (selectedCategory != null) selectedCategory!! else stringResource(R.string.all_channels),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSearchActive) {
                            isSearchActive = false
                            searchQuery = ""
                        } else {
                            // Don't show back button if this is a main tab
                        }
                    }) {
                        if (isSearchActive) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onBackground)
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Category filter chips
            if (!isSearchActive && categories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // All chip
                    item {
                        FilterChip(
                            selected = selectedCategory == null && !showOnlyFavorites && !showWorldCup,
                            onClick = { selectedCategory = null; showOnlyFavorites = false; showWorldCup = false },
                            label = { Text(stringResource(R.string.filter_all_short)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = Color.Transparent,
                                selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        )
                    }
                    // Favorites chip
                    item {
                        FilterChip(
                            selected = showOnlyFavorites,
                            onClick = { showOnlyFavorites = !showOnlyFavorites; if (showOnlyFavorites) { selectedCategory = null; showWorldCup = false } },
                            label = { Text(stringResource(R.string.filter_favorites)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.error,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = Color.Transparent,
                                selectedBorderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                            )
                        )
                    }
                    // World Cup chip
                    item {
                        FilterChip(
                            selected = showWorldCup,
                            onClick = { showWorldCup = !showWorldCup; if (showWorldCup) { selectedCategory = null; showOnlyFavorites = false } },
                            label = { Text(stringResource(R.string.filter_world_cup)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF4CAF50).copy(alpha = 0.25f),
                                selectedLabelColor = Color(0xFF4CAF50),
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = Color.Transparent,
                                selectedBorderColor = Color(0xFF4CAF50).copy(alpha = 0.3f)
                            )
                        )
                    }
                    // Category chips
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = {
                                selectedCategory = if (selectedCategory == category) null else category
                                showOnlyFavorites = false
                                showWorldCup = false
                            },
                            label = {
                                Text(
                                    category,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = Color.Transparent,
                                selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            // Channel list
            if (displayedChannels.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when {
                            showOnlyFavorites -> stringResource(R.string.no_favorites_yet)
                            showWorldCup -> stringResource(R.string.no_world_cup_channels)
                            searchQuery.isNotBlank() -> "No results for \"$searchQuery\""
                            else -> stringResource(R.string.no_channels_found)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(
                        count = displayedChannels.size,
                        key = { displayedChannels[it].id }
                    ) { index ->
                        val channel = displayedChannels[index]
                        ChannelItem(
                            channel = channel,
                            onChannelClick = {
                                viewModel.getStreamUrl(
                                    channel = channel,
                                    playlistId = playlistId,
                                    onResolved = { url -> onChannelClick(url, channel.name) },
                                    onError = { errorMsg = it }
                                )
                            },
                            onFavoriteClick = { viewModel.toggleFavorite(channel) },
                            modifier = Modifier.animateItemPlacement()
                        )
                    }
                }
            }
        }

        // Loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        // Error dialog
        if (errorMsg != null) {
            AlertDialog(
                onDismissRequest = { errorMsg = null },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text("Error", color = MaterialTheme.colorScheme.onBackground) },
                text = { Text(errorMsg!!, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                confirmButton = {
                    TextButton(onClick = { errorMsg = null }) {
                        Text("OK", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    }
}

@Composable
private fun ChannelItem(
    channel: ChannelEntity,
    onChannelClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .bounceClick(onClick = onChannelClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Channel logo
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!channel.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = channel.name.take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (channel.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Live",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = channel.groupName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
