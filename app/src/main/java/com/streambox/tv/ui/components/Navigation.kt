package com.streambox.tv.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
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
    val animatedWidth by animateDpAsState(
        targetValue = if (expanded) 220.dp else 84.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "rail-width",
    )
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(animatedWidth)
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
                Text("AuraTV", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                Text("IPTV · in focus", color = TextMuted, style = MaterialTheme.typography.labelSmall)
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

/**
 * Animated underline tabs.
 *
 * Each tab measures its own width and absolute x-position (in dp) and reports
 * back to a shared map. The teal underline animates its `offset` and `width`
 * with a spring whenever the selected tab changes.
 */
@Composable
fun SegmentedTabs(
    items: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
) {
    val density = LocalDensity.current
    // measured x-offset and width in dp for each tab key
    val offsets = remember(items) { mutableStateMapOf<String, androidx.compose.ui.unit.Dp>() }
    val widths = remember(items) { mutableStateMapOf<String, androidx.compose.ui.unit.Dp>() }

    val targetOffset by animateDpAsState(
        targetValue = offsets[selected] ?: 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "tab-offset",
    )
    val targetWidth by animateDpAsState(
        targetValue = widths[selected] ?: 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "tab-width",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Bg800)
            .padding(contentPadding),
    ) {
        // Track layer (the moving underline) — drawn under the tabs row.
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = targetOffset)
                .width(targetWidth)
                .height(2.dp)
                .background(Teal400, RoundedCornerShape(2.dp)),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            items.forEachIndexed { index, label ->
                val isSelected = label == selected
                val src = remember { MutableInteractionSource() }
                val focused by src.collectIsFocusedAsState()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .clickable(interactionSource = src, indication = null) { onSelected(label) }
                        .padding(vertical = 10.dp)
                        .onSizeAndOffsetChanged(
                            onChanged = { x, w ->
                                offsets[label] = with(density) { x.toDp() }
                                widths[label] = with(density) { w.toDp() }
                            },
                        ),
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
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * Reports back the (x, width) of a layout in pixels relative to its parent.
 * Used by [SegmentedTabs] to animate a single shared underline.
 */
private fun Modifier.onSizeAndOffsetChanged(
    onChanged: (xPx: Float, widthPx: Float) -> Unit,
): Modifier = this.onGloballyPositioned { coords ->
    val parent = coords.parentLayoutCoordinates ?: return@onGloballyPositioned
    val rel = parent.localPositionOf(coords, androidx.compose.ui.geometry.Offset.Zero)
    onChanged(rel.x, coords.size.width.toFloat())
}
