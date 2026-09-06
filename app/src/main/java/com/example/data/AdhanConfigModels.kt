package com.example.data

enum class AdhanScreenDisplayMode {
    SINGLE,
    SLIDESHOW
}

data class PrayerAudioConfig(
    val uriString: String? = null,
    val fileName: String? = null,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val isCompatible: Boolean = true,
    val isEnabled: Boolean = true
)

data class PrayerAlertSoundConfig(
    val uriString: String? = null,
    val fileName: String? = null,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val isCompatible: Boolean = true,
    val isEnabled: Boolean = false
)

data class PrayerDuaVideoConfig(
    val uriString: String? = null,
    val fileName: String? = null,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val isCompatible: Boolean = true,
    val isEnabled: Boolean = true
)

data class AdhanScreenConfig(
    val displayMode: AdhanScreenDisplayMode = AdhanScreenDisplayMode.SLIDESHOW,
    val slideIntervalSec: Int = 5,
    val showPrayerName: Boolean = true,
    val showClock: Boolean = true,
    val showHijriDate: Boolean = true,
    val showGregorianDate: Boolean = true,
    val showRemainingTime: Boolean = true,
    val selectedImageNames: List<String> = emptyList()
)
