package com.antigravity.iptv.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.iptv.ui.theme.AuraCyan
import com.antigravity.iptv.ui.theme.AuraPurple

@Composable
fun LanguageSelectionScreen(
    onLanguageSelected: (String) -> Unit
) {
    var selectedLanguage by remember { mutableStateOf("en") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Aura TV Branding
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 48.dp)
            ) {
                Text(
                    text = "Aura ",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 32.sp
                )
                Text(
                    text = "TV",
                    color = AuraPurple,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Titles in all 3 languages (since stringResource is not ready yet)
            Text(
                text = "Choose your language",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Choisissez votre langue",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "اختر لغتك",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            // Language Cards
            LanguageCard(
                languageName = "English",
                flag = "🇬🇧",
                isSelected = selectedLanguage == "en",
                onClick = { selectedLanguage = "en" }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LanguageCard(
                languageName = "Français",
                flag = "🇫🇷",
                isSelected = selectedLanguage == "fr",
                onClick = { selectedLanguage = "fr" }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LanguageCard(
                languageName = "العربية",
                flag = "🇸🇦",
                isSelected = selectedLanguage == "ar",
                onClick = { selectedLanguage = "ar" }
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Continue Button (multilingual)
            val buttonText = when (selectedLanguage) {
                "fr" -> "Continuer"
                "ar" -> "متابعة"
                else -> "Continue"
            }
            
            Button(
                onClick = { onLanguageSelected(selectedLanguage) },
                colors = ButtonDefaults.buttonColors(containerColor = AuraPurple),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = buttonText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun LanguageCard(
    languageName: String,
    flag: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) AuraCyan else Color.Transparent
    val backgroundColor = if (isSelected) AuraPurple.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = BorderStroke(if (isSelected) 2.dp else 0.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = flag,
                fontSize = 28.sp,
                modifier = Modifier.padding(end = 16.dp)
            )
            Text(
                text = languageName,
                fontSize = 20.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            
            // Checkbox/Radio indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(AuraCyan, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color.White, RoundedCornerShape(5.dp))
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.Transparent, RoundedCornerShape(12.dp))
                )
            }
        }
    }
}
