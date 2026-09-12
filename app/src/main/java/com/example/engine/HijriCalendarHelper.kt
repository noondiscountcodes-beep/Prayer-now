package com.example.engine

import java.util.Calendar
import kotlin.math.floor

data class HijriDate(
    val day: Int,
    val month: Int, // 1-12
    val year: Int,
    val isRamadan: Boolean = (month == 9)
) {
    fun format(languageCode: String): String {
        val monthName = HijriCalendarHelper.getMonthName(month, languageCode)
        return when (languageCode) {
            "ar" -> "$day $monthName $year هـ"
            "fr" -> "$day $monthName $year AH"
            else -> "$day $monthName $year AH"
        }
    }
}

object HijriCalendarHelper {

    fun getMonthName(month: Int, languageCode: String): String {
        return when (month) {
            1 -> when (languageCode) {
                "ar" -> "محرم"
                "fr" -> "Mouharram"
                else -> "Muharram"
            }
            2 -> when (languageCode) {
                "ar" -> "صفر"
                "fr" -> "Safar"
                else -> "Safar"
            }
            3 -> when (languageCode) {
                "ar" -> "ربيع الأول"
                "fr" -> "Rabi' al-awwal"
                else -> "Rabi' al-Awwal"
            }
            4 -> when (languageCode) {
                "ar" -> "ربيع الآخر"
                "fr" -> "Rabi' al-thani"
                else -> "Rabi' al-Thani"
            }
            5 -> when (languageCode) {
                "ar" -> "جمادى الأولى"
                "fr" -> "Joumada al-oula"
                else -> "Jumada al-Awwal"
            }
            6 -> when (languageCode) {
                "ar" -> "جمادى الآخرة"
                "fr" -> "Joumada al-thania"
                else -> "Jumada al-Thani"
            }
            7 -> when (languageCode) {
                "ar" -> "رجب"
                "fr" -> "Rajab"
                else -> "Rajab"
            }
            8 -> when (languageCode) {
                "ar" -> "شعبان"
                "fr" -> "Cha'ban"
                else -> "Sha'ban"
            }
            9 -> when (languageCode) {
                "ar" -> "رمضان"
                "fr" -> "Ramadan"
                else -> "Ramadan"
            }
            10 -> when (languageCode) {
                "ar" -> "شوال"
                "fr" -> "Chawwal"
                else -> "Shawwal"
            }
            11 -> when (languageCode) {
                "ar" -> "ذو القعدة"
                "fr" -> "Dhou al-qi'da"
                else -> "Dhu al-Qi'dah"
            }
            12 -> when (languageCode) {
                "ar" -> "ذو الحجة"
                "fr" -> "Dhou al-hijja"
                else -> "Dhu al-Hijjah"
            }
            else -> ""
        }
    }

    fun getDayOfWeekName(dayOfWeek: Int, languageCode: String): String {
        // Calendar.SUNDAY = 1, MONDAY = 2, ...
        return when (dayOfWeek) {
            Calendar.SUNDAY -> when (languageCode) {
                "ar" -> "الأحد"
                "fr" -> "Dimanche"
                else -> "Sunday"
            }
            Calendar.MONDAY -> when (languageCode) {
                "ar" -> "الإثنين"
                "fr" -> "Lundi"
                else -> "Monday"
            }
            Calendar.TUESDAY -> when (languageCode) {
                "ar" -> "الثلاثاء"
                "fr" -> "Mardi"
                else -> "Tuesday"
            }
            Calendar.WEDNESDAY -> when (languageCode) {
                "ar" -> "الأربعاء"
                "fr" -> "Mercredi"
                else -> "Wednesday"
            }
            Calendar.THURSDAY -> when (languageCode) {
                "ar" -> "الخميس"
                "fr" -> "Jeudi"
                else -> "Thursday"
            }
            Calendar.FRIDAY -> when (languageCode) {
                "ar" -> "الجمعة"
                "fr" -> "Vendredi"
                else -> "Friday"
            }
            Calendar.SATURDAY -> when (languageCode) {
                "ar" -> "السبت"
                "fr" -> "Samedi"
                else -> "Saturday"
            }
            else -> ""
        }
    }

    fun fromGregorian(year: Int, month: Int, day: Int, dayAdjustment: Int = 0): HijriDate {
        // Try standard java.time.chrono.HijrahChronology (Umm al-Qura calendar, default on Android 8.0+ / API 26+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                val localDate = java.time.LocalDate.of(year, month, day).plusDays(dayAdjustment.toLong())
                val hijrahDate = java.time.chrono.HijrahChronology.INSTANCE.date(localDate)
                val hYear = hijrahDate.get(java.time.temporal.ChronoField.YEAR)
                val hMonth = hijrahDate.get(java.time.temporal.ChronoField.MONTH_OF_YEAR)
                val hDay = hijrahDate.get(java.time.temporal.ChronoField.DAY_OF_MONTH)
                return HijriDate(
                    day = hDay,
                    month = hMonth,
                    year = hYear,
                    isRamadan = (hMonth == 9)
                )
            } catch (e: Exception) {
                // Fallback to tabular astronomical algorithm if java.time fails
            }
        }

        // Tabular Islamic Calendar Algorithm (accurate 30-year cycle fallback)
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        val jd = floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.0 + dayAdjustment

        // Days since Islamic epoch (July 16, 622 CE = JD 1948439.5)
        val daysSinceEpoch = (jd - 1948440.0 + 1.0).toLong()
        val cycle = floor((daysSinceEpoch - 1.0) / 10631.0).toLong()
        var dayInCycle = ((daysSinceEpoch - 1) % 10631).toInt()
        if (dayInCycle < 0) dayInCycle += 10631

        val leapYears = setOf(2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29)
        var yearInCycle = 1
        var remDays = dayInCycle
        while (yearInCycle <= 30) {
            val yearLen = if (leapYears.contains(yearInCycle)) 355 else 354
            if (remDays < yearLen) break
            remDays -= yearLen
            yearInCycle++
        }

        val hYear = (cycle * 30 + yearInCycle).toInt()
        var hMonth = 1
        while (hMonth <= 12) {
            val monthLen = when {
                hMonth % 2 == 1 -> 30
                hMonth == 12 && leapYears.contains(yearInCycle) -> 30
                else -> 29
            }
            if (remDays < monthLen) break
            remDays -= monthLen
            hMonth++
        }
        val hDay = (remDays + 1).coerceIn(1, 30)

        return HijriDate(
            day = hDay,
            month = hMonth.coerceIn(1, 12),
            year = hYear,
            isRamadan = (hMonth == 9)
        )
    }
}
