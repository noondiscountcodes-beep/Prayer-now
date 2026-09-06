package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.engine.CalculationMethod
import com.example.engine.DstSetting
import com.example.engine.Madhab
import com.example.engine.PrayerType
import com.example.localization.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.TimeZone

enum class AppThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

enum class RamadanMode {
    AUTO,
    MANUAL_ON,
    MANUAL_OFF
}

enum class SuhoorAlertMode {
    OFFSET_FAJR,
    FIXED_TIME
}

data class AdhanVideoConfig(
    val uriString: String? = null,
    val fileName: String? = null,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val isCompatible: Boolean = true,
    val isEnabled: Boolean = true
)

class AppPreferences(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("prayer_time_prefs", Context.MODE_PRIVATE)

    // Reactive StateFlows for UI
    private val _languageFlow = MutableStateFlow(loadLanguage())
    val languageFlow: StateFlow<AppLanguage> = _languageFlow.asStateFlow()

    private val _themeFlow = MutableStateFlow(loadTheme())
    val themeFlow: StateFlow<AppThemeMode> = _themeFlow.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0L)
    val refreshTrigger: StateFlow<Long> = _refreshTrigger.asStateFlow()

    fun notifyChanged() {
        _refreshTrigger.value = System.currentTimeMillis()
    }

    // Language
    fun getLanguage(): AppLanguage = loadLanguage()
    fun setLanguage(lang: AppLanguage) {
        prefs.edit().putString("app_language", lang.code).apply()
        _languageFlow.value = lang
        notifyChanged()
    }
    private fun loadLanguage(): AppLanguage {
        val code = prefs.getString("app_language", AppLanguage.ARABIC.code) ?: AppLanguage.ARABIC.code
        return AppLanguage.fromCode(code)
    }

    // Theme
    fun getTheme(): AppThemeMode = loadTheme()
    fun setTheme(mode: AppThemeMode) {
        prefs.edit().putString("app_theme", mode.name).apply()
        _themeFlow.value = mode
        notifyChanged()
    }
    private fun loadTheme(): AppThemeMode {
        val name = prefs.getString("app_theme", AppThemeMode.DARK.name) ?: AppThemeMode.DARK.name
        return try { AppThemeMode.valueOf(name) } catch (e: Exception) { AppThemeMode.DARK }
    }

    // Location
    fun isGpsLocation(): Boolean = prefs.getBoolean("is_gps_location", false)
    fun setGpsLocation(enabled: Boolean) {
        prefs.edit().putBoolean("is_gps_location", enabled).apply()
        notifyChanged()
    }

    fun getCityName(): String = prefs.getString("city_name", CitiesRepository.defaultCity.nameAr) ?: CitiesRepository.defaultCity.nameAr
    fun getCountryName(): String = prefs.getString("country_name", CitiesRepository.defaultCity.countryAr) ?: CitiesRepository.defaultCity.countryAr
    fun getLatitude(): Double = java.lang.Double.longBitsToDouble(prefs.getLong("latitude", java.lang.Double.doubleToLongBits(CitiesRepository.defaultCity.latitude)))
    fun getLongitude(): Double = java.lang.Double.longBitsToDouble(prefs.getLong("longitude", java.lang.Double.doubleToLongBits(CitiesRepository.defaultCity.longitude)))

    fun setLocation(cityName: String, countryName: String, lat: Double, lng: Double, timezoneId: String, isGps: Boolean) {
        prefs.edit()
            .putString("city_name", cityName)
            .putString("country_name", countryName)
            .putLong("latitude", java.lang.Double.doubleToLongBits(lat))
            .putLong("longitude", java.lang.Double.doubleToLongBits(lng))
            .putString("timezone_id", timezoneId)
            .putBoolean("is_gps_location", isGps)
            .apply()
        notifyChanged()
    }

    fun clearLocationData() {
        // Privacy First: Resets location to default Makkah coordinates
        setLocation(
            CitiesRepository.defaultCity.nameAr,
            CitiesRepository.defaultCity.countryAr,
            CitiesRepository.defaultCity.latitude,
            CitiesRepository.defaultCity.longitude,
            CitiesRepository.defaultCity.timezoneId,
            false
        )
    }

    // Timezone
    fun isTimezoneAuto(): Boolean = prefs.getBoolean("tz_auto", true)
    fun setTimezoneAuto(auto: Boolean) {
        prefs.edit().putBoolean("tz_auto", auto).apply()
        notifyChanged()
    }

    fun getTimezone(): TimeZone {
        return if (isTimezoneAuto()) {
            TimeZone.getDefault()
        } else {
            val tzId = prefs.getString("timezone_id", TimeZone.getDefault().id) ?: TimeZone.getDefault().id
            TimeZone.getTimeZone(tzId)
        }
    }

    fun setManualTimezoneId(id: String) {
        prefs.edit().putString("timezone_id", id).apply()
        notifyChanged()
    }

    // DST
    fun getDstSetting(): DstSetting {
        val name = prefs.getString("dst_setting", DstSetting.AUTO.name) ?: DstSetting.AUTO.name
        return try { DstSetting.valueOf(name) } catch (e: Exception) { DstSetting.AUTO }
    }
    fun setDstSetting(dst: DstSetting) {
        prefs.edit().putString("dst_setting", dst.name).apply()
        notifyChanged()
    }

    // Calculation Method
    fun getCalculationMethod(): CalculationMethod {
        val name = prefs.getString("calc_method", CalculationMethod.UMM_AL_QURA.name) ?: CalculationMethod.UMM_AL_QURA.name
        return try { CalculationMethod.valueOf(name) } catch (e: Exception) { CalculationMethod.UMM_AL_QURA }
    }
    fun setCalculationMethod(method: CalculationMethod) {
        prefs.edit().putString("calc_method", method.name).apply()
        notifyChanged()
    }

    fun getCustomFajrAngle(): Double = java.lang.Double.longBitsToDouble(prefs.getLong("custom_fajr_angle", java.lang.Double.doubleToLongBits(18.0)))
    fun setCustomFajrAngle(angle: Double) {
        prefs.edit().putLong("custom_fajr_angle", java.lang.Double.doubleToLongBits(angle)).apply()
        notifyChanged()
    }

    fun getCustomIshaAngle(): Double = java.lang.Double.longBitsToDouble(prefs.getLong("custom_isha_angle", java.lang.Double.doubleToLongBits(17.0)))
    fun setCustomIshaAngle(angle: Double) {
        prefs.edit().putLong("custom_isha_angle", java.lang.Double.doubleToLongBits(angle)).apply()
        notifyChanged()
    }

    // Madhab
    fun getMadhab(): Madhab {
        val name = prefs.getString("madhab", Madhab.SHAFI.name) ?: Madhab.SHAFI.name
        return try { Madhab.valueOf(name) } catch (e: Exception) { Madhab.SHAFI }
    }
    fun setMadhab(madhab: Madhab) {
        prefs.edit().putString("madhab", madhab.name).apply()
        notifyChanged()
    }

    // Persistent Notification Bar
    fun isPersistentNotificationEnabled(): Boolean = prefs.getBoolean("persistent_notif_enabled", true)
    fun setPersistentNotificationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("persistent_notif_enabled", enabled).apply()
        notifyChanged()
    }

    // Ramadan
    fun getRamadanMode(): RamadanMode {
        val name = prefs.getString("ramadan_mode", RamadanMode.AUTO.name) ?: RamadanMode.AUTO.name
        return try { RamadanMode.valueOf(name) } catch (e: Exception) { RamadanMode.AUTO }
    }
    fun setRamadanMode(mode: RamadanMode) {
        prefs.edit().putString("ramadan_mode", mode.name).apply()
        notifyChanged()
    }

    fun getSuhoorAlertMode(): SuhoorAlertMode {
        val name = prefs.getString("suhoor_alert_mode", SuhoorAlertMode.OFFSET_FAJR.name) ?: SuhoorAlertMode.OFFSET_FAJR.name
        return try { SuhoorAlertMode.valueOf(name) } catch (e: Exception) { SuhoorAlertMode.OFFSET_FAJR }
    }
    fun setSuhoorAlertMode(mode: SuhoorAlertMode) {
        prefs.edit().putString("suhoor_alert_mode", mode.name).apply()
        notifyChanged()
    }

    fun getSuhoorOffsetMinutes(): Int = prefs.getInt("suhoor_offset_minutes", 45)
    fun setSuhoorOffsetMinutes(min: Int) {
        prefs.edit().putInt("suhoor_offset_minutes", min).apply()
        notifyChanged()
    }

    fun getSuhoorManualTime(): String = prefs.getString("suhoor_manual_time", "04:00") ?: "04:00"
    fun setSuhoorManualTime(time: String) {
        prefs.edit().putString("suhoor_manual_time", time).apply()
        notifyChanged()
    }

    // Musaharati Media
    fun getMusaharatiConfig(): AdhanVideoConfig {
        return AdhanVideoConfig(
            uriString = prefs.getString("musaharati_uri", null),
            fileName = prefs.getString("musaharati_name", null),
            durationMs = prefs.getLong("musaharati_duration", 0L),
            sizeBytes = prefs.getLong("musaharati_size", 0L),
            isCompatible = prefs.getBoolean("musaharati_compatible", true),
            isEnabled = prefs.getBoolean("musaharati_enabled", true)
        )
    }

    fun setMusaharatiConfig(config: AdhanVideoConfig) {
        prefs.edit()
            .putString("musaharati_uri", config.uriString)
            .putString("musaharati_name", config.fileName)
            .putLong("musaharati_duration", config.durationMs)
            .putLong("musaharati_size", config.sizeBytes)
            .putBoolean("musaharati_compatible", config.isCompatible)
            .putBoolean("musaharati_enabled", config.isEnabled)
            .apply()
        notifyChanged()
    }

    // Adhan Videos for each prayer
    fun getAdhanConfig(type: PrayerType): AdhanVideoConfig {
        val keyPrefix = "adhan_${type.name.lowercase()}"
        return AdhanVideoConfig(
            uriString = prefs.getString("${keyPrefix}_uri", null),
            fileName = prefs.getString("${keyPrefix}_name", null),
            durationMs = prefs.getLong("${keyPrefix}_duration", 0L),
            sizeBytes = prefs.getLong("${keyPrefix}_size", 0L),
            isCompatible = prefs.getBoolean("${keyPrefix}_compatible", true),
            isEnabled = prefs.getBoolean("${keyPrefix}_enabled", true)
        )
    }

    fun setAdhanConfig(type: PrayerType, config: AdhanVideoConfig) {
        val keyPrefix = "adhan_${type.name.lowercase()}"
        prefs.edit()
            .putString("${keyPrefix}_uri", config.uriString)
            .putString("${keyPrefix}_name", config.fileName)
            .putLong("${keyPrefix}_duration", config.durationMs)
            .putLong("${keyPrefix}_size", config.sizeBytes)
            .putBoolean("${keyPrefix}_compatible", config.isCompatible)
            .putBoolean("${keyPrefix}_enabled", config.isEnabled)
            .apply()
        notifyChanged()
    }

    fun resetAdhanConfig(type: PrayerType) {
        val keyPrefix = "adhan_${type.name.lowercase()}"
        prefs.edit()
            .remove("${keyPrefix}_uri")
            .remove("${keyPrefix}_name")
            .remove("${keyPrefix}_duration")
            .remove("${keyPrefix}_size")
            .remove("${keyPrefix}_compatible")
            .apply()
        notifyChanged()
    }
}
