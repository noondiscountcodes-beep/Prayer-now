package com.example.media

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileOutputStream

data class MediaMetadataResult(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val isVideo: Boolean,
    val isCompatible: Boolean,
    val errorMessage: String? = null
)

object MediaHelper {
    private const val TAG = "MediaHelper"
    private var activePlayer: MediaPlayer? = null

    fun inspectMediaUri(context: Context, uri: Uri, isExpectedVideo: Boolean = false): MediaMetadataResult {
        var displayName = "media_file"
        var sizeBytes = 0L
        var durationMs = 0L
        var isVideo = false
        var isCompatible = false
        var errorMessage: String? = null

        // Try taking persistable URI permission if available
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (e: Exception) {
            Log.d(TAG, "Persistable URI permission not applicable: ${e.message}")
        }

        // Query ContentResolver for name and size
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) displayName = cursor.getString(nameIndex) ?: displayName
                    if (sizeIndex != -1) sizeBytes = cursor.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query OpenableColumns: ${e.message}")
        }

        // Use MediaMetadataRetriever to check track compatibility and duration
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            if (durationStr != null) {
                durationMs = durationStr.toLongOrNull() ?: 0L
            }
            val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
            isVideo = hasVideo != null && hasVideo.equals("yes", ignoreCase = true)
            isCompatible = if (isExpectedVideo) isVideo else (durationMs > 0)
        } catch (e: Exception) {
            Log.e(TAG, "MediaMetadataRetriever error inspecting file: ${e.message}")
            isCompatible = false
            errorMessage = e.localizedMessage ?: "Incompatible media format"
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // ignore
            }
        }

        return MediaMetadataResult(
            uri = uri,
            displayName = displayName,
            sizeBytes = sizeBytes,
            durationMs = durationMs,
            isVideo = isVideo,
            isCompatible = isCompatible,
            errorMessage = errorMessage
        )
    }

    fun isUriAvailable(context: Context, uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun playAudioPreview(context: Context, uriString: String?, onCompletion: (() -> Unit)? = null) {
        stopAudioPreview()
        if (uriString.isNullOrBlank()) {
            playDefaultTone(context, onCompletion)
            return
        }

        try {
            val uri = Uri.parse(uriString)
            val player = MediaPlayer().apply {
                setDataSource(context, uri)
                setOnPreparedListener { start() }
                setOnCompletionListener {
                    stopAudioPreview()
                    onCompletion?.invoke()
                }
                setOnErrorListener { _, _, _ ->
                    stopAudioPreview()
                    playDefaultTone(context, onCompletion)
                    true
                }
                prepareAsync()
            }
            activePlayer = player
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio preview, falling back to default: ${e.message}")
            stopAudioPreview()
            playDefaultTone(context, onCompletion)
        }
    }

    private fun playDefaultTone(context: Context, onCompletion: (() -> Unit)?) {
        try {
            val toneUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            val player = MediaPlayer.create(context, toneUri)
            if (player != null) {
                player.setOnCompletionListener {
                    stopAudioPreview()
                    onCompletion?.invoke()
                }
                player.start()
                activePlayer = player
            } else {
                onCompletion?.invoke()
            }
        } catch (e: Exception) {
            onCompletion?.invoke()
        }
    }

    fun stopAudioPreview() {
        try {
            activePlayer?.stop()
            activePlayer?.release()
        } catch (e: Exception) {
            // ignore
        } finally {
            activePlayer = null
        }
    }

    fun formatDuration(durationMs: Long): String {
        val totalSec = durationMs / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format("%02d:%02d", min, sec)
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format("%.1f MB", mb)
            kb >= 1.0 -> String.format("%.1f KB", kb)
            else -> "$bytes B"
        }
    }
}
