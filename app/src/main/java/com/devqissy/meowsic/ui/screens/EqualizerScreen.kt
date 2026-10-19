package com.devqissy.meowsic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devqissy.meowsic.audio.AudioEffectsManager
import com.devqissy.meowsic.audio.ReverbPreset
import com.devqissy.meowsic.ui.theme.AppThemeColors
import com.devqissy.meowsic.ui.theme.GlassShapes
import com.devqissy.meowsic.ui.theme.LocalAppThemeColors
import com.devqissy.meowsic.ui.theme.glassPanelColor
import com.devqissy.meowsic.ui.theme.glassSheen
import com.devqissy.meowsic.ui.theme.glassSurface
import com.devqissy.meowsic.ui.theme.isDarkTheme
import com.devqissy.meowsic.R
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun EqualizerScreen(
    audioEffectsManager: AudioEffectsManager,
    onReverbChange: (enabled: Boolean, presetId: String) -> Unit,
    onResetAll: () -> Unit,
    onBack: () -> Unit
) {
    val eqState by audioEffectsManager.eqState.collectAsStateWithLifecycle()
    val effectsState by audioEffectsManager.effectsState.collectAsStateWithLifecycle()
    val eqAvailable by audioEffectsManager.eqAvailable.collectAsStateWithLifecycle()
    val themeColors = LocalAppThemeColors.current
    val accent = themeColors.primary

    val customPresets = listOf(
        stringResource(R.string.normal),
        stringResource(R.string.pop),
        stringResource(R.string.rock),
        stringResource(R.string.classical),
        stringResource(R.string.jazz),
        stringResource(R.string.heavy_bass),
        stringResource(R.string.flat_sound)
    )

    val customPresetValues = mapOf(
        stringResource(R.string.normal) to listOf(0f, 0f, 0f, 0f, 0f),
        stringResource(R.string.pop) to listOf(-1f, 2f, 4f, 3f, 1f),
        stringResource(R.string.rock) to listOf(4f, 2f, -1f, 2f, 4f),
        stringResource(R.string.classical) to listOf(3f, 2f, 0f, 2f, 3f),
        stringResource(R.string.jazz) to listOf(2f, 1f, 3f, 2f, 1f),
        stringResource(R.string.heavy_bass) to listOf(6f, 4f, 0f, -1f, -1f),
        stringResource(R.string.flat_sound) to listOf(-2f, 0f, 2f, 3f, 2f)
    )

    val activePresetName = remember(eqState.bandLevels, eqState.numBands) {
        customPresetValues.entries.firstOrNull { (_, levels) ->
            eqState.bandLevels.take(levels.size) == levels
        }?.key
    }

    val reverbPresets = remember { ReverbPreset.entries.toList() }
    val reverbIds = remember(reverbPresets) { reverbPresets.map { it.id } }
    val reverbDisplayNames = reverbPresets.map { stringResource(it.labelRes) }

    var showResetConfirmation by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        themeColors.gradientStart,
                        themeColors.gradientMiddle,
                        themeColors.gradientEnd
                    )
                )
            )
    ) {
        EqualizerAmbientGlow(themeColors)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                )
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 10.dp)
                    .glassSurface(
                        shape = GlassShapes.header,
                        fillColor = themeColors.glassPanelColor,
                        borderColor = lerp(themeColors.glassBorder, accent, 0.30f),
                        sheenColor = themeColors.glassSheen
                    )
                    .glassSpecular(
                        shape = GlassShapes.header,
                        sheenColor = themeColors.glassSheen
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassCircleIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        onClick = onBack,
                        themeColors = themeColors
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = stringResource(R.string.audio_effects),
                        color = themeColors.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            EqSectionTitle(stringResource(R.string.eq_presets), themeColors)

            EqGlassCard(themeColors) {
                Column(modifier = Modifier.padding(12.dp)) {
                    EqChipGrid(
                        items = customPresets,
                        isSelected = { idx -> customPresets.getOrNull(idx) == activePresetName },
                        onSelect = { idx ->
                            val preset = customPresets[idx]
                            customPresetValues[preset]?.forEachIndexed { i, v ->
                                if (i < eqState.numBands) audioEffectsManager.setBandLevel(i, v)
                            }
                        },
                        themeColors = themeColors
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            EqSectionTitle(stringResource(R.string.equalizer), themeColors)

            EqGlassCard(themeColors) {
                if (eqAvailable && eqState.numBands > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        repeat(eqState.numBands) { i ->
                            val freqLabel = if (eqState.bandFrequencies.size > i) {
                                val hz = eqState.bandFrequencies[i]
                                if (hz >= 1000) "${hz / 1000}k" else "$hz"
                            } else "${i + 1}"
                            val level = eqState.bandLevels.getOrElse(i) { 0f }

                            VerticalBandSlider(
                                label = freqLabel,
                                value = level,
                                minDb = eqState.bandLevelRange.first,
                                maxDb = eqState.bandLevelRange.second,
                                onValueChange = { newValue ->
                                    audioEffectsManager.setBandLevel(i, newValue)
                                },
                                themeColors = themeColors
                            )
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.equalizer_unavailable),
                        color = themeColors.textDisabled,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp, horizontal = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            EqSectionTitle(stringResource(R.string.sound_boost), themeColors)

            EqGlassCard(themeColors) {
                Column(modifier = Modifier.padding(16.dp)) {

                    EffectSliderRow(
                        icon = painterResource(R.drawable.bass),
                        title = stringResource(R.string.bass_boost),
                        subtitle = "${(effectsState.bassBoostStrength * 100).toInt()}%",
                        enabled = effectsState.bassBoostEnabled,
                        value = effectsState.bassBoostStrength,
                        onToggle = {
                            audioEffectsManager.setBassBoost(it, effectsState.bassBoostStrength)
                        },
                        onValueChange = {
                            audioEffectsManager.setBassBoost(effectsState.bassBoostEnabled, it)
                        },
                        themeColors = themeColors
                    )

                    EqAccentDivider(themeColors)

                    EffectSliderRow(
                        icon = painterResource(R.drawable.spatial),
                        title = stringResource(R.string.spatial_audio),
                        subtitle = "${(effectsState.virtualizerStrength * 100).toInt()}%",
                        enabled = effectsState.virtualizerEnabled,
                        value = effectsState.virtualizerStrength,
                        onToggle = {
                            audioEffectsManager.setVirtualizer(it, effectsState.virtualizerStrength)
                        },
                        onValueChange = {
                            audioEffectsManager.setVirtualizer(effectsState.virtualizerEnabled, it)
                        },
                        themeColors = themeColors
                    )

                    EqAccentDivider(themeColors)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            EffectTile(
                                icon = painterResource(R.drawable.volume),
                                themeColors = themeColors
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    stringResource(R.string.loudness),
                                    color = themeColors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    stringResource(
                                        R.string.loudness_gain,
                                        effectsState.loudnessGain / 100
                                    ),
                                    color = themeColors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Switch(
                            checked = effectsState.loudnessEnabled,
                            onCheckedChange = {
                                audioEffectsManager.setLoudness(it, effectsState.loudnessGain)
                            },
                            colors = switchColors(themeColors)
                        )
                    }

                    AnimatedVisibility(
                        visible = effectsState.loudnessEnabled,
                        enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                        exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
                    ) {
                        EqStyleHorizontalSlider(
                            value = (effectsState.loudnessGain / 1000f).coerceIn(0f, 1f),
                            onValueChange = {
                                audioEffectsManager.setLoudness(true, (it * 1000f).roundToInt())
                            },
                            themeColors = themeColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .height(48.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            EqSectionTitle(stringResource(R.string.reverb), themeColors)

            EqGlassCard(themeColors) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            EffectTile(
                                icon = painterResource(R.drawable.waves),
                                themeColors = themeColors
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    stringResource(R.string.reverb),
                                    color = themeColors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                val reverbLabelRes = reverbPresets
                                    .firstOrNull { it.id == effectsState.reverbPresetId }?.labelRes

                                Text(
                                    when {
                                        !effectsState.reverbEnabled -> stringResource(R.string.disabled)
                                        reverbLabelRes != null -> stringResource(reverbLabelRes)
                                        else -> stringResource(R.string.reverb)
                                    },
                                    color = if (effectsState.reverbEnabled) accent
                                    else themeColors.textDisabled,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Switch(
                            checked = effectsState.reverbEnabled,
                            onCheckedChange = { onReverbChange(it, effectsState.reverbPresetId) },
                            colors = switchColors(themeColors)
                        )
                    }

                    AnimatedVisibility(
                        visible = effectsState.reverbEnabled,
                        enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                        exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(14.dp))
                            EqAccentDivider(themeColors)
                            Spacer(modifier = Modifier.height(14.dp))

                            EqChipGrid(
                                items = reverbDisplayNames,
                                isSelected = { idx ->
                                    val id = reverbIds[idx]
                                    if (id == ReverbPreset.NONE.id) {
                                        !effectsState.reverbEnabled
                                    } else {
                                        effectsState.reverbEnabled &&
                                                effectsState.reverbPresetId == id
                                    }
                                },
                                onSelect = { idx ->
                                    val id = reverbIds[idx]
                                    if (id == ReverbPreset.NONE.id) {
                                        onReverbChange(false, ReverbPreset.NONE.id)
                                    } else {
                                        onReverbChange(true, id)
                                    }
                                },
                                themeColors = themeColors
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            GlassPillButton(
                text = stringResource(R.string.reset_all),
                icon = painterResource(R.drawable.refresh),
                onClick = { showResetConfirmation = true },
                themeColors = themeColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showResetConfirmation) {
            ResetConfirmationDialog(
                themeColors = themeColors,
                onConfirm = {
                    showResetConfirmation = false
                    onResetAll()
                },
                onDismiss = { showResetConfirmation = false }
            )
        }
    }
}

@Composable
private fun ResetConfirmationDialog(
    themeColors: AppThemeColors,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.reset_all),
                color = themeColors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Text(
                stringResource(R.string.reset_all_confirmation),
                color = themeColors.textPrimary,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFFE5395F)
                )
            ) {
                Text(
                    stringResource(R.string.reset_confirm),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = themeColors.textPrimary
                )
            ) {
                Text(
                    stringResource(R.string.cancel),
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp
                )
            }
        },
        containerColor = themeColors.surface,
        shape = GlassShapes.dialog
    )
}

@Composable
private fun EqChipGrid(
    items: List<String>,
    isSelected: (Int) -> Boolean,
    onSelect: (Int) -> Unit,
    themeColors: AppThemeColors
) {
    Column {
        items.chunked(3).forEachIndexed { rowIdx, row ->
            if (rowIdx > 0) Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEachIndexed { colIdx, preset ->
                    val idx = rowIdx * 3 + colIdx
                    EqPresetChip(
                        label = preset,
                        selected = isSelected(idx),
                        onClick = { onSelect(idx) },
                        modifier = Modifier.weight(1f),
                        themeColors = themeColors
                    )
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun EqPresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    themeColors: AppThemeColors,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "chipScale"
    )

    val g1 by animateColorAsState(
        targetValue = if (selected) themeColors.primary else themeColors.glassWhiteStrong,
        animationSpec = tween(200),
        label = "chipG1"
    )
    val g2 by animateColorAsState(
        targetValue = if (selected) {
            lerp(themeColors.primary, themeColors.gradientEnd, 0.85f)
        } else {
            themeColors.glassWhiteStrong
        },
        animationSpec = tween(200),
        label = "chipG2"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            lerp(themeColors.primary, themeColors.gradientEnd, 0.5f)
        } else {
            lerp(themeColors.glassBorder, themeColors.primary, 0.25f)
        },
        animationSpec = tween(200),
        label = "chipBorder"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) themeColors.onPrimary
        else lerp(themeColors.textSecondary, themeColors.primary, 0.30f),
        animationSpec = tween(200),
        label = "chipText"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .height(38.dp)
            .shadow(
                elevation = if (selected) 8.dp else 0.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = lerp(themeColors.primary, themeColors.gradientEnd, 0.5f)
                    .copy(alpha = 0.45f),
                ambientColor = Color.Transparent
            )
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.horizontalGradient(listOf(g1, g2)))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VerticalBandSlider(
    label: String,
    value: Float,
    minDb: Int,
    maxDb: Int,
    onValueChange: (Float) -> Unit,
    themeColors: AppThemeColors
) {
    val accent = themeColors.primary
    val accent2 = themeColors.gradientEnd
    val haptics = LocalHapticFeedback.current
    var isDragging by remember { mutableStateOf(false) }

    val currentOnValueChange by rememberUpdatedState(onValueChange)

    var lastSnapped by remember { mutableStateOf(false) }

    val snapThreshold = 1.5f

    val sliderHeight = 220.dp
    val rangeSpan = (maxDb - minDb).toFloat().coerceAtLeast(1f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp)
    ) {
        val rounded = value.roundToInt()
        Text(
            text = if (rounded > 0) "+$rounded" else "$rounded",
            color = if (rounded != 0) accent else themeColors.textDisabled,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .width(48.dp)
                .height(sliderHeight)
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = value,
                        range = minDb.toFloat()..maxDb.toFloat()
                    )
                }
                .pointerInput(minDb, maxDb) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.pressed }
                                ?: event.changes.firstOrNull()
                                ?: continue

                            if (change.pressed) {
                                isDragging = true
                                change.consume()
                                val height = size.height.toFloat()
                                val touchY = change.position.y
                                val rawValue = (maxDb - (touchY / height) * rangeSpan)
                                    .coerceIn(minDb.toFloat(), maxDb.toFloat())

                                val snapped = abs(rawValue) <= snapThreshold
                                val newValue = if (snapped) 0f else rawValue

                                if (snapped && !lastSnapped) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                lastSnapped = snapped

                                currentOnValueChange(newValue)
                            } else {
                                isDragging = false
                                lastSnapped = false
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val trackWidth = 6.dp.toPx()
                val centerX = canvasWidth / 2f
                val centerY = canvasHeight / 2f
                val isAtZero = abs(value) < 0.05f

                drawRoundRect(
                    color = themeColors.glassWhiteStrong,
                    topLeft = Offset(centerX - trackWidth / 2, 0f),
                    size = Size(trackWidth, canvasHeight),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )

                val normalized = ((value - minDb) / rangeSpan).coerceIn(0f, 1f)
                val thumbY = canvasHeight * (1 - normalized)

                val fillBrush = Brush.verticalGradient(
                    colors = listOf(accent, accent2)
                )

                if (value > 0f) {
                    drawRoundRect(
                        brush = fillBrush,
                        topLeft = Offset(centerX - trackWidth / 2, thumbY),
                        size = Size(trackWidth, centerY - thumbY),
                        cornerRadius = CornerRadius(3.dp.toPx())
                    )
                } else if (value < 0f) {
                    drawRoundRect(
                        color = accent.copy(alpha = 0.55f),
                        topLeft = Offset(centerX - trackWidth / 2, centerY),
                        size = Size(trackWidth, thumbY - centerY),
                        cornerRadius = CornerRadius(3.dp.toPx())
                    )
                }

                val zeroLineHeight = (if (isAtZero) 2.dp else 1.dp).toPx()
                drawRoundRect(
                    color = accent.copy(alpha = if (isAtZero) 0.9f else 0.45f),
                    topLeft = Offset(0f, centerY - zeroLineHeight / 2),
                    size = Size(canvasWidth, zeroLineHeight),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )

                val thumbWidth = 16.dp.toPx()
                val thumbHeight = 38.dp.toPx()
                val thumbX = centerX - thumbWidth / 2
                val thumbTop = (thumbY - thumbHeight / 2)
                    .coerceIn(0f, canvasHeight - thumbHeight)

                drawRoundRect(
                    color = accent.copy(
                        alpha = when {
                            isDragging -> 0.30f
                            isAtZero -> 0.26f
                            else -> 0.16f
                        }
                    ),
                    topLeft = Offset(thumbX - 3.dp.toPx(), thumbTop - 3.dp.toPx()),
                    size = Size(thumbWidth + 6.dp.toPx(), thumbHeight + 6.dp.toPx()),
                    cornerRadius = CornerRadius(9.dp.toPx())
                )

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lerp(accent, Color.White, 0.75f),
                            accent
                        ),
                        startY = thumbTop,
                        endY = thumbTop + thumbHeight
                    ),
                    topLeft = Offset(thumbX, thumbTop),
                    size = Size(thumbWidth, thumbHeight),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )

                repeat(2) { i ->
                    val gripY = thumbTop + thumbHeight * (0.38f + i * 0.24f)
                    drawLine(
                        color = themeColors.onPrimary.copy(alpha = 0.55f),
                        start = Offset(thumbX + thumbWidth * 0.28f, gripY),
                        end = Offset(thumbX + thumbWidth * 0.72f, gripY),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            color = lerp(themeColors.textSecondary, accent, 0.35f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EqStyleHorizontalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    themeColors: AppThemeColors,
    modifier: Modifier = Modifier
) {
    val accent = themeColors.primary
    val accent2 = themeColors.gradientEnd
    var isDragging by remember { mutableStateOf(false) }
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    Box(
        modifier = modifier
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.pressed }
                            ?: event.changes.firstOrNull()
                            ?: continue
                        if (change.pressed) {
                            isDragging = true
                            change.consume()
                            val newValue = (change.position.x / size.width.toFloat())
                                .coerceIn(0f, 1f)
                            currentOnValueChange(newValue)
                        } else {
                            isDragging = false
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val trackHeight = 6.dp.toPx()
            val centerY = canvasHeight / 2f

            drawRoundRect(
                color = themeColors.glassWhiteStrong,
                topLeft = Offset(0f, centerY - trackHeight / 2),
                size = Size(canvasWidth, trackHeight),
                cornerRadius = CornerRadius(3.dp.toPx())
            )

            val fillWidth = canvasWidth * value.coerceIn(0f, 1f)
            if (fillWidth > 1f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(accent, accent2)),
                    topLeft = Offset(0f, centerY - trackHeight / 2),
                    size = Size(fillWidth, trackHeight),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
            }

            val thumbWidth = 14.dp.toPx()
            val thumbHeight = 34.dp.toPx()
            val thumbX = (fillWidth - thumbWidth / 2).coerceIn(0f, canvasWidth - thumbWidth)
            val thumbTop = centerY - thumbHeight / 2

            drawRoundRect(
                color = accent.copy(alpha = if (isDragging) 0.30f else 0.16f),
                topLeft = Offset(thumbX - 3.dp.toPx(), thumbTop - 3.dp.toPx()),
                size = Size(thumbWidth + 6.dp.toPx(), thumbHeight + 6.dp.toPx()),
                cornerRadius = CornerRadius(9.dp.toPx())
            )

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(lerp(accent, Color.White, 0.75f), accent),
                    startY = thumbTop,
                    endY = thumbTop + thumbHeight
                ),
                topLeft = Offset(thumbX, thumbTop),
                size = Size(thumbWidth, thumbHeight),
                cornerRadius = CornerRadius(7.dp.toPx())
            )

            repeat(2) { i ->
                val gripX = thumbX + thumbWidth * (0.35f + i * 0.30f)
                drawLine(
                    color = themeColors.onPrimary.copy(alpha = 0.55f),
                    start = Offset(gripX, thumbTop + thumbHeight * 0.28f),
                    end = Offset(gripX, thumbTop + thumbHeight * 0.72f),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }
    }
}

@Composable
private fun EffectTile(
    icon: Painter,
    themeColors: AppThemeColors,
    size: Dp = 36.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = themeColors.primary.copy(alpha = 0.40f),
                ambientColor = Color.Transparent
            )
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    listOf(themeColors.primary, themeColors.gradientEnd)
                )
            )
            .glassSpecular(
                shape = RoundedCornerShape(12.dp),
                sheenColor = Color.White.copy(alpha = 0.28f),
                topLine = false
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = themeColors.onPrimary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun EffectSliderRow(
    icon: Painter,
    title: String,
    subtitle: String,
    enabled: Boolean,
    value: Float,
    onToggle: (Boolean) -> Unit,
    onValueChange: (Float) -> Unit,
    themeColors: AppThemeColors
) {
    val contentAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.55f,
        animationSpec = tween(220),
        label = "effectRowAlpha"
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(contentAlpha),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                EffectTile(icon = icon, themeColors = themeColors)

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        title,
                        color = themeColors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        subtitle,
                        color = themeColors.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = switchColors(themeColors)
            )
        }

        AnimatedVisibility(
            visible = enabled,
            enter = expandVertically(tween(250)) + fadeIn(tween(250)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
        ) {
            EqStyleHorizontalSlider(
                value = value.coerceIn(0f, 1f),
                onValueChange = onValueChange,
                themeColors = themeColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(48.dp)
            )
        }
    }
}

@Composable
private fun GlassPillButton(
    text: String,
    icon: Painter,
    onClick: () -> Unit,
    themeColors: AppThemeColors,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pillScale"
    )

    Row(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(50),
                spotColor = themeColors.primary.copy(alpha = 0.20f),
                ambientColor = Color.Transparent
            )
            .clip(RoundedCornerShape(50))
            .background(themeColors.glassWhiteStrong)
            .border(
                1.dp,
                lerp(themeColors.glassBorder, themeColors.primary, 0.35f),
                RoundedCornerShape(50)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = lerp(themeColors.textSecondary, themeColors.primary, 0.45f),
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = text,
            color = lerp(themeColors.textSecondary, themeColors.primary, 0.45f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun switchColors(themeColors: AppThemeColors) = SwitchDefaults.colors(
    checkedThumbColor = themeColors.onPrimary,
    checkedTrackColor = themeColors.primary,
    uncheckedThumbColor = themeColors.textDisabled,
    uncheckedTrackColor = themeColors.glassWhiteStrong
)

@Composable
private fun EqSectionTitle(title: String, themeColors: AppThemeColors) {
    Row(
        modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp),
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
private fun EqGlassCard(themeColors: AppThemeColors, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
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
private fun EqAccentDivider(themeColors: AppThemeColors) {
    Box(
        modifier = Modifier
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

@Composable
private fun GlassCircleIconButton(
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
private fun GlassCircleIconButton(
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

private fun wave(t: Float): Float = (sin(t * 2f * PI.toFloat()) + 1f) / 2f

@Composable
private fun EqualizerAmbientGlow(themeColors: AppThemeColors) {
    val transition = rememberInfiniteTransition(label = "eqGlow")
    val t1 by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(12000, easing = LinearEasing)),
        label = "t1"
    )
    val t2 by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "t2"
    )
    val t3 by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(15000, easing = LinearEasing)),
        label = "t3"
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

private fun Modifier.glassSpecular(
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