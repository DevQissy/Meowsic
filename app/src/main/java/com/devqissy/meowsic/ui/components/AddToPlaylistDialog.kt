package com.devqissy.meowsic.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devqissy.meowsic.R
import com.devqissy.meowsic.data.db.PlaylistEntity
import com.devqissy.meowsic.data.model.Song
import com.devqissy.meowsic.ui.theme.AppThemeColors
import com.devqissy.meowsic.ui.theme.GlassShapes
import com.devqissy.meowsic.ui.theme.glassSheen
import com.devqissy.meowsic.ui.theme.glassSurface

@Composable
fun AddToPlaylistDialog(
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
                    stringResource(R.string.song_already_in_all_playlists)
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