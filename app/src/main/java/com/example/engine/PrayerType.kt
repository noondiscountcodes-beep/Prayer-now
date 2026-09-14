package com.example.engine

enum class PrayerType {
    FAJR,
    SUNRISE,
    DHUHR,
    ASR,
    MAGHRIB,
    ISHA,
    JUMUAH;

    val isObligatoryPrayer: Boolean
        get() = this != SUNRISE
}
