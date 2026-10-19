package com.devqissy.meowsic.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object VolumeKeyBridge {

    sealed class Command {
        data object Next : Command()
        data object Previous : Command()
    }

    private val _commands = MutableSharedFlow<Command>(extraBufferCapacity = 8)
    val commands: SharedFlow<Command> = _commands.asSharedFlow()

    @Volatile
    var isMusicPlaying: Boolean = false

    @Volatile
    var playerSnapshot: String = "player=none"

    fun sendCommand(command: Command) {
        _commands.tryEmit(command)
    }
}