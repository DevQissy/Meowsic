package com.devqissy.meowsic.data.local

import android.content.Context
import android.content.SharedPreferences
import com.devqissy.meowsic.data.preferences.PrefsFiles
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RatingPromptManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PrefsFiles.RATING_PROMPT, Context.MODE_PRIVATE)

    fun onFirstLaunch() {
        if (!prefs.contains(PrefsFiles.KEY_FIRST_LAUNCH)) {
            prefs.edit().putLong(PrefsFiles.KEY_FIRST_LAUNCH, System.currentTimeMillis()).apply()
        }
    }

    fun shouldShowRating(): Boolean {
        if (prefs.getBoolean(PrefsFiles.KEY_RATE_DONE, false)) return false
        val first = prefs.getLong(PrefsFiles.KEY_FIRST_LAUNCH, -1L)
        if (first == -1L) return false
        val now = System.currentTimeMillis()
        val nextPromptAt = prefs.getLong(PrefsFiles.KEY_NEXT_PROMPT, first + ONE_DAY_MS)
        return now >= nextPromptAt
    }

    fun onRateClicked() {
        prefs.edit().putBoolean(PrefsFiles.KEY_RATE_DONE, true).apply()
    }

    fun onDismissed() {
        prefs.edit()
            .putLong(PrefsFiles.KEY_NEXT_PROMPT, System.currentTimeMillis() + THREE_DAYS_MS)
            .apply()
    }

    companion object {
        private const val ONE_DAY_MS = 24L * 60 * 60 * 1000
        private const val THREE_DAYS_MS = 3 * ONE_DAY_MS
    }
}