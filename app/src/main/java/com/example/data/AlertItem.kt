package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.engine.PrayerType
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

data class AlertItem(
    val id: String = UUID.randomUUID().toString(),
    val targetPrayer: String = "ALL", // "ALL", "FAJR", "DHUHR", etc.
    val minutesBefore: Int = 15,
    val daysOfWeek: Set<Int> = setOf(
        Calendar.SATURDAY,
        Calendar.SUNDAY,
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY
    ), // 1..7
    val soundUri: String? = null,
    val soundName: String? = null,
    val isEnabled: Boolean = true
) {
    fun getPrayerDisplayName(lang: AppLanguage): String {
        return if (targetPrayer == "ALL") {
            when (lang) {
                AppLanguage.ARABIC -> "جميع الصلوات"
                AppLanguage.FRENCH -> "Toutes les prières"
                else -> "All Prayers"
            }
        } else {
            try {
                val pt = PrayerType.valueOf(targetPrayer)
                AppStrings.getPrayerName(pt, lang)
            } catch (e: Exception) {
                targetPrayer
            }
        }
    }

    fun getDaysSummary(lang: AppLanguage): String {
        if (daysOfWeek.size >= 7) {
            return when (lang) {
                AppLanguage.ARABIC -> "جميع الأيام"
                AppLanguage.FRENCH -> "Tous les jours"
                else -> "Everyday"
            }
        }
        val dayNames = daysOfWeek.sorted().map { day ->
            when (day) {
                Calendar.SATURDAY -> when (lang) { AppLanguage.ARABIC -> "السبت"; AppLanguage.FRENCH -> "Sam"; else -> "Sat" }
                Calendar.SUNDAY -> when (lang) { AppLanguage.ARABIC -> "الأحد"; AppLanguage.FRENCH -> "Dim"; else -> "Sun" }
                Calendar.MONDAY -> when (lang) { AppLanguage.ARABIC -> "الإثنين"; AppLanguage.FRENCH -> "Lun"; else -> "Mon" }
                Calendar.TUESDAY -> when (lang) { AppLanguage.ARABIC -> "الثلاثاء"; AppLanguage.FRENCH -> "Mar"; else -> "Tue" }
                Calendar.WEDNESDAY -> when (lang) { AppLanguage.ARABIC -> "الأربعاء"; AppLanguage.FRENCH -> "Mer"; else -> "Wed" }
                Calendar.THURSDAY -> when (lang) { AppLanguage.ARABIC -> "الخميس"; AppLanguage.FRENCH -> "Jeu"; else -> "Thu" }
                Calendar.FRIDAY -> when (lang) { AppLanguage.ARABIC -> "الجمعة"; AppLanguage.FRENCH -> "Ven"; else -> "Fri" }
                else -> ""
            }
        }
        return dayNames.joinToString(", ")
    }

    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("targetPrayer", targetPrayer)
        json.put("minutesBefore", minutesBefore)
        val daysArray = JSONArray()
        daysOfWeek.forEach { daysArray.put(it) }
        json.put("daysOfWeek", daysArray)
        json.put("soundUri", soundUri ?: "")
        json.put("soundName", soundName ?: "")
        json.put("isEnabled", isEnabled)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): AlertItem {
            val id = json.optString("id", UUID.randomUUID().toString())
            val targetPrayer = json.optString("targetPrayer", "ALL")
            val minutesBefore = json.optInt("minutesBefore", 15)
            val daysSet = mutableSetOf<Int>()
            val daysArray = json.optJSONArray("daysOfWeek")
            if (daysArray != null) {
                for (i in 0 until daysArray.length()) {
                    daysSet.add(daysArray.getInt(i))
                }
            } else {
                daysSet.addAll(listOf(1, 2, 3, 4, 5, 6, 7))
            }
            val soundUri = json.optString("soundUri").takeIf { it.isNotBlank() }
            val soundName = json.optString("soundName").takeIf { it.isNotBlank() }
            val isEnabled = json.optBoolean("isEnabled", true)
            return AlertItem(id, targetPrayer, minutesBefore, daysSet, soundUri, soundName, isEnabled)
        }
    }
}

class AlertsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("prayer_alerts_prefs", Context.MODE_PRIVATE)

    fun getAllAlerts(): List<AlertItem> {
        val jsonStr = prefs.getString("alerts_list_json", null) ?: return defaultAlerts()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<AlertItem>()
            for (i in 0 until array.length()) {
                list.add(AlertItem.fromJson(array.getJSONObject(i)))
            }
            if (list.isEmpty()) defaultAlerts() else list
        } catch (e: Exception) {
            defaultAlerts()
        }
    }

    fun saveAlerts(list: List<AlertItem>) {
        val array = JSONArray()
        list.forEach { array.put(it.toJson()) }
        prefs.edit().putString("alerts_list_json", array.toString()).apply()
    }

    fun addAlert(item: AlertItem): List<AlertItem> {
        val current = getAllAlerts().toMutableList()
        current.add(0, item)
        saveAlerts(current)
        return current
    }

    fun updateAlert(item: AlertItem): List<AlertItem> {
        val current = getAllAlerts().toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index != -1) {
            current[index] = item
            saveAlerts(current)
        }
        return current
    }

    fun deleteAlert(id: String): List<AlertItem> {
        val current = getAllAlerts().filter { it.id != id }
        saveAlerts(current)
        return current
    }

    fun duplicateAlert(id: String): List<AlertItem> {
        val current = getAllAlerts().toMutableList()
        val existing = current.find { it.id == id }
        if (existing != null) {
            val copy = existing.copy(id = UUID.randomUUID().toString())
            current.add(current.indexOf(existing) + 1, copy)
            saveAlerts(current)
        }
        return current
    }

    fun toggleAlert(id: String, enabled: Boolean): List<AlertItem> {
        val current = getAllAlerts().toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            current[index] = current[index].copy(isEnabled = enabled)
            saveAlerts(current)
        }
        return current
    }

    private fun defaultAlerts(): List<AlertItem> {
        return listOf(
            AlertItem(
                id = "default_15m_all",
                targetPrayer = "ALL",
                minutesBefore = 15,
                isEnabled = true
            ),
            AlertItem(
                id = "default_fajr_30m",
                targetPrayer = "FAJR",
                minutesBefore = 30,
                isEnabled = false
            )
        )
    }
}
