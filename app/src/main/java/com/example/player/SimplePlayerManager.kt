package com.example.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Information about a single subtitle / text track.
 */
data class SubtitleTrack(
    val id: String,
    val label: String,
    val language: String?,
    val groupIndex: Int,
    val trackIndex: Int,
    val isSelected: Boolean
)

/**
 * Data class representing the playback state of the player.
 */
data class PlayerState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackState: Int = Player.STATE_IDLE,
    val isBuffering: Boolean = false,
    val isEnded: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val subtitleTracks: List<SubtitleTrack> = emptyList(),
    val selectedSubtitle: SubtitleTrack? = null
)

/**
 * Manager class responsible for initializing and controlling AndroidX Media3 ExoPlayer,
 * MediaSession for background audio playback, subtitles selection, and playback speed.
 */
class SimplePlayerManager(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressUpdateJob: Job? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateState()
            if (isPlaying) {
                startProgressUpdates()
            } else {
                stopProgressUpdates()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            updateState()
            if (playbackState == Player.STATE_ENDED) {
                stopProgressUpdates()
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            updateState()
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            updateState()
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            updateState()
        }
    }

    /**
     * Initializes and returns the ExoPlayer instance and establishes the MediaSession.
     * Configures AudioAttributes with handleAudioFocus=true (for incoming calls) and
     * handleAudioBecomingNoisy=true (pauses when headphones are unplugged).
     */
    fun initializePlayer(): ExoPlayer {
        val player = exoPlayer ?: run {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()

            ExoPlayer.Builder(context)
                .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
                .setHandleAudioBecomingNoisy(true)
                .build()
        }.also { newPlayer ->
            newPlayer.addListener(playerListener)
            exoPlayer = newPlayer
            try {
                mediaSession = MediaSession.Builder(context, newPlayer).build()
                val serviceIntent = Intent(context, MediaPlaybackService::class.java)
                context.startService(serviceIntent)
            } catch (e: Exception) {
                // Background service launch may be restricted in some execution contexts
            }
        }
        updateState()
        return player
    }

    /**
     * Retrieves the current ExoPlayer instance if initialized.
     */
    fun getPlayer(): ExoPlayer? = exoPlayer

    /**
     * Plays video from a Uri path with optional metadata title.
     */
    fun playUri(uri: Uri, title: String = "الفيديو", playWhenReady: Boolean = true) {
        val player = initializePlayer()
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(title)
            .setDisplayTitle(title)
            .build()
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(mediaMetadata)
            .build()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = playWhenReady
        updateState()
    }

    /**
     * Prepares and starts playback of a given media item.
     */
    fun playMedia(mediaItem: MediaItem, playWhenReady: Boolean = true) {
        val player = initializePlayer()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = playWhenReady
        updateState()
    }

    /**
     * Sets the playback speed (e.g. 0.5x, 1.0x, 1.5x, 2.0x).
     */
    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.let { player ->
            player.playbackParameters = PlaybackParameters(speed)
            updateState()
        }
    }

    /**
     * Selects a subtitle track or disables subtitles if null.
     */
    fun selectSubtitleTrack(track: SubtitleTrack?) {
        val player = exoPlayer ?: return
        val currentParams = player.trackSelectionParameters
        if (track == null) {
            player.trackSelectionParameters = currentParams.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .build()
        } else {
            val tracks = player.currentTracks
            val group = tracks.groups.getOrNull(track.groupIndex) ?: return
            val override = TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex)
            player.trackSelectionParameters = currentParams.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(override)
                .build()
        }
        updateState()
    }

    /**
     * Toggles play/pause state.
     */
    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                if (player.playbackState == Player.STATE_ENDED) {
                    player.seekTo(0L)
                }
                player.play()
            }
            updateState()
        }
    }

    fun play() {
        exoPlayer?.let { player ->
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0L)
            }
            player.play()
            updateState()
        }
    }

    fun pause() {
        exoPlayer?.let { player ->
            player.pause()
            updateState()
        }
    }

    /**
     * Seeks to a specific position in milliseconds.
     */
    fun seekTo(positionMs: Long) {
        exoPlayer?.let { player ->
            val safeDuration = player.duration.coerceAtLeast(0L)
            val target = if (safeDuration > 0L) {
                positionMs.coerceIn(0L, safeDuration)
            } else {
                positionMs.coerceAtLeast(0L)
            }
            player.seekTo(target)
            updateState()
        }
    }

    /**
     * Skips forward by [seconds] (default 10s).
     */
    fun seekForward(seconds: Long = 10L) {
        exoPlayer?.let { player ->
            val safeDuration = player.duration.coerceAtLeast(0L)
            val target = (player.currentPosition + seconds * 1000L).let {
                if (safeDuration > 0L) it.coerceAtMost(safeDuration) else it
            }
            player.seekTo(target)
            updateState()
        }
    }

    /**
     * Skips backward by [seconds] (default 10s).
     */
    fun seekBackward(seconds: Long = 10L) {
        exoPlayer?.let { player ->
            val target = (player.currentPosition - seconds * 1000L).coerceAtLeast(0L)
            player.seekTo(target)
            updateState()
        }
    }

    private fun startProgressUpdates() {
        stopProgressUpdates()
        progressUpdateJob = scope.launch {
            while (isActive) {
                updateState()
                delay(1000L)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
    }

    private fun updateState() {
        val player = exoPlayer ?: return
        val currentPosition = player.currentPosition.coerceAtLeast(0L)
        val duration = player.duration.coerceAtLeast(0L)
        val isPlaying = player.isPlaying
        val playbackState = player.playbackState
        val isBuffering = playbackState == Player.STATE_BUFFERING
        val isEnded = playbackState == Player.STATE_ENDED
        val currentSpeed = player.playbackParameters.speed

        // Extract subtitle tracks
        val subtitleList = mutableListOf<SubtitleTrack>()
        var selectedSubtitle: SubtitleTrack? = null
        val groups = player.currentTracks.groups
        for (gIndex in groups.indices) {
            val group = groups[gIndex]
            if (group.type == C.TRACK_TYPE_TEXT) {
                for (tIndex in 0 until group.length) {
                    val format = group.getTrackFormat(tIndex)
                    val isSelected = group.isTrackSelected(tIndex)
                    val lang = format.language
                    val label = format.label
                        ?: if (!lang.isNullOrBlank()) "ترجمة ($lang)" else "مسار ${subtitleList.size + 1}"
                    val trackItem = SubtitleTrack(
                        id = format.id ?: "${gIndex}_${tIndex}",
                        label = label,
                        language = lang,
                        groupIndex = gIndex,
                        trackIndex = tIndex,
                        isSelected = isSelected
                    )
                    subtitleList.add(trackItem)
                    if (isSelected) {
                        selectedSubtitle = trackItem
                    }
                }
            }
        }

        _playerState.update {
            it.copy(
                isPlaying = isPlaying,
                currentPositionMs = currentPosition,
                durationMs = duration,
                playbackState = playbackState,
                isBuffering = isBuffering,
                isEnded = isEnded,
                playbackSpeed = currentSpeed,
                subtitleTracks = subtitleList,
                selectedSubtitle = selectedSubtitle
            )
        }
    }

    /**
     * Releases player resources when done to avoid memory leaks.
     */
    fun releasePlayer() {
        stopProgressUpdates()
        exoPlayer?.removeListener(playerListener)
        try {
            mediaSession?.release()
        } catch (e: Exception) {
            // Ignore
        }
        mediaSession = null
        exoPlayer?.release()
        exoPlayer = null
        _playerState.value = PlayerState()
    }

    companion object {
        @Volatile
        private var sInstance: SimplePlayerManager? = null

        fun getInstance(context: Context): SimplePlayerManager {
            return sInstance ?: synchronized(this) {
                sInstance ?: SimplePlayerManager(context.applicationContext).also {
                    sInstance = it
                }
            }
        }

        fun getMediaSession(): MediaSession? = sInstance?.mediaSession
        fun getActivePlayer(): ExoPlayer? = sInstance?.exoPlayer
    }
}
