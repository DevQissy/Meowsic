package com.devqissy.meowsic.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devqissy.meowsic.R
import com.devqissy.meowsic.ui.theme.AppThemeColors
import com.devqissy.meowsic.ui.theme.glassPanelColor
import com.devqissy.meowsic.ui.theme.glassSheen
import com.devqissy.meowsic.ui.theme.glassSurface
import com.devqissy.meowsic.ui.theme.isDarkTheme
import kotlin.math.PI
import kotlin.math.sin


val DangerRed = Color(0xFFFF6B6B)
val DangerRedDeep = Color(0xFFE5395F)

private fun wave(t: Float): Float = (sin(t * 2f * PI.toFloat()) + 1f) / 2f

fun Modifier.glassSpecular(
    shape: Shape,
    sheenColor: Color,
    topLine: Boolean = true
): Modifier = this
    .clip(shape)
    .drawWithContent {
        drawContent()

        val glossHeight = size.height * 0.45f
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(sheenColor, Color.Transparent),
                startY = 0f,
                endY = glossHeight
            ),
            size = Size(size.width, glossHeight)
        )

        if (topLine) {
            val lineY = 0.75.dp.toPx()
            drawLine(
                brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.5f to sheenColor.copy(alpha = sheenColor.alpha * 0.9f),
                    1f to Color.Transparent
                ),
                start = Offset(size.width * 0.12f, lineY),
                end = Offset(size.width * 0.88f, lineY),
                strokeWidth = 1.dp.toPx()
            )
        }
    }

@Composable
fun GlassCircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    themeColors: AppThemeColors,
    size: Dp = 42.dp,
    iconSize: Dp = 19.dp
) {
    GlassCircleIconButton(
        icon = rememberVectorPainter(icon),
        contentDescription = contentDescription,
        onClick = onClick,
        themeColors = themeColors,
        size = size,
        iconSize = iconSize
    )
}

@Composable
fun GlassCircleIconButton(
    icon: Painter,
    contentDescription: String?,
    onClick: () -> Unit,
    themeColors: AppThemeColors,
    size: Dp = 42.dp,
    iconSize: Dp = 19.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "circleBtnScale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .size(size)
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                spotColor = themeColors.primary.copy(alpha = 0.30f),
                ambientColor = Color.Transparent
            )
            .glassSurface(
                shape = CircleShape,
                fillColor = themeColors.glassWhite,
                borderColor = lerp(themeColors.glassBorder, themeColors.primary, 0.35f),
                sheenColor = themeColors.glassSheen
            )
            .glassSpecular(
                shape = CircleShape,
                sheenColor = themeColors.glassSheen,
                topLine = false
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = icon,
            contentDescription = contentDescription,
            tint = lerp(themeColors.textPrimary, themeColors.primary, 0.55f),
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun GradientCircleButton(
    icon: Painter,
    contentDescription: String?,
    onClick: () -> Unit,
    themeColors: AppThemeColors,
    size: Dp = 42.dp,
    iconSize: Dp = 19.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "gradientBtnScale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .size(size)
            .shadow(
                elevation = 10.dp,
                shape = CircleShape,
                spotColor = lerp(themeColors.primary, themeColors.gradientEnd, 0.5f)
                    .copy(alpha = 0.55f),
                ambientColor = Color.Transparent
            )
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(themeColors.primary, themeColors.gradientEnd)
                )
            )
            .glassSpecular(
                shape = CircleShape,
                sheenColor = Color.White.copy(alpha = 0.30f),
                topLine = false
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = icon,
            contentDescription = contentDescription,
            tint = themeColors.onPrimary,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun AmbientGlow(themeColors: AppThemeColors) {
    val transition = rememberInfiniteTransition(label = "ambientGlow")
    val t1 by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(12000, easing = LinearEasing)),
        label = "glowT1"
    )
    val t2 by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "glowT2"
    )
    val t3 by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(15000, easing = LinearEasing)),
        label = "glowT3"
    )

    val baseWhite = if (themeColors.isDarkTheme) {
        themeColors.glassWhiteStrong
    } else {
        themeColors.glassWhite
    }
    val glowColor = lerp(baseWhite, themeColors.primary, 0.30f)
    val primaryGlow = lerp(themeColors.primary, themeColors.gradientEnd, 0.35f).copy(
        alpha = if (themeColors.isDarkTheme) 0.45f else 0.34f
    )
    val endGlow = themeColors.gradientEnd.copy(
        alpha = if (themeColors.isDarkTheme) 0.40f else 0.32f
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset {
                    IntOffset(
                        (30 + wave(t1) * 55f).dp.roundToPx(),
                        (-60 + wave(t2) * 35f).dp.roundToPx()
                    )
                }
                .size(300.dp)
                .background(
                    brush = Brush.radialGradient(colors = listOf(glowColor, Color.Transparent)),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset {
                    IntOffset(
                        (-100 + wave(t2) * 45f).dp.roundToPx(),
                        (-30 + wave(t3) * 70f).dp.roundToPx()
                    )
                }
                .size(300.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryGlow, Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset {
                    IntOffset(
                        (15 + wave(t3) * 45f).dp.roundToPx(),
                        (30 + wave(t1) * 55f).dp.roundToPx()
                    )
                }
                .size(300.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(endGlow, Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )
    }
}

@Composable
fun MiniEqualizer(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.height(12.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (isPlaying) {
            val transition = rememberInfiniteTransition(label = "miniEq")
            val phases = listOf(640, 880, 1100, 780).map { duration ->
                transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(duration, easing = LinearEasing)),
                    label = "eqBar$duration"
                )
            }

            phases.forEach { state ->
                val w = (sin(state.value * 2f * PI.toFloat()) + 1f) / 2f
                val barHeight = (3f + w * 8.5f).dp

                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(barHeight)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    lerp(color, Color.White, 0.35f),
                                    color
                                )
                            )
                        )
                )
            }
        } else {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color.copy(alpha = 0.5f))
                )
            }
        }
    }
}

@Composable
fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    themeColors: AppThemeColors,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    iconSize: Dp = 22.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.86f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "playPauseScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .size(size)
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                spotColor = lerp(themeColors.primary, themeColors.gradientEnd, 0.5f)
                    .copy(alpha = 0.60f),
                ambientColor = Color.Transparent
            )
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(themeColors.primary, themeColors.gradientEnd)
                )
            )
            .glassSpecular(
                shape = CircleShape,
                sheenColor = Color.White.copy(alpha = 0.30f),
                topLine = false
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = isPlaying,
            animationSpec = tween(180),
            label = "playPauseIcon"
        ) { playing ->
            Icon(
                painter = painterResource(
                    if (playing) R.drawable.pause else R.drawable.play
                ),
                contentDescription = null,
                tint = themeColors.onPrimary,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
fun VerticalScrollbar(
    listState: LazyListState,
    thumbColor: Color,
    thumbWidth: Dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val thumbWidthPx = with(density) { thumbWidth.toPx() }
    val minThumbHeightPx = with(density) { 32.dp.toPx() }

    val touchAreaWidth = thumbWidth + 12.dp

    var trackHeightPx by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val geometry by remember(listState, minThumbHeightPx) {
        derivedStateOf {
            computeScrollbarGeometry(
                listState = listState,
                trackHeightPx = trackHeightPx,
                minThumbHeightPx = minThumbHeightPx
            )
        }
    }

    val scrollAlpha by animateFloatAsState(
        targetValue = if (listState.isScrollInProgress || isDragging) 1f else 0f,
        animationSpec = tween(350),
        label = "scrollbarAlpha"
    )

    val thumbGrow by animateFloatAsState(
        targetValue = if (isDragging) 1.8f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "scrollbarGrow"
    )

    Box(
        modifier = modifier
            .width(touchAreaWidth)
            .onSizeChanged { trackHeightPx = it.height.toFloat() }
            .pointerInput(listState, minThumbHeightPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)

                    val pressedGeometry = computeScrollbarGeometry(
                        listState = listState,
                        trackHeightPx = trackHeightPx,
                        minThumbHeightPx = minThumbHeightPx
                    )

                    if (pressedGeometry == null) return@awaitEachGesture

                    val g = pressedGeometry
                    val pressedOnThumb =
                        down.position.y >= g.thumbY &&
                                down.position.y <= g.thumbY + g.thumbHeight

                    if (!pressedOnThumb) {
                        val targetThumbY =
                            (down.position.y - g.thumbHeight / 2f)
                                .coerceIn(0f, g.scrollableThumbRange)
                        val targetScroll =
                            (targetThumbY / g.scrollableThumbRange) * g.maxScroll
                        listState.dispatchRawDelta(
                            targetScroll - currentListScrollOffset(listState)
                        )
                    }

                    down.consume()
                    isDragging = true
                    try {
                        verticalDrag(down.id) { change ->
                            val deltaY = change.positionChange().y
                            change.consume()

                            if (deltaY != 0f) {
                                val cg = computeScrollbarGeometry(
                                    listState = listState,
                                    trackHeightPx = trackHeightPx,
                                    minThumbHeightPx = minThumbHeightPx
                                )
                                if (cg != null) {
                                    val scrollPerThumbPx =
                                        cg.maxScroll / cg.scrollableThumbRange
                                    listState.dispatchRawDelta(deltaY * scrollPerThumbPx)
                                }
                            }
                        }
                    } finally {
                        isDragging = false
                    }
                }
            }
            .drawBehind {
                if (scrollAlpha > 0.02f) {
                    val currentGeometry = geometry ?: return@drawBehind

                    val drawWidthPx = thumbWidthPx * thumbGrow
                    val left = size.width - drawWidthPx

                    drawRoundRect(
                        color = thumbColor.copy(
                            alpha = thumbColor.alpha * 0.20f * scrollAlpha
                        ),
                        topLeft = Offset(left, 0f),
                        size = Size(drawWidthPx, size.height),
                        cornerRadius = CornerRadius(drawWidthPx / 2f)
                    )

                    drawRoundRect(
                        color = thumbColor.copy(
                            alpha = thumbColor.alpha * scrollAlpha
                        ),
                        topLeft = Offset(left, currentGeometry.thumbY),
                        size = Size(drawWidthPx, currentGeometry.thumbHeight),
                        cornerRadius = CornerRadius(drawWidthPx / 2f)
                    )
                }
            }
    )
}

private data class ScrollbarGeometry(
    val thumbY: Float,
    val thumbHeight: Float,
    val maxScroll: Float,
    val scrollableThumbRange: Float
)

private fun computeScrollbarGeometry(
    listState: LazyListState,
    trackHeightPx: Float,
    minThumbHeightPx: Float
): ScrollbarGeometry? {
    if (trackHeightPx <= 0f) return null

    val layoutInfo = listState.layoutInfo
    val totalItemsCount = layoutInfo.totalItemsCount
    val viewportHeight = layoutInfo.viewportSize.height.toFloat()
    val visibleItems = layoutInfo.visibleItemsInfo

    if (totalItemsCount == 0 || visibleItems.isEmpty() || viewportHeight <= 0f) return null

    val itemHeight = visibleItems.first().size.toFloat()
    if (itemHeight <= 0f) return null

    val totalContentHeight = itemHeight * totalItemsCount
    if (totalContentHeight <= viewportHeight) return null

    val firstVisible = visibleItems.first()
    val scrollOffset = (firstVisible.index * itemHeight) - firstVisible.offset

    val thumbHeight = (trackHeightPx / totalContentHeight * trackHeightPx)
        .coerceIn(minThumbHeightPx, trackHeightPx)

    val maxScroll = totalContentHeight - viewportHeight
    val scrolledFraction = (scrollOffset / maxScroll).coerceIn(0f, 1f)
    val scrollableThumbRange = (trackHeightPx - thumbHeight).coerceAtLeast(1f)

    return ScrollbarGeometry(
        thumbY = scrolledFraction * scrollableThumbRange,
        thumbHeight = thumbHeight,
        maxScroll = maxScroll,
        scrollableThumbRange = scrollableThumbRange
    )
}

private fun currentListScrollOffset(listState: LazyListState): Float {
    val first = listState.layoutInfo.visibleItemsInfo.firstOrNull() ?: return 0f
    val itemHeight = first.size.toFloat()
    return (first.index * itemHeight) - first.offset
}

@Composable
fun SectionTitle(
    title: String,
    themeColors: AppThemeColors,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 14.dp, height = 3.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(
                        listOf(themeColors.primary, themeColors.gradientEnd)
                    )
                )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = title,
            color = lerp(themeColors.textSecondary, themeColors.primary, 0.35f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun GlassCard(
    themeColors: AppThemeColors,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .glassSurface(
                shape = RoundedCornerShape(22.dp),
                fillColor = themeColors.glassPanelColor,
                borderColor = lerp(themeColors.glassBorder, themeColors.primary, 0.25f),
                sheenColor = themeColors.glassSheen
            )
            .glassSpecular(
                shape = RoundedCornerShape(22.dp),
                sheenColor = themeColors.glassSheen,
                topLine = false
            ),
        content = content
    )
}

@Composable
fun AccentDivider(
    themeColors: AppThemeColors,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(vertical = 12.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.5f to lerp(themeColors.glassBorder, themeColors.primary, 0.60f),
                    1f to Color.Transparent
                )
            )
    )
}