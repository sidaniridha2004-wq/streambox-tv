package com.streambox.tv.ui.components

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.streambox.tv.ui.theme.Bg600
import com.streambox.tv.ui.theme.Bg700
import com.streambox.tv.ui.theme.FocusRing
import com.streambox.tv.ui.theme.GlassStroke
import com.streambox.tv.ui.theme.Teal400
import com.streambox.tv.ui.theme.TextMuted
import com.streambox.tv.ui.theme.TextPrimary
import com.streambox.tv.ui.theme.TextSecondary

/** Square channel logo bubble used in lists and EPG. */
@Composable
fun ChannelLogo(
    logoUrl: String?,
    fallbackLetter: String,
    modifier: Modifier = Modifier.size(56.dp),
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .background(Bg600, shape)
            .border(1.dp, GlassStroke, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (!logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(6.dp),
            )
        } else {
            Text(
                fallbackLetter.take(2).uppercase(),
                color = Teal400,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

/** Live TV row item: logo + name + now/next + quality badge. Selected/focused highlight. */
@Composable
fun ChannelListItem(
    number: Int,
    name: String,
    group: String,
    nowTitle: String,
    nextTitle: String,
    logoUrl: String?,
    quality: String = "HD",
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    val highlight = isPlaying || focused
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .background(if (highlight) Bg600 else Bg700, shape)
            .border(1.dp, if (focused) FocusRing else if (isPlaying) Teal400 else GlassStroke, shape)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp),
    ) {
        Text(
            String.format("%03d", number),
            color = TextMuted,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(38.dp),
        )
        ChannelLogo(logoUrl, name, modifier = Modifier.size(48.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    name,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(8.dp))
                StatusPill(quality, color = Teal400)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                "Now · $nowTitle",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "Next · $nextTitle",
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(group, color = TextMuted, style = MaterialTheme.typography.labelSmall)
        if (isPlaying) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.LiveTv, null, tint = Teal400)
        }
    }
}

/** 2:3 portrait poster card for movies/series with focus zoom hint via border. */
@Composable
fun PosterCard(
    title: String,
    subtitle: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .border(if (focused) 2.dp else 1.dp, if (focused) FocusRing else GlassStroke, shape)
            .background(Bg700, shape)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .background(Bg600, RoundedCornerShape(10.dp)),
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(title.take(1).uppercase(), color = Teal400, style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(title, color = TextPrimary, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, color = TextMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun QuickActionCard(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .height(96.dp)
            .background(Bg700, shape)
            .border(1.dp, if (focused) FocusRing else GlassStroke, shape)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(Color(0x3314B8A6), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = Teal400) }
        Text(title, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
    }
}


