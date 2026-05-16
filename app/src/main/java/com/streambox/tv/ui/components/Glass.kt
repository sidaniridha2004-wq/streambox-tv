package com.streambox.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.streambox.tv.ui.theme.GlassFill
import com.streambox.tv.ui.theme.GlassStroke

/** Premium "glassy" surface: subtle vertical gradient, hairline stroke, rounded corners. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(16.dp),
    content: @Composable () -> Unit,
) {
    val gradient = Brush.verticalGradient(
        listOf(Color(0x14FFFFFF), Color(0x06FFFFFF))
    )
    Box(
        modifier = modifier
            .background(GlassFill, shape)
            .background(gradient, shape)
            .border(1.dp, GlassStroke, shape)
            .padding(contentPadding)
    ) { content() }
}
