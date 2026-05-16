package com.streambox.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import com.streambox.tv.ui.theme.Bg800
import com.streambox.tv.ui.theme.Bg900
import com.streambox.tv.ui.theme.Divider
import com.streambox.tv.ui.theme.FocusRing
import com.streambox.tv.ui.theme.GlassStroke
import com.streambox.tv.ui.theme.Teal400
import com.streambox.tv.ui.theme.TealGlow
import com.streambox.tv.ui.theme.TextMuted
import com.streambox.tv.ui.theme.TextPrimary

data class NavItem(val key: String, val label: String, val icon: ImageVector)

/** Phone bottom nav (compact). */
@Composable
fun PhoneBottomNav(
    items: List<NavItem>,
    current: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Bg800)
            .border(width = 1.dp, color = Divider, shape = RoundedCornerShape(0.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        items.forEach { item ->
            BottomNavItem(item, current == item.key) { onSelect(item.key) }
        }
    }
}

@Composable
private fun BottomNavItem(item: NavItem, selected: Boolean, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        val tint = if (selected) Teal400 else TextMuted
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .background(if (selected) TealGlow else Color.Transparent, RoundedCornerShape(12.dp)),
        ) { Icon(item.icon, null, tint = tint) }
        Spacer(Modifier.height(4.dp))
        Text(item.label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

/** TV / large screen left navigation rail. Designed for D-pad focus. */
@Composable
fun TvNavRail(
    items: List<NavItem>,
    current: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(if (expanded) 220.dp else 84.dp)
            .background(Bg900)
            .padding(vertical = 24.dp, horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BrandMark(expanded = expanded)
        Spacer(Modifier.height(12.dp))
        items.forEach { item ->
            RailItem(item = item, selected = current == item.key, expanded = expanded) { onSelect(item.key) }
        }
    }
}

@Composable
private fun BrandMark(expanded: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Teal400, RoundedCornerShape(10.dp)),
        )
        if (expanded) {
            Spacer(Modifier.width(10.dp))
            Column {
                Text("StreamBox", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                Text("TV · IPTV", color = TextMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun RailItem(item: NavItem, selected: Boolean, expanded: Boolean, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    val bg = when {
        selected -> TealGlow
        focused -> Color.White.copy(alpha = 0.06f)
        else -> Color.Transparent
    }
    val stroke = when {
        focused -> FocusRing
        selected -> Teal400.copy(alpha = 0.4f)
        else -> Color.Transparent
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(bg, shape)
            .border(1.dp, stroke, shape)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp),
    ) {
        Icon(item.icon, null, tint = if (selected) Teal400 else TextPrimary)
        if (expanded) {
            Spacer(Modifier.width(12.dp))
            Text(
                item.label,
                color = if (selected) Teal400 else TextPrimary,
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

/** Underlined segmented tabs (used in details, search). */
@Composable
fun SegmentedTabs(
    items: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Bg800)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { label ->
            val isSelected = label == selected
            val src = remember { MutableInteractionSource() }
            val focused by src.collectIsFocusedAsState()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .clickable(interactionSource = src, indication = null) { onSelected(label) }
                    .padding(vertical = 10.dp),
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    color = when {
                        isSelected -> Teal400
                        focused -> TextPrimary
                        else -> TextMuted
                    },
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .width(if (isSelected) 28.dp else 0.dp)
                        .background(Teal400, RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}
