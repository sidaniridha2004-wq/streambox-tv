package com.streambox.tv.ui.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import androidx.compose.ui.viewinterop.AndroidView
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
import kotlin.math.abs

private enum class ConnectionPhase { Buffering, Playing, Error }

private enum class GestureKind { None, Brightness, Volume, Seek }

private val ASPECT_OPTIONS = listOf(
    "Fit" to AspectRatioFrameLayout.RESIZE_MODE_FIT,
    "Fill" to AspectRatioFrameLayout.RESIZE_MODE_FILL,
    "Zoom" to AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
    "Fixed W" to AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH,
    "Fixed H" to AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT,
)

@Composable
fun PlayerScreen(nav: NavHostController, streamUrl: String, vm: PlayerViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val activity = ctx.findActivity()

    // ---- Force landscape + immersive while this screen is on top ----
    DisposableEffect(activity) {
        if (activity != null) {
            val originalOrientation = activity.requestedOrientation
            val originalBrightness = activity.window.attributes.screenBrightness
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            // Hide system bars (status + navigation) for an immersive cinema feel.
            val controller = androidx.core.view.WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            onDispose {
                activity.requestedOrientation = originalOrientation
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                val lp = activity.window.attributes
                lp.screenBrightness = originalBrightness
                activity.window.attributes = lp
            }
        } else onDispose { }
    }

    // ---- ExoPlayer lifecycle ----
    val exo = remember(streamUrl) {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(streamUrl))
            playWhenReady = true
            prepare()
        }
    }
    var phase by remember { mutableStateOf(ConnectionPhase.Buffering) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(true) }

    DisposableEffect(exo) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                phase = when (playbackState) {
                    Player.STATE_BUFFERING -> ConnectionPhase.Buffering
                    Player.STATE_READY, Player.STATE_ENDED -> ConnectionPhase.Playing
                    else -> phase
                }
                durationMs = exo.duration.coerceAtLeast(0L)
            }
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlayerError(error: PlaybackException) {
                phase = ConnectionPhase.Error
                errorMessage = error.message ?: error.errorCodeName
            }
        }
        exo.addListener(listener)
        onDispose { exo.removeListener(listener); exo.release() }
    }
    LaunchedEffect(exo) {
        while (true) {
            positionMs = exo.currentPosition.coerceAtLeast(0L)
            delay(500)
        }
    }

    // ---- Overlay & gesture state ----
    var overlayVisible by remember { mutableStateOf(true) }
    var drawerOpen by remember { mutableStateOf(false) }
    var aspectMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    var brightness by remember { mutableFloatStateOf(activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0 } ?: 0.5f) }
    val audio = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVol = remember { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    var volume by remember { mutableFloatStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVol) }

    var hudIcon by remember { mutableStateOf<ImageVector?>(null) }
    var hudLabel by remember { mutableStateOf("") }
    var hudFraction by remember { mutableFloatStateOf(0f) }
    var hudUntil by remember { mutableLongStateOf(0L) }

    // Auto-hide overlay
    LaunchedEffect(overlayVisible, isPlaying, phase) {
        if (overlayVisible && isPlaying && phase == ConnectionPhase.Playing) {
            delay(4000)
            overlayVisible = false
        }
    }
    // Auto-hide HUD
    LaunchedEffect(hudUntil) {
        if (hudUntil > 0) {
            val remaining = hudUntil - System.currentTimeMillis()
            if (remaining > 0) delay(remaining)
            hudIcon = null
        }
    }

    fun showHud(icon: ImageVector, label: String, fraction: Float) {
        hudIcon = icon
        hudLabel = label
        hudFraction = fraction.coerceIn(0f, 1f)
        hudUntil = System.currentTimeMillis() + 700
    }

    fun applyBrightness(value: Float) {
        brightness = value.coerceIn(0.01f, 1f)
        activity?.window?.attributes = activity?.window?.attributes?.also { it.screenBrightness = brightness }
        showHud(Icons.Default.BrightnessHigh, "Brightness", brightness)
    }
    fun applyVolume(value: Float) {
        volume = value.coerceIn(0f, 1f)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, (volume * maxVol).toInt(), 0)
        showHud(Icons.Default.VolumeUp, "Volume", volume)
    }
    fun seekRelative(deltaMs: Long) {
        val target = (exo.currentPosition + deltaMs).coerceAtLeast(0L)
        if (durationMs > 0) {
            exo.seekTo(target.coerceAtMost(durationMs))
            val frac = target.toFloat() / durationMs.coerceAtLeast(1)
            showHud(
                if (deltaMs > 0) Icons.Default.FastForward else Icons.Default.FastRewind,
                "${if (deltaMs > 0) "+" else ""}${deltaMs / 1000}s",
                frac,
            )
        } else {
            // live stream — show a quick toast
            showHud(Icons.Default.Info, "Live stream", 1f)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // ---- ExoPlayer surface ----
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { c ->
                PlayerView(c).apply {
                    useController = false   // we draw our own UI
                    player = exo
                    resizeMode = aspectMode
                }
            },
            update = { it.resizeMode = aspectMode },
        )

        // ---- Gesture surface (transparent, captures drag/tap) ----
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(streamUrl) {
                    var startX = 0f
                    var startY = 0f
                    var startBrightness = brightness
                    var startVolume = volume
                    var startPos = exo.currentPosition
                    var kind = GestureKind.None
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    detectDragGestures(
                        onDragStart = { off ->
                            startX = off.x; startY = off.y
                            startBrightness = brightness
                            startVolume = volume
                            startPos = exo.currentPosition
                            kind = GestureKind.None
                        },
                        onDragEnd = { kind = GestureKind.None },
                        onDragCancel = { kind = GestureKind.None },
                    ) { change, _ ->
                        val dx = change.position.x - startX
                        val dy = change.position.y - startY
                        if (kind == GestureKind.None) {
                            if (abs(dx) > 24f || abs(dy) > 24f) {
                                kind = if (abs(dx) > abs(dy)) GestureKind.Seek
                                else if (startX < w / 2f) GestureKind.Brightness
                                else GestureKind.Volume
                            }
                        }
                        when (kind) {
                            GestureKind.Brightness -> applyBrightness(startBrightness - dy / h)
                            GestureKind.Volume -> applyVolume(startVolume - dy / h)
                            GestureKind.Seek -> {
                                if (durationMs > 0) {
                                    val deltaMs = ((dx / w) * 90_000L).toLong() // 90s across full width
                                    val target = (startPos + deltaMs).coerceIn(0L, durationMs)
                                    exo.seekTo(target)
                                    val frac = target.toFloat() / durationMs
                                    showHud(
                                        if (deltaMs >= 0) Icons.Default.FastForward else Icons.Default.FastRewind,
                                        formatDuration(target) + " / " + formatDuration(durationMs),
                                        frac,
                                    )
                                } else {
                                    showHud(Icons.Default.Info, "Live stream — seeking unavailable", 1f)
                                }
                            }
                            GestureKind.None -> Unit
                        }
                    }
                }
                .pointerInput(streamUrl) {
                    detectTapGestures(
                        onTap = { overlayVisible = !overlayVisible },
                        onDoubleTap = { off ->
                            if (off.x < size.width / 2f) seekRelative(-10_000L)
                            else seekRelative(+10_000L)
                        },
                    )
                },
        )

        // ---- Center HUD (brightness / volume / seek) ----
        AnimatedVisibility(
            visible = hudIcon != null,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.Center),
        ) {
            CenterHud(icon = hudIcon ?: Icons.Default.Info, label = hudLabel, fraction = hudFraction)
        }

        // ---- Buffering / Error overlay ----
        when (phase) {
            ConnectionPhase.Buffering -> CenterStatus("Buffering…", Teal400)
            ConnectionPhase.Error -> CenterStatus(errorMessage ?: "Playback error", Red500)
            ConnectionPhase.Playing -> Unit
        }

        // ---- Top overlay ----
        AnimatedVisibility(
            visible = overlayVisible,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
        ) { TopOverlay(channel = state.currentChannel, onClose = { nav.popBackStack() }) }

        // ---- Bottom overlay ----
        AnimatedVisibility(
            visible = overlayVisible,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.BottomStart),
        ) {
            BottomOverlay(
                isPlaying = isPlaying,
                positionMs = positionMs,
                durationMs = durationMs,
                onPlayPause = { if (isPlaying) exo.pause() else exo.play() },
                onSeekTo = { ms -> exo.seekTo(ms) },
                onToggleDrawer = { drawerOpen = !drawerOpen },
                onCycleAspect = {
                    val next = ASPECT_OPTIONS[(ASPECT_OPTIONS.indexOfFirst { it.second == aspectMode } + 1) % ASPECT_OPTIONS.size]
                    aspectMode = next.second
                    showHud(Icons.Default.AspectRatio, next.first, 1f)
                },
            )
        }

        // ---- Right-side channel drawer ----
        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInHorizontally(tween(220)) { it } + fadeIn(tween(220)),
            exit = slideOutHorizontally(tween(180)) { it } + fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            ChannelDrawer(
                channels = state.channels,
                currentId = state.currentChannel?.id,
                onPick = { ch ->
                    drawerOpen = false
                    nav.navigate(Routes.player(ch.streamUrl)) {
                        popUpTo(Routes.Player) { inclusive = true }
                    }
                },
                onClose = { drawerOpen = false },
            )
        }
    }
}

// ---------- Helpers ----------

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is android.content.ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

// ---------- Overlay composables ----------

@Composable
private fun CenterHud(icon: ImageVector, label: String, fraction: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Icon(icon, null, tint = Teal400, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(8.dp))
        Text(label, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(4.dp)
                .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(2.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(4.dp)
                    .background(Teal400, RoundedCornerShape(2.dp)),
            )
        }
    }
}

@Composable
private fun CenterStatus(text: String, color: Color) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OverlayChip(Icons.Default.Info, "Info") {}
            OverlayChip(Icons.Default.PictureInPicture, "PiP") {}
            OverlayChip(Icons.Default.Close, "Close", onClose)
        }
    }
}

@Composable
private fun BottomOverlay(
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleDrawer: () -> Unit,
    onCycleAspect: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))))
            .padding(20.dp),
    ) {
        // Scrubber row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatDuration(positionMs),
                color = TextPrimary,
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.width(10.dp))
            val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0.65f
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(2.dp))
                    .clickable {
                        if (durationMs > 0) onSeekTo((durationMs * 0.5f).toLong())
                    },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(4.dp)
                        .background(Teal400, RoundedCornerShape(2.dp)),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                if (durationMs > 0) formatDuration(durationMs) else "LIVE",
                color = TextPrimary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OverlayChip(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (isPlaying) "Pause" else "Play", onPlayPause)
            OverlayChip(Icons.Default.AudioFile, "Audio") {}
            OverlayChip(Icons.Default.Subtitles, "Subtitles") {}
            OverlayChip(Icons.Default.AspectRatio, "Aspect", onCycleAspect)
            Spacer(Modifier.weight(1f))
            OverlayChip(Icons.Default.Info, "Channels", onToggleDrawer)
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
private fun ChannelDrawer(
    channels: List<Channel>,
    currentId: String?,
    onPick: (Channel) -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(380.dp)
            .fillMaxHeight()
            .background(Bg800)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Channels",
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.clickable { onClose() })
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(channels, key = { it.id }) { ch ->
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
