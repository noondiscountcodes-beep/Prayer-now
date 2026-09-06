package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import com.example.data.AppPreferences
import com.example.notifications.AlarmScheduler
import com.example.notifications.PrayerNotificationHelper
import com.example.notifications.PrayerNotificationService
import com.example.ui.MenuScreen
import com.example.ui.MenuTab
import com.example.ui.MosqueClockScreen
import com.example.ui.theme.MyApplicationTheme

enum class ScreenState {
    MOSQUE_CLOCK,
    MENU
}

class MainActivity : ComponentActivity() {

    private lateinit var prefs: AppPreferences

    // Dynamic notification triggers
    private val pendingVideoPrayerState = mutableStateOf<String?>(null)
    private val pendingMusaharatiState = mutableStateOf(false)
    private val initialMenuTabState = mutableStateOf(MenuTab.LANGUAGE)
    private val screenState = mutableStateOf(ScreenState.MOSQUE_CLOCK)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        prefs = AppPreferences(this)
        PrayerNotificationHelper.createNotificationChannels(this)

        // Schedule alarms & start persistent notification bar
        AlarmScheduler.scheduleAll(this)
        if (prefs.isPersistentNotificationEnabled()) {
            PrayerNotificationService.start(this)
        }

        handleIntent(intent)

        setContent {
            val language by prefs.languageFlow.collectAsState()
            val themeMode by prefs.themeFlow.collectAsState()

            // Notification permission request for Android 13+
            val notifPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted && prefs.isPersistentNotificationEnabled()) {
                    PrayerNotificationService.start(this)
                }
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            val layoutDirection = if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                MyApplicationTheme(darkTheme = true) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AnimatedContent(
                            targetState = screenState.value,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "ScreenTransition"
                        ) { targetScreen ->
                            when (targetScreen) {
                                ScreenState.MOSQUE_CLOCK -> {
                                    MosqueClockScreen(
                                        prefs = prefs,
                                        onOpenMenu = {
                                            screenState.value = ScreenState.MENU
                                        },
                                        pendingVideoPrayer = pendingVideoPrayerState.value,
                                        pendingMusaharati = pendingMusaharatiState.value,
                                        onVideoHandled = {
                                            pendingVideoPrayerState.value = null
                                            pendingMusaharatiState.value = false
                                        }
                                    )
                                }
                                ScreenState.MENU -> {
                                    MenuScreen(
                                        prefs = prefs,
                                        initialTab = initialMenuTabState.value,
                                        onBackToClock = {
                                            screenState.value = ScreenState.MOSQUE_CLOCK
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val prayerVideo = intent.getStringExtra("TRIGGER_ADHAN_VIDEO")
        val musaharati = intent.getBooleanExtra("TRIGGER_MUSAHARATI_VIDEO", false)
        val openTab = intent.getStringExtra("OPEN_TAB")

        if (prayerVideo != null) {
            pendingVideoPrayerState.value = prayerVideo
            screenState.value = ScreenState.MOSQUE_CLOCK
        }
        if (musaharati) {
            pendingMusaharatiState.value = true
            screenState.value = ScreenState.MOSQUE_CLOCK
        }
        if (openTab != null) {
            initialMenuTabState.value = when (openTab) {
                "ADHAN" -> MenuTab.ADHAN
                "ALERTS" -> MenuTab.ALERTS
                "RAMADAN" -> MenuTab.RAMADAN
                "LOCATION" -> MenuTab.LOCATION_METHOD
                else -> MenuTab.LANGUAGE
            }
            screenState.value = ScreenState.MENU
        }
    }
}
