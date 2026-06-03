package com.antigravity.iptv.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.antigravity.iptv.ui.MainViewModel
import com.antigravity.iptv.ui.theme.AuraPurple
import com.antigravity.iptv.ui.theme.AuraCyan
import com.antigravity.iptv.ui.theme.bounceClick
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import kotlinx.coroutines.delay

import androidx.compose.ui.res.stringResource
import com.antigravity.iptv.R

data class SettingsItemData(
    val titleRes: Int,
    val subtitleRes: Int? = null,
    val icon: ImageVector? = null,
    val isHeader: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onManagePlaylists: () -> Unit,
    onLanguageSettingsClick: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activePlaylist by viewModel.activePlaylist.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    
    // Dialog states
    var showPlaceholderDialog by remember { mutableStateOf<String?>(null) }
    var showDeviceIdDialog by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    
    val deviceId = remember {
        context.getSharedPreferences("aura_tv_prefs", Context.MODE_PRIVATE)
            .getString("device_id", null)
            ?: java.util.UUID.randomUUID().toString().take(20).also { id ->
                context.getSharedPreferences("aura_tv_prefs", Context.MODE_PRIVATE)
                    .edit().putString("device_id", id).apply()
            }
    }

    val settingsItems = listOf(
        SettingsItemData(R.string.settings_sync, icon = Icons.Outlined.CloudSync),
        SettingsItemData(R.string.settings_device_id, subtitleRes = R.string.settings_device_id_subtitle, icon = Icons.Outlined.Share),
        SettingsItemData(R.string.settings_manage_playlists, icon = Icons.Outlined.PlaylistPlay),
        SettingsItemData(R.string.settings_recordings, icon = Icons.Outlined.SaveAlt),
        SettingsItemData(R.string.settings_trakt, icon = Icons.Outlined.CheckCircle),
        
        SettingsItemData(R.string.settings_config, subtitleRes = R.string.settings_config_subtitle, icon = Icons.Outlined.Settings),
        SettingsItemData(R.string.settings_support, icon = Icons.Outlined.ChatBubbleOutline),
        
        SettingsItemData(R.string.settings_guide, icon = Icons.Outlined.MenuBook),
        SettingsItemData(R.string.settings_about, icon = Icons.Outlined.Info),
        SettingsItemData(R.string.settings_privacy, icon = Icons.Outlined.Lock),
        SettingsItemData(R.string.settings_terms, icon = Icons.Outlined.Description)
    )

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
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        var showCards by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { delay(150); showCards = true }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                AnimatedVisibility(
                    visible = showCards,
                    enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 3 }
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Aura", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, fontSize = 32.sp)
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AuraCyan, modifier = Modifier.size(32.dp))
                                Text("TV", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, fontSize = 32.sp)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Version gratuite", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Activer Premium", color = AuraCyan, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { 
                                Toast.makeText(context, "Premium sera bientôt disponible", Toast.LENGTH_SHORT).show()
                            })
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column {
                        settingsItems.take(5).forEachIndexed { index, item ->
                            SettingsRow(item) {
                                when (item.titleRes) {
                                    R.string.settings_sync -> {
                                        if (activePlaylist != null) {
                                            viewModel.syncPlaylist(activePlaylist!!)
                                            Toast.makeText(context, context.getString(R.string.settings_sync_started), Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, context.getString(R.string.settings_no_active_playlist), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    R.string.settings_device_id -> showDeviceIdDialog = true
                                    R.string.settings_manage_playlists -> onManagePlaylists()
                                    R.string.settings_recordings -> {
                                        Toast.makeText(context, context.getString(R.string.settings_no_recordings), Toast.LENGTH_SHORT).show()
                                    }
                                    R.string.settings_trakt -> {
                                        Toast.makeText(context, context.getString(R.string.settings_trakt_soon), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            if (index < 4) Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column {
                        settingsItems.subList(5, 7).forEachIndexed { index, item ->
                            SettingsRow(item) {
                                when (item.titleRes) {
                                    R.string.settings_config -> showConfigDialog = true
                                    R.string.settings_support -> showSupportDialog = true
                                }
                            }
                            if (index < 1) Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column {
                        settingsItems.drop(7).forEachIndexed { index, item ->
                            SettingsRow(item) {
                                when (item.titleRes) {
                                    R.string.settings_guide -> showGuideDialog = true
                                    R.string.settings_about -> showAboutDialog = true
                                    R.string.settings_privacy -> showPrivacyDialog = true
                                    R.string.settings_terms -> showTermsDialog = true
                                }
                            }
                            if (index < 3) Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                Text(
                    text = "Device ID: $deviceId",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ===== DIALOGS =====
        
        // Device ID Dialog
        if (showDeviceIdDialog) {
            AlertDialog(
                onDismissRequest = { showDeviceIdDialog = false },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                title = { Text(stringResource(R.string.settings_device_id), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(stringResource(R.string.settings_your_device_id), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = deviceId,
                                color = AuraCyan,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Device ID", deviceId))
                        Toast.makeText(context, context.getString(R.string.settings_id_copied), Toast.LENGTH_SHORT).show()
                        showDeviceIdDialog = false
                    }) { Text(stringResource(R.string.copy), color = AuraCyan) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeviceIdDialog = false }) { Text(stringResource(R.string.close), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            )
        }
        
        // Configurations Dialog (Theme toggle)
        if (showConfigDialog) {
            AlertDialog(
                onDismissRequest = { showConfigDialog = false },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                title = { Text(stringResource(R.string.settings_config), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.settings_dark_mode), color = MaterialTheme.colorScheme.onBackground)
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { viewModel.toggleTheme() },
                                colors = SwitchDefaults.colors(checkedTrackColor = AuraCyan)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                showConfigDialog = false
                                onLanguageSettingsClick()
                            }.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.settings_language), color = MaterialTheme.colorScheme.onBackground)
                            Icon(Icons.Outlined.Language, contentDescription = null, tint = AuraCyan)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showConfigDialog = false }) { Text("OK", color = AuraCyan) }
                }
            )
        }
        
        // Support Dialog
        if (showSupportDialog) {
            AlertDialog(
                onDismissRequest = { showSupportDialog = false },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                title = { Text("Support et commentaires", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Besoin d'aide ? Contactez-nous :", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("📧 support@auratv.app", color = AuraCyan, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Nous répondrons dans les 24 heures.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSupportDialog = false }) { Text("OK", color = AuraCyan) }
                }
            )
        }
        
        // Guide Dialog
        if (showGuideDialog) {
            AlertDialog(
                onDismissRequest = { showGuideDialog = false },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                title = { Text(stringResource(R.string.settings_quick_guide), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        GuideStep("1", stringResource(R.string.settings_guide_step1))
                        GuideStep("2", stringResource(R.string.settings_guide_step2))
                        GuideStep("3", stringResource(R.string.settings_guide_step3))
                        GuideStep("4", stringResource(R.string.settings_guide_step4))
                        GuideStep("5", stringResource(R.string.settings_guide_step5))
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showGuideDialog = false }) { Text(stringResource(R.string.got_it), color = AuraCyan) }
                }
            )
        }
        
        // About Dialog
        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Aura", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Icon(Icons.Default.PlayArrow, null, tint = AuraCyan, modifier = Modifier.size(20.dp))
                        Text("TV", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }
                },
                text = {
                    Column {
                        Text("Version 1.0.0", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.settings_about_desc), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(stringResource(R.string.settings_copyright), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAboutDialog = false }) { Text("OK", color = AuraCyan) }
                }
            )
        }
        
        // Privacy Policy Dialog
        if (showPrivacyDialog) {
            AlertDialog(
                onDismissRequest = { showPrivacyDialog = false },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                title = { Text(stringResource(R.string.settings_privacy), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            stringResource(R.string.settings_privacy_text),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPrivacyDialog = false }) { Text("OK", color = AuraCyan) }
                }
            )
        }
        
        // Terms Dialog
        if (showTermsDialog) {
            AlertDialog(
                onDismissRequest = { showTermsDialog = false },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                title = { Text(stringResource(R.string.settings_terms), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            stringResource(R.string.settings_terms_text),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTermsDialog = false }) { Text("OK", color = AuraCyan) }
                }
            )
        }
    }
}

@Composable
private fun GuideStep(number: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(AuraCyan, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(number, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
    }
}

@Composable
fun SettingsRow(item: SettingsItemData, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.icon != null) {
            Icon(
                item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column {
            Text(
                text = stringResource(id = item.titleRes),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
            if (item.subtitleRes != null) {
                Text(
                    text = stringResource(id = item.subtitleRes),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}
