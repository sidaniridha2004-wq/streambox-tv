package com.streambox.tv.ui.auth

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.streambox.tv.ui.components.GhostButton
import com.streambox.tv.ui.components.GlassCard
import com.streambox.tv.ui.components.PrimaryButton
import com.streambox.tv.ui.theme.Bg900
import com.streambox.tv.ui.theme.Teal400
import com.streambox.tv.ui.theme.Teal500
import com.streambox.tv.ui.theme.TealGlow
import com.streambox.tv.ui.theme.TextMuted
import com.streambox.tv.ui.theme.TextPrimary
import com.streambox.tv.ui.theme.TextSecondary

@Composable
fun WelcomeScreen(onAddPlaylist: () -> Unit, onSkip: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Bg900, androidx.compose.ui.graphics.Color(0xFF050B11)))),
    ) {
        // Decorative glow
        Box(
            modifier = Modifier
                .size(420.dp)
                .padding(60.dp)
                .background(TealGlow, RoundedCornerShape(999.dp))
                .align(Alignment.TopEnd),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.size(64.dp).background(Brush.linearGradient(listOf(Teal500, Teal400)), RoundedCornerShape(20.dp)),
            )
            Spacer(Modifier.height(28.dp))
            Text("Welcome to StreamBox", style = MaterialTheme.typography.displaySmall, color = TextPrimary)
            Spacer(Modifier.height(12.dp))
            Text(
                "Bring your own IPTV. Connect a playlist or a Stalker portal and we’ll handle the rest — channels, EPG, movies, series, favorites and a player tuned for both phone and TV.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth(0.7f),
            )
            Spacer(Modifier.height(28.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(0.7f)) {
                Column {
                    FeatureLine("Live TV with EPG, now & next, channel zapping")
                    Spacer(Modifier.height(8.dp))
                    FeatureLine("Movies, Series with seasons and watched progress")
                    Spacer(Modifier.height(8.dp))
                    FeatureLine("Multiple providers with quick switching")
                    Spacer(Modifier.height(8.dp))
                    FeatureLine("D-pad friendly UI for Android TV")
                }
            }
            Spacer(Modifier.height(36.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PrimaryButton(text = "Add playlist", onClick = onAddPlaylist, leadingIcon = Icons.Default.Add)
                Spacer(Modifier.width(12.dp))
                GhostButton(text = "Skip — Browse demo", onClick = onSkip)
            }
        }
    }
}

@Composable
private fun FeatureLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(6.dp).background(Teal400, RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(10.dp))
        Text(text, color = TextMuted, style = MaterialTheme.typography.bodyMedium)
    }
}
