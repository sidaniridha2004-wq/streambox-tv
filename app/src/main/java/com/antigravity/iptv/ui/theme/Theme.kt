package com.antigravity.iptv.ui.theme

import android.app.Activity
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Aura TV Dark Theme - matches the neon cyan/purple logo on deep navy
private val DarkColorScheme = darkColorScheme(
    primary = AuraCyan,
    onPrimary = BlackBackground,
    primaryContainer = AuraCyan.copy(alpha = 0.15f),
    onPrimaryContainer = AuraCyan,
    
    secondary = AuraPurple,
    onSecondary = WhiteBackground,
    secondaryContainer = AuraPurple.copy(alpha = 0.15f),
    onSecondaryContainer = AuraPurple,
    
    tertiary = AuraBlue,
    onTertiary = TextPrimary,

    background = BlackBackground,
    onBackground = TextPrimary,
    
    surface = DarkSurface,
    onSurface = TextPrimary,
    
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    
    error = Color(0xFFFF5C5C),
    onError = WhiteBackground,
    errorContainer = Color(0xFFFF5C5C).copy(alpha = 0.12f),
    onErrorContainer = Color(0xFFFF5C5C),
    
    outline = DividerColor,
    outlineVariant = DividerColor
)

// Aura TV Light Theme
private val LightColorScheme = lightColorScheme(
    primary = AuraCyan,
    onPrimary = WhiteBackground,
    primaryContainer = AuraCyan.copy(alpha = 0.1f),
    onPrimaryContainer = AuraCyan,

    secondary = AuraPurple,
    onSecondary = WhiteBackground,

    tertiary = AuraBlue,
    onTertiary = TextPrimaryLight,

    background = WhiteBackground,
    onBackground = TextPrimaryLight,

    surface = LightSurface,
    onSurface = TextPrimaryLight,

    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,

    error = Color(0xFFE53935),
    onError = WhiteBackground,
    errorContainer = Color(0xFFE53935).copy(alpha = 0.1f),
    onErrorContainer = Color(0xFFE53935),

    outline = Color(0xFFCDD2E0),
    outlineVariant = Color(0xFFE0E4EE)
)

@Composable
fun IPTVPlayerTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
