package com.example.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppPreferences
import com.example.engine.PrayerBannerHelper

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
}
