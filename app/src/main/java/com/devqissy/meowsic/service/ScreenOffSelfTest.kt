package com.devqissy.meowsic.service

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


object ScreenOffSelfTest {

    sealed interface Status {
        data object Idle : Status
        data object Waiting : Status
        data object Supported : Status
        data object NotSupported : Status
    }

    private val _status = MutableStateFlow<Status>(Status.Idle)
    val status: StateFlow<Status> = _status.asStateFlow()

    @Volatile
    private var windowEndAt = 0L

    private val mainHandler = Handler(Looper.getMainLooper())

    private val timeoutRunnable = Runnable {
        if (_status.value == Status.Waiting) {
            _status.value = Status.NotSupported
        }
    }

    fun start(durationMs: Long = 90_000L) {
        windowEndAt = SystemClock.elapsedRealtime() + durationMs
        _status.value = Status.Waiting
        mainHandler.removeCallbacks(timeoutRunnable)
        mainHandler.postDelayed(timeoutRunnable, durationMs)
    }

    fun notifyKeyEventWhileScreenOff() {
        if (_status.value != Status.Waiting) return
        if (SystemClock.elapsedRealtime() > windowEndAt) {
            _status.value = Status.NotSupported
            return
        }
        mainHandler.removeCallbacks(timeoutRunnable)
        _status.value = Status.Supported
    }

    fun reset() {
        mainHandler.removeCallbacks(timeoutRunnable)
        _status.value = Status.Idle
    }
}