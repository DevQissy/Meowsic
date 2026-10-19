package com.devqissy.meowsic.data.viewmodel

import android.app.PendingIntent
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.devqissy.meowsic.R
import com.devqissy.meowsic.audio.AudioEffectsManager
import com.devqissy.meowsic.data.db.PlaylistEntity
import com.devqissy.meowsic.data.local.LastSessionManager
import com.devqissy.meowsic.data.model.Song
import com.devqissy.meowsic.data.preferences.PrefsFiles
import com.devqissy.meowsic.data.repository.MusicRepository
import com.devqissy.meowsic.service.MusicService
import com.devqissy.meowsic.service.SleepTimerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class MusicViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MusicRepository,
    val audioEffectsManager: AudioEffectsManager,
    private val lastSessionManager: LastSessionManager,
    private val sleepTimerManager: SleepTimerManager
) : ViewModel() {

    private val tag = "MusicViewModel"

    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _playlist = MutableStateFlow<List<Song>>(emptyList())
    val playlist: StateFlow<List<Song>> = _playlist.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _prevSongInOrder = MutableStateFlow<Song?>(null)
    val prevSongInOrder: StateFlow<Song?> = _prevSongInOrder.asStateFlow()

    private val _nextSongInOrder = MutableStateFlow<Song?>(null)
    val nextSongInOrder: StateFlow<Song?> = _nextSongInOrder.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    private val _selectedPlaylistId = MutableStateFlow<Long?>(null)
    val selectedPlaylistId: StateFlow<Long?> = _selectedPlaylistId.asStateFlow()

    val playlists: StateFlow<List<PlaylistEntity>> = repository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _playlistSongIds = MutableStateFlow<Set<Long>>(emptySet())

    private val _playlistsWithoutSong = MutableStateFlow<List<PlaylistEntity>>(emptyList())
    val playlistsWithoutSong: StateFlow<List<PlaylistEntity>> = _playlistsWithoutSong.asStateFlow()

    private val _filteredSongs = MutableStateFlow<List<Song>>(emptyList())

    private val _favoriteSongIds = MutableStateFlow<Set<Long>>(emptySet())
    val favoriteSongIds: StateFlow<Set<Long>> = _favoriteSongIds.asStateFlow()

    val filteredSongs: StateFlow<List<Song>> = _filteredSongs.asStateFlow()

    val searchResults: StateFlow<List<Song>> =
        combine(_allSongs, _searchQuery) { songs, q ->
            if (q.isBlank()) emptyList()
            else songs.filter {
                it.title.contains(q, ignoreCase = true) ||
                        it.artist.contains(q, ignoreCase = true) ||
                        it.album.contains(q, ignoreCase = true)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sleepTimerRemaining: StateFlow<Long> = sleepTimerManager.remainingTime
    val sleepTimerIsRunning: StateFlow<Boolean> = sleepTimerManager.isRunning

    private var hasRestoredSession = false
    private var isServiceRunning = false
    private var isSongsLoaded = false

    private var pendingPlayRequest: Song? = null
    private var pendingPlayQueue: List<Song>? = null

    private val playbackPrefs by lazy {
        context.getSharedPreferences(PrefsFiles.PLAYBACK_STATE, Context.MODE_PRIVATE)
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!repository.hasDefaultPlaylist()) {
                    val favoritesName = context.getString(R.string.default_playlist_name)
                    repository.createPlaylist(favoritesName, isDefault = true)
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to ensure default playlist", e)
            }
        }

        viewModelScope.launch {
            _selectedPlaylistId.collectLatest { playlistId ->
                if (playlistId == null) _playlistSongIds.value = emptySet()
                else repository.getSongsInPlaylist(playlistId).collect { entities ->
                    _playlistSongIds.value = entities.map { it.songId }.toSet()
                }
            }
        }

        viewModelScope.launch {
            combine(
                _allSongs,
                _searchQuery,
                _selectedPlaylistId,
                _playlistSongIds
            ) { allSongs, query, playlistId, playlistSongIds ->
                var result = allSongs
                if (playlistId != null) result = result.filter { it.id in playlistSongIds }
                if (query.isNotBlank()) result = result.filter {
                    it.title.contains(query, ignoreCase = true) ||
                            it.artist.contains(query, ignoreCase = true)
                }
                result
            }
                .debounce(150)
                .distinctUntilChanged { old, new ->
                    old.size == new.size && old.map { it.id } == new.map { it.id }
                }
                .collect { newList ->
                    _filteredSongs.value = newList

                    if (!hasRestoredSession && newList.isNotEmpty() && !isServiceRunning) {
                        restoreLastSession(newList)
                        hasRestoredSession = true
                    }
                }
        }

        viewModelScope.launch {
            _allSongs.collect { songs ->
                _playlist.value = songs
                updateCurrentIndex()
            }
        }

        viewModelScope.launch {
            _currentSong.collect {
                updateCurrentIndex()
            }
        }

        viewModelScope.launch {
            playlists
                .map { list -> list.firstOrNull { it.isDefault }?.id }
                .filterNotNull()
                .flatMapLatest { favId ->
                    repository.getSongsInPlaylist(favId).map { entities ->
                        entities.map { it.songId }.toSet()
                    }
                }
                .collect { ids -> _favoriteSongIds.value = ids }
        }

    }

    private fun updateCurrentIndex() {
        val current = _currentSong.value
        val list = _playlist.value
        if (current != null && list.isNotEmpty()) {
            val index = list.indexOfFirst { it.id == current.id }
            if (index != -1 && _currentIndex.value != index) {
                _currentIndex.value = index
            }
        } else {
            _currentIndex.value = -1
        }
    }

    private fun updateNeighborSongs() {
        val controller = mediaController
        if (controller == null || controller.mediaItemCount == 0) {
            _prevSongInOrder.value = null
            _nextSongInOrder.value = null
            return
        }

        fun songAt(windowIndex: Int): Song? {
            if (windowIndex < 0) return null
            val mediaId = controller.getMediaItemAt(windowIndex)
                .mediaId.toLongOrNull()
            return _allSongs.value.find { it.id == mediaId }
                ?: _playlist.value.find { it.id == mediaId }
        }

        _prevSongInOrder.value = songAt(controller.previousMediaItemIndex)
        _nextSongInOrder.value = songAt(controller.nextMediaItemIndex)
    }

    fun reloadAll() {
        if (isSongsLoaded) return
        isSongsLoaded = true

        viewModelScope.launch {
            loadSongsSuspend()
            initMediaController()
        }
    }

    private val _deletePermissionRequest = MutableSharedFlow<PendingIntent>()
    val deletePermissionRequest: SharedFlow<PendingIntent> = _deletePermissionRequest.asSharedFlow()
    private var pendingDeleteSong: Song? = null

    private var mediaController: MediaController? = null

    private fun restoreLastSession(songs: List<Song>) {
        val lastSongId = lastSessionManager.getLastSongId()
        if (lastSongId != -1L && lastSongId > 0) {
            val song = songs.find { it.id == lastSongId }
            if (song != null) {
                _currentSong.value = song
                updateCurrentIndex()
            }
        }
    }

    private fun initMediaController(retry: Int = 0) {
        viewModelScope.launch {
            try {
                val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
                val controller = MediaController.Builder(context, sessionToken)
                    .buildAsync()
                    .await()

                mediaController = controller
                controller.addListener(playerListener)

                val currentMediaItem = controller.currentMediaItem
                val isControllerPlaying = controller.isPlaying

                if (currentMediaItem != null) {
                    isServiceRunning = true
                    val currentSongId = currentMediaItem.mediaId.toLongOrNull()
                    val song = _allSongs.value.find { it.id == currentSongId }
                        ?: _playlist.value.find { it.id == currentSongId }

                    _currentSong.value = song
                    _isPlaying.value = isControllerPlaying

                    if (controller.playbackState == Player.STATE_READY) {
                        _duration.value = controller.duration
                        _currentPosition.value = controller.currentPosition
                    }

                    _shuffleEnabled.value = controller.shuffleModeEnabled
                    _repeatMode.value = controller.repeatMode

                    controller.playbackParameters?.let { params ->
                        _playbackSpeed.value = params.speed
                    }

                    updateCurrentIndex()
                } else if (_currentSong.value != null && _allSongs.value.isNotEmpty()) {
                    playSong(_currentSong.value!!, autoPlay = false)
                    isServiceRunning = true
                }

                val savedShuffle = playbackPrefs.getBoolean(PrefsFiles.KEY_SHUFFLE, false)
                val savedRepeat = playbackPrefs.getInt(PrefsFiles.KEY_REPEAT, Player.REPEAT_MODE_OFF)

                if (controller.shuffleModeEnabled != savedShuffle) {
                    controller.shuffleModeEnabled = savedShuffle
                }
                if (controller.repeatMode != savedRepeat) {
                    controller.repeatMode = savedRepeat
                }
                _shuffleEnabled.value = controller.shuffleModeEnabled
                _repeatMode.value = controller.repeatMode

                updateNeighborSongs()

                pendingPlayRequest?.let { song ->
                    val queue = pendingPlayQueue ?: listOf(song)
                    pendingPlayRequest = null
                    pendingPlayQueue = null
                    Log.d(tag, "Controller connected → executing queued play (${song.title})")
                    playSong(song, queue, autoPlay = true)
                }
            } catch (e: Exception) {
                Log.e(tag, "initMediaController failed (attempt ${retry + 1})", e)
                try {
                    mediaController?.release()
                } catch (_: Exception) {
                }
                mediaController = null

                if (retry < MAX_CONTROLLER_RETRIES) {
                    delay(500L * (retry + 1))
                    initMediaController(retry + 1)
                }
            }
        }
    }

    fun playExternalUri(uri: Uri) {
        viewModelScope.launch {
            val song = resolveExternalSong(uri)

            val librarySong = _allSongs.value.find { it.id == song.id }
            if (librarySong != null) {
                playSong(librarySong)
                return@launch
            }

            _currentSong.value = song
            _playlist.value = listOf(song)
            _currentIndex.value = 0

            playSong(song, listOf(song), autoPlay = true)
        }
    }

    private suspend fun resolveExternalSong(uri: Uri): Song = withContext(Dispatchers.IO) {
        if (uri.authority == MediaStore.AUTHORITY) {
            val id = uri.lastPathSegment?.toLongOrNull()
            if (id != null) {
                repository.getAllSongs().firstOrNull { it.id == id }?.let { return@withContext it }
            }
        }

        var title = uri.lastPathSegment?.substringAfterLast('/') ?: "Unknown"
        var artist = "Unknown Artist"
        var album = ""
        var durationMs = 0L
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: title
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: artist
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: album
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            Log.w(tag, "Failed to read metadata for external URI", e)
        } finally {
            retriever.release()
        }

        Song(
            id = -System.currentTimeMillis(),
            title = title,
            artist = artist,
            album = album,
            duration = durationMs,
            uri = uri,
            albumArtUri = null,
            trackNumber = 0,
            year = 0,
            dateAdded = 0
        )
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        mediaController?.let { controller ->
            val playbackParameters = PlaybackParameters(speed)
            controller.setPlaybackParameters(playbackParameters)
        }
    }

    private suspend fun loadSongsSuspend() {
        val songs = repository.getAllSongs()
        withContext(Dispatchers.Main) {
            _allSongs.value = songs
            _playlist.value = songs
            updateCurrentIndex()
        }
    }

    fun playSong(song: Song, queue: List<Song> = _allSongs.value, autoPlay: Boolean = true) {
        val controller = mediaController

        if (controller == null) {
            Log.d(tag, "playSong: controller not ready → queueing (${song.title})")
            pendingPlayRequest = song
            pendingPlayQueue = queue
            _currentSong.value = song
            _currentIndex.value = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            _playlist.value = queue
            if (song.id > 0) lastSessionManager.save(song.id)
            return
        }

        val mediaItems = queue.map { s ->
            MediaItem.Builder()
                .setMediaId(s.id.toString())
                .setUri(s.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(s.title)
                        .setArtist(s.artist)
                        .setAlbumTitle(s.album)
                        .setArtworkUri(s.albumArtUri)
                        .build()
                )
                .build()
        }

        val startIndex = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

        controller.run {
            setMediaItems(mediaItems, startIndex, 0L)
            prepare()
            if (autoPlay) {
                play()
            }
        }

        _currentSong.value = song
        _currentIndex.value = startIndex
        _playlist.value = queue
        if (song.id > 0) {
            lastSessionManager.save(song.id)
        }
    }

    fun togglePlayPause() {
        val controller = mediaController
        if (controller == null) {
            _currentSong.value?.let { song ->
                Log.d(tag, "togglePlayPause: controller not ready → queueing (${song.title})")
                pendingPlayRequest = song
                pendingPlayQueue = _playlist.value.ifEmpty { listOf(song) }
            }
            return
        }

        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.currentMediaItem == null && _currentSong.value != null) {
                playSong(_currentSong.value!!, _playlist.value.ifEmpty { listOf(_currentSong.value!!) }, autoPlay = true)
            } else {
                controller.play()
            }
            isServiceRunning = true
        }
    }

    fun skipToNext() {
        mediaController?.let { controller ->
            if (controller.currentMediaItem == null && _currentSong.value != null) {
                playSong(_currentSong.value!!, _playlist.value.ifEmpty { listOf(_currentSong.value!!) }, autoPlay = true)
                return
            }
            if (controller.mediaItemCount == 0) return

            if (controller.hasNextMediaItem()) {
                controller.seekToNextMediaItem()
            } else {
                controller.seekTo(0, 0L)
            }
        }
    }

    fun skipToPrevious() {
        mediaController?.let { controller ->
            if (controller.currentMediaItem == null && _currentSong.value != null) {
                playSong(_currentSong.value!!, _playlist.value.ifEmpty { listOf(_currentSong.value!!) }, autoPlay = true)
                return
            }
            if (controller.mediaItemCount == 0) return

            if (controller.hasPreviousMediaItem()) {
                controller.seekToPreviousMediaItem()
            } else {
                controller.seekTo(controller.mediaItemCount - 1, 0L)
            }
        }
    }

    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
        _currentPosition.value = position
    }

    fun toggleShuffle() {
        mediaController?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeatMode() {
        mediaController?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    private var lastPositionUpdate = 0L
    fun updatePosition() {
        mediaController?.let { _currentPosition.value = it.currentPosition }
    }

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun selectPlaylistFilter(playlistId: Long?) { _selectedPlaylistId.value = playlistId }

    fun createPlaylist(name: String, isDefault: Boolean = false) {
        viewModelScope.launch { repository.createPlaylist(name, isDefault) }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        if (playlist.isDefault) return

        viewModelScope.launch {
            if (_selectedPlaylistId.value == playlist.id) _selectedPlaylistId.value = null
            repository.deletePlaylistById(playlist.id)
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch { repository.addSongToPlaylist(playlistId, songId) }
    }

    fun removeSongFromPlaylist(songId: Long) {
        val currentPlaylistId = _selectedPlaylistId.value ?: return
        viewModelScope.launch {
            repository.removeSongFromPlaylist(currentPlaylistId, songId)
        }
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val fav = repository.getDefaultPlaylist() ?: return@launch
            if (_favoriteSongIds.value.contains(songId)) {
                repository.removeSongFromPlaylist(fav.id, songId)
            } else {
                repository.addSongToPlaylist(fav.id, songId)
            }
        }
    }

    private var playlistsWithoutSongJob: Job? = null

    fun loadPlaylistsWithoutSong(songId: Long) {
        playlistsWithoutSongJob?.cancel()
        playlistsWithoutSongJob = viewModelScope.launch {
            repository.getPlaylistsWithoutSong(songId).collect { playlists ->
                _playlistsWithoutSong.value = playlists
            }
        }
    }

    fun resetAll() {
        viewModelScope.launch { audioEffectsManager.resetAll() }
    }

    fun setReverb(enabled: Boolean, presetName: String) {
        viewModelScope.launch { audioEffectsManager.setReverb(enabled, presetName) }
    }

    fun startSleepTimer(minutes: Long) {
        sleepTimerManager.startTimer(minutes)
    }

    fun stopSleepTimer() {
        sleepTimerManager.stopTimer()
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch {
            try {
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    pendingDeleteSong = song
                    _deletePermissionRequest.emit(
                        MediaStore.createDeleteRequest(context.contentResolver, listOf(contentUri))
                    )
                } else {
                    context.contentResolver.delete(contentUri, null, null)
                    removeSongFromState(song)
                }
            } catch (e: Exception) {
                Log.e(tag, "deleteSong failed", e)
            }
        }
    }

    fun onDeleteConfirmed() {
        pendingDeleteSong?.let {
            removeSongFromState(it)
            pendingDeleteSong = null
        }
    }

    fun onDeleteCancelled() {
        pendingDeleteSong = null
    }

    private fun removeSongFromState(song: Song) {
        viewModelScope.launch {
            repository.removeSongFromCache(song.id)

            mediaController?.let { controller ->
                val removeIndex = (0 until controller.mediaItemCount)
                    .firstOrNull { controller.getMediaItemAt(it).mediaId.toLongOrNull() == song.id }
                if (removeIndex != null) {
                    controller.removeMediaItem(removeIndex)
                }
            }

            val newList = _allSongs.value.filter { it.id != song.id }
            _allSongs.value = newList
            _playlist.value = newList

            if (mediaController == null && _currentSong.value?.id == song.id) {
                _currentSong.value = null
                _currentIndex.value = -1
            }

            updateCurrentIndex()
            updateNeighborSongs()
        }
    }

    override fun onCleared() {
        try {
            mediaController?.removeListener(playerListener)
            mediaController?.release()
        } catch (e: Exception) {
            Log.e(tag, "onCleared cleanup failed", e)
        } finally {
            mediaController = null
        }
        super.onCleared()
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
                isServiceRunning = true
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val mediaId = mediaItem?.mediaId?.toLongOrNull()
            val inLibrary = mediaId?.let { id -> _allSongs.value.find { it.id == id } }

            _currentSong.value = when {
                inLibrary != null -> inLibrary
                mediaId != null && _currentSong.value?.id == mediaId -> _currentSong.value
                mediaId != null -> _playlist.value.find { it.id == mediaId }
                else -> null
            }

            mediaController?.let { c ->
                val d = c.duration
                if (d > 0) _duration.value = d
                _currentPosition.value = c.currentPosition
                lastPositionUpdate = 0L
            }

            updateCurrentIndex()
            updateNeighborSongs()

            _currentSong.value?.let {
                if (it.id > 0) lastSessionManager.save(it.id)
            }
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            updateNeighborSongs()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                _duration.value = mediaController?.duration ?: 0L
                _currentPosition.value = mediaController?.currentPosition ?: 0L
                mediaController?.playbackParameters?.let { params ->
                    _playbackSpeed.value = params.speed
                }
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _shuffleEnabled.value = shuffleModeEnabled
            playbackPrefs.edit().putBoolean(PrefsFiles.KEY_SHUFFLE, shuffleModeEnabled).apply()
            updateNeighborSongs()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode
            playbackPrefs.edit().putInt(PrefsFiles.KEY_REPEAT, repeatMode).apply()
            updateNeighborSongs()
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            _playbackSpeed.value = playbackParameters.speed
        }
    }

    companion object {
        private const val MAX_CONTROLLER_RETRIES = 5
    }
}