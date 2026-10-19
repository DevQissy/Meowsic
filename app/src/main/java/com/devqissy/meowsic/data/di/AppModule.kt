package com.devqissy.meowsic.data.di

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.room.Room
import com.devqissy.meowsic.MainActivity
import com.devqissy.meowsic.audio.FadeOutAudioProcessor
import com.devqissy.meowsic.audio.ReverbAudioProcessor
import com.devqissy.meowsic.data.db.ALL_MIGRATIONS
import com.devqissy.meowsic.data.db.AppDatabase
import com.devqissy.meowsic.data.db.PlaylistDao
import com.devqissy.meowsic.service.MeowsicSessionCallback
import com.devqissy.meowsic.service.MusicSessionCallback
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "meowsic_db")
            .addMigrations(*ALL_MIGRATIONS)
            .build()

    @Provides
    @Singleton
    fun providePlaylistDao(db: AppDatabase): PlaylistDao = db.playlistDao()

    @Provides
    @Singleton
    fun provideAudioAttributes(): AudioAttributes =
        AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

    @OptIn(UnstableApi::class)
    @Provides
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        audioAttributes: AudioAttributes,
        reverbProcessor: ReverbAudioProcessor,
        fadeOutProcessor: FadeOutAudioProcessor
    ): ExoPlayer {

        val loadControl = DefaultLoadControl.Builder().build()

        val audioSink = DefaultAudioSink.Builder(context)
            .setAudioProcessorChain(
                DefaultAudioSink.DefaultAudioProcessorChain(
                    reverbProcessor,
                    fadeOutProcessor
                )
            )
            .build()

        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink = audioSink
        }

        return ExoPlayer.Builder(context, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setLoadControl(loadControl)
            .build()
    }

    @Provides
    fun provideMediaSession(
        @ApplicationContext context: Context,
        player: ExoPlayer,
        callback: MusicSessionCallback
    ): MediaSession {
        return MediaSession.Builder(context, player)
            .setCallback(MeowsicSessionCallback())
            .setCallback(callback)
            .setSessionActivity(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
    }
}