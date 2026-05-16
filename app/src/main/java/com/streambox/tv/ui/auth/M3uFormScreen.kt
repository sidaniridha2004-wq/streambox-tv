package com.streambox.tv.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.streambox.tv.ui.components.GhostButton
import com.streambox.tv.ui.components.GlassCard
import com.streambox.tv.ui.components.LabeledInput
import com.streambox.tv.ui.components.PrimaryButton
import com.streambox.tv.ui.components.SegmentedTabs
import com.streambox.tv.ui.theme.Bg900
import com.streambox.tv.ui.theme.TextPrimary
import com.streambox.tv.ui.theme.TextSecondary

private const val TAB_M3U = "M3U URL"
private const val TAB_XTREAM = "Xtream Codes"

@Composable
fun M3uFormScreen(onSubmit: () -> Unit, onBack: () -> Unit, vm: AuthViewModel = hiltViewModel()) {
    var tab by remember { mutableStateOf(TAB_M3U) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg900)
            .verticalScroll(rememberScrollState())
            .padding(40.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) { GhostButton(text = "Back", onClick = onBack) }
        Spacer(Modifier.height(20.dp))
        Text("Add an M3U source", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text(
            "Two formats are supported: a direct M3U/M3U8 playlist URL, or Xtream Codes credentials (host + username + password).",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(20.dp))
        SegmentedTabs(
            items = listOf(TAB_M3U, TAB_XTREAM),
            selected = tab,
            onSelected = { tab = it },
            modifier = Modifier.fillMaxWidth(0.7f),
        )
        Spacer(Modifier.height(20.dp))
        when (tab) {
            TAB_XTREAM -> XtreamForm(vm = vm, onSubmit = onSubmit)
            else -> M3uUrlForm(vm = vm, onSubmit = onSubmit)
        }
    }
}

@Composable
private fun M3uUrlForm(vm: AuthViewModel, onSubmit: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var epg by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    GlassCard(modifier = Modifier.fillMaxWidth(0.7f)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LabeledInput("Playlist name", name, { name = it }, placeholder = "e.g. MyIPTV Pro")
            LabeledInput("M3U URL", url, { url = it }, placeholder = "https://example.com/get.php?...", keyboardType = KeyboardType.Uri)
            LabeledInput("EPG URL", epg, { epg = it }, placeholder = "https://example.com/epg.xml.gz", optional = true, keyboardType = KeyboardType.Uri)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LabeledInput("Username", user, { user = it }, optional = true, modifier = Modifier.weight(1f))
                LabeledInput("Password", pass, { pass = it }, optional = true, isPassword = true, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            PrimaryButton(text = "Save & sync", leadingIcon = Icons.Default.Save, onClick = {
                vm.addM3u(name, url, epg.ifBlank { null }, user.ifBlank { null }, pass.ifBlank { null })
                onSubmit()
            })
        }
    }
}

@Composable
private fun XtreamForm(vm: AuthViewModel, onSubmit: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("m3u8") }

    GlassCard(modifier = Modifier.fillMaxWidth(0.7f)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LabeledInput(
                label = "Provider name",
                value = name,
                onValueChange = { name = it },
                placeholder = "e.g. MyIPTV Pro",
            )
            LabeledInput(
                label = "Server URL",
                value = host,
                onValueChange = { host = it },
                placeholder = "http://your-host.tv:8080",
                helper = "Include the port if your provider uses one (e.g. :8080, :2095, :25461).",
                keyboardType = KeyboardType.Uri,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LabeledInput("Username", user, { user = it }, modifier = Modifier.weight(1f))
                LabeledInput("Password", pass, { pass = it }, isPassword = true, modifier = Modifier.weight(1f))
            }
            LabeledInput(
                label = "Output format",
                value = output,
                onValueChange = { output = it },
                placeholder = "m3u8 / ts",
                helper = "Most providers use m3u8 for HLS. Use ts only if your provider requires it.",
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "We will build the playlist URL automatically: <host>/get.php?username=…&password=…&type=m3u_plus&output=$output, plus the EPG endpoint /xmltv.php for the program guide.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))
            PrimaryButton(text = "Connect Xtream", leadingIcon = Icons.Default.Save, onClick = {
                vm.addXtream(name, host, user, pass, output.ifBlank { "m3u8" })
                onSubmit()
            })
        }
    }
}
