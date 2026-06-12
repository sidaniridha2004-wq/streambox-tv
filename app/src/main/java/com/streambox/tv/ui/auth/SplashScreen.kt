package com.streambox.tv.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.streambox.tv.ui.theme.Bg900
import com.streambox.tv.ui.theme.Teal400
import com.streambox.tv.ui.theme.Teal500
import com.streambox.tv.ui.theme.TextMuted
import com.streambox.tv.ui.theme.TextPrimary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onContinue: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(900)
        onContinue()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(Color(0xFF0F2027), Bg900))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(Brush.linearGradient(listOf(Teal500, Teal400)), RoundedCornerShape(28.dp)),
            )
            Spacer(Modifier.height(20.dp))
            Text("AuraTV", color = TextPrimary, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(6.dp))
            Text("IPTV, in focus.", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
