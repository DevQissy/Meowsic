package com.devqissy.meowsic.service

import android.content.Context
import com.devqissy.meowsic.data.preferences.PrefsFiles

object VolumeKeyPrefs {

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PrefsFiles.SETTINGS, Context.MODE_PRIVATE)
            .getBoolean(PrefsFiles.KEY_VOLUME_KEY_NAVIGATION, false)

    fun setEnabled(context: Context, value: Boolean) {
        android.util.Log.d("VolumeKeyPrefs", "setEnabled($value) → ${PrefsFiles.SETTINGS}")
        context.getSharedPreferences(PrefsFiles.SETTINGS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PrefsFiles.KEY_VOLUME_KEY_NAVIGATION, value)
            .apply()
    }
}