package com.example.utils

import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Utility functions for formatting time and media durations.
 */
object TimeUtils {

    /**
     * Formats milliseconds into a standard duration string (e.g., "04:12" or "01:23:45").
     */
    fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0) return "00:00"
        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }
}
