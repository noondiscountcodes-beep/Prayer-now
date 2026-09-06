package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.engine.PrayerType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdhanPreferencesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("adhan_settings_prefs", Context.MODE_PRIVATE)

    private val _updates = MutableStateFlow(0L)
    val updates: StateFlow<Long> = _updates.asStateFlow()

    private fun notifyUpdate() {
        _updates.value = System.currentTimeMillis()
    }

    // 1. Audio Config (Adhan Sound)
    fun getAudioConfig(prayer: PrayerType): PrayerAudioConfig {
        val p = prayer.name.lowercase()
        return PrayerAudioConfig(
            uriString = prefs.getString("audio_${p}_uri", null),
            fileName = prefs.getString("audio_${p}_name", null),
            durationMs = prefs.getLong("audio_${p}_duration", 0L),
            sizeBytes = prefs.getLong("audio_${p}_size", 0L),
            isCompatible = prefs.getBoolean("audio_${p}_compat", true),
            isEnabled = prefs.getBoolean("audio_${p}_enabled", true)
        )
    }

    fun setAudioConfig(prayer: PrayerType, config: PrayerAudioConfig) {
        val p = prayer.name.lowercase()
        prefs.edit()
            .putString("audio_${p}_uri", config.uriString)
            .putString("audio_${p}_name", config.fileName)
            .putLong("audio_${p}_duration", config.durationMs)
            .putLong("audio_${p}_size", config.sizeBytes)
            .putBoolean("audio_${p}_compat", config.isCompatible)
            .putBoolean("audio_${p}_enabled", config.isEnabled)
            .apply()
        notifyUpdate()
    }

    fun resetAudioConfig(prayer: PrayerType) {
        val p = prayer.name.lowercase()
        prefs.edit()
            .remove("audio_${p}_uri")
            .remove("audio_${p}_name")
            .remove("audio_${p}_duration")
            .remove("audio_${p}_size")
            .remove("audio_${p}_compat")
            .putBoolean("audio_${p}_enabled", true)
            .apply()
        notifyUpdate()
    }

    // 2. Alert Sound (Immediately before Adhan at exact prayer time)
    fun getAlertConfig(prayer: PrayerType): PrayerAlertSoundConfig {
        val p = prayer.name.lowercase()
        return PrayerAlertSoundConfig(
            uriString = prefs.getString("alert_${p}_uri", null),
            fileName = prefs.getString("alert_${p}_name", null),
            durationMs = prefs.getLong("alert_${p}_duration", 0L),
            sizeBytes = prefs.getLong("alert_${p}_size", 0L),
            isCompatible = prefs.getBoolean("alert_${p}_compat", true),
            isEnabled = prefs.getBoolean("alert_${p}_enabled", false)
        )
    }

    fun setAlertConfig(prayer: PrayerType, config: PrayerAlertSoundConfig) {
        val p = prayer.name.lowercase()
        prefs.edit()
            .putString("alert_${p}_uri", config.uriString)
            .putString("alert_${p}_name", config.fileName)
            .putLong("alert_${p}_duration", config.durationMs)
            .putLong("alert_${p}_size", config.sizeBytes)
            .putBoolean("alert_${p}_compat", config.isCompatible)
            .putBoolean("alert_${p}_enabled", config.isEnabled)
            .apply()
        notifyUpdate()
    }

    fun resetAlertConfig(prayer: PrayerType) {
        val p = prayer.name.lowercase()
        prefs.edit()
            .remove("alert_${p}_uri")
            .remove("alert_${p}_name")
            .remove("alert_${p}_duration")
            .remove("alert_${p}_size")
            .remove("alert_${p}_compat")
            .putBoolean("alert_${p}_enabled", false)
            .apply()
        notifyUpdate()
    }

    // 3. Post-Adhan Du'aa Video
    fun getDuaConfig(prayer: PrayerType): PrayerDuaVideoConfig {
        val p = prayer.name.lowercase()
        return PrayerDuaVideoConfig(
            uriString = prefs.getString("dua_${p}_uri", null),
            fileName = prefs.getString("dua_${p}_name", null),
            durationMs = prefs.getLong("dua_${p}_duration", 0L),
            sizeBytes = prefs.getLong("dua_${p}_size", 0L),
            isCompatible = prefs.getBoolean("dua_${p}_compat", true),
            isEnabled = prefs.getBoolean("dua_${p}_enabled", true)
        )
    }

    fun setDuaConfig(prayer: PrayerType, config: PrayerDuaVideoConfig) {
        val p = prayer.name.lowercase()
        prefs.edit()
            .putString("dua_${p}_uri", config.uriString)
            .putString("dua_${p}_name", config.fileName)
            .putLong("dua_${p}_duration", config.durationMs)
            .putLong("dua_${p}_size", config.sizeBytes)
            .putBoolean("dua_${p}_compat", config.isCompatible)
            .putBoolean("dua_${p}_enabled", config.isEnabled)
            .apply()
        notifyUpdate()
    }

    fun resetDuaConfig(prayer: PrayerType) {
        val p = prayer.name.lowercase()
        prefs.edit()
            .remove("dua_${p}_uri")
            .remove("dua_${p}_name")
            .remove("dua_${p}_duration")
            .remove("dua_${p}_size")
            .remove("dua_${p}_compat")
            .putBoolean("dua_${p}_enabled", true)
            .apply()
        notifyUpdate()
    }

    // 4. Adhan Screen Settings
    fun getScreenConfig(prayer: PrayerType): AdhanScreenConfig {
        val p = prayer.name.lowercase()
        val modeStr = prefs.getString("screen_${p}_mode", AdhanScreenDisplayMode.SLIDESHOW.name)
            ?: AdhanScreenDisplayMode.SLIDESHOW.name
        val mode = try { AdhanScreenDisplayMode.valueOf(modeStr) } catch (e: Exception) { AdhanScreenDisplayMode.SLIDESHOW }
        val interval = prefs.getInt("screen_${p}_interval", 5)
        val showPrayer = prefs.getBoolean("screen_${p}_show_prayer", true)
        val showClock = prefs.getBoolean("screen_${p}_show_clock", true)
        val showHijri = prefs.getBoolean("screen_${p}_show_hijri", true)
        val showGregorian = prefs.getBoolean("screen_${p}_show_gregorian", true)
        val showRemaining = prefs.getBoolean("screen_${p}_show_remaining", true)
        val selectedImagesStr = prefs.getString("screen_${p}_images", "") ?: ""
        val selectedImages = if (selectedImagesStr.isBlank()) emptyList() else selectedImagesStr.split("|")

        return AdhanScreenConfig(
            displayMode = mode,
            slideIntervalSec = interval,
            showPrayerName = showPrayer,
            showClock = showClock,
            showHijriDate = showHijri,
            showGregorianDate = showGregorian,
            showRemainingTime = showRemaining,
            selectedImageNames = selectedImages
        )
    }

    fun setScreenConfig(prayer: PrayerType, config: AdhanScreenConfig) {
        val p = prayer.name.lowercase()
        prefs.edit()
            .putString("screen_${p}_mode", config.displayMode.name)
            .putInt("screen_${p}_interval", config.slideIntervalSec)
            .putBoolean("screen_${p}_show_prayer", config.showPrayerName)
            .putBoolean("screen_${p}_show_clock", config.showClock)
            .putBoolean("screen_${p}_show_hijri", config.showHijriDate)
            .putBoolean("screen_${p}_show_gregorian", config.showGregorianDate)
            .putBoolean("screen_${p}_show_remaining", config.showRemainingTime)
            .putString("screen_${p}_images", config.selectedImageNames.joinToString("|"))
            .apply()
        notifyUpdate()
    }
}
