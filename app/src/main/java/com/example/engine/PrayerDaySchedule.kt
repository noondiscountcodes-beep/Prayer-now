package com.example.engine

import java.util.Calendar

data class PrayerTimeEntry(
    val type: PrayerType,
    val timestampMillis: Long,
    val formattedTime24: String,
    val formattedTime12: String
)

data class PrayerDaySchedule(
    val dateYear: Int,
    val dateMonth: Int, // 1-12
    val dateDay: Int,
    val fajr: PrayerTimeEntry,
    val sunrise: PrayerTimeEntry,
    val dhuhr: PrayerTimeEntry,
    val asr: PrayerTimeEntry,
    val maghrib: PrayerTimeEntry,
    val isha: PrayerTimeEntry
) {
    val allEntries: List<PrayerTimeEntry>
        get() = listOf(fajr, sunrise, dhuhr, asr, maghrib, isha)

    val obligatoryPrayers: List<PrayerTimeEntry>
        get() = listOf(fajr, dhuhr, asr, maghrib, isha)

    fun getCurrentPrayer(currentTimeMillis: Long): PrayerType {
        val prayers = listOf(fajr, dhuhr, asr, maghrib, isha)
        // If current time is before Fajr, current prayer is Isha of previous night
        if (currentTimeMillis < fajr.timestampMillis) {
            return PrayerType.ISHA
        }
        for (i in prayers.indices.reversed()) {
            if (currentTimeMillis >= prayers[i].timestampMillis) {
                return prayers[i].type
            }
        }
        return PrayerType.ISHA
    }

    fun getNextPrayer(currentTimeMillis: Long): PrayerTimeEntry {
        // Among obligatory prayers (or including sunrise if desired, user requested 5 prayers)
        // We evaluate next upcoming prayer:
        for (entry in obligatoryPrayers) {
            if (entry.timestampMillis > currentTimeMillis) {
                return entry
            }
        }
        // If all prayers passed today, next prayer is tomorrow's Fajr
        // We can approximate by returning Fajr with timestamp + 24 hours
        return fajr.copy(timestampMillis = fajr.timestampMillis + 24 * 60 * 60 * 1000L)
    }

    fun getRemainingSecondsToNext(currentTimeMillis: Long): Long {
        val next = getNextPrayer(currentTimeMillis)
        val diffSec = (next.timestampMillis - currentTimeMillis) / 1000L
        return if (diffSec > 0) diffSec else 0L
    }

    companion object {
        fun formatRemaining(seconds: Long): String {
            val s = if (seconds < 0) 0 else seconds
            val hours = s / 3600
            val minutes = (s % 3600) / 60
            val secs = s % 60
            return String.format("%02d:%02d:%02d", hours, minutes, secs)
        }
    }
}
