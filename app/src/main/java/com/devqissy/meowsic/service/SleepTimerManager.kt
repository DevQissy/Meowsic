package com.devqissy.meowsic.service

import android.os.CountDownTimer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepTimerManager @Inject constructor() {

    private var countDownTimer: CountDownTimer? = null

    private val _remainingTime = MutableStateFlow(0L)
    val remainingTime: StateFlow<Long> = _remainingTime.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _selectedDuration = MutableStateFlow(0L)
    val selectedDuration: StateFlow<Long> = _selectedDuration.asStateFlow()

    private val _timerFinished = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val timerFinished: SharedFlow<Unit> = _timerFinished.asSharedFlow()

    fun startTimer(minutes: Long) {
        stopTimer()

        val millisInFuture = minutes * 60 * 1000L
        _selectedDuration.value = millisInFuture
        _remainingTime.value = millisInFuture
        _isRunning.value = true

        countDownTimer = object : CountDownTimer(millisInFuture, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _remainingTime.value = millisUntilFinished
            }

            override fun onFinish() {
                _remainingTime.value = 0
                _isRunning.value = false
                _selectedDuration.value = 0
                _timerFinished.tryEmit(Unit)
            }
        }.start()
    }

    fun stopTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
        _isRunning.value = false
        _remainingTime.value = 0
        _selectedDuration.value = 0
    }
}