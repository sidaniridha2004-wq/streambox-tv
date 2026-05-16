package com.streambox.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.streambox.tv.ui.theme.FocusRing
import com.streambox.tv.ui.theme.GlassFill
import com.streambox.tv.ui.theme.GlassStroke
import com.streambox.tv.ui.theme.Teal400
import com.streambox.tv.ui.theme.Teal500
import com.streambox.tv.ui.theme.TextPrimary

private val PillShape = RoundedCornerShape(999.dp)
private val InkOnTeal = Color(0xFF0A0F14)

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val gradient = Brush.horizontalGradient(listOf(Teal500, Teal400))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(48.dp)
            .background(gradient, PillShape)
            .border(if (focused) 2.dp else 0.dp, FocusRing, PillShape)
            .clickable(
                enabled = enabled,
                interactionSource = src,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 24.dp),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, null, tint = InkOnTeal)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = InkOnTeal, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(48.dp)
            .background(GlassFill, PillShape)
            .border(1.dp, if (focused) FocusRing else GlassStroke, PillShape)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 24.dp),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, null, tint = TextPrimary)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = TextPrimary, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(40.dp)
            .border(1.dp, if (focused) FocusRing else Color.Transparent, PillShape)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp),
    ) {
        Text(text, color = TextPrimary, style = MaterialTheme.typography.labelLarge)
    }
}
