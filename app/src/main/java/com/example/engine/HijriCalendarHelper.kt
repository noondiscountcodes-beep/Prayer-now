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
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        var jd = floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5 + dayAdjustment

        jd = floor(jd) + 0.5
        var z = jd - 1948440 + 10632
        val n = floor((z - 1) / 10631.0)
        z = z - 10631.0 * n + 354.0

        val j = floor((10985.0 - z) / 5316.0) * floor((50.0 * z) / 17719.0) +
                floor(z / 5670.0) * floor((43.0 * z) / 15238.0)
        z = z - floor((30.0 - j) / 15.0) * floor((17719.0 * j) / 50.0) -
                floor(j / 16.0) * floor((15238.0 * j) / 43.0) + 29.0

        val hm = floor((24.0 * z) / 709.0)
        val hd = z - floor((709.0 * hm) / 24.0)
        val hy = 30.0 * n + j - 30.0

        val finalDay = hd.toInt().coerceIn(1, 30)
        val finalMonth = (hm.toInt() + 1).coerceIn(1, 12)
        val finalYear = hy.toInt()

        return HijriDate(
            day = finalDay,
            month = finalMonth,
            year = finalYear,
            isRamadan = (finalMonth == 9)
        )
    }
}
