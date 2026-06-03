package com.antigravity.iptv.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.antigravity.iptv.data.local.entity.PlaylistEntity
import com.antigravity.iptv.ui.MainViewModel
import com.antigravity.iptv.ui.theme.AuraPurple
import com.antigravity.iptv.ui.theme.AuraCyan
import com.antigravity.iptv.ui.theme.bounceClick
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import com.antigravity.iptv.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistSelectionScreen(
    title: String = "Bienvenue sur Aura TV",
    playlists: List<PlaylistEntity>,
    onSelectPlaylist: (Int) -> Unit,
    onAddPlaylist: () -> Unit,
    onEditPlaylist: (Int) -> Unit,
    onDeletePlaylist: (PlaylistEntity) -> Unit,
    onClose: (() -> Unit)? = null
) {
    val deviceId = "mob38149c6d9b0fe9b7" // Placeholder for device ID

    if (playlists.isNotEmpty()) {
        // Staggered entrance animations for playlist list
        var showItems by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { delay(50); showItems = true }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                    navigationIcon = {
                        if (onClose != null) {
                            IconButton(onClick = onClose) {
                                Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onAddPlaylist,
                    containerColor = AuraCyan,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, "Add Playlist")
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                playlists.forEachIndexed { index, playlist ->
                    AnimatedVisibility(
                        visible = showItems,
                        enter = fadeIn(tween(400, delayMillis = index * 100)) +
                                slideInVertically(tween(400, delayMillis = index * 100)) { it / 2 }
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .bounceClick { onSelectPlaylist(playlist.id) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { onEditPlaylist(playlist.id) }) {
                                        Text(stringResource(R.string.edit), color = AuraCyan)
                                    }
                                    IconButton(onClick = { onDeletePlaylist(playlist) }) {
                                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // EMPTY STATE with staggered entrance
        var showWelcome by remember { mutableStateOf(false) }
        var showSubtext by remember { mutableStateOf(false) }
        var showButton by remember { mutableStateOf(false) }
        var showDeviceId by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            delay(50); showWelcome = true
            delay(80); showSubtext = true
            delay(80); showButton = true
            delay(80); showDeviceId = true
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Aura ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            Text("TV", fontWeight = FontWeight.Bold, color = AuraPurple)
                        }
                    },
                    navigationIcon = {
                        if (onClose != null) {
                            IconButton(onClick = onClose) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = showButton,
                    enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it }
                ) {
                    Button(
                        onClick = onAddPlaylist,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AuraCyan)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.add_playlist_btn), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedVisibility(
                    visible = showWelcome,
                    enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { -it / 3 }
                ) {
                    Text(
                        text = stringResource(R.string.welcome_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                AnimatedVisibility(
                    visible = showSubtext,
                    enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 3 }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.before_using_app),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        TextButton(onClick = onAddPlaylist) {
                            Text(stringResource(R.string.add_playlist_btn), color = AuraCyan, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                            Text(stringResource(R.string.or_separator), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                            Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = stringResource(R.string.login_manage_device),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { /* TODO: Login */ },
                            modifier = Modifier.height(48.dp).padding(horizontal = 32.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AuraCyan)
                        ) {
                            Text(stringResource(R.string.login_btn), color = Color.White, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(48.dp))

                AnimatedVisibility(
                    visible = showDeviceId,
                    enter = fadeIn(tween(800))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.no_playlists_found),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${stringResource(R.string.device_id_label)}: $deviceId",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Icon(
                                Icons.Default.ContentCopy, 
                                contentDescription = "Copy", 
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(20.dp).clickable { /* TODO: Copy to clipboard */ }
                            )
                        }
                    }
                }
            }
        }
    }
}
