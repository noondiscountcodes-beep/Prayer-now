package com.example

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
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
import com.example.widgets.WidgetSyncHelper

enum class ScreenState {
    MOSQUE_CLOCK,
    MENU
}

class MainActivity : ComponentActivity() {

    private lateinit var prefs: AppPreferences

    // Dynamic notification triggers
    private val pendingVideoPrayerState = mutableStateOf<String?>(null)
    private val pendingMusaharatiState = mutableStateOf(false)
    private val pendingIftarCannonState = mutableStateOf(false)
    private val pendingAdhanScreenPrayer = mutableStateOf<String?>(null)
    private val pendingAdhanScreenType = mutableStateOf("ADHAN")
    private val pendingAlertMinutes = mutableStateOf<Int?>(null)
    private val initialMenuTabState = mutableStateOf(MenuTab.LANGUAGE)
    private val screenState = mutableStateOf(ScreenState.MOSQUE_CLOCK)

    private fun clearScreenLockFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(false)
            setTurnScreenOn(false)
        } else {
            @Suppress("DEPRECATION")
            window.clearFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        prefs = AppPreferences(this)
        PrayerNotificationHelper.createNotificationChannels(this)
        clearScreenLockFlags()

        // Schedule alarms & start persistent notification bar
        AlarmScheduler.scheduleAll(this)
        if (prefs.isPersistentNotificationEnabled()) {
            PrayerNotificationService.start(this)
        }
        WidgetSyncHelper.syncAll(this)

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
                        Box(modifier = Modifier.fillMaxSize()) {
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
                                            pendingIftarCannon = pendingIftarCannonState.value,
                                            onVideoHandled = {
                                                pendingVideoPrayerState.value = null
                                                pendingMusaharatiState.value = false
                                                pendingIftarCannonState.value = false
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

                            // Trigger Adhan / Suhoor / Alert / Iftar Cannon True Full-Screen View
                            pendingAdhanScreenPrayer.value?.let { prayerStr ->
                                val pt = try { com.example.engine.PrayerType.valueOf(prayerStr) } catch (e: Exception) { com.example.engine.PrayerType.FAJR }
                                com.example.ui.AdhanFullScreenView(
                                    prayer = pt,
                                    prefs = prefs,
                                    triggerType = pendingAdhanScreenType.value,
                                    alertMinutes = pendingAlertMinutes.value,
                                    onDismiss = {
                                        pendingAdhanScreenPrayer.value = null
                                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                                    },
                                    onOpenDuaVideo = { uri ->
                                        val currentScreenType = pendingAdhanScreenType.value
                                        pendingAdhanScreenPrayer.value = null
                                        if (currentScreenType == "IFTAR_CANNON") {
                                            pendingIftarCannonState.value = true
                                        } else if (currentScreenType == "SUHOOR") {
                                            pendingMusaharatiState.value = true
                                        } else {
                                            pendingVideoPrayerState.value = prayerStr
                                        }
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

    override fun onResume() {
        super.onResume()
        WidgetSyncHelper.syncAll(this)
        AlarmScheduler.scheduleAll(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val prayerVideo = intent.getStringExtra("TRIGGER_ADHAN_VIDEO")
        val adhanScreen = intent.getStringExtra("TRIGGER_ADHAN_SCREEN")
        val screenType = intent.getStringExtra("TRIGGER_SCREEN_TYPE") ?: "ADHAN"
        val alertMin = intent.getIntExtra("TRIGGER_ALERT_MINUTES", -1).takeIf { it > 0 }
        val musaharati = intent.getBooleanExtra("TRIGGER_MUSAHARATI_VIDEO", false)
        val iftarCannon = intent.getBooleanExtra("TRIGGER_IFTAR_CANNON_VIDEO", false)
        val openTab = intent.getStringExtra("OPEN_TAB")

        if (adhanScreen != null) {
            pendingAdhanScreenPrayer.value = adhanScreen
            pendingAdhanScreenType.value = screenType
            pendingAlertMinutes.value = alertMin
        }

        if (prayerVideo != null) {
            pendingAdhanScreenPrayer.value = null
            pendingVideoPrayerState.value = prayerVideo
            screenState.value = ScreenState.MOSQUE_CLOCK
        }
        if (musaharati) {
            pendingMusaharatiState.value = true
            screenState.value = ScreenState.MOSQUE_CLOCK
        }
        if (iftarCannon) {
            pendingIftarCannonState.value = true
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
