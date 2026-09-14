package com.example.data

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

data class SalawatConfig(
    val isEnabled: Boolean = false,
    val intervalMinutes: Int = 30,
    val isAllDay: Boolean = true,
    val startHour: Int = 8,
    val startMinute: Int = 0,
    val endHour: Int = 22,
    val endMinute: Int = 0,
    val isFridayOnly: Boolean = false,
    val vibrate: Boolean = true,
    val lastTriggerMillis: Long = 0L,
    val audioSelectionMode: String = "RANDOM", // "RANDOM" or "SPECIFIC"
    val selectedAudioFileName: String? = null,
    val showPersistentNotification: Boolean = true,
    val nextScheduledTriggerMillis: Long = 0L
)

class SalawatPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("salawat_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ENABLED = "salawat_enabled"
        private const val KEY_INTERVAL = "salawat_interval_minutes"
        private const val KEY_ALL_DAY = "salawat_is_all_day"
        private const val KEY_START_HOUR = "salawat_start_hour"
        private const val KEY_START_MINUTE = "salawat_start_minute"
        private const val KEY_END_HOUR = "salawat_end_hour"
        private const val KEY_END_MINUTE = "salawat_end_minute"
        private const val KEY_FRIDAY_ONLY = "salawat_friday_only"
        private const val KEY_VIBRATE = "salawat_vibrate"
        private const val KEY_LAST_TRIGGER = "salawat_last_trigger"
        private const val KEY_AUDIO_SELECTION_MODE = "salawat_audio_selection_mode"
        private const val KEY_SELECTED_AUDIO_NAME = "salawat_selected_audio_name"
        private const val KEY_SHOW_PERSISTENT_NOTIF = "salawat_show_persistent_notif"
        private const val KEY_NEXT_SCHEDULED_TRIGGER = "salawat_next_scheduled_trigger"
    }

    fun getConfig(): SalawatConfig {
        return SalawatConfig(
            isEnabled = prefs.getBoolean(KEY_ENABLED, false),
            intervalMinutes = prefs.getInt(KEY_INTERVAL, 30).coerceAtLeast(5),
            isAllDay = prefs.getBoolean(KEY_ALL_DAY, true),
            startHour = prefs.getInt(KEY_START_HOUR, 8),
            startMinute = prefs.getInt(KEY_START_MINUTE, 0),
            endHour = prefs.getInt(KEY_END_HOUR, 22),
            endMinute = prefs.getInt(KEY_END_MINUTE, 0),
            isFridayOnly = prefs.getBoolean(KEY_FRIDAY_ONLY, false),
            vibrate = prefs.getBoolean(KEY_VIBRATE, true),
            lastTriggerMillis = prefs.getLong(KEY_LAST_TRIGGER, 0L),
            audioSelectionMode = prefs.getString(KEY_AUDIO_SELECTION_MODE, "RANDOM") ?: "RANDOM",
            selectedAudioFileName = prefs.getString(KEY_SELECTED_AUDIO_NAME, null),
            showPersistentNotification = prefs.getBoolean(KEY_SHOW_PERSISTENT_NOTIF, true),
            nextScheduledTriggerMillis = prefs.getLong(KEY_NEXT_SCHEDULED_TRIGGER, 0L)
        )
    }

    fun setConfig(config: SalawatConfig) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, config.isEnabled)
            .putInt(KEY_INTERVAL, config.intervalMinutes)
            .putBoolean(KEY_ALL_DAY, config.isAllDay)
            .putInt(KEY_START_HOUR, config.startHour)
            .putInt(KEY_START_MINUTE, config.startMinute)
            .putInt(KEY_END_HOUR, config.endHour)
            .putInt(KEY_END_MINUTE, config.endMinute)
            .putBoolean(KEY_FRIDAY_ONLY, config.isFridayOnly)
            .putBoolean(KEY_VIBRATE, config.vibrate)
            .putLong(KEY_LAST_TRIGGER, config.lastTriggerMillis)
            .putString(KEY_AUDIO_SELECTION_MODE, config.audioSelectionMode)
            .putString(KEY_SELECTED_AUDIO_NAME, config.selectedAudioFileName)
            .putBoolean(KEY_SHOW_PERSISTENT_NOTIF, config.showPersistentNotification)
            .putLong(KEY_NEXT_SCHEDULED_TRIGGER, config.nextScheduledTriggerMillis)
            .apply()
    }

    fun setNextScheduledTriggerTime(timeMillis: Long) {
        prefs.edit().putLong(KEY_NEXT_SCHEDULED_TRIGGER, timeMillis).apply()
    }

    fun getNextScheduledTriggerTime(): Long {
        return prefs.getLong(KEY_NEXT_SCHEDULED_TRIGGER, 0L)
    }

    fun setLastTriggerTime(timeMillis: Long) {
        prefs.edit().putLong(KEY_LAST_TRIGGER, timeMillis).apply()
    }

    fun calculateNextTriggerTime(config: SalawatConfig, fromMillis: Long = System.currentTimeMillis()): Long {
        if (!config.isEnabled) return -1L

        val intervalMillis = config.intervalMinutes.coerceAtLeast(5) * 60 * 1000L
        var candidate = fromMillis + intervalMillis

        val cal = Calendar.getInstance().apply { timeInMillis = candidate }

        if (config.isFridayOnly) {
            var safetyCounter = 0
            while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY && safetyCounter < 8) {
                cal.add(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, config.startHour)
                cal.set(Calendar.MINUTE, config.startMinute)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                safetyCounter++
            }
            candidate = cal.timeInMillis
        }

        if (!config.isAllDay) {
            val candCal = Calendar.getInstance().apply { timeInMillis = candidate }
            val currentMinutesOfDay = candCal.get(Calendar.HOUR_OF_DAY) * 60 + candCal.get(Calendar.MINUTE)
            val startMinutesOfDay = config.startHour * 60 + config.startMinute
            val endMinutesOfDay = config.endHour * 60 + config.endMinute

            if (startMinutesOfDay <= endMinutesOfDay) {
                if (currentMinutesOfDay < startMinutesOfDay) {
                    candCal.set(Calendar.HOUR_OF_DAY, config.startHour)
                    candCal.set(Calendar.MINUTE, config.startMinute)
                    candCal.set(Calendar.SECOND, 0)
                    candCal.set(Calendar.MILLISECOND, 0)
                    candidate = candCal.timeInMillis
                } else if (currentMinutesOfDay > endMinutesOfDay) {
                    candCal.add(Calendar.DAY_OF_MONTH, 1)
                    if (config.isFridayOnly) {
                        while (candCal.get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY) {
                            candCal.add(Calendar.DAY_OF_MONTH, 1)
                        }
                    }
                    candCal.set(Calendar.HOUR_OF_DAY, config.startHour)
                    candCal.set(Calendar.MINUTE, config.startMinute)
                    candCal.set(Calendar.SECOND, 0)
                    candCal.set(Calendar.MILLISECOND, 0)
                    candidate = candCal.timeInMillis
                }
            }
        }

        return candidate
    }
}
