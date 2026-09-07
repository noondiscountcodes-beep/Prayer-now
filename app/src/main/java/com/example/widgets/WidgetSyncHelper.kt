package com.example.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.AppPreferences
import com.example.notifications.PrayerNotificationHelper

object WidgetSyncHelper {

    private const val TAG = "WidgetSyncHelper"

    /**
     * Synchronizes all active home screen widgets immediately.
     */
    fun updateAllWidgets(context: Context) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return

            // 1. Next Prayer Widget
            val nextComponent = ComponentName(context, NextPrayerWidget::class.java)
            val nextIds = appWidgetManager.getAppWidgetIds(nextComponent)
            if (nextIds.isNotEmpty()) {
                NextPrayerWidget().onUpdate(context, appWidgetManager, nextIds)
                val intent = Intent(context, NextPrayerWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, nextIds)
                }
                context.sendBroadcast(intent)
            }

            // 2. Prayer Times (Electronic Clock Banner) Widget
            val ptComponent = ComponentName(context, PrayerTimesWidget::class.java)
            val ptIds = appWidgetManager.getAppWidgetIds(ptComponent)
            if (ptIds.isNotEmpty()) {
                PrayerTimesWidget().onUpdate(context, appWidgetManager, ptIds)
                val intent = Intent(context, PrayerTimesWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ptIds)
                }
                context.sendBroadcast(intent)
            }

            // 3. Islamic Date Widget
            val dateComponent = ComponentName(context, IslamicDateWidget::class.java)
            val dateIds = appWidgetManager.getAppWidgetIds(dateComponent)
            if (dateIds.isNotEmpty()) {
                IslamicDateWidget().onUpdate(context, appWidgetManager, dateIds)
                val intent = Intent(context, IslamicDateWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, dateIds)
                }
                context.sendBroadcast(intent)
            }

            // 4. Ramadan Suhoor / Iftar Widget
            val ramadanComponent = ComponentName(context, RamadanWidget::class.java)
            val ramadanIds = appWidgetManager.getAppWidgetIds(ramadanComponent)
            if (ramadanIds.isNotEmpty()) {
                RamadanWidget().onUpdate(context, appWidgetManager, ramadanIds)
                val intent = Intent(context, RamadanWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ramadanIds)
                }
                context.sendBroadcast(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update widgets", e)
        }
    }

    /**
     * Synchronizes the persistent prayer notification bar in the notification shade.
     */
    fun syncNotificationBar(context: Context) {
        try {
            val prefs = AppPreferences(context)
            if (!prefs.isPersistentNotificationEnabled()) return

            val notif = PrayerNotificationHelper.buildPersistentNotification(context)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.notify(PrayerNotificationHelper.NOTIFICATION_ID_PERSISTENT, notif)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync persistent notification", e)
        }
    }

    /**
     * Unified synchronization of both the notification shade and all desktop widgets.
     */
    fun syncAll(context: Context) {
        syncNotificationBar(context)
        updateAllWidgets(context)
    }
}
