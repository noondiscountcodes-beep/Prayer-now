package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.MainActivity
import com.example.data.SalawatPreferencesRepository

object SalawatAlarmScheduler {
    private const val TAG = "SalawatAlarmScheduler"
    private const val REQUEST_CODE_SALAWAT = 7700

    fun scheduleNext(context: Context) {
        val repo = SalawatPreferencesRepository(context)
        val config = repo.getConfig()

        if (!config.isEnabled) {
            cancel(context)
            return
        }

        val nextTriggerTime = repo.calculateNextTriggerTime(config)
        if (nextTriggerTime <= 0L) {
            Log.d(TAG, "Salawat is disabled or no valid trigger time")
            repo.setNextScheduledTriggerTime(0L)
            return
        }

        repo.setNextScheduledTriggerTime(nextTriggerTime)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = createSalawatPendingIntent(context)

        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_TAB", "SALAWAT")
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_SALAWAT + 1,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canExact) {
            try {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(nextTriggerTime, showPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                Log.d(TAG, "Scheduled Salawat alarmClock for: $nextTriggerTime")
                updatePersistentNotification(context, config, nextTriggerTime)
                return
            } catch (e: Exception) {
                Log.w(TAG, "setAlarmClock failed: ${e.message}, trying setExactAndAllowWhileIdle")
            }

            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTriggerTime, pendingIntent)
                Log.d(TAG, "Scheduled Salawat exact alarm for: $nextTriggerTime")
                updatePersistentNotification(context, config, nextTriggerTime)
                return
            } catch (e2: Exception) {
                Log.w(TAG, "setExactAndAllowWhileIdle failed: ${e2.message}")
            }
        }

        try {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTriggerTime, pendingIntent)
            updatePersistentNotification(context, config, nextTriggerTime)
        } catch (e3: Exception) {
            Log.e(TAG, "Failed to schedule Salawat alarm: ${e3.message}")
        }
    }

    private fun updatePersistentNotification(context: Context, config: com.example.data.SalawatConfig, nextTriggerTime: Long) {
        if (config.showPersistentNotification) {
            PrayerNotificationHelper.updateSalawatPersistentNotification(context, nextTriggerTime)
        } else {
            PrayerNotificationHelper.cancelSalawatPersistentNotification(context)
        }
    }

    fun cancel(context: Context) {
        try {
            val repo = SalawatPreferencesRepository(context)
            repo.setNextScheduledTriggerTime(0L)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val pendingIntent = createSalawatPendingIntent(context)
            alarmManager.cancel(pendingIntent)
            PrayerNotificationHelper.cancelSalawatPersistentNotification(context)
            Log.d(TAG, "Cancelled Salawat alarm")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling Salawat alarm: ${e.message}")
        }
    }

    fun triggerNow(context: Context) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SALAWAT_REMINDER
        }
        context.sendBroadcast(intent)
    }

    private fun createSalawatPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SALAWAT_REMINDER
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_SALAWAT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
