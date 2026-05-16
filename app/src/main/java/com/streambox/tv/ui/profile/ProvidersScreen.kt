package com.streambox.tv.ui.profile

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.streambox.tv.data.Provider
import com.streambox.tv.data.ProviderStatus
import com.streambox.tv.data.ProviderType
import com.streambox.tv.nav.Routes
import com.streambox.tv.ui.components.PrimaryButton
import com.streambox.tv.ui.components.SectionHeader
import com.streambox.tv.ui.components.StatusPill
import com.streambox.tv.ui.theme.Amber500
import com.streambox.tv.ui.theme.Bg700
import com.streambox.tv.ui.theme.Bg900
import com.streambox.tv.ui.theme.FocusRing
import com.streambox.tv.ui.theme.GlassStroke
import com.streambox.tv.ui.theme.Green500
import com.streambox.tv.ui.theme.Red500
import com.streambox.tv.ui.theme.Teal400
import com.streambox.tv.ui.theme.TealGlow
import com.streambox.tv.ui.theme.TextMuted
import com.streambox.tv.ui.theme.TextPrimary
import com.streambox.tv.ui.theme.TextSecondary

@Composable
fun ProvidersScreen(nav: NavHostController, vm: ProvidersViewModel = hiltViewModel()) {
    val providers by vm.providers.collectAsStateWithLifecycle()
    val activeId by vm.activeProviderId.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(Bg900)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Providers", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Text("Manage your IPTV accounts and playlists.", color = TextMuted, style = MaterialTheme.typography.labelMedium)
            }
            PrimaryButton(text = "Add provider", leadingIcon = Icons.Default.Add, onClick = { nav.navigate(Routes.LoginChooser) })
        }

        SectionHeader("Connected accounts")
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        ) {
            items(providers, key = { it.id }) { p ->
                ProviderCard(
                    provider = p,
                    isActive = p.id == activeId,
                    onActivate = { vm.setActive(p.id) },
                    onDelete = { vm.delete(p.id) },
                )
            }
        }
    }
}

@Composable
private fun ProviderCard(provider: Provider, isActive: Boolean, onActivate: () -> Unit, onDelete: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val shape = RoundedCornerShape(18.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .background(Bg700, shape)
            .border(1.dp, if (focused) FocusRing else if (isActive) Teal400 else GlassStroke, shape)
            .clickable(interactionSource = src, indication = null, onClick = onActivate)
            .padding(horizontal = 16.dp),
    ) {
        Box(
            modifier = Modifier.size(56.dp).background(TealGlow, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(provider.name.take(1).uppercase(), color = Teal400, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(provider.name, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                StatusPill(typeLabel(provider.type), color = Teal400)
                StatusPill(provider.status.name, color = providerColor(provider.status))
                if (isActive) StatusPill("ACTIVE", color = Green500)
            }
            Spacer(Modifier.size(4.dp))
            Text(provider.endpoint, color = TextSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            Spacer(Modifier.size(2.dp))
            Text(
                "${provider.channelCount} channels · ${provider.movieCount} movies · ${provider.seriesCount} series · ${provider.lastSync}",
                color = TextMuted,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        ActionIcon(Icons.Default.Sync) {}
        Spacer(Modifier.size(8.dp))
        ActionIcon(Icons.Default.Edit) {}
        Spacer(Modifier.size(8.dp))
        ActionIcon(Icons.Default.Delete, tint = Red500, onClick = onDelete)
    }
}

@Composable
private fun ActionIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color = TextMuted, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(Bg900, RoundedCornerShape(10.dp))
            .border(1.dp, if (focused) FocusRing else GlassStroke, RoundedCornerShape(10.dp))
            .clickable(interactionSource = src, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint) }
}

private fun typeLabel(type: ProviderType) = when (type) {
    ProviderType.M3U -> "M3U"
    ProviderType.XTREAM -> "XTREAM"
    ProviderType.STALKER -> "STALKER"
}

private fun providerColor(s: ProviderStatus) = when (s) {
    ProviderStatus.OK -> Green500
    ProviderStatus.SYNCING -> Teal400
    ProviderStatus.EXPIRED -> Amber500
    ProviderStatus.FAILED -> Red500
}
