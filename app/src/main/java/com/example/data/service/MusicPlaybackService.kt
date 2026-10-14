package com.example.data.service

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.data.audio.PlayerHolder

class MusicPlaybackService : MediaSessionService() {

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return PlayerHolder.getMediaSession(this)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
