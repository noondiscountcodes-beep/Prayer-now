package com.example.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppPreferences
import com.example.engine.HijriCalendarHelper
import com.example.engine.PrayerBannerHelper
import com.example.engine.PrayerDaySchedule
import com.example.engine.PrayerTimesCalculator
import com.example.localization.AppStrings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NextPrayerWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = AppPreferences(context)
        val lang = prefs.getLanguage()
        val cal = Calendar.getInstance(prefs.getTimezone())

        val schedule = PrayerTimesCalculator.calculate(
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

        val now = System.currentTimeMillis()
        val next = schedule.getNextPrayer(now)
        val remainingSec = schedule.getRemainingSecondsToNext(now)
        val remainingFormatted = PrayerDaySchedule.formatRemaining(remainingSec)
        val nextName = AppStrings.getPrayerName(next.type, lang)

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_next_prayer)
            views.setTextViewText(R.id.tv_next_prayer_title, AppStrings.nextPrayer(lang))
            views.setTextViewText(R.id.tv_next_prayer_name, nextName)
            views.setTextViewText(R.id.tv_remaining_countdown, "${AppStrings.remaining(lang)}: $remainingFormatted")
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}

class PrayerTimesWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = AppPreferences(context)
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_prayer_times)
            PrayerBannerHelper.bindWidgetViews(context, views, prefs)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}

class IslamicDateWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = AppPreferences(context)
        val lang = prefs.getLanguage()
        val cal = Calendar.getInstance(prefs.getTimezone())

        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val dayName = HijriCalendarHelper.getDayOfWeekName(dayOfWeek, lang.code)

        val hijri = HijriCalendarHelper.fromGregorian(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        val hijriFormatted = hijri.format(lang.code)

        val gregFormat = SimpleDateFormat("dd MMMM yyyy", when (lang) {
            com.example.localization.AppLanguage.ARABIC -> Locale("ar")
            com.example.localization.AppLanguage.FRENCH -> Locale.FRENCH
            else -> Locale.ENGLISH
        })
        val gregFormatted = gregFormat.format(cal.time)

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 2, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_islamic_date)
            views.setTextViewText(R.id.tv_day_name, dayName)
            views.setTextViewText(R.id.tv_hijri_date, hijriFormatted)
            views.setTextViewText(R.id.tv_gregorian_date, gregFormatted)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}

class RamadanWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = AppPreferences(context)
        val lang = prefs.getLanguage()
        val cal = Calendar.getInstance(prefs.getTimezone())

        val schedule = PrayerTimesCalculator.calculate(
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

        val now = System.currentTimeMillis()
        val suhoorLabel = if (lang.code == "ar") "السحور / الإمساك" else "Suhoor"
        val iftarLabel = if (lang.code == "ar") "الإفطار" else "Iftar"

        val isBeforeIftar = now < schedule.maghrib.timestampMillis
        val diffSec = if (isBeforeIftar) {
            (schedule.maghrib.timestampMillis - now) / 1000
        } else {
            (schedule.fajr.timestampMillis + 24 * 3600 * 1000L - now) / 1000
        }
        val countdown = PrayerDaySchedule.formatRemaining(diffSec)

        val targetLabel = if (isBeforeIftar) {
            if (lang.code == "ar") "متبقي للإفطار: $countdown" else "Time to Iftar: $countdown"
        } else {
            if (lang.code == "ar") "متبقي للسحور: $countdown" else "Time to Suhoor: $countdown"
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 3, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_ramadan)
            views.setTextViewText(R.id.tv_suhoor_time, "$suhoorLabel\n${schedule.fajr.formattedTime24}")
            views.setTextViewText(R.id.tv_iftar_time, "$iftarLabel\n${schedule.maghrib.formattedTime24}")
            views.setTextViewText(R.id.tv_ramadan_countdown, targetLabel)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}
