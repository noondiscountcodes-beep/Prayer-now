package com.example.engine

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.example.R
import com.example.data.AppPreferences
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object PrayerBannerHelper {

    fun toArabicDigits(input: String): String {
        val arabicChars = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        val sb = StringBuilder()
        for (c in input) {
            if (c in '0'..'9') {
                sb.append(arabicChars[c - '0'])
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    fun formatTime12h(timestampMillis: Long, tz: TimeZone, isArabic: Boolean): String {
        val cal = Calendar.getInstance(tz).apply { timeInMillis = timestampMillis }
        val hour24 = cal.get(Calendar.HOUR_OF_DAY)
        val hour12 = cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
        val minute = cal.get(Calendar.MINUTE)
        val isPm = hour24 >= 12
        val marker = if (isArabic) (if (isPm) "م" else "ص") else (if (isPm) "PM" else "AM")
        val rawTime = String.format(Locale.US, "%02d:%02d", hour12, minute)
        val finalDigits = if (isArabic) toArabicDigits(rawTime) else rawTime
        return "$finalDigits $marker"
    }

    fun calculateDiffString(currentTime: Long, currentPrayerMillis: Long, isArabic: Boolean): String {
        return if (currentTime >= currentPrayerMillis) {
            val elapsedSec = (currentTime - currentPrayerMillis) / 1000
            val mins = elapsedSec / 60
            val secs = elapsedSec % 60
            val raw = String.format(Locale.US, "+ %02d:%02d", mins, secs)
            if (isArabic) toArabicDigits(raw) else raw
        } else {
            val remSec = (currentPrayerMillis - currentTime) / 1000
            val mins = remSec / 60
            val secs = remSec % 60
            val raw = String.format(Locale.US, "- %02d:%02d", mins, secs)
            if (isArabic) toArabicDigits(raw) else raw
        }
    }

    /**
     * Binds the electronic banner layout (R.layout.widget_prayer_times)
     */
    fun bindWidgetViews(context: Context, views: RemoteViews, prefs: AppPreferences) {
        val lang = prefs.getLanguage()
        val isArabic = lang == AppLanguage.ARABIC
        val tz = prefs.getTimezone()
        val cal = Calendar.getInstance(tz)

        val schedule = PrayerTimesCalculator.calculate(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH),
            latitude = prefs.getLatitude(),
            longitude = prefs.getLongitude(),
            timezone = tz,
            dstSetting = prefs.getDstSetting(),
            method = prefs.getCalculationMethod(),
            madhab = prefs.getMadhab(),
            customFajrAngle = prefs.getCustomFajrAngle(),
            customIshaAngle = prefs.getCustomIshaAngle()
        )

        val now = System.currentTimeMillis()
        val currentType = schedule.getCurrentPrayer(now)
        val currentEntry = schedule.getEntry(currentType)
        val nextPrayer = schedule.getNextPrayer(now)
        val nextType = nextPrayer.type

        // City & Header
        val cityName = prefs.getCityName()
        views.setTextViewText(R.id.tv_city_name, cityName)

        // Hero Banner
        val heroPrayerName = AppStrings.getPrayerName(currentType, lang)
        val heroDiff = calculateDiffString(now, currentEntry.timestampMillis, isArabic)
        views.setTextViewText(R.id.tv_hero_prayer, heroPrayerName)
        views.setTextViewText(R.id.tv_hero_diff, heroDiff)

        // 5 Prayers setup
        data class ColIds(
            val colId: Int,
            val badgeId: Int,
            val nameId: Int,
            val timeId: Int,
            val prayerType: PrayerType,
            val timeMillis: Long
        )

        val prayerColumns = listOf(
            ColIds(R.id.col_fajr, R.id.badge_fajr, R.id.tv_name_fajr, R.id.tv_time_fajr, PrayerType.FAJR, schedule.fajr.timestampMillis),
            ColIds(R.id.col_dhuhr, R.id.badge_dhuhr, R.id.tv_name_dhuhr, R.id.tv_time_dhuhr, PrayerType.DHUHR, schedule.dhuhr.timestampMillis),
            ColIds(R.id.col_asr, R.id.badge_asr, R.id.tv_name_asr, R.id.tv_time_asr, PrayerType.ASR, schedule.asr.timestampMillis),
            ColIds(R.id.col_maghrib, R.id.badge_maghrib, R.id.tv_name_maghrib, R.id.tv_time_maghrib, PrayerType.MAGHRIB, schedule.maghrib.timestampMillis),
            ColIds(R.id.col_isha, R.id.badge_isha, R.id.tv_name_isha, R.id.tv_time_isha, PrayerType.ISHA, schedule.isha.timestampMillis)
        )

        for (col in prayerColumns) {
            views.setTextViewText(col.nameId, AppStrings.getPrayerName(col.prayerType, lang))
            views.setTextViewText(col.timeId, formatTime12h(col.timeMillis, tz, isArabic))

            if (col.prayerType == currentType) {
                // Active column
                views.setTextViewText(col.badgeId, if (isArabic) "الان" else "Now")
                views.setInt(col.badgeId, "setBackgroundResource", R.drawable.bg_badge_cyan)
                views.setViewVisibility(col.badgeId, View.VISIBLE)
                views.setInt(col.colId, "setBackgroundResource", R.drawable.bg_prayer_col_active)
                views.setTextColor(col.nameId, Color.WHITE)
                views.setTextColor(col.timeId, Color.WHITE)
            } else if (col.prayerType == nextType) {
                // Next prayer column
                views.setTextViewText(col.badgeId, if (isArabic) "لاحقا" else "Next")
                views.setInt(col.badgeId, "setBackgroundResource", R.drawable.bg_badge_cyan)
                views.setViewVisibility(col.badgeId, View.VISIBLE)
                views.setInt(col.colId, "setBackgroundResource", R.drawable.bg_prayer_col_inactive)
                views.setTextColor(col.nameId, Color.parseColor("#334155"))
                views.setTextColor(col.timeId, Color.parseColor("#1E293B"))
            } else {
                // Normal column
                views.setTextViewText(col.badgeId, "")
                views.setInt(col.badgeId, "setBackgroundColor", Color.TRANSPARENT)
                views.setViewVisibility(col.badgeId, View.VISIBLE)
                views.setInt(col.colId, "setBackgroundResource", R.drawable.bg_prayer_col_inactive)
                views.setTextColor(col.nameId, Color.parseColor("#334155"))
                views.setTextColor(col.timeId, Color.parseColor("#1E293B"))
            }
        }

        // Bottom dates
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val dayName = HijriCalendarHelper.getDayOfWeekName(dayOfWeek, lang.code)
        val hijri = HijriCalendarHelper.fromGregorian(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            prefs.getHijriAdjustment()
        )
        val monthName = HijriCalendarHelper.getMonthName(hijri.month, lang.code)
        val hijriText = if (isArabic) {
            "$dayName ${toArabicDigits(hijri.day.toString())} $monthName ${toArabicDigits(hijri.year.toString())}"
        } else {
            "$dayName ${hijri.day} $monthName ${hijri.year}"
        }
        views.setTextViewText(R.id.tv_hijri_date, hijriText)

        val gregFormat = SimpleDateFormat("d-M-yyyy", Locale.US).apply { timeZone = tz }
        val rawGreg = gregFormat.format(cal.time)
        val gregText = if (isArabic) toArabicDigits(rawGreg) else rawGreg
        views.setTextViewText(R.id.tv_gregorian_date, gregText)
    }

    /**
     * Binds the custom notification expanded layout (R.layout.notification_custom_prayer)
     */
    fun bindNotificationViews(context: Context, views: RemoteViews, prefs: AppPreferences) {
        val lang = prefs.getLanguage()
        val isArabic = lang == AppLanguage.ARABIC
        val tz = prefs.getTimezone()
        val cal = Calendar.getInstance(tz)

        val schedule = PrayerTimesCalculator.calculate(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH),
            latitude = prefs.getLatitude(),
            longitude = prefs.getLongitude(),
            timezone = tz,
            dstSetting = prefs.getDstSetting(),
            method = prefs.getCalculationMethod(),
            madhab = prefs.getMadhab(),
            customFajrAngle = prefs.getCustomFajrAngle(),
            customIshaAngle = prefs.getCustomIshaAngle()
        )

        val now = System.currentTimeMillis()
        val currentType = schedule.getCurrentPrayer(now)
        val currentEntry = schedule.getEntry(currentType)
        val nextPrayer = schedule.getNextPrayer(now)
        val nextType = nextPrayer.type

        views.setTextViewText(R.id.notif_city_name, prefs.getCityName())

        val heroPrayerName = AppStrings.getPrayerName(currentType, lang)
        val heroDiff = calculateDiffString(now, currentEntry.timestampMillis, isArabic)
        views.setTextViewText(R.id.notif_hero_prayer, heroPrayerName)
        views.setTextViewText(R.id.notif_hero_diff, heroDiff)

        data class ColNotifIds(
            val colId: Int,
            val badgeId: Int,
            val nameId: Int,
            val timeId: Int,
            val prayerType: PrayerType,
            val timeMillis: Long
        )

        val columns = listOf(
            ColNotifIds(R.id.notif_col_fajr, R.id.notif_badge_fajr, R.id.notif_name_fajr, R.id.notif_time_fajr, PrayerType.FAJR, schedule.fajr.timestampMillis),
            ColNotifIds(R.id.notif_col_dhuhr, R.id.notif_badge_dhuhr, R.id.notif_name_dhuhr, R.id.notif_time_dhuhr, PrayerType.DHUHR, schedule.dhuhr.timestampMillis),
            ColNotifIds(R.id.notif_col_asr, R.id.notif_badge_asr, R.id.notif_name_asr, R.id.notif_time_asr, PrayerType.ASR, schedule.asr.timestampMillis),
            ColNotifIds(R.id.notif_col_maghrib, R.id.notif_badge_maghrib, R.id.notif_name_maghrib, R.id.notif_time_maghrib, PrayerType.MAGHRIB, schedule.maghrib.timestampMillis),
            ColNotifIds(R.id.notif_col_isha, R.id.notif_badge_isha, R.id.notif_name_isha, R.id.notif_time_isha, PrayerType.ISHA, schedule.isha.timestampMillis)
        )

        for (col in columns) {
            views.setTextViewText(col.nameId, AppStrings.getPrayerName(col.prayerType, lang))
            views.setTextViewText(col.timeId, formatTime12h(col.timeMillis, tz, isArabic))

            if (col.prayerType == currentType) {
                views.setTextViewText(col.badgeId, if (isArabic) "الان" else "Now")
                views.setInt(col.badgeId, "setBackgroundResource", R.drawable.bg_badge_cyan)
                views.setViewVisibility(col.badgeId, View.VISIBLE)
                views.setInt(col.colId, "setBackgroundResource", R.drawable.bg_prayer_col_active)
                views.setTextColor(col.nameId, Color.WHITE)
                views.setTextColor(col.timeId, Color.WHITE)
            } else if (col.prayerType == nextType) {
                views.setTextViewText(col.badgeId, if (isArabic) "لاحقا" else "Next")
                views.setInt(col.badgeId, "setBackgroundResource", R.drawable.bg_badge_cyan)
                views.setViewVisibility(col.badgeId, View.VISIBLE)
                views.setInt(col.colId, "setBackgroundResource", R.drawable.bg_prayer_col_inactive)
                views.setTextColor(col.nameId, Color.parseColor("#334155"))
                views.setTextColor(col.timeId, Color.parseColor("#1E293B"))
            } else {
                views.setTextViewText(col.badgeId, "")
                views.setInt(col.badgeId, "setBackgroundColor", Color.TRANSPARENT)
                views.setViewVisibility(col.badgeId, View.VISIBLE)
                views.setInt(col.colId, "setBackgroundResource", R.drawable.bg_prayer_col_inactive)
                views.setTextColor(col.nameId, Color.parseColor("#334155"))
                views.setTextColor(col.timeId, Color.parseColor("#1E293B"))
            }
        }

        // Bottom dates
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val dayName = HijriCalendarHelper.getDayOfWeekName(dayOfWeek, lang.code)
        val hijri = HijriCalendarHelper.fromGregorian(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            prefs.getHijriAdjustment()
        )
        val monthName = HijriCalendarHelper.getMonthName(hijri.month, lang.code)
        val hijriText = if (isArabic) {
            "$dayName ${toArabicDigits(hijri.day.toString())} $monthName ${toArabicDigits(hijri.year.toString())}"
        } else {
            "$dayName ${hijri.day} $monthName ${hijri.year}"
        }
        views.setTextViewText(R.id.notif_hijri_date, hijriText)

        val gregFormat = SimpleDateFormat("d-M-yyyy", Locale.US).apply { timeZone = tz }
        val rawGreg = gregFormat.format(cal.time)
        val gregText = if (isArabic) toArabicDigits(rawGreg) else rawGreg
        views.setTextViewText(R.id.notif_gregorian_date, gregText)
    }

    /**
     * Binds the collapsed notification layout (R.layout.notification_custom_prayer_collapsed)
     */
    fun bindNotificationCollapsedViews(context: Context, views: RemoteViews, prefs: AppPreferences) {
        val lang = prefs.getLanguage()
        val isArabic = lang == AppLanguage.ARABIC
        val tz = prefs.getTimezone()
        val cal = Calendar.getInstance(tz)

        val schedule = PrayerTimesCalculator.calculate(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH),
            latitude = prefs.getLatitude(),
            longitude = prefs.getLongitude(),
            timezone = tz,
            dstSetting = prefs.getDstSetting(),
            method = prefs.getCalculationMethod(),
            madhab = prefs.getMadhab(),
            customFajrAngle = prefs.getCustomFajrAngle(),
            customIshaAngle = prefs.getCustomIshaAngle()
        )

        val now = System.currentTimeMillis()
        val currentType = schedule.getCurrentPrayer(now)
        val currentEntry = schedule.getEntry(currentType)
        val heroPrayerName = AppStrings.getPrayerName(currentType, lang)
        val heroDiff = calculateDiffString(now, currentEntry.timestampMillis, isArabic)

        views.setTextViewText(R.id.notif_collapsed_prayer, heroPrayerName)
        views.setTextViewText(R.id.notif_collapsed_diff, heroDiff)
        views.setTextViewText(R.id.notif_collapsed_city, prefs.getCityName())
    }
}
