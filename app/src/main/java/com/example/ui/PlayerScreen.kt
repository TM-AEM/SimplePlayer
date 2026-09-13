package com.example.ui

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Rational
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.data.SettingsRepository
import com.example.player.SimplePlayerManager
import com.example.player.SubtitleTrack
import com.example.utils.TimeUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class DragType {
    VOLUME, BRIGHTNESS
}

private data class TapInfo(
    val time: Long,
    val pos: Offset,
    val isRightSide: Boolean
)

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Screen displaying video playback using Media3 ExoPlayer with YouTube-style gestures,
 * Fullscreen support, Playback Speed selector, Subtitle track chooser, Picture-in-Picture (PiP),
 * and Background Audio Playback with system MediaSession.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    videoTitle: String,
    videoUri: Uri,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val coroutineScope = rememberCoroutineScope()

    val playerManager = remember { SimplePlayerManager.getInstance(context) }
    val playerState by playerManager.playerState.collectAsStateWithLifecycle()

    // Settings observation for Seek Duration and Long Press Speed
    val settingsRepository = remember(context) { SettingsRepository(context) }
    val seekDurationSeconds by settingsRepository.seekDurationSecondsFlow
        .collectAsStateWithLifecycle(initialValue = SettingsRepository.DEFAULT_SEEK_SECONDS)
    val longPressSpeedMultiplier by settingsRepository.longPressSpeedMultiplierFlow
        .collectAsStateWithLifecycle(initialValue = SettingsRepository.DEFAULT_SPEED_MULTIPLIER)

    // Long Press to Speed Up states
    var isLongPressSpeedActive by remember { mutableStateOf(false) }
    var speedBeforeLongPress by remember { mutableFloatStateOf(1.0f) }

    // Picture-in-Picture Mode state
    var isInPipMode by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                activity?.isInPictureInPictureMode ?: false
            } else false
        )
    }

    DisposableEffect(activity) {
        if (activity is ComponentActivity) {
            val pipListener = Consumer<PictureInPictureModeChangedInfo> { info ->
                isInPipMode = info.isInPictureInPictureMode
            }
            activity.addOnPictureInPictureModeChangedListener(pipListener)
            onDispose {
                activity.removeOnPictureInPictureModeChangedListener(pipListener)
            }
        } else {
            onDispose { }
        }
    }

    // Fullscreen and Orientation detection
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var isManualFullscreen by remember { mutableStateOf(false) }
    val isFullscreen = isLandscape || isManualFullscreen

    // System bars immersive mode handling
    LaunchedEffect(isFullscreen, isInPipMode) {
        if (isInPipMode) return@LaunchedEffect
        val window = activity?.window ?: return@LaunchedEffect
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (isFullscreen) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    fun toggleFullscreen() {
        val targetLandscape = !isFullscreen
        if (targetLandscape) {
            isManualFullscreen = true
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            isManualFullscreen = false
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    fun enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val aspectRatio = Rational(16, 9)
                val builder = PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    builder.setAutoEnterEnabled(true)
                }
                activity?.enterPictureInPictureMode(builder.build())
            } catch (e: Exception) {
                Toast.makeText(context, "وضع PiP غير مدعوم على هذا الجهاز", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "وضع PiP يتطلب أندرويد 8.0+", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleBack() {
        if (isManualFullscreen || isLandscape) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            isManualFullscreen = false
        } else {
            onBack()
        }
    }

    // Handle system back press
    BackHandler {
        handleBack()
    }

    // Auto-hide controls logic
    var areControlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(areControlsVisible, lastInteractionTime, playerState.isPlaying) {
        if (areControlsVisible && playerState.isPlaying) {
            delay(3000L)
            areControlsVisible = false
        }
    }

    fun resetControlsTimer() {
        lastInteractionTime = System.currentTimeMillis()
        if (!areControlsVisible) {
            areControlsVisible = true
        }
    }

    // Slider scrubbing state
    var isUserDragging by remember { mutableStateOf(false) }
    var draggingPositionMs by remember { mutableFloatStateOf(0f) }

    // BottomSheet states for Speed & Subtitles
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showSubtitlesSheet by remember { mutableStateOf(false) }

    // Volume & Brightness HUD state
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxVolume = remember { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15 }

    var isVolumeHudVisible by remember { mutableStateOf(false) }
    var volumePercent by remember {
        val cur = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: (maxVolume / 2)
        mutableIntStateOf(((cur.toFloat() / maxVolume) * 100).roundToInt())
    }
    var currentVolumeFloat by remember {
        val cur = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: (maxVolume / 2)
        mutableFloatStateOf(cur.toFloat())
    }

    var isBrightnessHudVisible by remember { mutableStateOf(false) }
    var brightnessPercent by remember { mutableIntStateOf(50) }
    var currentBrightnessFloat by remember { mutableFloatStateOf(0.5f) }

    var volumeTimerJob by remember { mutableStateOf<Job?>(null) }
    var brightnessTimerJob by remember { mutableStateOf<Job?>(null) }

    fun showVolumeHud(percent: Int) {
        volumePercent = percent
        isVolumeHudVisible = true
        volumeTimerJob?.cancel()
        volumeTimerJob = coroutineScope.launch {
            delay(1500L)
            isVolumeHudVisible = false
        }
    }

    fun showBrightnessHud(percent: Int) {
        brightnessPercent = percent
        isBrightnessHudVisible = true
        brightnessTimerJob?.cancel()
        brightnessTimerJob = coroutineScope.launch {
            delay(1500L)
            isBrightnessHudVisible = false
        }
    }

    // Double Tap visual feedback state
    var isForwardFeedbackVisible by remember { mutableStateOf(false) }
    var isRewindFeedbackVisible by remember { mutableStateOf(false) }
    var feedbackJob by remember { mutableStateOf<Job?>(null) }

    fun triggerForwardFeedback() {
        isForwardFeedbackVisible = true
        isRewindFeedbackVisible = false
        feedbackJob?.cancel()
        feedbackJob = coroutineScope.launch {
            delay(750L)
            isForwardFeedbackVisible = false
        }
    }

    fun triggerRewindFeedback() {
        isRewindFeedbackVisible = true
        isForwardFeedbackVisible = false
        feedbackJob?.cancel()
        feedbackJob = coroutineScope.launch {
            delay(750L)
            isRewindFeedbackVisible = false
        }
    }

    // Start video playback and release on exit
    DisposableEffect(videoUri) {
        playerManager.playUri(videoUri, title = videoTitle, playWhenReady = true)
        onDispose {
            if (isLongPressSpeedActive) {
                playerManager.setPlaybackSpeed(speedBeforeLongPress)
                isLongPressSpeedActive = false
            }
            playerManager.releasePlayer()
            // Restore window system bars, orientation, and brightness
            activity?.let { act ->
                val window = act.window
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                val lp = window.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = lp
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    // Pointer state tracking for Gestures (Tap, Double Tap, Vertical Drag)
    var pendingTapJob by remember { mutableStateOf<Job?>(null) }
    var lastTapInfo by remember { mutableStateOf<TapInfo?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("player_screen")
    ) {
        // ExoPlayer Media3 AndroidView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = playerManager.initializePlayer()
                    useController = false // Custom Compose overlay controls
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("player_view")
        )

        // Only display gestures and overlay UI when NOT in Picture-in-Picture mode
        if (!isInPipMode) {
            // YouTube-style Gesture Overlay Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("gesture_overlay_area")
                    .pointerInput(areControlsVisible, longPressSpeedMultiplier, seekDurationSeconds) {
                        val touchSlop = viewConfiguration.touchSlop
                        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val downX = down.position.x
                            val downY = down.position.y
                            val isRightSide = downX >= (size.width / 2f)

                            // Ignore touches in top header and bottom bar when controls are visible
                            if (areControlsVisible && (downY < 100.dp.toPx() || downY > size.height - 120.dp.toPx())) {
                                return@awaitEachGesture
                            }

                            var isDrag = false
                            var isLongPress = false
                            var lastY = downY
                            var dragType: DragType? = null

                            // Launch long press timer coroutine
                            val longPressJob = coroutineScope.launch {
                                delay(longPressTimeout)
                                if (!isDrag) {
                                    isLongPress = true
                                    pendingTapJob?.cancel()
                                    pendingTapJob = null
                                    lastTapInfo = null

                                    // Save previous speed and activate speed multiplier
                                    speedBeforeLongPress = playerManager.playerState.value.playbackSpeed
                                    playerManager.setPlaybackSpeed(longPressSpeedMultiplier)
                                    isLongPressSpeedActive = true
                                }
                            }

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break

                                if (!change.pressed) {
                                    // Pointer up
                                    longPressJob.cancel()

                                    if (isLongPress) {
                                        // Finger released: restore original speed immediately
                                        isLongPressSpeedActive = false
                                        playerManager.setPlaybackSpeed(speedBeforeLongPress)
                                        isLongPress = false
                                    } else if (!isDrag) {
                                        val currentTime = System.currentTimeMillis()
                                        val tapPos = change.position
                                        val prevTap = lastTapInfo

                                        if (prevTap != null &&
                                            (currentTime - prevTap.time) < 320L &&
                                            abs(tapPos.x - prevTap.pos.x) < 120.dp.toPx() &&
                                            abs(tapPos.y - prevTap.pos.y) < 120.dp.toPx() &&
                                            prevTap.isRightSide == isRightSide
                                        ) {
                                            // Double Tap detected!
                                            pendingTapJob?.cancel()
                                            pendingTapJob = null
                                            lastTapInfo = null

                                            if (isRightSide) {
                                                playerManager.seekForward(seekDurationSeconds.toLong())
                                                triggerForwardFeedback()
                                            } else {
                                                playerManager.seekBackward(seekDurationSeconds.toLong())
                                                triggerRewindFeedback()
                                            }
                                            resetControlsTimer()
                                        } else {
                                            // Potential single tap
                                            lastTapInfo = TapInfo(currentTime, tapPos, isRightSide)
                                            pendingTapJob?.cancel()
                                            pendingTapJob = coroutineScope.launch {
                                                delay(300L)
                                                areControlsVisible = !areControlsVisible
                                                if (areControlsVisible) {
                                                    resetControlsTimer()
                                                }
                                                lastTapInfo = null
                                            }
                                        }
                                    }
                                    break
                                }

                                val totalDx = abs(change.position.x - downX)
                                val totalDy = abs(change.position.y - downY)

                                if (!isDrag && !isLongPress) {
                                    if (totalDx > touchSlop || totalDy > touchSlop) {
                                        longPressJob.cancel()
                                    }

                                    if (totalDy > touchSlop && totalDy > totalDx * 1.2f) {
                                        isDrag = true
                                        pendingTapJob?.cancel()
                                        pendingTapJob = null
                                        lastTapInfo = null
                                        dragType = if (isRightSide) DragType.VOLUME else DragType.BRIGHTNESS

                                        if (dragType == DragType.VOLUME) {
                                            currentVolumeFloat = audioManager
                                                ?.getStreamVolume(AudioManager.STREAM_MUSIC)
                                                ?.toFloat() ?: (maxVolume / 2f)
                                        } else {
                                            val window = activity?.window
                                            val winBright = window?.attributes?.screenBrightness ?: -1f
                                            currentBrightnessFloat = if (winBright >= 0f) {
                                                winBright
                                            } else {
                                                try {
                                                    Settings.System.getInt(
                                                        context.contentResolver,
                                                        Settings.System.SCREEN_BRIGHTNESS
                                                    ) / 255f
                                                } catch (e: Exception) {
                                                    0.5f
                                                }
                                            }
                                        }
                                        lastY = change.position.y
                                    }
                                }

                                if (isDrag) {
                                    change.consume()
                                    val deltaY = change.position.y - lastY
                                    lastY = change.position.y

                                    if (dragType == DragType.VOLUME) {
                                        val deltaVolume = (-deltaY / (size.height * 0.45f)) * maxVolume
                                        currentVolumeFloat = (currentVolumeFloat + deltaVolume)
                                            .coerceIn(0f, maxVolume.toFloat())
                                        val newIntVol = currentVolumeFloat.roundToInt()
                                        audioManager?.setStreamVolume(
                                            AudioManager.STREAM_MUSIC,
                                            newIntVol,
                                            0
                                        )
                                        val pct = ((currentVolumeFloat / maxVolume) * 100).roundToInt()
                                        showVolumeHud(pct)
                                    } else if (dragType == DragType.BRIGHTNESS) {
                                        val deltaBrightness = -deltaY / (size.height * 0.45f)
                                        currentBrightnessFloat = (currentBrightnessFloat + deltaBrightness)
                                            .coerceIn(0.01f, 1.0f)
                                        activity?.window?.let { win ->
                                            val lp = win.attributes
                                            lp.screenBrightness = currentBrightnessFloat
                                            win.attributes = lp
                                        }
                                        val pct = (currentBrightnessFloat * 100).roundToInt()
                                        showBrightnessHud(pct)
                                    }
                                }
                            }
                        }
                    }
            )

            // YouTube-style Long Press Speed Indicator HUD (Centered at Top)
            AnimatedVisibility(
                visible = isLongPressSpeedActive,
                enter = fadeIn() + slideInVertically { -it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(top = 18.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.85f),
                    contentColor = Color.White,
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .testTag("long_press_speed_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(
                                id = R.string.speed_hud_text,
                                formatSpeedText(longPressSpeedMultiplier)
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Double-tap visual ripples
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                DoubleTapFeedback(
                    isForward = false,
                    visible = isRewindFeedbackVisible,
                    seekSeconds = seekDurationSeconds,
                    modifier = Modifier.testTag("rewind_feedback_indicator")
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                DoubleTapFeedback(
                    isForward = true,
                    visible = isForwardFeedbackVisible,
                    seekSeconds = seekDurationSeconds,
                    modifier = Modifier.testTag("forward_feedback_indicator")
                )
            }

            // Volume Indicator Overlay (Right Side - Vertical Capsule HUD)
            AnimatedVisibility(
                visible = isVolumeHudVisible,
                enter = fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.88f, animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(350)) + scaleOut(targetScale = 0.88f, animationSpec = tween(350)),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(end = 28.dp)
            ) {
                val volumeIcon = when {
                    volumePercent <= 0 -> Icons.Default.VolumeMute
                    volumePercent <= 50 -> Icons.Default.VolumeDown
                    else -> Icons.Default.VolumeUp
                }
                VerticalHudCapsule(
                    icon = volumeIcon,
                    label = "الصوت",
                    percent = volumePercent,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("volume_hud_overlay")
                )
            }

            // Brightness Indicator Overlay (Left Side - Vertical Capsule HUD)
            AnimatedVisibility(
                visible = isBrightnessHudVisible,
                enter = fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.88f, animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(350)) + scaleOut(targetScale = 0.88f, animationSpec = tween(350)),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(start = 28.dp)
            ) {
                val brightnessIcon = when {
                    brightnessPercent < 30 -> Icons.Default.BrightnessLow
                    brightnessPercent <= 70 -> Icons.Default.BrightnessMedium
                    else -> Icons.Default.BrightnessHigh
                }
                VerticalHudCapsule(
                    icon = brightnessIcon,
                    label = "السطوع",
                    percent = brightnessPercent,
                    accentColor = Color(0xFFFFD54F),
                    modifier = Modifier.testTag("brightness_hud_overlay")
                )
            }

            // Overlay Controls with smooth fade animation
            AnimatedVisibility(
                visible = areControlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                ) {
                    // Top Header (Back + Title + Subtitles + Speed + PiP + Fullscreen)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                                )
                            )
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                resetControlsTimer()
                                handleBack()
                            },
                            modifier = Modifier.testTag("player_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = videoTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("player_video_title")
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        // Subtitle Button
                        IconButton(
                            onClick = {
                                resetControlsTimer()
                                if (playerState.subtitleTracks.isEmpty()) {
                                    Toast.makeText(context, "لا توجد ترجمة متاحة", Toast.LENGTH_SHORT).show()
                                } else {
                                    showSubtitlesSheet = true
                                }
                            },
                            modifier = Modifier.testTag("player_subtitles_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Subtitles,
                                contentDescription = "Subtitles",
                                tint = if (playerState.selectedSubtitle != null) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }

                        // Playback Speed Button
                        Surface(
                            onClick = {
                                resetControlsTimer()
                                showSpeedSheet = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.18f),
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .testTag("player_speed_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Playback Speed",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                val speedText = formatSpeedText(playerState.playbackSpeed)
                                Text(
                                    text = speedText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // PiP Button
                        IconButton(
                            onClick = {
                                resetControlsTimer()
                                enterPip()
                            },
                            modifier = Modifier.testTag("player_pip_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureInPictureAlt,
                                contentDescription = "Picture in Picture",
                                tint = Color.White
                            )
                        }

                        // Manual Fullscreen toggle button
                        IconButton(
                            onClick = {
                                resetControlsTimer()
                                toggleFullscreen()
                            },
                            modifier = Modifier.testTag("player_fullscreen_button")
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = if (isFullscreen) "Exit Fullscreen" else "Enter Fullscreen",
                                tint = Color.White
                            )
                        }
                    }

                    // Center Controls (Rewind, Play/Pause, Forward)
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rewind button
                        IconButton(
                            onClick = {
                                resetControlsTimer()
                                playerManager.seekBackward(seekDurationSeconds.toLong())
                                triggerRewindFeedback()
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("player_replay_10_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Replay $seekDurationSeconds seconds",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Play/Pause / Buffering button
                        Surface(
                            onClick = {
                                resetControlsTimer()
                                playerManager.togglePlayPause()
                            },
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.22f),
                            contentColor = Color.White,
                            modifier = Modifier
                                .size(72.dp)
                                .testTag("player_play_pause_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (playerState.isBuffering) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(36.dp),
                                        color = Color.White,
                                        strokeWidth = 3.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                        modifier = Modifier.size(44.dp),
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        // Forward button
                        IconButton(
                            onClick = {
                                resetControlsTimer()
                                playerManager.seekForward(seekDurationSeconds.toLong())
                                triggerForwardFeedback()
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("player_forward_10_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "Forward $seekDurationSeconds seconds",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // Bottom Control Bar (Time indicator + Slider)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                )
                            )
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        val currentPos = if (isUserDragging) draggingPositionMs.toLong() else playerState.currentPositionMs
                        val totalDuration = playerState.durationMs

                        // Time display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${TimeUtils.formatDuration(currentPos)} / ${TimeUtils.formatDuration(totalDuration)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.testTag("player_time_text")
                            )
                        }

                        // Interactive Scrubbing Slider
                        val sliderPosition = if (isUserDragging) {
                            draggingPositionMs
                        } else {
                            playerState.currentPositionMs.toFloat()
                        }
                        val maxSliderValue = totalDuration.coerceAtLeast(1L).toFloat()

                        Slider(
                            value = sliderPosition.coerceIn(0f, maxSliderValue),
                            onValueChange = { newPos ->
                                isUserDragging = true
                                draggingPositionMs = newPos
                                resetControlsTimer()
                            },
                            onValueChangeFinished = {
                                playerManager.seekTo(draggingPositionMs.toLong())
                                isUserDragging = false
                                resetControlsTimer()
                            },
                            valueRange = 0f..maxSliderValue,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("player_slider")
                        )
                    }
                }
            }
        }

        // Playback Speed BottomSheet
        if (showSpeedSheet) {
            val speedOptions = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
            ModalBottomSheet(
                onDismissRequest = { showSpeedSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("speed_bottom_sheet")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "سرعة التشغيل",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    HorizontalDivider()
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(speedOptions) { speed ->
                            val isSelected = (playerState.playbackSpeed == speed)
                            val label = if (speed == 1.0f) "1x (عادي)" else formatSpeedText(speed)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        playerManager.setPlaybackSpeed(speed)
                                        showSpeedSheet = false
                                        resetControlsTimer()
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Subtitles BottomSheet
        if (showSubtitlesSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSubtitlesSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("subtitles_bottom_sheet")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "الترجمة",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    HorizontalDivider()

                    // Off option
                    val isOffSelected = (playerState.selectedSubtitle == null)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                playerManager.selectSubtitleTrack(null)
                                showSubtitlesSheet = false
                                resetControlsTimer()
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "إيقاف الترجمة (Off)",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isOffSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isOffSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (isOffSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Available subtitle tracks
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(playerState.subtitleTracks) { track ->
                            val isSelected = (playerState.selectedSubtitle?.id == track.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        playerManager.selectSubtitleTrack(track)
                                        showSubtitlesSheet = false
                                        resetControlsTimer()
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = track.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

private fun formatSpeedText(speed: Float): String {
    return if (speed == speed.toInt().toFloat()) {
        "${speed.toInt()}x"
    } else {
        "${speed}x"
    }
}

/**
 * Modern vertical capsule HUD (Samsung One UI / YouTube style) for Volume & Brightness.
 * Features a semi-transparent dark capsule with a dynamic icon, clear percentage text,
 * vertical smooth-filling progress bar (bottom to top), and subtle bounce pulse on boundary values (0% and 100%).
 */
@Composable
private fun VerticalHudCapsule(
    icon: ImageVector,
    label: String,
    percent: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val clampedPercent = percent.coerceIn(0, 100)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedPercent / 100f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "vertical_hud_progress"
    )

    // Boundary bounce animation at 0% or 100%
    val scaleAnim = remember { Animatable(1f) }
    LaunchedEffect(clampedPercent) {
        if (clampedPercent == 0 || clampedPercent == 100) {
            scaleAnim.animateTo(1.08f, animationSpec = tween(75, easing = FastOutSlowInEasing))
            scaleAnim.animateTo(1.0f, animationSpec = tween(120, easing = FastOutSlowInEasing))
        }
    }

    val isBoundary = clampedPercent == 0 || clampedPercent == 100
    val borderColor = if (isBoundary) {
        accentColor.copy(alpha = 0.75f)
    } else {
        Color.White.copy(alpha = 0.18f)
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color(0xE8161618),
        contentColor = Color.White,
        border = BorderStroke(1.2.dp, borderColor),
        shadowElevation = 8.dp,
        modifier = modifier
            .width(54.dp)
            .height(185.dp)
            .graphicsLayer {
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dynamic Header Icon
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Percentage value text in clear, bold font
            Text(
                text = "${clampedPercent}%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Vertical Progress Bar (fills smoothly from bottom to top)
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(animatedProgress)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    accentColor,
                                    accentColor.copy(alpha = 0.85f)
                                )
                            )
                        )
                )
            }
        }
    }
}

/**
 * YouTube-style animated feedback circle for double tap seek (+Ns / -Ns).
 */
@Composable
private fun DoubleTapFeedback(
    isForward: Boolean,
    visible: Boolean,
    seekSeconds: Int = 10,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.75f),
        exit = fadeOut() + scaleOut(targetScale = 1.25f),
        modifier = modifier
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.25f),
            modifier = Modifier.size(92.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isForward) Icons.Default.Forward10 else Icons.Default.Replay10,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isForward) "+${seekSeconds}s" else "-${seekSeconds}s",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.testTag(if (isForward) "forward_feedback_text" else "rewind_feedback_text")
                )
            }
        }
    }
}
