package com.example.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppPreferences
import com.example.engine.HijriCalendarHelper
import com.example.engine.PrayerDaySchedule
import com.example.engine.PrayerTimesCalculator
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object PrayerNotificationHelper {

    const val CHANNEL_PERSISTENT = "prayer_persistent_bar"
    const val CHANNEL_ALERTS = "prayer_alerts_channel"
    const val CHANNEL_ADHAN = "prayer_adhan_channel"
    const val CHANNEL_MUSAHARATI = "prayer_musaharati_channel"

    const val NOTIFICATION_ID_PERSISTENT = 1001
    const val NOTIFICATION_ID_ALERT = 2001
    const val NOTIFICATION_ID_ADHAN = 3001
    const val NOTIFICATION_ID_MUSAHARATI = 4001

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val persistentChannel = NotificationChannel(
                CHANNEL_PERSISTENT,
                "شريط مواقيت الصلاة / Prayer Times Bar",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "يعرض مواقيت الصلاة والوقت المتبقي بشكل مستمر / Persistent prayer times"
                setShowBadge(false)
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "تنبيهات الصلاة / Prayer Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات الصلوات المخصصة / Custom prayer alerts"
                enableVibration(true)
            }

            val adhanChannel = NotificationChannel(
                CHANNEL_ADHAN,
                "الأذان / Adhan Broadcast",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات الأذان عند دخول وقت الصلاة / Adhan notifications"
                enableVibration(true)
            }

            val musaharatiChannel = NotificationChannel(
                CHANNEL_MUSAHARATI,
                "المسحراتي / Musaharati",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيه السحور والمسحراتي في رمضان / Musaharati Suhoor alerts"
                enableVibration(true)
            }

            manager.createNotificationChannel(persistentChannel)
            manager.createNotificationChannel(alertsChannel)
            manager.createNotificationChannel(adhanChannel)
            manager.createNotificationChannel(musaharatiChannel)
        }
    }

    fun buildPersistentNotification(context: Context): Notification {
        createNotificationChannels(context)
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

        val currentTime = System.currentTimeMillis()
        val nextPrayer = schedule.getNextPrayer(currentTime)
        val remainingSec = schedule.getRemainingSecondsToNext(currentTime)
        val formattedRemaining = PrayerDaySchedule.formatRemaining(remainingSec)
        val nextName = AppStrings.getPrayerName(nextPrayer.type, lang)

        val hijri = HijriCalendarHelper.fromGregorian(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        val hijriFormatted = hijri.format(lang.code)

        val gregFormat = SimpleDateFormat("dd MMMM yyyy", when (lang) {
            AppLanguage.ARABIC -> Locale("ar")
            AppLanguage.FRENCH -> Locale.FRENCH
            else -> Locale.ENGLISH
        })
        val gregFormatted = gregFormat.format(cal.time)

        // Notification intents
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            10,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openSettingsIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("OPEN_TAB", "ADHAN")
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val settingsPendingIntent = PendingIntent.getActivity(
            context,
            11,
            openSettingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleAlertsIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TOGGLE_ALERTS
        }
        val toggleAlertsPendingIntent = PendingIntent.getBroadcast(
            context,
            12,
            toggleAlertsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "${AppStrings.nextPrayer(lang)}: $nextName"
        val subtitle = "${AppStrings.remaining(lang)}: $formattedRemaining"

        val bigText = buildString {
            append("━━━━━━━━━━━━\n")
            append("🕌 ${if (lang == AppLanguage.ARABIC) "مواقيت الصلاة" else "Prayer Times"}\n")
            append("━━━━━━━━━━━━\n")
            append("${AppStrings.getPrayerName(com.example.engine.PrayerType.FAJR, lang).padEnd(10)} ${schedule.fajr.formattedTime24}\n")
            append("${AppStrings.getPrayerName(com.example.engine.PrayerType.SUNRISE, lang).padEnd(10)} ${schedule.sunrise.formattedTime24}\n")
            append("${AppStrings.getPrayerName(com.example.engine.PrayerType.DHUHR, lang).padEnd(10)} ${schedule.dhuhr.formattedTime24}\n")
            append("${AppStrings.getPrayerName(com.example.engine.PrayerType.ASR, lang).padEnd(10)} ${schedule.asr.formattedTime24}\n")
            append("${AppStrings.getPrayerName(com.example.engine.PrayerType.MAGHRIB, lang).padEnd(10)} ${schedule.maghrib.formattedTime24}\n")
            append("${AppStrings.getPrayerName(com.example.engine.PrayerType.ISHA, lang).padEnd(10)} ${schedule.isha.formattedTime24}\n\n")
            append("${AppStrings.nextPrayer(lang)}:\n$nextName — ${AppStrings.remaining(lang)} $formattedRemaining\n\n")
            append("${if (lang == AppLanguage.ARABIC) "الهجري" else "Hijri"}: $hijriFormatted\n")
            append("${if (lang == AppLanguage.ARABIC) "الميلادي" else "Gregorian"}: $gregFormatted")
        }

        val openLabel = if (lang == AppLanguage.ARABIC) "فتح التطبيق" else "Open App"
        val adhanLabel = if (lang == AppLanguage.ARABIC) "إعدادات الأذان" else "Adhan"
        val alertLabel = if (lang == AppLanguage.ARABIC) "التنبيهات" else "Alerts"

        return NotificationCompat.Builder(context, CHANNEL_PERSISTENT)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setSubText(hijriFormatted)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_menu_today, openLabel, openPendingIntent)
            .addAction(android.R.drawable.ic_lock_silent_mode_off, alertLabel, toggleAlertsPendingIntent)
            .addAction(android.R.drawable.ic_menu_preferences, adhanLabel, settingsPendingIntent)
            .build()
    }
}
