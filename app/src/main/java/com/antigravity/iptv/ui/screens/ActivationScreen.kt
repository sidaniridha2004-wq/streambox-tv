package com.antigravity.iptv.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.iptv.ui.theme.AuraCyan
import com.antigravity.iptv.ui.theme.AuraPurple
import com.antigravity.iptv.ui.theme.AuraBlue
import com.antigravity.iptv.ui.theme.bounceClick
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import com.antigravity.iptv.R

@Composable
fun ActivationScreen(
    onActivated: () -> Unit
) {
    val context = LocalContext.current

    var activationCode by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var isActivating by remember { mutableStateOf(false) }

    // Staggered entrance animations
    var showLogo by remember { mutableStateOf(true) }
    var showTitle by remember { mutableStateOf(true) }
    var showInput by remember { mutableStateOf(true) }
    var showButton by remember { mutableStateOf(true) }
    var showDeviceId by remember { mutableStateOf(true) }

    // Get or generate device ID
    val sharedPrefs = context.getSharedPreferences("aura_tv_prefs", Context.MODE_PRIVATE)
    val deviceId = remember {
        sharedPrefs.getString("device_id", null) ?: run {
            val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            val id = (1..16).map { chars.random() }.joinToString("")
            sharedPrefs.edit().putString("device_id", id).apply()
            id
        }
    }

    fun activate() {
        if (activationCode.trim() == "2026") {
            isActivating = true
            onActivated()
        } else {
            showError = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF06091A),
                        Color(0xFF0D1225),
                        Color(0xFF06091A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Logo
            AnimatedVisibility(
                visible = showLogo,
                enter = fadeIn(tween(800)) + slideInVertically(tween(800)) { -it / 2 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // TV antenna icon using text
                    Text(
                        text = "📡",
                        fontSize = 36.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Aura",
                            color = AuraCyan,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " TV",
                            color = AuraPurple,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Title
            AnimatedVisibility(
                visible = showTitle,
                enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 3 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.activation_required),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.activation_enter_code),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Input
            AnimatedVisibility(
                visible = showInput,
                enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 3 }
            ) {
                OutlinedTextField(
                    value = activationCode,
                    onValueChange = {
                        activationCode = it
                        showError = false
                    },
                    placeholder = {
                        Text(stringResource(R.string.activation_code_hint), color = Color.White.copy(alpha = 0.3f))
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { activate() }),
                    isError = showError,
                    supportingText = if (showError) {
                        { Text(stringResource(R.string.activation_invalid_code), color = MaterialTheme.colorScheme.error) }
                    } else null,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        errorBorderColor = MaterialTheme.colorScheme.error,
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = AuraCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Button
            AnimatedVisibility(
                visible = showButton,
                enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 3 }
            ) {
                Button(
                    onClick = { activate() },
                    enabled = !isActivating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuraCyan.copy(alpha = 0.15f),
                        contentColor = AuraCyan
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp
                    )
                ) {
                    if (isActivating) {
                        CircularProgressIndicator(
                            color = AuraCyan,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.activate_now),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Device ID
            AnimatedVisibility(
                visible = showDeviceId,
                enter = fadeIn(tween(800))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    Text(
                        text = stringResource(R.string.device_id_label),
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 10.sp,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = deviceId,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 2.sp
                    )
                }
            }
        }
    }
}
