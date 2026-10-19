package com.devqissy.meowsic.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devqissy.meowsic.data.db.PlaylistEntity
import com.devqissy.meowsic.data.viewmodel.MusicViewModel
import com.devqissy.meowsic.ui.components.AmbientGlow
import com.devqissy.meowsic.ui.components.DangerRed
import com.devqissy.meowsic.ui.components.DangerRedDeep
import com.devqissy.meowsic.ui.components.GlassCircleIconButton
import com.devqissy.meowsic.ui.components.GradientCircleButton
import com.devqissy.meowsic.ui.components.glassSpecular
import com.devqissy.meowsic.ui.theme.AppThemeColors
import com.devqissy.meowsic.ui.theme.GlassShapes
import com.devqissy.meowsic.ui.theme.LocalAppThemeColors
import com.devqissy.meowsic.ui.theme.glassPanelColor
import com.devqissy.meowsic.ui.theme.glassSheen
import com.devqissy.meowsic.ui.theme.glassSurface
import com.devqissy.meowsic.R
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition

@Composable
fun PlaylistScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit
) {
    val selectedPlaylistId by viewModel.selectedPlaylistId.collectAsStateWithLifecycle()
    val themeColors = LocalAppThemeColors.current
    val accent = themeColors.primary

    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }

    var playlistToDelete by remember { mutableStateOf<PlaylistEntity?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

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
        AmbientGlow(themeColors)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                )
                .navigationBarsPadding()
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
                        text = stringResource(R.string.playlists_title),
                        color = themeColors.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    GradientCircleButton(
                        icon = painterResource(R.drawable.list_plus),
                        contentDescription = stringResource(R.string.new_playlist),
                        onClick = { showDialog = true },
                        themeColors = themeColors
                    )
                }
            }

            if (playlists.isEmpty()) {
                PlaylistEmptyState(themeColors)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(playlists, key = { it.id }) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            isSelected = selectedPlaylistId == playlist.id,
                            onClick = {
                                viewModel.selectPlaylistFilter(playlist.id)
                                onBack()
                            },
                            onDeleteClick = {
                                playlistToDelete = playlist
                                showDeleteConfirmation = true
                            },
                            themeColors = themeColors
                        )
                    }
                }
            }
        }

        if (showDialog) {
            CreatePlaylistDialog(
                themeColors = themeColors,
                onCreate = { name ->
                    viewModel.createPlaylist(name)
                    showDialog = false
                },
                onDismiss = { showDialog = false }
            )
        }

        if (showDeleteConfirmation) {
            val toDelete = playlistToDelete
            if (toDelete != null) {
                DeletePlaylistDialog(
                    playlistName = if (toDelete.isDefault) {
                        stringResource(R.string.favorites_playlist)
                    } else {
                        toDelete.name
                    },
                    themeColors = themeColors,
                    onConfirm = {
                        viewModel.deletePlaylist(toDelete)
                        showDeleteConfirmation = false
                        playlistToDelete = null
                    },
                    onDismiss = {
                        showDeleteConfirmation = false
                        playlistToDelete = null
                    }
                )
            }
        }
    }
}

@Composable
private fun PlaylistCard(
    playlist: PlaylistEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    themeColors: AppThemeColors
) {
    val accent = themeColors.primary
    val accent2 = themeColors.gradientEnd
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardScale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accent else lerp(themeColors.glassBorder, accent, 0.25f),
        animationSpec = tween(220),
        label = "cardBorder"
    )

    val displayName = if (playlist.isDefault) {
        stringResource(R.string.favorites_playlist)
    } else {
        playlist.name
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .glassSurface(
                shape = RoundedCornerShape(20.dp),
                fillColor = themeColors.glassWhite,
                borderColor = borderColor,
                sheenColor = themeColors.glassSheen
            )
            .glassSpecular(
                shape = RoundedCornerShape(20.dp),
                sheenColor = themeColors.glassSheen,
                topLine = false
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(14.dp),
                    spotColor = accent.copy(alpha = 0.45f),
                    ambientColor = Color.Transparent
                )
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(colors = listOf(accent, accent2)))
                .glassSpecular(
                    shape = RoundedCornerShape(14.dp),
                    sheenColor = Color.White.copy(alpha = 0.28f),
                    topLine = false
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    if (playlist.isDefault) R.drawable.star else R.drawable.list_music
                ),
                contentDescription = null,
                tint = themeColors.onPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                color = themeColors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (playlist.isDefault) {
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(themeColors.glassWhiteStrong)
                        .border(1.dp, themeColors.glassBorder, RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.star),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(10.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = stringResource(R.string.default_playlist_label),
                        color = lerp(themeColors.textSecondary, accent, 0.45f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (!playlist.isDefault) {
            Spacer(modifier = Modifier.width(4.dp))

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDeleteClick),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(themeColors.glassWhite)
                        .border(1.dp, themeColors.glassBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.trash),
                        contentDescription = stringResource(R.string.delete),
                        tint = lerp(themeColors.textSecondary, DangerRed, 0.55f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))
        }

        if (isSelected) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(accent, accent2)))
            )
        }
    }
}

@Composable
private fun PlaylistEmptyState(themeColors: AppThemeColors) {
    val transition = rememberInfiniteTransition(label = "playlistEmptyPulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "emptyPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .glassSurface(
                    shape = RoundedCornerShape(24.dp),
                    fillColor = themeColors.glassPanelColor,
                    borderColor = lerp(themeColors.glassBorder, themeColors.primary, 0.25f),
                    sheenColor = themeColors.glassSheen
                )
                .glassSpecular(
                    shape = RoundedCornerShape(24.dp),
                    sheenColor = themeColors.glassSheen,
                    topLine = false
                )
                .padding(horizontal = 36.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(76.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .scale(0.75f + pulse * 0.5f)
                        .alpha((1f - pulse) * 0.55f)
                        .border(1.5.dp, themeColors.primary, CircleShape)
                )

                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .glassSurface(
                            shape = CircleShape,
                            fillColor = themeColors.glassWhiteStrong,
                            borderColor = lerp(themeColors.glassBorder, themeColors.primary, 0.35f),
                            sheenColor = themeColors.glassSheen
                        )
                        .glassSpecular(
                            shape = CircleShape,
                            sheenColor = themeColors.glassSheen,
                            topLine = false
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.list_music),
                        contentDescription = null,
                        tint = themeColors.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.no_playlists_yet),
                color = lerp(themeColors.textSecondary, themeColors.primary, 0.20f),
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.tap_plus_to_start),
                color = lerp(themeColors.textDisabled, themeColors.primary, 0.15f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CreatePlaylistDialog(
    themeColors: AppThemeColors,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val accent = themeColors.primary
    val accent2 = themeColors.gradientEnd

    var name by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val submit: () -> Unit = {
        if (name.isNotBlank()) onCreate(name.trim())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .glassSurface(
                            shape = CircleShape,
                            fillColor = themeColors.glassWhiteStrong,
                            borderColor = lerp(themeColors.glassBorder, accent, 0.35f),
                            sheenColor = themeColors.glassSheen
                        )
                        .glassSpecular(
                            shape = CircleShape,
                            sheenColor = themeColors.glassSheen,
                            topLine = false
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.list_plus),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    stringResource(R.string.create_new_playlist),
                    color = themeColors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassSurface(
                        shape = RoundedCornerShape(16.dp),
                        fillColor = themeColors.glassWhiteStrong,
                        borderColor = lerp(themeColors.glassBorder, accent, 0.30f),
                        sheenColor = themeColors.glassSheen
                    )
                    .glassSpecular(
                        shape = RoundedCornerShape(16.dp),
                        sheenColor = themeColors.glassSheen,
                        topLine = false
                    )
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (name.isEmpty()) {
                        Text(
                            text = stringResource(R.string.playlist_name_hint),
                            color = themeColors.textDisabled,
                            fontSize = 15.sp,
                            maxLines = 1
                        )
                    }

                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = themeColors.textPrimary,
                            fontSize = 15.sp
                        ),
                        cursorBrush = Brush.verticalGradient(listOf(accent, accent2)),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }
            }
        },
        confirmButton = {
            DialogPillButton(
                text = stringResource(R.string.create),
                themeColors = themeColors,
                enabled = name.isNotBlank(),
                filled = true,
                fillBrush = Brush.linearGradient(listOf(accent, accent2)),
                shadowSpot = lerp(accent, accent2, 0.5f).copy(alpha = 0.5f),
                onClick = submit
            )
        },
        dismissButton = {
            DialogPillButton(
                text = stringResource(R.string.cancel),
                themeColors = themeColors,
                onClick = onDismiss
            )
        },
        containerColor = themeColors.surface,
        shape = GlassShapes.dialog
    )
}

@Composable
private fun DeletePlaylistDialog(
    playlistName: String,
    themeColors: AppThemeColors,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            spotColor = DangerRedDeep.copy(alpha = 0.45f),
                            ambientColor = Color.Transparent
                        )
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(DangerRed, DangerRedDeep)))
                        .glassSpecular(
                            shape = CircleShape,
                            sheenColor = Color.White.copy(alpha = 0.28f),
                            topLine = false
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.trash),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    stringResource(R.string.delete_playlist_title),
                    color = themeColors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Text(
                stringResource(R.string.delete_playlist_message, playlistName),
                color = themeColors.textPrimary,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            DialogPillButton(
                text = stringResource(R.string.delete_confirm),
                themeColors = themeColors,
                filled = true,
                fillBrush = Brush.linearGradient(listOf(DangerRed, DangerRedDeep)),
                shadowSpot = DangerRedDeep.copy(alpha = 0.5f),
                onClick = onConfirm
            )
        },
        dismissButton = {
            DialogPillButton(
                text = stringResource(R.string.dismiss),
                themeColors = themeColors,
                onClick = onDismiss
            )
        },
        containerColor = themeColors.surface,
        shape = GlassShapes.dialog
    )
}

@Composable
private fun DialogPillButton(
    text: String,
    themeColors: AppThemeColors,
    onClick: () -> Unit,
    filled: Boolean = false,
    enabled: Boolean = true,
    fillBrush: Brush? = null,
    shadowSpot: Color = Color.Transparent
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pillBtnScale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .alpha(if (enabled) 1f else 0.45f)
            .shadow(
                elevation = if (filled && enabled) 8.dp else 0.dp,
                shape = RoundedCornerShape(50),
                spotColor = shadowSpot,
                ambientColor = Color.Transparent
            )
            .clip(RoundedCornerShape(50))
            .then(
                if (filled && fillBrush != null) {
                    Modifier.background(fillBrush)
                } else {
                    Modifier.background(themeColors.glassWhiteStrong)
                }
            )
            .border(
                1.dp,
                if (filled) Color.Transparent else lerp(themeColors.glassBorder, themeColors.primary, 0.35f),
                RoundedCornerShape(50)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 22.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (filled) themeColors.onPrimary else themeColors.primary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}