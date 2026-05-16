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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streambox.tv.data.ProviderStatus
import com.streambox.tv.ui.components.ErrorBanner
import com.streambox.tv.ui.components.GlassCard
import com.streambox.tv.ui.components.PrimaryButton
import com.streambox.tv.ui.theme.Bg700
import com.streambox.tv.ui.theme.Bg900
import com.streambox.tv.ui.theme.GlassStroke
import com.streambox.tv.ui.theme.Green500
import com.streambox.tv.ui.theme.Red500
import com.streambox.tv.ui.theme.Teal400
import com.streambox.tv.ui.theme.TextMuted
import com.streambox.tv.ui.theme.TextPrimary
import com.streambox.tv.ui.theme.TextSecondary
import kotlinx.coroutines.delay

private data class Step(val label: String, val detail: String)

private val STEPS = listOf(
    Step("Authenticating provider", "Verifying credentials and endpoint…"),
    Step("Downloading playlist", "Fetching M3U / Xtream manifest…"),
    Step("Parsing channels", "Grouping by category and language…"),
    Step("Loading EPG", "Reading XMLTV programs for the next 48h…"),
    Step("Finalizing library", "Indexing channels for fast zapping…"),
)

@Composable
fun SyncingScreen(
    onDone: () -> Unit,
    onError: () -> Unit,
    vm: SyncingViewModel = hiltViewModel(),
) {
    val activeProvider by vm.activeProvider.collectAsStateWithLifecycle()
    val status = activeProvider?.status

    // While the provider is still SYNCING, walk through the cosmetic steps.
    // We hold on the last step until the real fetch finishes.
    var stepIdx by remember { mutableIntStateOf(0) }
    LaunchedEffect(status) {
        if (status == ProviderStatus.SYNCING) {
            for (i in STEPS.indices) {
                stepIdx = i
                delay(550)
            }
            // Stay on the last step until the real status flips
            stepIdx = STEPS.lastIndex
        }
    }

    // React to terminal states
    LaunchedEffect(status) {
        when (status) {
            ProviderStatus.OK -> {
                delay(350)
                onDone()
            }
            else -> Unit
        }
    }

    val progress by animateFloatAsState(
        targetValue = when (status) {
            ProviderStatus.OK -> 1f
            ProviderStatus.FAILED -> (stepIdx + 1f) / STEPS.size
            else -> (stepIdx + 1f) / STEPS.size
        },
        animationSpec = tween(500),
        label = "progress",
    )

    val title = when (status) {
        ProviderStatus.OK -> "Library ready"
        ProviderStatus.FAILED -> "Couldn’t connect"
        else -> "Syncing your library…"
    }
    val subtitle = when (status) {
        ProviderStatus.OK -> "${activeProvider?.channelCount ?: 0} channels imported."
        ProviderStatus.FAILED -> "We hit a snag while talking to your provider."
        else -> "This usually takes under a minute."
    }
    val errorMessage by remember(activeProvider) {
        derivedStateOf {
            if (status == ProviderStatus.FAILED) activeProvider?.lastSync ?: "Unknown error" else null
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Bg900).padding(48.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(28.dp))

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
                    .background(
                        if (status == ProviderStatus.FAILED) Red500 else Teal400,
                        RoundedCornerShape(4.dp),
                    ),
            )
        }
        Spacer(Modifier.height(28.dp))

        GlassCard(modifier = Modifier.fillMaxWidth(0.6f)) {
            Column {
                STEPS.forEachIndexed { index, step ->
                    val state = when {
                        status == ProviderStatus.OK -> StepState.Done
                        status == ProviderStatus.FAILED && index >= stepIdx -> StepState.Pending
                        index < stepIdx -> StepState.Done
                        index == stepIdx -> StepState.Active
                        else -> StepState.Pending
                    }
                    StepRow(step, state)
                    if (index != STEPS.lastIndex) Spacer(Modifier.height(12.dp))
                }
            }
        }

        if (status == ProviderStatus.FAILED) {
            Spacer(Modifier.height(20.dp))
            ErrorBanner(errorMessage ?: "Unknown error.")
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PrimaryButton(text = "Back to login", onClick = onError)
                Spacer(Modifier.width(12.dp))
                // Allow user to continue with sample data so they can still browse the UI
                com.streambox.tv.ui.components.SecondaryButton(
                    text = "Continue with demo data",
                    onClick = onDone,
                )
            }
        }
    }
}

private enum class StepState { Pending, Active, Done }

@Composable
private fun StepRow(step: Step, state: StepState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(Bg700, RoundedCornerShape(8.dp))
                .border(1.dp, GlassStroke, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                StepState.Done -> Icon(Icons.Default.CheckCircle, null, tint = Green500)
                StepState.Active -> Box(
                    modifier = Modifier.size(10.dp).background(Teal400, RoundedCornerShape(5.dp)),
                )
                StepState.Pending -> Box(
                    modifier = Modifier.size(8.dp).background(TextMuted, RoundedCornerShape(4.dp)),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                step.label,
                color = if (state == StepState.Pending) TextMuted else TextPrimary,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(step.detail, color = TextMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}
