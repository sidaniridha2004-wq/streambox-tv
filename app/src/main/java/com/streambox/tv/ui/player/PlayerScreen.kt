package com.streambox.tv.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.streambox.tv.data.Channel
import com.streambox.tv.nav.Routes
import com.streambox.tv.ui.components.ChannelLogo
import com.streambox.tv.ui.components.StatusPill
import com.streambox.tv.ui.theme.Bg700
import com.streambox.tv.ui.theme.Bg800
import com.streambox.tv.ui.theme.FocusRing
import com.streambox.tv.ui.theme.GlassStroke
import com.streambox.tv.ui.theme.Red500
import com.streambox.tv.ui.theme.Teal400
import com.streambox.tv.ui.theme.TealGlow
import com.streambox.tv.ui.theme.TextMuted
import com.streambox.tv.ui.theme.TextPrimary
import com.streambox.tv.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(nav: NavHostController, streamUrl: String, vm: PlayerViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var overlayVisible by remember { mutableStateOf(true) }
    var drawerOpen by remember { mutableStateOf(false) }
    var connectionPhase by remember { mutableStateOf(ConnectionPhase.Buffering) }

    // Simulate the connection lifecycle so the UI can demo all states. In the
    // real build these phases would map to ExoPlayer Player.Listener events.
    LaunchedEffect(streamUrl) {
        connectionPhase = ConnectionPhase.Buffering
        delay(900)
        connectionPhase = ConnectionPhase.Playing
    }

    // Auto-hide overlay after a few seconds when playing
    LaunchedEffect(connectionPhase, overlayVisible) {
        if (connectionPhase == ConnectionPhase.Playing && overlayVisible) {
            delay(4000)
            overlayVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { overlayVisible = !overlayVisible },
    ) {
        // Surface: a real implementation would inflate androidx.media3.ui.PlayerView
        // here via AndroidView. We render a stylized placeholder so the UI is
        // fully demoable without needing a live stream.
        VideoSurfacePlaceholder(state.currentChannel)

        when (connectionPhase) {
            ConnectionPhase.Buffering -> CenterStatus(text = "Buffering…", color = Teal400)
            ConnectionPhase.NoSignal -> CenterStatus(text = "No signal — tap to retry", color = Red500)
            ConnectionPhase.Playing -> Unit
        }

        AnimatedVisibility(
            visible = overlayVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) { TopOverlay(channel = state.currentChannel, onClose = { nav.popBackStack() }) }

        AnimatedVisibility(
            visible = overlayVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomStart),
        ) {
            BottomOverlay(
                onToggleDrawer = { drawerOpen = !drawerOpen },
                onPlayPause = {},
                isPlaying = connectionPhase == ConnectionPhase.Playing,
            )
        }

        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            ChannelDrawer(
                channels = state.channels,
                currentId = state.currentChannel?.id,
                onPick = { ch ->
                    drawerOpen = false
                    nav.navigate(Routes.player(ch.streamUrl)) { popUpTo(Routes.Player) { inclusive = true } }
                },
                onClose = { drawerOpen = false },
            )
        }
    }
}

private enum class ConnectionPhase { Buffering, Playing, NoSignal }

@Composable
private fun VideoSurfacePlaceholder(ch: Channel?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF112030), Color(0xFF050A10)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (ch != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ChannelLogo(ch.logoUrl, ch.name, modifier = Modifier.size(120.dp))
                Spacer(Modifier.height(16.dp))
                Text(ch.name, color = TextPrimary.copy(alpha = 0.6f), style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}

@Composable
private fun CenterStatus(text: String, color: Color) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
                .padding(horizontal = 18.dp, vertical = 10.dp),
        ) {
            Box(modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(4.dp)))
            Spacer(Modifier.width(10.dp))
            Text(text, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun TopOverlay(channel: Channel?, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChannelLogo(channel?.logoUrl, channel?.name ?: "?", modifier = Modifier.size(48.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill("● LIVE", color = Red500)
                Text(channel?.group ?: "—", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
            }
            Text(channel?.name ?: "Stream", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
            Text(
                "Now · La Liga: Real Madrid vs Barcelona  ·  Next · Football Tonight",
                color = TextMuted,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OverlayChip(Icons.Default.Info, "Info") {}
            OverlayChip(Icons.Default.Tune, "Source") {}
            OverlayChip(Icons.Default.Close, "Close", onClose)
        }
    }
}

@Composable
private fun BottomOverlay(onToggleDrawer: () -> Unit, onPlayPause: () -> Unit, isPlaying: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
            .padding(20.dp),
    ) {
        // Progress (live) bar
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(2.dp))) {
            Box(modifier = Modifier.fillMaxWidth(0.65f).height(3.dp).background(Teal400, RoundedCornerShape(2.dp)))
        }
        Spacer(Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OverlayChip(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (isPlaying) "Pause" else "Play", onPlayPause)
            OverlayChip(Icons.Default.AudioFile, "Audio") {}
            OverlayChip(Icons.Default.Subtitles, "Subtitles") {}
            OverlayChip(Icons.Default.AspectRatio, "Aspect") {}
            Spacer(Modifier.weight(1f))
            OverlayChip(Icons.Default.ViewList, "Channels", onToggleDrawer)
            OverlayChip(Icons.Default.Fullscreen, "Fullscreen") {}
        }
    }
}

@Composable
private fun OverlayChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val shape = RoundedCornerShape(999.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(40.dp)
            .background(Color.White.copy(alpha = 0.08f), shape)
            .border(1.dp, if (focused) FocusRing else GlassStroke, shape)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp),
    ) {
        Icon(icon, null, tint = TextPrimary)
        Spacer(Modifier.width(8.dp))
        Text(label, color = TextPrimary, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ChannelDrawer(channels: List<Channel>, currentId: String?, onPick: (Channel) -> Unit, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .width(380.dp)
            .fillMaxHeight()
            .background(Bg800)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Channels", color = TextPrimary, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.clickable { onClose() })
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(channels) { ch ->
                val isCurrent = ch.id == currentId
                val src = remember { MutableInteractionSource() }
                val focused by src.collectIsFocusedAsState()
                val shape = RoundedCornerShape(12.dp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(if (isCurrent) TealGlow else Bg700, shape)
                        .border(1.dp, if (focused) FocusRing else if (isCurrent) Teal400 else GlassStroke, shape)
                        .clickable(interactionSource = src, indication = null) { onPick(ch) }
                        .padding(8.dp),
                ) {
                    ChannelLogo(ch.logoUrl, ch.name, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ch.name, color = TextPrimary, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                        Text(ch.group, color = TextMuted, style = MaterialTheme.typography.labelSmall)
                    }
                    StatusPill(ch.quality, color = Teal400)
                }
            }
        }
    }
}
