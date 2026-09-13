package com.example.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Utility functions for handling storage permissions across Android versions.
 * Supports Android 13+ (READ_MEDIA_VIDEO) and older Android versions (READ_EXTERNAL_STORAGE).
 */
object PermissionUtils {

    /**
     * Returns the required permission to read video files on the current Android version.
     */
    val requiredVideoPermission: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    /**
     * Checks if the required video permission has been granted.
     */
    fun hasVideoPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            requiredVideoPermission
        ) == PackageManager.PERMISSION_GRANTED
    }
}
