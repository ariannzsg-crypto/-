package com.example.data.audio

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.example.MainActivity

@OptIn(UnstableApi::class)
object PlayerHolder {
    @Volatile
    private var playerInstance: ExoPlayer? = null

    @Volatile
    private var sessionInstance: MediaSession? = null

    fun getPlayer(context: Context): ExoPlayer {
        return playerInstance ?: synchronized(this) {
            playerInstance ?: buildPlayer(context.applicationContext).also {
                playerInstance = it
            }
        }
    }

    fun getMediaSession(context: Context): MediaSession {
        return sessionInstance ?: synchronized(this) {
            val p = getPlayer(context)
            sessionInstance ?: buildSession(context.applicationContext, p).also {
                sessionInstance = it
            }
        }
    }

    private fun buildPlayer(context: Context): ExoPlayer {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        return ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
    }

    private fun buildSession(context: Context, player: ExoPlayer): MediaSession {
        val sessionActivityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return MediaSession.Builder(context, player)
            .setSessionActivity(pendingIntent)
            .build()
    }

    fun release() {
        try {
            sessionInstance?.release()
            sessionInstance = null
            playerInstance?.release()
            playerInstance = null
        } catch (e: Throwable) {
            // Ignore release exceptions
        }
    }
}
