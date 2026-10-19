package com.devqissy.meowsic.ui.components

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.devqissy.meowsic.data.model.Song
import com.devqissy.meowsic.ui.theme.LocalAppThemeColors
import com.devqissy.meowsic.R
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun SongItem(
    song: Song,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    onAddToPlaylist: ((Song) -> Unit)? = null,
    onRemoveFromPlaylist: ((Song) -> Unit)? = null,
    onDeleteClick: ((Song) -> Unit)? = null
) {
    val themeColors = LocalAppThemeColors.current
    val accent = themeColors.primary
    val context = LocalContext.current

    val shareSongText = stringResource(R.string.share_song)
    val shareText = stringResource(R.string.share)
    val addToPlaylistText = stringResource(R.string.add_to_playlist)
    val removeFromPlaylistText = stringResource(R.string.remove_from_playlist)
    val deleteSongText = stringResource(R.string.delete_song)
    val moreText = stringResource(R.string.more)
    val musicNoteIconText = stringResource(R.string.music_note_icon)

    val imageRequest = remember(song.albumArtUri) {
        if (song.albumArtUri != null) {
            ImageRequest.Builder(context)
                .data(song.albumArtUri)
                .size(96)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .allowHardware(false)
                .crossfade(0)
                .build()
        } else null
    }

    var showFallback by remember(song.id) { mutableStateOf(false) }
    var showItemMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isPlaying) themeColors.glassWhite else Color.Transparent)
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (imageRequest != null && !showFallback) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop,
                    onError = { showFallback = true }
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    lerp(themeColors.gradientMiddle, accent, 0.20f),
                                    themeColors.gradientEnd.copy(alpha = 0.70f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            lerp(themeColors.glassBorder, accent, 0.30f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.music),
                        contentDescription = musicNoteIconText,
                        tint = themeColors.textPrimary.copy(alpha = 0.65f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = if (isPlaying) accent else themeColors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = if (isPlaying) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artist,
                    color = themeColors.textSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isPlaying) {
                MiniEqualizer(color = accent)
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                AnimatedVisibility(
                    visible = true,
                    enter = expandHorizontally(tween(180)) + fadeIn(tween(180)),
                    exit = shrinkHorizontally(tween(180)) + fadeOut(tween(180))
                ) {
                    Text(
                        text = song.duration.toTimeFormat(),
                        color = themeColors.textDisabled,
                        fontSize = 12.sp
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { showItemMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more),
                        contentDescription = moreText,
                        tint = themeColors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (showItemMenu) {
                    DropdownMenu(
                        expanded = showItemMenu,
                        onDismissRequest = { showItemMenu = false },
                        containerColor = themeColors.surface,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        if (onAddToPlaylist != null) {
                            DropdownMenuItem(
                                text = {
                                    Text(addToPlaylistText, color = themeColors.textPrimary, fontSize = 14.sp)
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.list_plus),
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    showItemMenu = false
                                    onAddToPlaylist(song)
                                }
                            )
                        }

                        if (onRemoveFromPlaylist != null) {
                            DropdownMenuItem(
                                text = {
                                    Text(removeFromPlaylistText, color = themeColors.textPrimary, fontSize = 14.sp)
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.list_minus),
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    showItemMenu = false
                                    onRemoveFromPlaylist(song)
                                }
                            )
                        }

                        HorizontalDivider(color = themeColors.glassBorder, thickness = 0.5.dp)

                        DropdownMenuItem(
                            text = {
                                Text(shareText, color = themeColors.textPrimary, fontSize = 14.sp)
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.share),
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                showItemMenu = false
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "audio/*"
                                    putExtra(Intent.EXTRA_STREAM, song.uri)
                                    putExtra(Intent.EXTRA_TEXT, "🎵 ${song.title} - ${song.artist}")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(shareIntent, shareSongText)
                                )
                            }
                        )

                        if (onDeleteClick != null) {
                            HorizontalDivider(color = themeColors.glassBorder, thickness = 0.5.dp)
                            DropdownMenuItem(
                                text = {
                                    Text(deleteSongText, color = themeColors.textPrimary, fontSize = 14.sp)
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.trash),
                                        contentDescription = null,
                                        tint = Color(0xFFFF6B6B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    showItemMenu = false
                                    onDeleteClick(song)
                                }
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(start = 80.dp),
            color = themeColors.glassBorder.copy(alpha = 0.2f),
            thickness = 0.5.dp
        )
    }
}

@Composable
private fun MiniEqualizer(
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.height(12.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
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
                            colors = listOf(lerp(color, Color.White, 0.35f), color)
                        )
                    )
            )
        }
    }
}

fun Long.toTimeFormat(): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this) % 60
    return "%d:%02d".format(minutes, seconds)
}