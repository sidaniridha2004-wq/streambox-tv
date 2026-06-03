package com.antigravity.iptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.iptv.domain.model.PlaylistType
import com.antigravity.iptv.ui.MainViewModel
import com.antigravity.iptv.ui.theme.AuraCyan
import com.antigravity.iptv.ui.theme.AuraPurple
import androidx.compose.ui.res.stringResource
import com.antigravity.iptv.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlaylistScreen(
    playlistId: Int = -1,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val isEditMode = playlistId != -1
    val playlists by viewModel.playlists.collectAsState()
    val editingPlaylist = remember(playlists, playlistId) { playlists.find { it.id == playlistId } }

    var selectedType by remember { mutableStateOf("") }
    var showTypeSelector by remember { mutableStateOf(!isEditMode) }

    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var macAddress by remember { mutableStateOf("") }

    val context = androidx.compose.ui.platform.LocalContext.current
    var deviceId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val prefs = context.getSharedPreferences("aura_prefs", android.content.Context.MODE_PRIVATE)
            var id = prefs.getString("device_id", null)
            if (id == null) {
                id = java.util.UUID.randomUUID().toString().replace("-", "").take(16).uppercase()
                prefs.edit().putString("device_id", id).apply()
            }
            deviceId = id
        }
    }

    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            url = uri.toString()
        }
    }

    val typeM3u = stringResource(R.string.type_m3u)
    val typeM3uLocal = stringResource(R.string.type_m3u_local)
    val typeStalker = stringResource(R.string.type_stalker)

    LaunchedEffect(Unit) {
        if (selectedType.isEmpty()) {
            selectedType = typeM3u
        }
    }

    LaunchedEffect(editingPlaylist) {
        if (editingPlaylist != null) {
            name = editingPlaylist.name
            url = editingPlaylist.url ?: ""
            macAddress = editingPlaylist.macAddress ?: ""
            selectedType = if (editingPlaylist.type == PlaylistType.M3U) typeM3u else typeStalker
        }
    }

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.errorMsg.collectAsState()
    val progressLogs by viewModel.progressLogs.collectAsState()
    var showProgressUI by remember { mutableStateOf(false) }
    var isDone by remember { mutableStateOf(false) }

    if (showProgressUI) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.95f)).padding(16.dp)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header with logo
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 32.dp)) {
                    Text("Aura ", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 24.sp)
                    Text("TV", color = AuraPurple, fontSize = 16.sp, modifier = Modifier.background(Color.White, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp))
                }

                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(progressLogs) { log ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(log, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                if (isDone) {
                    Button(
                        onClick = onSuccess,
                        colors = ButtonDefaults.buttonColors(containerColor = AuraCyan),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth(0.5f).height(48.dp)
                    ) {
                        Text(stringResource(R.string.finish), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (isLoading) {
                    CircularProgressIndicator(color = AuraCyan)
                }
                
                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showProgressUI = false; isDone = false }) {
                        Text(stringResource(R.string.close))
                    }
                }
            }
        }
        return // Do not render the rest of the screen if progress is showing
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditMode) stringResource(R.string.edit_playlist)
                        else if (showTypeSelector) stringResource(R.string.choose_type)
                        else stringResource(R.string.add_playlist),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            if (!showTypeSelector) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = AuraCyan),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(stringResource(R.string.back), color = Color.White, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            showProgressUI = true
                            isDone = false
                            val callback = { isDone = true }
                            if (isEditMode) {
                                if (selectedType == typeM3u || selectedType == typeM3uLocal) {
                                    viewModel.updateM3uPlaylist(playlistId, name, url, callback)
                                } else {
                                    viewModel.updateStalkerPlaylist(playlistId, name, url, macAddress.ifEmpty { "00:1A:79:00:00:00" }, callback)
                                }
                            } else {
                                if (selectedType == typeM3u || selectedType == typeM3uLocal) {
                                    viewModel.addM3uPlaylist(name, url, callback)
                                } else {
                                    viewModel.addStalkerPlaylist(name, url, macAddress.ifEmpty { "00:1A:79:00:00:00" }, callback)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AuraCyan),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f).height(48.dp),
                        enabled = name.isNotBlank() && url.isNotBlank() && !isLoading && (selectedType == typeM3u || selectedType == typeM3uLocal || macAddress.isNotBlank())
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text(if (isEditMode) stringResource(R.string.save) else stringResource(R.string.next), color = Color.White, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (showTypeSelector) {
            // Inline type selector — smooth, no heavy ModalBottomSheet
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.choose_playlist_type),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                val options = listOf(
                    typeM3u to Icons.Outlined.Description,
                    typeM3uLocal to Icons.Outlined.Description,
                    typeStalker to Icons.Outlined.Router
                )

                options.forEach { (title, icon) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                selectedType = title
                                showTypeSelector = false
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, contentDescription = null, tint = AuraCyan)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    stringResource(R.string.playlist_name_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                val textFieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    cursorColor = AuraCyan,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.playlist_name_label), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedType == typeM3u || selectedType == typeM3uLocal) {
                    if (selectedType == typeM3uLocal) {
                        OutlinedTextField(
                            value = url.takeIf { it.isNotBlank() } ?: stringResource(R.string.no_file_selected),
                            onValueChange = { },
                            readOnly = true,
                            label = { Text(stringResource(R.string.m3u_file_label), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = textFieldColors,
                            trailingIcon = {
                                IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                                    Icon(Icons.Outlined.Description, contentDescription = stringResource(R.string.choose_file))
                                }
                            }
                        )
                    } else {
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            label = { Text(stringResource(R.string.m3u_link_label), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = textFieldColors
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = macAddress,
                        onValueChange = { macAddress = it },
                        label = { Text(stringResource(R.string.epg_link_label), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = textFieldColors
                    )
                } else {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text(stringResource(R.string.portal_url_label), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = textFieldColors
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = macAddress,
                        onValueChange = { macAddress = it },
                        label = { Text(stringResource(R.string.mac_address_label), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = textFieldColors
                    )
                }

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.device_id_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(deviceId ?: "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = com.antigravity.iptv.ui.theme.AuraCyan)
                }
            }
        }
    }
}
