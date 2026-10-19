package com.devqissy.meowsic.service

import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.devqissy.meowsic.audio.AudioEffectsManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class MusicService : MediaSessionService() {

    @Inject lateinit var mediaSession: MediaSession
    @Inject lateinit var audioEffectsManager: AudioEffectsManager
    @Inject lateinit var sleepTimerManager: SleepTimerManager

    private val tag = "MusicService"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val player: Player
        get() = mediaSession.player

    override fun onCreate() {
        super.onCreate()
        try {
            player.addListener(playerListener)
            setupSleepTimerListener()
            setupFlushListener()
        } catch (e: Exception) {
            Log.e(tag, "onCreate setup failed", e)
        }
    }

    private fun setupSleepTimerListener() {
        serviceScope.launch {
            sleepTimerManager.timerFinished.collect {
                if (player.isPlaying) {
                    player.pause()
                }
            }
        }
    }

    private fun setupFlushListener() {
        serviceScope.launch {
            audioEffectsManager.needsFlush
                .debounce(250)
                .collectLatest {
                    val wasPlaying = player.isPlaying
                    val position = player.currentPosition
                    if (wasPlaying) player.pause()
                    player.seekTo(position)
                    if (wasPlaying) player.play()
                }
        }
    }

    private val playerListener = object : Player.Listener {

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            audioEffectsManager.attachToSession(audioSessionId)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            Log.d(tag, "playbackState=$playbackState")
            if (playbackState == Player.STATE_READY) {
                audioEffectsManager.ensureAttached()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Log.d(tag, "onIsPlayingChanged: $isPlaying")
            if (isPlaying) audioEffectsManager.ensureAttached()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    override fun onDestroy() {
        val p = player
        p.removeListener(playerListener)
        serviceScope.cancel()

        try {
            p.release()
        } catch (e: Exception) {
            Log.e(tag, "Failed to release player", e)
        }
        try {
            mediaSession.release()
        } catch (e: Exception) {
            Log.e(tag, "Failed to release mediaSession", e)
        }
        try {
            audioEffectsManager.release()
        } catch (e: Exception) {
            Log.e(tag, "Failed to release audioEffectsManager", e)
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = player
        if (!p.playWhenReady || p.mediaItemCount == 0 ||
            p.playbackState == Player.STATE_ENDED
        ) {
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                ACTION_PLAY_PAUSE -> {
                    if (player.isPlaying) player.pause() else player.play()
                }
                ACTION_NEXT -> {
                    if (player.hasNextMediaItem()) player.seekToNextMediaItem()
                    else player.seekTo(0, 0L)
                }
                ACTION_PREVIOUS -> {
                    if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
                    else player.seekTo(player.mediaItemCount - 1, 0L)
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.devqissy.meowsic.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.devqissy.meowsic.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.devqissy.meowsic.ACTION_PREVIOUS"
    }
}