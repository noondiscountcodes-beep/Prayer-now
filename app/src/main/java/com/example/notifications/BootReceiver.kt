package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppPreferences

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            // 1. Reschedule alarms
            AlarmScheduler.scheduleAll(context)

            // 2. Restore persistent notification bar if enabled
            val prefs = AppPreferences(context)
            if (prefs.isPersistentNotificationEnabled()) {
                PrayerNotificationService.start(context)
            }
        }
    }
}
