package com.example.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AlertItem
import com.example.data.AlertsRepository
import com.example.data.AppPreferences
import com.example.engine.PrayerType
import com.example.localization.AppStrings
import com.example.media.MediaHelper
import com.example.widgets.WidgetSyncHelper

class AlarmReceiver : BroadcastReceiver() {

    private fun acquireWakeLock(context: Context, tag: String, timeoutMs: Long = 60_000L) {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            @Suppress("DEPRECATION")
            val wakeLock = powerManager?.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                PowerManager.ON_AFTER_RELEASE,
                tag
            )
            wakeLock?.acquire(timeoutMs)
        } catch (e: Exception) {
            try {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val wakeLock = powerManager?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    tag
                )
                wakeLock?.acquire(timeoutMs)
            } catch (e2: Exception) {
                Log.e("AlarmReceiver", "Failed to acquire wake lock", e2)
            }
        }
    }

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

                val adhanRepo = com.example.data.AdhanPreferencesRepository(context)
                val duaConfig = adhanRepo.getDuaConfig(prayerType)
                val screenConfig = adhanRepo.getScreenConfig(prayerType)

                val openIntent = Intent(context, MainActivity::class.java).apply {
                    putExtra("TRIGGER_ADHAN_SCREEN", prayerType.name)
                    putExtra("TRIGGER_SCREEN_TYPE", "ADHAN")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
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
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setContentIntent(pendingIntent)
                    .setFullScreenIntent(pendingIntent, true)
                    .setAutoCancel(true)
                    .setVibrate(longArrayOf(0, 500, 200, 500))

                val screenLabel = if (lang.code == "ar") "فتح شاشة الأذان" else "Open Adhan Screen"
                builder.addAction(android.R.drawable.ic_menu_view, screenLabel, pendingIntent)

                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(PrayerNotificationHelper.NOTIFICATION_ID_ADHAN + prayerType.ordinal, builder.build())

                // Wake screen and automatically open Adhan screen
                acquireWakeLock(context, "MosqueClock:AdhanScreenWake", 90_000L)
                try {
                    context.startActivity(openIntent)
                } catch (e: Exception) {
                    try {
                        pendingIntent.send()
                    } catch (e2: Exception) {
                        Log.e("AlarmReceiver", "Failed to auto-launch Adhan screen activity", e2)
                    }
                }

                // Play exact sequence: Prayer Time -> Alert Sound (if enabled) -> Adhan immediately -> Du'aa
                AdhanSequencePlayer.playPrayerSequence(
                    context = context,
                    prayer = prayerType,
                    onAdhanFinished = {
                        // After Adhan completes: immediately launch Du'aa video directly!
                        val fallbackDuaUri = com.example.engine.PrayerType.entries.map { adhanRepo.getDuaConfig(it) }
                            .firstOrNull { it.isEnabled && !it.uriString.isNullOrBlank() }?.uriString

                        val targetDuaUri = if (duaConfig.isEnabled && !duaConfig.uriString.isNullOrBlank()) {
                            duaConfig.uriString
                        } else {
                            fallbackDuaUri
                        }

                        if (!targetDuaUri.isNullOrBlank()) {
                            val duaIntent = Intent(context, MainActivity::class.java).apply {
                                putExtra("TRIGGER_ADHAN_VIDEO", prayerType.name)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }
                            val duaPendingIntent = PendingIntent.getActivity(
                                context,
                                5000 + prayerType.ordinal,
                                duaIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )

                            acquireWakeLock(context, "MosqueClock:DuaVideoWake", 60_000L)
                            try {
                                context.startActivity(duaIntent)
                            } catch (e: Exception) {
                                try {
                                    duaPendingIntent.send()
                                } catch (e2: Exception) {
                                    Log.e("AlarmReceiver", "Failed to launch Dua video intent", e2)
                                }
                            }

                            val duaTitle = if (lang.code == "ar") "دعاء ما بعد الأذان" else "Post-Adhan Supplication"
                            val duaMsg = if (lang.code == "ar") "تشغيل فيديو دعاء صلاة $localizedName" else "Playing supplication video for $localizedName"
                            val duaBuilder = NotificationCompat.Builder(context, PrayerNotificationHelper.CHANNEL_ADHAN)
                                .setSmallIcon(android.R.drawable.ic_media_play)
                                .setContentTitle(duaTitle)
                                .setContentText(duaMsg)
                                .setPriority(NotificationCompat.PRIORITY_MAX)
                                .setCategory(NotificationCompat.CATEGORY_ALARM)
                                .setContentIntent(duaPendingIntent)
                                .setFullScreenIntent(duaPendingIntent, true)
                                .setAutoCancel(true)
                            manager.notify(PrayerNotificationHelper.NOTIFICATION_ID_ADHAN + 100 + prayerType.ordinal, duaBuilder.build())
                        }
                    }
                )

                // Reschedule next prayer and synchronize widgets + notification bar
                AlarmScheduler.scheduleAll(context)
                WidgetSyncHelper.syncAll(context)
            }

            ACTION_CUSTOM_ALERT -> {
                val alertId = intent.getStringExtra(EXTRA_ALERT_ID) ?: ""
                val prayerNameExtra = intent.getStringExtra(EXTRA_PRAYER_NAME)
                val repo = AlertsRepository(context)
                val alert = repo.getAllAlerts().find { it.id == alertId }

                // Resolve the exact prayer for which this alert was scheduled (e.g. DHUHR, ASR, MAGHRIB, etc.)
                val targetPrayerType = try {
                    if (!prayerNameExtra.isNullOrEmpty()) {
                        PrayerType.valueOf(prayerNameExtra)
                    } else if (alert != null && alert.targetPrayer != "ALL") {
                        PrayerType.valueOf(alert.targetPrayer)
                    } else {
                        PrayerType.FAJR
                    }
                } catch (e: Exception) {
                    PrayerType.FAJR
                }

                if (alert == null || alert.isEnabled) {
                    val minutesBefore = alert?.minutesBefore ?: 15
                    val actualPrayerName = AppStrings.getPrayerName(targetPrayerType, lang)
                    val title = if (lang.code == "ar") "تنبيه اقتراب موعد صلاة $actualPrayerName" else "Prayer Reminder: $actualPrayerName"
                    val message = if (lang.code == "ar") {
                        "يتبقى $minutesBefore دقيقة على أذان صلاة $actualPrayerName"
                    } else if (lang.code == "fr") {
                        "$minutesBefore min avant la prière de $actualPrayerName"
                    } else {
                        "$minutesBefore minutes before $actualPrayerName prayer"
                    }

                    val adhanRepo = com.example.data.AdhanPreferencesRepository(context)
                    val screenConfig = adhanRepo.getScreenConfig(targetPrayerType)

                    val openIntent = Intent(context, MainActivity::class.java).apply {
                        putExtra("TRIGGER_ADHAN_SCREEN", targetPrayerType.name)
                        putExtra("TRIGGER_SCREEN_TYPE", "ALERT")
                        putExtra("TRIGGER_ALERT_MINUTES", minutesBefore)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    }
                    val safeHash = ((alert?.id?.hashCode() ?: 0) and 0x7FFFFFFF) % 10000
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        30000 + (safeHash * 10) + targetPrayerType.ordinal,
                        openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val builder = NotificationCompat.Builder(context, PrayerNotificationHelper.CHANNEL_ALERTS)
                        .setSmallIcon(android.R.drawable.ic_dialog_alert)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setContentIntent(pendingIntent)
                        .setFullScreenIntent(pendingIntent, true)
                        .setAutoCancel(true)
                        .setVibrate(longArrayOf(0, 400, 200, 400))

                    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(PrayerNotificationHelper.NOTIFICATION_ID_ALERT + targetPrayerType.ordinal * 50 + safeHash % 50, builder.build())

                    // Play audio tone
                    MediaHelper.playAudioPreview(context, alert?.soundUri)

                    // Wake screen and automatically open Alert Screen
                    acquireWakeLock(context, "MosqueClock:AlertScreenWake", 60_000L)
                    try {
                        context.startActivity(openIntent)
                    } catch (e: Exception) {
                        try {
                            pendingIntent.send()
                        } catch (e2: Exception) {
                            Log.e("AlarmReceiver", "Failed to auto-launch Alert screen activity", e2)
                        }
                    }
                }

                AlarmScheduler.scheduleAll(context)
                WidgetSyncHelper.syncAll(context)
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

                val adhanRepo = com.example.data.AdhanPreferencesRepository(context)
                val screenConfig = adhanRepo.getScreenConfig(PrayerType.FAJR)

                val openIntent = Intent(context, MainActivity::class.java).apply {
                    putExtra("TRIGGER_ADHAN_SCREEN", "FAJR")
                    putExtra("TRIGGER_SCREEN_TYPE", "SUHOOR")
                    putExtra("TRIGGER_MUSAHARATI_VIDEO", true)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
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
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setContentIntent(pendingIntent)
                    .setFullScreenIntent(pendingIntent, true)
                    .setAutoCancel(true)
                    .setVibrate(longArrayOf(0, 500, 200, 500))

                if (config.uriString != null && MediaHelper.isUriAvailable(context, config.uriString)) {
                    val playLabel = if (lang.code == "ar") "تشغيل فيديو المسحراتي" else "Play Video"
                    builder.addAction(android.R.drawable.ic_media_play, playLabel, pendingIntent)
                }

                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(PrayerNotificationHelper.NOTIFICATION_ID_MUSAHARATI, builder.build())

                MediaHelper.playAudioPreview(context, config.uriString)

                // Wake screen and automatically open Suhoor Screen
                acquireWakeLock(context, "MosqueClock:SuhoorScreenWake")
                if (screenConfig.autoOpenOnSuhoor) {
                    try {
                        context.startActivity(openIntent)
                    } catch (e: Exception) {
                        Log.e("AlarmReceiver", "Failed to auto-launch Suhoor screen activity", e)
                    }
                }

                AlarmScheduler.scheduleAll(context)
                WidgetSyncHelper.syncAll(context)
            }

            ACTION_IFTAR_CANNON -> {
                val config = prefs.getIftarCannonConfig()
                val title = if (lang.code == "ar") "💥 مدفع الإفطار — رمضان مبارك" else "💥 Ramadan Iftar Cannon"
                val message = if (lang.code == "ar") {
                    "مدفع الإفطار.. اضْرِب! حان موعد إفطار الصائمين — صياماً مقبولاً وإفطاراً شهياً"
                } else if (lang.code == "fr") {
                    "Le canon de l'Iftar a retenti ! Bon appétit"
                } else {
                    "Iftar Cannon has fired! Blessed Iftar"
                }

                val adhanRepo = com.example.data.AdhanPreferencesRepository(context)
                val screenConfig = adhanRepo.getScreenConfig(PrayerType.MAGHRIB)

                val openIntent = Intent(context, MainActivity::class.java).apply {
                    putExtra("TRIGGER_ADHAN_SCREEN", "MAGHRIB")
                    putExtra("TRIGGER_SCREEN_TYPE", "IFTAR_CANNON")
                    putExtra("TRIGGER_IFTAR_CANNON_VIDEO", true)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    98,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val builder = NotificationCompat.Builder(context, PrayerNotificationHelper.CHANNEL_IFTAR_CANNON)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setContentIntent(pendingIntent)
                    .setFullScreenIntent(pendingIntent, true)
                    .setAutoCancel(true)
                    .setVibrate(longArrayOf(0, 700, 300, 700))

                if (config.uriString != null && MediaHelper.isUriAvailable(context, config.uriString)) {
                    val playLabel = if (lang.code == "ar") "تشغيل فيديو مدفع الإفطار" else "Play Video"
                    builder.addAction(android.R.drawable.ic_media_play, playLabel, pendingIntent)
                }

                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(PrayerNotificationHelper.NOTIFICATION_ID_IFTAR_CANNON, builder.build())

                MediaHelper.playAudioPreview(context, config.uriString)

                // Wake screen and automatically open Iftar Cannon Screen
                acquireWakeLock(context, "MosqueClock:IftarCannonScreenWake")
                if (screenConfig.autoOpenOnIftarCannon) {
                    try {
                        context.startActivity(openIntent)
                    } catch (e: Exception) {
                        Log.e("AlarmReceiver", "Failed to auto-launch Iftar Cannon screen activity", e)
                    }
                }

                AlarmScheduler.scheduleAll(context)
                WidgetSyncHelper.syncAll(context)
            }

            ACTION_TOGGLE_ALERTS -> {
                // Toggles alerts
                val repo = AlertsRepository(context)
                val alerts = repo.getAllAlerts()
                val anyEnabled = alerts.any { it.isEnabled }
                alerts.forEach { repo.toggleAlert(it.id, !anyEnabled) }
                AlarmScheduler.scheduleAll(context)
                WidgetSyncHelper.syncAll(context)
            }
        }
    }

    companion object {
        const val ACTION_PRAYER_ALARM = "com.example.ACTION_PRAYER_ALARM"
        const val ACTION_CUSTOM_ALERT = "com.example.ACTION_CUSTOM_ALERT"
        const val ACTION_MUSAHARATI = "com.example.ACTION_MUSAHARATI"
        const val ACTION_IFTAR_CANNON = "com.example.ACTION_IFTAR_CANNON"
        const val ACTION_TOGGLE_ALERTS = "com.example.ACTION_TOGGLE_ALERTS"

        const val EXTRA_PRAYER_NAME = "EXTRA_PRAYER_NAME"
        const val EXTRA_ALERT_ID = "EXTRA_ALERT_ID"
    }
}
