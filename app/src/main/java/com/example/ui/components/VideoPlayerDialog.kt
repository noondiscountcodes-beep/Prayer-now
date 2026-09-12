package com.example.ui.components

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.media.MediaHelper
import com.example.ui.theme.IslamicGoldLight

/**
 * Fullscreen Immersive Video Player (YouTube style) for post-Adhan Du'aa and Ramadan videos.
 * Takes up the complete screen Edge-to-Edge with media controls, orientation toggle, and auto-dismiss.
 */
@Composable
fun VideoPlayerDialog(
    videoUriString: String?,
    videoTitle: String,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val isAvailable = remember(videoUriString) {
        MediaHelper.isUriAvailable(context, videoUriString)
    }

    var showControls by remember { mutableStateOf(true) }
    var isLandscape by remember { mutableStateOf(false) }

    // Keep screen on while video dialog is open
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    BackHandler {
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.let { win ->
                win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                win.setBackgroundDrawable(ColorDrawable(android.graphics.Color.BLACK))
                win.setDimAmount(0f)
                WindowCompat.setDecorFitsSystemWindows(win, false)
                val insetsController = WindowCompat.getInsetsController(win, win.decorView)
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("video_player_dialog"),
            contentAlignment = Alignment.Center
        ) {
            if (videoUriString != null && isAvailable) {
                // True Fullscreen Edge-to-Edge Video Surface like YouTube
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
                                try {
                                    mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                                } catch (e: Exception) {
                                    // Ignore if scaling mode not supported
                                }
                                start()
                            }
                            setOnCompletionListener {
                                // Auto dismiss when video playback completes
                                onDismiss()
                            }
                            setOnClickListener {
                                showControls = !showControls
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Missing file or error state
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E293B))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFF87171),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (language.code == "ar") "تعذر العثور على الفيديو" else "Video Not Found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (language.code == "ar")
                            "قد يكون تم حذف ملف الفيديو أو نقله من ذاكرة الجهاز. يرجى اختيار فيديو صالح من قائمة الأذان."
                        else
                            "The video file was removed or is inaccessible. Please select a valid file from the Adhan menu.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text(AppStrings.close(language))
                    }
                }
            }

            // Top Header Overlay with Title, Orientation Switcher & Close button
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = videoTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = IslamicGoldLight,
                                maxLines = 1
                            )
                            Text(
                                text = if (language.code == "ar") "ملء الشاشة • تشغيل تلقائي" else "Full Screen • Auto Play",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFA5BFB9)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Orientation toggle (YouTube style fullscreen rotation)
                            IconButton(
                                onClick = {
                                    isLandscape = !isLandscape
                                    activity?.requestedOrientation = if (isLandscape) {
                                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    }
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ScreenRotation,
                                    contentDescription = "Rotate",
                                    tint = Color.White
                                )
                            }

                            // Close / Dismiss button
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    .testTag("close_video_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = AppStrings.close(language),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
