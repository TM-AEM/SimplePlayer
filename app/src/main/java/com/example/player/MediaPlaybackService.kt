package com.example.player

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Foreground MediaSessionService enabling background audio playback
 * and system media notifications with playback controls.
 */
@OptIn(UnstableApi::class)
class MediaPlaybackService : MediaSessionService() {

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return SimplePlayerManager.getMediaSession()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = SimplePlayerManager.getActivePlayer()
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
