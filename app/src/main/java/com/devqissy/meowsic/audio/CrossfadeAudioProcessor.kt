package com.devqissy.meowsic.audio

import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class FadeOutAudioProcessor @Inject constructor() : AudioProcessor {

    private val tag = "FadeOutProcessor"

    private var currentFormat: AudioProcessor.AudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var outputBuffer = ByteBuffer.allocate(0)
    private var inputEnded = false

    @Volatile var enabled = false
    @Volatile var fadeDurationMs = 2000f
    @Volatile var isFading = false
    @Volatile var fadeProgress = 0f

    private var sampleRate = 44100
    private var channelCount = 2
    private var frameSize = 4

    private var fadeFrames = 0L
    private var totalFadeFrames = 0L

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        currentFormat = inputAudioFormat
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        frameSize = if (channelCount >= 2) 4 else 2
        return inputAudioFormat
    }

    override fun isActive(): Boolean = enabled

    override fun queueInput(inputBuffer: ByteBuffer) {
        val inputBytes = inputBuffer.remaining()

        if (!isFading) {
            if (outputBuffer.capacity() < inputBytes) {
                outputBuffer = ByteBuffer.allocate(inputBytes).order(ByteOrder.LITTLE_ENDIAN)
            } else {
                outputBuffer.clear()
            }
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        if (outputBuffer.capacity() < inputBytes) {
            outputBuffer = ByteBuffer.allocate(inputBytes).order(ByteOrder.LITTLE_ENDIAN)
        } else {
            outputBuffer.clear()
        }

        val input = inputBuffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        val isStereo = channelCount >= 2
        val frames = inputBytes / frameSize

        while (input.remaining() >= frameSize) {
            fadeFrames++
            fadeProgress = if (totalFadeFrames > 0) {
                (fadeFrames.toFloat() / totalFadeFrames).coerceIn(0f, 1f)
            } else 1f

            val fadeFactor = (1f - fadeProgress).coerceIn(0f, 1f)

            val leftSample = input.short.toFloat() / 32768f
            val rightSample = if (isStereo) input.short.toFloat() / 32768f else leftSample

            val outLeft = leftSample * fadeFactor
            val outRight = rightSample * fadeFactor

            outputBuffer.putShort((outLeft.coerceIn(-1f, 1f) * 32767f).toInt().toShort())
            if (isStereo) {
                outputBuffer.putShort((outRight.coerceIn(-1f, 1f) * 32767f).toInt().toShort())
            }
        }

        if (fadeProgress >= 1f) {
            isFading = false
        }

        inputBuffer.position(inputBuffer.limit())
        outputBuffer.flip()

        if (frames == 0 && inputBytes > 0) {
            Log.w(tag, "queueInput: trailing bytes dropped ($inputBytes bytes, frameSize=$frameSize)")
        }
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
        isFading = false
        fadeProgress = 0f
        fadeFrames = 0L
    }

    override fun reset() {
        currentFormat = AudioProcessor.AudioFormat.NOT_SET
        outputBuffer = ByteBuffer.allocate(0)
        inputEnded = false
        isFading = false
        fadeProgress = 0f
        fadeFrames = 0L
        totalFadeFrames = 0L
    }
}