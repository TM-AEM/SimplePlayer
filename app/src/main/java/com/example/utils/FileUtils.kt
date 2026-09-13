package com.example.utils

import java.util.Locale

/**
 * Utility functions for file operations and formatting.
 */
object FileUtils {

    /**
     * Formats bytes into human readable format (e.g. 120 MB, 1.4 GB, 450 KB).
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.getDefault(), "%.1f GB", gb)
            mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.getDefault(), "%.1f KB", kb)
            else -> "$bytes B"
        }
    }
}
