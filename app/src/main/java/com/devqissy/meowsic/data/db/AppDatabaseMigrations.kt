package com.devqissy.meowsic.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE playlist_songs RENAME TO playlist_songs_old")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS playlist_songs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                playlistId INTEGER NOT NULL,
                songId INTEGER NOT NULL,
                position INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(playlistId) REFERENCES playlists(id) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO playlist_songs (id, playlistId, songId, position)
            SELECT id, playlistId, songId, `order` FROM playlist_songs_old
        """.trimIndent())
        db.execSQL("DROP TABLE playlist_songs_old")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_songs_playlistId ON playlist_songs(playlistId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_songs_songId ON playlist_songs(songId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_playlist_songs_playlistId_songId ON playlist_songs(playlistId, songId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_songs_playlistId_position ON playlist_songs(playlistId, position)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_playlists_isDefault ON playlists(isDefault)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_playlists_createdAt ON playlists(createdAt)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DELETE FROM playlist_songs WHERE id NOT IN (SELECT MIN(id) FROM playlist_songs GROUP BY playlistId, songId)")
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)