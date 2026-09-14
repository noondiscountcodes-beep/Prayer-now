package com.example.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppPreferences
import com.example.engine.PrayerBannerHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object PrayerNotificationHelper {

    const val CHANNEL_PERSISTENT = "prayer_persistent_bar"
    const val CHANNEL_ALERTS = "prayer_alerts_channel"
    const val CHANNEL_ADHAN = "prayer_adhan_channel"
    const val CHANNEL_MUSAHARATI = "prayer_musaharati_channel"
    const val CHANNEL_IFTAR_CANNON = "prayer_iftar_cannon_channel"
    const val CHANNEL_SALAWAT = "prayer_salawat_channel"
    const val CHANNEL_SALAWAT_PERSISTENT = "salawat_persistent_countdown_channel"

    const val NOTIFICATION_ID_PERSISTENT = 1001
    const val NOTIFICATION_ID_ALERT = 2001
    const val NOTIFICATION_ID_ADHAN = 3001
    const val NOTIFICATION_ID_MUSAHARATI = 4001
    const val NOTIFICATION_ID_IFTAR_CANNON = 5001
    const val NOTIFICATION_ID_SALAWAT = 6001
    const val NOTIFICATION_ID_SALAWAT_PERSISTENT = 6002

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

            val iftarCannonChannel = NotificationChannel(
                CHANNEL_IFTAR_CANNON,
                "مدفع الإفطار / Iftar Cannon",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيه وشاشة مدفع الإفطار في رمضان / Ramadan Iftar Cannon alerts"
                enableVibration(true)
            }

            val salawatChannel = NotificationChannel(
                CHANNEL_SALAWAT,
                "الصلاة على النبي / Salawat on Prophet",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات وأصوات التذكير بالصلاة على النبي ﷺ / Salawat reminders"
                enableVibration(true)
            }

            val salawatPersistentChannel = NotificationChannel(
                CHANNEL_SALAWAT_PERSISTENT,
                "شعار الصلاة على النبي الدائم / Persistent Salawat Reminder",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعار دائم يعرض العد التنازلي للتذكير القادم بالصلاة على النبي ﷺ / Persistent countdown for Salawat"
                setShowBadge(false)
            }

            manager.createNotificationChannel(persistentChannel)
            manager.createNotificationChannel(alertsChannel)
            manager.createNotificationChannel(adhanChannel)
            manager.createNotificationChannel(musaharatiChannel)
            manager.createNotificationChannel(iftarCannonChannel)
            manager.createNotificationChannel(salawatChannel)
            manager.createNotificationChannel(salawatPersistentChannel)
        }
    }

    fun buildPersistentNotification(context: Context): Notification {
        createNotificationChannels(context)
        val prefs = AppPreferences(context)
        val lang = prefs.getLanguage()

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            10,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remoteViewsExpanded = RemoteViews(context.packageName, R.layout.notification_custom_prayer)
        PrayerBannerHelper.bindNotificationViews(context, remoteViewsExpanded, prefs)
        remoteViewsExpanded.setOnClickPendingIntent(R.id.notification_root, openPendingIntent)

        val remoteViewsCollapsed = RemoteViews(context.packageName, R.layout.notification_custom_prayer_collapsed)
        PrayerBannerHelper.bindNotificationCollapsedViews(context, remoteViewsCollapsed, prefs)
        remoteViewsCollapsed.setOnClickPendingIntent(R.id.notification_collapsed_root, openPendingIntent)

        return NotificationCompat.Builder(context, CHANNEL_PERSISTENT)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setCustomContentView(remoteViewsCollapsed)
            .setCustomBigContentView(remoteViewsExpanded)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun buildSalawatPersistentNotification(context: Context, nextTriggerTime: Long): Notification {
        createNotificationChannels(context)
        val prefs = AppPreferences(context)
        val isArabic = prefs.getLanguage().code == "ar"

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_TAB", "SALAWAT")
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            8810,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerNowIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SALAWAT_REMINDER
        }
        val triggerNowPendingIntent = PendingIntent.getBroadcast(
            context,
            8811,
            triggerNowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val salawatRepo = com.example.data.SalawatPreferencesRepository(context)
        val config = salawatRepo.getConfig()
        val effectiveTrigger = if (nextTriggerTime > System.currentTimeMillis()) {
            nextTriggerTime
        } else {
            salawatRepo.calculateNextTriggerTime(config)
        }

        val now = System.currentTimeMillis()
        val diffMillis = (effectiveTrigger - now).coerceAtLeast(0L)
        val remainingSec = diffMillis / 1000L
        val hours = remainingSec / 3600
        val mins = (remainingSec % 3600) / 60
        val secs = remainingSec % 60
        val countdownStr = if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }

        val cal = Calendar.getInstance().apply { timeInMillis = effectiveTrigger }
        val timeFormat = SimpleDateFormat("hh:mm a", if (isArabic) Locale("ar") else Locale.US)
        val targetClockTime = timeFormat.format(cal.time)

        val audioDesc = if (config.audioSelectionMode == "SPECIFIC" && !config.selectedAudioFileName.isNullOrEmpty()) {
            if (isArabic) "الصوت: ${config.selectedAudioFileName}" else "Audio: ${config.selectedAudioFileName}"
        } else {
            if (isArabic) "الصوت: عشوائي من ملف الـ ZIP" else "Audio: Random from ZIP"
        }

        val chronometerBase = SystemClock.elapsedRealtime() + diffMillis

        val remoteViewsExpanded = RemoteViews(context.packageName, R.layout.notification_salawat_persistent).apply {
            setTextViewText(R.id.notif_salawat_title, if (isArabic) "ﷺ الصلاة على النبي" else "ﷺ Salawat on Prophet Muhammad")
            setTextViewText(R.id.notif_salawat_hadith_badge, if (isArabic) "تذكير دائم" else "Ongoing")
            setTextViewText(R.id.notif_salawat_countdown_label, if (isArabic) "الوقت المتبقي للتذكير القادم:" else "Remaining until next reminder:")
            setTextViewText(R.id.notif_salawat_target_time, if (isArabic) "الموعد القادم: $targetClockTime" else "Next alert: $targetClockTime")
            setTextViewText(R.id.notif_salawat_audio_info, audioDesc)
            setTextViewText(R.id.notif_salawat_btn_pray, if (isArabic) "صلِّ الآن ﷺ" else "Pray Now ﷺ")

            setChronometer(R.id.notif_salawat_chronometer, chronometerBase, "%s", true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setChronometerCountDown(R.id.notif_salawat_chronometer, true)
            }
            setOnClickPendingIntent(R.id.notif_salawat_root, openPendingIntent)
            setOnClickPendingIntent(R.id.notif_salawat_btn_pray, triggerNowPendingIntent)
        }

        val remoteViewsCollapsed = RemoteViews(context.packageName, R.layout.notification_salawat_persistent_collapsed).apply {
            setTextViewText(R.id.notif_salawat_collapsed_title, if (isArabic) "ﷺ الصلاة على النبي" else "ﷺ Salawat")
            setTextViewText(R.id.notif_salawat_collapsed_time, if (isArabic) "عند $targetClockTime" else "At $targetClockTime")
            setTextViewText(R.id.notif_salawat_collapsed_label, if (isArabic) "يتبقى للتذكير القادم" else "Remaining")

            setChronometer(R.id.notif_salawat_collapsed_chronometer, chronometerBase, "%s", true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setChronometerCountDown(R.id.notif_salawat_collapsed_chronometer, true)
            }
            setOnClickPendingIntent(R.id.notif_salawat_collapsed_root, openPendingIntent)
        }

        val title = if (isArabic) "ﷺ الصلاة على النبي" else "ﷺ Salawat on Prophet Muhammad"
        val content = if (isArabic) "الوقت المتبقي: $countdownStr (الموعد: $targetClockTime)" else "Next reminder in $countdownStr (at $targetClockTime)"

        val builder = NotificationCompat.Builder(context, CHANNEL_SALAWAT_PERSISTENT)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setCustomContentView(remoteViewsCollapsed)
            .setCustomBigContentView(remoteViewsExpanded)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContentTitle(title)
            .setContentText(content)
            .setSubText(if (isArabic) "تذكير مستمر" else "Ongoing Reminder")
            .setWhen(effectiveTrigger)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openPendingIntent)
            .addAction(
                android.R.drawable.ic_media_play,
                if (isArabic) "صلِّ الآن ﷺ" else "Pray Now ﷺ",
                triggerNowPendingIntent
            )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setChronometerCountDown(true)
        }

        return builder.build()
    }

    fun updateSalawatPersistentNotification(context: Context, nextTriggerTime: Long) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val notification = buildSalawatPersistentNotification(context, nextTriggerTime)
            manager.notify(NOTIFICATION_ID_SALAWAT_PERSISTENT, notification)
        } catch (e: Exception) {
            android.util.Log.e("PrayerNotification", "Error updating Salawat persistent notification: ${e.message}")
        }
    }

    fun cancelSalawatPersistentNotification(context: Context) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            manager.cancel(NOTIFICATION_ID_SALAWAT_PERSISTENT)
        } catch (e: Exception) {
            android.util.Log.e("PrayerNotification", "Error cancelling Salawat persistent notification: ${e.message}")
        }
    }
}
