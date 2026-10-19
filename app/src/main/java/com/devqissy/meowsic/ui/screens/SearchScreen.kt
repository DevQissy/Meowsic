package com.devqissy.meowsic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devqissy.meowsic.data.db.PlaylistEntity
import com.devqissy.meowsic.data.model.Song
import com.devqissy.meowsic.data.viewmodel.MusicViewModel
import com.devqissy.meowsic.ui.components.AmbientGlow
import com.devqissy.meowsic.ui.components.GlassCircleIconButton
import com.devqissy.meowsic.ui.components.MiniEqualizer
import com.devqissy.meowsic.ui.components.SongItem
import com.devqissy.meowsic.ui.components.glassSpecular
import com.devqissy.meowsic.ui.theme.AppThemeColors
import com.devqissy.meowsic.ui.theme.GlassShapes
import com.devqissy.meowsic.ui.theme.LocalAppThemeColors
import com.devqissy.meowsic.ui.theme.glassPanelColor
import com.devqissy.meowsic.ui.theme.glassSheen
import com.devqissy.meowsic.ui.theme.glassSurface
import com.devqissy.meowsic.ui.theme.isDarkTheme
import com.devqissy.meowsic.R
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import com.devqissy.meowsic.ui.components.VerticalScrollbar

@Composable
fun SearchScreen(
    viewModel: MusicViewModel,
    onSongClick: () -> Unit,
    onBack: () -> Unit
) {
    val themeColors = LocalAppThemeColors.current
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val results by viewModel.searchResults.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val playlistsWithoutSong by viewModel.playlistsWithoutSong.collectAsStateWithLifecycle()

    val currentSongId = currentSong?.id

    val listState = rememberLazyListState()
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var selectedSongForPlaylist by remember { mutableStateOf<Song?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.setSearchQuery("")
        }
    }

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
            SearchGlassHeader(
                query = query,
                resultCount = results.size,
                currentSong = currentSong,
                isPlaying = isPlaying,
                onPlayPause = { viewModel.togglePlayPause() },
                onPrevious = { viewModel.skipToPrevious() },
                onNext = { viewModel.skipToNext() },
                onExpand = onSongClick,
                onClose = onBack,
                themeColors = themeColors
            )

            GlassSearchField(
                query = query,
                onQueryChange = { viewModel.setSearchQuery(it) },
                onClear = { viewModel.setSearchQuery("") },
                hint = stringResource(R.string.search_hint),
                clearDescription = stringResource(R.string.clear_search),
                themeColors = themeColors
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                when {
                    query.isEmpty() -> {
                        PromptGlassState(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.search),
                                    contentDescription = null,
                                    tint = themeColors.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            },
                            message = stringResource(R.string.search_prompt),
                            themeColors = themeColors
                        )
                    }

                    results.isEmpty() -> {
                        PromptGlassState(
                            icon = {
                                Icon(
                                    Icons.Default.MusicOff,
                                    contentDescription = null,
                                    tint = themeColors.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            },
                            message = stringResource(R.string.no_results_found),
                            subMessage = stringResource(R.string.search_another_word),
                            themeColors = themeColors
                        )
                    }

                    else -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .glassSurface(
                                        shape = GlassShapes.panel,
                                        fillColor = themeColors.glassPanelColor,
                                        borderColor = lerp(
                                            themeColors.glassBorder,
                                            themeColors.primary,
                                            0.25f
                                        ),
                                        sheenColor = themeColors.glassSheen
                                    )
                                    .glassSpecular(
                                        shape = GlassShapes.panel,
                                        sheenColor = themeColors.glassSheen
                                    )
                            )

                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(GlassShapes.panel),
                                contentPadding = PaddingValues(
                                    start = 8.dp,
                                    end = 14.dp,
                                    top = 10.dp,
                                    bottom = 18.dp
                                )
                            ) {
                                items(results, key = { it.id }) { song ->
                                    val isThisSongPlaying =
                                        currentSongId == song.id && isPlaying

                                    SongItem(
                                        song = song,
                                        isPlaying = isThisSongPlaying,
                                        onClick = {
                                            viewModel.playSong(song, results)
                                            onSongClick()
                                        },
                                        onAddToPlaylist = { selectedSong ->
                                            selectedSongForPlaylist = selectedSong
                                            viewModel.loadPlaylistsWithoutSong(selectedSong.id)
                                            showAddToPlaylistDialog = true
                                        },
                                        onDeleteClick = { selectedSong ->
                                            viewModel.deleteSong(selectedSong)
                                        }
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(26.dp)
                                    .clip(GlassShapes.panel)
                                    .drawBehind {
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    themeColors.gradientEnd
                                                        .copy(alpha = 0.55f)
                                                ),
                                                startY = size.height * 0.2f,
                                                endY = size.height
                                            )
                                        )
                                    }
                            )

                            VerticalScrollbar(
                                listState = listState,
                                thumbColor = themeColors.primary.copy(alpha = 0.65f),
                                thumbWidth = 3.dp,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 5.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }

        selectedSongForPlaylist?.let { selected ->
            if (showAddToPlaylistDialog) {
                AddToPlaylistDialog(
                    song = selected,
                    playlists = playlistsWithoutSong,
                    hasAnyPlaylist = playlists.isNotEmpty(),
                    onAddToPlaylist = { playlistId ->
                        viewModel.addSongToPlaylist(playlistId, selected.id)
                        showAddToPlaylistDialog = false
                        selectedSongForPlaylist = null
                    },
                    onDismiss = {
                        showAddToPlaylistDialog = false
                        selectedSongForPlaylist = null
                    },
                    themeColors = themeColors
                )
            }
        }
    }
}

@Composable
private fun SearchGlassHeader(
    query: String,
    resultCount: Int,
    currentSong: Song?,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    themeColors: AppThemeColors
) {
    val accent = themeColors.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp, bottom = 4.dp)
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
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassCircleIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = stringResource(R.string.close),
                    onClick = onClose,
                    themeColors = themeColors
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        color = themeColors.textPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (query.isNotEmpty()) {
                            stringResource(R.string.search_results_count, resultCount)
                        } else {
                            stringResource(R.string.search_in_songs)
                        },
                        color = lerp(themeColors.textSecondary, accent, 0.45f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            currentSong?.let { song ->
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    0f to Color.Transparent,
                                    0.5f to accent.copy(alpha = 0.55f),
                                    1f to Color.Transparent
                                )
                            )
                    )

                    SearchNowPlayingBar(
                        songTitle = song.title,
                        isPlaying = isPlaying,
                        onPlayPause = onPlayPause,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onExpand = onExpand,
                        themeColors = themeColors
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchNowPlayingBar(
    songTitle: String,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit,
    themeColors: AppThemeColors
) {
    val accent = themeColors.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onExpand)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MiniEqualizer(isPlaying = isPlaying, color = accent)

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = songTitle,
            color = themeColors.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .basicMarquee(iterations = Int.MAX_VALUE)
        )

        Spacer(modifier = Modifier.width(8.dp))

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                GlassCircleIconButton(
                    icon = painterResource(R.drawable.rewind),
                    contentDescription = null,
                    onClick = onPrevious,
                    themeColors = themeColors,
                    size = 36.dp,
                    iconSize = 17.dp
                )

                PlayPauseButton(
                    isPlaying = isPlaying,
                    onClick = onPlayPause,
                    themeColors = themeColors
                )

                GlassCircleIconButton(
                    icon = painterResource(R.drawable.fast_forward),
                    contentDescription = null,
                    onClick = onNext,
                    themeColors = themeColors,
                    size = 36.dp,
                    iconSize = 17.dp
                )
            }
        }
    }
}

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    themeColors: AppThemeColors
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
        modifier = Modifier
            .scale(scale)
            .size(42.dp)
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
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun GlassSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    hint: String,
    clearDescription: String,
    themeColors: AppThemeColors
) {
    val accent = themeColors.primary
    val accent2 = themeColors.gradientEnd
    val focusRequester = remember { FocusRequester() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .glassSurface(
                shape = RoundedCornerShape(18.dp),
                fillColor = themeColors.glassWhiteStrong,
                borderColor = lerp(themeColors.glassBorder, accent, 0.30f),
                sheenColor = themeColors.glassSheen
            )
            .glassSpecular(
                shape = RoundedCornerShape(18.dp),
                sheenColor = themeColors.glassSheen,
                topLine = false
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusRequester.requestFocus() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.search),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = hint,
                    color = themeColors.textDisabled,
                    fontSize = 15.sp,
                    maxLines = 1
                )
            }

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = themeColors.textPrimary,
                    fontSize = 15.sp
                ),
                cursorBrush = Brush.verticalGradient(listOf(accent, accent2)),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        }

        AnimatedVisibility(
            visible = query.isNotEmpty(),
            enter = fadeIn(tween(150)) + expandHorizontally(tween(150)),
            exit = fadeOut(tween(120)) + shrinkHorizontally(tween(120))
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .clickable(onClick = onClear),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(themeColors.glassWhite)
                        .border(1.dp, themeColors.glassBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = clearDescription,
                        tint = themeColors.textSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PromptGlassState(
    icon: @Composable () -> Unit,
    message: String,
    themeColors: AppThemeColors,
    subMessage: String? = null
) {
    val transition = rememberInfiniteTransition(label = "promptPulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "promptPulseValue"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
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
                            borderColor = lerp(
                                themeColors.glassBorder,
                                themeColors.primary,
                                0.35f
                            ),
                            sheenColor = themeColors.glassSheen
                        )
                        .glassSpecular(
                            shape = CircleShape,
                            sheenColor = themeColors.glassSheen,
                            topLine = false
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = message,
                color = lerp(themeColors.textSecondary, themeColors.primary, 0.20f),
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )

            if (subMessage != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subMessage,
                    color = lerp(themeColors.textDisabled, themeColors.primary, 0.15f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AddToPlaylistDialog(
    song: Song,
    playlists: List<PlaylistEntity>,
    hasAnyPlaylist: Boolean,
    onAddToPlaylist: (Long) -> Unit,
    onDismiss: () -> Unit,
    themeColors: AppThemeColors
) {
    val accent = themeColors.primary
    val accent2 = themeColors.gradientEnd

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
                        painter = painterResource(R.drawable.plus),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    stringResource(R.string.add_song_to_playlist_title, song.title),
                    color = themeColors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            if (playlists.isEmpty()) {
                val message = if (!hasAnyPlaylist) {
                    stringResource(R.string.create_playlist_first)
                } else {
                    stringResource(R.string.song_in_all_playlists)
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .glassSurface(
                                shape = CircleShape,
                                fillColor = themeColors.glassWhiteStrong,
                                borderColor = lerp(themeColors.glassBorder, accent, 0.35f),
                                sheenColor = themeColors.glassSheen
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.plus),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        message,
                        color = lerp(themeColors.textSecondary, accent, 0.20f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    playlists.forEach { playlist ->
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(
                            targetValue = if (isPressed) 0.97f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium),
                            label = "playlistItemScale"
                        )

                        Box(
                            modifier = Modifier
                                .scale(scale)
                                .fillMaxWidth()
                                .glassSurface(
                                    shape = RoundedCornerShape(16.dp),
                                    fillColor = themeColors.glassWhite,
                                    borderColor = lerp(themeColors.glassBorder, accent, 0.30f),
                                    sheenColor = themeColors.glassSheen
                                )
                                .glassSpecular(
                                    shape = RoundedCornerShape(16.dp),
                                    sheenColor = themeColors.glassSheen,
                                    topLine = false
                                )
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) { onAddToPlaylist(playlist.id) }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.linearGradient(colors = listOf(accent, accent2))
                                        )
                                        .glassSpecular(
                                            shape = RoundedCornerShape(12.dp),
                                            sheenColor = Color.White.copy(alpha = 0.28f),
                                            topLine = false
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.list_music),
                                        contentDescription = null,
                                        tint = themeColors.onPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    playlist.name,
                                    color = themeColors.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )

                                Icon(
                                    painter = painterResource(R.drawable.plus),
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(themeColors.glassWhiteStrong)
                    .border(
                        1.dp,
                        lerp(themeColors.glassBorder, accent, 0.35f),
                        RoundedCornerShape(50)
                    )
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 20.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.close),
                    color = accent,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        },
        containerColor = themeColors.surface,
        shape = GlassShapes.dialog
    )
}