package com.devqissy.meowsic.service

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import com.devqissy.meowsic.data.local.LastSessionManager
import com.devqissy.meowsic.data.model.Song
import com.devqissy.meowsic.data.repository.MusicRepository
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(UnstableApi::class)
class MusicSessionCallback @Inject constructor(
    private val repository: MusicRepository,
    private val lastSessionManager: LastSessionManager
) : MediaSession.Callback {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>
    ): ListenableFuture<List<MediaItem>> {
        val resolvedItems = mediaItems.mapNotNull { item ->
            if (item.localConfiguration != null) {
                item
            } else {
                item.requestMetadata.mediaUri?.let { uri ->
                    item.buildUpon().setUri(uri).build()
                }
            }
        }
        return Futures.immediateFuture(resolvedItems)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo
    ): ListenableFuture<MediaItemsWithStartPosition> {
        val future = SettableFuture.create<MediaItemsWithStartPosition>()

        scope.launch {
            try {
                val songs = repository.getAllSongs()
                if (songs.isEmpty()) {
                    future.set(MediaItemsWithStartPosition(emptyList(), 0, 0L))
                    return@launch
                }

                val items = songs.map { it.toMediaItem() }
                val lastId = lastSessionManager.getLastSongId()
                val index = songs.indexOfFirst { it.id == lastId }.takeIf { it >= 0 } ?: 0

                future.set(MediaItemsWithStartPosition(items, index, 0L))
            } catch (e: Exception) {
                future.setException(e)
            }
        }

        return future
    }

    private fun Song.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(albumArtUri)
                    .build()
            )
            .build()
}