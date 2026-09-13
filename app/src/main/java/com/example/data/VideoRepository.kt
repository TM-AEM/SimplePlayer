package com.example.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository responsible for querying local video files from device storage
 * using ContentResolver and MediaStore on Dispatchers.IO.
 */
class VideoRepository(private val context: Context) {

    /**
     * Queries device storage for all video files.
     * Guaranteed to execute asynchronously on Dispatchers.IO.
     */
    suspend fun queryDeviceVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        val videoList = mutableListOf<VideoItem>()
        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.MIME_TYPE
        )

        // Filter out zero-sized files
        val selection = "${MediaStore.Video.Media.SIZE} > 0"
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val displayNameColumn = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val titleColumn = cursor.getColumnIndex(MediaStore.Video.Media.TITLE)
                val durationColumn = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val sizeColumn = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                val dateAddedColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
                val mimeTypeColumn = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val displayName = if (displayNameColumn != -1) cursor.getString(displayNameColumn) else null
                    val title = if (titleColumn != -1) cursor.getString(titleColumn) else null
                    val duration = if (durationColumn != -1) cursor.getLong(durationColumn) else 0L
                    val size = if (sizeColumn != -1) cursor.getLong(sizeColumn) else 0L
                    val dateAdded = if (dateAddedColumn != -1) cursor.getLong(dateAddedColumn) else 0L
                    val mimeType = if (mimeTypeColumn != -1) cursor.getString(mimeTypeColumn) ?: "video/*" else "video/*"

                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val videoName = displayName?.takeIf { it.isNotBlank() }
                        ?: title?.takeIf { it.isNotBlank() }
                        ?: "Video_$id"

                    videoList.add(
                        VideoItem(
                            id = id,
                            title = videoName,
                            uri = contentUri,
                            durationMs = duration,
                            sizeBytes = size,
                            dateAdded = dateAdded,
                            mimeType = mimeType
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("VideoRepository", "Error querying device videos from MediaStore", e)
        }

        videoList
    }
}
