package com.devqissy.meowsic.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.devqissy.meowsic.data.db.PlaylistDao
import com.devqissy.meowsic.data.db.PlaylistEntity
import com.devqissy.meowsic.data.db.PlaylistSongEntity
import com.devqissy.meowsic.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playlistDao: PlaylistDao
) {

    private val tag = "MusicRepository"

    private var cachedSongs: List<Song>? = null
    private var lastLoadTime: Long = 0
    private val cacheExpiryMs = 30_000L
    private val cacheMutex = Mutex()

    suspend fun getAllSongs(): List<Song> = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            val now = System.currentTimeMillis()

            cachedSongs?.takeIf { (now - lastLoadTime) < cacheExpiryMs }?.let {
                return@withLock it
            }

            val songs = mutableListOf<Song>()

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.DATE_ADDED
            )

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

            val cursor: Cursor? = context.contentResolver.query(
                collection, projection, selection, null, sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val trackCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val dateAddedCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val albumId = c.getLong(albumIdCol)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                    )
                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"), albumId
                    )

                    songs.add(
                        Song(
                            id = id,
                            title = c.getString(titleCol) ?: "Unknown",
                            artist = c.getString(artistCol) ?: "Unknown Artist",
                            album = c.getString(albumCol) ?: "Unknown Album",
                            duration = c.getLong(durationCol),
                            uri = contentUri,
                            albumArtUri = albumArtUri,
                            trackNumber = c.getInt(trackCol),
                            year = c.getInt(yearCol),
                            dateAdded = c.getLong(dateAddedCol)
                        )
                    )
                }
            }

            cachedSongs = songs
            lastLoadTime = now

            songs
        }
    }

    suspend fun invalidateCache() = cacheMutex.withLock {
        cachedSongs = null
        lastLoadTime = 0
    }

    suspend fun removeSongFromCache(songId: Long) = cacheMutex.withLock {
        cachedSongs = cachedSongs?.filter { it.id != songId }
    }

    suspend fun searchSongs(query: String): List<Song> {
        val allSongs = getAllSongs()
        return allSongs.filter { song ->
            song.title.contains(query, ignoreCase = true) ||
                    song.artist.contains(query, ignoreCase = true) ||
                    song.album.contains(query, ignoreCase = true)
        }
    }

    suspend fun refreshSongs() {
        invalidateCache()
        getAllSongs()
    }

    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    fun getPlaylistsForSong(songId: Long): Flow<List<PlaylistEntity>> =
        playlistDao.getPlaylistsForSong(songId)

    fun getPlaylistsWithoutSong(songId: Long): Flow<List<PlaylistEntity>> =
        playlistDao.getPlaylistsWithoutSong(songId)

    fun getSongsInPlaylist(playlistId: Long): Flow<List<PlaylistSongEntity>> =
        playlistDao.getSongsInPlaylist(playlistId)

    suspend fun createPlaylist(name: String, isDefault: Boolean = false): Long =
        playlistDao.insertPlaylist(
            PlaylistEntity(
                name = name,
                createdAt = System.currentTimeMillis(),
                isDefault = isDefault
            )
        )

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) =
        playlistDao.addSongToPlaylist(playlistId, songId)

    suspend fun deletePlaylistById(id: Long) =
        playlistDao.deletePlaylistWithSongs(id)

    suspend fun hasDefaultPlaylist(): Boolean = playlistDao.hasDefaultPlaylist()

    suspend fun getDefaultPlaylist(): PlaylistEntity? = playlistDao.getDefaultPlaylist()

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) =
        playlistDao.removeSongFromPlaylist(playlistId, songId)
}