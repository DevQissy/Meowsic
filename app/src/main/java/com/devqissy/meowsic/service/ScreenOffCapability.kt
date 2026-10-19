package com.devqissy.meowsic.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log

object ScreenOffCapability {

    private const val TAG = "ScreenOffCap"

    sealed class Status {

        data class NeedsAod(val aodEnabled: Boolean) : Status()

        object Unsupported : Status()
    }

    fun getStatus(context: Context): Status = when {
        Build.VERSION.SDK_INT >= 31 -> Status.NeedsAod(isAodEnabled(context))
        else -> Status.Unsupported
    }

    fun isAodEnabled(context: Context): Boolean {
        val keys = listOf(
            "doze_always_on",
            "aod_mode",
            "always_on_display",
            "aod_activated",
            "doze_enabled"
        )
        val resolver = context.contentResolver
        for (key in keys) {
            try {
                if (Settings.Secure.getInt(resolver, key) == 1) return true
            } catch (_: Settings.SettingNotFoundException) {
            } catch (e: Exception) {
                Log.w(TAG, "AOD check failed for '$key': ${e.message}")
            }
        }
        return false
    }

    fun openAodSettings(context: Context) {
        val candidates = listOf(
            "com.android.settings.DISPLAY_SETTINGS",
            "com.samsung.android.settings.display.ScreenTimeoutActivity",
            Settings.ACTION_DISPLAY_SETTINGS
        )
        for (action in candidates) {
            try {
                context.startActivity(
                    Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                return
            } catch (_: Exception) {}
        }
        context.startActivity(
            Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}