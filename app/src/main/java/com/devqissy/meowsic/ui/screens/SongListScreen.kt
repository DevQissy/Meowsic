package com.devqissy.meowsic.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devqissy.meowsic.data.db.PlaylistEntity
import com.devqissy.meowsic.data.local.RatingPromptManager
import com.devqissy.meowsic.data.model.Song
import com.devqissy.meowsic.data.viewmodel.MusicViewModel
import com.devqissy.meowsic.ui.components.AddToPlaylistDialog
import com.devqissy.meowsic.ui.components.AmbientGlow
import com.devqissy.meowsic.ui.components.GlassCircleIconButton
import com.devqissy.meowsic.ui.components.MiniEqualizer
import com.devqissy.meowsic.ui.components.PlayPauseButton
import com.devqissy.meowsic.ui.components.SongItem
import com.devqissy.meowsic.ui.components.VerticalScrollbar
import com.devqissy.meowsic.ui.components.glassSpecular
import com.devqissy.meowsic.ui.theme.AppThemeColors
import com.devqissy.meowsic.ui.theme.GlassShapes
import com.devqissy.meowsic.ui.theme.LocalAppThemeColors
import com.devqissy.meowsic.ui.theme.glassPanelColor
import com.devqissy.meowsic.ui.theme.glassSheen
import com.devqissy.meowsic.ui.theme.glassSurface
import com.devqissy.meowsic.ui.theme.isDarkTheme
import com.devqissy.meowsic.util.openMyketForRating
import com.devqissy.meowsic.R
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.shadow
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition

@Composable
fun SongListScreen(
    viewModel: MusicViewModel,
    onSongClick: () -> Unit,
    onSearchClick: () -> Unit,
    onPlaylistClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onEqualizerClick: () -> Unit
) {
    val themeColors = LocalAppThemeColors.current
    val context = LocalContext.current

    val songs by viewModel.filteredSongs.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val selectedPlaylistId by viewModel.selectedPlaylistId.collectAsStateWithLifecycle()
    val playlistsWithoutSong by viewModel.playlistsWithoutSong.collectAsStateWithLifecycle()

    val currentSongId = currentSong?.id

    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var selectedSongForPlaylist by remember { mutableStateOf<Song?>(null) }
    val listState = rememberLazyListState()

    val ratingManager = remember { RatingPromptManager(context) }
    var showRateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { ratingManager.onFirstLaunch() }

    LaunchedEffect(Unit) {
        delay(3000)
        if (ratingManager.shouldShowRating()) showRateDialog = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
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
        ) {
            SongListHeader(
                songCount = songs.size,
                currentSong = currentSong,
                isPlaying = isPlaying,
                selectedPlaylistId = selectedPlaylistId,
                onPlayPause = { viewModel.togglePlayPause() },
                onPrevious = { viewModel.skipToPrevious() },
                onNext = { viewModel.skipToNext() },
                onExpand = onSongClick,
                onSearchClick = onSearchClick,
                onEqualizerClick = onEqualizerClick,
                onPlaylistClick = onPlaylistClick,
                onSettingsClick = onSettingsClick
            )

            PlaylistFilterRow(
                playlists = playlists,
                selectedPlaylistId = selectedPlaylistId,
                onSelectPlaylist = { viewModel.selectPlaylistFilter(it) },
                themeColors = themeColors
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .glassSurface(
                            shape = GlassShapes.panel,
                            fillColor = themeColors.glassPanelColor,
                            borderColor = themeColors.glassBorder,
                            sheenColor = themeColors.glassSheen
                        )
                        .glassSpecular(
                            shape = GlassShapes.panel,
                            sheenColor = themeColors.glassSheen
                        )
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(GlassShapes.panel),
                    state = listState,
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        end = 14.dp,
                        top = 10.dp,
                        bottom = 18.dp
                    )
                ) {
                    if (songs.isEmpty()) {
                        item(key = "empty") {
                            val emptyMessage = if (selectedPlaylistId != null) {
                                stringResource(R.string.playlist_empty)
                            } else {
                                stringResource(R.string.no_songs_found)
                            }
                            EmptyGlassState(
                                message = emptyMessage,
                                themeColors = themeColors
                            )
                        }
                    } else {
                        items(
                            items = songs,
                            key = { it.id },
                            contentType = { "song" }
                        ) { song ->
                            val isThisSongPlaying = currentSongId == song.id && isPlaying

                            SongItem(
                                song = song,
                                isPlaying = isThisSongPlaying,
                                onClick = {
                                    viewModel.playSong(song, songs)
                                    onSongClick()
                                },
                                onAddToPlaylist = { selectedSong ->
                                    selectedSongForPlaylist = selectedSong
                                    viewModel.loadPlaylistsWithoutSong(selectedSong.id)
                                    showAddToPlaylistDialog = true
                                },
                                onRemoveFromPlaylist = if (selectedPlaylistId != null) {
                                    { selectedSong ->
                                        viewModel.removeSongFromPlaylist(selectedSong.id)
                                    }
                                } else null,
                                onDeleteClick = { selectedSong ->
                                    viewModel.deleteSong(selectedSong)
                                }
                            )
                        }
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
                                        themeColors.gradientEnd.copy(alpha = 0.55f)
                                    ),
                                    startY = size.height * 0.2f,
                                    endY = size.height
                                )
                            )
                        }
                )

                if (songs.isNotEmpty()) {
                    VerticalScrollbar(
                        listState = listState,
                        thumbColor = themeColors.primary.copy(alpha = 0.65f),
                        thumbWidth = 3.dp,
                        modifier = Modifier
                            .fillMaxHeight()
                            .align(Alignment.CenterEnd)
                            .padding(end = 5.dp)
                            .padding(top = 14.dp, bottom = 14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.navigationBarsPadding())
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

        if (showRateDialog) {
            RateAppDialog(
                onRate = {
                    ratingManager.onRateClicked()
                    openMyketForRating(context)
                    showRateDialog = false
                },
                onDismiss = {
                    ratingManager.onDismissed()
                    showRateDialog = false
                },
                themeColors = themeColors
            )
        }
    }
}

@Composable
private fun SongListHeader(
    songCount: Int,
    currentSong: Song?,
    isPlaying: Boolean,
    selectedPlaylistId: Long?,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit,
    onSearchClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onPlaylistClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val themeColors = LocalAppThemeColors.current
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp, bottom = 2.dp)
            .glassSurface(
                shape = GlassShapes.header,
                fillColor = themeColors.glassPanelColor,
                borderColor = themeColors.glassBorder,
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        color = themeColors.textPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (selectedPlaylistId != null) {
                            pluralStringResource(
                                R.plurals.songs_in_playlist,
                                songCount,
                                songCount
                            )
                        } else {
                            pluralStringResource(
                                R.plurals.songs_total,
                                songCount,
                                songCount
                            )
                        },
                        color = themeColors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassCircleIconButton(
                        icon = painterResource(R.drawable.search),
                        contentDescription = stringResource(R.string.search),
                        onClick = onSearchClick,
                        themeColors = themeColors,
                        size = 40.dp,
                        iconSize = 18.dp
                    )

                    Box {
                        GlassCircleIconButton(
                            icon = painterResource(R.drawable.more),
                            contentDescription = stringResource(R.string.more_options),
                            onClick = { showMenu = true },
                            themeColors = themeColors,
                            size = 40.dp,
                            iconSize = 18.dp
                        )

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            containerColor = themeColors.surface,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.effects),
                                        color = themeColors.textPrimary,
                                        fontSize = 14.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.audio_lines),
                                        contentDescription = null,
                                        tint = themeColors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onEqualizerClick()
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.playlists),
                                        color = themeColors.textPrimary,
                                        fontSize = 14.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.list_music),
                                        contentDescription = null,
                                        tint = themeColors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onPlaylistClick()
                                }
                            )

                            HorizontalDivider(
                                color = themeColors.glassBorder,
                                thickness = 0.5.dp
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.settings),
                                        color = themeColors.textPrimary,
                                        fontSize = 14.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.settings),
                                        contentDescription = null,
                                        tint = themeColors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onSettingsClick()
                                }
                            )
                        }
                    }
                }
            }

            currentSong?.let { song ->
                Column {
                    HorizontalDivider(
                        color = themeColors.glassBorder,
                        thickness = 0.5.dp
                    )

                    NowPlayingBar(
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
private fun NowPlayingBar(
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
private fun PlaylistFilterRow(
    playlists: List<PlaylistEntity>,
    selectedPlaylistId: Long?,
    onSelectPlaylist: (Long?) -> Unit,
    themeColors: AppThemeColors
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChipItem(
            selected = selectedPlaylistId == null,
            label = stringResource(R.string.all_filter),
            onClick = { onSelectPlaylist(null) },
            themeColors = themeColors
        )

        playlists.forEach { playlist ->
            FilterChipItem(
                selected = selectedPlaylistId == playlist.id,
                label = playlist.name,
                onClick = { onSelectPlaylist(playlist.id) },
                themeColors = themeColors
            )
        }
    }
}

@Composable
private fun FilterChipItem(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    themeColors: AppThemeColors
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "filterChipScale"
    )

    val gradStart by animateColorAsState(
        targetValue = if (selected) themeColors.primary else themeColors.glassWhite,
        animationSpec = tween(220),
        label = "filterGradStart"
    )
    val gradEnd by animateColorAsState(
        targetValue = if (selected) {
            lerp(themeColors.primary, themeColors.gradientEnd, 0.85f)
        } else {
            themeColors.glassWhite
        },
        animationSpec = tween(220),
        label = "filterGradEnd"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) {
            themeColors.onPrimary
        } else {
            lerp(themeColors.textSecondary, themeColors.primary, 0.30f)
        },
        animationSpec = tween(220),
        label = "filterText"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            lerp(themeColors.primary, themeColors.gradientEnd, 0.5f)
        } else {
            lerp(themeColors.glassBorder, themeColors.primary, 0.30f)
        },
        animationSpec = tween(220),
        label = "filterBorder"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .height(34.dp)
            .shadow(
                elevation = if (selected) 12.dp else 0.dp,
                shape = GlassShapes.chip,
                spotColor = lerp(themeColors.primary, themeColors.gradientEnd, 0.5f)
                    .copy(alpha = 0.55f),
                ambientColor = Color.Transparent
            )
            .clip(GlassShapes.chip)
            .background(Brush.horizontalGradient(listOf(gradStart, gradEnd)))
            .border(1.dp, borderColor, GlassShapes.chip)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun EmptyGlassState(
    message: String,
    themeColors: AppThemeColors
) {
    val transition = rememberInfiniteTransition(label = "emptyState")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "emptyStatePulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 44.dp),
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
                        Icons.Default.MusicOff,
                        contentDescription = null,
                        tint = themeColors.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = message,
                color = lerp(themeColors.textSecondary, themeColors.primary, 0.20f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun RateAppDialog(
    onRate: () -> Unit,
    onDismiss: () -> Unit,
    themeColors: AppThemeColors
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = themeColors.surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
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
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = themeColors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    stringResource(R.string.rate_dialog_title),
                    color = themeColors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Text(
                stringResource(R.string.rate_dialog_message),
                color = themeColors.textSecondary,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onRate) {
                Text(
                    stringResource(R.string.rate_dialog_rate),
                    color = themeColors.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.rate_dialog_later),
                    color = themeColors.textSecondary
                )
            }
        }
    )
}