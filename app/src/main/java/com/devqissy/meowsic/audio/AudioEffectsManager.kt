package com.devqissy.meowsic.audio

import android.content.Context
import android.media.audiofx.*
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.devqissy.meowsic.data.preferences.EqPreferencesDataStore
import com.devqissy.meowsic.data.preferences.EqPrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioEffectsManager @OptIn(UnstableApi::class) @Inject constructor(
    @UnstableApi val reverbProcessor: ReverbAudioProcessor,
    private val dataStore: EqPreferencesDataStore,
    @ApplicationContext private val context: Context
) {
    private val tag = "AudioEffectsManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    @Volatile
    private var currentSessionId = -1

    private var presetJob: Job? = null
    private var saveJob: Job? = null

    private val _eqState = MutableStateFlow(EqState())
    val eqState: StateFlow<EqState> = _eqState.asStateFlow()

    private val _effectsState = MutableStateFlow(EffectsState())
    val effectsState: StateFlow<EffectsState> = _effectsState.asStateFlow()

    private val _eqAvailable = MutableStateFlow(false)
    val eqAvailable: StateFlow<Boolean> = _eqAvailable.asStateFlow()

    private val _needsFlush = MutableSharedFlow<Unit>(replay = 0)
    val needsFlush: SharedFlow<Unit> = _needsFlush.asSharedFlow()

    init {
        scope.launch {
            try {
                val prefs = dataStore.eqPrefsFlow.first()
                _effectsState.value = EffectsState(
                    bassBoostEnabled = prefs.bassEnabled,
                    bassBoostStrength = prefs.bassStrength,
                    virtualizerEnabled = prefs.virtEnabled,
                    virtualizerStrength = prefs.virtStrength,
                    reverbEnabled = prefs.reverbEnabled,
                    reverbPresetId = ReverbPreset.fromId(prefs.reverbPreset).id,
                    loudnessEnabled = prefs.loudnessEnabled,
                    loudnessGain = prefs.loudnessGain
                )
                _eqState.value = _eqState.value.copy(
                    bandLevels = prefs.bandLevels
                )
                if (currentSessionId != -1) {
                    applyCurrentStateToEffects()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(tag, "Init preload failed", e)
            }
        }
    }

    fun attachToSession(audioSessionId: Int) {
        if (audioSessionId == 0) return
        val isNewSession = audioSessionId != currentSessionId
        if (isNewSession) {
            releaseSessionEffects()
            currentSessionId = audioSessionId
        }

        if (equalizer == null) {
            equalizer = try {
                Equalizer(0, audioSessionId).apply {
                    enabled = true
                    val numBands = numberOfBands.toInt()
                    val freqs = (0 until numBands).map { getCenterFreq(it.toShort()) / 1000 }
                    val range = getBandLevelRange()
                    val savedLevels = _eqState.value.bandLevels
                    val normalizedLevels = when {
                        savedLevels.size >= numBands -> savedLevels.take(numBands)
                        savedLevels.isEmpty() -> List(numBands) { 0f }
                        else -> savedLevels + List(numBands - savedLevels.size) { 0f }
                    }
                    _eqState.value = _eqState.value.copy(
                        numBands = numBands,
                        bandFrequencies = freqs,
                        bandLevelRange = Pair(range[0] / 100, range[1] / 100),
                        bandLevels = normalizedLevels
                    )
                }
            } catch (e: Exception) {
                Log.w(tag, "Equalizer unavailable on this device/session", e)
                null
            }
            _eqAvailable.value = equalizer != null
        }

        if (bassBoost == null) {
            bassBoost = try {
                BassBoost(0, audioSessionId).apply { enabled = false }
            } catch (e: Exception) {
                Log.w(tag, "BassBoost unavailable on this device/session", e)
                null
            }
        }

        if (virtualizer == null) {
            virtualizer = try {
                Virtualizer(0, audioSessionId).apply { enabled = false }
            } catch (e: Exception) {
                Log.w(tag, "Virtualizer unavailable on this device/session", e)
                null
            }
        }

        if (loudnessEnhancer == null) {
            loudnessEnhancer = try {
                LoudnessEnhancer(audioSessionId).apply { enabled = false }
            } catch (e: Exception) {
                Log.w(tag, "LoudnessEnhancer unavailable on this device/session", e)
                null
            }
        }

        scope.launch { applyCurrentStateToEffects() }
    }

    private var retryCount = 0

    fun ensureAttached() {
        val id = currentSessionId
        if (id == -1 || id == 0) return
        if (equalizer == null || bassBoost == null ||
            virtualizer == null || loudnessEnhancer == null
        ) {
            if (retryCount >= 3) return
            retryCount++
            Log.d(tag, "ensureAttached: retry #$retryCount")
            attachToSession(id)
        }
    }

    @OptIn(UnstableApi::class)
    private suspend fun applyCurrentStateToEffects() {
        try {
            val effects = _effectsState.value
            val eq = _eqState.value

            val numBands = eq.numBands
            eq.bandLevels.take(numBands).forEachIndexed { i, level ->
                equalizer?.setBandLevel(i.toShort(), (level * 100).toInt().toShort())
            }

            if (bassBoost?.strengthSupported == true)
                bassBoost?.setStrength((effects.bassBoostStrength * 1000).toInt().toShort())
            bassBoost?.enabled = effects.bassBoostEnabled

            if (virtualizer?.strengthSupported == true)
                virtualizer?.setStrength((effects.virtualizerStrength * 1000).toInt().toShort())
            virtualizer?.enabled = effects.virtualizerEnabled

            loudnessEnhancer?.setTargetGain(effects.loudnessGain)
            loudnessEnhancer?.enabled = effects.loudnessEnabled

            if (effects.reverbEnabled) {
                reverbProcessor.applyPreset(effects.reverbPresetId)
                delay(500)
                _needsFlush.emit(Unit)
            }

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(tag, "applyCurrentStateToEffects failed", e)
        }
    }

    private fun saveAll() {
        saveJob?.cancel()
        saveJob = scope.launch {
            try {
                delay(300)
                dataStore.saveAll(
                    EqPrefs(
                        bandLevels = _eqState.value.bandLevels,
                        bassEnabled = _effectsState.value.bassBoostEnabled,
                        bassStrength = _effectsState.value.bassBoostStrength,
                        virtEnabled = _effectsState.value.virtualizerEnabled,
                        virtStrength = _effectsState.value.virtualizerStrength,
                        reverbEnabled = _effectsState.value.reverbEnabled,
                        reverbPreset = _effectsState.value.reverbPresetId,
                        loudnessEnabled = _effectsState.value.loudnessEnabled,
                        loudnessGain = _effectsState.value.loudnessGain
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(tag, "saveAll failed", e)
            }
        }
    }

    fun setBandLevel(bandIndex: Int, levelDb: Float) {
        try {
            equalizer?.setBandLevel(bandIndex.toShort(), (levelDb * 100).toInt().toShort())
            val newLevels = _eqState.value.bandLevels.toMutableList()
            while (newLevels.size <= bandIndex) newLevels.add(0f)
            newLevels[bandIndex] = levelDb
            _eqState.value = _eqState.value.copy(bandLevels = newLevels, selectedPreset = -1)
            saveAll()
        } catch (e: Exception) {
            Log.e(tag, "setBandLevel failed", e)
        }
    }

    fun applyPreset(presetIndex: Short) {
        presetJob?.cancel()
        presetJob = scope.launch {
            try {
                val eq = equalizer
                if (eq == null) {
                    Log.w(tag, "applyPreset: equalizer is null, audio session not initialized")
                    return@launch
                }
                val numBands = eq.numberOfBands.toInt()
                val range = eq.bandLevelRange

                _eqState.value = _eqState.value.copy(selectedPreset = presetIndex.toInt())

                try {
                    eq.usePreset(presetIndex)
                } catch (e: Exception) {
                    Log.e(tag, "usePreset($presetIndex) failed", e)
                }

                delay(50)

                if (_eqState.value.selectedPreset != presetIndex.toInt()) return@launch
                //  Fix: preset may have changed while waiting, abort to avoid race

                val levels = (0 until numBands).map { band ->
                    val raw = eq.getBandLevel(band.toShort())
                    (raw / 100f).coerceIn(range[0] / 100f, range[1] / 100f)
                }

                _eqState.value = _eqState.value.copy(bandLevels = levels)
                saveAll()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(tag, "applyPreset failed", e)
            }
        }
    }

    fun setBassBoost(enabled: Boolean, strength: Float) {
        try {
            bassBoost?.enabled = enabled
            if (bassBoost?.strengthSupported == true)
                bassBoost?.setStrength((strength * 1000).toInt().toShort())
            _effectsState.value = _effectsState.value.copy(
                bassBoostEnabled = enabled,
                bassBoostStrength = strength
            )
            saveAll()
        } catch (e: Exception) {
            Log.e(tag, "setBassBoost failed", e)
        }
    }

    fun setVirtualizer(enabled: Boolean, strength: Float) {
        try {
            virtualizer?.enabled = enabled
            if (virtualizer?.strengthSupported == true)
                virtualizer?.setStrength((strength * 1000).toInt().toShort())
            _effectsState.value = _effectsState.value.copy(
                virtualizerEnabled = enabled,
                virtualizerStrength = strength
            )
            saveAll()
        } catch (e: Exception) {
            Log.e(tag, "setVirtualizer failed", e)
        }
    }

    @OptIn(UnstableApi::class)
    suspend fun setReverb(enabled: Boolean, presetId: String) {
        val targetId = when {
            !enabled -> ReverbPreset.NONE.id
            presetId.isBlank() || presetId == ReverbPreset.NONE.id ->
                ReverbPreset.DEFAULT_ENABLED_ID
            else -> presetId
        }
        reverbProcessor.applyPreset(targetId)
        _effectsState.value = _effectsState.value.copy(
            reverbEnabled = enabled,
            reverbPresetId = if (enabled) targetId else presetId
        )
        saveAll()
        _needsFlush.emit(Unit)
    }

    fun setLoudness(enabled: Boolean, gainMb: Int) {
        try {
            loudnessEnhancer?.setTargetGain(gainMb)
            loudnessEnhancer?.enabled = enabled
            _effectsState.value = _effectsState.value.copy(
                loudnessEnabled = enabled,
                loudnessGain = gainMb
            )
            saveAll()
        } catch (e: Exception) {
            Log.e(tag, "setLoudness failed", e)
        }
    }

    @OptIn(UnstableApi::class)
    suspend fun resetAll() {
        try {
            val numBands = equalizer?.numberOfBands?.toInt() ?: 5
            repeat(numBands) { equalizer?.setBandLevel(it.toShort(), 0) }
            bassBoost?.enabled = false
            bassBoost?.setStrength(0)
            virtualizer?.enabled = false
            virtualizer?.setStrength(0)
            reverbProcessor.applyPreset(ReverbPreset.NONE.id)
            loudnessEnhancer?.setTargetGain(0)
            loudnessEnhancer?.enabled = false
            _eqState.value = EqState(
                numBands = numBands,
                bandLevels = List(numBands) { 0f },
                bandFrequencies = _eqState.value.bandFrequencies,
                bandLevelRange = _eqState.value.bandLevelRange
            )
            _effectsState.value = EffectsState(
                reverbPresetId = ReverbPreset.NONE.id,
                loudnessGain = 0
            )
            dataStore.clearAll()
            _needsFlush.emit(Unit)
        } catch (e: Exception) {
            Log.e(tag, "resetAll failed", e)
        }
    }

    private fun releaseSessionEffects() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            loudnessEnhancer?.release()
        } catch (e: Exception) {
            Log.e(tag, "releaseSessionEffects failed", e)
        } finally {
            equalizer = null
            bassBoost = null
            virtualizer = null
            loudnessEnhancer = null
            _eqAvailable.value = false
        }
    }

    fun release() {
        presetJob?.cancel()
        saveJob?.cancel()
        releaseSessionEffects()
        currentSessionId = -1
    }
}

data class EqState(
    val numBands: Int = 5,
    val bandLevels: List<Float> = List(5) { 0f },
    val bandFrequencies: List<Int> = listOf(60, 230, 910, 3600, 14000),
    val bandLevelRange: Pair<Int, Int> = Pair(-15, 15),
    val selectedPreset: Int = -1
)

data class EffectsState(
    val bassBoostEnabled: Boolean = false,
    val bassBoostStrength: Float = 0f,
    val virtualizerEnabled: Boolean = false,
    val virtualizerStrength: Float = 0f,
    val reverbEnabled: Boolean = false,
    val reverbPresetId: String = ReverbPreset.NONE.id,
    val loudnessEnabled: Boolean = false,
    val loudnessGain: Int = 300
)