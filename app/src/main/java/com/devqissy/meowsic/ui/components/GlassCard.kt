package com.devqissy.meowsic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devqissy.meowsic.ui.theme.LocalAppThemeColors

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val themeColors = LocalAppThemeColors.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(themeColors.glassWhite)
            .border(
                width = 1.dp,
                color = themeColors.glassBorder,
                shape = RoundedCornerShape(cornerRadius)
            ),
        content = content
    )
}