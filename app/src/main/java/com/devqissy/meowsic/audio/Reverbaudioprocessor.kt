package com.devqissy.meowsic.audio

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class ReverbAudioProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) : AudioProcessor {

    private val tag = "ReverbProcessor"

    private var currentFormat = AudioFormat.NOT_SET
    private var outputBuffer = ByteBuffer.allocate(0)
    private var inputEnded = false

    @Volatile var enabled = false
    @Volatile var roomSize = 0.5f
    @Volatile var damping = 0.5f
    @Volatile var wetLevel = 0.33f
    @Volatile var dryLevel = 0.7f
    @Volatile var width = 1.0f

    private val numCombs = 8
    private val numAllpasses = 4

    private val combTuningL = intArrayOf(1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617)
    private val combTuningR = intArrayOf(1139, 1211, 1300, 1379, 1445, 1514, 1580, 1640)
    private val allpassTuningL = intArrayOf(556, 441, 341, 225)
    private val allpassTuningR = intArrayOf(579, 464, 364, 248)

    private var combBufL = Array(numCombs) { FloatArray(0) }
    private var combBufR = Array(numCombs) { FloatArray(0) }
    private var allpassBufL = Array(numAllpasses) { FloatArray(0) }
    private var allpassBufR = Array(numAllpasses) { FloatArray(0) }
    private var combIdxL = IntArray(numCombs)
    private var combIdxR = IntArray(numCombs)
    private var apIdxL = IntArray(numAllpasses)
    private var apIdxR = IntArray(numAllpasses)
    private var filterL = FloatArray(numCombs)
    private var filterR = FloatArray(numCombs)

    private var buffersReady = false

    private var outL = 0f
    private var outR = 0f

    private val presetMap: Map<String, PresetConfig> = mapOf(
        ReverbPreset.NONE.id to PresetConfig(false, 0f, 0f, 0f, 0f, 0f),
        ReverbPreset.STUDIO.id to PresetConfig(true, 0.2f, 0.9f, 0.25f, 0.8f, 0.8f),
        ReverbPreset.SMALL_ROOM.id to PresetConfig(true, 0.35f, 0.7f, 0.33f, 0.7f, 0.8f),
        ReverbPreset.MEDIUM_ROOM.id to PresetConfig(true, 0.5f, 0.6f, 0.38f, 0.65f, 0.9f),
        ReverbPreset.LARGE_ROOM.id to PresetConfig(true, 0.65f, 0.5f, 0.43f, 0.6f, 1.0f),
        ReverbPreset.CONCERT_HALL.id to PresetConfig(true, 0.8f, 0.4f, 0.5f, 0.55f, 1.0f),
        ReverbPreset.LARGE_HALL.id to PresetConfig(true, 0.88f, 0.3f, 0.55f, 0.5f, 1.0f),
        ReverbPreset.OPEN_SPACE.id to PresetConfig(true, 0.75f, 0.2f, 0.45f, 0.6f, 1.0f),
        ReverbPreset.CAVE.id to PresetConfig(true, 0.95f, 0.1f, 0.65f, 0.45f, 1.0f)
    )

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        currentFormat = inputAudioFormat
        allocateBuffers(inputAudioFormat.sampleRate)
        return inputAudioFormat
    }

    override fun isActive(): Boolean = enabled

    override fun queueInput(inputBuffer: ByteBuffer) {
        val inputBytes = inputBuffer.remaining()
        if (outputBuffer.capacity() < inputBytes) {
            outputBuffer = ByteBuffer.allocate(inputBytes).order(ByteOrder.LITTLE_ENDIAN)
        } else {
            outputBuffer.clear()
        }

        val input = inputBuffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        val isStereo = currentFormat.channelCount >= 2

        while (if (isStereo) input.remaining() >= 4 else input.remaining() >= 2) {
            val sL = input.short.toFloat() / 32768f
            val sR = if (isStereo) input.short.toFloat() / 32768f else sL

            processFreeverb(sL, sR)

            outputBuffer.putShort(outL.coerceIn(-1f, 1f).times(32767f).toInt().toShort())
            if (isStereo) outputBuffer.putShort(outR.coerceIn(-1f, 1f).times(32767f).toInt().toShort())
        }

        inputBuffer.position(inputBuffer.limit())
        outputBuffer.flip()
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer =
        if (outputBuffer.hasRemaining()) outputBuffer else AudioProcessor.EMPTY_BUFFER

    override fun isEnded(): Boolean = inputEnded && !outputBuffer.hasRemaining()

    override fun flush() {
        inputEnded = false
        outputBuffer.clear()
        clearBuffers()
    }

    override fun reset() {
        currentFormat = AudioFormat.NOT_SET
        outputBuffer = ByteBuffer.allocate(0)
        inputEnded = false
        clearBuffers()
    }

    private fun allocateBuffers(sampleRate: Int) {
        val scale = sampleRate / 44100f
        combBufL = Array(numCombs) { i ->
            FloatArray((combTuningL[i] * scale).toInt().coerceAtLeast(1))
        }
        combBufR = Array(numCombs) { i ->
            FloatArray((combTuningR[i] * scale).toInt().coerceAtLeast(1))
        }
        allpassBufL = Array(numAllpasses) { i ->
            FloatArray((allpassTuningL[i] * scale).toInt().coerceAtLeast(1))
        }
        allpassBufR = Array(numAllpasses) { i ->
            FloatArray((allpassTuningR[i] * scale).toInt().coerceAtLeast(1))
        }
        combIdxL = IntArray(numCombs)
        combIdxR = IntArray(numCombs)
        apIdxL = IntArray(numAllpasses)
        apIdxR = IntArray(numAllpasses)
        filterL = FloatArray(numCombs)
        filterR = FloatArray(numCombs)
        buffersReady = true
    }

    private fun clearBuffers() {
        if (!buffersReady) return
        combBufL.forEach { it.fill(0f) }
        combBufR.forEach { it.fill(0f) }
        allpassBufL.forEach { it.fill(0f) }
        allpassBufR.forEach { it.fill(0f) }
        combIdxL.fill(0)
        combIdxR.fill(0)
        apIdxL.fill(0)
        apIdxR.fill(0)
        filterL.fill(0f)
        filterR.fill(0f)
    }

    private fun processFreeverb(inL: Float, inR: Float) {
        val feedback = roomSize * 0.28f + 0.7f
        val damp1 = damping * 0.4f
        val damp2 = 1f - damp1
        val input = (inL + inR) * 0.015f

        var accL = 0f
        var accR = 0f

        for (i in 0 until numCombs) {
            val bL = combBufL[i]
            val cL = bL[combIdxL[i]]
            filterL[i] = cL * damp2 + filterL[i] * damp1
            bL[combIdxL[i]] = input + filterL[i] * feedback
            combIdxL[i] = (combIdxL[i] + 1) % bL.size
            accL += cL

            val bR = combBufR[i]
            val cR = bR[combIdxR[i]]
            filterR[i] = cR * damp2 + filterR[i] * damp1
            bR[combIdxR[i]] = input + filterR[i] * feedback
            combIdxR[i] = (combIdxR[i] + 1) % bR.size
            accR += cR
        }

        for (i in 0 until numAllpasses) {
            val bL = allpassBufL[i]
            val bO = bL[apIdxL[i]]
            bL[apIdxL[i]] = accL + bO * 0.5f
            apIdxL[i] = (apIdxL[i] + 1) % bL.size
            accL = bO - accL

            val bR = allpassBufR[i]
            val bOR = bR[apIdxR[i]]
            bR[apIdxR[i]] = accR + bOR * 0.5f
            apIdxR[i] = (apIdxR[i] + 1) % bR.size
            accR = bOR - accR
        }

        val wet1 = wetLevel * (width * 0.5f + 0.5f)
        val wet2 = wetLevel * ((1f - width) * 0.5f)
        outL = accL * wet1 + accR * wet2 + inL * dryLevel
        outR = accR * wet1 + accL * wet2 + inR * dryLevel
    }

    fun applyPreset(presetId: String) {
        val config = presetMap[presetId] ?: presetMap[ReverbPreset.NONE.id]
        if (config != null) {
            set(config.enabled, config.room, config.damp, config.wet, config.dry, config.width)
        } else {
            set(false, 0f, 0f, 0f, 0f, 0f)
        }
        if (buffersReady) clearBuffers()
    }

    private fun set(
        en: Boolean,
        room: Float,
        damp: Float,
        wet: Float,
        dry: Float,
        w: Float
    ) {
        enabled = en
        roomSize = room
        damping = damp
        wetLevel = wet
        dryLevel = dry
        width = w
    }

    private data class PresetConfig(
        val enabled: Boolean,
        val room: Float,
        val damp: Float,
        val wet: Float,
        val dry: Float,
        val width: Float
    )
}