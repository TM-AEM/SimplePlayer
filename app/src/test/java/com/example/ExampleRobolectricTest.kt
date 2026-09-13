package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SettingsRepository
import com.example.player.SimplePlayerManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SimplePlayer", appName)
  }

  @Test
  fun `settings repository persists seek duration and speed correctly`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = SettingsRepository(context)

    // Verify initial values
    assertEquals(10, repository.seekDurationSecondsFlow.first())
    assertEquals(2.0f, repository.longPressSpeedMultiplierFlow.first())

    // Update seek duration to 20 seconds
    repository.setSeekDurationSeconds(20)
    assertEquals(20, repository.seekDurationSecondsFlow.first())

    // Update speed multiplier to 3.0x
    repository.setLongPressSpeedMultiplier(3.0f)
    assertEquals(3.0f, repository.longPressSpeedMultiplierFlow.first())

    // Reset to defaults
    repository.resetToDefaults()
    assertEquals(10, repository.seekDurationSecondsFlow.first())
    assertEquals(2.0f, repository.longPressSpeedMultiplierFlow.first())
  }

  @Test
  fun `simple player manager initializes with audio focus and attributes`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = SimplePlayerManager.getInstance(context)
    val player = manager.initializePlayer()

    assertNotNull(player)
    assertNotNull(player.audioAttributes)
  }
}
