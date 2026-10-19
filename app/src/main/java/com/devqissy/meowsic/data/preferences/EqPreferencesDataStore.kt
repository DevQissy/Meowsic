package com.devqissy.meowsic.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.devqissy.meowsic.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.eqDataStore: DataStore<Preferences> by preferencesDataStore(name = "eq_preferences")

@Singleton
class EqPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val ds = context.eqDataStore
    private val defaultReverbPreset: String = context.getString(R.string.no_reverb)

    companion object {
        const val MAX_BANDS = 10

        val BAND_KEYS = (0 until MAX_BANDS).map { floatPreferencesKey("eq_band_$it") }

        val BASS_ENABLED = booleanPreferencesKey("bass_enabled")
        val BASS_STRENGTH = floatPreferencesKey("bass_strength")

        val VIRT_ENABLED = booleanPreferencesKey("virt_enabled")
        val VIRT_STRENGTH = floatPreferencesKey("virt_strength")

        val REVERB_ENABLED = booleanPreferencesKey("reverb_enabled")
        val REVERB_PRESET = stringPreferencesKey("reverb_preset")

        val LOUDNESS_ENABLED = booleanPreferencesKey("loudness_enabled")
        val LOUDNESS_GAIN = intPreferencesKey("loudness_gain")
    }

    val eqPrefsFlow: Flow<EqPrefs> = ds.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences())
            else throw e
        }
        .map { prefs ->
            EqPrefs(
                bandLevels = BAND_KEYS.map { prefs[it] ?: 0f },
                bassEnabled = prefs[BASS_ENABLED] ?: false,
                bassStrength = prefs[BASS_STRENGTH] ?: 0f,
                virtEnabled = prefs[VIRT_ENABLED] ?: false,
                virtStrength = prefs[VIRT_STRENGTH] ?: 0f,
                reverbEnabled = prefs[REVERB_ENABLED] ?: false,
                reverbPreset = prefs[REVERB_PRESET] ?: defaultReverbPreset,
                loudnessEnabled = prefs[LOUDNESS_ENABLED] ?: false,
                loudnessGain = prefs[LOUDNESS_GAIN] ?: 300
            )
        }

    suspend fun saveAll(prefs: EqPrefs) {
        ds.edit { p ->
            prefs.bandLevels.forEachIndexed { i, v ->
                if (i < MAX_BANDS) p[BAND_KEYS[i]] = v
            }
            p[BASS_ENABLED] = prefs.bassEnabled
            p[BASS_STRENGTH] = prefs.bassStrength
            p[VIRT_ENABLED] = prefs.virtEnabled
            p[VIRT_STRENGTH] = prefs.virtStrength
            p[REVERB_ENABLED] = prefs.reverbEnabled
            p[REVERB_PRESET] = prefs.reverbPreset
            p[LOUDNESS_ENABLED] = prefs.loudnessEnabled
            p[LOUDNESS_GAIN] = prefs.loudnessGain
        }
    }

    suspend fun clearAll() {
        ds.edit { it.clear() }
    }
}

data class EqPrefs(
    val bandLevels: List<Float> = List(EqPreferencesDataStore.MAX_BANDS) { 0f },
    val bassEnabled: Boolean = false,
    val bassStrength: Float = 0f,
    val virtEnabled: Boolean = false,
    val virtStrength: Float = 0f,
    val reverbEnabled: Boolean = false,
    val reverbPreset: String = "",
    val loudnessEnabled: Boolean = false,
    val loudnessGain: Int = 300
)