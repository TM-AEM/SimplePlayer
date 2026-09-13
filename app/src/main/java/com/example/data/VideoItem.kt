package com.example.data

import android.net.Uri
import com.example.utils.FileUtils
import com.example.utils.TimeUtils

/**
 * Data model representing a video file for SimplePlayer.
 */
data class VideoItem(
    val id: Long = 0L,
    val title: String,
    val uri: Uri,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val dateAdded: Long = 0L,
    val mimeType: String = "video/*"
) {
    val formattedDuration: String
        get() = TimeUtils.formatDuration(durationMs)

    val formattedSize: String
        get() = FileUtils.formatFileSize(sizeBytes)
}
