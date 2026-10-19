package com.devqissy.meowsic.data.local

import android.content.Context
import android.content.SharedPreferences
import com.devqissy.meowsic.data.preferences.PrefsFiles
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LastSessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PrefsFiles.LAST_SESSION, Context.MODE_PRIVATE)

    fun save(songId: Long) {
        prefs.edit().putLong(PrefsFiles.KEY_LAST_SONG_ID, songId).apply()
    }

    fun getLastSongId(): Long = prefs.getLong(PrefsFiles.KEY_LAST_SONG_ID, -1L)

    fun clear() {
        prefs.edit().clear().apply()
    }
}