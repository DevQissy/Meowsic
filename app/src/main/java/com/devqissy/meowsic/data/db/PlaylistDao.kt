package com.devqissy.meowsic.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY isDefault DESC, createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position")
    fun getSongsInPlaylist(playlistId: Long): Flow<List<PlaylistSongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongToPlaylist(song: PlaylistSongEntity)

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getMaxPosition(playlistId: Long): Int

    @Transaction
    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        val nextPosition = getMaxPosition(playlistId) + 1
        insertSongToPlaylist(
            PlaylistSongEntity(
                playlistId = playlistId,
                songId = songId,
                position = nextPosition
            )
        )
    }

    @Query("DELETE FROM playlists WHERE id = :id AND isDefault = 0")
    suspend fun deletePlaylistById(id: Long)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun deletePlaylistSongs(playlistId: Long)

    @Transaction
    suspend fun deletePlaylistWithSongs(playlistId: Long) {
        deletePlaylistSongs(playlistId)
        deletePlaylistById(playlistId)
    }

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM playlists WHERE isDefault = 1 LIMIT 1)")
    suspend fun hasDefaultPlaylist(): Boolean

    @Query("SELECT * FROM playlists WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultPlaylist(): PlaylistEntity?

    @Query("""
        SELECT p.* FROM playlists p
        INNER JOIN playlist_songs ps ON p.id = ps.playlistId
        WHERE ps.songId = :songId
        ORDER BY p.isDefault DESC, p.createdAt DESC
    """)
    fun getPlaylistsForSong(songId: Long): Flow<List<PlaylistEntity>>

    @Query("""
        SELECT * FROM playlists p
        WHERE p.id NOT IN (
            SELECT ps.playlistId FROM playlist_songs ps
            WHERE ps.songId = :songId
        )
        ORDER BY p.isDefault DESC, p.createdAt DESC
    """)
    fun getPlaylistsWithoutSong(songId: Long): Flow<List<PlaylistEntity>>
}