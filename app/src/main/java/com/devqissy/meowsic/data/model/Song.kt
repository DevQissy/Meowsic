package com.devqissy.meowsic.data.model

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: Uri,
    val albumArtUri: Uri?,
    val trackNumber: Int = 0,
    val year: Int = 0,
    val dateAdded: Long = 0L
)