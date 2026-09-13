package com.example

import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.data.VideoItem
import com.example.player.SimplePlayerManager
import com.example.ui.PlayerScreen
import com.example.ui.SettingsScreen
import com.example.ui.SimplePlayerHomeScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Request notification permission on Android 13+ for MediaSession notification
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(
          this,
          android.Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
      }
    }

    setContent {
      MyApplicationTheme {
        SimplePlayerApp()
      }
    }
  }

  fun enterPipMode() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      try {
        val aspectRatio = Rational(16, 9)
        val builder = PictureInPictureParams.Builder()
          .setAspectRatio(aspectRatio)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          builder.setAutoEnterEnabled(true)
        }
        enterPictureInPictureMode(builder.build())
      } catch (e: Exception) {
        // Handle gracefully if device does not support PiP
      }
    }
  }

  override fun onUserLeaveHint() {
    super.onUserLeaveHint()
    val player = SimplePlayerManager.getActivePlayer()
    if (player != null && player.isPlaying) {
      enterPipMode()
    }
  }
}

@Composable
fun SimplePlayerApp(modifier: Modifier = Modifier) {
  var selectedVideo by remember { mutableStateOf<VideoItem?>(null) }
  var isSettingsOpen by remember { mutableStateOf(false) }

  val video = selectedVideo
  if (video != null) {
    PlayerScreen(
      videoTitle = video.title,
      videoUri = video.uri,
      onBack = { selectedVideo = null },
      modifier = modifier.fillMaxSize()
    )
  } else if (isSettingsOpen) {
    SettingsScreen(
      onBack = { isSettingsOpen = false },
      modifier = modifier.fillMaxSize()
    )
  } else {
    SimplePlayerHomeScreen(
      onPlayVideo = { videoItem ->
        selectedVideo = videoItem
      },
      onOpenSettings = {
        isSettingsOpen = true
      },
      modifier = modifier.fillMaxSize()
    )
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  SimplePlayerApp(modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  MyApplicationTheme { SimplePlayerApp() }
}

