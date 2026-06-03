package com.antigravity.iptv.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.antigravity.iptv.data.local.entity.ChannelEntity
import com.antigravity.iptv.ui.MainViewModel
import com.antigravity.iptv.ui.theme.*
import androidx.compose.foundation.ExperimentalFoundationApi

import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import androidx.compose.ui.res.stringResource
import com.antigravity.iptv.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VideoPlayerScreen(
    streamUrl: String,
    channelName: String = "",
    viewModel: MainViewModel? = null,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Force landscape + immersive on enter
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        onDispose {
            activity?.requestedOrientation = originalOrientation
            activity?.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, true)
                WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    var currentStreamUrl by remember { mutableStateOf(streamUrl) }
    var currentChannelName by remember { mutableStateOf(channelName) }
    
    val exoPlayer = remember {
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                30000,     // minBufferMs (30s — aggressive but not excessive)
                120000,    // maxBufferMs (2 minutes max buffer)
                500,       // bufferForPlaybackMs (start playing after 500ms)
                1500       // bufferForPlaybackAfterRebufferMs (quick recovery)
            )
            .setBackBuffer(120000, true) // Keep 2 min back buffer, retain from keyframe
            .setPrioritizeTimeOverSizeThresholds(true)
            .setTargetBufferBytes(-1)
            .build()

        // Adaptive track selection for automatic quality switching on bad network
        val trackSelector = androidx.media3.exoplayer.trackselection.DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setMaxVideoSizeSd() // Prefer SD to reduce bandwidth pressure
                    .setForceLowestBitrate(false)
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
            )
        }

        androidx.media3.exoplayer.ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .build().apply {
                playWhenReady = true
            }
    }

    var showResumeToast by remember { mutableStateOf(false) }

    // Check favorite status and get entity if viewModel exists
    val channels by (viewModel?.activeChannels ?: kotlinx.coroutines.flow.flowOf(emptyList<ChannelEntity>())).collectAsState(initial = emptyList())
    val channelEntity = remember(channels, currentChannelName) { channels.firstOrNull { it.name == currentChannelName } }

    LaunchedEffect(currentStreamUrl, channelEntity) {
        val mediaItem = androidx.media3.common.MediaItem.Builder()
            .setUri(currentStreamUrl)
            .setLiveConfiguration(
                androidx.media3.common.MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(15000) // 15s behind live edge (good balance)
                    .setMaxPlaybackSpeed(1.04f) // Allow 4% speedup to catch up
                    .build()
            )
            .build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        if (viewModel != null && channelEntity != null && (channelEntity.sourceType == "movie" || channelEntity.sourceType == "series")) {
            val savedProgress = viewModel.getWatchProgress(channelEntity)
            if (savedProgress > 5000L) { // Only resume if watched for more than 5s
                exoPlayer.seekTo(savedProgress)
                showResumeToast = true
                delay(3000)
                showResumeToast = false
            }
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(0L) }
    var bufferedPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var showChannelList by remember { mutableStateOf(false) }
    var showPlaybackSettings by remember { mutableStateOf(false) }
    var showRetryIndicator by remember { mutableStateOf(false) }
    var retryCount by remember { mutableStateOf(0) }


    // Brightness state (0.0 to 1.0)
    var brightness by remember { 
        mutableStateOf(activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.5f)
    }

    // Volume state (0 to Max)
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    var volume by remember { mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) }

    DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: android.content.Intent?) {
                if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                    volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                }
            }
        }
        context.registerReceiver(receiver, android.content.IntentFilter("android.media.VOLUME_CHANGED_ACTION"))
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    // Fit Screen Mode
    var scaleMode by remember { mutableStateOf(0) } // 0=Fit, 1=Fill, 2=Zoom
    
    // PiP Mode State
    var isInPipMode by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val consumer = androidx.core.util.Consumer<androidx.core.app.PictureInPictureModeChangedInfo> { info ->
            isInPipMode = info.isInPictureInPictureMode
            if (info.isInPictureInPictureMode) showControls = false
        }
        val componentActivity = activity as? androidx.activity.ComponentActivity
        componentActivity?.addOnPictureInPictureModeChangedListener(consumer)
        onDispose {
            componentActivity?.removeOnPictureInPictureModeChangedListener(consumer)
        }
    }

    val isFavorite = channelEntity?.isFavorite == true
    
    val filteredDrawerChannels = remember(channels, channelEntity) {
        if (channelEntity != null) {
            channels.filter { it.groupName == channelEntity.groupName }
        } else channels.take(100)
    }
    
    var alternativeQualities by remember { mutableStateOf(emptyList<ChannelEntity>()) }
    
    LaunchedEffect(currentChannelName, channelEntity) {
        if (viewModel != null && channelEntity != null) {
            val alternatives = viewModel.getAlternativeQualities(channelEntity.playlistId, currentChannelName)
            if (alternatives.size > 1) {
                alternativeQualities = alternatives
            } else {
                alternativeQualities = emptyList()
            }
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                isPlaying = isPlayingNow
                if (isPlayingNow) {
                    showRetryIndicator = false
                }
            }
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_BUFFERING && retryCount > 0) {
                    showRetryIndicator = true
                }
                if (state == Player.STATE_READY) {
                    showRetryIndicator = false
                    retryCount = 0
                }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                retryCount++
                showRetryIndicator = true
                exoPlayer.seekToDefaultPosition()
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    val componentActivity = activity as? androidx.activity.ComponentActivity
                    val inPip = componentActivity?.isInPictureInPictureMode == true
                    if (!inPip) {
                        exoPlayer.pause()
                    }
                }
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    exoPlayer.pause()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Auto-update progress
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition
            bufferedPosition = exoPlayer.bufferedPosition
            duration = exoPlayer.duration.coerceAtLeast(0L)
            
            // Save progress if it's a VOD
            if (channelEntity != null && (channelEntity.sourceType == "movie" || channelEntity.sourceType == "series")) {
                if (duration > 0 && currentPosition > 0) {
                    viewModel?.updateWatchProgress(channelEntity, currentPosition, duration)
                }
            }
            delay(1000L)
        }
    }

    // Auto-hide controls
    LaunchedEffect(showControls, isPlaying, showChannelList) {
        if (showControls && isPlaying && !showChannelList) {
            delay(5000)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video Surface
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    keepScreenOn = true
                    useController = false
                }
            },
            update = { view ->
                view.resizeMode = when (scaleMode) {
                    0 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    1 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                    else -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            }
        )

        // Auto-hide retry indicator after 5 seconds
        LaunchedEffect(showRetryIndicator) {
            if (showRetryIndicator) {
                delay(5000)
                if (!isBuffering) showRetryIndicator = false
            }
        }

        // Retry/Reconnection Indicator
        AnimatedVisibility(
            visible = showRetryIndicator,
            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { -it },
            exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { -it },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        color = AuraCyan,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = if (retryCount > 0) stringResource(R.string.reconnecting, retryCount) else stringResource(R.string.buffering),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Resume Toast Overlay
        AnimatedVisibility(
            visible = showResumeToast,
            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { -it },
            exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { -it },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = AuraPurple.copy(alpha = 0.9f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Resuming playback...",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Gesture Overlay Zones
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Zone: Brightness & Double Tap Rewind
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        if (showChannelList) showChannelList = false else showControls = !showControls
                    }
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull()
                                if (change != null && change.pressed && !showChannelList) {
                                    // Make sure it's a drag/move or down event, not just a tap (so double tap works)
                                    if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Move) {
                                        showControls = true
                                        val y = change.position.y
                                        val h = size.height.toFloat()
                                        brightness = (1f - (y / h)).coerceIn(0f, 1f)
                                        
                                        activity?.window?.let { window ->
                                            val lp = window.attributes
                                            lp.screenBrightness = brightness
                                            window.attributes = lp
                                        }
                                        change.consume()
                                    }
                                }
                            }
                        }
                    }
            )

            // Right Zone: Volume & Double Tap Fast Forward
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        if (showChannelList) showChannelList = false else showControls = !showControls
                    }
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull()
                                if (change != null && change.pressed && !showChannelList) {
                                    if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Move) {
                                        showControls = true
                                        val y = change.position.y
                                        val h = size.height.toFloat()
                                        val ratio = (1f - (y / h)).coerceIn(0f, 1f)
                                        
                                        volume = (ratio * maxVolume).roundToInt()
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
                                        change.consume()
                                    }
                                }
                            }
                        }
                    }
            )
        }

        AnimatedVisibility(
            visible = showControls && !isInPipMode,
            enter = fadeIn(tween(400)),
            exit = fadeOut(tween(400)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // TOP BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Left: Back button + Title
                    Row(verticalAlignment = Alignment.Top) {
                        if (onBack != null) {
                            AnimatedIconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        val qualityRegex = Regex("(?i)\\b(HD|FHD|SD|UHD|4K|8K|HEVC|H265|H264|1080p|720p)\\b")
                        val qualityMatch = qualityRegex.find(currentChannelName)?.value?.uppercase()
                        val cleanName = currentChannelName.replace(qualityRegex, "").trim().replace(Regex("\\s+"), " ")
                        
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = cleanName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                if (alternativeQualities.isNotEmpty()) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        items(alternativeQualities, key = { it.id }) { altChannel ->
                                            val altQualityMatch = qualityRegex.find(altChannel.name)?.value?.uppercase() ?: "SD"
                                            val isCurrent = altChannel.name == currentChannelName
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (isCurrent) com.antigravity.iptv.ui.theme.AuraCyan else Color.White.copy(alpha = 0.2f))
                                                    .clickable {
                                                        if (!isCurrent) {
                                                            viewModel?.getStreamUrl(
                                                                channel = altChannel,
                                                                playlistId = altChannel.playlistId,
                                                                onResolved = { url -> 
                                                                    currentStreamUrl = url
                                                                    currentChannelName = altChannel.name
                                                                },
                                                                onError = { Toast.makeText(context, context.getString(R.string.error), Toast.LENGTH_SHORT).show() }
                                                            )
                                                        }
                                                    }
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(text = altQualityMatch, color = if (isCurrent) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                } else if (qualityMatch != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(com.antigravity.iptv.ui.theme.AuraCyan)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = qualityMatch, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Text(
                                text = channelEntity?.groupName ?: "Stream",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Right: Top action icons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedIconButton(onClick = { 
                            Toast.makeText(context, "Stream: $currentChannelName", Toast.LENGTH_SHORT).show() 
                        }) {
                            Icon(Icons.Outlined.Info, contentDescription = "Info", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        AnimatedIconButton(onClick = { 
                            if (channelEntity != null) {
                                viewModel?.toggleFavorite(channelEntity)
                                Toast.makeText(context, if (isFavorite) context.getString(R.string.removed_from_list) else context.getString(R.string.added_to_list), Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, context.getString(R.string.cannot_favorite), Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(
                                if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, 
                                contentDescription = "Favorite", 
                                tint = if (isFavorite) com.antigravity.iptv.ui.theme.AuraPurple else Color.White, 
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        val fitStr = stringResource(R.string.aspect_fit)
                        val fillStr = stringResource(R.string.aspect_fill)
                        val zoomStr = stringResource(R.string.aspect_zoom)
                        val ratioLabel = stringResource(R.string.aspect_ratio_label, "%1\$s") // Will be formatted below
                        AnimatedIconButton(onClick = { 
                            scaleMode = (scaleMode + 1) % 3 
                            val modeStr = when(scaleMode) { 0 -> fitStr; 1 -> fillStr; else -> zoomStr }
                            Toast.makeText(context, String.format(ratioLabel, modeStr), Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Outlined.FitScreen, contentDescription = "Aspect Ratio", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        AnimatedIconButton(onClick = { 
                            showChannelList = !showChannelList
                        }) {
                            Icon(Icons.Outlined.List, contentDescription = "Channels", tint = if (showChannelList) com.antigravity.iptv.ui.theme.AuraCyan else Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                }

                // CENTER CONTROLS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(horizontal = 64.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Brightness Slider
                    VerticalPillSlider(
                        value = brightness,
                        icon = Icons.Default.BrightnessAuto,
                        text = "${(brightness * 100).roundToInt()}%",
                        color = com.antigravity.iptv.ui.theme.AuraCyan
                    )

                    // Play/Pause/Seek
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedIconButton(
                            onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0)) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Replay10, "Rewind 10s", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.fillMaxSize())
                        }

                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .bounceClick { if (isPlaying) exoPlayer.pause() else exoPlayer.play() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isBuffering) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(32.dp))
                            } else {
                                Crossfade(
                                    targetState = isPlaying, 
                                    animationSpec = tween(300),
                                    label = "play_pause_crossfade"
                                ) { playing ->
                                    Icon(
                                        imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (playing) "Pause" else "Play",
                                        tint = Color.Black,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                        }

                        AnimatedIconButton(
                            onClick = { exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Forward10, "Forward 10s", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.fillMaxSize())
                        }
                    }

                    // Volume Slider
                    val volumeRatio = if (maxVolume > 0) volume.toFloat() / maxVolume else 0f
                    VerticalPillSlider(
                        value = volumeRatio,
                        icon = if (volume == 0) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        text = "${(volumeRatio * 100).roundToInt()}%",
                        color = com.antigravity.iptv.ui.theme.AuraCyan
                    )
                }

                // BOTTOM CONTROLS
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                ) {
                    val isLive = duration <= 0L || duration == androidx.media3.common.C.TIME_UNSET

                    // Time & Mute/Fullscreen row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isLive) "LIVE" else "${formatTime(currentPosition)} / ${formatTime(duration)}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            AnimatedIconButton(onClick = { 
                                if (volume > 0) {
                                    volume = 0
                                } else {
                                    volume = maxVolume / 3
                                }
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
                            }) {
                                Icon(
                                    if (volume == 0) Icons.Default.VolumeOff else Icons.Default.VolumeUp, 
                                    contentDescription = "Mute", 
                                    tint = Color.White
                                )
                            }
                            AnimatedIconButton(onClick = { 
                                showPlaybackSettings = true
                            }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Scrub bar
                    if (!isLive) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            val durFloat = duration.toFloat().coerceAtLeast(1f)
                            val bufferedRatio = (bufferedPosition.toFloat() / durFloat).coerceIn(0f, 1f)
                            
                            // Background Track
                            Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(50)))
                            // Buffered Track (Grey)
                            Box(modifier = Modifier.fillMaxWidth(bufferedRatio).height(4.dp).background(Color.Gray.copy(alpha = 0.8f), RoundedCornerShape(50)))
                            
                            // Interactive Slider (transparent tracks)
                            Slider(
                                value = currentPosition.toFloat(),
                                onValueChange = { newPos ->
                                    exoPlayer.seekTo(newPos.toLong())
                                    currentPosition = newPos.toLong()
                                },
                                valueRange = 0f..durFloat,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = com.antigravity.iptv.ui.theme.AuraCyan,
                                    inactiveTrackColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(Color.White)
                        )
                    }
                }
            }
        }

        // Channels Side Drawer
        AnimatedVisibility(
            visible = showChannelList,
            enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing)),
            exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing)),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(320.dp)
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.channels), color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showChannelList = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    val listState = rememberLazyListState()
                    LaunchedEffect(filteredDrawerChannels, currentChannelName, showChannelList) {
                        if (showChannelList) {
                            val index = filteredDrawerChannels.indexOfFirst { it.name == currentChannelName }
                            if (index >= 0) {
                                listState.animateScrollToItem(index)
                            }
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredDrawerChannels, key = { it.id }) { channel ->
                            val isSelected = channel.name == currentChannelName
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItemPlacement()
                                    .bounceClick {
                                        viewModel?.getStreamUrl(
                                            channel = channel,
                                            playlistId = channel.playlistId,
                                            onResolved = { url -> 
                                                currentStreamUrl = url
                                                currentChannelName = channel.name
                                                showChannelList = false
                                            },
                                            onError = {}
                                        )
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) com.antigravity.iptv.ui.theme.AuraCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, com.antigravity.iptv.ui.theme.AuraCyan) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!channel.logoUrl.isNullOrBlank()) {
                                        coil.compose.AsyncImage(
                                            model = channel.logoUrl,
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }
                                    Text(
                                        text = channel.name, 
                                        color = if (isSelected) com.antigravity.iptv.ui.theme.AuraCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Playback Settings Overlay
        AnimatedVisibility(
            visible = showPlaybackSettings,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable { showPlaybackSettings = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.width(300.dp).clickable(enabled = false) {}, // absorb clicks
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.playback_speed), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(speeds) { speed ->
                                val isSelected = exoPlayer.playbackParameters.speed == speed
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) com.antigravity.iptv.ui.theme.AuraCyan.copy(alpha = 0.2f) else Color.Transparent)
                                        .clickable {
                                            exoPlayer.setPlaybackSpeed(speed)
                                            showPlaybackSettings = false
                                        }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${speed}x",
                                        color = if (isSelected) com.antigravity.iptv.ui.theme.AuraCyan else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.weight(1f))
                                        Icon(Icons.Default.Check, contentDescription = null, tint = com.antigravity.iptv.ui.theme.AuraCyan)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { showPlaybackSettings = false }, modifier = Modifier.align(Alignment.End)) {
                            Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.8f else 1f)

    Box(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

@Composable
fun VerticalPillSlider(
    value: Float, // 0f to 1f
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val animatedValue by androidx.compose.animation.core.animateFloatAsState(
        targetValue = value.coerceIn(0f, 1f), 
        animationSpec = androidx.compose.animation.core.tween(150)
    )
    
    Box(
        modifier = modifier
            .width(60.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Fill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(animatedValue)
                .background(color)
        )
        
        // Icon and Text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            val contentColor = if (animatedValue > 0.35f) Color.Black else Color.White
            Icon(
                imageVector = icon, 
                contentDescription = null, 
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text, 
                color = contentColor, 
                fontSize = 12.sp, 
                fontWeight = FontWeight.Bold
            )
        }
    }
}
