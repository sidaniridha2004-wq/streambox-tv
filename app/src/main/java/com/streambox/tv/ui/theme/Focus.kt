package com.streambox.tv.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.dp

/** D-pad / TV friendly focus ring. Use on every focusable card. */
fun Modifier.tvFocusable(
    shape: Shape = RoundedCornerShape(14.dp),
    interactionSource: MutableInteractionSource? = null,
): Modifier = composed {
    val src = interactionSource ?: remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val ringColor = if (focused) FocusRing else androidx.compose.ui.graphics.Color.Transparent
    this.then(
        Modifier.border(
            width = if (focused) 2.dp else 0.dp,
            brush = SolidColor(ringColor.takeOrElse { FocusRing }),
            shape = shape,
        )
    )
}
