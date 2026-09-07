package com.example.notifications

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.example.data.AppPreferences
import kotlinx.coroutines.*

class PrayerNotificationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var updateJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        PrayerNotificationHelper.createNotificationChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = AppPreferences(this)
        if (!prefs.isPersistentNotificationEnabled()) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = PrayerNotificationHelper.buildPersistentNotification(this)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    PrayerNotificationHelper.NOTIFICATION_ID_PERSISTENT,
                    notification,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    } else 0
                )
            } else {
                startForeground(PrayerNotificationHelper.NOTIFICATION_ID_PERSISTENT, notification)
            }
        } catch (e: Exception) {
            startForeground(PrayerNotificationHelper.NOTIFICATION_ID_PERSISTENT, notification)
        }

        startPeriodicUpdate()

        return START_STICKY
    }

    private fun startPeriodicUpdate() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            while (isActive) {
                // Sleep for 30 seconds for battery-friendly countdown update
                delay(30_000L)
                val prefs = AppPreferences(this@PrayerNotificationService)
                if (!prefs.isPersistentNotificationEnabled()) {
                    stopSelf()
                    break
                }
                com.example.widgets.WidgetSyncHelper.syncAll(this@PrayerNotificationService)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel()
        serviceScope.cancel()
    }

    companion object {
        fun start(context: Context) {
            val prefs = AppPreferences(context)
            if (!prefs.isPersistentNotificationEnabled()) return
            val intent = Intent(context, PrayerNotificationService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // Catch any background start restrictions gracefully
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, PrayerNotificationService::class.java)
            context.stopService(intent)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.cancel(PrayerNotificationHelper.NOTIFICATION_ID_PERSISTENT)
        }
    }
}
