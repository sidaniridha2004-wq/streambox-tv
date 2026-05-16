package com.streambox.tv.ui.theme

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

private val DarkColors = darkColorScheme(
    primary = Teal400,
    onPrimary = Bg900,
    primaryContainer = Teal500,
    onPrimaryContainer = Bg900,
    secondary = Blue500,
    onSecondary = TextPrimary,
    background = Bg900,
    onBackground = TextPrimary,
    surface = Bg800,
    onSurface = TextPrimary,
    surfaceVariant = Bg700,
    onSurfaceVariant = TextSecondary,
    surfaceContainerHighest = Bg600,
    error = Red500,
    onError = TextPrimary,
    outline = GlassStroke,
    outlineVariant = Divider,
)

val LocalIsTv = compositionLocalOf { false }

fun isAndroidTv(context: Context): Boolean {
    val ui = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    return ui?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
}

@Composable
fun StreamBoxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
