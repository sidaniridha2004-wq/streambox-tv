package com.streambox.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.streambox.tv.nav.AdaptiveAppShell
import com.streambox.tv.ui.theme.AuraTheme
import com.streambox.tv.ui.theme.LocalIsTv
import com.streambox.tv.ui.theme.isAndroidTv
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isTv = isAndroidTv(this)
            AuraTheme {
                CompositionLocalProvider(LocalIsTv provides isTv) {
                    val windowSize = calculateWindowSizeClass(this)
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AdaptiveAppShell(windowSize = windowSize, isTv = isTv)
                    }
                }
            }
        }
    }
}
