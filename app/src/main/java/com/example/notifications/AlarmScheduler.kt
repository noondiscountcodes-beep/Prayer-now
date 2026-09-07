package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.AlertsRepository
import com.example.data.AppPreferences
import com.example.data.RamadanMode
import com.example.data.SuhoorAlertMode
import com.example.engine.HijriCalendarHelper
import com.example.engine.PrayerTimesCalculator
import com.example.engine.PrayerType
import java.util.Calendar

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"

    fun scheduleAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val prefs = AppPreferences(context)
        val now = System.currentTimeMillis()

        val cal = Calendar.getInstance(prefs.getTimezone())
        val todaySchedule = PrayerTimesCalculator.calculate(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH),
            latitude = prefs.getLatitude(),
            longitude = prefs.getLongitude(),
            timezone = prefs.getTimezone(),
            dstSetting = prefs.getDstSetting(),
            method = prefs.getCalculationMethod(),
            madhab = prefs.getMadhab(),
            customFajrAngle = prefs.getCustomFajrAngle(),
            customIshaAngle = prefs.getCustomIshaAngle()
        )

        // 1. Schedule next Adhan
        for (prayer in todaySchedule.obligatoryPrayers) {
            val config = prefs.getAdhanConfig(prayer.type)
            if (config.isEnabled && prayer.timestampMillis > now) {
                scheduleExact(
                    context,
                    alarmManager,
                    prayer.timestampMillis,
                    createPrayerAlarmIntent(context, prayer.type)
                )
                break
            }
        }

        // 2. Schedule custom alerts
        val alertsRepo = AlertsRepository(context)
        val activeAlerts = alertsRepo.getAllAlerts().filter { it.isEnabled }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

        for (alert in activeAlerts) {
            if (alert.daysOfWeek.contains(dayOfWeek)) {
                val targets = if (alert.targetPrayer == "ALL") {
                    todaySchedule.obligatoryPrayers
                } else {
                    todaySchedule.obligatoryPrayers.filter { it.type.name == alert.targetPrayer }
                }

                for (target in targets) {
                    val triggerTime = target.timestampMillis - (alert.minutesBefore * 60 * 1000L)
                    if (triggerTime > now) {
                        scheduleExact(
                            context,
                            alarmManager,
                            triggerTime,
                            createCustomAlertIntent(context, alert.id)
                        )
                    }
                }
            }
        }

        // 3. Schedule Musaharati & Iftar Cannon if Ramadan mode is enabled
        val hijri = HijriCalendarHelper.fromGregorian(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            prefs.getHijriAdjustment()
        )
        val isRamadanActive = when (prefs.getRamadanMode()) {
            RamadanMode.AUTO -> hijri.isRamadan
            RamadanMode.MANUAL_ON -> true
            RamadanMode.MANUAL_OFF -> false
        }

        if (isRamadanActive) {
            // Suhoor Musaharati
            val musaharatiConfig = prefs.getMusaharatiConfig()
            if (musaharatiConfig.isEnabled) {
                val suhoorTriggerTime = when (prefs.getSuhoorAlertMode()) {
                    SuhoorAlertMode.OFFSET_FAJR -> {
                        todaySchedule.fajr.timestampMillis - (prefs.getSuhoorOffsetMinutes() * 60 * 1000L)
                    }
                    SuhoorAlertMode.FIXED_TIME -> {
                        val parts = prefs.getSuhoorManualTime().split(":")
                        val h = parts.getOrNull(0)?.toIntOrNull() ?: 4
                        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        val fixedCal = Calendar.getInstance(prefs.getTimezone()).apply {
                            set(Calendar.HOUR_OF_DAY, h)
                            set(Calendar.MINUTE, m)
                            set(Calendar.SECOND, 0)
                        }
                        fixedCal.timeInMillis
                    }
                }

                if (suhoorTriggerTime > now) {
                    scheduleExact(
                        context,
                        alarmManager,
                        suhoorTriggerTime,
                        createMusaharatiIntent(context)
                    )
                }
            }

            // Iftar Cannon (Works right before Maghrib Adhan in Ramadan)
            val iftarCannonConfig = prefs.getIftarCannonConfig()
            if (iftarCannonConfig.isEnabled) {
                val offsetMin = prefs.getIftarCannonOffsetMinutes()
                val cannonTriggerTime = todaySchedule.maghrib.timestampMillis - (offsetMin * 60 * 1000L)
                if (cannonTriggerTime > now) {
                    scheduleExact(
                        context,
                        alarmManager,
                        cannonTriggerTime,
                        createIftarCannonIntent(context)
                    )
                }
            }
        }
    }

    private fun scheduleExact(
        context: Context,
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm: ${e.message}")
        }
    }

    private fun createPrayerAlarmIntent(context: Context, type: PrayerType): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_PRAYER_ALARM
            putExtra(AlarmReceiver.EXTRA_PRAYER_NAME, type.name)
        }
        return PendingIntent.getBroadcast(
            context,
            type.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createCustomAlertIntent(context: Context, alertId: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_CUSTOM_ALERT
            putExtra(AlarmReceiver.EXTRA_ALERT_ID, alertId)
        }
        return PendingIntent.getBroadcast(
            context,
            alertId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createMusaharatiIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_MUSAHARATI
        }
        return PendingIntent.getBroadcast(
            context,
            888,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createIftarCannonIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_IFTAR_CANNON
        }
        return PendingIntent.getBroadcast(
            context,
            889,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
