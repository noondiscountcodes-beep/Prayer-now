package com.example.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AlertItem
import com.example.data.AlertsRepository
import com.example.data.AppPreferences
import com.example.engine.PrayerType
import com.example.localization.AppStrings
import com.example.media.MediaHelper

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        PrayerNotificationHelper.createNotificationChannels(context)
        val prefs = AppPreferences(context)
        val lang = prefs.getLanguage()

        when (intent.action) {
            ACTION_PRAYER_ALARM -> {
                val prayerNameStr = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "FAJR"
                val prayerType = try { PrayerType.valueOf(prayerNameStr) } catch (e: Exception) { PrayerType.FAJR }
                val localizedName = AppStrings.getPrayerName(prayerType, lang)

                val adhanConfig = prefs.getAdhanConfig(prayerType)

                val openIntent = Intent(context, MainActivity::class.java).apply {
                    putExtra("TRIGGER_ADHAN_VIDEO", prayerType.name)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    prayerType.ordinal,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val title = "${if (lang.code == "ar") "حان الآن موعد أذان" else "Adhan Time"}: $localizedName"
                val message = if (lang.code == "ar") {
                    "الله أكبر الله أكبر — حان وقت صلاة $localizedName"
                } else if (lang.code == "fr") {
                    "C'est l'heure de la prière de $localizedName"
                } else {
                    "It is now time for $localizedName prayer"
                }

                val builder = NotificationCompat.Builder(context, PrayerNotificationHelper.CHANNEL_ADHAN)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setVibrate(longArrayOf(0, 500, 200, 500))

                if (adhanConfig.uriString != null && MediaHelper.isUriAvailable(context, adhanConfig.uriString)) {
                    val playLabel = if (lang.code == "ar") "تشغيل فيديو الأذان" else "Play Adhan Video"
                    builder.addAction(android.R.drawable.ic_media_play, playLabel, pendingIntent)
                }

                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(PrayerNotificationHelper.NOTIFICATION_ID_ADHAN + prayerType.ordinal, builder.build())

                // Play preview sound/tone if enabled
                MediaHelper.playAudioPreview(context, adhanConfig.uriString)

                // Reschedule next prayer
                AlarmScheduler.scheduleAll(context)
            }

            ACTION_CUSTOM_ALERT -> {
                val alertId = intent.getStringExtra(EXTRA_ALERT_ID) ?: ""
                val repo = AlertsRepository(context)
                val alert = repo.getAllAlerts().find { it.id == alertId }
                if (alert != null && alert.isEnabled) {
                    val targetName = alert.getPrayerDisplayName(lang)
                    val title = if (lang.code == "ar") "تنبيه اقتراب الصلاة" else "Prayer Reminder"
                    val message = if (lang.code == "ar") {
                        "متبقي ${alert.minutesBefore} دقيقة على صلاة $targetName"
                    } else if (lang.code == "fr") {
                        "${alert.minutesBefore} min avant la prière de $targetName"
                    } else {
                        "${alert.minutesBefore} minutes before $targetName prayer"
                    }

                    val openIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        alert.hashCode(),
                        openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val builder = NotificationCompat.Builder(context, PrayerNotificationHelper.CHANNEL_ALERTS)
                        .setSmallIcon(android.R.drawable.ic_dialog_alert)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .setVibrate(longArrayOf(0, 400, 200, 400))

                    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(PrayerNotificationHelper.NOTIFICATION_ID_ALERT + alert.hashCode(), builder.build())

                    // Play audio tone
                    MediaHelper.playAudioPreview(context, alert.soundUri)
                }

                AlarmScheduler.scheduleAll(context)
            }

            ACTION_MUSAHARATI -> {
                val config = prefs.getMusaharatiConfig()
                val title = if (lang.code == "ar") "🌙 وقت السحور — المسحراتي" else "🌙 Suhoor Reminder — Musaharati"
                val message = if (lang.code == "ar") {
                    "اصحى يا نايم وحّد الدايم.. حان موعد السحور المبارك"
                } else if (lang.code == "fr") {
                    "C'est l'heure bénie du Souhour"
                } else {
                    "Blessed Suhoor time has arrived"
                }

                val openIntent = Intent(context, MainActivity::class.java).apply {
                    putExtra("TRIGGER_MUSAHARATI_VIDEO", true)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    99,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val builder = NotificationCompat.Builder(context, PrayerNotificationHelper.CHANNEL_MUSAHARATI)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setVibrate(longArrayOf(0, 500, 200, 500))

                if (config.uriString != null && MediaHelper.isUriAvailable(context, config.uriString)) {
                    val playLabel = if (lang.code == "ar") "تشغيل فيديو المسحراتي" else "Play Video"
                    builder.addAction(android.R.drawable.ic_media_play, playLabel, pendingIntent)
                }

                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(PrayerNotificationHelper.NOTIFICATION_ID_MUSAHARATI, builder.build())

                MediaHelper.playAudioPreview(context, config.uriString)

                AlarmScheduler.scheduleAll(context)
            }

            ACTION_TOGGLE_ALERTS -> {
                // Toggles alerts
                val repo = AlertsRepository(context)
                val alerts = repo.getAllAlerts()
                val anyEnabled = alerts.any { it.isEnabled }
                alerts.forEach { repo.toggleAlert(it.id, !anyEnabled) }
                AlarmScheduler.scheduleAll(context)
                // Refresh persistent notification
                val notif = PrayerNotificationHelper.buildPersistentNotification(context)
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(PrayerNotificationHelper.NOTIFICATION_ID_PERSISTENT, notif)
            }
        }
    }

    companion object {
        const val ACTION_PRAYER_ALARM = "com.example.ACTION_PRAYER_ALARM"
        const val ACTION_CUSTOM_ALERT = "com.example.ACTION_CUSTOM_ALERT"
        const val ACTION_MUSAHARATI = "com.example.ACTION_MUSAHARATI"
        const val ACTION_TOGGLE_ALERTS = "com.example.ACTION_TOGGLE_ALERTS"

        const val EXTRA_PRAYER_NAME = "EXTRA_PRAYER_NAME"
        const val EXTRA_ALERT_ID = "EXTRA_ALERT_ID"
    }
}
