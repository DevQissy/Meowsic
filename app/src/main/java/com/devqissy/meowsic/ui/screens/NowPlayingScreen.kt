@file:OptIn(ExperimentalMaterial3Api::class)

package com.devqissy.meowsic.ui.screens

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.devqissy.meowsic.data.model.Song
import com.devqissy.meowsic.data.viewmodel.MusicViewModel
import com.devqissy.meowsic.ui.components.toTimeFormat
import com.devqissy.meowsic.ui.theme.AppThemeColors
import com.devqissy.meowsic.ui.theme.GlassShapes
import com.devqissy.meowsic.ui.theme.LocalAppThemeColors
import com.devqissy.meowsic.ui.theme.glassSheen
import com.devqissy.meowsic.ui.theme.glassSurface
import com.devqissy.meowsic.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import java.util.Locale
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

private val DangerRed = Color(0xFFFF6B6B)

private fun shareSong(context: Context, song: Song, chooserTitle: String) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, song.uri)
            putExtra(Intent.EXTRA_TEXT, "🎵 ${song.title} - ${song.artist}")
            clipData = ClipData.newRawUri(null, song.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
    } catch (e: Exception) {
        Log.e("NowPlaying", "Failed to share song", e)
    }
}

private fun formatSpeed(speed: Float): String {
    val formatted = when {
        speed == speed.toInt().toFloat() -> "%.1f".format(Locale.US, speed)
        else -> "%.2f".format(Locale.US, speed).trimEnd('0').trimEnd('.')
    }
    return "${formatted}x"
}

@Composable
fun NowPlayingScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onEqualizerClick: () -> Unit
) {
    val themeColors = LocalAppThemeColors.current
    val context = LocalContext.current
    val accent = themeColors.primary
    val accent2 = themeColors.gradientEnd
    val likeColor = DangerRed
    val shareSongText = stringResource(R.string.share_song)

    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val position by viewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val shuffleOn by viewModel.shuffleEnabled.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()

    val favoriteSongIds by viewModel.favoriteSongIds.collectAsStateWithLifecycle()
    val currentSongId = currentSong?.id
    val isLiked = currentSongId != null && currentSongId in favoriteSongIds

    val heartScale = remember { Animatable(1f) }
    LaunchedEffect(isLiked) {
        if (isLiked) {
            heartScale.snapTo(1.4f)
            heartScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    var showMenu by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }

    val sleepTimerRemaining by viewModel.sleepTimerRemaining.collectAsStateWithLifecycle()
    val sleepTimerRunning by viewModel.sleepTimerIsRunning.collectAsStateWithLifecycle()

    var isUserSeeking by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var waveform by remember { mutableStateOf<FloatArray?>(null) }

    val swipeThresholdPx = with(LocalDensity.current) { 60.dp.toPx() }
    var swipeAccum by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(position, duration) {
        if (!isUserSeeking && duration > 0) {
            sliderPosition = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        }
    }

    val displayPosition = if (duration > 0) position.coerceAtMost(duration) else position

    LaunchedEffect(currentSong?.id) {
        val song = currentSong ?: run { waveform = null; return@LaunchedEffect }

        SongWaveformCache.get(song.id.toString())?.let {
            waveform = it
            return@LaunchedEffect
        }

        waveform = null

        val result = SongWaveformExtractor.extract(
            context = context.applicationContext,
            uri = song.uri,
            bars = 64,
            durationMs = song.duration
        )
        if (result != null) {
            SongWaveformCache.put(song.id.toString(), result)
            waveform = result
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            viewModel.updatePosition()
            delay(250)
        }
    }

    val previousSong by viewModel.prevSongInOrder.collectAsStateWithLifecycle()
    val nextSong by viewModel.nextSongInOrder.collectAsStateWithLifecycle()

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
        NowPlayingAmbientGlow(themeColors)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                )
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    onClick = onBack,
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )

                AnimatedVisibility(
                    visible = sleepTimerRunning,
                    enter = expandHorizontally(tween(250)) + fadeIn(tween(250)),
                    exit = shrinkHorizontally(tween(200)) + fadeOut(tween(200))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .height(48.dp)
                            .glassSurface(
                                shape = RoundedCornerShape(50),
                                fillColor = themeColors.glassWhite,
                                borderColor = lerp(themeColors.glassBorder, themeColors.primary, 0.35f),
                                sheenColor = themeColors.glassSheen
                            )
                            .padding(start = 14.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.moon),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = compactRemainingTime(sleepTimerRemaining),
                            color = themeColors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { viewModel.stopSleepTimer() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.cancel_timer),
                                tint = likeColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row {
                    GlassIconButton(
                        onClick = onEqualizerClick,
                        iconPainter = painterResource(id = R.drawable.audio_lines),
                        contentDescription = stringResource(R.string.equalizer)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    GlassIconButton(
                        onClick = { showMenu = true },
                        icon = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.more_options)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = { swipeAccum = 0f },
                                onDragEnd = {
                                    when {
                                        swipeAccum > swipeThresholdPx -> viewModel.skipToPrevious()
                                        swipeAccum < -swipeThresholdPx -> viewModel.skipToNext()
                                    }
                                    swipeAccum = 0f
                                },
                                onDragCancel = { swipeAccum = 0f }
                            ) { change, dragAmount ->
                                change.consume()
                                swipeAccum += dragAmount
                            }
                        }
                ) {
                    AlbumCarousel(
                        currentSong = currentSong,
                        previousSong = previousSong,
                        nextSong = nextSong,
                        onPrevious = { viewModel.skipToPrevious() },
                        onNext = { viewModel.skipToNext() },
                        themeColors = themeColors
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Crossfade(
                        targetState = currentSong,
                        animationSpec = tween(250),
                        modifier = Modifier.weight(1f),
                        label = "songInfo"
                    ) { song ->
                        Column {
                            Text(
                                text = song?.artist ?: "",
                                color = themeColors.textSecondary,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = song?.title ?: stringResource(R.string.not_selected),
                                color = themeColors.textPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    GlassIconButton(
                        onClick = { currentSong?.let { viewModel.toggleFavorite(it.id) } },
                        iconPainter = painterResource(
                            if (isLiked) R.drawable.heart_filled else R.drawable.heart
                        ),
                        contentDescription = stringResource(
                            if (isLiked) R.string.remove_from_favorites
                            else R.string.add_to_favorites
                        ),
                        size = 48.dp,
                        iconSize = 24.dp,
                        tint = if (isLiked) likeColor else themeColors.textPrimary,
                        iconScale = heartScale.value
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    SongWaveformBar(
                        waveform = waveform,
                        progress = sliderPosition,
                        onSeekStart = { isUserSeeking = true },
                        onSeek = { sliderPosition = it },
                        onSeekFinished = { target ->
                            sliderPosition = target
                            viewModel.seekTo((target * duration).toLong())
                            isUserSeeking = false
                        },
                        playedColor = accent,
                        playedGradientEnd = accent2,
                        unplayedColor = themeColors.glassBorder,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val shownTime =
                        if (isUserSeeking) (sliderPosition * duration).toLong()
                        else displayPosition

                    Text(
                        text = shownTime.toTimeFormat(),
                        color = themeColors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = duration.toTimeFormat(),
                        color = themeColors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.toggleShuffle() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.shuffle),
                            contentDescription = stringResource(R.string.shuffle),
                            tint = if (shuffleOn) accent else themeColors.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.skipToPrevious() },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rewind),
                            contentDescription = stringResource(R.string.previous),
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    GlassPlayButton(
                        isPlaying = isPlaying,
                        onClick = { viewModel.togglePlayPause() }
                    )

                    IconButton(
                        onClick = { viewModel.skipToNext() },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.fast_forward),
                            contentDescription = stringResource(R.string.next),
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.cycleRepeatMode() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        val repeatResId = when (repeatMode) {
                            Player.REPEAT_MODE_ONE -> R.drawable.repeat_1
                            Player.REPEAT_MODE_ALL -> R.drawable.repeat
                            else -> R.drawable.repeat_off
                        }
                        Icon(
                            painter = painterResource(id = repeatResId),
                            contentDescription = stringResource(R.string.repeat),
                            tint = if (repeatMode != Player.REPEAT_MODE_OFF)
                                accent
                            else
                                themeColors.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formatSpeed(playbackSpeed),
                color = themeColors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { showSpeedDialog = true }
            )
        }

        if (showSleepTimerDialog) {
            SleepTimerDialog(
                onDismiss = { showSleepTimerDialog = false },
                onStartTimer = { minutes -> viewModel.startSleepTimer(minutes.toLong()) }
            )
        }

        if (showSpeedDialog) {
            SpeedControlDialog(
                currentSpeed = playbackSpeed,
                onSpeedSelected = { viewModel.setPlaybackSpeed(it) },
                onDismiss = { showSpeedDialog = false }
            )
        }

        if (showDetailsDialog) {
            currentSong?.let { song ->
                SongDetailsDialog(
                    song = song,
                    onDismiss = { showDetailsDialog = false }
                )
            }
        }

        if (showMenu) {
            GlassMenu(
                onDismiss = { showMenu = false },
                sleepTimerRunning = sleepTimerRunning,
                onSleepTimerClick = {
                    showMenu = false
                    if (sleepTimerRunning) viewModel.stopSleepTimer()
                    else showSleepTimerDialog = true
                },
                onSpeedClick = {
                    showMenu = false
                    showSpeedDialog = true
                },
                onShareClick = {
                    showMenu = false
                    currentSong?.let { song -> shareSong(context, song, shareSongText) }
                },
                onDetailsClick = {
                    showMenu = false
                    showDetailsDialog = true
                },
                onDeleteClick = {
                    showMenu = false
                    currentSong?.let { song ->
                        viewModel.deleteSong(song)
                        onBack()
                    }
                },
                themeColors = themeColors
            )
        }
    }
}

@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconPainter: Painter? = null,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    tint: Color = Color.Unspecified,
    iconScale: Float = 1f
) {
    val themeColors = LocalAppThemeColors.current
    val effectiveTint = if (tint == Color.Unspecified) {
        lerp(themeColors.textPrimary, themeColors.primary, 0.55f)
    } else tint

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "glassBtnPress"
    )

    Box(
        modifier = modifier
            .scale(pressScale)
            .size(size)
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                spotColor = themeColors.primary.copy(alpha = 0.25f),
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
        when {
            icon != null -> {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = effectiveTint,
                    modifier = Modifier
                        .size(iconSize)
                        .scale(iconScale)
                )
            }
            iconPainter != null -> {
                Icon(
                    painter = iconPainter,
                    contentDescription = contentDescription,
                    tint = effectiveTint,
                    modifier = Modifier
                        .size(iconSize)
                        .scale(iconScale)
                )
            }
        }
    }
}

@Composable
fun GlassPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = LocalAppThemeColors.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "playBtnPress"
    )

    Box(
        modifier = modifier
            .scale(pressScale)
            .size(72.dp)
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                spotColor = lerp(themeColors.primary, themeColors.gradientEnd, 0.5f)
                    .copy(alpha = 0.55f),
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
        Icon(
            painter = painterResource(
                id = if (isPlaying) R.drawable.pause else R.drawable.play
            ),
            contentDescription = if (isPlaying) stringResource(R.string.pause)
            else stringResource(R.string.play),
            tint = Color.White,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
fun GlassMenu(
    onDismiss: () -> Unit,
    sleepTimerRunning: Boolean,
    onSleepTimerClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onShareClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onDeleteClick: () -> Unit,
    themeColors: AppThemeColors
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = Color.Black.copy(alpha = 0.4f),
                    spotColor = Color.Black.copy(alpha = 0.4f)
                )
                .clip(RoundedCornerShape(20.dp))
                .background(themeColors.surface.copy(alpha = 0.97f)),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.more_options),
                        color = themeColors.textSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = themeColors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(
                    color = themeColors.glassBorder,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                GlassMenuItem(
                    icon = painterResource(id = R.drawable.moon),
                    text = if (sleepTimerRunning) stringResource(R.string.sleep_timer_active)
                    else stringResource(R.string.sleep_timer),
                    onClick = onSleepTimerClick,
                    iconTint = themeColors.primary
                )

                GlassMenuItem(
                    icon = painterResource(id = R.drawable.gauge),
                    text = stringResource(R.string.playback_speed),
                    onClick = onSpeedClick,
                    iconTint = themeColors.primary
                )

                HorizontalDivider(
                    color = themeColors.glassBorder,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )

                GlassMenuItem(
                    icon = painterResource(id = R.drawable.share),
                    text = stringResource(R.string.share),
                    onClick = onShareClick,
                    iconTint = themeColors.primary
                )

                GlassMenuItem(
                    icon = painterResource(id = R.drawable.info),
                    text = stringResource(R.string.song_details),
                    onClick = onDetailsClick,
                    iconTint = themeColors.primary
                )

                HorizontalDivider(
                    color = themeColors.glassBorder,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )

                GlassMenuItem(
                    icon = painterResource(id = R.drawable.trash),
                    text = stringResource(R.string.delete),
                    onClick = onDeleteClick,
                    iconTint = DangerRed,
                    textColor = DangerRed
                )
            }
        }
    }
}

@Composable
fun GlassMenuItem(
    icon: Any,
    text: String,
    onClick: () -> Unit,
    iconTint: Color = Color.Unspecified,
    textColor: Color = Color.Unspecified
) {
    val themeColors = LocalAppThemeColors.current
    val effectiveIconTint = if (iconTint == Color.Unspecified) themeColors.textPrimary else iconTint
    val effectiveTextColor = if (textColor == Color.Unspecified) themeColors.textPrimary else textColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (icon) {
            is ImageVector -> {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = effectiveIconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            is Painter -> {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = effectiveIconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            color = effectiveTextColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private data class CarouselEntry(val song: Song, val slot: Int)

@Composable
fun AlbumCarousel(
    currentSong: Song?,
    previousSong: Song?,
    nextSong: Song?,
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    themeColors: AppThemeColors
) {
    val density = LocalDensity.current
    val containerWidth = LocalWindowInfo.current.containerSize.width
    val screenWidth = if (containerWidth > 0) {
        with(density) { containerWidth.toDp() }
    } else {
        360.dp
    }

    val finalMainSize = (screenWidth * 0.70f).coerceIn(220.dp, 380.dp)
    val finalSideSize = (screenWidth * 0.60f).coerceIn(130.dp, 220.dp)
    val finalVisiblePart = (screenWidth * 0.07f).coerceIn(15.dp, 50.dp)

    val entries = remember(currentSong?.id, previousSong?.id, nextSong?.id) {
        listOfNotNull(
            previousSong?.let { CarouselEntry(it, -1) },
            currentSong?.let { CarouselEntry(it, 0) },
            nextSong?.let { CarouselEntry(it, 1) }
        ).distinctBy { it.song.id }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(finalMainSize + 20.dp)
    ) {
        entries.forEach { entry ->
            key(entry.song.id) {
                SlidingAlbumCard(
                    entry = entry,
                    boxWidth = maxWidth,
                    boxHeight = maxHeight,
                    mainSize = finalMainSize,
                    sideSize = finalSideSize,
                    visiblePart = finalVisiblePart,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    themeColors = themeColors
                )
            }
        }
    }
}

@Composable
private fun SlidingAlbumCard(
    entry: CarouselEntry,
    boxWidth: Dp,
    boxHeight: Dp,
    mainSize: Dp,
    sideSize: Dp,
    visiblePart: Dp,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
    themeColors: AppThemeColors
) {
    val density = LocalDensity.current

    val mainPx = with(density) { mainSize.toPx() }
    val sidePx = with(density) { sideSize.toPx() }
    val boxW = with(density) { boxWidth.toPx() }
    val boxH = with(density) { boxHeight.toPx() }
    val visPx = with(density) { visiblePart.toPx() }
    val mainShadowPx = with(density) { 24.dp.toPx() }
    val sideShadowPx = with(density) { 8.dp.toPx() }

    val mainX = (boxW - mainPx) / 2f
    val leftX = visPx - sidePx
    val rightX = boxW - visPx
    val sideRatio = sidePx / mainPx
    val mainCorner = 30.dp
    val sideCorner = 20.dp

    val slotAnim = remember { Animatable(entry.slot.toFloat()) }
    LaunchedEffect(entry.slot) {
        slotAnim.animateTo(
            entry.slot.toFloat(),
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    var appeared by remember { mutableStateOf(false) }
    val appearAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (!appeared) {
            appeared = true
            appearAnim.animateTo(1f, tween(250))
        }
    }

    val context = LocalContext.current
    val uri = entry.song.albumArtUri
    val imageModel = remember(uri) {
        ImageRequest.Builder(context)
            .data(uri)
            .crossfade(220)
            .build()
    }

    val sideInteractionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(mainSize)
            .graphicsLayer {
                val s = slotAnim.value.coerceIn(-1f, 1f)
                val sideT = abs(s)
                val targetX = if (s < 0f) leftX else rightX
                val scale = 1f + (sideRatio - 1f) * sideT

                transformOrigin = TransformOrigin(0f, 0f)
                translationX = mainX + (targetX - mainX) * sideT
                translationY = (boxH - mainPx * scale) / 2f
                scaleX = scale
                scaleY = scale
                shape = RoundedCornerShape(mainCorner + (sideCorner - mainCorner) * sideT)
                clip = true
                shadowElevation = mainShadowPx + (sideShadowPx - mainShadowPx) * sideT
                alpha = appearAnim.value
            }
            .then(
                if (entry.slot != 0) {
                    Modifier.clickable(
                        interactionSource = sideInteractionSource,
                        indication = null
                    ) {
                        if (entry.slot < 0) onPrevious?.invoke() else onNext?.invoke()
                    }
                } else {
                    Modifier
                }
            )
    ) {
        AlbumArtFallback(
            iconSize = mainSize * 0.40f,
            themeColors = themeColors
        )

        if (uri != null) {
            AsyncImage(
                model = imageModel,
                contentDescription = if (entry.slot == 0)
                    stringResource(R.string.album_art) else null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun AlbumArtFallback(
    iconSize: Dp,
    themeColors: AppThemeColors
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        lerp(themeColors.gradientMiddle, themeColors.primary, 0.25f),
                        themeColors.gradientEnd.copy(alpha = 0.85f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(iconSize * 2.1f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Icon(
            painter = painterResource(id = R.drawable.music),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun SongDetailsDialog(
    song: Song,
    onDismiss: () -> Unit
) {
    val themeColors = LocalAppThemeColors.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = GlassShapes.dialog,
            colors = CardDefaults.cardColors(containerColor = themeColors.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.song_detail_title),
                        color = themeColors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = themeColors.textSecondary
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = themeColors.glassBorder
                )

                DetailRow(stringResource(R.string.title_label), song.title)
                DetailRow(stringResource(R.string.artist_label), song.artist)
                DetailRow(
                    stringResource(R.string.album_label),
                    song.album.ifEmpty { stringResource(R.string.unknown) }
                )
                DetailRow(
                    stringResource(R.string.duration_label),
                    song.duration.toTimeFormat()
                )
                if (song.year > 0) {
                    DetailRow(stringResource(R.string.year_label), song.year.toString())
                }
                if (song.trackNumber > 0) {
                    DetailRow(
                        stringResource(R.string.track_number_label),
                        song.trackNumber.toString()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                ) {
                    Text(
                        stringResource(R.string.close_button),
                        color = themeColors.onPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    val themeColors = LocalAppThemeColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = themeColors.textSecondary,
            fontSize = 13.sp,
            modifier = Modifier.weight(0.3f)
        )
        Text(
            text = value,
            color = themeColors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.7f)
        )
    }
}

@Composable
private fun SpeedControlDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val themeColors = LocalAppThemeColors.current
    val accent = themeColors.primary

    val speedOptions = listOf(
        0.5f to "0.5x",
        0.75f to "0.75x",
        1.0f to "1.0x",
        1.25f to "1.25x",
        1.5f to "1.5x",
        2.0f to "2.0x"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = GlassShapes.dialog,
            colors = CardDefaults.cardColors(containerColor = themeColors.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.speed_title),
                        color = themeColors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = themeColors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                speedOptions.forEach { (speed, label) ->
                    val isSelected = abs(speed - currentSpeed) < 0.001f

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSpeedSelected(speed)
                                onDismiss()
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) accent.copy(alpha = 0.15f)
                            else themeColors.glassWhite
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) accent else themeColors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepTimerDialog(
    onDismiss: () -> Unit,
    onStartTimer: (Int) -> Unit
) {
    val themeColors = LocalAppThemeColors.current
    val options = listOf(5, 10, 15, 20, 30, 45, 60, 90, 120)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = GlassShapes.dialog,
            colors = CardDefaults.cardColors(containerColor = themeColors.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.moon),
                            contentDescription = null,
                            tint = themeColors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.sleep_timer),
                            color = themeColors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = themeColors.textSecondary
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = themeColors.glassBorder
                )

                options.chunked(3).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowOptions.forEach { minutes ->
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onStartTimer(minutes)
                                        onDismiss()
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = themeColors.glassWhite
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "$minutes",
                                        color = themeColors.textPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(R.string.minutes_unit),
                                        color = themeColors.textSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun compactRemainingTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "$h:${m.toString().padStart(2, '0')}"
    else "$m:${s.toString().padStart(2, '0')}"
}

@Composable
fun SongWaveformBar(
    waveform: FloatArray?,
    progress: Float,
    onSeekStart: () -> Unit,
    onSeek: (Float) -> Unit,
    onSeekFinished: (Float) -> Unit,
    playedColor: Color,
    playedGradientEnd: Color,
    unplayedColor: Color,
    modifier: Modifier = Modifier
) {
    val growProgress = remember { Animatable(0f) }

    LaunchedEffect(waveform) {
        if (waveform == null) {
            growProgress.snapTo(0f)
        } else {
            growProgress.snapTo(0f)
            growProgress.animateTo(
                1f,
                spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val shown = if (isDragging) dragValue else progress

    Box(
        modifier = modifier
            .height(40.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(shown, 0f..1f)
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragValue = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeekStart()
                        onSeek(dragValue)
                    },
                    onDragEnd = {
                        onSeekFinished(dragValue)
                        isDragging = false
                    },
                    onDragCancel = { isDragging = false }
                ) { change, _ ->
                    change.consume()
                    dragValue = (change.position.x / size.width).coerceIn(0f, 1f)
                    onSeek(dragValue)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onSeekStart()
                    onSeekFinished((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
            .drawBehind {
                val bars = waveform
                val barCount = bars?.size ?: 64
                if (barCount == 0) return@drawBehind

                val gap = 2.dp.toPx()
                val barWidth = (size.width - gap * (barCount - 1)) / barCount
                val playX = size.width * shown.coerceIn(0f, 1f)

                val minAmp = 0.10f
                val growT = growProgress.value.coerceIn(0f, 1f)

                var x = 0f
                for (i in 0 until barCount) {
                    val targetAmp = bars?.get(i)?.coerceIn(minAmp, 1f) ?: minAmp
                    val amp = minAmp + (targetAmp - minAmp) * growT

                    val barHeight = size.height * amp
                    val y = (size.height - barHeight) / 2f
                    val cx = x + barWidth / 2f

                    val color = if (cx <= playX) {
                        val t = if (playX > 0f) (cx / playX).coerceIn(0f, 1f) else 0f
                        lerp(playedColor, playedGradientEnd, t)
                    } else {
                        unplayedColor
                    }

                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                    x += barWidth + gap
                }
            }
    )
}

object SongWaveformCache {
    private const val MAX_ENTRIES = 128

    private val map = object : LinkedHashMap<String, FloatArray>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, FloatArray>) =
            size > MAX_ENTRIES
    }

    fun get(key: String): FloatArray? = synchronized(map) { map[key] }
    fun put(key: String, value: FloatArray) = synchronized(map) { map[key] = value }
}

object SongWaveformExtractor {

    private const val TAG = "WaveformExtractor"
    private const val WINDOW_US_PER_BAR = 90_000L
    private const val MAX_IN_WINDOW_BUFFERS = 6

    suspend fun extract(
        context: Context,
        uri: Uri,
        bars: Int,
        durationMs: Long
    ): FloatArray? = withContext(Dispatchers.IO) {
        var extractor: MediaExtractor? = null
        var codec: MediaCodec? = null
        try {
            val ex = MediaExtractor()
            extractor = ex
            ex.setDataSource(context, uri, null)

            var selectedTrack = -1
            var trackFormat: MediaFormat? = null
            for (i in 0 until ex.trackCount) {
                val f = ex.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    selectedTrack = i
                    trackFormat = f
                    break
                }
            }

            val format = trackFormat
            if (selectedTrack < 0 || format == null) return@withContext null

            ex.selectTrack(selectedTrack)

            val durationUs = when {
                durationMs > 0 -> durationMs * 1000L
                format.containsKey(MediaFormat.KEY_DURATION) ->
                    format.getLong(MediaFormat.KEY_DURATION)
                else -> 180_000_000L
            }

            val mime = format.getString(MediaFormat.KEY_MIME)
                ?: return@withContext null
            codec = MediaCodec.createDecoderByType(mime).apply {
                configure(format, null, null, 0)
                start()
            }

            val raw = FloatArray(bars)
            val filled = BooleanArray(bars)
            val info = MediaCodec.BufferInfo()
            var sawEos = false

            for (bar in 0 until bars) {
                coroutineContext.ensureActive()
                if (sawEos) break

                val targetUs = ((bar.toDouble() + 0.5) / bars * durationUs).toLong()
                    .coerceAtMost((durationUs - 1).coerceAtLeast(0L))
                ex.seekTo(targetUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                val windowEndUs = targetUs + WINDOW_US_PER_BAR
                var sumSquares = 0.0
                var sampleCount = 0L
                var inWindowBuffers = 0
                var windowDone = false

                while (!windowDone && !sawEos) {
                    coroutineContext.ensureActive()

                    val inIdx = codec.dequeueInputBuffer(10_000)
                    if (inIdx >= 0) {
                        val inBuf = codec.getInputBuffer(inIdx)
                        if (inBuf == null) {
                            sawEos = true
                        } else {
                            val sampleSize = ex.readSampleData(inBuf, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(
                                    inIdx, 0, 0, 0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                sawEos = true
                            } else {
                                codec.queueInputBuffer(
                                    inIdx, 0, sampleSize, ex.sampleTime, 0
                                )
                                ex.advance()
                            }
                        }
                    }

                    while (true) {
                        val outIdx = codec.dequeueOutputBuffer(info, 0)
                        if (outIdx < 0) break

                        val pts = info.presentationTimeUs
                        if (info.size > 0 && pts >= targetUs && pts < windowEndUs) {
                            val outBuf = codec.getOutputBuffer(outIdx)
                            if (outBuf != null) {
                                outBuf.order(ByteOrder.LITTLE_ENDIAN)
                                val shorts = outBuf.asShortBuffer()
                                val n = shorts.remaining()
                                var i = 0
                                while (i < n) {
                                    val v = shorts.get(i) / 32768f
                                    sumSquares += (v * v).toDouble()
                                    i++
                                }
                                sampleCount += n
                            }
                            inWindowBuffers++
                            if (inWindowBuffers >= MAX_IN_WINDOW_BUFFERS) windowDone = true
                        }

                        codec.releaseOutputBuffer(outIdx, false)

                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            sawEos = true
                        }
                        if (pts >= windowEndUs) windowDone = true
                    }
                }

                if (sampleCount > 0) {
                    raw[bar] = sqrt(sumSquares / sampleCount).toFloat()
                    filled[bar] = true
                }
            }

            if (filled.none { it }) return@withContext null

            for (i in 0 until bars) {
                if (!filled[i]) raw[i] = interpolateEmpty(raw, filled, i)
            }

            shapeToWaveform(raw)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Waveform extraction failed for $uri", e)
            null
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor?.release() }
        }
    }

    private fun interpolateEmpty(
        raw: FloatArray,
        filled: BooleanArray,
        index: Int
    ): Float {
        var left = index - 1
        while (left >= 0 && !filled[left]) left--
        var right = index + 1
        while (right < raw.size && !filled[right]) right++

        val lv = if (left >= 0) raw[left] else -1f
        val rv = if (right < raw.size) raw[right] else -1f
        return when {
            lv >= 0f && rv >= 0f -> (lv + rv) / 2f
            lv >= 0f -> lv
            rv >= 0f -> rv
            else -> 0f
        }
    }

    private fun shapeToWaveform(rms: FloatArray): FloatArray {
        val db = FloatArray(rms.size) { i ->
            maxOf(20f * log10(rms[i].coerceAtLeast(1e-5f)), -70f)
        }

        val sorted = db.clone()
        sorted.sort()

        fun pct(p: Float): Float {
            val idx = (p * (sorted.size - 1)).toInt().coerceIn(0, sorted.size - 1)
            return sorted[idx]
        }

        val p95 = pct(0.95f)
        val p20 = pct(0.20f)

        var range = p95 - p20
        if (range < 6f) range = 6f
        if (range > 22f) range = 22f

        val floorDb = p95 - range

        val shaped = FloatArray(db.size) { i ->
            val t = ((db[i] - floorDb) / range).coerceIn(0f, 1f)
            0.10f + 0.90f * t.pow(0.85f)
        }

        val out = FloatArray(shaped.size)
        for (i in shaped.indices) {
            val prev = shaped[if (i > 0) i - 1 else i]
            val next = shaped[if (i < shaped.size - 1) i + 1 else i]
            out[i] = 0.25f * prev + 0.5f * shaped[i] + 0.25f * next
        }
        return out
    }
}

@Composable
fun NowPlayingAmbientGlow(
    themeColors: AppThemeColors,
    animate: Boolean = true
) {
    val glow = remember { Animatable(0.8f) }

    LaunchedEffect(animate) {
        if (animate) {
            while (true) {
                glow.animateTo(1f, tween(2000, easing = LinearEasing))
                glow.animateTo(0.65f, tween(2000, easing = LinearEasing))
            }
        } else {
            glow.animateTo(0.8f, tween(800, easing = LinearEasing))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val g = glow.value

                val topCenter = Offset(size.width * 0.5f, size.height * 0.22f)
                val topRadius = size.minDimension * 0.75f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            themeColors.primary.copy(alpha = 0.22f * g),
                            Color.Transparent
                        ),
                        center = topCenter,
                        radius = topRadius
                    ),
                    center = topCenter,
                    radius = topRadius
                )

                val bottomCenter = Offset(size.width * 0.2f, size.height * 0.85f)
                val bottomRadius = size.minDimension * 0.6f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            themeColors.gradientEnd.copy(alpha = 0.16f * g),
                            Color.Transparent
                        ),
                        center = bottomCenter,
                        radius = bottomRadius
                    ),
                    center = bottomCenter,
                    radius = bottomRadius
                )
            }
    )
}

private fun Modifier.glassSpecular(
    shape: Shape,
    sheenColor: Color,
    topLine: Boolean = true
): Modifier = this
    .clip(shape)
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    sheenColor.copy(alpha = if (topLine) 0.55f else 0.35f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = if (topLine) size.height * 0.35f else size.height * 0.6f
            )
        )
    }