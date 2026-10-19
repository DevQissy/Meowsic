package com.devqissy.meowsic.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

@SuppressLint("AccessibilityPolicy")
class VolumeKeyService : AccessibilityService() {

    private val tag = "VolumeKeyService"

    private val audioManager by lazy {
        getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private val powerManager by lazy {
        getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    private val handler = Handler(Looper.getMainLooper())

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var reconnectScheduled = false

    @Volatile
    private var destroyed = false

    private var upPressed = PressState()
    private var downPressed = PressState()

    private class PressState {
        var isDown = false
        var isLongPress = false
        var longPressRunnable: Runnable? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Some OEM ROMs don't apply the XML flag correctly, so set it programmatically
        serviceInfo = serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        connectToSession()
    }

    private fun connectToSession() {
        if (destroyed || mediaController != null || controllerFuture != null) return

        try {
            val token = SessionToken(
                applicationContext,
                ComponentName(applicationContext, MusicService::class.java)
            )

            controllerFuture = MediaController.Builder(applicationContext, token)
                .setListener(object : MediaController.Listener {
                    override fun onDisconnected(controller: MediaController) {
                        Log.w(tag, "MediaController disconnected")
                        mediaController = null
                        controllerFuture = null
                        scheduleReconnect()
                    }
                })
                .buildAsync()
                .also { future ->
                    future.addListener({
                        handler.post {
                            try {
                                mediaController = future.get()
                                Log.d(tag, "MediaController connected")
                            } catch (e: Exception) {
                                Log.w(tag, "MediaController connect failed: ${e.message}")
                                controllerFuture = null
                                scheduleReconnect()
                            }
                        }
                    }, MoreExecutors.directExecutor())
                }
        } catch (e: Exception) {
            Log.w(tag, "connectToSession failed: ${e.message}")
            controllerFuture = null
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        if (destroyed || reconnectScheduled) return
        reconnectScheduled = true
        handler.postDelayed({
            reconnectScheduled = false
            connectToSession()
        }, RECONNECT_DELAY_MS)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_VOLUME_UP &&
            event.keyCode != KeyEvent.KEYCODE_VOLUME_DOWN
        ) return false

        if (!powerManager.isInteractive) {
            ScreenOffSelfTest.notifyKeyEventWhileScreenOff()
        }

        if (mediaController == null && controllerFuture == null) connectToSession()

        if (!VolumeKeyPrefs.isEnabled(this)) return false

        val controller = mediaController ?: return false
        if (!controller.isPlaying) return false

        return when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> handle(event, isNext = true, state = upPressed)
            else -> handle(event, isNext = false, state = downPressed)
        }
    }

    private fun handle(event: KeyEvent, isNext: Boolean, state: PressState): Boolean {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) {
                    state.isDown = true
                    state.isLongPress = false

                    state.longPressRunnable = Runnable {
                        if (state.isDown && !state.isLongPress) {
                            state.isLongPress = true
                            if (isNext) skipToNext() else skipToPrevious()
                        }
                    }.also { handler.postDelayed(it, LONG_PRESS_MS) }
                }
                return true
            }

            KeyEvent.ACTION_UP -> {
                val wasDown = state.isDown
                state.isDown = false
                state.longPressRunnable?.let { handler.removeCallbacks(it) }
                state.longPressRunnable = null

                if (wasDown && !state.isLongPress) {
                    adjustMediaVolume(raise = isNext)
                }
                return wasDown
            }
        }
        return false
    }

    private fun adjustMediaVolume(raise: Boolean) {
        val direction = if (raise) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        val flags = if (powerManager.isInteractive) AudioManager.FLAG_SHOW_UI else 0
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, flags)
    }

    private fun skipToNext() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) controller.seekToNextMediaItem()
        else controller.seekTo(0, 0L)
    }

    private fun skipToPrevious() {
        val controller = mediaController ?: return
        if (controller.hasPreviousMediaItem()) controller.seekToPreviousMediaItem()
        else controller.seekTo(controller.mediaItemCount - 1, 0L)
    }

    override fun onInterrupt() {}
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onUnbind(intent: Intent?): Boolean {
        Log.w(tag, "Service unbound")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        destroyed = true
        handler.removeCallbacksAndMessages(null)

        try {
            controllerFuture?.cancel(true)
        } catch (_: Exception) {
        }
        controllerFuture = null

        try {
            mediaController?.release()
        } catch (_: Exception) {
        }
        mediaController = null

        super.onDestroy()
    }

    companion object {
        private const val LONG_PRESS_MS = 450L
        private const val RECONNECT_DELAY_MS = 15_000L
        private val SERVICE_CLASS = VolumeKeyService::class.java

        fun isAccessibilityEnabled(context: Context): Boolean {
            val expected = ComponentName(context, SERVICE_CLASS)

            val enabledSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )

            if (!enabledSetting.isNullOrEmpty()) {
                for (entry in enabledSetting.split(':')) {
                    val trimmed = entry.trim()
                    if (trimmed.isEmpty()) continue
                    if (ComponentName.unflattenFromString(trimmed) == expected) return true
                }
            }

            // fallback
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
                ?: return false
            return am.getEnabledAccessibilityServiceList(FEEDBACK_ALL_MASK)
                .any {
                    it.resolveInfo?.serviceInfo?.packageName == context.packageName &&
                            it.resolveInfo?.serviceInfo?.name == SERVICE_CLASS.name
                }
        }

        fun openAccessibilitySettings(context: Context) {
            context.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}