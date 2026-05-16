package com.streambox.tv.ui.auth

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.streambox.tv.ui.components.ErrorBanner
import com.streambox.tv.ui.components.GlassCard
import com.streambox.tv.ui.components.PrimaryButton
import com.streambox.tv.ui.theme.Bg700
import com.streambox.tv.ui.theme.Bg900
import com.streambox.tv.ui.theme.GlassStroke
import com.streambox.tv.ui.theme.Green500
import com.streambox.tv.ui.theme.Teal400
import com.streambox.tv.ui.theme.TextMuted
import com.streambox.tv.ui.theme.TextPrimary
import com.streambox.tv.ui.theme.TextSecondary
import kotlinx.coroutines.delay

private data class Step(val label: String, val detail: String)

@Composable
fun SyncingScreen(onDone: () -> Unit, onError: () -> Unit) {
    val steps = listOf(
        Step("Authenticating provider", "Verifying credentials and endpoint…"),
        Step("Downloading playlist", "Fetching M3U / portal manifest…"),
        Step("Parsing 4,862 channels", "Grouping by category and language…"),
        Step("Loading EPG", "Reading XMLTV programs for the next 48h…"),
        Step("Finalizing library", "Indexing movies and series…"),
    )
    var current by remember { mutableStateOf(0) }
    var errored by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = (current + 1f) / steps.size,
        animationSpec = tween(600),
        label = "progress",
    )

    LaunchedEffect(Unit) {
        for (i in steps.indices) {
            current = i
            delay(700)
        }
        delay(300)
        onDone()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Bg900).padding(48.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Syncing your library…", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text("This usually takes under a minute.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(28.dp))

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(8.dp)
                .background(Bg700, RoundedCornerShape(4.dp))
                .border(1.dp, GlassStroke, RoundedCornerShape(4.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .background(Teal400, RoundedCornerShape(4.dp)),
            )
        }
        Spacer(Modifier.height(28.dp))

        GlassCard(modifier = Modifier.fillMaxWidth(0.6f)) {
            Column {
                steps.forEachIndexed { index, step ->
                    StepRow(step, when {
                        index < current -> StepState.Done
                        index == current -> StepState.Active
                        else -> StepState.Pending
                    })
                    if (index != steps.lastIndex) Spacer(Modifier.height(12.dp))
                }
            }
        }

        if (errored) {
            Spacer(Modifier.height(20.dp))
            ErrorBanner("Could not reach provider. Check the URL and try again.")
            Spacer(Modifier.height(12.dp))
            PrimaryButton(text = "Back to login", onClick = onError)
        }
    }
}

private enum class StepState { Pending, Active, Done }

@Composable
private fun StepRow(step: Step, state: StepState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(28.dp).background(Bg700, RoundedCornerShape(8.dp)).border(1.dp, GlassStroke, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                StepState.Done -> Icon(Icons.Default.CheckCircle, null, tint = Green500)
                StepState.Active -> Box(modifier = Modifier.size(10.dp).background(Teal400, RoundedCornerShape(5.dp)))
                StepState.Pending -> Box(modifier = Modifier.size(8.dp).background(TextMuted, RoundedCornerShape(4.dp)))
            }
        }
        Spacer(Modifier.size(14.dp))
        Column {
            Text(step.label, color = if (state == StepState.Pending) TextMuted else TextPrimary, style = MaterialTheme.typography.titleSmall)
            Text(step.detail, color = TextMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}
