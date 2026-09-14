package com.example.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.data.AppPreferences
import com.example.engine.PrayerType
import com.example.media.MediaHelper
import com.example.notifications.AdhanSequencePlayer
import com.example.ui.components.VideoPlayerDialog
import com.example.ui.theme.MosqueDarkBackground
import com.example.ui.theme.MyApplicationTheme

/**
 * Dedicated Activity shown directly over the lock screen for Adhan, Pre-Prayer Alerts,
 * Suhoor, and Ramadan Cannon notifications.
 *
 * It runs as an independent overlay task that DOES NOT open the main app (MainActivity).
 * When dismissed, the activity finishes and leaves the user's phone locked and secure.
 */
class LockScreenAdhanActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure window to show over lockscreen and turn the display on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        // Keep screen lit during active alert/adhan session
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val prefs = AppPreferences(this)
        val prayerNameStr = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "FAJR"
        val triggerType = intent.getStringExtra(EXTRA_TRIGGER_TYPE) ?: "ADHAN"
        val alertMinutes = intent.getIntExtra(EXTRA_ALERT_MINUTES, -1).takeIf { it > 0 }
        val videoUriExtra = intent.getStringExtra(EXTRA_VIDEO_URI)

        val prayerType = try {
            PrayerType.valueOf(prayerNameStr)
        } catch (e: Exception) {
            PrayerType.FAJR
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MosqueDarkBackground
                ) {
                    var activeVideoUri by remember { mutableStateOf(videoUriExtra) }

                    if (activeVideoUri != null) {
                        val videoTitle = when (triggerType) {
                            "IFTAR_CANNON" -> "💥 مدفع الإفطار"
                            "SUHOOR" -> "🌙 المسحراتي"
                            else -> "🤲 دعاء بعد الأذان"
                        }
                        VideoPlayerDialog(
                            videoUriString = activeVideoUri,
                            videoTitle = videoTitle,
                            language = prefs.getLanguage(),
                            onDismiss = {
                                activeVideoUri = null
                                closeLockScreen()
                            }
                        )
                    } else {
                        AdhanFullScreenView(
                            prayer = prayerType,
                            prefs = prefs,
                            triggerType = triggerType,
                            alertMinutes = alertMinutes,
                            onDismiss = {
                                closeLockScreen()
                            },
                            onOpenDuaVideo = { videoUri ->
                                if (!videoUri.isNullOrBlank()) {
                                    activeVideoUri = videoUri
                                } else {
                                    closeLockScreen()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun closeLockScreen() {
        AdhanSequencePlayer.stopAll()
        MediaHelper.stopAudioPreview()
        com.example.notifications.AlarmReceiver.releaseAlertWakeLock()
        try {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setTurnScreenOn(false)
            }
            val lp = window.attributes
            lp.screenBrightness = 0.0f
            window.attributes = lp
        } catch (_: Exception) {}
        finishAndRemoveTask()
    }

    override fun onDestroy() {
        super.onDestroy()
        MediaHelper.stopAudioPreview()
        com.example.notifications.AlarmReceiver.releaseAlertWakeLock()
        try {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } catch (_: Exception) {}
    }

    companion object {
        const val EXTRA_PRAYER_NAME = "EXTRA_PRAYER_NAME"
        const val EXTRA_TRIGGER_TYPE = "EXTRA_TRIGGER_TYPE"
        const val EXTRA_ALERT_MINUTES = "EXTRA_ALERT_MINUTES"
        const val EXTRA_VIDEO_URI = "EXTRA_VIDEO_URI"

        fun createIntent(
            context: Context,
            prayer: PrayerType,
            triggerType: String = "ADHAN",
            alertMinutes: Int? = null,
            videoUri: String? = null
        ): Intent {
            return Intent(context, LockScreenAdhanActivity::class.java).apply {
                putExtra(EXTRA_PRAYER_NAME, prayer.name)
                putExtra(EXTRA_TRIGGER_TYPE, triggerType)
                if (alertMinutes != null) {
                    putExtra(EXTRA_ALERT_MINUTES, alertMinutes)
                }
                if (videoUri != null) {
                    putExtra(EXTRA_VIDEO_URI, videoUri)
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }
    }
}
