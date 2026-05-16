package com.streambox.tv.ui.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.streambox.tv.nav.Routes
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
fun SettingsScreen(nav: NavHostController) {
    Column(
        modifier = Modifier.fillMaxSize().background(Bg900).verticalScroll(rememberScrollState()).padding(24.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Spacer(Modifier.height(20.dp))

        SettingsGroup("Account & sources") {
            SettingsRow(Icons.Default.AccountCircle, "Manage providers", "M3U, Xtream Codes & Stalker portals", onClick = { nav.navigate(Routes.Providers) })
        }

        SettingsGroup("Appearance") {
            SettingsRow(Icons.Default.Brightness4, "Theme mode", "Dark — high contrast", onClick = {})
        }

        SettingsGroup("Player") {
            SettingsRow(Icons.Default.Tune, "Player settings", "Buffer, hardware decoding, default audio", onClick = {})
            SettingsRow(Icons.Default.Subtitles, "Subtitle settings", "Size, color, background, language", onClick = {})
            SettingsRow(Icons.Default.OpenInNew, "External player", "Use VLC / MX Player when available", onClick = {})
        }

        SettingsGroup("Privacy & maintenance") {
            SettingsRow(Icons.Default.Lock, "Parental control", "Restrict adult categories with a PIN", onClick = {})
            SettingsRow(Icons.Default.CleaningServices, "Clear cache", "Free up logos, posters and EPG cache", onClick = {})
        }

        SettingsGroup("About") {
            SettingsRow(Icons.Default.Info, "About StreamBox TV", "v1.0.0 · build 1", onClick = {})
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)) {
        Text(title.uppercase(), color = TextMuted, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Bg700, shape)
            .border(1.dp, if (focused) FocusRing else GlassStroke, shape)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(TealGlow, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = Teal400) }
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        }
        Icon(Icons.Default.ChevronRight, null, tint = TextMuted)
    }
    Spacer(Modifier.height(8.dp))
}
