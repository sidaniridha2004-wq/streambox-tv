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
import com.streambox.tv.ui.theme.Bg900
import com.streambox.tv.ui.theme.TextPrimary
import com.streambox.tv.ui.theme.TextSecondary

@Composable
fun StalkerFormScreen(onSubmit: () -> Unit, onBack: () -> Unit, vm: AuthViewModel = hiltViewModel()) {
    var portal by remember { mutableStateOf("") }
    var mac by remember { mutableStateOf("00:1A:79:") }
    var device by remember { mutableStateOf("") }
    var serial by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg900)
            .verticalScroll(rememberScrollState())
            .padding(40.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) { GhostButton(text = "Back", onClick = onBack) }
        Spacer(Modifier.height(20.dp))
        Text("Add Stalker portal", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text("Authentication uses your MAC address. Device ID and serial are sometimes required.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))
        GlassCard(modifier = Modifier.fillMaxWidth(0.7f)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                LabeledInput(label = "Portal URL", value = portal, onValueChange = { portal = it }, placeholder = "http://portal.example.tv/c/", keyboardType = KeyboardType.Uri)
                LabeledInput(label = "MAC address", value = mac, onValueChange = { mac = it }, placeholder = "00:1A:79:XX:XX:XX", helper = "Use uppercase, colon separated")
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LabeledInput(label = "Device ID", value = device, onValueChange = { device = it }, optional = true, modifier = Modifier.weight(1f))
                    LabeledInput(label = "Serial", value = serial, onValueChange = { serial = it }, optional = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PrimaryButton(text = "Save & connect", leadingIcon = Icons.Default.Save, onClick = {
                        vm.addStalker(portal, mac, device.ifBlank { null }, serial.ifBlank { null })
                        onSubmit()
                    })
                }
            }
        }
    }
}
