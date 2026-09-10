package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.MainActivity
import com.example.data.AdhanPreferencesRepository
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
        val adhanRepo = AdhanPreferencesRepository(context)
        val alertsRepo = AlertsRepository(context)
        val activeAlerts = alertsRepo.getAllAlerts().filter { it.isEnabled }
        val now = System.currentTimeMillis()

        // Schedule for the next 7 days continuously (ensures alarms never expire if app remains closed)
        val baseCal = Calendar.getInstance(prefs.getTimezone())

        for (dayOffset in 0..6) {
            val cal = Calendar.getInstance(prefs.getTimezone()).apply {
                timeInMillis = baseCal.timeInMillis
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }

            val daySchedule = PrayerTimesCalculator.calculate(
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

            // 1. Schedule all upcoming Adhans for this day
            for (prayer in daySchedule.obligatoryPrayers) {
                val audioConfig = adhanRepo.getAudioConfig(prayer.type)
                val screenConfig = adhanRepo.getScreenConfig(prayer.type)
                val legacyConfig = prefs.getAdhanConfig(prayer.type)

                // Adhan alarm is active if audio or screen is enabled or legacy is enabled
                val isAdhanActive = audioConfig.isEnabled || screenConfig.autoOpenOnAdhan || legacyConfig.isEnabled

                if (isAdhanActive && prayer.timestampMillis > now) {
                    scheduleExact(
                        context,
                        alarmManager,
                        prayer.timestampMillis,
                        createPrayerAlarmIntent(context, prayer.type, dayOffset)
                    )
                }
            }

            // 2. Schedule custom alerts for this day
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            for (alert in activeAlerts) {
                if (alert.daysOfWeek.contains(dayOfWeek)) {
                    val targets = if (alert.targetPrayer == "ALL") {
                        daySchedule.obligatoryPrayers
                    } else {
                        daySchedule.obligatoryPrayers.filter { it.type.name == alert.targetPrayer }
                    }

                    for (target in targets) {
                        val triggerTime = target.timestampMillis - (alert.minutesBefore * 60 * 1000L)
                        if (triggerTime > now) {
                            scheduleExact(
                                context,
                                alarmManager,
                                triggerTime,
                                createCustomAlertIntent(context, alert.id, target.type, dayOffset)
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
                            daySchedule.fajr.timestampMillis - (prefs.getSuhoorOffsetMinutes() * 60 * 1000L)
                        }
                        SuhoorAlertMode.FIXED_TIME -> {
                            val parts = prefs.getSuhoorManualTime().split(":")
                            val h = parts.getOrNull(0)?.toIntOrNull() ?: 4
                            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                            val fixedCal = Calendar.getInstance(prefs.getTimezone()).apply {
                                timeInMillis = cal.timeInMillis
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
                            createMusaharatiIntent(context, dayOffset)
                        )
                    }
                }

                // Iftar Cannon
                val iftarCannonConfig = prefs.getIftarCannonConfig()
                if (iftarCannonConfig.isEnabled) {
                    val offsetMin = prefs.getIftarCannonOffsetMinutes()
                    val cannonTriggerTime = daySchedule.maghrib.timestampMillis - (offsetMin * 60 * 1000L)
                    if (cannonTriggerTime > now) {
                        scheduleExact(
                            context,
                            alarmManager,
                            cannonTriggerTime,
                            createIftarCannonIntent(context, dayOffset)
                        )
                    }
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
        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val safeActivityCode = ((triggerAtMillis / 1000) and 0x7FFFFFFF).mod(100000)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            safeActivityCode,
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
                // SetAlarmClock is the Android OS highest priority alarm level.
                // It wakes the CPU and screen even from Doze mode and is exempt from standard OEM app-standby restrictions.
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                return
            } catch (e: Exception) {
                Log.w(TAG, "setAlarmClock failed: ${e.message}, trying setExactAndAllowWhileIdle")
            }

            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                return
            } catch (e2: Exception) {
                Log.w(TAG, "setExactAndAllowWhileIdle failed: ${e2.message}")
            }
        }

        // Fallback for non-exact allowed state
        try {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } catch (e3: Exception) {
            Log.e(TAG, "All alarm scheduling methods failed: ${e3.message}")
        }
    }

    private fun createPrayerAlarmIntent(context: Context, type: PrayerType, dayOffset: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_PRAYER_ALARM
            putExtra(AlarmReceiver.EXTRA_PRAYER_NAME, type.name)
        }
        val requestCode = 1000 + (dayOffset * 10) + type.ordinal
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createCustomAlertIntent(
        context: Context,
        alertId: String,
        targetPrayer: PrayerType,
        dayOffset: Int
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_CUSTOM_ALERT
            putExtra(AlarmReceiver.EXTRA_ALERT_ID, alertId)
            putExtra(AlarmReceiver.EXTRA_PRAYER_NAME, targetPrayer.name)
        }
        val alertHash = (alertId.hashCode() and 0x7FFFFFFF) % 10000
        val requestCode = 20000 + (alertHash * 50) + (dayOffset * 6) + targetPrayer.ordinal
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createMusaharatiIntent(context: Context, dayOffset: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_MUSAHARATI
        }
        val requestCode = 880 + dayOffset
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createIftarCannonIntent(context: Context, dayOffset: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_IFTAR_CANNON
        }
        val requestCode = 890 + dayOffset
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
