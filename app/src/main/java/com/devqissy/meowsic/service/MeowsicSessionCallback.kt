package com.devqissy.meowsic.service

import android.content.Intent
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.core.content.IntentCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession


@OptIn(UnstableApi::class)
class MeowsicSessionCallback : MediaSession.Callback {

    override fun onMediaButtonEvent(
        session: MediaSession,
        controllerInfo: MediaSession.ControllerInfo,
        intent: Intent
    ): Boolean {
        val keyEvent = IntentCompat.getParcelableExtra(
            intent,
            Intent.EXTRA_KEY_EVENT,
            KeyEvent::class.java
        )

        if (keyEvent?.action == KeyEvent.ACTION_DOWN) {
            when (keyEvent.keyCode) {
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                    val player = session.player
                    if (player.hasNextMediaItem()) player.seekToNextMediaItem()
                    else player.seekTo(0, 0L)
                    return true
                }

                KeyEvent.KEYCODE_MEDIA_REWIND -> {
                    val player = session.player
                    if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
                    else player.seekTo(player.mediaItemCount - 1, 0L)
                    return true
                }
            }
        }

        return super.onMediaButtonEvent(session, controllerInfo, intent)
    }
}