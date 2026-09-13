package com.example

import com.example.utils.TimeUtils
import org.junit.Assert.*
import org.junit.Test

/**
 * Local unit tests for SimplePlayer utility and state models.
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun formatDuration_formatsCorrectly() {
    assertEquals("00:00", TimeUtils.formatDuration(0L))
    assertEquals("01:05", TimeUtils.formatDuration(65_000L))
    assertEquals("01:00:00", TimeUtils.formatDuration(3600_000L))
  }

  @Test
  fun formatFileSize_formatsCorrectly() {
    assertEquals("0 B", com.example.utils.FileUtils.formatFileSize(0L))
    assertEquals("500 B", com.example.utils.FileUtils.formatFileSize(500L))
    assertTrue(com.example.utils.FileUtils.formatFileSize(1024 * 1024 * 120L).contains("120"))
    assertTrue(com.example.utils.FileUtils.formatFileSize(1024 * 1024 * 1024L * 2L).contains("2"))
  }

  @Test
  fun settingsRepository_defaultsAreValid() {
    assertEquals(10, com.example.data.SettingsRepository.DEFAULT_SEEK_SECONDS)
    assertEquals(2.0f, com.example.data.SettingsRepository.DEFAULT_SPEED_MULTIPLIER)
    assertTrue(com.example.data.SettingsRepository.AVAILABLE_LONG_PRESS_SPEEDS.contains(2.0f))
    assertTrue(com.example.data.SettingsRepository.AVAILABLE_LONG_PRESS_SPEEDS.contains(1.5f))
    assertTrue(com.example.data.SettingsRepository.AVAILABLE_LONG_PRESS_SPEEDS.contains(2.5f))
    assertTrue(com.example.data.SettingsRepository.AVAILABLE_LONG_PRESS_SPEEDS.contains(3.0f))
  }
}

