package com.example.ui.components

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.media.MediaHelper

@Composable
fun VideoPlayerDialog(
    videoUriString: String?,
    videoTitle: String,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isAvailable = remember(videoUriString) {
        MediaHelper.isUriAvailable(context, videoUriString)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("video_player_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = videoTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_video_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = AppStrings.close(language)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (videoUriString != null && isAvailable) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    val uri = Uri.parse(videoUriString)
                                    setVideoURI(uri)
                                    val controller = MediaController(ctx)
                                    controller.setAnchorView(this)
                                    setMediaController(controller)
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = false
                                        start()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    // Fallback / error gracefully handled as requested
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (language.code == "ar") {
                                    "الملف غير موجود أو تم حذفه من الجهاز، يرجى إعادة اختيار ملف فيديو صالح."
                                } else {
                                    "File not found or was moved/deleted from device. Please select a valid video file."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dismiss_video_button")
                ) {
                    Text(AppStrings.close(language))
                }
            }
        }
    }
}
