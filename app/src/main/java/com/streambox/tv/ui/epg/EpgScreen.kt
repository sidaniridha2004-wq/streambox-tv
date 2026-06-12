package com.streambox.tv.ui.epg

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.streambox.tv.data.Channel
import com.streambox.tv.data.EpgProgram
import com.streambox.tv.nav.Routes
import com.streambox.tv.ui.components.ChannelLogo
import com.streambox.tv.ui.theme.Bg700
import com.streambox.tv.ui.theme.Bg800
import com.streambox.tv.ui.theme.Bg900
import com.streambox.tv.ui.theme.FocusRing
import com.streambox.tv.ui.theme.GlassStroke
import com.streambox.tv.ui.theme.Red500
import com.streambox.tv.ui.theme.Teal400
import com.streambox.tv.ui.theme.TealGlow
import com.streambox.tv.ui.theme.TextMuted
import com.streambox.tv.ui.theme.TextPrimary

private val PIXELS_PER_MINUTE = 4.dp
private val ROW_HEIGHT = 64.dp
private val CHANNEL_COLUMN_WIDTH = 200.dp
private const val START_OFFSET_MIN = -60     // show 1h in the past
private const val END_OFFSET_MIN = 240       // and 4h in the future

@Composable
fun EpgScreen(nav: NavHostController, vm: EpgViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val hScroll = rememberScrollState()
    val totalMinutes = END_OFFSET_MIN - START_OFFSET_MIN
    val timelineWidth = PIXELS_PER_MINUTE * totalMinutes
    val nowOffsetPx = -START_OFFSET_MIN  // minutes from start to "now"

    Column(modifier = Modifier.fillMaxSize().background(Bg900)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("TV Guide", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Text("Now · ${state.timeLabel}", style = MaterialTheme.typography.labelMedium, color = TextMuted)
            }
            LegendRow()
        }

        Row(modifier = Modifier.fillMaxSize()) {
            // Channel column (sticky on the left)
            Column(
                modifier = Modifier
                    .width(CHANNEL_COLUMN_WIDTH)
                    .fillMaxHeight()
                    .background(Bg800)
                    .padding(start = 12.dp, end = 4.dp, top = 32.dp),
            ) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(state.channels) { ch ->
                        ChannelCell(ch)
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Column(modifier = Modifier.horizontalScroll(hScroll)) {
                    TimelineHeader(totalMinutes)
                    Box(modifier = Modifier.width(timelineWidth)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                            state.channels.forEach { ch ->
                                ProgramRow(
                                    programs = state.epgByChannel[ch.id] ?: emptyList(),
                                    onProgramClick = { _ ->
                                        nav.navigate(Routes.player(ch.streamUrl))
                                    },
                                )
                            }
                        }
                        // Current time indicator (vertical red line)
                        Box(
                            modifier = Modifier
                                .padding(start = (PIXELS_PER_MINUTE * nowOffsetPx))
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(Red500),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendRow() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LegendDot(Red500, "Now")
        LegendDot(Teal400, "Selected")
        LegendDot(TextMuted, "Past")
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(4.dp)))
        Spacer(Modifier.width(6.dp))
        Text(label, color = TextMuted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ChannelCell(channel: Channel) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .background(Bg700, RoundedCornerShape(10.dp))
            .border(1.dp, GlassStroke, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp),
    ) {
        ChannelLogo(channel.logoUrl, channel.name, modifier = Modifier.size(40.dp))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(channel.name, color = TextPrimary, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${String.format("%03d", channel.number)} · ${channel.group}", color = TextMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
private fun TimelineHeader(totalMinutes: Int) {
    Row(modifier = Modifier.height(28.dp).padding(start = 0.dp)) {
        var t = START_OFFSET_MIN
        while (t < END_OFFSET_MIN) {
            val isHour = t % 60 == 0
            Box(
                modifier = Modifier
                    .width(PIXELS_PER_MINUTE * 30) // 30-min cell
                    .fillMaxHeight()
                    .background(if (isHour) Color.White.copy(alpha = 0.04f) else Color.Transparent),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    formatRelativeTime(t),
                    color = if (isHour) TextPrimary else TextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            t += 30
        }
    }
}

private fun formatRelativeTime(offsetMin: Int): String {
    // Anchor "now" to a fixed string so the demo is deterministic
    val baseHour = 20
    val total = baseHour * 60 + 0 + offsetMin
    val hh = ((total / 60) % 24 + 24) % 24
    val mm = ((total % 60) + 60) % 60
    return "%02d:%02d".format(hh, mm)
}

@Composable
private fun ProgramRow(programs: List<EpgProgram>, onProgramClick: (EpgProgram) -> Unit) {
    Row(modifier = Modifier.height(ROW_HEIGHT).fillMaxWidth()) {
        var cursor = START_OFFSET_MIN
        val sorted = programs.sortedBy { it.startMinute }
        sorted.forEach { p ->
            // Skip programs that are completely outside the window
            val start = p.startMinute
            val end = p.startMinute + p.durationMinutes
            if (end <= START_OFFSET_MIN || start >= END_OFFSET_MIN) return@forEach
            val visibleStart = maxOf(start, START_OFFSET_MIN)
            val visibleEnd = minOf(end, END_OFFSET_MIN)
            if (visibleStart > cursor) {
                Spacer(modifier = Modifier.width(PIXELS_PER_MINUTE * (visibleStart - cursor)))
            }
            ProgramBlock(
                program = p,
                isPast = end <= 0,
                isNow = start <= 0 && end > 0,
                widthMinutes = visibleEnd - visibleStart,
                onClick = { onProgramClick(p) },
            )
            cursor = visibleEnd
        }
    }
}

@Composable
private fun ProgramBlock(
    program: EpgProgram,
    isPast: Boolean,
    isNow: Boolean,
    widthMinutes: Int,
    onClick: () -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val shape = RoundedCornerShape(8.dp)
    val bg = when {
        focused -> TealGlow
        isNow -> Color(0xFF173040)
        isPast -> Bg800
        else -> Bg700
    }
    val stroke = when {
        focused -> FocusRing
        isNow -> Teal400
        else -> GlassStroke
    }
    Column(
        modifier = Modifier
            .padding(end = 2.dp)
            .width(PIXELS_PER_MINUTE * widthMinutes)
            .fillMaxHeight()
            .background(bg, shape)
            .border(1.dp, stroke, shape)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Text(
            program.title,
            color = if (isPast && !focused) TextMuted else TextPrimary,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "${program.durationMinutes} min",
            color = TextMuted,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
